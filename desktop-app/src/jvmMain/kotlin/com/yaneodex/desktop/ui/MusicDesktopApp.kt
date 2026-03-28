package com.yaneodex.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import com.yaneodex.core.importer.MatchedTrackCandidate
import com.yaneodex.core.importer.ScreenshotImportItemStatus
import com.yaneodex.core.model.PlaylistRecord
import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.model.TrackRecord
import com.yaneodex.core.state.AppLanguage
import com.yaneodex.core.state.DesktopSection
import com.yaneodex.core.state.DesktopUiState
import com.yaneodex.core.state.PlaybackVisualizerState
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val Panel = Color(0xFF111313)
private val PanelRaised = Color(0xFF171A1A)
private val Outline = Color(0x1FFFFFFF)
private val Muted = Color(0xFF8A928B)
private val TextPrimary = Color(0xFFF4F7F3)
private val Moss = Color(0xFFA7F46A)
private val Gold = Color(0xFFE7C669)
private val Sky = Color(0xFF7CC8FF)
private val Coral = Color(0xFFE58B6B)

private data class LayoutMetrics(
    val pagePadding: androidx.compose.ui.unit.Dp,
    val sectionGap: androidx.compose.ui.unit.Dp,
    val sidebarWidth: androidx.compose.ui.unit.Dp,
    val rightRailWidth: androidx.compose.ui.unit.Dp,
    val searchWidth: androidx.compose.ui.unit.Dp,
    val bottomInfoWidth: androidx.compose.ui.unit.Dp,
    val compact: Boolean,
)

private sealed interface PlaylistEditorMode {
    data object Create : PlaylistEditorMode
    data class Edit(val playlistId: String) : PlaylistEditorMode
}

private data class TrackActionSpec(
    val label: String,
    val onClick: (String) -> Unit,
)

private data class TrackBulkActionSpec(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val onClick: (List<String>) -> Unit,
)

@Composable
fun MusicDesktopApp(
    state: DesktopUiState,
    onSelectSection: (DesktopSection) -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onRenamePlaylist: (String, String, String) -> Unit,
    onPlayTrack: (String) -> Unit,
    onPlayPlaylist: () -> Unit,
    onAddTrackToPlaylist: (String) -> Unit,
    onAddTracksToPlaylist: (List<String>) -> Unit,
    onRemoveTrackFromPlaylist: (String) -> Unit,
    onRemoveTracksFromPlaylist: (List<String>) -> Unit,
    onDeleteTracksFromLibrary: (List<String>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSeekPlayback: (Long) -> Unit,
    onSetPlaybackVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onSearchChange: (String) -> Unit,
    onRunParserSearch: (String) -> Unit,
    onParserResultClick: (RemoteTrackCandidate) -> Unit,
    onParserPreview: (RemoteTrackCandidate) -> Unit,
    onParserDownload: (RemoteTrackCandidate) -> Unit,
    onParserAddToPlaylist: (RemoteTrackCandidate) -> Unit,
    onImportLibraryFolders: () -> Unit,
    onRefreshLibrary: () -> Unit,
    onOcrServerUrlChange: (String) -> Unit,
    onOcrTokenChange: (String) -> Unit,
    onPickScreenshots: () -> Unit,
) {
    val strings = desktopStrings(state.language)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0D0C), Color(0xFF050606)))),
    ) {
        val metrics = remember(maxWidth) {
            when {
                maxWidth < 1180.dp -> LayoutMetrics(12.dp, 12.dp, 194.dp, 224.dp, 280.dp, 170.dp, true)
                maxWidth < 1480.dp -> LayoutMetrics(14.dp, 14.dp, 214.dp, 266.dp, 360.dp, 220.dp, false)
                else -> LayoutMetrics(18.dp, 18.dp, 238.dp, 320.dp, 430.dp, 260.dp, false)
            }
        }

        AmbientGlow()
        Row(
            modifier = Modifier.fillMaxSize().padding(metrics.pagePadding),
            horizontalArrangement = Arrangement.spacedBy(metrics.sectionGap),
        ) {
            Sidebar(state, strings, metrics, onSelectSection, onLanguageChange)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
            ) {
                TopBar(state.searchQuery, strings, metrics, onSearchChange, onRunParserSearch)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                ) {
                    MainColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        state = state,
                        strings = strings,
                        metrics = metrics,
                        onOpenSearch = { onSelectSection(DesktopSection.SEARCH) },
                        onSelectPlaylist = onSelectPlaylist,
                        onCreatePlaylist = onCreatePlaylist,
                        onRenamePlaylist = onRenamePlaylist,
                        onPlayTrack = onPlayTrack,
                        onPlayPlaylist = onPlayPlaylist,
                        onAddTrackToPlaylist = onAddTrackToPlaylist,
                        onAddTracksToPlaylist = onAddTracksToPlaylist,
                        onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
                        onRemoveTracksFromPlaylist = onRemoveTracksFromPlaylist,
                        onDeleteTracksFromLibrary = onDeleteTracksFromLibrary,
                        onParserResultClick = onParserResultClick,
                        onParserPreview = onParserPreview,
                        onParserDownload = onParserDownload,
                        onParserAddToPlaylist = onParserAddToPlaylist,
                        onImportLibraryFolders = onImportLibraryFolders,
                        onRefreshLibrary = onRefreshLibrary,
                        onOcrServerUrlChange = onOcrServerUrlChange,
                        onOcrTokenChange = onOcrTokenChange,
                        onPickScreenshots = onPickScreenshots,
                    )
                    RightRail(state, strings, metrics, onToggleShuffle, onPlayTrack)
                }
                BottomPlayer(state, strings, metrics, onTogglePlayPause, onPlayPrevious, onPlayNext, onSeekPlayback, onSetPlaybackVolume, onToggleShuffle)
            }
        }
    }
}

