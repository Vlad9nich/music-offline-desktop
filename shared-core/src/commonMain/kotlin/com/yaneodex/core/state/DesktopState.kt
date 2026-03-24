package com.yaneodex.core.state

import com.yaneodex.core.importer.MatchedTrackCandidate
import com.yaneodex.core.model.LibrarySnapshot
import com.yaneodex.core.model.PlaylistRecord
import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.model.TrackRecord
import kotlinx.serialization.Serializable

enum class DesktopSection {
    HOME,
    SEARCH,
    PLAYLISTS,
    LIBRARY,
    IMPORT,
}

data class SpotlightCard(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val accent: String,
)

@Serializable
data class OcrSettings(
    val serverUrl: String = "",
    val authToken: String = "",
)

data class DesktopUiState(
    val snapshot: LibrarySnapshot,
    val libraryRoots: List<String>,
    val libraryStatus: String,
    val selectedSection: DesktopSection,
    val selectedPlaylistId: String,
    val currentTrackId: String?,
    val playbackQueue: List<TrackRecord>,
    val shuffleEnabled: Boolean,
    val isPlaying: Boolean,
    val searchQuery: String,
    val parserResults: List<RemoteTrackCandidate>,
    val parserStatus: String,
    val parserLoading: Boolean,
    val ocrSettings: OcrSettings,
    val ocrStatus: String,
    val ocrLoading: Boolean,
    val importMatches: List<MatchedTrackCandidate>,
    val spotlight: SpotlightCard,
    val highlightedTag: String,
) {
    val selectedPlaylist: PlaylistRecord?
        get() = snapshot.playlists.firstOrNull { it.id == selectedPlaylistId }

    val selectedPlaylistTracks: List<TrackRecord>
        get() {
            val tracksById = snapshot.tracks.associateBy { it.id }
            return selectedPlaylist?.trackIds.orEmpty().mapNotNull(tracksById::get)
        }

    val currentTrack: TrackRecord?
        get() = snapshot.tracks.firstOrNull { it.id == currentTrackId }

    val filteredTracks: List<TrackRecord>
        get() {
            if (searchQuery.isBlank()) return snapshot.tracks
            val query = searchQuery.trim()
            return snapshot.tracks.filter {
                it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
            }
        }
}
