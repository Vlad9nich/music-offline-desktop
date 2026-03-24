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
            playlist("library-all", "All Tracks", "AT", "#95F15A", "Your scanned local music files.", emptyList(), now - 90_000),
        ),
    )

    fun initialState(random: Random = Random(42)): DesktopUiState {
        val firstPlaylist = snapshot.playlists.first()
        return DesktopUiState(
            snapshot = snapshot,
            libraryRoots = emptyList(),
            libraryStatus = "No local audio files found. Add your Windows music folders.",
            selectedSection = DesktopSection.HOME,
            selectedPlaylistId = firstPlaylist.id,
            currentTrackId = null,
            playbackQueue = buildPlaybackQueue(snapshot.tracks, null, false, random),
            shuffleEnabled = false,
            isPlaying = false,
            searchQuery = "",
            parserResults = emptyList(),
            parserStatus = "Search the parser to pull remote candidates.",
            parserLoading = false,
            ocrSettings = OcrSettings(),
            ocrStatus = "Configure the OCR endpoint and choose Windows screenshots.",
            ocrLoading = false,
            importMatches = emptyList<MatchedTrackCandidate>(),
            spotlight = SpotlightCard(
                eyebrow = "Desktop MVP",
                title = "Local-first Windows music",
                subtitle = "Scan folders, play tracks, import playlists from OCR screenshots.",
                accent = "#95F15A",
            ),
            highlightedTag = "Night Drive",
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
