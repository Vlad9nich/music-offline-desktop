package com.yaneodex.desktop.integration

import com.yaneodex.core.contracts.PlaybackBackend
import com.yaneodex.core.contracts.PlaybackSnapshot
import com.yaneodex.core.model.TrackRecord
import com.yaneodex.core.state.PlaybackVisualizerState
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import java.util.concurrent.CountDownLatch
import kotlin.math.pow

class JavaFxPlaybackBackend : PlaybackBackend {
    private var queue: List<TrackRecord> = emptyList()
    private var currentIndex: Int = -1
    private var mediaPlayer: MediaPlayer? = null
    private var volume: Float = 0.72f
    private var playbackSessionId: Long = 0L

    override fun playQueue(
        queue: List<TrackRecord>,
        startTrackId: String?,
        onState: (PlaybackSnapshot) -> Unit,
    ) {
        if (queue.isEmpty()) {
            onState(PlaybackSnapshot())
            return
        }
        this.queue = queue
        currentIndex = queue.indexOfFirst { it.id == startTrackId }.takeIf { it >= 0 } ?: 0
        playCurrent(onState)
    }

    override fun togglePlayPause(onState: (PlaybackSnapshot) -> Unit) {
        val player = mediaPlayer ?: return
        runOnFxThread {
            when (player.status) {
                MediaPlayer.Status.PLAYING -> {
                    player.pause()
                    onState(
                        PlaybackSnapshot(
                            currentTrackId = queue.getOrNull(currentIndex)?.id,
                            isPlaying = false,
                            visualizer = PlaybackVisualizerState.idle(),
                        positionMs = player.currentTime?.toMillis()?.toLong()?.coerceAtLeast(0L) ?: 0L,
                        durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong()
                            ?: queue.getOrNull(currentIndex)?.durationMs
                            ?: 0L,
                        volume = volume,
                    ),
                )
                }
                else -> {
                    player.play()
                    onState(
                        PlaybackSnapshot(
                            currentTrackId = queue.getOrNull(currentIndex)?.id,
                            isPlaying = true,
                        positionMs = player.currentTime?.toMillis()?.toLong()?.coerceAtLeast(0L) ?: 0L,
                        durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong()
                            ?: queue.getOrNull(currentIndex)?.durationMs
                            ?: 0L,
                        volume = volume,
                    ),
                )
                }
            }
        }
    }

