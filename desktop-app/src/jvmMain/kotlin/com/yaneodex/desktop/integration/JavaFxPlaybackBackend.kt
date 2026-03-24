package com.yaneodex.desktop.integration

import com.yaneodex.core.contracts.PlaybackBackend
import com.yaneodex.core.contracts.PlaybackSnapshot
import com.yaneodex.core.model.TrackRecord
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class JavaFxPlaybackBackend : PlaybackBackend {
    private var queue: List<TrackRecord> = emptyList()
    private var currentIndex: Int = -1
    private var mediaPlayer: MediaPlayer? = null
    private val started = AtomicBoolean(false)

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
                MediaPlayer.Status.PLAYING -> player.pause()
                else -> player.play()
            }
            onState(PlaybackSnapshot(currentTrackId = queue.getOrNull(currentIndex)?.id, isPlaying = player.status != MediaPlayer.Status.PAUSED))
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

    override fun stop() {
        runOnFxThread {
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
            mediaPlayer?.stop()
            mediaPlayer?.dispose()
            val media = Media(track.uri)
            val player = MediaPlayer(media)
            mediaPlayer = player
            player.setOnReady {
                player.play()
                onState(PlaybackSnapshot(currentTrackId = track.id, isPlaying = true))
            }
            player.setOnEndOfMedia {
                if (currentIndex < queue.lastIndex) {
                    currentIndex += 1
                    playCurrent(onState)
                } else {
                    onState(PlaybackSnapshot(currentTrackId = track.id, isPlaying = false))
                }
            }
            player.setOnError {
                onState(PlaybackSnapshot(currentTrackId = track.id, isPlaying = false, errorMessage = player.error?.message ?: "Playback failed."))
            }
        }
    }

    private fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            JFXPanel()
        }
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
