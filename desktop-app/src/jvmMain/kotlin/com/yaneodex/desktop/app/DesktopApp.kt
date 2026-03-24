package com.yaneodex.desktop.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yaneodex.desktop.integration.WindowsFileDialogs
import com.yaneodex.desktop.ui.MusicDesktopApp
import com.yaneodex.desktop.ui.theme.YaNeoDexDesktopTheme

private val controller = DesktopController()

@Composable
fun DesktopApp() {
    val state by controller.state.collectAsState()
    YaNeoDexDesktopTheme {
        MusicDesktopApp(
            state = state,
            onSelectSection = controller::selectSection,
            onSelectPlaylist = controller::selectPlaylist,
            onCreatePlaylist = controller::createPlaylist,
            onRenamePlaylist = controller::renameSelectedPlaylist,
            onPlayTrack = controller::playTrack,
            onPlayPlaylist = controller::playCurrentPlaylist,
            onAddTrackToPlaylist = controller::addTrackToSelectedPlaylist,
            onRemoveTrackFromPlaylist = controller::removeTrackFromSelectedPlaylist,
            onTogglePlayPause = controller::togglePlayPause,
            onPlayNext = controller::playNext,
            onPlayPrevious = controller::playPrevious,
            onToggleShuffle = controller::toggleShuffle,
            onSearchChange = controller::updateSearchQuery,
            onRunParserSearch = controller::searchParser,
            onParserResultClick = controller::applyParserResult,
            onTagClick = controller::activateTag,
            onImportLibraryFolders = { controller.importLibraryFolders(WindowsFileDialogs.pickFolders().map { file -> file.absolutePath }) },
            onRefreshLibrary = controller::refreshLibrary,
            onOcrServerUrlChange = { controller.updateOcrSettings(serverUrl = it) },
            onOcrTokenChange = { controller.updateOcrSettings(authToken = it) },
            onPickScreenshots = { controller.analyzeScreenshots(WindowsFileDialogs.pickImageFiles()) },
        )
    }
}
