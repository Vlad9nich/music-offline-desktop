package com.yaneodex.desktop.app

import com.yaneodex.core.contracts.LibraryRepository
import com.yaneodex.core.contracts.MusicSourceCatalog
import com.yaneodex.core.contracts.OcrJobResult
import com.yaneodex.core.contracts.PlaybackBackend
import com.yaneodex.core.contracts.PlaybackSnapshot
import com.yaneodex.core.importer.MatchedTrackCandidate
import com.yaneodex.core.importer.ScreenshotImportMatcher
import com.yaneodex.core.model.LibrarySnapshot
import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.playback.buildPlaybackQueue
import com.yaneodex.core.state.DemoLibrary
import com.yaneodex.core.state.DesktopSection
import com.yaneodex.core.state.DesktopUiState
import com.yaneodex.core.state.OcrSettings
import com.yaneodex.core.state.SpotlightCard
import com.yaneodex.desktop.integration.DesktopConfig
import com.yaneodex.desktop.integration.DesktopLibraryRepository
import com.yaneodex.desktop.integration.DesktopMusicSourceCatalog
import com.yaneodex.desktop.integration.DesktopPersistence
import com.yaneodex.desktop.integration.JavaFxPlaybackBackend
import com.yaneodex.desktop.integration.WindowsOcrClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class DesktopController(
    private val random: Random = Random(42),
    private val config: DesktopConfig = DesktopConfig.load(),
    private val libraryRepository: LibraryRepository = DesktopLibraryRepository(
        configuredDefaultRoots = listOfNotNull(config.libraryPath?.takeIf { it.isNotBlank() }),
    ),
    private val sourceCatalog: MusicSourceCatalog = DesktopMusicSourceCatalog(),
    private val ocrClient: WindowsOcrClient = WindowsOcrClient(),
    private val persistence: DesktopPersistence = DesktopPersistence(),
    private val playbackBackend: PlaybackBackend = JavaFxPlaybackBackend(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<DesktopUiState> = _state.asStateFlow()

    fun selectSection(section: DesktopSection) {
        mutate { it.copy(selectedSection = section) }
    }

    fun selectPlaylist(playlistId: String) {
        mutate { current ->
            val playlist = current.snapshot.playlists.firstOrNull { it.id == playlistId }
            if (playlist == null) {
                current
            } else {
                val queue = playlist.trackIds.mapNotNull { id -> current.snapshot.tracks.firstOrNull { it.id == id } }
                current.copy(
                    selectedPlaylistId = playlistId,
                    selectedSection = DesktopSection.PLAYLISTS,
                    currentTrackId = queue.firstOrNull()?.id ?: current.currentTrackId,
                    playbackQueue = buildPlaybackQueue(queue.ifEmpty { current.snapshot.tracks }, queue.firstOrNull()?.id, current.shuffleEnabled, random),
                    spotlight = SpotlightCard("Playlist focus", playlist.name, playlist.description, playlist.tone),
                )
            }
        }
    }

    fun createPlaylist(name: String) {
        scope.launch {
            runCatching { libraryRepository.createPlaylist(name) }
                .onSuccess { stored ->
                    mutate { current ->
                        rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(
                            selectedSection = DesktopSection.PLAYLISTS,
                            libraryStatus = "Playlist created: ${name.trim().ifBlank { "New Playlist" }}",
                        )
                    }
                }
                .onFailure { error ->
                    mutate { it.copy(libraryStatus = error.message ?: "Failed to create playlist.") }
                }
        }
    }

    fun renameSelectedPlaylist(name: String) {
        val playlistId = state.value.selectedPlaylistId
        if (playlistId.isBlank()) return
        scope.launch {
            runCatching { libraryRepository.renamePlaylist(playlistId, name) }
                .onSuccess { stored ->
                    mutate { current ->
                        rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(
                            libraryStatus = "Playlist renamed.",
                        )
                    }
                }
                .onFailure { error ->
                    mutate { it.copy(libraryStatus = error.message ?: "Failed to rename playlist.") }
                }
        }
    }

    fun addTrackToSelectedPlaylist(trackId: String) {
        val playlistId = state.value.selectedPlaylistId
        if (playlistId.isBlank()) return
        scope.launch {
            runCatching { libraryRepository.addTrackToPlaylist(trackId, playlistId) }
                .onSuccess { stored ->
                    mutate { current ->
                        rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(libraryStatus = "Track added to playlist.")
                    }
                }
                .onFailure { error ->
                    mutate { it.copy(libraryStatus = error.message ?: "Failed to add track to playlist.") }
                }
        }
    }

    fun removeTrackFromSelectedPlaylist(trackId: String) {
        val playlistId = state.value.selectedPlaylistId
        if (playlistId.isBlank()) return
        scope.launch {
            runCatching { libraryRepository.removeTrackFromPlaylist(trackId, playlistId) }
                .onSuccess { stored ->
                    mutate { current ->
                        rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(libraryStatus = "Track removed from playlist.")
                    }
                }
                .onFailure { error ->
                    mutate { it.copy(libraryStatus = error.message ?: "Failed to remove track from playlist.") }
                }
        }
    }

    fun playTrack(trackId: String) {
        mutate { current ->
            val source = when (current.selectedSection) {
                DesktopSection.PLAYLISTS -> current.selectedPlaylistTracks.ifEmpty { current.snapshot.tracks }
                DesktopSection.SEARCH -> current.filteredTracks.ifEmpty { current.snapshot.tracks }
                else -> current.snapshot.tracks
            }
            current.copy(
                currentTrackId = trackId,
                playbackQueue = buildPlaybackQueue(source, trackId, current.shuffleEnabled, random),
            )
        }
        playbackBackend.playQueue(state.value.playbackQueue, trackId, ::syncPlaybackState)
    }

    fun playCurrentPlaylist() {
        mutate { current ->
            val queue = current.selectedPlaylistTracks.ifEmpty { current.snapshot.tracks }
            current.copy(
                currentTrackId = queue.firstOrNull()?.id,
                playbackQueue = buildPlaybackQueue(queue, queue.firstOrNull()?.id, current.shuffleEnabled, random),
            )
        }
        playbackBackend.playQueue(state.value.playbackQueue, state.value.currentTrackId, ::syncPlaybackState)
    }

    fun toggleShuffle() {
        mutate { current ->
            val nextShuffle = !current.shuffleEnabled
            val source = when (current.selectedSection) {
                DesktopSection.PLAYLISTS -> current.selectedPlaylistTracks.ifEmpty { current.snapshot.tracks }
                else -> current.snapshot.tracks
            }
            current.copy(
                shuffleEnabled = nextShuffle,
                playbackQueue = buildPlaybackQueue(source, current.currentTrackId, nextShuffle, random),
            )
        }
    }

    fun togglePlayPause() {
        playbackBackend.togglePlayPause(::syncPlaybackState)
    }

    fun playNext() {
        playbackBackend.playNext(::syncPlaybackState)
    }

    fun playPrevious() {
        playbackBackend.playPrevious(::syncPlaybackState)
    }

    fun updateSearchQuery(query: String) {
        mutate { it.copy(searchQuery = query, selectedSection = DesktopSection.SEARCH) }
    }

    fun activateTag(tag: String) {
        mutate {
            it.copy(
                highlightedTag = tag,
                spotlight = SpotlightCard("Mood selected", tag, "Use this lane for adaptive desktop mixes and recommendation rails.", it.selectedPlaylist?.tone ?: "#95F15A"),
            )
        }
    }

    fun searchParser(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            mutate { it.copy(parserResults = emptyList(), parserStatus = "Enter a parser query first.", parserLoading = false) }
            return
        }

        mutate { it.copy(selectedSection = DesktopSection.SEARCH, parserLoading = true, parserStatus = "Searching parser sources...") }
        scope.launch {
            runCatching { sourceCatalog.search(normalized) }
                .onSuccess { results ->
                    mutate {
                        it.copy(
                            parserResults = results,
                            parserLoading = false,
                            parserStatus = if (results.isEmpty()) "Nothing found for \"$normalized\"." else "Found ${results.size} parser candidates.",
                        )
                    }
                }
                .onFailure { error ->
                    mutate { it.copy(parserLoading = false, parserStatus = error.message ?: "Parser search failed.") }
                }
        }
    }

    fun applyParserResult(track: RemoteTrackCandidate) {
        mutate { it.copy(parserStatus = "Resolving direct media URL for ${track.title}...") }
        scope.launch {
            runCatching { sourceCatalog.resolve(track) }
                .onSuccess { blueprint ->
                    mutate { current ->
                        current.copy(
                            spotlight = SpotlightCard("Parser candidate", blueprint.title, "${blueprint.artist} ready from ${track.sourceId}", "#7CC8FF"),
                            parserStatus = "Resolved download URL: ${blueprint.url.take(96)}",
                            selectedSection = DesktopSection.SEARCH,
                        )
                    }
                }
                .onFailure { error ->
                    mutate { it.copy(parserStatus = error.message ?: "Parser resolve failed.") }
                }
        }
    }

    fun updateOcrSettings(serverUrl: String? = null, authToken: String? = null) {
        mutate {
            it.copy(
                ocrSettings = OcrSettings(
                    serverUrl = serverUrl ?: it.ocrSettings.serverUrl,
                    authToken = authToken ?: it.ocrSettings.authToken,
                ),
            )
        }
    }

    fun importLibraryFolders(paths: List<String>) {
        if (paths.isEmpty()) return
        scope.launch {
            runCatching { libraryRepository.importRoots(paths) }
                .onSuccess { stored ->
                    mutate { current ->
                        val rebuilt = rebuildSnapshotState(current, stored.snapshot, stored.roots)
                        rebuilt.copy(libraryStatus = "Imported ${stored.snapshot.tracks.size} local tracks from ${stored.roots.size} root(s).")
                    }
                }
                .onFailure { error ->
                    mutate { it.copy(libraryStatus = error.message ?: "Library import failed.") }
                }
        }
    }

    fun refreshLibrary() {
        scope.launch {
            runCatching { libraryRepository.refresh() }
                .onSuccess { stored ->
                    mutate { current ->
                        rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(
                            libraryStatus = if (stored.snapshot.tracks.isEmpty()) {
                                "No local audio files found in configured roots."
                            } else {
                                "Library refreshed: ${stored.snapshot.tracks.size} local tracks."
                            },
                        )
                    }
                }
                .onFailure { error ->
                    mutate { it.copy(libraryStatus = error.message ?: "Library refresh failed.") }
                }
        }
    }

    fun analyzeScreenshots(files: List<File>) {
        if (files.isEmpty()) return
        val settings = state.value.ocrSettings
        if (settings.serverUrl.isBlank()) {
            mutate { it.copy(selectedSection = DesktopSection.IMPORT, ocrStatus = "Set OCR server URL first.") }
            return
        }

        mutate { it.copy(selectedSection = DesktopSection.IMPORT, ocrLoading = true, ocrStatus = "Uploading screenshots to OCR service...") }
        scope.launch {
            runCatching {
                val response = if (files.size == 1) {
                    ocrClient.submitSingleImageFile(settings.serverUrl, settings.authToken, files.first())
                } else {
                    val job = ocrClient.submitJobFiles(settings.serverUrl, settings.authToken, files)
                    ocrClient.pollJob(settings.serverUrl, settings.authToken, job.jobId)
                }
                buildImportMatches(response)
            }.onSuccess { matches ->
                mutate {
                    it.copy(
                        importMatches = matches,
                        ocrLoading = false,
                        ocrStatus = "OCR completed. Matches: ${matches.count { match -> match.bestMatch != null }}.",
                    )
                }
            }.onFailure { error ->
                mutate { it.copy(ocrLoading = false, ocrStatus = error.message ?: "OCR import failed.") }
            }
        }
    }

    private fun buildImportMatches(response: OcrJobResult): List<MatchedTrackCandidate> {
        val remoteCandidates = state.value.snapshot.tracks.map { track ->
            RemoteTrackCandidate(
                sourceId = "local-library",
                title = track.title,
                artist = track.artist,
                detailUrl = track.sourceUri,
                downloadUrl = track.uri,
            )
        }
        return ScreenshotImportMatcher
            .deduplicate(response.candidates)
            .map { candidate ->
                ScreenshotImportMatcher.pickBestMatch(candidate, remoteCandidates, state.value.selectedPlaylistTracks)
            }
    }

    private fun mutate(transform: (DesktopUiState) -> DesktopUiState) {
        _state.update { current ->
            val updated = transform(current)
            persistence.save(updated)
            updated
        }
    }

    private fun buildInitialState(): DesktopUiState {
        val storedLibrary = libraryRepository.load()
        val base = DemoLibrary.initialState(random).copy(
            snapshot = storedLibrary.snapshot,
            libraryRoots = storedLibrary.roots,
            libraryStatus = if (storedLibrary.snapshot.tracks.isEmpty()) {
                "No local audio files found. Add your Windows music folders."
            } else {
                "Loaded ${storedLibrary.snapshot.tracks.size} local tracks from disk."
            },
            selectedPlaylistId = storedLibrary.snapshot.playlists.firstOrNull()?.id ?: "library-all",
            currentTrackId = storedLibrary.snapshot.tracks.firstOrNull()?.id,
            playbackQueue = buildPlaybackQueue(storedLibrary.snapshot.tracks, storedLibrary.snapshot.tracks.firstOrNull()?.id, false, random),
            ocrSettings = OcrSettings(
                serverUrl = config.ocrBaseUrl.orEmpty(),
                authToken = config.ocrToken.orEmpty(),
            ),
        )
        return persistence.apply(base, persistence.load())
    }

    private fun rebuildSnapshotState(current: DesktopUiState, snapshot: LibrarySnapshot, roots: List<String>): DesktopUiState {
        val selectedPlaylistId = snapshot.playlists.firstOrNull { it.id == current.selectedPlaylistId }?.id
            ?: snapshot.playlists.firstOrNull()?.id.orEmpty()
        val currentTrackId = snapshot.tracks.firstOrNull { it.id == current.currentTrackId }?.id
            ?: snapshot.tracks.firstOrNull()?.id
        val selectedPlaylistTracks = snapshot.playlists.firstOrNull { it.id == selectedPlaylistId }?.trackIds
            ?.mapNotNull { id -> snapshot.tracks.firstOrNull { it.id == id } }
            .orEmpty()
        val queueSource = selectedPlaylistTracks.ifEmpty { snapshot.tracks }

        return current.copy(
            snapshot = snapshot,
            libraryRoots = roots,
            selectedPlaylistId = selectedPlaylistId,
            currentTrackId = currentTrackId,
            playbackQueue = buildPlaybackQueue(queueSource, currentTrackId, current.shuffleEnabled, random),
        )
    }

    private fun syncPlaybackState(snapshot: PlaybackSnapshot) {
        mutate { current ->
            current.copy(
                currentTrackId = snapshot.currentTrackId ?: current.currentTrackId,
                isPlaying = snapshot.isPlaying,
                libraryStatus = snapshot.errorMessage ?: current.libraryStatus,
            )
        }
    }
}
