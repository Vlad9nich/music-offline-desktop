package com.yaneodex.core.state

import com.yaneodex.core.importer.MatchedTrackCandidate
import com.yaneodex.core.model.LibrarySnapshot
import com.yaneodex.core.model.PlaylistRecord
import com.yaneodex.core.playback.buildPlaybackQueue
import kotlin.random.Random

object DemoLibrary {
    private val now = 1_711_251_200_000L

    val snapshot: LibrarySnapshot = LibrarySnapshot(
        tracks = emptyList(),
        playlists = listOf(
            playlist("library-all", "Все треки", "AT", "#95F15A", "", emptyList(), now - 90_000),
        ),
    )

    fun initialState(random: Random = Random(42)): DesktopUiState {
        val firstPlaylist = snapshot.playlists.first()
        return DesktopUiState(
            snapshot = snapshot,
            libraryRoots = emptyList(),
            libraryStatus = "Добавь папки с музыкой.",
            language = AppLanguage.RU,
            selectedSection = DesktopSection.HOME,
            selectedPlaylistId = firstPlaylist.id,
            currentTrackId = null,
            playbackQueue = buildPlaybackQueue(snapshot.tracks, null, false, random),
            visualizer = PlaybackVisualizerState.idle(),
            playbackPositionMs = 0L,
            playbackDurationMs = 0L,
            shuffleEnabled = false,
            isPlaying = false,
            searchQuery = "",
            parserResults = emptyList(),
            parserStatus = "Запусти поиск.",
            parserLoading = false,
            ocrSettings = OcrSettings(),
            ocrStatus = "Выбери скриншоты.",
            ocrLoading = false,
            importMatches = emptyList<MatchedTrackCandidate>(),
            spotlight = SpotlightCard(
                eyebrow = "",
                title = "Музыка",
                subtitle = "",
                accent = "#95F15A",
            ),
            highlightedTag = "",
        )
    }

    private fun playlist(
        id: String,
        name: String,
        artworkHint: String,
        tone: String,
        description: String,
        trackIds: List<String>,
        createdAtEpochMs: Long,
    ): PlaylistRecord {
        return PlaylistRecord(id, name, artworkHint, tone, description, trackIds, createdAtEpochMs)
    }
}
