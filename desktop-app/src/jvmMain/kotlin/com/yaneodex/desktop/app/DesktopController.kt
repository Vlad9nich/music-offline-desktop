package com.yaneodex.desktop.app

import com.yaneodex.core.contracts.LibraryRepository
import com.yaneodex.core.contracts.MusicSourceCatalog
import com.yaneodex.core.contracts.OcrImportClient
import com.yaneodex.core.contracts.OcrJobResult
import com.yaneodex.core.contracts.PlaybackBackend
import com.yaneodex.core.contracts.PlaybackSnapshot
import com.yaneodex.core.importer.MatchedTrackCandidate
import com.yaneodex.core.importer.ScreenshotImportMatcher
import com.yaneodex.core.model.LibrarySnapshot
import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.model.TrackRecord
import com.yaneodex.core.playback.buildPlaybackQueue
import com.yaneodex.core.state.AppLanguage
import com.yaneodex.core.state.DemoLibrary
import com.yaneodex.core.state.DesktopSection
import com.yaneodex.core.state.DesktopUiState
import com.yaneodex.core.state.OcrSettings
import com.yaneodex.core.state.PlaybackVisualizerState
import com.yaneodex.core.state.SpotlightCard
import com.yaneodex.desktop.integration.DesktopConfig
import com.yaneodex.desktop.integration.DesktopDownloadManager
import com.yaneodex.desktop.integration.DownloadManager
import com.yaneodex.desktop.integration.DesktopLibraryRepository
import com.yaneodex.desktop.integration.DesktopMusicSourceCatalog
import com.yaneodex.desktop.integration.DesktopPersistence
import com.yaneodex.desktop.integration.JavaFxPlaybackBackend
import com.yaneodex.desktop.integration.WindowsOcrClient
import com.yaneodex.desktop.ui.desktopStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.random.Random

