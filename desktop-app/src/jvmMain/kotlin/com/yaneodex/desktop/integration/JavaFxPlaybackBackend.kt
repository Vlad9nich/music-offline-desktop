package com.yaneodex.desktop.integration

import com.yaneodex.core.contracts.PlaybackBackend
import com.yaneodex.core.contracts.PlaybackSnapshot
import com.yaneodex.core.model.TrackRecord
import com.yaneodex.core.state.PlaybackVisualizerState
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Thread-safe JavaFX media backend.
 * All mutations hop to the FX thread; session ids invalidate stale callbacks after rapid seeks/clicks.
 */
class JavaFxPlaybackBackend : PlaybackBackend {
    private var queue: List<TrackRecord> = emptyList()
    private var currentIndex: Int = -1
    private var mediaPlayer: MediaPlayer? = null
    private var volume: Float = 0.72f
    private val sessionCounter = AtomicLong(0L)
    @Volatile private var activeSessionId: Long = 0L
    private var lastTimelineEmitAtMs: Long = 0L
    private var lastSpectrumEmitAtMs: Long = 0L
    private var lastKnownPositionMs: Long = 0L
    private var currentTrackId: String? = null

    override fun playQueue(
        queue: List<TrackRecord>,
        startTrackId: String?,
        onState: (PlaybackSnapshot) -> Unit,
    ) {
        if (queue.isEmpty()) {
            onState(PlaybackSnapshot())
            return
        }
        // Snapshot on caller thread so concurrent mutate races don't corrupt queue mid-play.
        val snapshotQueue = queue.toList()
        val startIndex = snapshotQueue.indexOfFirst { it.id == startTrackId }.takeIf { it >= 0 } ?: 0
        runOnFxThread {
            this.queue = snapshotQueue
            this.currentIndex = startIndex
            playCurrentLocked(onState)
        }
    }

    /**
     * Swaps the play order in place. The audio session, position and play/pause state are
     * untouched — only the index of the current track is recalculated. This is what keeps
     * the UI queue and the player queue from drifting apart after a shuffle toggle.
     */
    override fun setQueue(queue: List<TrackRecord>, onState: (PlaybackSnapshot) -> Unit) {
        val snapshotQueue = queue.toList()
        runOnFxThread {
            this.queue = snapshotQueue
            val anchorId = currentTrackId ?: snapshotQueue.getOrNull(currentIndex)?.id
            val anchoredIndex = snapshotQueue.indexOfFirst { it.id == anchorId }
            currentIndex = when {
                anchoredIndex >= 0 -> anchoredIndex
                snapshotQueue.isEmpty() -> -1
                else -> 0
            }
            emitState(
                onState = onState,
                isPlaying = mediaPlayer?.status == MediaPlayer.Status.PLAYING,
            )
        }
    }

    override fun togglePlayPause(onState: (PlaybackSnapshot) -> Unit) {
        runOnFxThread {
            val player = mediaPlayer
            if (player == null) {
                // Recover after dispose races: restart current queue item.
                if (queue.isNotEmpty() && currentIndex in queue.indices) {
                    playCurrentLocked(onState)
                }
                return@runOnFxThread
            }
            when (player.status) {
                MediaPlayer.Status.PLAYING -> {
                    player.pause()
                    emitState(
                        onState = onState,
                        isPlaying = false,
                        visualizer = PlaybackVisualizerState.idle(SPECTRUM_BANDS),
                    )
                }
                MediaPlayer.Status.UNKNOWN,
                MediaPlayer.Status.HALTED,
                MediaPlayer.Status.DISPOSED,
                -> {
                    playCurrentLocked(onState)
                }
                else -> {
                    player.play()
                    // The spectrum listener supplies the next frame; null keeps the last one
                    // on screen instead of flashing a synthetic burst.
                    emitState(onState = onState, isPlaying = true, visualizer = null)
                }
            }
        }
    }

    override fun playNext(onState: (PlaybackSnapshot) -> Unit) {
        runOnFxThread {
            if (queue.isEmpty()) return@runOnFxThread
            if (currentIndex < queue.lastIndex) {
                currentIndex += 1
                playCurrentLocked(onState)
            } else {
                // End of queue: hand the decision to the controller instead of silently doing nothing.
                emitQueueExhausted(onState)
            }
        }
    }

    override fun playPrevious(onState: (PlaybackSnapshot) -> Unit) {
        runOnFxThread {
            if (queue.isEmpty()) return@runOnFxThread
            // Restart the track if we're more than 3s in, otherwise step back.
            // The order is stable while a cycle plays, so index-1 is what was actually heard.
            val pos = mediaPlayer?.currentTime?.toMillis()?.toLong() ?: 0L
            if (pos > 3000L) {
                mediaPlayer?.seek(Duration.ZERO)
                lastKnownPositionMs = 0L
                emitState(onState, isPlaying = mediaPlayer?.status == MediaPlayer.Status.PLAYING, positionMs = 0L)
            } else if (currentIndex > 0) {
                currentIndex -= 1
                playCurrentLocked(onState)
            } else {
                mediaPlayer?.seek(Duration.ZERO)
                lastKnownPositionMs = 0L
                emitState(onState, isPlaying = mediaPlayer?.status == MediaPlayer.Status.PLAYING, positionMs = 0L)
            }
        }
    }

