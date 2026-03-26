package com.yaneodex.desktop.app

import com.yaneodex.core.contracts.LibraryRepository
import com.yaneodex.core.contracts.MusicSourceCatalog
import com.yaneodex.core.contracts.PlaybackBackend
import com.yaneodex.core.contracts.PlaybackSnapshot
import com.yaneodex.core.contracts.StoredLibraryState
import com.yaneodex.core.model.DownloadBlueprint
import com.yaneodex.core.model.LibrarySnapshot
import com.yaneodex.core.model.PlaylistRecord
import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.model.SourceDescriptor
import com.yaneodex.core.model.TrackRecord
import com.yaneodex.core.state.PlaybackVisualizerState
import com.yaneodex.desktop.integration.DesktopConfig
import com.yaneodex.desktop.integration.DesktopDownloadManager
import com.yaneodex.desktop.integration.DesktopPersistence
import com.yaneodex.desktop.integration.WindowsOcrClient
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopControllerTest {
    @Test
    fun `playback snapshots update visualizer state`() {
        val backend = FakePlaybackBackend()
        val controller = controller(backend = backend)
        val trackId = controller.state.value.snapshot.tracks.first().id

        controller.playTrack(trackId)
        backend.emit(
            PlaybackSnapshot(
                currentTrackId = trackId,
                isPlaying = true,
                visualizer = PlaybackVisualizerState(
                    bands = listOf(0.2f, 0.6f, 0.9f, 0.4f),
                    intensity = 0.7f,
                    active = true,
                ),
            ),
        )

        assertTrue(controller.state.value.visualizer.active)
        assertTrue(controller.state.value.visualizer.bands.size >= 4)
        assertTrue(controller.state.value.visualizer.intensity > 0.3f)
    }

    @Test
    fun `paused playback decays visualizer`() {
        val backend = FakePlaybackBackend()
        val controller = controller(backend = backend)
        val trackId = controller.state.value.snapshot.tracks.first().id

        controller.playTrack(trackId)
        backend.emit(
            PlaybackSnapshot(
                currentTrackId = trackId,
                isPlaying = true,
                visualizer = PlaybackVisualizerState(
                    bands = listOf(1f, 0.8f, 0.6f, 0.4f),
                    intensity = 0.9f,
                    active = true,
                ),
            ),
        )
        backend.emit(
            PlaybackSnapshot(
                currentTrackId = trackId,
                isPlaying = false,
                visualizer = PlaybackVisualizerState.idle(4),
            ),
        )

        assertFalse(controller.state.value.isPlaying)
        assertFalse(controller.state.value.visualizer.active)
    }

    @Test
    fun `parser preview shares playback visualizer pipeline`() {
        val backend = FakePlaybackBackend()
        val controller = controller(
            backend = backend,
            sourceCatalog = FakeMusicSourceCatalog(),
        )

        controller.previewParserResult(
            RemoteTrackCandidate(
                sourceId = "fake",
                title = "Preview Song",
                artist = "Preview Artist",
                detailUrl = "https://example.test/detail",
            ),
        )

        val previewTrackId = waitForStartedTrackId(backend)
        backend.emit(
            PlaybackSnapshot(
                currentTrackId = previewTrackId,
                isPlaying = true,
                visualizer = PlaybackVisualizerState(
                    bands = listOf(0.3f, 0.5f, 0.7f, 0.9f),
                    intensity = 0.6f,
                    active = true,
                ),
            ),
        )

        assertEquals(previewTrackId, controller.state.value.currentTrackId)
        assertTrue(controller.state.value.visualizer.active)
        assertEquals("Preview Song", controller.state.value.currentTrack?.title)
    }

    @Test
    fun `seek updates playback progress`() {
        val backend = FakePlaybackBackend()
        val controller = controller(backend = backend)
        val trackId = controller.state.value.snapshot.tracks.first().id

        controller.playTrack(trackId)
        controller.seekPlayback(42_000L)
        backend.emit(
            PlaybackSnapshot(
                currentTrackId = trackId,
                isPlaying = true,
                positionMs = 42_000L,
                durationMs = 180_000L,
            ),
        )

        assertEquals(42_000L, backend.lastSeekPositionMs)
        assertEquals(42_000L, controller.state.value.playbackPositionMs)
        assertEquals(180_000L, controller.state.value.playbackDurationMs)
    }

    private fun controller(
        backend: FakePlaybackBackend,
        sourceCatalog: MusicSourceCatalog = FakeMusicSourceCatalog(),
    ): DesktopController {
        val stateFile = File.createTempFile("yaneodex-controller", ".json")
        stateFile.deleteOnExit()
        return DesktopController(
            config = DesktopConfig(null, null, null, null),
            libraryRepository = FakeLibraryRepository(),
            sourceCatalog = sourceCatalog,
            ocrClient = WindowsOcrClient(),
            persistence = DesktopPersistence(stateFile),
            playbackBackend = backend,
            downloadManager = DesktopDownloadManager(),
        )
    }

    private fun waitForStartedTrackId(backend: FakePlaybackBackend): String {
        repeat(50) {
            backend.startedTrackId?.let { return it }
            Thread.sleep(20)
        }
        error("Track id was not set in time")
    }
}

