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
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

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
                        visualizer = syntheticVisualizer(playing = false),
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
                    emitState(
                        onState = onState,
                        isPlaying = true,
                        visualizer = syntheticVisualizer(playing = true),
                    )
                }
            }
        }
    }

    override fun playNext(onState: (PlaybackSnapshot) -> Unit) {
        runOnFxThread {
            if (queue.isEmpty()) return@runOnFxThread
            currentIndex = (currentIndex + 1).coerceAtMost(queue.lastIndex)
            playCurrentLocked(onState)
        }
    }

    override fun playPrevious(onState: (PlaybackSnapshot) -> Unit) {
        runOnFxThread {
            if (queue.isEmpty()) return@runOnFxThread
            // Restart track if >3s in, else previous
            val pos = mediaPlayer?.currentTime?.toMillis()?.toLong() ?: 0L
            if (pos > 3000L) {
                mediaPlayer?.seek(Duration.ZERO)
                emitState(onState, isPlaying = mediaPlayer?.status == MediaPlayer.Status.PLAYING)
            } else {
                currentIndex = (currentIndex - 1).coerceAtLeast(0)
                playCurrentLocked(onState)
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

        val player = runCatching { MediaPlayer(Media(track.uri)) }.getOrElse { error ->
            onState(
                PlaybackSnapshot(
                    currentTrackId = track.id,
                    isPlaying = false,
                    errorMessage = error.message ?: "Playback failed.",
                    visualizer = PlaybackVisualizerState.idle(32),
                ),
            )
            return
        }
        player.volume = volume.toDouble()
        player.audioSpectrumNumBands = 32
        player.audioSpectrumInterval = 0.08
        player.audioSpectrumThreshold = -70

        player.currentTimeProperty().addListener { _, _, currentTime ->
            if (sessionId != activeSessionId || mediaPlayer !== player) return@addListener
            val now = System.currentTimeMillis()
            if (now - lastTimelineEmitAtMs < 120L) return@addListener
            lastTimelineEmitAtMs = now
            lastKnownPositionMs = currentTime.toMillis().toLong().coerceAtLeast(0L)
            // Keep visualizer alive even when spectrum is silent (common for some codecs)
            val playing = player.status == MediaPlayer.Status.PLAYING
            emitState(
                onState = onState,
                isPlaying = playing,
                positionMs = lastKnownPositionMs,
                visualizer = if (playing) {
                    // Only inject synthetic if no recent spectrum
                    if (now - lastSpectrumEmitAtMs > 250L) {
                        syntheticVisualizer(playing = true, positionMs = lastKnownPositionMs)
                    } else {
                        null
                    }
                } else {
                    PlaybackVisualizerState.idle(32)
                },
            )
        }

        player.setAudioSpectrumListener { _, _, magnitudes, _ ->
            if (sessionId != activeSessionId || mediaPlayer !== player) return@setAudioSpectrumListener
            val now = System.currentTimeMillis()
            if (now - lastSpectrumEmitAtMs < 70L) return@setAudioSpectrumListener
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
            // If spectrum is flat, fall back to synthetic ASCII motion
            val peak = reactiveBands.maxOrNull() ?: 0f
            val bands = if (peak < 0.04f && player.status == MediaPlayer.Status.PLAYING) {
                syntheticBands(positionMs = lastKnownPositionMs)
            } else {
                reactiveBands
            }
            val intensity = bands.average().toFloat().coerceIn(0f, 1f)
            onState(
                PlaybackSnapshot(
                    currentTrackId = track.id,
                    isPlaying = true,
                    visualizer = PlaybackVisualizerState(
                        bands = bands,
                        intensity = intensity,
                        active = true,
                    ),
                    positionMs = player.currentTime?.toMillis()?.toLong()?.coerceAtLeast(0L) ?: lastKnownPositionMs,
                    durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong() ?: track.durationMs,
                    volume = volume,
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
                onState(
                    PlaybackSnapshot(
                        currentTrackId = track.id,
                        isPlaying = false,
                        visualizer = PlaybackVisualizerState.idle(32),
                        positionMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong() ?: track.durationMs,
                        durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong() ?: track.durationMs,
                        volume = volume,
                    ),
                )
            }
        }
        player.setOnError {
            if (sessionId != activeSessionId || mediaPlayer !== player) return@setOnError
            onState(
                PlaybackSnapshot(
                    currentTrackId = track.id,
                    isPlaying = false,
                    errorMessage = player.error?.message ?: "Playback failed.",
                    visualizer = PlaybackVisualizerState.idle(32),
                    volume = volume,
                ),
            )
        }
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

    private fun syntheticVisualizer(playing: Boolean, positionMs: Long = lastKnownPositionMs): PlaybackVisualizerState {
        if (!playing) return PlaybackVisualizerState.idle(32)
        val bands = syntheticBands(positionMs)
        return PlaybackVisualizerState(bands = bands, intensity = bands.average().toFloat(), active = true)
    }

    private fun syntheticBands(positionMs: Long): List<Float> {
        val t = positionMs / 1000.0
        return List(32) { i ->
            val wave = sin(t * 2.4 + i * 0.55) * 0.5 + 0.5
            val pulse = sin(t * 5.1 + i * 0.2) * 0.25 + 0.55
            val noise = Random((positionMs / 80L + i).toInt()).nextFloat() * 0.12f
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
}
