package com.yaneodex.desktop.ui

import com.yaneodex.desktop.ui.theme.Wd2
import com.yaneodex.desktop.ui.theme.Wd2Fonts
import com.yaneodex.desktop.ui.theme.Wd2Radius
import com.yaneodex.desktop.ui.theme.YaNeoDexMark
import com.yaneodex.desktop.ui.theme.YdxGlyph
import com.yaneodex.desktop.ui.theme.YdxIcon

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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// ctOS shell — near-black ground, white type, red signal
private val Panel = Wd2.Panel
private val PanelRaised = Wd2.PanelRaised
private val Outline = Wd2.LineDim
private val Muted = Wd2.Muted
private val TextPrimary = Wd2.Text
private val TextDim = Wd2.TextDim
private val Moss = Wd2.Text        // "live" reads as white pulse
private val Gold = Wd2.Accent      // red

/** How many sections the back/forward history keeps. */
private const val NAV_HISTORY_LIMIT = 20

/** Single band count everywhere, so the visualizer never thrashes between 24 and 32. */
private const val VISUALIZER_BANDS = 32
private const val DEFAULT_FRAME_SECONDS = 1f / 60f
private const val ATTACK_PER_FRAME = 0.45f
private const val RELEASE_PER_FRAME = 0.14f
private const val PEAK_FALL_PER_SECOND = 0.6f
private val Sky = Wd2.TextDim
private val Coral = Wd2.Warn

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
    val icon: YdxGlyph,
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

    // Navigation behaves like a browser: back/forward over the sections you actually visited.
    var backStack by remember { mutableStateOf(emptyList<DesktopSection>()) }
    var forwardStack by remember { mutableStateOf(emptyList<DesktopSection>()) }
    var lastSection by remember { mutableStateOf(state.selectedSection) }

    // Sidebar and queue rail are chrome the listener can fold away; both default to open.
    var sidebarCollapsed by remember { mutableStateOf(false) }
    var queuePanelOpen by remember { mutableStateOf(true) }
    var searchFocused by remember { mutableStateOf(false) }
    val shortcutFocus = remember { FocusRequester() }

    LaunchedEffect(state.selectedSection) {
        if (state.selectedSection != lastSection) {
            backStack = (backStack + lastSection).takeLast(NAV_HISTORY_LIMIT)
            forwardStack = emptyList()
            lastSection = state.selectedSection
        }
    }

    LaunchedEffect(Unit) { runCatching { shortcutFocus.requestFocus() } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Wd2.Bg)
            .focusRequester(shortcutFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    // Space is the universal play/pause, unless the listener is typing.
                    event.key == Key.Spacebar && !searchFocused && !event.isCtrlPressed -> {
                        onTogglePlayPause()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.DirectionRight -> {
                        onPlayNext()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.DirectionLeft -> {
                        onPlayPrevious()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.Spacebar -> {
                        onTogglePlayPause()
                        true
                    }
                    else -> false
                }
            },
    ) {
        val metrics = remember(maxWidth, sidebarCollapsed, queuePanelOpen) {
            val base = when {
                maxWidth < 1180.dp -> LayoutMetrics(10.dp, 10.dp, 200.dp, 220.dp, 280.dp, 170.dp, true)
                maxWidth < 1480.dp -> LayoutMetrics(12.dp, 12.dp, 220.dp, 250.dp, 360.dp, 220.dp, false)
                else -> LayoutMetrics(14.dp, 14.dp, 240.dp, 280.dp, 430.dp, 260.dp, false)
            }
            base.copy(sidebarWidth = if (sidebarCollapsed) 68.dp else base.sidebarWidth)
        }

        // Static, very low-contrast atmosphere. The animated CRT layer is gone: it repainted the
        // whole window forever, including while paused, and was a large part of the harsh look.
        CrtAtmosphere()
        Row(
            modifier = Modifier.fillMaxSize().padding(metrics.pagePadding),
            horizontalArrangement = Arrangement.spacedBy(metrics.sectionGap),
        ) {
            Sidebar(
                state = state,
                strings = strings,
                metrics = metrics,
                collapsed = sidebarCollapsed,
                onToggleCollapsed = { sidebarCollapsed = !sidebarCollapsed },
                onSelectSection = onSelectSection,
                onLanguageChange = onLanguageChange,
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
            ) {
                TopBar(
                    query = state.searchQuery,
                    strings = strings,
                    metrics = metrics,
                    canGoBack = backStack.isNotEmpty(),
                    canGoForward = forwardStack.isNotEmpty(),
                    queuePanelOpen = queuePanelOpen,
                    onNavigateBack = {
                        val previous = backStack.lastOrNull() ?: return@TopBar
                        backStack = backStack.dropLast(1)
                        forwardStack = forwardStack + state.selectedSection
                        lastSection = previous
                        onSelectSection(previous)
                    },
                    onNavigateForward = {
                        val next = forwardStack.lastOrNull() ?: return@TopBar
                        forwardStack = forwardStack.dropLast(1)
                        backStack = backStack + state.selectedSection
                        lastSection = next
                        onSelectSection(next)
                    },
                    onToggleQueuePanel = { queuePanelOpen = !queuePanelOpen },
                    onSearchChange = onSearchChange,
                    onSearchFocusChange = { searchFocused = it },
                    onRunParserSearch = onRunParserSearch,
                )
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
                        onLanguageChange = onLanguageChange,
                    )
                    if (queuePanelOpen) {
                        RightRail(state, strings, metrics, onToggleShuffle, onPlayTrack)
                    }
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
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onSelectSection: (DesktopSection) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(metrics.sidebarWidth)
            .fillMaxHeight(),
        color = Panel,
        shape = RoundedCornerShape(Wd2Radius.lg),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (collapsed) 8.dp else 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                YaNeoDexMark(boxSize = 22.dp)
                if (!collapsed) {
                    // Wordmark, not a headline: tight caps with wide tracking reads as a logo.
                    Text(
                        "YANEODEX",
                        color = TextPrimary,
                        fontFamily = Wd2Fonts.Content,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 2.4.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                RoundAction(
                    icon = YdxGlyph.Panel,
                    active = false,
                    onClick = onToggleCollapsed,
                    size = 30.dp,
                    iconSize = 18.dp,
                    subdued = true,
                    mirrored = collapsed,
                )
            }

            if (!collapsed) {
                Text(
                    "OFFLINE NODE · BUILD 0.1.2",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 34.dp, bottom = 8.dp),
                    maxLines = 1,
                )
            }

            SidebarNavigation(state.selectedSection, strings, collapsed, onSelectSection)
            Spacer(Modifier.weight(1f))
            LanguageSwitcher(state.language, strings, collapsed, onLanguageChange)
        }
    }
}