    override fun playNext(onState: (PlaybackSnapshot) -> Unit) {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex + 1).coerceAtMost(queue.lastIndex)
        playCurrent(onState)
    }

    override fun playPrevious(onState: (PlaybackSnapshot) -> Unit) {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex - 1).coerceAtLeast(0)
        playCurrent(onState)
    }

    override fun seekTo(positionMs: Long, onState: (PlaybackSnapshot) -> Unit) {
        val player = mediaPlayer ?: return
        runOnFxThread {
            val durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong()
                ?: queue.getOrNull(currentIndex)?.durationMs
                ?: 0L
            val targetMs = positionMs.coerceIn(0L, durationMs.takeIf { it > 0 } ?: positionMs.coerceAtLeast(0L))
            player.seek(Duration.millis(targetMs.toDouble()))
            onState(
                PlaybackSnapshot(
                    currentTrackId = queue.getOrNull(currentIndex)?.id,
                    isPlaying = player.status == MediaPlayer.Status.PLAYING,
                    positionMs = targetMs,
                    durationMs = durationMs,
                    volume = volume,
                ),
            )
        }
    }

    override fun setVolume(volume: Float, onState: (PlaybackSnapshot) -> Unit) {
        this.volume = volume.coerceIn(0f, 1f)
        runOnFxThread {
            mediaPlayer?.volume = this.volume.toDouble()
            val player = mediaPlayer
            onState(
                PlaybackSnapshot(
                    currentTrackId = queue.getOrNull(currentIndex)?.id,
                    isPlaying = player?.status == MediaPlayer.Status.PLAYING,
                    visualizer = if (player?.status == MediaPlayer.Status.PLAYING) null else PlaybackVisualizerState.idle(),
                    positionMs = player?.currentTime?.toMillis()?.toLong()?.coerceAtLeast(0L) ?: 0L,
                    durationMs = player?.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong()
                        ?: queue.getOrNull(currentIndex)?.durationMs
                        ?: 0L,
                    volume = this.volume,
                ),
            )
        }
    }

    override fun stop() {
        runOnFxThread {
            playbackSessionId += 1L
            mediaPlayer?.stop()
            mediaPlayer?.dispose()
            mediaPlayer = null
        }
    }

    private fun playCurrent(onState: (PlaybackSnapshot) -> Unit) {
        val track = queue.getOrNull(currentIndex)
        if (track == null) {
            onState(PlaybackSnapshot())
            return
        }
        runOnFxThread {
            playbackSessionId += 1L
            val sessionId = playbackSessionId
            mediaPlayer?.stop()
            mediaPlayer?.dispose()
            val player = MediaPlayer(Media(track.uri))
            player.volume = volume.toDouble()
            player.audioSpectrumNumBands = 32
            player.audioSpectrumInterval = 0.05
            player.audioSpectrumThreshold = -70
            player.currentTimeProperty().addListener { _, _, currentTime ->
                if (sessionId != playbackSessionId || mediaPlayer !== player) return@addListener
                onState(
                    PlaybackSnapshot(
                        currentTrackId = track.id,
                        isPlaying = player.status == MediaPlayer.Status.PLAYING,
                        positionMs = currentTime.toMillis().toLong().coerceAtLeast(0L),
                        durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong() ?: track.durationMs,
                        volume = volume,
                    ),
                )
            }
            player.setAudioSpectrumListener { _, _, magnitudes, _ ->
                if (sessionId != playbackSessionId || mediaPlayer !== player) return@setAudioSpectrumListener
                val reactiveBands = magnitudes.mapIndexed { index, magnitude ->
                    val normalized = ((magnitude + 70f) / 70f).coerceIn(0f, 1f)
                    val bassBias = when {
                        index <= 3 -> 1.7f
                        index <= 7 -> 1.35f
                        else -> 1f - ((index - 8).coerceAtLeast(0) * 0.01f)
                    }
                    (normalized.toDouble().pow(0.72).toFloat() * bassBias).coerceIn(0f, 1f)
                }
                val peak = reactiveBands.maxOrNull() ?: 0f
                val average = reactiveBands.average().toFloat()
                val intensity = (average * 0.55f + peak * 0.45f).coerceIn(0f, 1f)
                onState(
                    PlaybackSnapshot(
                        currentTrackId = track.id,
                        isPlaying = true,
                        visualizer = PlaybackVisualizerState(
                            bands = reactiveBands.map { band ->
                                val climaxBoost = 0.82f + intensity * 0.45f
                                (band * climaxBoost).coerceIn(0f, 1f)
                            },
                            intensity = intensity,
                            active = true,
                        ),
                        positionMs = player.currentTime?.toMillis()?.toLong()?.coerceAtLeast(0L) ?: 0L,
                        durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong() ?: track.durationMs,
                        volume = volume,
                    ),
                )
            }
            mediaPlayer = player
            player.setOnReady {
                if (sessionId != playbackSessionId || mediaPlayer !== player) return@setOnReady
                player.play()
                onState(
                    PlaybackSnapshot(
                        currentTrackId = track.id,
                        isPlaying = true,
                        visualizer = PlaybackVisualizerState.idle(32).copy(active = true),
                        positionMs = 0L,
                        durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong() ?: track.durationMs,
                        volume = volume,
                    ),
                )
            }
            player.setOnEndOfMedia {
                if (sessionId != playbackSessionId || mediaPlayer !== player) return@setOnEndOfMedia
                if (currentIndex < queue.lastIndex) {
                    currentIndex += 1
                    playCurrent(onState)
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
                if (sessionId != playbackSessionId || mediaPlayer !== player) return@setOnError
                onState(
                    PlaybackSnapshot(
                        currentTrackId = track.id,
                        isPlaying = false,
                        errorMessage = player.error?.message ?: "Playback failed.",
                        visualizer = PlaybackVisualizerState.idle(32),
                        positionMs = player.currentTime?.toMillis()?.toLong()?.coerceAtLeast(0L) ?: 0L,
                        durationMs = player.totalDuration?.toMillis()?.takeIf { it.isFinite() && it > 0 }?.toLong() ?: track.durationMs,
                        volume = volume,
                    ),
                )
            }
        }
    }

    private fun ensureStarted() {
        JavaFxRuntime.ensureInitialized()
    }

    private fun runOnFxThread(block: () -> Unit) {
        ensureStarted()
        if (Platform.isFxApplicationThread()) {
            block()
            return
        }
        val latch = CountDownLatch(1)
        Platform.runLater {
            try {
                block()
            } finally {
                latch.countDown()
            }
        }
        latch.await()
    }
}