@Composable
private fun Sidebar(
    state: DesktopUiState,
    strings: DesktopStrings,
    metrics: LayoutMetrics,
    onSelectSection: (DesktopSection) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Surface(
        modifier = Modifier.width(metrics.sidebarWidth).fillMaxHeight(),
        color = Panel,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("YND", color = Moss, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
            Text(
                strings.appTitle,
                color = TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = FontFamily.Serif,
            )
            Spacer(Modifier.height(8.dp))
            SidebarNavigation(state.selectedSection, strings, onSelectSection)
            Spacer(Modifier.weight(1f))
            LanguageSwitcher(state.language, strings, onLanguageChange)
        }
    }
}

@Composable
private fun SidebarNavigation(
    selectedSection: DesktopSection,
    strings: DesktopStrings,
    onSelectSection: (DesktopSection) -> Unit,
) {
    val items = listOf(
        Triple(DesktopSection.HOME, strings.navHome, Icons.Rounded.Home),
        Triple(DesktopSection.SEARCH, strings.navSearch, Icons.Rounded.Search),
        Triple(DesktopSection.PLAYLISTS, strings.navPlaylists, Icons.AutoMirrored.Rounded.PlaylistPlay),
        Triple(DesktopSection.LIBRARY, strings.navLibrary, Icons.Rounded.LibraryMusic),
        Triple(DesktopSection.IMPORT, strings.navImport, Icons.Rounded.AutoAwesome),
    )
    val selectedIndex = items.indexOfFirst { it.first == selectedSection }.coerceAtLeast(0)
    val itemHeight = 56.dp
    val itemSpacing = 12.dp
    val highlightOffset by animateDpAsState(
        targetValue = (itemHeight + itemSpacing) * selectedIndex,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "sidebar-selection-offset",
    )

    Box {
        Box(
            modifier = Modifier
                .padding(top = highlightOffset)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(Moss.copy(alpha = 0.14f))
                .border(1.dp, Moss.copy(alpha = 0.34f), RoundedCornerShape(20.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(itemSpacing)) {
            items.forEach { (section, label, icon) ->
                NavPill(
                    label = label,
                    icon = icon,
                    selected = section == selectedSection,
                    onClick = { onSelectSection(section) },
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    query: String,
    strings: DesktopStrings,
    metrics: LayoutMetrics,
    onSearchChange: (String) -> Unit,
    onRunParserSearch: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(strings.topTitle, color = TextPrimary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .width(metrics.searchWidth)
                .clip(RoundedCornerShape(26.dp))
                .background(Panel)
                .border(1.dp, Outline, RoundedCornerShape(26.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = Muted)
            BasicTextField(
                value = query,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                cursorBrush = SolidColor(Moss),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                decorationBox = { inner ->
                    if (query.isBlank()) {
                        Text(strings.searchPlaceholder, color = Muted, style = MaterialTheme.typography.bodyLarge)
                    }
                    inner()
                },
            )
            PillButton(strings.runAction, Moss) { onRunParserSearch(query) }
        }
    }
}

@Composable
private fun MainColumn(
    modifier: Modifier = Modifier,
    state: DesktopUiState,
    strings: DesktopStrings,
    metrics: LayoutMetrics,
    onOpenSearch: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onRenamePlaylist: (String, String, String) -> Unit,
    onPlayTrack: (String) -> Unit,
    onPlayPlaylist: () -> Unit,
    onAddTrackToPlaylist: (String) -> Unit,
    onAddTracksToPlaylist: (List<String>) -> Unit,
    onRemoveTrackFromPlaylist: (String) -> Unit,
    onRemoveTracksFromPlaylist: (List<String>) -> Unit,
    onDeleteTracksFromLibrary: (List<String>) -> Unit,
    onParserResultClick: (RemoteTrackCandidate) -> Unit,
    onParserPreview: (RemoteTrackCandidate) -> Unit,
    onParserDownload: (RemoteTrackCandidate) -> Unit,
    onParserAddToPlaylist: (RemoteTrackCandidate) -> Unit,
    onImportLibraryFolders: () -> Unit,
    onRefreshLibrary: () -> Unit,
    onOcrServerUrlChange: (String) -> Unit,
    onOcrTokenChange: (String) -> Unit,
    onPickScreenshots: () -> Unit,
) {
    Surface(modifier = modifier, color = Color(0xCC101111), shape = RoundedCornerShape(32.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (metrics.compact) 16.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(if (metrics.compact) 16.dp else 20.dp),
        ) {
            Hero(state, strings, onPlayPlaylist)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
            ) {
                AnimatedContent(
                    targetState = state.selectedSection,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)) { fullWidth -> direction * fullWidth / 10 }) togetherWith
                            (fadeOut(animationSpec = tween(180)) +
                                slideOutHorizontally(animationSpec = tween(180)) { fullWidth -> -direction * fullWidth / 14 })
                    },
                    contentAlignment = Alignment.TopStart,
                    label = "desktop-section-transition",
                ) { section ->
                    when (section) {
                        DesktopSection.HOME -> {
                            if (state.isFirstRun) {
                                OnboardingSection(strings, onImportLibraryFolders, onRefreshLibrary, onOpenSearch)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    SectionTitle(strings.sectionPlaylists)
                                    PlaylistRow(state.snapshot.playlists, onSelectPlaylist)
                                    SectionTitle(strings.sectionRecentLibrary)
                                    TrackList(state.snapshot.tracks.take(6), state.currentTrackId, strings, onPlayTrack)
                                }
                            }
                        }
                        DesktopSection.SEARCH -> {
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                StatusCard(strings.sectionParser, state.parserStatus)
                                SectionTitle(strings.sectionParserResults)
                                ParserResults(state.parserResults, strings, onParserResultClick, onParserPreview, onParserDownload, onParserAddToPlaylist)
                            }
                        }
                        DesktopSection.PLAYLISTS -> {
                            PlaylistSection(
                                state,
                                strings,
                                onSelectPlaylist,
                                onPlayTrack,
                                onCreatePlaylist,
                                onRenamePlaylist,
                                onRemoveTrackFromPlaylist,
                                onRemoveTracksFromPlaylist,
                            )
                        }
                        DesktopSection.LIBRARY -> {
                            if (state.isFirstRun) {
                                OnboardingSection(strings, onImportLibraryFolders, onRefreshLibrary, onOpenSearch)
                            } else {
                                LibrarySection(
                                    state,
                                    strings,
                                    onPlayTrack,
                                    onAddTrackToPlaylist,
                                    onAddTracksToPlaylist,
                                    onDeleteTracksFromLibrary,
                                    onImportLibraryFolders,
                                    onRefreshLibrary,
                                )
                            }
                        }
                        DesktopSection.IMPORT -> {
                            ImportSection(state, strings, onOcrServerUrlChange, onOcrTokenChange, onPickScreenshots)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(state: DesktopUiState, strings: DesktopStrings, onPlayPlaylist: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(268.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        parseTone(state.spotlight.accent).copy(alpha = 0.42f),
                        Color(0xFF1A1E1B),
                        Color(0xFF0D0F0E),
                    ),
                ),
            )
            .border(1.dp, Outline, RoundedCornerShape(30.dp))
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1.05f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Crossfade(state.currentTrack?.title ?: state.spotlight.title, label = "hero-track-title") { title ->
                        Text(title, color = TextPrimary, style = MaterialTheme.typography.displayLarge, fontFamily = FontFamily.Serif, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        state.currentTrack?.artist ?: strings.noTrackSelectedSubtitle,
                        color = Color(0xFFD8DDD8),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AccentAction(strings.playLaneAction, Icons.Rounded.PlayArrow, Moss, onPlayPlaylist)
                    }
                }
            }
            Box(
                modifier = Modifier.weight(0.95f).fillMaxHeight().clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(parseTone(state.spotlight.accent).copy(alpha = 0.32f), Color.Transparent),
                            radius = 380f,
                        ),
                    ),
            ) {
                PlaybackVisualizer(
                    state = state.visualizer,
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(150.dp).padding(horizontal = 18.dp),
                    accent = parseTone(state.spotlight.accent),
                    dense = true,
                )
            }
        }
    }
}

