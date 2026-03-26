package com.yaneodex.desktop.integration

import com.yaneodex.core.state.DesktopSection
import com.yaneodex.core.state.DesktopUiState
import com.yaneodex.core.state.OcrSettings
import com.yaneodex.core.state.AppLanguage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class DesktopPersistence(
    private val stateFile: File = File(System.getProperty("user.home"), ".yaneodex-desktop/state.json"),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun load(): PersistedDesktopState? {
        if (!stateFile.exists()) return null
        return runCatching {
            json.decodeFromString<PersistedDesktopState>(stateFile.readText())
        }.getOrNull()
    }

    fun save(state: DesktopUiState) {
        val persisted = PersistedDesktopState(
            selectedSection = state.selectedSection,
            selectedPlaylistId = state.selectedPlaylistId,
            currentTrackId = state.currentTrackId,
            language = state.language,
            shuffleEnabled = state.shuffleEnabled,
            searchQuery = state.searchQuery,
            highlightedTag = state.highlightedTag,
            ocrSettings = state.ocrSettings,
        )
        runCatching {
            stateFile.parentFile?.mkdirs()
            stateFile.writeText(json.encodeToString(PersistedDesktopState.serializer(), persisted))
        }
    }

    fun apply(base: DesktopUiState, persisted: PersistedDesktopState?): DesktopUiState {
        if (persisted == null) return base
        val playlistId = base.snapshot.playlists.firstOrNull { it.id == persisted.selectedPlaylistId }?.id
            ?: base.selectedPlaylistId
        val currentTrackId = base.snapshot.tracks.firstOrNull { it.id == persisted.currentTrackId }?.id
            ?: base.currentTrackId

        return base.copy(
            selectedSection = persisted.selectedSection,
            selectedPlaylistId = playlistId,
            currentTrackId = currentTrackId,
            language = persisted.language,
            shuffleEnabled = persisted.shuffleEnabled,
            searchQuery = persisted.searchQuery,
            highlightedTag = persisted.highlightedTag.takeIf { it.isNotBlank() } ?: base.highlightedTag,
            ocrSettings = persisted.ocrSettings,
            spotlight = base.snapshot.playlists.firstOrNull { it.id == playlistId }?.let {
                base.spotlight.copy(title = it.name, subtitle = it.description, accent = it.tone)
            } ?: base.spotlight,
        )
    }
}

@Serializable
data class PersistedDesktopState(
    val selectedSection: DesktopSection = DesktopSection.HOME,
    val selectedPlaylistId: String = "library-all",
    val currentTrackId: String? = null,
    val language: AppLanguage = AppLanguage.RU,
    val shuffleEnabled: Boolean = false,
    val searchQuery: String = "",
    val highlightedTag: String = "",
    val ocrSettings: OcrSettings = OcrSettings(),
)
