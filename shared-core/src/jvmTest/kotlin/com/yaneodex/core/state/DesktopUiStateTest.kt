package com.yaneodex.core.state

import com.yaneodex.core.model.LibrarySnapshot
import com.yaneodex.core.model.PlaylistRecord
import com.yaneodex.core.model.TrackRecord
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopUiStateTest {
    @Test
    fun `detects first run when no roots and no tracks`() {
        val state = DemoLibrary.initialState().copy(
            snapshot = LibrarySnapshot(tracks = emptyList(), playlists = emptyList()),
            libraryRoots = emptyList(),
        )

        assertTrue(state.isFirstRun)
    }

    @Test
    fun `does not treat imported library as first run`() {
        val state = DemoLibrary.initialState().copy(
            snapshot = LibrarySnapshot(
                tracks = listOf(sampleTrack()),
                playlists = listOf(samplePlaylist()),
            ),
            libraryRoots = listOf("C:\\Music"),
        )

        assertFalse(state.isFirstRun)
    }

    private fun sampleTrack(): TrackRecord {
        return TrackRecord(
            id = "track-1",
            uri = "file:///track-1.mp3",
            title = "Track 1",
            artist = "Artist 1",
            durationMs = 180_000,
            importedAtEpochMs = 1_700_000_000_000L,
        )
    }

    private fun samplePlaylist(): PlaylistRecord {
        return PlaylistRecord(
            id = "playlist-1",
            name = "Playlist 1",
            artworkHint = "P1",
            tone = "#95F15A",
            description = "",
            trackIds = listOf("track-1"),
            createdAtEpochMs = 1_700_000_000_000L,
        )
    }
}
