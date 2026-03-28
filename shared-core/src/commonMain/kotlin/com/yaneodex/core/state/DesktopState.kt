package com.yaneodex.core.state

import com.yaneodex.core.importer.MatchedTrackCandidate
import com.yaneodex.core.model.LibrarySnapshot
import com.yaneodex.core.model.PlaylistRecord
import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.model.TrackRecord
import kotlinx.serialization.Serializable
import kotlin.LazyThreadSafetyMode

enum class DesktopSection {
    HOME,
    SEARCH,
    PLAYLISTS,
    LIBRARY,
    IMPORT,
}

@Serializable
enum class AppLanguage {
    RU,
    EN,
}

data class SpotlightCard(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val accent: String,
)

data class PlaybackVisualizerState(
    val bands: List<Float> = List(24) { 0f },
    val intensity: Float = 0f,
    val active: Boolean = false,
) {
    companion object {
        fun idle(size: Int = 24): PlaybackVisualizerState = PlaybackVisualizerState(
            bands = List(size) { 0f },
            intensity = 0f,
            active = false,
        )
    }
}

@Serializable
data class OcrSettings(
    val serverUrl: String = "",
    val authToken: String = "",
)

data class DesktopUiState(
    val snapshot: LibrarySnapshot,
    val libraryRoots: List<String>,
    val libraryStatus: String,
    val language: AppLanguage,
    val selectedSection: DesktopSection,
    val selectedPlaylistId: String,
    val currentTrackId: String?,
    val playbackQueue: List<TrackRecord>,
    val visualizer: PlaybackVisualizerState,
    val playbackPositionMs: Long,
    val playbackDurationMs: Long,
    val playbackVolume: Float,
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
    private val tracksById: Map<String, TrackRecord> by lazy(LazyThreadSafetyMode.NONE) {
        snapshot.tracks.associateBy { it.id }
    }

    private val queueTracksById: Map<String, TrackRecord> by lazy(LazyThreadSafetyMode.NONE) {
        playbackQueue.associateBy { it.id }
    }

    private val playlistsById: Map<String, PlaylistRecord> by lazy(LazyThreadSafetyMode.NONE) {
        snapshot.playlists.associateBy { it.id }
    }

    val selectedPlaylist: PlaylistRecord?
        get() = playlistsById[selectedPlaylistId]

    val selectedPlaylistTracks: List<TrackRecord>
        get() {
            return selectedPlaylist?.trackIds.orEmpty().mapNotNull(tracksById::get)
        }

    val currentTrack: TrackRecord?
        get() = currentTrackId?.let { id -> tracksById[id] ?: queueTracksById[id] }

    val isFirstRun: Boolean
        get() = libraryRoots.isEmpty() && snapshot.tracks.isEmpty()

    val filteredTracks: List<TrackRecord>
        get() {
            if (searchQuery.isBlank()) return snapshot.tracks
            val query = searchQuery.trim()
            return snapshot.tracks.filter {
                it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
            }
        }
}