@Composable
private fun SidebarNavigation(
    selectedSection: DesktopSection,
    strings: DesktopStrings,
    collapsed: Boolean,
    onSelectSection: (DesktopSection) -> Unit,
) {
    // Labels read as language now; the terminal index stays as a quiet mono detail.
    val items = listOf(
        NavItem(DesktopSection.HOME, strings.navHome, "01", YdxGlyph.Home),
        NavItem(DesktopSection.SEARCH, strings.navSearch, "02", YdxGlyph.Search),
        NavItem(DesktopSection.PLAYLISTS, strings.navPlaylists, "03", YdxGlyph.Playlist),
        NavItem(DesktopSection.LIBRARY, strings.navLibrary, "04", YdxGlyph.Library),
        NavItem(DesktopSection.IMPORT, strings.navImport, "05", YdxGlyph.Import),
        NavItem(DesktopSection.SETTINGS, strings.navSettings, "06", YdxGlyph.Settings),
    )
    val selectedIndex = items.indexOfFirst { it.section == selectedSection }.coerceAtLeast(0)
    val itemHeight = 40.dp
    val itemSpacing = 2.dp
    val highlightOffset by animateDpAsState(
        targetValue = (itemHeight + itemSpacing) * selectedIndex,
        animationSpec = tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "sidebar-selection-offset",
    )

    Box {
        Row(
            modifier = Modifier
                .padding(top = highlightOffset)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(Wd2Radius.md))
                .background(Wd2.AccentSoft),
        ) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(Wd2.Accent)) {}
            Spacer(Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(itemSpacing)) {
            items.forEach { item ->
                NavPill(
                    label = item.label,
                    index = item.index,
                    icon = item.icon,
                    collapsed = collapsed,
                    selected = item.section == selectedSection,
                    onClick = { onSelectSection(item.section) },
                )
            }
        }
    }
}

private data class NavItem(
    val section: DesktopSection,
    val label: String,
    val index: String,
    val icon: YdxGlyph,
)

@Composable
private fun TopBar(
    query: String,
    strings: DesktopStrings,
    metrics: LayoutMetrics,
    canGoBack: Boolean,
    canGoForward: Boolean,
    queuePanelOpen: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onToggleQueuePanel: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onRunParserSearch: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Browser-style history controls, the way Spotify's desktop shell does it.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundAction(
                icon = YdxGlyph.Back,
                active = false,
                onClick = onNavigateBack,
                enabled = canGoBack,
                size = 34.dp,
                iconSize = 20.dp,
                subdued = true,
            )
            RoundAction(
                icon = YdxGlyph.Forward,
                active = false,
                onClick = onNavigateForward,
                enabled = canGoForward,
                size = 34.dp,
                iconSize = 20.dp,
                subdued = true,
            )
        }

        Text(
            strings.topTitle,
            color = TextPrimary,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            modifier = Modifier
                .width(metrics.searchWidth)
                .clip(RoundedCornerShape(Wd2Radius.pill))
                .background(PanelRaised)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            YdxIcon(YdxGlyph.Search, tint = Muted, boxSize = 20.dp)
            BasicTextField(
                value = query,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onSearchFocusChange(it.isFocused) },
                singleLine = true,
                cursorBrush = SolidColor(Wd2.Accent),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                decorationBox = { inner ->
                    if (query.isBlank()) {
                        Text(strings.searchPlaceholder, color = Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    inner()
                },
            )
            if (query.isNotBlank()) {
                RoundAction(
                    icon = YdxGlyph.Search,
                    active = true,
                    onClick = { onRunParserSearch(query) },
                    size = 28.dp,
                    iconSize = 17.dp,
                )
            }
        }

        RoundAction(
            icon = YdxGlyph.Queue,
            active = queuePanelOpen,
            onClick = onToggleQueuePanel,
            size = 34.dp,
            iconSize = 20.dp,
        )
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
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Surface(modifier = modifier, color = Panel, shape = RoundedCornerShape(Wd2Radius.lg)) {
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
                        // Directional motion: content follows the sidebar instead of just blinking.
                        val forward = targetState.ordinal >= initialState.ordinal
                        val enter = slideInHorizontally(
                            animationSpec = tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            initialOffsetX = { width -> if (forward) (width / 12) else -(width / 12) },
                        ) + fadeIn(animationSpec = tween(220))
                        val exit = slideOutHorizontally(
                            animationSpec = tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            targetOffsetX = { width -> if (forward) -(width / 16) else (width / 16) },
                        ) + fadeOut(animationSpec = tween(160))
                        enter togetherWith exit
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
                                // Only surface the parser line when it actually reports something
                                // (progress, a result count, a failure). At rest it used to read
                                // "Запусти поиск" forever, which is instruction noise, not status.
                                if (state.parserStatus.isNotBlank()) {
                                    StatusCard(strings.sectionParser, state.parserStatus)
                                }
                                if (state.parserResults.isNotEmpty()) {
                                    SectionTitle(strings.sectionParserResults)
                                    ParserResults(state.parserResults, strings, onParserResultClick, onParserPreview, onParserDownload, onParserAddToPlaylist)
                                }
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
                        DesktopSection.SETTINGS -> {
                            SettingsSection(
                                state = state,
                                strings = strings,
                                onLanguageChange = onLanguageChange,
                                onOcrServerUrlChange = onOcrServerUrlChange,
                                onOcrTokenChange = onOcrTokenChange,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    state: DesktopUiState,
    strings: DesktopStrings,
    onLanguageChange: (AppLanguage) -> Unit,
    onOcrServerUrlChange: (String) -> Unit,
    onOcrTokenChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Wd2Radius.lg))
            .background(Panel)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(strings.settingsTitle, color = TextPrimary, style = MaterialTheme.typography.headlineLarge)
        Text(strings.settingsAbout, color = TextDim, style = MaterialTheme.typography.bodyMedium)
        SettingsDivider()
        Text(strings.settingsLanguage, color = TextDim, style = MaterialTheme.typography.labelLarge)
        LanguageSwitcher(state.language, strings, onLanguageChange = onLanguageChange)
        SettingsDivider()
        Text(strings.sectionOcrSettings, color = TextDim, style = MaterialTheme.typography.labelLarge)
        LabeledField(strings.ocrServerLabel, state.ocrSettings.serverUrl, strings.ocrServerPlaceholder, onOcrServerUrlChange)
        LabeledField(strings.bearerTokenLabel, state.ocrSettings.authToken, strings.bearerTokenPlaceholder, onOcrTokenChange)
        SettingsDivider()
        // Shortcuts used to be invisible; surfacing them is most of what makes the shell feel friendly.
        Text(strings.settingsShortcuts, color = TextDim, style = MaterialTheme.typography.labelLarge)
        ShortcutRow("Space", strings.shortcutPlayPause)
        ShortcutRow("Ctrl + ←", strings.shortcutPrevious)
        ShortcutRow("Ctrl + →", strings.shortcutNext)
        ShortcutRow("← / →", strings.shortcutSeek)
        SettingsDivider()
        Text(strings.settingsLibrary, color = TextDim, style = MaterialTheme.typography.labelLarge)
        Text(
            buildLibraryStatusText(state, strings),
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Wd2.LineDim),
    )
}