private class FakePlaybackBackend : PlaybackBackend {
    private var callback: ((PlaybackSnapshot) -> Unit)? = null
    var startedTrackId: String? = null
        private set
    var lastSeekPositionMs: Long? = null
        private set

    override fun playQueue(queue: List<TrackRecord>, startTrackId: String?, onState: (PlaybackSnapshot) -> Unit) {
        callback = onState
        startedTrackId = startTrackId
    }

    override fun togglePlayPause(onState: (PlaybackSnapshot) -> Unit) {
        callback = onState
    }

    override fun playNext(onState: (PlaybackSnapshot) -> Unit) {
        callback = onState
    }

    override fun playPrevious(onState: (PlaybackSnapshot) -> Unit) {
        callback = onState
    }

    override fun seekTo(positionMs: Long, onState: (PlaybackSnapshot) -> Unit) {
        callback = onState
        lastSeekPositionMs = positionMs
    }

    override fun stop() = Unit

    fun emit(snapshot: PlaybackSnapshot) {
        callback?.invoke(snapshot)
    }
}

private class FakeMusicSourceCatalog : MusicSourceCatalog {
    override val descriptors: List<SourceDescriptor> = listOf(
        SourceDescriptor("fake", "Fake", "Active", "", true),
    )

    override suspend fun search(query: String): List<RemoteTrackCandidate> = emptyList()

    override suspend fun resolve(track: RemoteTrackCandidate): DownloadBlueprint {
        return DownloadBlueprint(
            title = track.title,
            artist = track.artist,
            url = "https://example.test/${track.title}.mp3",
            suggestedFilename = "${track.artist} - ${track.title}.mp3",
        )
    }
}

private class FakeLibraryRepository : LibraryRepository {
    private var roots: List<String> = listOf("C:\\Music")
    private var snapshot: LibrarySnapshot = LibrarySnapshot(
        tracks = listOf(
            TrackRecord(
                id = "track-1",
                uri = "file:///C:/Music/track-1.mp3",
                sourceUri = "C:\\Music\\track-1.mp3",
                title = "Track 1",
                artist = "Artist 1",
                durationMs = 180_000,
                importedAtEpochMs = 1_700_000_000_000L,
            ),
        ),
        playlists = listOf(
            PlaylistRecord("library-all", "All Tracks", "AT", "#95F15A", "", listOf("track-1"), 1_700_000_000_000L),
            PlaylistRecord("playlist-1", "Inbox", "IN", "#E7C669", "", emptyList(), 1_700_000_000_001L),
        ),
    )

    override fun load(): StoredLibraryState = StoredLibraryState(roots, snapshot)

    override fun importRoots(paths: List<String>): StoredLibraryState {
        roots = (roots + paths).distinct()
        return StoredLibraryState(roots, snapshot)
    }

    override fun refresh(): StoredLibraryState = StoredLibraryState(roots, snapshot)

    override fun createPlaylist(name: String): StoredLibraryState {
        snapshot = snapshot.copy(
            playlists = snapshot.playlists + PlaylistRecord("playlist-${snapshot.playlists.size}", name, "PL", "#7CC8FF", "", emptyList(), 1_700_000_000_100L),
        )
        return StoredLibraryState(roots, snapshot)
    }

    override fun renamePlaylist(playlistId: String, name: String): StoredLibraryState {
        snapshot = snapshot.copy(
            playlists = snapshot.playlists.map { if (it.id == playlistId) it.copy(name = name) else it },
        )
        return StoredLibraryState(roots, snapshot)
    }

    override fun addTrackToPlaylist(trackId: String, playlistId: String): StoredLibraryState {
        snapshot = snapshot.copy(
            playlists = snapshot.playlists.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(trackIds = (playlist.trackIds + trackId).distinct()) else playlist
            },
        )
        return StoredLibraryState(roots, snapshot)
    }

    override fun removeTrackFromPlaylist(trackId: String, playlistId: String): StoredLibraryState {
        snapshot = snapshot.copy(
            playlists = snapshot.playlists.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(trackIds = playlist.trackIds.filterNot { it == trackId }) else playlist
            },
        )
        return StoredLibraryState(roots, snapshot)
    }
}