    override fun seekTo(positionMs: Long, onState: (PlaybackSnapshot) -> Unit) {
        runOnFxThread {
            val player = mediaPlayer ?: return@runOnFxThread
            val durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong()
                ?: queue.getOrNull(currentIndex)?.durationMs
                ?: 0L
            val targetMs = positionMs.coerceIn(0L, durationMs.takeIf { it > 0 } ?: positionMs.coerceAtLeast(0L))
            player.seek(Duration.millis(targetMs.toDouble()))
            lastKnownPositionMs = targetMs
            emitState(onState, isPlaying = player.status == MediaPlayer.Status.PLAYING, positionMs = targetMs)
        }
    }

    override fun setVolume(volume: Float, onState: (PlaybackSnapshot) -> Unit) {
        this.volume = volume.coerceIn(0f, 1f)
        runOnFxThread {
            mediaPlayer?.volume = this.volume.toDouble()
            emitState(
                onState = onState,
                isPlaying = mediaPlayer?.status == MediaPlayer.Status.PLAYING,
            )
        }
    }

    override fun stop() {
        runOnFxThread {
            invalidateSession()
            disposePlayer()
        }
    }

    private fun playCurrentLocked(onState: (PlaybackSnapshot) -> Unit) {
        val track = queue.getOrNull(currentIndex)
        if (track == null) {
            onState(PlaybackSnapshot())
            return
        }
        invalidateSession()
        disposePlayer()
        val sessionId = sessionCounter.incrementAndGet()
        activeSessionId = sessionId
        lastKnownPositionMs = 0L
        currentTrackId = track.id

        val player = runCatching { MediaPlayer(Media(track.uri)) }.getOrElse { error ->
            onState(
                PlaybackSnapshot(
                    currentTrackId = track.id,
                    isPlaying = false,
                    errorMessage = error.message ?: "Playback failed.",
                    visualizer = PlaybackVisualizerState.idle(SPECTRUM_BANDS),
                ),
            )
            return
        }
        player.volume = volume.toDouble()
        player.audioSpectrumNumBands = SPECTRUM_BANDS
        player.audioSpectrumInterval = SPECTRUM_INTERVAL_SECONDS
        player.audioSpectrumThreshold = -70

        player.currentTimeProperty().addListener { _, _, currentTime ->
            if (sessionId != activeSessionId || mediaPlayer !== player) return@addListener
            val now = System.currentTimeMillis()
            if (now - lastTimelineEmitAtMs < TIMELINE_EMIT_INTERVAL_MS) return@addListener
            lastTimelineEmitAtMs = now
            lastKnownPositionMs = currentTime.toMillis().toLong().coerceAtLeast(0L)
            val playing = player.status == MediaPlayer.Status.PLAYING
            // Timeline ticks carry position only. Spectrum frames own the visualizer, so the
            // two streams no longer fight each other and the bars stop stuttering.
            emitState(
                onState = onState,
                isPlaying = playing,
                positionMs = lastKnownPositionMs,
                visualizer = if (playing) null else PlaybackVisualizerState.idle(SPECTRUM_BANDS),
            )
        }

        player.setAudioSpectrumListener { _, _, magnitudes, _ ->
            if (sessionId != activeSessionId || mediaPlayer !== player) return@setAudioSpectrumListener
            val now = System.currentTimeMillis()
            if (now - lastSpectrumEmitAtMs < SPECTRUM_EMIT_INTERVAL_MS) return@setAudioSpectrumListener
            lastSpectrumEmitAtMs = now
            val reactiveBands = magnitudes.mapIndexed { index, magnitude ->
                val normalized = ((magnitude + 70f) / 70f).coerceIn(0f, 1f)
                val bassBias = when {
                    index <= 3 -> 1.7f
                    index <= 7 -> 1.35f
                    else -> 1f - ((index - 8).coerceAtLeast(0) * 0.01f)
                }
                (normalized.toDouble().pow(0.72).toFloat() * bassBias).coerceIn(0f, 1f)
            }
            // JavaFX returns a flat spectrum for a few Windows codecs. Rather than faking bars
            // and pretending they are audio, report the fallback so the UI can label it.
            val playing = player.status == MediaPlayer.Status.PLAYING
            val liveSpectrum = (reactiveBands.maxOrNull() ?: 0f) >= FLAT_SPECTRUM_THRESHOLD
            val bands = if (playing && !liveSpectrum) {
                syntheticBands(positionMs = lastKnownPositionMs)
            } else {
                reactiveBands
            }
            val intensity = bands.average().toFloat().coerceIn(0f, 1f)
            onState(
                PlaybackSnapshot(
                    currentTrackId = track.id,
                    isPlaying = true,
                    positionMs = player.currentTime?.toMillis()?.toLong()?.coerceAtLeast(0L) ?: lastKnownPositionMs,
                    durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong() ?: track.durationMs,
                    volume = volume,
                    visualizer = PlaybackVisualizerState(
                        bands = bands,
                        intensity = intensity,
                        active = playing,
                        spectrumLive = playing && liveSpectrum,
                    ),
                ),
            )
        }

        mediaPlayer = player
        player.setOnReady {
            if (sessionId != activeSessionId || mediaPlayer !== player) return@setOnReady
            player.play()
            onState(
                PlaybackSnapshot(
                    currentTrackId = track.id,
                    isPlaying = true,
                    visualizer = PlaybackVisualizerState(
                        bands = syntheticBands(0L),
                        intensity = 0.35f,
                        active = true,
                        spectrumLive = false,
                    ),
                    positionMs = 0L,
                    durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong() ?: track.durationMs,
                    volume = volume,
                ),
            )
        }
        player.setOnEndOfMedia {
            if (sessionId != activeSessionId || mediaPlayer !== player) return@setOnEndOfMedia
            if (currentIndex < queue.lastIndex) {
                currentIndex += 1
                playCurrentLocked(onState)
            } else {
                // Repeat/reshuffle is a product decision, so ask the controller instead of stopping dead.
                emitQueueExhausted(onState)
            }
        }
        player.setOnError {
            if (sessionId != activeSessionId || mediaPlayer !== player) return@setOnError
            onState(
                PlaybackSnapshot(
                    currentTrackId = track.id,
                    isPlaying = false,
                    errorMessage = player.error?.message ?: "Playback failed.",
                    visualizer = PlaybackVisualizerState.idle(SPECTRUM_BANDS),
                    volume = volume,
                ),
            )
        }
    }

