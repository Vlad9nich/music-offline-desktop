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
import com.yaneodex.desktop.integration.DesktopPersistence
import com.yaneodex.desktop.integration.DownloadManager
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopControllerTest {
    @Test
    fun `bulk actions update library and playlist state`() {
        val backend = FakePlaybackBackend()
        val controller = controller(backend = backend)

        controller.addTracksToSelectedPlaylist(listOf("track-1"))
        waitForCondition {
            controller.state.value.snapshot.playlists
                .firstOrNull { it.id == "playlist-1" }
                ?.trackIds
                ?.contains("track-1")
                ?.takeIf { it }
        }
        assertTrue(controller.state.value.snapshot.playlists.first { it.id == "playlist-1" }.trackIds.contains("track-1"))

        controller.removeTracksFromSelectedPlaylist(listOf("track-1"))
        waitForCondition {
            controller.state.value.snapshot.playlists
                .firstOrNull { it.id == "playlist-1" }
                ?.trackIds
                ?.contains("track-1")
                ?.takeIf { present -> !present }
        }
        assertTrue(controller.state.value.snapshot.playlists.first { it.id == "playlist-1" }.trackIds.none { it == "track-1" })

        controller.deleteTracksFromLibrary(listOf("track-1"))
        waitForCondition { controller.state.value.snapshot.tracks.none { it.id == "track-1" }.takeIf { it } }
        assertTrue(controller.state.value.snapshot.tracks.none { it.id == "track-1" })
    }

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

    @Test
    fun `download parser result reports saved filename instead of raw path`() {
        val backend = FakePlaybackBackend()
        val controller = controller(
            backend = backend,
            sourceCatalog = FakeMusicSourceCatalog(),
            downloadManager = FakeDownloadManager(),
        )

        controller.downloadParserResult(
            RemoteTrackCandidate(
                sourceId = "fake",
                title = "Preview Song",
                artist = "Preview Artist",
                detailUrl = "https://example.test/detail",
            ),
        )

        waitForStatus(controller) { it.startsWith("Скачано:") }
        assertEquals("Скачано: Preview Artist - Preview Song.mp3", controller.state.value.parserStatus)
    }

    @Test
    fun `toggling shuffle pushes the new order to the player in place`() {
        val backend = FakePlaybackBackend()
        val controller = controller(backend = backend, libraryRepository = FakeLibraryRepository(extraTracks = 11))
        val trackId = controller.state.value.snapshot.tracks.first().id

        controller.playTrack(trackId)
        val playingOrder = backend.queue
        assertFalse(backend.lastQueueSwapWasInPlace)

        controller.toggleShuffle()

        assertTrue(controller.state.value.shuffleEnabled)
        // The player must actually receive the new order, and without restarting the track.
        assertTrue(backend.lastQueueSwapWasInPlace)
        assertEquals(controller.state.value.playbackQueue.map { it.id }, backend.queue)
        assertEquals(playingOrder.toSet(), backend.queue.toSet())
        assertTrue(playingOrder != backend.queue, "shuffle should change the order for a 12 track library")

        controller.toggleShuffle()

        assertFalse(controller.state.value.shuffleEnabled)
        assertEquals(playingOrder, backend.queue)
    }

    @Test
    fun `queue exhaustion reshuffles instead of stopping on the last track`() {
        val backend = FakePlaybackBackend()
        val controller = controller(backend = backend, libraryRepository = FakeLibraryRepository(extraTracks = 5))
        val trackId = controller.state.value.snapshot.tracks.first().id

        controller.playTrack(trackId)
        controller.toggleShuffle()

        backend.emit(
            PlaybackSnapshot(
                currentTrackId = trackId,
                isPlaying = false,
                queueExhausted = true,
                positionMs = 180_000L,
                durationMs = 180_000L,
            ),
        )

        assertTrue(controller.state.value.isPlaying, "playback should continue after the queue runs out")
        assertEquals(0L, controller.state.value.playbackPositionMs)
        assertEquals(controller.state.value.playbackQueue.map { it.id }, backend.queue)
        assertTrue(backend.queue.size == controller.state.value.snapshot.tracks.size)
    }

    @Test
    fun `single track queue stops cleanly when it ends`() {
        val backend = FakePlaybackBackend()
        val controller = controller(backend = backend, libraryRepository = FakeLibraryRepository(extraTracks = 0))
        val trackId = controller.state.value.snapshot.tracks.first().id

        controller.playTrack(trackId)
        backend.emit(
            PlaybackSnapshot(
                currentTrackId = trackId,
                isPlaying = false,
                queueExhausted = true,
            ),
        )

        assertFalse(controller.state.value.isPlaying)
    }

    private fun controller(
        backend: FakePlaybackBackend,
        sourceCatalog: MusicSourceCatalog = FakeMusicSourceCatalog(),
        downloadManager: DownloadManager = FakeDownloadManager(),
        libraryRepository: FakeLibraryRepository = FakeLibraryRepository(),
    ): DesktopController {
        val stateFile = File.createTempFile("yaneodex-controller", ".json")
        stateFile.deleteOnExit()
        return DesktopController(
            config = DesktopConfig(null, null),
            libraryRepository = libraryRepository,
            sourceCatalog = sourceCatalog,
            persistence = DesktopPersistence(stateFile),
            playbackBackend = backend,
            downloadManager = downloadManager,
        )
    }

    private fun waitForStartedTrackId(backend: FakePlaybackBackend): String =
        waitForCondition { backend.startedTrackId }

    private fun waitForStatus(controller: DesktopController, predicate: (String) -> Boolean): String =
        waitForCondition { controller.state.value.parserStatus.takeIf(predicate) }

    private fun <T> waitForCondition(check: () -> T?): T {
        repeat(50) {
            check()?.let { return it }
            Thread.sleep(20)
        }
        error("Condition was not satisfied in time")
    }
}