@Composable
private fun OnboardingSection(
    strings: DesktopStrings,
    onImportLibraryFolders: () -> Unit,
    onRefreshLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Moss.copy(alpha = 0.18f),
                        Color(0xFF191C1A),
                        Color(0xFF0D0F0E),
                    ),
                ),
            )
            .border(1.dp, Outline, RoundedCornerShape(32.dp))
            .padding(28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.onboardingTitle, color = TextPrimary, style = MaterialTheme.typography.displaySmall, fontFamily = FontFamily.Serif)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentAction(strings.onboardingPrimaryAction, Icons.Rounded.FolderOpen, Moss, onImportLibraryFolders)
                AccentAction(strings.onboardingSecondaryAction, Icons.Rounded.Refresh, Gold, onRefreshLibrary)
                AccentAction(strings.onboardingParserAction, Icons.Rounded.Search, Sky, onOpenSearch)
            }
        }
        RecordHalo(modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun PlaylistSection(
    state: DesktopUiState,
    strings: DesktopStrings,
    onSelectPlaylist: (String) -> Unit,
    onPlayTrack: (String) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onRenamePlaylist: (String, String, String) -> Unit,
    onRemoveTrackFromPlaylist: (String) -> Unit,
    onRemoveTracksFromPlaylist: (List<String>) -> Unit,
) {
    var dialogMode by remember { mutableStateOf<PlaylistEditorMode?>(null) }
    val editingPlaylist = dialogMode?.let { mode ->
        if (mode is PlaylistEditorMode.Edit) {
            state.snapshot.playlists.firstOrNull { it.id == mode.playlistId }
        } else {
            null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(strings.sectionPlaylists)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            AccentAction(strings.createAction, Icons.AutoMirrored.Rounded.PlaylistPlay, Moss) {
                dialogMode = PlaylistEditorMode.Create
            }
        }
        PlaylistColumn(
            playlists = state.snapshot.playlists,
            selectedPlaylistId = state.selectedPlaylistId,
            strings = strings,
            onSelectPlaylist = onSelectPlaylist,
            onEditPlaylist = { playlist -> dialogMode = PlaylistEditorMode.Edit(playlist.id) },
        )
        SectionTitle(state.selectedPlaylist?.name ?: strings.sectionPlaylist)
        TrackList(
            tracks = state.selectedPlaylistTracks,
            currentTrackId = state.currentTrackId,
            strings = strings,
            onPlayTrack = onPlayTrack,
            rowActions = listOf(
                TrackActionSpec(strings.removeAction, onRemoveTrackFromPlaylist),
            ),
            bulkActions = listOf(
                TrackBulkActionSpec(strings.removeAction, Icons.Rounded.Delete, Gold, onRemoveTracksFromPlaylist),
            ),
        )
    }

    if (dialogMode != null) {
        PlaylistEditorDialog(
            strings = strings,
            initialName = editingPlaylist?.name.orEmpty(),
            initialArtworkHint = editingPlaylist?.artworkHint.orEmpty(),
            isEditing = editingPlaylist != null,
            onDismiss = { dialogMode = null },
            onSubmit = { name, artworkHint ->
                val trimmedName = name.trim()
                if (trimmedName.isBlank()) return@PlaylistEditorDialog
                if (editingPlaylist != null) {
                    onRenamePlaylist(editingPlaylist.id, trimmedName, artworkHint)
                } else {
                    onCreatePlaylist(trimmedName, artworkHint)
                }
                dialogMode = null
            },
        )
    }
}

@Composable
private fun LibrarySection(
    state: DesktopUiState,
    strings: DesktopStrings,
    onPlayTrack: (String) -> Unit,
    onAddTrackToPlaylist: (String) -> Unit,
    onAddTracksToPlaylist: (List<String>) -> Unit,
    onDeleteTracksFromLibrary: (List<String>) -> Unit,
    onImportLibraryFolders: () -> Unit,
    onRefreshLibrary: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentAction(strings.addFoldersAction, Icons.Rounded.FolderOpen, Moss, onImportLibraryFolders)
            AccentAction(strings.refreshAction, Icons.Rounded.Refresh, Gold, onRefreshLibrary)
        }
        StatusCard(strings.sectionLibrary, buildLibraryStatusText(state, strings))
        TrackList(
            tracks = state.snapshot.tracks,
            currentTrackId = state.currentTrackId,
            strings = strings,
            onPlayTrack = onPlayTrack,
            rowActions = listOf(
                TrackActionSpec(strings.addAction, onAddTrackToPlaylist),
                TrackActionSpec(strings.deleteAction) { trackId -> onDeleteTracksFromLibrary(listOf(trackId)) },
            ),
            bulkActions = listOf(
                TrackBulkActionSpec(strings.addAction, Icons.AutoMirrored.Rounded.PlaylistPlay, Moss, onAddTracksToPlaylist),
                TrackBulkActionSpec(strings.deleteAction, Icons.Rounded.Delete, Coral, onDeleteTracksFromLibrary),
            ),
        )
    }
}

