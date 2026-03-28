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
            onLanguageChange = controller::setLanguage,
            onCreatePlaylist = { name, artworkHint -> controller.createPlaylist(name, artworkHint) },
            onRenamePlaylist = { playlistId, name, artworkHint -> controller.renameSelectedPlaylist(playlistId, name, artworkHint) },
            onPlayTrack = controller::playTrack,
            onPlayPlaylist = controller::playCurrentPlaylist,
            onAddTrackToPlaylist = controller::addTrackToSelectedPlaylist,
            onAddTracksToPlaylist = controller::addTracksToSelectedPlaylist,
            onRemoveTrackFromPlaylist = controller::removeTrackFromSelectedPlaylist,
            onRemoveTracksFromPlaylist = controller::removeTracksFromSelectedPlaylist,
            onDeleteTracksFromLibrary = controller::deleteTracksFromLibrary,
            onTogglePlayPause = controller::togglePlayPause,
            onPlayNext = controller::playNext,
            onPlayPrevious = controller::playPrevious,
            onSeekPlayback = controller::seekPlayback,
            onSetPlaybackVolume = controller::setPlaybackVolume,
            onToggleShuffle = controller::toggleShuffle,
            onSearchChange = controller::updateSearchQuery,
            onRunParserSearch = controller::searchParser,
            onParserResultClick = controller::applyParserResult,
            onParserPreview = controller::previewParserResult,
            onParserDownload = controller::downloadParserResult,
            onParserAddToPlaylist = controller::addParserResultToPlaylist,
            onImportLibraryFolders = { controller.importLibraryFolders(WindowsFileDialogs.pickFolders().map { file -> file.absolutePath }) },
            onRefreshLibrary = controller::refreshLibrary,
            onOcrServerUrlChange = { controller.updateOcrSettings(serverUrl = it) },
            onOcrTokenChange = { controller.updateOcrSettings(authToken = it) },
            onPickScreenshots = { controller.analyzeScreenshots(WindowsFileDialogs.pickImageFiles()) },
        )
    }
}