private class FakePlaybackBackend : PlaybackBackend {
    private var callback: ((PlaybackSnapshot) -> Unit)? = null
    var startedTrackId: String? = null
        private set
    var lastSeekPositionMs: Long? = null
        private set
    var lastVolume: Float = 0.72f
        private set

    /** Order last handed to the player, either by playQueue or setQueue. */
    var queue: List<String> = emptyList()
        private set

    /** True when the order arrived via setQueue (i.e. without restarting playback). */
    var lastQueueSwapWasInPlace: Boolean = false
        private set

    override fun playQueue(queue: List<TrackRecord>, startTrackId: String?, onState: (PlaybackSnapshot) -> Unit) {
        callback = onState
        startedTrackId = startTrackId
        this.queue = queue.map { it.id }
        lastQueueSwapWasInPlace = false
    }

    override fun setQueue(queue: List<TrackRecord>, onState: (PlaybackSnapshot) -> Unit) {
        callback = onState
        this.queue = queue.map { it.id }
        lastQueueSwapWasInPlace = true
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

    override fun setVolume(volume: Float, onState: (PlaybackSnapshot) -> Unit) {
        callback = onState
        lastVolume = volume
    }

    override fun stop() = Unit

    fun emit(snapshot: PlaybackSnapshot) {
        callback?.invoke(snapshot)
    }
}

private class FakeDownloadManager : DownloadManager {
    override fun download(blueprint: DownloadBlueprint, targetDirectory: File): File {
        return File(targetDirectory, blueprint.suggestedFilename)
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

private class FakeLibraryRepository(extraTracks: Int = 0) : LibraryRepository {
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
        ) + List(extraTracks) { index ->
            // Two tracks per artist so shuffle has something to spread.
            val trackNumber = index + 2
            TrackRecord(
                id = "track-$trackNumber",
                uri = "file:///C:/Music/track-$trackNumber.mp3",
                sourceUri = "C:\\Music\\track-$trackNumber.mp3",
                title = "Track $trackNumber",
                artist = "Artist ${(index / 2) + 2}",
                durationMs = 180_000,
                importedAtEpochMs = 1_700_000_000_000L + trackNumber,
            )
        },
        playlists = listOf(
            PlaylistRecord(
                "library-all",
                "All Tracks",
                "AT",
                "#95F15A",
                "",
                listOf("track-1") + List(extraTracks) { "track-${it + 2}" },
                1_700_000_000_000L,
            ),
            PlaylistRecord("playlist-1", "Inbox", "IN", "#E7C669", "", emptyList(), 1_700_000_000_001L),
        ),
    )

    override fun load(): StoredLibraryState = StoredLibraryState(roots, snapshot)

    override fun importRoots(paths: List<String>): StoredLibraryState {
        roots = (roots + paths).distinct()
        return StoredLibraryState(roots, snapshot)
    }

    override fun refresh(): StoredLibraryState = StoredLibraryState(roots, snapshot)

    override fun createPlaylist(name: String, artworkHint: String): StoredLibraryState {
        snapshot = snapshot.copy(
            playlists = snapshot.playlists + PlaylistRecord("playlist-${snapshot.playlists.size}", name, artworkHint.ifBlank { "PL" }, "#7CC8FF", "", emptyList(), 1_700_000_000_100L),
        )
        return StoredLibraryState(roots, snapshot)
    }

    override fun renamePlaylist(playlistId: String, name: String, artworkHint: String): StoredLibraryState {
        snapshot = snapshot.copy(
            playlists = snapshot.playlists.map {
                if (it.id == playlistId) it.copy(name = name, artworkHint = artworkHint.ifBlank { it.artworkHint }) else it
            },
        )
        return StoredLibraryState(roots, snapshot)
    }

    override fun addTrackToPlaylist(trackId: String, playlistId: String): StoredLibraryState {
        return addTracksToPlaylist(listOf(trackId), playlistId)
    }

    override fun addTracksToPlaylist(trackIds: List<String>, playlistId: String): StoredLibraryState {
        snapshot = snapshot.copy(
            playlists = snapshot.playlists.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(trackIds = (playlist.trackIds + trackIds).distinct()) else playlist
            },
        )
        return StoredLibraryState(roots, snapshot)
    }

    override fun removeTrackFromPlaylist(trackId: String, playlistId: String): StoredLibraryState {
        return removeTracksFromPlaylist(listOf(trackId), playlistId)
    }

    override fun removeTracksFromPlaylist(trackIds: List<String>, playlistId: String): StoredLibraryState {
        snapshot = snapshot.copy(
            playlists = snapshot.playlists.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(trackIds = playlist.trackIds.filterNot { it in trackIds.toSet() }) else playlist
            },
        )
        return StoredLibraryState(roots, snapshot)
    }

    override fun removeTracksFromLibrary(trackIds: List<String>): StoredLibraryState {
        val removed = trackIds.toSet()
        snapshot = snapshot.copy(
            tracks = snapshot.tracks.filterNot { it.id in removed },
            playlists = snapshot.playlists.map { playlist ->
                playlist.copy(trackIds = playlist.trackIds.filterNot { it in removed })
            },
        )
        return StoredLibraryState(roots, snapshot)
    }
}
