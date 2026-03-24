package com.yaneodex.desktop.integration

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopLibraryRepositoryTest {
    @Test
    fun `imports supported audio files from selected roots`() {
        val root = createTempDirectory("yaneodex-library").toFile()
        try {
            File(root, "Aurora Lane - Afterglow.mp3").writeBytes(byteArrayOf(1, 2, 3))
            File(root, "notes.txt").writeText("ignore")

            val storage = File(root, "library.json")
            val repository = DesktopLibraryRepository(storage)
            val stored = repository.importRoots(listOf(root.absolutePath))

            assertEquals(1, stored.snapshot.tracks.size)
            assertEquals("Afterglow", stored.snapshot.tracks.first().title)
            assertEquals("Aurora Lane", stored.snapshot.tracks.first().artist)
            assertTrue(stored.roots.contains(root.absolutePath))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `creates and renames playlist`() {
        val root = createTempDirectory("yaneodex-playlists").toFile()
        try {
            val storage = File(root, "library.json")
            val repository = DesktopLibraryRepository(storage)

            val created = repository.createPlaylist("Road Mix")
            val createdPlaylist = created.snapshot.playlists.firstOrNull { it.name == "Road Mix" }
            assertTrue(createdPlaylist != null)

            val renamed = repository.renamePlaylist(createdPlaylist.id, "Road Mix Updated")
            assertTrue(renamed.snapshot.playlists.any { it.id == createdPlaylist.id && it.name == "Road Mix Updated" })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `adds and removes track in playlist`() {
        val root = createTempDirectory("yaneodex-playlist-tracks").toFile()
        try {
            File(root, "Artist - Track.mp3").writeBytes(byteArrayOf(1, 2, 3))
            val storage = File(root, "library.json")
            val repository = DesktopLibraryRepository(storage)
            val imported = repository.importRoots(listOf(root.absolutePath))

            val playlist = repository.createPlaylist("Inbox").snapshot.playlists.first { it.name == "Inbox" }
            val trackId = imported.snapshot.tracks.first().id

            val withTrack = repository.addTrackToPlaylist(trackId, playlist.id)
            assertTrue(withTrack.snapshot.playlists.first { it.id == playlist.id }.trackIds.contains(trackId))

            val withoutTrack = repository.removeTrackFromPlaylist(trackId, playlist.id)
            assertTrue(withoutTrack.snapshot.playlists.first { it.id == playlist.id }.trackIds.none { it == trackId })
        } finally {
            root.deleteRecursively()
        }
    }
}