@Composable
private fun ImportSection(
    state: DesktopUiState,
    strings: DesktopStrings,
    onOcrServerUrlChange: (String) -> Unit,
    onOcrTokenChange: (String) -> Unit,
    onPickScreenshots: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(strings.sectionOcrSettings)
        Surface(shape = RoundedCornerShape(24.dp), color = Panel) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(strings.ocrServerLabel, state.ocrSettings.serverUrl, strings.ocrServerPlaceholder, onOcrServerUrlChange)
                LabeledField(strings.bearerTokenLabel, state.ocrSettings.authToken, strings.bearerTokenPlaceholder, onOcrTokenChange)
                AccentAction(strings.chooseScreenshotsAction, Icons.Rounded.FolderOpen, Moss, onPickScreenshots)
            }
        }
        StatusCard(strings.sectionOcr, state.ocrStatus)
        SectionTitle(strings.sectionImportReview)
        ImportMatches(state.importMatches, strings)
    }
}

@Composable
private fun RightRail(
    state: DesktopUiState,
    strings: DesktopStrings,
    metrics: LayoutMetrics,
    onToggleShuffle: () -> Unit,
    onPlayTrack: (String) -> Unit,
) {
    Surface(modifier = Modifier.width(metrics.rightRailWidth).fillMaxHeight(), color = Color(0xCC101111), shape = RoundedCornerShape(32.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(strings.sectionNowShaping, color = Gold, style = MaterialTheme.typography.labelSmall)
            CompactNowPlayingCard(
                title = state.currentTrack?.title ?: strings.noTrackSelected,
                subtitle = state.currentTrack?.artist ?: strings.noTrackSelectedSubtitle,
            )
            MiniPanel(strings.queueLabel, "${state.playbackQueue.size} ${strings.queueItemsSuffix}", Icons.AutoMirrored.Rounded.QueueMusic, Moss)
            MiniPanel(strings.shuffleLabel, if (state.shuffleEnabled) strings.shuffleOn else strings.shuffleOff, Icons.Rounded.Shuffle, Sky, onToggleShuffle)
            MiniPanel(strings.parserLabel, state.parserStatus, Icons.Rounded.Sync, Gold)
            MiniPanel(strings.ocrLabel, state.ocrStatus, Icons.Rounded.AutoAwesome, Coral)
            Text(strings.sectionUpNext, color = Muted, style = MaterialTheme.typography.labelMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.playbackQueue.take(if (metrics.compact) 3 else 4).forEach { track -> QueueRow(track, onPlayTrack, compact = true) }
            }
        }
    }
}