class DesktopController(
    private val random: Random = Random(42),
    private val config: DesktopConfig = DesktopConfig.load(),
    private val libraryRepository: LibraryRepository = DesktopLibraryRepository(
        configuredDefaultRoots = listOfNotNull(config.libraryPath?.takeIf { it.isNotBlank() }),
    ),
    private val sourceCatalog: MusicSourceCatalog = DesktopMusicSourceCatalog(),
    private val ocrClient: OcrImportClient = WindowsOcrClient(),
    private val persistence: DesktopPersistence = DesktopPersistence(),
    private val playbackBackend: PlaybackBackend = JavaFxPlaybackBackend(),
    private val downloadManager: DownloadManager = DesktopDownloadManager(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<DesktopUiState> = _state.asStateFlow()

    init {
        playbackBackend.setVolume(_state.value.playbackVolume, ::syncPlaybackState)
    }

    fun selectSection(section: DesktopSection) {
        mutate { it.copy(selectedSection = section) }
    }

    fun setLanguage(language: AppLanguage) {
        mutate { current -> if (current.language == language) current else current.copy(language = language) }
    }

    fun selectPlaylist(playlistId: String) {
        mutate { current ->
            val playlist = current.snapshot.playlists.firstOrNull { it.id == playlistId } ?: return@mutate current
            val queue = playlist.trackIds.mapNotNull { id -> current.snapshot.tracks.firstOrNull { it.id == id } }
            current.copy(
                selectedPlaylistId = playlistId,
                selectedSection = DesktopSection.PLAYLISTS,
            currentTrackId = queue.firstOrNull()?.id ?: current.currentTrackId,
            playbackQueue = buildPlaybackQueue(
                queue.ifEmpty { current.snapshot.tracks },
                queue.firstOrNull()?.id,
                current.shuffleEnabled,
                random,
            ),
            visualizer = PlaybackVisualizerState.idle(),
            playbackPositionMs = 0L,
            playbackDurationMs = queue.firstOrNull()?.durationMs ?: current.playbackDurationMs,
            spotlight = spotlightFor(playlist.name, playlist.tone),
        )
        }
    }

    fun createPlaylist(name: String, artworkHint: String = "") {
        scope.launch {
            runCatching { libraryRepository.createPlaylist(name, artworkHint) }
                .onSuccess { stored ->
                    mutate { current ->
                        val playlist = stored.snapshot.playlists.lastOrNull()
                        rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(
                            selectedSection = DesktopSection.PLAYLISTS,
                            selectedPlaylistId = playlist?.id ?: current.selectedPlaylistId,
                            libraryStatus = text(current.language, "Плейлист создан.", "Playlist created."),
                            spotlight = playlist?.let { spotlightFor(it.name, it.tone) } ?: current.spotlight,
                        )
                    }
                }
                .onFailure { error ->
                    mutate { current ->
                        current.copy(libraryStatus = error.message ?: text(current.language, "Не удалось создать плейлист.", "Failed to create playlist."))
                    }
                }
        }
    }

    fun renameSelectedPlaylist(playlistId: String, name: String, artworkHint: String = "") {
        if (playlistId.isBlank() || playlistId == ALL_TRACKS_PLAYLIST_ID) return

        scope.launch {
            runCatching { libraryRepository.renamePlaylist(playlistId, name, artworkHint) }
                .onSuccess { stored ->
                    mutate { current ->
                        val playlist = stored.snapshot.playlists.firstOrNull { it.id == playlistId }
                        rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(
                            libraryStatus = text(current.language, "Плейлист обновлён.", "Playlist updated."),
                            spotlight = playlist?.let { spotlightFor(it.name, it.tone) } ?: current.spotlight,
                        )
                    }
                }
                .onFailure { error ->
                    mutate { current ->
                        current.copy(libraryStatus = error.message ?: text(current.language, "Не удалось переименовать плейлист.", "Failed to rename playlist."))
                    }
                }
        }
    }

    fun addTrackToSelectedPlaylist(trackId: String) {
        addTracksToSelectedPlaylist(listOf(trackId))
    }

    fun addTracksToSelectedPlaylist(trackIds: List<String>) {
        val normalizedTrackIds = trackIds.distinct()
        if (normalizedTrackIds.isEmpty()) return
        scope.launch {
            val currentState = state.value
            runCatching {
                val ensuredState = ensureEditablePlaylist(currentState)
                val targetPlaylistId = resolveEditablePlaylistId(ensuredState)
                    ?: error(text(currentState.language, "Нет доступного плейлиста.", "No editable playlist available."))
                targetPlaylistId to libraryRepository.addTracksToPlaylist(normalizedTrackIds, targetPlaylistId)
            }.onSuccess { (targetPlaylistId, stored) ->
                mutate { current ->
                    val refreshed = rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(
                        selectedSection = DesktopSection.PLAYLISTS,
                        selectedPlaylistId = targetPlaylistId,
                    )
                    val playlist = refreshed.snapshot.playlists.firstOrNull { it.id == targetPlaylistId }
                    refreshed.copy(
                        libraryStatus = "${text(current.language, "Добавлено в", "Added to")} ${playlist?.name ?: defaultPlaylistName(current.language)}",
                        spotlight = playlist?.let { spotlightFor(it.name, it.tone) } ?: refreshed.spotlight,
                    )
                }
            }.onFailure { error ->
                mutate { current ->
                    current.copy(libraryStatus = error.message ?: text(current.language, "Не удалось добавить трек.", "Failed to add track."))
                }
            }
        }
    }

    fun removeTrackFromSelectedPlaylist(trackId: String) {
        removeTracksFromSelectedPlaylist(listOf(trackId))
    }

    fun removeTracksFromSelectedPlaylist(trackIds: List<String>) {
        val playlistId = state.value.selectedPlaylistId
        val normalizedTrackIds = trackIds.distinct()
        if (playlistId.isBlank() || playlistId == ALL_TRACKS_PLAYLIST_ID || normalizedTrackIds.isEmpty()) return

        scope.launch {
            runCatching { libraryRepository.removeTracksFromPlaylist(normalizedTrackIds, playlistId) }
                .onSuccess { stored ->
                    mutate { current ->
                        rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(
                            libraryStatus = text(current.language, "Трек удалён.", "Track removed."),
                        )
                    }
                }
                .onFailure { error ->
                    mutate { current ->
                        current.copy(libraryStatus = error.message ?: text(current.language, "Не удалось удалить трек.", "Failed to remove track."))
                    }
                }
        }
    }

    fun deleteTracksFromLibrary(trackIds: List<String>) {
        val normalizedTrackIds = trackIds.distinct()
        if (normalizedTrackIds.isEmpty()) return

        scope.launch {
            val shouldStopPlayback = state.value.currentTrackId in normalizedTrackIds
            runCatching { libraryRepository.removeTracksFromLibrary(normalizedTrackIds) }
                .onSuccess { stored ->
                    if (shouldStopPlayback) {
                        playbackBackend.stop()
                    }
                    mutate { current ->
                        rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(
                            isPlaying = if (shouldStopPlayback) false else current.isPlaying,
                            visualizer = if (shouldStopPlayback) PlaybackVisualizerState.idle() else current.visualizer,
                            playbackPositionMs = if (shouldStopPlayback) 0L else current.playbackPositionMs,
                            libraryStatus = if (normalizedTrackIds.size == 1) {
                                text(current.language, "РўСЂРµРє СѓРґР°Р»С‘РЅ РёР· Р±РёР±Р»РёРѕС‚РµРєРё.", "Track removed from library.")
                            } else {
                                text(current.language, "РўСЂРµРєРё СѓРґР°Р»РµРЅС‹ РёР· Р±РёР±Р»РёРѕС‚РµРєРё.", "Tracks removed from library.")
                            },
                        )
                    }
                }
                .onFailure { error ->
                    mutate { current ->
                        current.copy(
                            libraryStatus = error.message ?: if (normalizedTrackIds.size == 1) {
                                text(current.language, "РќРµ СѓРґР°Р»РѕСЃСЊ СѓРґР°Р»РёС‚СЊ С‚СЂРµРє РёР· Р±РёР±Р»РёРѕС‚РµРєРё.", "Failed to remove track from library.")
                            } else {
                                text(current.language, "РќРµ СѓРґР°Р»РѕСЃСЊ СѓРґР°Р»РёС‚СЊ С‚СЂРµРєРё РёР· Р±РёР±Р»РёРѕС‚РµРєРё.", "Failed to remove tracks from library.")
                            },
                        )
                    }
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
                visualizer = PlaybackVisualizerState.idle(),
                playbackPositionMs = 0L,
                playbackDurationMs = source.firstOrNull { it.id == trackId }?.durationMs ?: 0L,
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
                visualizer = PlaybackVisualizerState.idle(),
                playbackPositionMs = 0L,
                playbackDurationMs = queue.firstOrNull()?.durationMs ?: 0L,
            )
        }
        playbackBackend.playQueue(state.value.playbackQueue, state.value.currentTrackId, ::syncPlaybackState)
    }

    fun toggleShuffle() {
        mutate { current ->
            val nextShuffle = !current.shuffleEnabled
            val source = when (current.selectedSection) {
                DesktopSection.PLAYLISTS -> current.selectedPlaylistTracks.ifEmpty { current.snapshot.tracks }
                DesktopSection.SEARCH -> current.filteredTracks.ifEmpty { current.snapshot.tracks }
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

    fun seekPlayback(positionMs: Long) {
        val current = state.value
        val duration = current.playbackDurationMs.takeIf { it > 0 } ?: current.currentTrack?.durationMs ?: 0L
        mutate(persist = false) {
            it.copy(playbackPositionMs = positionMs.coerceIn(0L, duration.takeIf { total -> total > 0 } ?: positionMs.coerceAtLeast(0L)))
        }
        playbackBackend.seekTo(positionMs, ::syncPlaybackState)
    }

    fun setPlaybackVolume(volume: Float) {
        mutate {
            it.copy(playbackVolume = volume.coerceIn(0f, 1f))
        }
        playbackBackend.setVolume(volume, ::syncPlaybackState)
    }

    fun updateSearchQuery(query: String) {
        mutate { it.copy(searchQuery = query, selectedSection = DesktopSection.SEARCH) }
    }

    fun activateTag(tag: String) {
        mutate { current ->
            current.copy(
                highlightedTag = tag,
                spotlight = SpotlightCard("", tag, "", current.selectedPlaylist?.tone ?: "#95F15A"),
            )
        }
    }

    fun searchParser(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            mutate {
                it.copy(
                    parserResults = emptyList(),
                    parserStatus = text(it.language, "Введи запрос.", "Enter a query."),
                    parserLoading = false,
                )
            }
            return
        }

        mutate { current ->
            current.copy(
                selectedSection = DesktopSection.SEARCH,
                parserLoading = true,
                parserStatus = text(current.language, "Поиск...", "Searching..."),
            )
        }

        scope.launch {
            runCatching { sourceCatalog.search(normalized) }
                .onSuccess { results ->
                    mutate { current ->
                        current.copy(
                            parserResults = results,
                            parserLoading = false,
                            parserStatus = if (results.isEmpty()) {
                                text(current.language, "Ничего не найдено.", "Nothing found.")
                            } else {
                                text(current.language, "Найдено: ${results.size}", "Found: ${results.size}")
                            },
                        )
                    }
                }
                .onFailure { error ->
                    mutate { current ->
                        current.copy(
                            parserLoading = false,
                            parserStatus = friendlyParserFailure(current.language, error),
                        )
                    }
                }
        }
    }

    fun previewParserResult(track: RemoteTrackCandidate) {
        scope.launch {
            mutate { current -> current.copy(parserStatus = text(current.language, "Подготовка предпрослушивания...", "Preparing preview...")) }
            runCatching { sourceCatalog.resolve(track) }
                .onSuccess { blueprint ->
                    val previewTrack = TrackRecord(
                        id = "preview-${UUID.nameUUIDFromBytes(blueprint.url.toByteArray())}",
                        uri = blueprint.url,
                        sourceUri = blueprint.url,
                        title = blueprint.title,
                        artist = blueprint.artist,
                        durationMs = 0L,
                        importedAtEpochMs = System.currentTimeMillis(),
                    )
                    mutate { current ->
                        current.copy(
                            currentTrackId = previewTrack.id,
                            playbackQueue = listOf(previewTrack),
                            visualizer = PlaybackVisualizerState.idle(),
                            playbackPositionMs = 0L,
                            playbackDurationMs = 0L,
                            parserStatus = text(current.language, "Предпрослушивание.", "Previewing."),
                            spotlight = spotlightFor(previewTrack.title, "#7CC8FF"),
                        )
                    }
                    playbackBackend.playQueue(listOf(previewTrack), previewTrack.id, ::syncPlaybackState)
                }
                .onFailure { error ->
                    mutate { current ->
                        current.copy(parserStatus = friendlyPreviewFailure(current.language, error))
                    }
                }
        }
    }

    fun downloadParserResult(track: RemoteTrackCandidate) {
        scope.launch {
            mutate { current -> current.copy(parserStatus = text(current.language, "Скачивание...", "Downloading...")) }
            runCatching {
                val blueprint = sourceCatalog.resolve(track)
                val targetDir = resolveDownloadDirectory(alsoImportIntoLibrary = false)
                downloadManager.download(blueprint, targetDir)
            }.onSuccess { file ->
                mutate { current -> current.copy(parserStatus = friendlyDownloadSuccess(current.language, file)) }
            }.onFailure { error ->
                mutate { current ->
                    current.copy(parserStatus = friendlyDownloadFailure(current.language, error))
                }
            }
        }
    }

    fun addParserResultToPlaylist(track: RemoteTrackCandidate) {
        scope.launch {
            val currentState = state.value
            mutate { current -> current.copy(parserStatus = text(current.language, "Скачивание в библиотеку...", "Downloading to library...")) }
            runCatching {
                val blueprint = sourceCatalog.resolve(track)
                val targetRoot = resolveDownloadDirectory(alsoImportIntoLibrary = true)
                val file = downloadManager.download(blueprint, targetRoot)
                val refreshed = refreshLibraryRoot(targetRoot)
                val trackId = refreshed.snapshot.tracks.firstOrNull { it.sourceUri == file.absolutePath }?.id
                    ?: error(text(currentState.language, "Скачанный трек не найден в библиотеке.", "Downloaded track not found in library."))
                val ensured = ensureEditablePlaylist(rebuildSnapshotState(state.value, refreshed.snapshot, refreshed.roots))
                val playlistId = resolveEditablePlaylistId(ensured)
                    ?: error(text(currentState.language, "Нет доступного плейлиста.", "No editable playlist available."))
                val withTrack = libraryRepository.addTrackToPlaylist(trackId, playlistId)
                Triple(playlistId, withTrack.snapshot, withTrack.roots)
            }.onSuccess { (playlistId, snapshot, roots) ->
                mutate { current ->
                    val refreshed = rebuildSnapshotState(current, snapshot, roots).copy(
                        selectedSection = DesktopSection.PLAYLISTS,
                        selectedPlaylistId = playlistId,
                    )
                    val playlist = refreshed.snapshot.playlists.firstOrNull { it.id == playlistId }
                    refreshed.copy(
                        parserStatus = "${text(current.language, "Добавлено в", "Added to")} ${playlist?.name ?: defaultPlaylistName(current.language)}",
                        spotlight = playlist?.let { spotlightFor(it.name, it.tone) } ?: refreshed.spotlight,
                    )
                }
            }.onFailure { error ->
                mutate { current ->
                    current.copy(parserStatus = friendlyAddTrackFailure(current.language, error))
                }
            }
        }
    }

    fun applyParserResult(track: RemoteTrackCandidate) {
        previewParserResult(track)
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
                        rebuildSnapshotState(current, stored.snapshot, stored.roots).copy(
                            libraryStatus = text(current.language, "Импортировано: ${stored.snapshot.tracks.size}", "Imported: ${stored.snapshot.tracks.size}"),
                        )
                    }
                }
                .onFailure { error ->
                    mutate { current ->
                        current.copy(libraryStatus = error.message ?: text(current.language, "Ошибка импорта.", "Import failed."))
                    }
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
                                text(current.language, "Аудиофайлы не найдены.", "No audio files found.")
                            } else {
                                text(current.language, "Треков: ${stored.snapshot.tracks.size}", "Tracks: ${stored.snapshot.tracks.size}")
                            },
                        )
                    }
                }
                .onFailure { error ->
                    mutate { current ->
                        current.copy(libraryStatus = error.message ?: text(current.language, "Не удалось обновить библиотеку.", "Refresh failed."))
                    }
                }
        }
    }

    fun analyzeScreenshots(files: List<File>) {
        if (files.isEmpty()) return
        val settings = state.value.ocrSettings
        if (settings.serverUrl.isBlank()) {
            mutate { current ->
                current.copy(
                    selectedSection = DesktopSection.IMPORT,
                    ocrStatus = text(current.language, "Укажи URL OCR сервера.", "Set OCR server URL first."),
                )
            }
            return
        }

        mutate { current ->
            current.copy(
                selectedSection = DesktopSection.IMPORT,
                ocrLoading = true,
                ocrStatus = text(current.language, "Загрузка...", "Uploading..."),
            )
        }

        scope.launch {
            runCatching {
                val response = if (files.size == 1) {
                    val file = files.first()
                    ocrClient.submitSingleImage(settings.serverUrl, settings.authToken, file.readBytes(), file.name)
                } else {
                    val job = ocrClient.submitJob(
                        settings.serverUrl,
                        settings.authToken,
                        files.map { it.name to it.readBytes() },
                    )
                    ocrClient.pollJob(settings.serverUrl, settings.authToken, job.jobId)
                }
                buildImportMatches(response)
            }.onSuccess { matches ->
                mutate { current ->
                    current.copy(
                        importMatches = matches,
                        ocrLoading = false,
                        ocrStatus = text(current.language, "Совпадений: ${matches.count { it.bestMatch != null }}", "Matches: ${matches.count { it.bestMatch != null }}"),
                    )
                }
            }.onFailure { error ->
                mutate { current ->
                    current.copy(ocrLoading = false, ocrStatus = friendlyOcrFailure(current.language, error))
                }
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
            .map { candidate -> ScreenshotImportMatcher.pickBestMatch(candidate, remoteCandidates, state.value.selectedPlaylistTracks) }
    }

    private fun mutate(persist: Boolean = true, transform: (DesktopUiState) -> DesktopUiState) {
        _state.update { current ->
            val updated = transform(current)
            if (persist) {
                persistence.save(updated)
            }
            updated
        }
    }

    private fun buildInitialState(): DesktopUiState {
        val storedLibrary = libraryRepository.load()
        val firstRealPlaylistId = storedLibrary.snapshot.playlists.firstOrNull { it.id != ALL_TRACKS_PLAYLIST_ID }?.id
        val fallbackPlaylistId = storedLibrary.snapshot.playlists.firstOrNull()?.id ?: ALL_TRACKS_PLAYLIST_ID

        val base = DemoLibrary.initialState(random).copy(
            snapshot = storedLibrary.snapshot,
            libraryRoots = storedLibrary.roots,
            selectedPlaylistId = firstRealPlaylistId ?: fallbackPlaylistId,
            currentTrackId = storedLibrary.snapshot.tracks.firstOrNull()?.id,
            playbackQueue = buildPlaybackQueue(storedLibrary.snapshot.tracks, storedLibrary.snapshot.tracks.firstOrNull()?.id, false, random),
            visualizer = PlaybackVisualizerState.idle(),
            ocrSettings = OcrSettings(
                serverUrl = config.ocrBaseUrl.orEmpty(),
                authToken = config.ocrToken.orEmpty(),
            ),
        )
        val restored = persistence.apply(base, persistence.load())
        val selectedPlaylist = storedLibrary.snapshot.playlists.firstOrNull { it.id == restored.selectedPlaylistId }

        return restored.copy(
            libraryStatus = if (storedLibrary.snapshot.tracks.isEmpty()) {
                text(restored.language, "Добавь папки с музыкой.", "Add music folders.")
            } else {
                text(restored.language, "Треков: ${storedLibrary.snapshot.tracks.size}", "Tracks: ${storedLibrary.snapshot.tracks.size}")
            },
            parserStatus = if (restored.parserResults.isEmpty()) strings(restored.language).parserResultsIdle else restored.parserStatus,
            ocrStatus = if (restored.importMatches.isEmpty()) text(restored.language, "Выбери скриншоты.", "Choose screenshots.") else restored.ocrStatus,
            spotlight = selectedPlaylist?.let { spotlightFor(it.name, it.tone) } ?: restored.spotlight,
        )
    }

    private fun rebuildSnapshotState(current: DesktopUiState, snapshot: LibrarySnapshot, roots: List<String>): DesktopUiState {
        val selectedPlaylistId = snapshot.playlists.firstOrNull { it.id == current.selectedPlaylistId }?.id
            ?: snapshot.playlists.firstOrNull { it.id != ALL_TRACKS_PLAYLIST_ID }?.id
            ?: snapshot.playlists.firstOrNull()?.id
            ?: ALL_TRACKS_PLAYLIST_ID
        val currentTrackId = snapshot.tracks.firstOrNull { it.id == current.currentTrackId }?.id ?: snapshot.tracks.firstOrNull()?.id
        val stillHasCurrentTrack = snapshot.tracks.any { it.id == current.currentTrackId }
        val selectedPlaylistTracks = snapshot.playlists
            .firstOrNull { it.id == selectedPlaylistId }
            ?.trackIds
            ?.mapNotNull { id -> snapshot.tracks.firstOrNull { it.id == id } }
            .orEmpty()
        val queueSource = selectedPlaylistTracks.ifEmpty { snapshot.tracks }

        return current.copy(
            snapshot = snapshot,
            libraryRoots = roots,
            selectedPlaylistId = selectedPlaylistId,
            currentTrackId = currentTrackId,
            playbackQueue = buildPlaybackQueue(queueSource, currentTrackId, current.shuffleEnabled, random),
            isPlaying = current.isPlaying && stillHasCurrentTrack,
            visualizer = if (current.isPlaying && stillHasCurrentTrack) current.visualizer else PlaybackVisualizerState.idle(current.visualizer.bands.size),
            playbackPositionMs = if (currentTrackId == current.currentTrackId && stillHasCurrentTrack) current.playbackPositionMs else 0L,
            playbackDurationMs = snapshot.tracks.firstOrNull { it.id == currentTrackId }?.durationMs ?: current.playbackDurationMs,
            playbackVolume = current.playbackVolume,
        )
    }

    private fun syncPlaybackState(snapshot: PlaybackSnapshot) {
        mutate(persist = false) { current ->
            current.copy(
                currentTrackId = snapshot.currentTrackId ?: current.currentTrackId,
                isPlaying = snapshot.isPlaying,
                visualizer = mergeVisualizer(current.visualizer, snapshot.visualizer, snapshot.isPlaying),
                playbackPositionMs = snapshot.positionMs,
                playbackDurationMs = snapshot.durationMs.takeIf { it > 0 } ?: current.currentTrack?.durationMs ?: current.playbackDurationMs,
                playbackVolume = snapshot.volume.coerceIn(0f, 1f),
                libraryStatus = snapshot.errorMessage ?: current.libraryStatus,
            )
        }
    }

    private fun mergeVisualizer(
        current: PlaybackVisualizerState,
        incoming: PlaybackVisualizerState?,
        isPlaying: Boolean,
    ): PlaybackVisualizerState {
        if (incoming == null) {
            return if (isPlaying) current else decayVisualizer(current)
        }

        val maxSize = max(current.bands.size, incoming.bands.size)
        val currentBands = current.bands.padTo(maxSize)
        val incomingBands = incoming.bands.padTo(maxSize)
        val smoothedBands = currentBands.zip(incomingBands) { old, next ->
            val blend = if (next >= old) 0.58f else 0.26f
            (old + (next - old) * blend).coerceIn(0f, 1f)
        }
        val intensity = (current.intensity * 0.32f + incoming.intensity * 0.68f).coerceIn(0f, 1f)
        return PlaybackVisualizerState(
            bands = smoothedBands,
            intensity = intensity,
            active = isPlaying && incoming.active,
        )
    }

    private fun decayVisualizer(current: PlaybackVisualizerState): PlaybackVisualizerState {
        val bands = current.bands.map { (it * 0.84f).coerceAtLeast(0f) }
        val intensity = (current.intensity * 0.82f).coerceAtLeast(0f)
        return PlaybackVisualizerState(
            bands = bands,
            intensity = intensity,
            active = intensity > 0.03f,
        )
    }

    private fun ensureEditablePlaylist(current: DesktopUiState): DesktopUiState {
        val existingEditablePlaylist = current.snapshot.playlists.firstOrNull {
            it.id == current.selectedPlaylistId && it.id != ALL_TRACKS_PLAYLIST_ID
        } ?: current.snapshot.playlists.firstOrNull { it.id != ALL_TRACKS_PLAYLIST_ID }

        if (existingEditablePlaylist != null) {
            return current.copy(selectedPlaylistId = existingEditablePlaylist.id)
        }

        val created = libraryRepository.createPlaylist(defaultPlaylistName(current.language))
        mutate { latest ->
            rebuildSnapshotState(latest, created.snapshot, created.roots).copy(
                selectedPlaylistId = created.snapshot.playlists.lastOrNull()?.id ?: latest.selectedPlaylistId,
                selectedSection = DesktopSection.PLAYLISTS,
            )
        }
        return state.value
    }

    private fun resolveEditablePlaylistId(current: DesktopUiState): String? {
        return current.snapshot.playlists.firstOrNull { it.id == current.selectedPlaylistId && it.id != ALL_TRACKS_PLAYLIST_ID }?.id
            ?: current.snapshot.playlists.firstOrNull { it.id != ALL_TRACKS_PLAYLIST_ID }?.id
    }

    private fun refreshLibraryRoot(root: File) =
        if (state.value.libraryRoots.any { it.equals(root.absolutePath, ignoreCase = true) }) {
            libraryRepository.refresh()
        } else {
            libraryRepository.importRoots(listOf(root.absolutePath))
        }

    private fun resolveDownloadDirectory(alsoImportIntoLibrary: Boolean): File {
        val configured = config.downloadDir?.takeIf { it.isNotBlank() }?.let(::File)
        if (configured != null) return configured

        if (alsoImportIntoLibrary) {
            state.value.libraryRoots.firstOrNull()?.let(::File)?.let { return it }
        }

        return File(File(System.getProperty("user.home"), "Music"), "YaNeoDex")
    }

    private fun friendlyParserFailure(language: AppLanguage, error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("HTTP", ignoreCase = true) -> text(language, "Источник сейчас недоступен.", "Source is unavailable right now.")
            message.contains("timeout", ignoreCase = true) -> text(language, "Источник отвечает слишком долго.", "Source is taking too long to respond.")
            else -> text(language, "Не удалось выполнить поиск.", "Failed to run search.")
        }
    }

    private fun friendlyPreviewFailure(language: AppLanguage, error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("HTTP", ignoreCase = true) -> text(language, "Превью сейчас недоступно.", "Preview is unavailable right now.")
            else -> text(language, "Не удалось включить предпрослушивание.", "Failed to start preview.")
        }
    }

    private fun friendlyDownloadSuccess(language: AppLanguage, file: File): String =
        text(language, "Скачано: ${file.name}", "Saved: ${file.name}")

    private fun friendlyDownloadFailure(language: AppLanguage, error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("HTTP 401", ignoreCase = true) || message.contains("HTTP 403", ignoreCase = true) ->
                text(language, "Источник отклонил скачивание.", "Source rejected the download.")
            message.contains("HTTP", ignoreCase = true) ->
                text(language, "Сейчас не удалось скачать трек.", "Couldn't download the track right now.")
            else -> text(language, "Не удалось скачать трек.", "Failed to download the track.")
        }
    }

    private fun friendlyAddTrackFailure(language: AppLanguage, error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("not found in library", ignoreCase = true) ->
                text(language, "Трек скачан, но ещё не появился в библиотеке.", "Track was saved but is not visible in the library yet.")
            else -> text(language, "Не удалось добавить трек.", "Failed to add the track.")
        }
    }

    private fun friendlyOcrFailure(language: AppLanguage, error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("http://", ignoreCase = true) || message.contains("https://", ignoreCase = true) ->
                text(language, "Проверь адрес OCR сервера.", "Check the OCR server URL.")
            message.contains("HTTP 401", ignoreCase = true) || message.contains("HTTP 403", ignoreCase = true) ->
                text(language, "Проверь OCR token.", "Check the OCR token.")
            message.contains("did not finish in time", ignoreCase = true) || message.contains("timeout", ignoreCase = true) ->
                text(language, "OCR отвечает слишком долго.", "OCR is taking too long to respond.")
            message.contains("HTTP", ignoreCase = true) ->
                text(language, "OCR сервер сейчас недоступен.", "OCR server is unavailable right now.")
            else -> text(language, "Не удалось обработать скриншоты.", "Failed to process screenshots.")
        }
    }

    private fun spotlightFor(title: String, tone: String): SpotlightCard = SpotlightCard("", title, "", tone)

    private fun strings(language: AppLanguage) = desktopStrings(language)

    private fun text(language: AppLanguage, ru: String, en: String): String = when (language) {
        AppLanguage.RU -> ru
        AppLanguage.EN -> en
    }

    private fun defaultPlaylistName(language: AppLanguage): String = text(language, "Избранное", "Favorites")

    private fun List<Float>.padTo(size: Int): List<Float> = if (this.size >= size) this else this + List(size - this.size) { 0f }

    private companion object {
        const val ALL_TRACKS_PLAYLIST_ID = "library-all"
    }
}
