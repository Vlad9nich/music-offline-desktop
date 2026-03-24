package com.yaneodex.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaneodex.core.importer.MatchedTrackCandidate
import com.yaneodex.core.importer.ScreenshotImportItemStatus
import com.yaneodex.core.model.PlaylistRecord
import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.model.TrackRecord
import com.yaneodex.core.state.DesktopSection
import com.yaneodex.core.state.DesktopUiState

private val Panel = Color(0xFF131514)
private val PanelAlt = Color(0xFF1A1D1C)
private val Outline = Color(0x22FFFFFF)
private val Muted = Color(0xFF99A19A)
private val Moss = Color(0xFFA7F46A)
private val Gold = Color(0xFFE7C669)
private val Sky = Color(0xFF7CC8FF)
private val Coral = Color(0xFFE58B6B)

@Composable
fun MusicDesktopApp(
    state: DesktopUiState,
    onSelectSection: (DesktopSection) -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onPlayTrack: (String) -> Unit,
    onPlayPlaylist: () -> Unit,
    onAddTrackToPlaylist: (String) -> Unit,
    onRemoveTrackFromPlaylist: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSearchChange: (String) -> Unit,
    onRunParserSearch: (String) -> Unit,
    onParserResultClick: (RemoteTrackCandidate) -> Unit,
    onTagClick: (String) -> Unit,
    onImportLibraryFolders: () -> Unit,
    onRefreshLibrary: () -> Unit,
    onOcrServerUrlChange: (String) -> Unit,
    onOcrTokenChange: (String) -> Unit,
    onPickScreenshots: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF171C17), Color(0xFF090A0A)))),
    ) {
        AmbientGlow()
        Row(modifier = Modifier.fillMaxSize().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Sidebar(state.selectedSection, onSelectSection)
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                TopBar(state.searchQuery, onSearchChange, onRunParserSearch)
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    MainColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        state = state,
                        onSelectPlaylist = onSelectPlaylist,
                        onCreatePlaylist = onCreatePlaylist,
                        onRenamePlaylist = onRenamePlaylist,
                        onPlayTrack = onPlayTrack,
                        onPlayPlaylist = onPlayPlaylist,
                        onAddTrackToPlaylist = onAddTrackToPlaylist,
                        onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
                        onParserResultClick = onParserResultClick,
                        onTagClick = onTagClick,
                        onImportLibraryFolders = onImportLibraryFolders,
                        onRefreshLibrary = onRefreshLibrary,
                        onOcrServerUrlChange = onOcrServerUrlChange,
                        onOcrTokenChange = onOcrTokenChange,
                        onPickScreenshots = onPickScreenshots,
                    )
                    RightRail(state, onToggleShuffle, onPlayTrack)
                }
                BottomPlayer(state, onTogglePlayPause, onPlayPrevious, onPlayNext, onToggleShuffle)
            }
        }
    }
}