@Composable
private fun ShortcutRow(keys: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(description, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Text(
            keys,
            color = TextDim,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(Wd2Radius.sm))
                .background(PanelRaised)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Hero(state: DesktopUiState, strings: DesktopStrings, onPlayPlaylist: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(Wd2Radius.lg))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Wd2.PanelRaised,
                        Wd2.Panel,
                    ),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (state.isPlaying) Wd2.Accent else Muted),
                        )
                        Text(
                            if (state.isPlaying) "Сейчас играет" else "Пауза",
                            color = if (state.isPlaying) Wd2.Accent else Muted,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Crossfade(state.currentTrack?.title ?: state.spotlight.title, label = "hero-track-title") { title ->
                        Text(
                            title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.displayLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    state.currentTrack?.artist?.let { artist ->
                        Text(
                            artist,
                            color = TextDim,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AccentAction(strings.playLaneAction, YdxGlyph.Play, Moss, onPlayPlaylist)
                }
            }
            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(Wd2Radius.md))
                    .background(Wd2.Bg),
            ) {
                PlaybackVisualizer(
                    state = state.visualizer,
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(112.dp).padding(horizontal = 14.dp),
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
            .clip(RoundedCornerShape(Wd2Radius.lg))
            .background(
                Brush.linearGradient(
                    listOf(
                        PanelRaised,
                        Wd2.Panel,
                        Wd2.Bg,
                    ),
                ),
            )
            .padding(28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(strings.onboardingTitle, color = TextPrimary, style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentAction(strings.onboardingPrimaryAction, YdxGlyph.Folder, Moss, onImportLibraryFolders)
                AccentAction(strings.onboardingSecondaryAction, YdxGlyph.Refresh, Gold, onRefreshLibrary)
                AccentAction(strings.onboardingParserAction, YdxGlyph.Search, Sky, onOpenSearch)
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
            AccentAction(strings.createAction, YdxGlyph.Playlist, Moss) {
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
                TrackBulkActionSpec(strings.removeAction, YdxGlyph.Delete, Gold, onRemoveTracksFromPlaylist),
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
            AccentAction(strings.addFoldersAction, YdxGlyph.Folder, Moss, onImportLibraryFolders)
            AccentAction(strings.refreshAction, YdxGlyph.Refresh, Gold, onRefreshLibrary)
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
                TrackBulkActionSpec(strings.addAction, YdxGlyph.Playlist, Moss, onAddTracksToPlaylist),
                TrackBulkActionSpec(strings.deleteAction, YdxGlyph.Delete, Coral, onDeleteTracksFromLibrary),
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
        Surface(shape = RoundedCornerShape(Wd2Radius.md), color = Panel) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(strings.ocrServerLabel, state.ocrSettings.serverUrl, strings.ocrServerPlaceholder, onOcrServerUrlChange)
                LabeledField(strings.bearerTokenLabel, state.ocrSettings.authToken, strings.bearerTokenPlaceholder, onOcrTokenChange)
                AccentAction(strings.chooseScreenshotsAction, YdxGlyph.Folder, Moss, onPickScreenshots)
            }
        }
        if (state.ocrStatus.isNotBlank()) {
            StatusCard(strings.sectionOcr, state.ocrStatus)
        }
        if (state.importMatches.isNotEmpty()) {
            SectionTitle(strings.sectionImportReview)
            ImportMatches(state.importMatches, strings)
        }
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
    val upNext = state.playbackQueue.take(if (metrics.compact) 3 else 6)
    val statuses = listOf(state.parserStatus, state.ocrStatus).filter { it.isNotBlank() }

    Surface(
        modifier = Modifier.width(metrics.rightRailWidth).fillMaxHeight(),
        color = Panel,
        shape = RoundedCornerShape(Wd2Radius.lg),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.sectionNowShaping, color = Muted, style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkBadge(
                    label = state.currentTrack?.title?.take(2)?.uppercase() ?: "YN",
                    color = Wd2.Accent,
                    compact = true,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        state.currentTrack?.title ?: strings.noTrackSelected,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    state.currentTrack?.artist?.let { artist ->
                        Text(
                            artist,
                            color = TextDim,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            SettingsDivider()

            // Queue summary, with shuffle as an actual button. It used to be one big red-tinted
            // block carrying both the count and the label, which shouted louder than the track
            // that is playing.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${strings.queueLabel} · ${state.playbackQueue.size}",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${strings.shuffleLabel}: " +
                            if (state.shuffleEnabled) strings.shuffleOn else strings.shuffleOff,
                        color = if (state.shuffleEnabled) Wd2.Accent else Muted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                RoundAction(
                    icon = YdxGlyph.Shuffle,
                    active = state.shuffleEnabled,
                    onClick = onToggleShuffle,
                    size = 34.dp,
                    iconSize = 19.dp,
                )
            }

            if (statuses.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Wd2Radius.md))
                        .background(Wd2.BgElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    statuses.forEach { status ->
                        Text(
                            status,
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            SettingsDivider()

            Text(strings.sectionUpNext, color = Muted, style = MaterialTheme.typography.labelLarge)
            if (upNext.isEmpty()) {
                Text(strings.noTrackSelected, color = Muted, style = MaterialTheme.typography.bodySmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    upNext.forEachIndexed { index, track ->
                        QueueRow(index = index + 1, track = track, onPlayTrack = onPlayTrack, compact = true)
                    }
                }
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
    // Player dock — raised surface with a hairline top edge instead of a full white frame.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Wd2Radius.lg))
            .background(Wd2.BgElevated),
    ) {
        // Status strip: a small live dot plus volume, no full-width red bar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Wd2.Panel)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (state.isPlaying) Wd2.Accent else Muted),
                )
                Text(
                    if (state.isPlaying) "Воспроизведение" else "Остановлено",
                    color = if (state.isPlaying) TextPrimary else Muted,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                "VOL ${(state.playbackVolume * 100).roundToInt()}%",
                color = Muted,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compactPlayer = useCompactPlayerLayout(maxWidth.value.toInt())
            if (compactPlayer) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtworkBadge(state.currentTrack?.title?.take(2)?.uppercase() ?: "YN", Wd2.Accent, compact = true)
                        Column(modifier = Modifier.weight(1f)) {
                            Crossfade(state.currentTrack?.title ?: strings.noTrackSelected, label = "bottom-track-title-compact") { title ->
                                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            state.currentTrack?.artist?.let { artist ->
                                Text(
                                    artist,
                                    color = TextDim,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        PlaybackVisualizer(
                            state = state.visualizer,
                            modifier = Modifier.width(88.dp).height(36.dp),
                            accent = Wd2.Accent,
                            dense = true,
                        )
                    }
                    PlaybackTimeline(
                        positionMs = state.playbackPositionMs,
                        durationMs = state.playbackDurationMs,
                        accent = Wd2.Accent,
                        onSeek = onSeekPlayback,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PixelAction(YdxGlyph.Shuffle, state.shuffleEnabled, onToggleShuffle)
                        VolumeControl(volume = state.playbackVolume, accent = Wd2.Accent, compact = true, onChange = onSetPlaybackVolume)
                        Spacer(Modifier.weight(1f))
                        PixelAction(YdxGlyph.Prev, false, onPlayPrevious)
                        PixelAction(if (state.isPlaying) YdxGlyph.Pause else YdxGlyph.Play, true, onTogglePlayPause, primary = true)
                        PixelAction(YdxGlyph.Next, false, onPlayNext)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.widthIn(max = metrics.bottomInfoWidth + 40.dp),
                    ) {
                        ArtworkBadge(state.currentTrack?.title?.take(2)?.uppercase() ?: "YN", Wd2.Accent)
                        Column {
                            Crossfade(state.currentTrack?.title ?: strings.noTrackSelected, label = "bottom-track-title") { title ->
                                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            state.currentTrack?.artist?.let { artist ->
                                Text(
                                    artist,
                                    color = TextDim,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    PlaybackVisualizer(
                        state = state.visualizer,
                        modifier = Modifier.width(120.dp).height(40.dp),
                        accent = Wd2.Accent,
                        dense = true,
                    )
                    PlaybackTimeline(
                        positionMs = state.playbackPositionMs,
                        durationMs = state.playbackDurationMs,
                        accent = Wd2.Accent,
                        onSeek = onSeekPlayback,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        VolumeControl(volume = state.playbackVolume, accent = Wd2.Accent, compact = false, onChange = onSetPlaybackVolume)
                        PixelAction(YdxGlyph.Shuffle, state.shuffleEnabled, onToggleShuffle)
                        PixelAction(YdxGlyph.Prev, false, onPlayPrevious)
                        PixelAction(if (state.isPlaying) YdxGlyph.Pause else YdxGlyph.Play, true, onTogglePlayPause, primary = true)
                        PixelAction(YdxGlyph.Next, false, onPlayNext)
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSwitcher(
    language: AppLanguage,
    strings: DesktopStrings,
    collapsed: Boolean = false,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Surface(shape = RoundedCornerShape(Wd2Radius.md), color = PanelRaised) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (collapsed) 6.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (collapsed) {
                YdxIcon(
                    YdxGlyph.Globe,
                    tint = TextDim,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    boxSize = 20.dp,
                    contentDescription = strings.languageLabel,
                )
                LanguageButton("R", language == AppLanguage.RU) { onLanguageChange(AppLanguage.RU) }
                LanguageButton("E", language == AppLanguage.EN) { onLanguageChange(AppLanguage.EN) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    YdxIcon(YdxGlyph.Globe, tint = TextDim, boxSize = 19.dp)
                    Text(strings.languageLabel, color = TextDim, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageButton("RU", language == AppLanguage.RU) { onLanguageChange(AppLanguage.RU) }
                    LanguageButton("EN", language == AppLanguage.EN) { onLanguageChange(AppLanguage.EN) }
                }
            }
        }
    }
}

@Composable
private fun LanguageButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Wd2Radius.sm))
            .background(if (selected) Wd2.Accent else Wd2.Bg)
            .pressClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Wd2.AccentText else TextDim,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
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
                    .clip(RoundedCornerShape(Wd2Radius.md))
                    .background(if (playlist.id == selectedPlaylistId) parseTone(playlist.tone).copy(alpha = 0.12f) else Panel)
                    .border(1.dp, if (playlist.id == selectedPlaylistId) parseTone(playlist.tone).copy(alpha = 0.34f) else Outline, RoundedCornerShape(Wd2Radius.md))
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
                            .clip(RoundedCornerShape(Wd2Radius.md))
                            .background(PanelRaised)
                            .border(1.dp, Outline, RoundedCornerShape(Wd2Radius.md))
                            .pressClickable { onEditPlaylist(playlist) }
                            .padding(10.dp),
                    ) {
                        YdxIcon(YdxGlyph.Edit, tint = TextPrimary, boxSize = 19.dp, contentDescription = strings.editAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistRecord, onClick: () -> Unit) {
    // Used to be a full-bleed gradient slab with 32sp black serif initials — the loudest and
    // crudest thing on the page. Now it is a quiet tile with one muted note glyph, the way a
    // missing cover is handled everywhere else.
    Surface(
        modifier = Modifier.width(200.dp).pressClickable(onClick = onClick),
        shape = RoundedCornerShape(Wd2Radius.lg),
        color = Panel,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(RoundedCornerShape(Wd2Radius.md))
                    .background(PanelRaised),
                contentAlignment = Alignment.Center,
            ) {
                YdxIcon(
                    YdxGlyph.Library,
                    tint = parseTone(playlist.tone).copy(alpha = 0.5f),
                    boxSize = 40.dp,
                    stroke = 1.4.dp,
                )
            }
            Text(
                playlist.name,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            shape = RoundedCornerShape(Wd2Radius.md),
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
                        icon = if (isEditing) YdxGlyph.Edit else YdxGlyph.Playlist,
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
    val trackIdsKey = remember(tracks) { tracks.map { it.id } }
    var selectedTrackIds by remember(trackIdsKey) { mutableStateOf(emptySet<String>()) }
    val selectionEnabled = bulkActions.isNotEmpty()
    val selectionMode = selectionEnabled && selectedTrackIds.isNotEmpty()
    // Cap height so LazyColumn can virtualize inside parent verticalScroll safely.
    val listMaxHeight = when {
        tracks.size <= 8 -> (tracks.size * 56).coerceAtLeast(100).dp
        tracks.size <= 40 -> 400.dp
        else -> 480.dp
    }

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
        if (tracks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = listMaxHeight),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    val selected = track.id in selectedTrackIds
                    val rowInteraction = remember { MutableInteractionSource() }
                    val rowHovered by rowInteraction.collectIsHoveredAsState()
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(Wd2Radius.md))
                            .background(
                                when {
                                    selected -> Gold.copy(alpha = 0.10f)
                                    track.id == currentTrackId -> Moss.copy(alpha = 0.06f)
                                    rowHovered -> Wd2.PanelHover
                                    else -> Color.Transparent
                                },
                            )
                            .hoverable(interactionSource = rowInteraction)
                            .pressClickable {
                                if (selectionMode) {
                                    selectedTrackIds = if (selected) selectedTrackIds - track.id else selectedTrackIds + track.id
                                } else {
                                    onPlayTrack(track.id)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        val compactRow = useCompactTrackRowLayout(maxWidth.value.toInt())
                        if (compactRow) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
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
    Surface(shape = RoundedCornerShape(Wd2Radius.md), color = PanelRaised) {
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
            .clip(RoundedCornerShape(Wd2Radius.md))
            .background(action.accent.copy(alpha = 0.12f))
            .border(1.dp, action.accent.copy(alpha = 0.32f), RoundedCornerShape(Wd2Radius.md))
            .pressClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YdxIcon(action.icon, tint = action.accent, boxSize = 18.dp, contentDescription = action.label)
        Text(action.label, color = TextPrimary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SelectionToggleButton(selected: Boolean, onClick: () -> Unit) {
    // One ring, not a ring inside a ring — the old version drew a bordered circle around
    // a circular icon, which is where a lot of the "cramped" feel came from.
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .pressClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        YdxIcon(
            if (selected) YdxGlyph.CheckCircle else YdxGlyph.Circle,
            tint = if (selected) Gold else Muted,
            boxSize = 20.dp,
        )
    }
}

@Composable
private fun RowActionChip(trackId: String, rowActionLabel: String, onRowAction: (String) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Wd2Radius.md))
            .background(PanelRaised)
            .border(1.dp, Outline, RoundedCornerShape(Wd2Radius.md))
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
    if (results.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        results.forEach { item ->
            BoxWithConstraints {
                val compactCard = useCompactParserCardLayout(maxWidth.value.toInt())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (compactCard) 108.dp else 94.dp)
                        .clip(RoundedCornerShape(Wd2Radius.md))
                        .background(Panel)
                        .border(1.dp, Outline, RoundedCornerShape(Wd2Radius.md))
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
            Surface(shape = RoundedCornerShape(Wd2Radius.md), color = Panel) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.recognized.rawText, color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    item.message?.let { message ->
                        Text(message, color = accent, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    item.bestMatch?.let { match ->
                        Text("${match.artist} - ${match.title}", color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    index: Int,
    track: TrackRecord,
    onPlayTrack: (String) -> Unit,
    compact: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 40.dp else 54.dp)
            .clip(RoundedCornerShape(Wd2Radius.md))
            .background(
                when {
                    hovered -> Wd2.PanelHover
                    compact -> Color.Transparent
                    else -> PanelRaised
                },
            )
            .hoverable(interactionSource = interactionSource)
            .pressClickable { onPlayTrack(track.id) }
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 5.dp else 9.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A quiet position number reads as a queue. It used to be a lone initial letter pinned
        // into a 14dp column, which looked like a badge that had failed to load.
        Text(
            index.toString().padStart(2, '0'),
            color = if (hovered) Wd2.Accent else Muted.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(18.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(track.title, color = TextPrimary, style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = TextDim, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TextInput(value: String, placeholder: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(RoundedCornerShape(Wd2Radius.md))
            .background(PanelRaised)
            .border(1.dp, Outline, RoundedCornerShape(Wd2Radius.md))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
        cursorBrush = SolidColor(Wd2.Accent),
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
        Text(label, color = TextDim, style = MaterialTheme.typography.labelLarge)
        TextInput(value, placeholder, Modifier.fillMaxWidth(), onChange)
    }
}

@Composable
private fun StatusCard(title: String, body: String) {
    Surface(shape = RoundedCornerShape(Wd2Radius.md), color = Panel) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = TextDim, style = MaterialTheme.typography.labelLarge)
            Text(body, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(Wd2Radius.pill)).background(Wd2.Accent))
        Text(title, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun NavPill(
    label: String,
    index: String,
    icon: YdxGlyph,
    collapsed: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val tint by animateColor(selected = selected)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(Wd2Radius.md))
            .background(if (!selected && hovered) Wd2.PanelHover else Color.Transparent)
            .hoverable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YdxIcon(icon, tint = tint, boxSize = 22.dp, contentDescription = label)
        if (!collapsed) {
            Text(
                label,
                color = if (selected) TextPrimary else TextDim,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Quiet terminal index — keeps the ctOS flavour without shouting it.
            Text(
                index,
                color = if (selected) Wd2.Accent.copy(alpha = 0.85f) else Muted.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun AccentAction(label: String, icon: YdxGlyph, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val isPrimary = color == Gold || color == Wd2.Accent

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Wd2Radius.pill))
            .background(
                when {
                    isPrimary -> if (hovered) Wd2.Accent else Wd2.AccentDim
                    hovered -> Wd2.PanelHover
                    else -> PanelRaised
                },
            )
            .hoverable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YdxIcon(
            icon,
            tint = if (isPrimary) Wd2.AccentText else TextDim,
            boxSize = 19.dp,
            contentDescription = label,
        )
        Text(
            label,
            color = if (isPrimary) Wd2.AccentText else TextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun SecondaryAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Wd2Radius.md))
            .background(PanelRaised)
            .border(1.dp, Outline, RoundedCornerShape(Wd2Radius.md))
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
                    shape = RoundedCornerShape(Wd2Radius.md),
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
            .background(color)
            .border(1.dp, Wd2.Line)
            .pressClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AccentChip(label: String, background: Color, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Wd2Radius.md))
            .background(background)
            .border(1.dp, Outline, RoundedCornerShape(Wd2Radius.md))
            .pressClickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Track artwork placeholder.
 *
 * This used to be a coloured box inside a coloured border with heavy black initials —
 * three competing edges on a 40dp square, which is why a list of tracks read as a grid of
 * little logos. Now it is one soft tile with quiet mono initials, so the column reads as
 * text and the artwork area stops shouting.
 */
@Composable
private fun ArtworkBadge(label: String, color: Color, compact: Boolean = false) {
    val size = if (compact) 40.dp else 48.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(Wd2Radius.md))
            .background(PanelRaised),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = color.copy(alpha = 0.7f),
            fontFamily = Wd2Fonts.Meta,
            fontWeight = FontWeight.Medium,
            fontSize = if (compact) 11.sp else 12.sp,
            letterSpacing = 1.sp,
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
private fun PixelAction(
    icon: YdxGlyph,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    RoundAction(icon = icon, active = active, onClick = onClick, modifier = modifier, primary = primary)
}

@Composable
private fun RoundAction(
    icon: YdxGlyph,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = if (primary) 48.dp else 40.dp,
    iconSize: androidx.compose.ui.unit.Dp = if (primary) 26.dp else 22.dp,
    /** Quiet chrome buttons (history, collapse) that should not compete with the accent. */
    subdued: Boolean = false,
    /** Flips the glyph horizontally — used for the sidebar toggle in its collapsed state. */
    mirrored: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.93f
            hovered -> 1.05f
            else -> 1f
        },
        animationSpec = tween(120, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "player-button-scale",
    )

    val bg = when {
        primary && active -> Wd2.Accent
        primary -> Wd2.AccentDim
        active -> Wd2.AccentSoft
        hovered && enabled -> Wd2.PanelHover
        else -> Color.Transparent
    }
    val shape = RoundedCornerShape(if (primary) Wd2Radius.pill else Wd2Radius.md)
    val contentColor = when {
        !enabled -> Muted.copy(alpha = 0.4f)
        primary -> Wd2.AccentText
        active -> Wd2.Accent
        hovered -> TextPrimary
        subdued -> TextDim
        else -> TextPrimary
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
            .size(size)
            .clip(shape)
            .background(bg)
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        YdxIcon(
            icon,
            tint = contentColor,
            modifier = if (mirrored) Modifier.graphicsLayer { scaleX = -1f } else Modifier,
            boxSize = iconSize,
        )
    }
}

/**
 * Static ctOS atmosphere: a faint grid and a soft red glow in two corners.
 *
 * Previously this was an animated CRT layer (moving interference band, 48 redrawn speckles,
 * scanlines every 3px) on a 900ms loop. It repainted the entire window forever — including
 * while paused — and its high-frequency noise was a big part of the harsh look. The texture
 * stays, the churn does not.
 */
@Composable
private fun CrtAtmosphere() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridStep = 28f
        var x = 0f
        while (x < size.width) {
            drawRect(Wd2.LineFaint, Offset(x, 0f), Size(1f, size.height))
            x += gridStep
        }
        var y = 0f
        while (y < size.height) {
            drawRect(Wd2.LineFaint, Offset(0f, y), Size(size.width, 1f))
            y += gridStep
        }
        drawCircle(
            brush = Brush.radialGradient(listOf(Wd2.Accent.copy(alpha = 0.05f), Color.Transparent)),
            radius = size.minDimension * 0.6f,
            center = Offset(size.width * 0.06f, size.height * 0.08f),
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Wd2.Accent.copy(alpha = 0.035f), Color.Transparent)),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * 0.94f, size.height * 0.92f),
        )
    }
}

@Composable
private fun AmbientGlow() = CrtAtmosphere()

private fun parseTone(value: String): Color = when (value) {
    "#95F15A", "#A7F46A" -> Moss
    "#E7C669", "#FF0000", "#F00" -> Gold
    "#7CC8FF" -> Sky
    "#E58B6B" -> Coral
    else -> Wd2.TextDim
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

/**
 * Player visualizer.
 *
 * Spectrum frames arrive from the backend at roughly 30 fps. Instead of drawing those samples
 * as stepped blocks, the canvas runs on the display's own frame clock and eases a smoothed
 * band array toward the latest target, so the motion is continuous no matter how coarse the
 * incoming data is. Idle state is honest: flat line, no fake motion.
 */
@Composable
private fun PlaybackVisualizer(
    state: PlaybackVisualizerState,
    modifier: Modifier,
    accent: Color,
    dense: Boolean,
) {
    val bandCount = VISUALIZER_BANDS
    val latest = rememberUpdatedState(state)
    val rendered = remember { FloatArray(bandCount) }
    val peaks = remember { FloatArray(bandCount) }
    val frameTick = remember { mutableStateOf(0L) }
    val elapsed = remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrame = 0L
        while (true) {
            withFrameNanos { now ->
                val dtSeconds = if (lastFrame == 0L) {
                    DEFAULT_FRAME_SECONDS
                } else {
                    ((now - lastFrame) / 1_000_000_000.0).toFloat().coerceIn(0.002f, 0.05f)
                }
                lastFrame = now

                val snapshot = latest.value
                var moving = false
                for (index in 0 until bandCount) {
                    val target = snapshot.bands.getOrNull(index)?.coerceIn(0f, 1f) ?: 0f
                    val current = rendered[index]
                    // Fast attack, slower release — the classic meter feel.
                    val rate = if (target > current) {
                        1f - (1f - ATTACK_PER_FRAME).pow(dtSeconds / DEFAULT_FRAME_SECONDS)
                    } else {
                        1f - (1f - RELEASE_PER_FRAME).pow(dtSeconds / DEFAULT_FRAME_SECONDS)
                    }
                    val next = current + (target - current) * rate
                    rendered[index] = next
                    peaks[index] = maxOf(peaks[index] - dtSeconds * PEAK_FALL_PER_SECOND, next)
                    if (kotlin.math.abs(next - target) > 0.002f || next > 0.002f) moving = true
                }

                if (snapshot.active) elapsed.value = (elapsed.value + dtSeconds) % 1000f
                // Only invalidate when something is actually changing, so an idle player costs nothing.
                if (moving || snapshot.active) frameTick.value = now
            }
        }
    }

    val tick = frameTick.value
    val phase = elapsed.value
    val live = state.spectrumLive
    val active = state.active

    Canvas(
        modifier = modifier.clip(RoundedCornerShape(Wd2Radius.md)),
    ) {
        // Suppress the unused-value warning while keeping the frame dependency explicit.
        if (tick < 0L) return@Canvas

        val width = size.width
        val height = size.height
        val baseline = height - 1f

        // Quiet reference line — reads as a meter at rest rather than a dead box.
        drawLine(
            color = Wd2.LineDim,
            start = Offset(0f, baseline),
            end = Offset(width, baseline),
            strokeWidth = 1f,
        )

        if (rendered.isEmpty()) return@Canvas

        val step = width / (bandCount - 1).coerceAtLeast(1)
        val usableHeight = height * 0.92f
        val points = List(bandCount) { index ->
            // A slow sine keeps the shape alive while the track plays but the codec reports flat.
            val breathe = if (active && !live) {
                (0.9f + 0.1f * kotlin.math.sin(phase * 3.1f + index * 0.35f))
            } else {
                1f
            }
            val value = (rendered[index] * breathe).coerceIn(0f, 1f)
            Offset(index * step, baseline - value * usableHeight)
        }

        val area = Path().apply {
            moveTo(0f, baseline)
            lineTo(points.first().x, points.first().y)
            for (index in 1 until points.size) {
                val previous = points[index - 1]
                val current = points[index]
                val midX = (previous.x + current.x) / 2f
                cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
            }
            lineTo(width, baseline)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accent.copy(alpha = if (active) 0.85f else 0.22f),
                    accent.copy(alpha = if (active) 0.28f else 0.08f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = height,
            ),
        )

        val outline = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (index in 1 until points.size) {
                val previous = points[index - 1]
                val current = points[index]
                val midX = (previous.x + current.x) / 2f
                cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
            }
        }
        drawPath(
            path = outline,
            color = accent.copy(alpha = if (active) 0.95f else 0.3f),
            style = Stroke(width = 1.6f),
        )

        // Peak-hold ticks, drawn only when there is real movement to track.
        if (active && dense) {
            peaks.forEachIndexed { index, peak ->
                if (peak <= 0.03f) return@forEachIndexed
                val x = index * step
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(x, baseline - peak * usableHeight),
                    end = Offset(x, (baseline - peak * usableHeight + 3f).coerceAtMost(baseline)),
                    strokeWidth = 1.5f,
                )
            }
        }

        // Honest label: the bars are generated, not decoded, when the codec reports no spectrum.
        if (active && !live) {
            drawRect(
                color = accent.copy(alpha = 0.55f),
                topLeft = Offset(width - 5f, height - 5f),
                size = Size(3f, 3f),
            )
        }
    }
}

/**
 * Seek bar.
 *
 * This used to be a Material3 `Slider` dressed up as something else: a 10dp pill track with a
 * 1dp border, a red -> white horizontal gradient for the played part, and a white thumb ringed
 * in red on top. Four competing ideas stacked on ten pixels, which is what read as crude.
 *
 * Now it is one flat rail that thickens when you reach for it, fills with white at rest and
 * picks up the accent only while you are actually interacting — red stays reserved for active
 * state, the way the rest of the shell uses it. The thumb is a plain dot that fades in on
 * hover, so the bar is a hairline when you are just listening.
 */
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
    val interactive = safeDuration > 0L

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }

    val playbackFraction = if (safeDuration > 0L) {
        (safePosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    // Follow the pointer 1:1 while dragging; otherwise ease toward the clock, so the 250ms
    // timeline tick reads as motion instead of a jump.
    val fraction by animateFloatAsState(
        targetValue = if (dragging) dragFraction else playbackFraction,
        animationSpec = tween(if (dragging) 0 else 220),
        label = "timeline-fraction",
    )

    val active = interactive && (hovered || dragging)
    val activeAmount by animateFloatAsState(if (active) 1f else 0f, tween(160), label = "timeline-active")
    val railHeight by animateDpAsState(if (active) 6.dp else 4.dp, tween(160), label = "timeline-rail")
    val thumbSize by animateDpAsState(if (dragging) 14.dp else 12.dp, tween(140), label = "timeline-thumb")
    val thumbAlpha by animateFloatAsState(if (active) 1f else 0f, tween(140), label = "timeline-thumb-alpha")

    val fillColor = lerp(Wd2.Text.copy(alpha = 0.82f), accent, activeAmount)
    val railColor = lerp(PanelRaised, Wd2.LineStrong, activeAmount)
    val displayedPosition = if (dragging) (dragFraction * safeDuration).roundToLong() else safePosition

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .hoverable(interactionSource = interactionSource, enabled = interactive)
                .focusable(enabled = interactive)
                .onPreviewKeyEvent { event ->
                    if (!interactive || event.type != KeyEventType.KeyDown) {
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
                }
                .pointerInput(interactive, safeDuration) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (!interactive) return@awaitEachGesture
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        var latest = (down.position.x / width).coerceIn(0f, 1f)
                        dragging = true
                        dragFraction = latest
                        down.consume()
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            latest = (change.position.x / width).coerceIn(0f, 1f)
                            dragFraction = latest
                            change.consume()
                        }
                        dragging = false
                        onSeek(clampTimelinePosition((latest * safeDuration).roundToLong(), safeDuration))
                    }
                },
        ) {
            // The dot rides inside the rail, so the fill always ends at the dot's centre.
            val travel = (maxWidth - thumbSize).coerceAtLeast(0.dp)
            val thumbOffset = travel * fraction

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(railHeight)
                    .clip(RoundedCornerShape(Wd2Radius.pill))
                    .background(railColor),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(thumbSize / 2 + thumbOffset)
                    .height(railHeight)
                    .clip(RoundedCornerShape(Wd2Radius.pill))
                    .background(fillColor),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(fillColor.copy(alpha = fillColor.alpha * thumbAlpha)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatDuration(displayedPosition), color = fillColor, style = MaterialTheme.typography.labelMedium)
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

// One speaker glyph, muted or not — the old set had three separate volume icons whose
// weights did not match, and the difference between them was invisible at 18dp anyway.
private fun volumeIconFor(volume: Float): YdxGlyph =
    if (volume <= 0.001f) YdxGlyph.VolumeOff else YdxGlyph.Volume

internal fun timelineKeyboardStepMs(durationMs: Long): Long =
    (durationMs / 24L).coerceIn(3_000L, 12_000L)

@Composable
private fun animateColor(selected: Boolean): androidx.compose.runtime.State<Color> {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(180),
        label = "nav-color-progress",
    )
    return remember(progress) { mutableStateOf(lerp(Muted, Moss, progress)) }
}