@Composable
private fun BottomPlayer(
    state: DesktopUiState,
    strings: DesktopStrings,
    metrics: LayoutMetrics,
    onTogglePlayPause: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onSeekPlayback: (Long) -> Unit,
    onSetPlaybackVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(28.dp), color = Panel) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compactPlayer = useCompactPlayerLayout(maxWidth.value.toInt())
            if (compactPlayer) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtworkBadge(state.currentTrack?.title?.take(2)?.uppercase() ?: "YN", Moss, compact = true)
                        Column(modifier = Modifier.weight(1f)) {
                            Crossfade(state.currentTrack?.title ?: strings.noTrackSelected, label = "bottom-track-title-compact") { title ->
                                Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(state.currentTrack?.artist ?: strings.noTrackSelectedSubtitle, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    PlaybackTimeline(
                        positionMs = state.playbackPositionMs,
                        durationMs = state.playbackDurationMs,
                        accent = Moss,
                        onSeek = onSeekPlayback,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RoundAction(Icons.Rounded.Shuffle, state.shuffleEnabled, onToggleShuffle)
                        VolumeControl(
                            volume = state.playbackVolume,
                            accent = Moss,
                            compact = true,
                            onChange = onSetPlaybackVolume,
                        )
                        RoundAction(Icons.AutoMirrored.Rounded.NavigateBefore, false, onPlayPrevious)
                        RoundAction(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, true, onTogglePlayPause)
                        RoundAction(Icons.AutoMirrored.Rounded.NavigateNext, false, onPlayNext)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtworkBadge(state.currentTrack?.title?.take(2)?.uppercase() ?: "YN", Moss)
                        Column(modifier = Modifier.widthIn(max = metrics.bottomInfoWidth)) {
                            Crossfade(state.currentTrack?.title ?: strings.noTrackSelected, label = "bottom-track-title") { title ->
                                Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(state.currentTrack?.artist ?: strings.noTrackSelectedSubtitle, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    PlaybackTimeline(
                        positionMs = state.playbackPositionMs,
                        durationMs = state.playbackDurationMs,
                        accent = Moss,
                        onSeek = onSeekPlayback,
                        modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        VolumeControl(
                            volume = state.playbackVolume,
                            accent = Moss,
                            compact = false,
                            onChange = onSetPlaybackVolume,
                        )
                        RoundAction(Icons.Rounded.Shuffle, state.shuffleEnabled, onToggleShuffle)
                        RoundAction(Icons.AutoMirrored.Rounded.NavigateBefore, false, onPlayPrevious)
                        RoundAction(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, true, onTogglePlayPause)
                        RoundAction(Icons.AutoMirrored.Rounded.NavigateNext, false, onPlayNext)
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSwitcher(language: AppLanguage, strings: DesktopStrings, onLanguageChange: (AppLanguage) -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = PanelRaised) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Language, contentDescription = null, tint = Gold)
                Text(strings.languageLabel, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageButton("RU", language == AppLanguage.RU) { onLanguageChange(AppLanguage.RU) }
                LanguageButton("EN", language == AppLanguage.EN) { onLanguageChange(AppLanguage.EN) }
            }
        }
    }
}

@Composable
private fun LanguageButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Moss else Panel)
            .border(1.dp, if (selected) Moss.copy(alpha = 0.35f) else Outline, RoundedCornerShape(16.dp))
            .pressClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) Color.Black else TextPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlaylistRow(playlists: List<PlaylistRecord>, onSelectPlaylist: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        playlists.take(3).forEach { playlist -> PlaylistCard(playlist) { onSelectPlaylist(playlist.id) } }
    }
}

@Composable
private fun PlaylistColumn(
    playlists: List<PlaylistRecord>,
    selectedPlaylistId: String,
    strings: DesktopStrings,
    onSelectPlaylist: (String) -> Unit,
    onEditPlaylist: (PlaylistRecord) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        playlists.forEach { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (playlist.id == selectedPlaylistId) parseTone(playlist.tone).copy(alpha = 0.12f) else Panel)
                    .border(1.dp, if (playlist.id == selectedPlaylistId) parseTone(playlist.tone).copy(alpha = 0.34f) else Outline, RoundedCornerShape(22.dp))
                    .pressClickable { onSelectPlaylist(playlist.id) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ArtworkBadge(playlist.artworkHint, parseTone(playlist.tone))
                Column(modifier = Modifier.weight(1f)) {
                    Text(playlist.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                }
                if (playlist.id != "library-all") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(PanelRaised)
                            .border(1.dp, Outline, RoundedCornerShape(14.dp))
                            .pressClickable { onEditPlaylist(playlist) }
                            .padding(10.dp),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = strings.editAction, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistRecord, onClick: () -> Unit) {
    Surface(modifier = Modifier.width(220.dp).pressClickable(onClick = onClick), shape = RoundedCornerShape(24.dp), color = Panel) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(parseTone(playlist.tone), Color(0xFF232625)))),
            ) {
                Text(
                    playlist.artworkHint,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    color = Color.Black,
                    fontFamily = FontFamily.Serif,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(playlist.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PlaylistEditorDialog(
    strings: DesktopStrings,
    initialName: String,
    initialArtworkHint: String,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var artworkHint by remember(initialArtworkHint) { mutableStateOf(initialArtworkHint) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Panel,
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    if (isEditing) strings.editPlaylistDialogTitle else strings.createPlaylistDialogTitle,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtworkBadge(
                        label = derivePlaylistArtworkHint(name, artworkHint),
                        color = Moss,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LabeledField(strings.playlistNameLabel, name, strings.createPlaylistPlaceholder) { name = it }
                        LabeledField(strings.playlistAvatarLabel, artworkHint, strings.playlistAvatarPlaceholder) {
                            artworkHint = it.take(2)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryAction(strings.cancelAction, onDismiss)
                    AccentAction(
                        label = if (isEditing) strings.renameAction else strings.createAction,
                        icon = if (isEditing) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.PlaylistPlay,
                        color = if (isEditing) Gold else Moss,
                    ) {
                        onSubmit(name, artworkHint)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<TrackRecord>,
    currentTrackId: String?,
    strings: DesktopStrings,
    onPlayTrack: (String) -> Unit,
    rowActions: List<TrackActionSpec> = emptyList(),
    bulkActions: List<TrackBulkActionSpec> = emptyList(),
) {
    var selectedTrackIds by remember(tracks.map { it.id }) { mutableStateOf(emptySet<String>()) }
    val selectionEnabled = bulkActions.isNotEmpty()
    val selectionMode = selectionEnabled && selectedTrackIds.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (selectionMode) {
            BulkSelectionBar(
                count = selectedTrackIds.size,
                strings = strings,
                actions = bulkActions,
                onApply = { action ->
                    action.onClick(selectedTrackIds.toList())
                    selectedTrackIds = emptySet()
                },
                onClear = { selectedTrackIds = emptySet() },
            )
        }
        tracks.forEachIndexed { index, track ->
            val selected = track.id in selectedTrackIds
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 78.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        when {
                            selected -> Gold.copy(alpha = 0.09f)
                            track.id == currentTrackId -> Moss.copy(alpha = 0.08f)
                            else -> Panel
                        },
                    )
                    .border(
                        1.dp,
                        when {
                            selected -> Gold.copy(alpha = 0.34f)
                            track.id == currentTrackId -> Moss.copy(alpha = 0.24f)
                            else -> Outline
                        },
                        RoundedCornerShape(18.dp),
                    )
                    .pressClickable {
                        if (selectionMode) {
                            selectedTrackIds = if (selected) selectedTrackIds - track.id else selectedTrackIds + track.id
                        } else {
                            onPlayTrack(track.id)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                val compactRow = useCompactTrackRowLayout(maxWidth.value.toInt())
                if (compactRow) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selectionEnabled) {
                                SelectionToggleButton(
                                    selected = selected,
                                    onClick = {
                                        selectedTrackIds = if (selected) selectedTrackIds - track.id else selectedTrackIds + track.id
                                    },
                                )
                            }
                            ArtworkBadge(track.title.take(2).uppercase(), Moss, compact = true)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${index + 1}".padStart(2, '0'), color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(28.dp))
                            Spacer(Modifier.weight(1f))
                            rowActions.forEach { action ->
                                RowActionChip(track.id, action.label, action.onClick)
                            }
                            Text(formatDuration(track.durationMs), color = if (track.id == currentTrackId) Moss else Muted, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("${index + 1}".padStart(2, '0'), color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(28.dp))
                        if (selectionEnabled) {
                            SelectionToggleButton(
                                selected = selected,
                                onClick = {
                                    selectedTrackIds = if (selected) selectedTrackIds - track.id else selectedTrackIds + track.id
                                },
                            )
                        }
                        ArtworkBadge(track.title.take(2).uppercase(), Moss, compact = true)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        rowActions.forEach { action ->
                            RowActionChip(track.id, action.label, action.onClick)
                        }
                        Text(formatDuration(track.durationMs), color = if (track.id == currentTrackId) Moss else Muted, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (tracks.isEmpty()) {
            EmptyState(strings.emptyStateTitle)
        }
    }
}

@Composable
private fun BulkSelectionBar(
    count: Int,
    strings: DesktopStrings,
    actions: List<TrackBulkActionSpec>,
    onApply: (TrackBulkActionSpec) -> Unit,
    onClear: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = PanelRaised) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${strings.selectedLabel}: $count", color = Gold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            actions.forEach { action ->
                BulkActionChip(action = action) { onApply(action) }
            }
            SecondaryAction(strings.clearSelectionAction, onClear)
        }
    }
}

@Composable
private fun BulkActionChip(action: TrackBulkActionSpec, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(action.accent.copy(alpha = 0.12f))
            .border(1.dp, action.accent.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
            .pressClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(action.icon, contentDescription = action.label, tint = action.accent, modifier = Modifier.size(16.dp))
        Text(action.label, color = TextPrimary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SelectionToggleButton(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (selected) Gold.copy(alpha = 0.18f) else PanelRaised)
            .border(1.dp, if (selected) Gold.copy(alpha = 0.42f) else Outline, CircleShape)
            .pressClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) Gold else Muted,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun RowActionChip(trackId: String, rowActionLabel: String, onRowAction: (String) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PanelRaised)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .pressClickable(pressedScale = 0.96f) { onRowAction(trackId) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(rowActionLabel, color = TextPrimary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ParserResults(
    results: List<RemoteTrackCandidate>,
    strings: DesktopStrings,
    onParserResultClick: (RemoteTrackCandidate) -> Unit,
    onParserPreview: (RemoteTrackCandidate) -> Unit,
    onParserDownload: (RemoteTrackCandidate) -> Unit,
    onParserAddToPlaylist: (RemoteTrackCandidate) -> Unit,
) {
    if (results.isEmpty()) {
        EmptyState(strings.noParserResults)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        results.forEach { item ->
            BoxWithConstraints {
                val compactCard = useCompactParserCardLayout(maxWidth.value.toInt())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (compactCard) 108.dp else 94.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Panel)
                        .border(1.dp, Outline, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ArtworkBadge(item.title.take(2).uppercase(), Sky, compact = compactCard)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.artist, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (!compactCard) {
                            AccentChip(item.sourceId, PanelRaised, onClick = { onParserResultClick(item) })
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (compactCard) {
                            AccentChip(item.sourceId, PanelRaised, onClick = { onParserResultClick(item) })
                        }
                        AccentChip(strings.previewAction, PanelRaised, onClick = { onParserPreview(item) })
                        AccentChip(strings.downloadAction, PanelRaised, onClick = { onParserDownload(item) })
                        AccentChip(strings.addAction, PanelRaised, onClick = { onParserAddToPlaylist(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportMatches(matches: List<MatchedTrackCandidate>, strings: DesktopStrings) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        matches.forEach { item ->
            val accent = when (item.status) {
                ScreenshotImportItemStatus.MATCHED -> Moss
                ScreenshotImportItemStatus.ALREADY_IN_PLAYLIST -> Gold
                ScreenshotImportItemStatus.LOW_CONFIDENCE_MATCH -> Coral
                ScreenshotImportItemStatus.NOT_FOUND -> Muted
                ScreenshotImportItemStatus.RECOGNIZED -> Sky
            }
            Surface(shape = RoundedCornerShape(20.dp), color = Panel) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.recognized.rawText, color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.message ?: strings.importReadyMessage, color = accent, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    item.bestMatch?.let { match ->
                        Text("${match.artist} - ${match.title}", color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(track: TrackRecord, onPlayTrack: (String) -> Unit, compact: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 58.dp else 72.dp)
            .clip(RoundedCornerShape(if (compact) 16.dp else 18.dp))
            .background(Panel)
            .border(1.dp, Outline, RoundedCornerShape(if (compact) 16.dp else 18.dp))
            .pressClickable { onPlayTrack(track.id) }
            .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 9.dp else 12.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkBadge(track.title.take(2).uppercase(), Moss, compact = compact)
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = TextPrimary, style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TextInput(value: String, placeholder: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PanelRaised)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
        cursorBrush = SolidColor(Moss),
        decorationBox = { inner ->
            if (value.isBlank()) {
                Text(placeholder, color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            inner()
        },
    )
}

@Composable
private fun LabeledField(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = Gold, style = MaterialTheme.typography.labelMedium)
        TextInput(value, placeholder, Modifier.fillMaxWidth(), onChange)
    }
}

@Composable
private fun StatusCard(title: String, body: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = Panel) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title.uppercase(), color = Gold, style = MaterialTheme.typography.labelMedium)
            Text(body, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CompactNowPlayingCard(title: String, subtitle: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Panel) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.uppercase(), color = Gold, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyState(title: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = Panel) {
        Box(modifier = Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.CenterStart) {
            Text(title, color = Muted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun NavPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
            .border(1.dp, if (selected) Color.Transparent else Outline, RoundedCornerShape(20.dp))
            .interactiveClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint by animateColor(selected = selected)
        Icon(icon, contentDescription = label, tint = tint)
        Text(label, color = if (selected) TextPrimary else Muted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AccentAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.32f), RoundedCornerShape(22.dp))
            .pressClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = color)
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SecondaryAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(PanelRaised)
            .border(1.dp, Outline, RoundedCornerShape(22.dp))
            .pressClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun VolumeControl(
    volume: Float,
    accent: Color,
    compact: Boolean,
    onChange: (Float) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val normalizedVolume = volume.coerceIn(0f, 1f)
    val animatedVolume by animateFloatAsState(
        targetValue = normalizedVolume,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "volume-level",
    )
    val percentage by animateIntAsState(
        targetValue = (normalizedVolume * 100).roundToInt(),
        animationSpec = tween(180),
        label = "volume-percent",
    )
    val panelAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(180),
        label = "volume-panel-alpha",
    )
    val panelLift by animateDpAsState(
        targetValue = if (expanded) 0.dp else 8.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "volume-panel-lift",
    )
    val pulseScale by animateFloatAsState(
        targetValue = if (expanded) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "volume-button-scale",
    )
    val buttonShift by animateDpAsState(
        targetValue = if (expanded) 10.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "volume-button-shift",
    )

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(46.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (expanded || panelAlpha > 0.01f) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, if (compact) -104 else -116),
                properties = PopupProperties(focusable = false),
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PanelRaised.copy(alpha = panelAlpha),
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = panelLift.toPx()
                            alpha = panelAlpha
                        }
                        .width(if (compact) 70.dp else 94.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("$percentage%", color = accent, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = animatedVolume,
                            onValueChange = onChange,
                            modifier = Modifier
                                .graphicsLayer(rotationZ = -90f)
                                .width(if (compact) 74.dp else 88.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = Panel,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        }
        RoundAction(
            icon = volumeIconFor(normalizedVolume),
            active = normalizedVolume > 0.001f || expanded,
            onClick = { expanded = !expanded },
            modifier = Modifier.graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                translationY = buttonShift.toPx()
            },
        )
    }
}

@Composable
private fun PillButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .pressClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, color = Color.Black, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AccentChip(label: String, background: Color, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .border(1.dp, Outline, RoundedCornerShape(22.dp))
            .pressClickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MiniPanel(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.dp, Outline, RoundedCornerShape(18.dp))
            .pressClickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = TextPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ArtworkBadge(label: String, color: Color, compact: Boolean = false) {
    val size = if (compact) 42.dp else 52.dp
    val corner = if (compact) 14.dp else 16.dp
    Box(modifier = Modifier.size(size).clip(RoundedCornerShape(corner)).background(color), contentAlignment = Alignment.Center) {
        Text(
            label,
            color = Color.Black,
            fontWeight = FontWeight.Black,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun RecordHalo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(180.dp)) {
        drawCircle(Color(0x15FFFFFF), radius = size.minDimension / 2)
        drawCircle(Color(0x22000000), radius = size.minDimension / 2.6f)
        drawCircle(Moss.copy(alpha = 0.25f), radius = size.minDimension / 5.2f)
        drawCircle(Color(0xFF0D0F0E), radius = size.minDimension / 7.5f)
    }
}

@Composable
private fun RoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.9f
            hovered -> 1.05f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "player-button-scale",
    )
    val yOffset by animateDpAsState(
        targetValue = when {
            pressed -> 1.dp
            hovered -> (-2).dp
            else -> 0.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "player-button-offset",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = when {
            pressed -> 0.26f
            hovered || active -> 0.18f
            else -> 0f
        },
        animationSpec = tween(140),
        label = "player-button-glow",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = yOffset.toPx()
            }
            .size(46.dp)
            .clip(CircleShape)
            .background(if (active) Moss.copy(alpha = 0.16f + glowAlpha * 0.35f) else PanelRaised)
            .border(1.dp, if (active || hovered) Moss.copy(alpha = 0.4f + glowAlpha * 0.45f) else Outline, CircleShape)
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active || hovered) Moss else TextPrimary,
            modifier = Modifier.graphicsLayer {
                scaleX = if (pressed) 0.94f else 1f
                scaleY = if (pressed) 0.94f else 1f
            },
        )
    }
}

@Composable
private fun AmbientGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(brush = Brush.radialGradient(listOf(Moss.copy(alpha = 0.10f), Color.Transparent)), radius = 440f, center = Offset(size.width * 0.72f, size.height * 0.12f))
        drawCircle(brush = Brush.radialGradient(listOf(Gold.copy(alpha = 0.08f), Color.Transparent)), radius = 360f, center = Offset(size.width * 0.14f, size.height * 0.82f))
    }
}

private fun parseTone(value: String): Color = when (value) {
    "#95F15A", "#A7F46A" -> Moss
    "#E7C669" -> Gold
    "#7CC8FF" -> Sky
    "#E58B6B" -> Coral
    else -> Color(0xFF8A938D)
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt()
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun buildLibraryStatusText(state: DesktopUiState, strings: DesktopStrings): String {
    val roots = if (state.libraryRoots.isEmpty()) strings.noRootsConfigured else state.libraryRoots.joinToString("\n")
    return "${state.libraryStatus}\n\n${strings.rootsHeader}\n$roots"
}

private fun Modifier.pressClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    onClick: () -> Unit,
): Modifier = composed {
    interactiveClickable(enabled = enabled, pressedScale = pressedScale, hoverScale = 1f, onClick = onClick)
}

private fun Modifier.interactiveClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    hoverScale: Float = 1.012f,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val scaleTarget = when {
        pressed -> pressedScale
        hovered -> hoverScale
        else -> 1f
    }
    val scale by animateFloatAsState(scaleTarget, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "interaction-scale")
    val alpha by animateFloatAsState(if (hovered) 1f else 0.98f, animationSpec = tween(140), label = "interaction-alpha")
    graphicsLayer(
        scaleX = scale,
        scaleY = scale,
        alpha = alpha,
    )
        .hoverable(interactionSource = interactionSource, enabled = enabled)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

@Composable
private fun PlaybackVisualizer(
    state: PlaybackVisualizerState,
    modifier: Modifier,
    accent: Color,
    dense: Boolean,
) {
    val transition = rememberInfiniteTransition(label = "visualizer-idle")
    val idlePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
        ),
        label = "visualizer-idle-phase",
    )
    val animatedBands = state.bands.mapIndexed { index, band ->
        animateFloatAsState(
            targetValue = if (state.active) band else idleValue(index, idlePhase, dense),
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "band-$index",
        ).value
    }

    Canvas(modifier = modifier) {
        val bands = if (animatedBands.isEmpty()) List(if (dense) 32 else 24) { 0f } else animatedBands
        val gap = if (dense) 5f else 7f
        val barWidth = ((size.width - gap * (bands.size - 1)) / bands.size).coerceAtLeast(3f)
        bands.forEachIndexed { index, value ->
            val normalized = value.coerceIn(0f, 1f)
            val barHeight = (size.height * (0.18f + normalized * 0.82f)).coerceAtLeast(size.height * 0.12f)
            val left = index * (barWidth + gap)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.18f + normalized * 0.25f),
                        accent.copy(alpha = 0.68f + normalized * 0.2f),
                    ),
                ),
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackTimeline(
    positionMs: Long,
    durationMs: Long,
    accent: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(0L)
    val safePosition = clampTimelinePosition(positionMs, safeDuration)
    val progress = if (safeDuration > 0L) (safePosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) else 0f
    var sliderPositionMs by remember(safePosition, safeDuration) { mutableStateOf(safePosition.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    val displayedProgress = if (safeDuration > 0L) {
        ((if (dragging) sliderPositionMs else safePosition.toFloat()) / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        progress
    }
    val animatedProgress by animateFloatAsState(
        targetValue = displayedProgress,
        animationSpec = tween(180),
        label = "timeline-progress",
    )

    LaunchedEffect(safePosition, safeDuration, dragging) {
        if (!dragging) {
            sliderPositionMs = safePosition.toFloat()
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Slider(
            value = if (dragging) sliderPositionMs else safePosition.toFloat(),
            onValueChange = { nextValue ->
                dragging = true
                sliderPositionMs = nextValue.coerceIn(0f, safeDuration.coerceAtLeast(1L).toFloat())
            },
            onValueChangeFinished = {
                dragging = false
                onSeek(clampTimelinePosition(sliderPositionMs.roundToLong(), safeDuration))
            },
            valueRange = 0f..safeDuration.coerceAtLeast(1L).toFloat(),
            enabled = safeDuration > 0L,
            colors = SliderDefaults.colors(
                thumbColor = Color.White.copy(alpha = 0.92f),
                activeTrackColor = accent,
                inactiveTrackColor = PanelRaised,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledThumbColor = Muted,
                disabledActiveTrackColor = accent.copy(alpha = 0.32f),
                disabledInactiveTrackColor = PanelRaised,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .focusable(enabled = safeDuration > 0L)
                .onPreviewKeyEvent { event ->
                    if (safeDuration <= 0L || event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }
                    val step = timelineKeyboardStepMs(safeDuration)
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onSeek(clampTimelinePosition(safePosition - step, safeDuration))
                            true
                        }
                        Key.DirectionRight -> {
                            onSeek(clampTimelinePosition(safePosition + step, safeDuration))
                            true
                        }
                        else -> false
                    }
                },
            thumb = { _ ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.96f))
                        .border(1.dp, accent.copy(alpha = 0.42f), CircleShape),
                )
            },
            track = { _ ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(PanelRaised)
                        .border(1.dp, Outline, RoundedCornerShape(999.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accent.copy(alpha = 0.58f),
                                        accent,
                                        Color.White.copy(alpha = 0.88f),
                                    ),
                                ),
                            ),
                    )
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatDuration(safePosition), color = accent.copy(alpha = 0.95f), style = MaterialTheme.typography.labelMedium)
            Text(formatDuration(safeDuration), color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

internal fun clampTimelinePosition(positionMs: Long, durationMs: Long): Long {
    val safeDuration = durationMs.coerceAtLeast(0L)
    val upperBound = safeDuration.takeIf { it > 0L } ?: positionMs.coerceAtLeast(0L)
    return positionMs.coerceIn(0L, upperBound)
}

internal fun useCompactPlayerLayout(widthDp: Int): Boolean = widthDp < 960

internal fun useCompactParserCardLayout(widthDp: Int): Boolean = widthDp < 860

internal fun useCompactTrackRowLayout(widthDp: Int): Boolean = widthDp < 760

private fun derivePlaylistArtworkHint(name: String, artworkHint: String): String {
    val resolved = artworkHint.trim().ifBlank { name.take(2) }.uppercase()
    return resolved.take(2).padEnd(2, ' ').trim().ifBlank { "PL" }
}

private fun volumeIconFor(volume: Float) = when {
    volume <= 0.001f -> Icons.AutoMirrored.Rounded.VolumeOff
    volume < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
    else -> Icons.AutoMirrored.Rounded.VolumeUp
}

internal fun timelineKeyboardStepMs(durationMs: Long): Long =
    (durationMs / 24L).coerceIn(3_000L, 12_000L)

private fun idleValue(index: Int, phase: Float, dense: Boolean): Float {
    val seed = if (dense) 0.22f else 0.16f
    val wave = kotlin.math.sin((phase * 6.28318f) + index * 0.45f)
    return (seed + (wave + 1f) * 0.08f).coerceIn(0f, 0.35f)
}

@Composable
private fun animateColor(selected: Boolean): androidx.compose.runtime.State<Color> {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(180),
        label = "nav-color-progress",
    )
    return remember(progress) { mutableStateOf(lerp(Muted, Moss, progress)) }
}