@Composable
private fun Sidebar(selectedSection: DesktopSection, onSelectSection: (DesktopSection) -> Unit) {
    Surface(modifier = Modifier.width(248.dp).fillMaxHeight(), color = Panel, shape = RoundedCornerShape(28.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("YND", color = Moss, style = MaterialTheme.typography.labelMedium)
            Text("YaNeoDex Desktop", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text("Windows-first parser, OCR and shared-core music client.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            NavPill("Home", Icons.Rounded.Home, selectedSection == DesktopSection.HOME) { onSelectSection(DesktopSection.HOME) }
            NavPill("Search", Icons.Rounded.Search, selectedSection == DesktopSection.SEARCH) { onSelectSection(DesktopSection.SEARCH) }
            NavPill("Playlists", Icons.Rounded.PlaylistPlay, selectedSection == DesktopSection.PLAYLISTS) { onSelectSection(DesktopSection.PLAYLISTS) }
            NavPill("Library", Icons.Rounded.LibraryMusic, selectedSection == DesktopSection.LIBRARY) { onSelectSection(DesktopSection.LIBRARY) }
            NavPill("Import", Icons.Rounded.AutoAwesome, selectedSection == DesktopSection.IMPORT) { onSelectSection(DesktopSection.IMPORT) }
            Spacer(Modifier.weight(1f))
            PanelText("Shared core", "Queue logic and OCR matching are separated from the desktop UI layer.")
        }
    }
}

@Composable
private fun TopBar(searchQuery: String, onSearchChange: (String) -> Unit, onRunParserSearch: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Dense, dark, musical desktop UI", color = Gold, style = MaterialTheme.typography.labelMedium)
            Text("Spotify-like composition, desktop-specific structure", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        Row(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Panel)
                .border(1.dp, Outline, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = Muted)
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                cursorBrush = SolidColor(Moss),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                decorationBox = { inner ->
                    if (searchQuery.isBlank()) Text("Search tracks, then run parser", color = Muted, style = MaterialTheme.typography.bodyLarge)
                    inner()
                },
            )
            Box(
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Moss).clickable { onRunParserSearch(searchQuery) }.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("Run", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MainColumn(
    modifier: Modifier = Modifier,
    state: DesktopUiState,
    onSelectPlaylist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onPlayTrack: (String) -> Unit,
    onPlayPlaylist: () -> Unit,
    onAddTrackToPlaylist: (String) -> Unit,
    onRemoveTrackFromPlaylist: (String) -> Unit,
    onParserResultClick: (RemoteTrackCandidate) -> Unit,
    onTagClick: (String) -> Unit,
    onImportLibraryFolders: () -> Unit,
    onRefreshLibrary: () -> Unit,
    onOcrServerUrlChange: (String) -> Unit,
    onOcrTokenChange: (String) -> Unit,
    onPickScreenshots: () -> Unit,
) {
    Surface(modifier = modifier, color = Color(0xDD101111), shape = RoundedCornerShape(30.dp)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Hero(state.spotlight.title, state.spotlight.subtitle, state.spotlight.accent, onPlayPlaylist)
            Tags(state.highlightedTag, onTagClick)
            when (state.selectedSection) {
                DesktopSection.HOME -> HomeSection(state, onSelectPlaylist, onPlayTrack)
                DesktopSection.SEARCH -> SearchSection(state, onParserResultClick)
                DesktopSection.PLAYLISTS -> PlaylistSection(state, onSelectPlaylist, onPlayTrack, onCreatePlaylist, onRenamePlaylist, onRemoveTrackFromPlaylist)
                DesktopSection.LIBRARY -> LibrarySection(state, onPlayTrack, onAddTrackToPlaylist, onImportLibraryFolders, onRefreshLibrary)
                DesktopSection.IMPORT -> ImportSection(state, onOcrServerUrlChange, onOcrTokenChange, onPickScreenshots)
            }
        }
    }
}

@Composable
private fun HomeSection(state: DesktopUiState, onSelectPlaylist: (String) -> Unit, onPlayTrack: (String) -> Unit) {
    SectionTitle("Playlist lanes")
    PlaylistRow(state.snapshot.playlists, onSelectPlaylist)
    SectionTitle("Recent library")
    TrackList(state.snapshot.tracks.take(6), state.currentTrackId, onPlayTrack)
}

@Composable
private fun SearchSection(state: DesktopUiState, onParserResultClick: (RemoteTrackCandidate) -> Unit) {
    SectionTitle("Parser status")
    PanelText("Parser", state.parserStatus)
    SectionTitle("Parser results")
    ParserResults(state.parserResults, onParserResultClick)
}

@Composable
private fun PlaylistSection(
    state: DesktopUiState,
    onSelectPlaylist: (String) -> Unit,
    onPlayTrack: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onRemoveTrackFromPlaylist: (String) -> Unit,
) {
    var newPlaylistName by remember { mutableStateOf("") }
    var renameValue by remember(state.selectedPlaylistId) { mutableStateOf(state.selectedPlaylist?.name.orEmpty()) }
    SectionTitle("Playlists")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = newPlaylistName,
            onValueChange = { newPlaylistName = it },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(PanelAlt)
                .border(1.dp, Outline, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            cursorBrush = SolidColor(Moss),
            decorationBox = { inner ->
                if (newPlaylistName.isBlank()) Text("New playlist name", color = Muted, style = MaterialTheme.typography.bodyMedium)
                inner()
            },
        )
        AccentAction("Create", Icons.Rounded.PlaylistPlay, Moss) {
            onCreatePlaylist(newPlaylistName)
            newPlaylistName = ""
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = renameValue,
            onValueChange = { renameValue = it },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(PanelAlt)
                .border(1.dp, Outline, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            cursorBrush = SolidColor(Moss),
            decorationBox = { inner ->
                if (renameValue.isBlank()) Text("Rename selected playlist", color = Muted, style = MaterialTheme.typography.bodyMedium)
                inner()
            },
        )
        AccentAction("Rename", Icons.Rounded.Tune, Gold) { onRenamePlaylist(renameValue) }
    }
    PlaylistColumn(state.snapshot.playlists, state.selectedPlaylistId, onSelectPlaylist)
    SectionTitle(state.selectedPlaylist?.name ?: "Playlist")
    TrackList(
        tracks = state.selectedPlaylistTracks,
        currentTrackId = state.currentTrackId,
        onPlayTrack = onPlayTrack,
        rowActionLabel = "Remove",
        onRowAction = onRemoveTrackFromPlaylist,
    )
}

@Composable
private fun LibrarySection(
    state: DesktopUiState,
    onPlayTrack: (String) -> Unit,
    onAddTrackToPlaylist: (String) -> Unit,
    onImportLibraryFolders: () -> Unit,
    onRefreshLibrary: () -> Unit,
) {
    SectionTitle("Library")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        PrimaryAction("Add folders", Icons.Rounded.FolderOpen, onImportLibraryFolders)
        AccentAction("Refresh", Icons.Rounded.Refresh, Gold, onRefreshLibrary)
    }
    PanelText("Local library", buildLibraryStatusText(state))
    TrackList(
        tracks = state.snapshot.tracks,
        currentTrackId = state.currentTrackId,
        onPlayTrack = onPlayTrack,
        rowActionLabel = "Add",
        onRowAction = onAddTrackToPlaylist,
    )
}

@Composable
private fun ImportSection(
    state: DesktopUiState,
    onOcrServerUrlChange: (String) -> Unit,
    onOcrTokenChange: (String) -> Unit,
    onPickScreenshots: () -> Unit,
) {
    SectionTitle("OCR settings")
    OcrSettingsPanel(state.ocrSettings.serverUrl, state.ocrSettings.authToken, onOcrServerUrlChange, onOcrTokenChange, onPickScreenshots)
    PanelText("OCR", state.ocrStatus)
    SectionTitle("Import review")
    ImportMatches(state.importMatches)
}

@Composable
private fun Hero(title: String, subtitle: String, accent: String, onPlayPlaylist: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(parseTone(accent).copy(alpha = 0.46f), Color(0xFF20251E), Color(0xFF0D0F0E))))
            .border(1.dp, Outline, RoundedCornerShape(30.dp))
            .padding(26.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Curated sequence", color = Gold, style = MaterialTheme.typography.labelMedium)
                Text(title, color = Color.White, style = MaterialTheme.typography.displayLarge)
                Text(subtitle, color = Color(0xFFE0E4DE), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(520.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PrimaryAction("Play lane", Icons.Rounded.PlayArrow, onPlayPlaylist)
                AccentChip("Parser ready", Moss)
                AccentChip("OCR ready", Gold)
                AccentChip("Windows build", Sky)
            }
        }
        RecordHalo(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 28.dp))
    }
}