    /** Tells the controller that the queue ran out, so it can wrap around or reshuffle. */
    private fun emitQueueExhausted(onState: (PlaybackSnapshot) -> Unit) {
        val track = queue.getOrNull(currentIndex)
        val durationMs = mediaPlayer?.totalDuration?.toMillis()
            ?.takeIf { it.isFinite() && it > 0 }?.toLong()
            ?: track?.durationMs
            ?: 0L
        onState(
            PlaybackSnapshot(
                currentTrackId = track?.id,
                isPlaying = false,
                visualizer = PlaybackVisualizerState.idle(SPECTRUM_BANDS),
                positionMs = durationMs,
                durationMs = durationMs,
                volume = volume,
                queueExhausted = true,
            ),
        )
    }

    private fun emitState(
        onState: (PlaybackSnapshot) -> Unit,
        isPlaying: Boolean,
        positionMs: Long? = null,
        visualizer: PlaybackVisualizerState? = null,
    ) {
        val track = queue.getOrNull(currentIndex)
        val player = mediaPlayer
        onState(
            PlaybackSnapshot(
                currentTrackId = track?.id,
                isPlaying = isPlaying,
                visualizer = visualizer,
                positionMs = positionMs
                    ?: player?.currentTime?.toMillis()?.toLong()?.coerceAtLeast(0L)
                    ?: lastKnownPositionMs,
                durationMs = player?.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong()
                    ?: track?.durationMs
                    ?: 0L,
                volume = volume,
            ),
        )
    }

    /**
     * Fallback motion for codecs whose spectrum JavaFX reports as flat.
     *
     * Deliberately cheap: a phase-driven sine pair plus a precomputed noise table, so a frame
     * costs no allocations (the previous version created a `Random` per band per frame).
     */
    private fun syntheticBands(positionMs: Long): List<Float> {
        val t = positionMs / 1000.0
        return List(SPECTRUM_BANDS) { index ->
            val wave = sin(t * 2.4 + index * 0.55) * 0.5 + 0.5
            val pulse = sin(t * 5.1 + index * 0.2) * 0.25 + 0.55
            val noise = NOISE_TABLE[((positionMs / 80L).toInt() + index * 7).mod(NOISE_TABLE.size)] * 0.12f
            ((wave * pulse).toFloat() + noise).coerceIn(0.08f, 1f)
        }
    }

    private fun invalidateSession() {
        activeSessionId = sessionCounter.incrementAndGet()
    }

    private fun disposePlayer() {
        mediaPlayer?.runCatching {
            stop()
            dispose()
        }
        mediaPlayer = null
    }

    private fun ensureStarted() {
        JavaFxRuntime.ensureInitialized()
    }

    private fun runOnFxThread(block: () -> Unit) {
        ensureStarted()
        if (Platform.isFxApplicationThread()) {
            block()
        } else {
            Platform.runLater(block)
        }
    }

    private companion object {
        const val SPECTRUM_BANDS = 32

        /** ~30 fps of spectrum frames: smooth enough for the renderer to interpolate up to 60. */
        const val SPECTRUM_INTERVAL_SECONDS = 0.033
        const val SPECTRUM_EMIT_INTERVAL_MS = 33L
        const val TIMELINE_EMIT_INTERVAL_MS = 250L

        /** Below this peak JavaFX is effectively reporting "no spectrum" for the current codec. */
        const val FLAT_SPECTRUM_THRESHOLD = 0.04f

        /** Fixed pseudo-noise so the fallback bars breathe without allocating per frame. */
        val NOISE_TABLE = FloatArray(97) { index -> ((index * 37 + 11) % 100) / 100f }
    }
}