@Composable
private fun Tags(selectedTag: String, onTagClick: (String) -> Unit) {
    val tags = listOf("Night Drive", "Focused Code", "Glass Pop", "Soft Warehouse", "Late Commute")
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        tags.forEach { tag ->
            AccentChip(tag, if (tag == selectedTag) Moss else Color(0xFF4D534F), onClick = { onTagClick(tag) })
        }
    }
}

@Composable
private fun PlaylistRow(playlists: List<PlaylistRecord>, onSelectPlaylist: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        playlists.take(3).forEach { playlist -> PlaylistCard(playlist) { onSelectPlaylist(playlist.id) } }
    }
}

@Composable
private fun PlaylistColumn(playlists: List<PlaylistRecord>, selectedPlaylistId: String, onSelectPlaylist: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        playlists.forEach { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (playlist.id == selectedPlaylistId) parseTone(playlist.tone).copy(alpha = 0.12f) else Panel)
                    .border(1.dp, if (playlist.id == selectedPlaylistId) parseTone(playlist.tone).copy(alpha = 0.34f) else Outline, RoundedCornerShape(22.dp))
                    .clickable { onSelectPlaylist(playlist.id) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ArtworkBadge(playlist.artworkHint, parseTone(playlist.tone))
                Column(modifier = Modifier.weight(1f)) {
                    Text(playlist.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(playlist.description, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistRecord, onClick: () -> Unit) {
    Surface(modifier = Modifier.width(220.dp).clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), color = Panel) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(parseTone(playlist.tone), Color(0xFF262926)))),
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
            Text(playlist.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(playlist.description, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<TrackRecord>,
    currentTrackId: String?,
    onPlayTrack: (String) -> Unit,
    rowActionLabel: String? = null,
    onRowAction: ((String) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tracks.forEachIndexed { index, track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (track.id == currentTrackId) Moss.copy(alpha = 0.08f) else Panel)
                    .border(1.dp, if (track.id == currentTrackId) Moss.copy(alpha = 0.24f) else Outline, RoundedCornerShape(18.dp))
                    .clickable { onPlayTrack(track.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("${index + 1}".padStart(2, '0'), color = Muted, style = MaterialTheme.typography.labelMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(track.artist, color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
                if (!rowActionLabel.isNullOrBlank() && onRowAction != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(PanelAlt)
                            .border(1.dp, Outline, RoundedCornerShape(14.dp))
                            .clickable { onRowAction(track.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(rowActionLabel, color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(formatDuration(track.durationMs), color = if (track.id == currentTrackId) Moss else Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ParserResults(results: List<RemoteTrackCandidate>, onParserResultClick: (RemoteTrackCandidate) -> Unit) {
    if (results.isEmpty()) {
        PanelText("Parser results", "No parser results yet.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        results.take(8).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Panel)
                    .border(1.dp, Outline, RoundedCornerShape(18.dp))
                    .clickable { onParserResultClick(item) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ArtworkBadge(item.title.take(2).uppercase(), Sky)
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(item.artist, color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
                Text(item.sourceId, color = Gold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun OcrSettingsPanel(
    serverUrl: String,
    authToken: String,
    onOcrServerUrlChange: (String) -> Unit,
    onOcrTokenChange: (String) -> Unit,
    onPickScreenshots: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(24.dp), color = Panel) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField("OCR server", serverUrl, "https://host", onOcrServerUrlChange)
            LabeledField("Bearer token", authToken, "Optional token", onOcrTokenChange)
            PrimaryAction("Choose screenshots", Icons.Rounded.FolderOpen, onPickScreenshots)
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = Gold, style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(PanelAlt)
                .border(1.dp, Outline, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                cursorBrush = SolidColor(Moss),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                decorationBox = { inner ->
                    if (value.isBlank()) Text(placeholder, color = Muted, style = MaterialTheme.typography.bodyLarge)
                    inner()
                },
            )
        }
    }
}

@Composable
private fun ImportMatches(matches: List<MatchedTrackCandidate>) {
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
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.recognized.rawText, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(item.message ?: "Match ready.", color = accent, style = MaterialTheme.typography.bodyMedium)
                    item.bestMatch?.let { match ->
                        Text("${match.artist} - ${match.title}", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RightRail(state: DesktopUiState, onToggleShuffle: () -> Unit, onPlayTrack: (String) -> Unit) {
    Surface(modifier = Modifier.width(320.dp).fillMaxHeight(), color = Color(0xDD101111), shape = RoundedCornerShape(30.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Now shaping", color = Gold, style = MaterialTheme.typography.labelMedium)
            PanelText(state.currentTrack?.title ?: "Nothing selected", state.currentTrack?.artist ?: "Choose a track to build the queue.")
            MiniPanel("Queue", "${state.playbackQueue.size} items", Icons.Rounded.QueueMusic, Moss)
            MiniPanel("Shuffle", if (state.shuffleEnabled) "Artist-aware" else "Off", Icons.Rounded.Shuffle, Sky, onToggleShuffle)
            MiniPanel("Parser", state.parserStatus, Icons.Rounded.Sync, Gold)
            MiniPanel("OCR", state.ocrStatus, Icons.Rounded.AutoAwesome, Coral)
            Text("Up next", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.playbackQueue.take(5).forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Panel)
                            .border(1.dp, Outline, RoundedCornerShape(18.dp))
                            .clickable { onPlayTrack(track.id) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtworkBadge(track.title.take(2).uppercase(), Moss)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomPlayer(
    state: DesktopUiState,
    onTogglePlayPause: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleShuffle: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(28.dp), color = Panel) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtworkBadge(state.currentTrack?.title?.take(2)?.uppercase() ?: "YN", Moss)
                Column {
                    Text(state.currentTrack?.title ?: "No track selected", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(state.currentTrack?.artist ?: "Build a lane from parser or library", color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundAction(Icons.Rounded.Shuffle, state.shuffleEnabled, onToggleShuffle)
                RoundAction(Icons.Rounded.NavigateBefore, false, onPlayPrevious)
                RoundAction(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, true, onTogglePlayPause)
                RoundAction(Icons.Rounded.NavigateNext, false, onPlayNext)
                RoundAction(Icons.Rounded.Tune, false) {}
            }
        }
    }
}

@Composable
private fun NavPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Moss.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (selected) Moss.copy(alpha = 0.35f) else Outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Moss else Muted)
        Text(label, color = if (selected) Color.White else Muted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PrimaryAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Moss)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = Color.Black)
        Text(label, color = Color.Black, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AccentAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = color)
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AccentChip(label: String, color: Color, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MiniPanel(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .border(1.dp, Outline, RoundedCornerShape(20.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Muted, style = MaterialTheme.typography.labelMedium)
            Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PanelText(title: String, body: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = Panel) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), color = Gold, style = MaterialTheme.typography.labelMedium)
            Text(body, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun ArtworkBadge(label: String, color: Color) {
    Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(color), contentAlignment = Alignment.Center) {
        Text(label, color = Color.Black, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RecordHalo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(180.dp)) {
        drawCircle(Color(0x18FFFFFF), radius = size.minDimension / 2)
        drawCircle(Color(0x22000000), radius = size.minDimension / 2.6f)
        drawCircle(Moss.copy(alpha = 0.25f), radius = size.minDimension / 5.2f)
        drawCircle(Color(0xFF0D0F0E), radius = size.minDimension / 7.5f)
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (active) Moss.copy(alpha = 0.18f) else PanelAlt)
            .border(1.dp, if (active) Moss.copy(alpha = 0.4f) else Outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (active) Moss else Color.White)
    }
}

@Composable
private fun AmbientGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(brush = Brush.radialGradient(listOf(Moss.copy(alpha = 0.12f), Color.Transparent)), radius = 420f, center = Offset(size.width * 0.72f, size.height * 0.12f))
        drawCircle(brush = Brush.radialGradient(listOf(Gold.copy(alpha = 0.08f), Color.Transparent)), radius = 360f, center = Offset(size.width * 0.15f, size.height * 0.86f))
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

private fun buildLibraryStatusText(state: DesktopUiState): String {
    val roots = if (state.libraryRoots.isEmpty()) "No roots configured" else state.libraryRoots.joinToString("\n")
    return "${state.libraryStatus}\n\nRoots:\n$roots"
}
