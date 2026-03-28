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
            val repository = DesktopLibraryRepository(
                storageFile = storage,
                configuredDefaultRoots = listOf(root.absolutePath),
            )
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
            val repository = DesktopLibraryRepository(
                storageFile = storage,
                configuredDefaultRoots = listOf(root.absolutePath),
            )

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
            val repository = DesktopLibraryRepository(
                storageFile = storage,
                configuredDefaultRoots = listOf(root.absolutePath),
            )
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

    @Test
    fun `bulk playlist updates keep unique track ids`() {
        val root = createTempDirectory("yaneodex-playlist-bulk").toFile()
        try {
            File(root, "Artist - Track One.mp3").writeBytes(byteArrayOf(1, 2, 3))
            File(root, "Artist - Track Two.mp3").writeBytes(byteArrayOf(1, 2, 4))
            val storage = File(root, "library.json")
            val repository = DesktopLibraryRepository(
                storageFile = storage,
                configuredDefaultRoots = listOf(root.absolutePath),
            )
            val imported = repository.importRoots(listOf(root.absolutePath))
            val playlist = repository.createPlaylist("Inbox").snapshot.playlists.first { it.name == "Inbox" }
            val ids = imported.snapshot.tracks.map { it.id }

            val withTracks = repository.addTracksToPlaylist(ids + ids.first(), playlist.id)
            assertEquals(ids.toSet(), withTracks.snapshot.playlists.first { it.id == playlist.id }.trackIds.toSet())

            val withoutTracks = repository.removeTracksFromPlaylist(ids, playlist.id)
            assertTrue(withoutTracks.snapshot.playlists.first { it.id == playlist.id }.trackIds.isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `removing tracks from library hides them and cleans playlists`() {
        val root = createTempDirectory("yaneodex-library-remove").toFile()
        try {
            File(root, "Artist - Track One.mp3").writeBytes(byteArrayOf(1, 2, 3))
            File(root, "Artist - Track Two.mp3").writeBytes(byteArrayOf(1, 2, 4))
            val storage = File(root, "library.json")
            val repository = DesktopLibraryRepository(
                storageFile = storage,
                configuredDefaultRoots = listOf(root.absolutePath),
            )
            val imported = repository.importRoots(listOf(root.absolutePath))
            val playlist = repository.createPlaylist("Inbox").snapshot.playlists.first { it.name == "Inbox" }
            val trackIds = imported.snapshot.tracks.map { it.id }
            repository.addTracksToPlaylist(trackIds, playlist.id)

            val updated = repository.removeTracksFromLibrary(listOf(trackIds.first()))

            assertEquals(1, updated.snapshot.tracks.size)
            assertTrue(updated.snapshot.tracks.none { it.id == trackIds.first() })
            assertTrue(updated.snapshot.playlists.first { it.id == playlist.id }.trackIds.none { it == trackIds.first() })

            val reloaded = repository.load()
            assertEquals(updated.snapshot.tracks.map { it.id }, reloaded.snapshot.tracks.map { it.id })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `repeated imports do not duplicate roots or tracks`() {
        val root = createTempDirectory("yaneodex-duplicates").toFile()
        try {
            File(root, "Artist - Track.mp3").writeBytes(byteArrayOf(1, 2, 3))
            val repository = DesktopLibraryRepository(
                storageFile = File(root, "library.json"),
                configuredDefaultRoots = emptyList(),
            )

            val first = repository.importRoots(listOf(root.absolutePath))
            val second = repository.importRoots(listOf(root.absolutePath, "${root.absolutePath}\\"))

            assertEquals(1, first.roots.size)
            assertEquals(1, second.roots.size)
            assertEquals(1, second.snapshot.tracks.size)
            assertEquals("library-all", second.snapshot.playlists.first().id)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `missing roots are dropped gracefully on load`() {
        val root = createTempDirectory("yaneodex-missing-root").toFile()
        val missingRoot = File(root, "gone")
        val repository = DesktopLibraryRepository(
            storageFile = File(root, "library.json"),
            configuredDefaultRoots = listOf(missingRoot.absolutePath),
        )

        val loaded = repository.load()

        assertTrue(loaded.roots.isEmpty())
        assertTrue(loaded.snapshot.tracks.isEmpty())
        assertEquals("library-all", loaded.snapshot.playlists.first().id)
        root.deleteRecursively()
    }

    @Test
    fun `load self-heals stored state and keeps library all first`() {
        val root = createTempDirectory("yaneodex-library-heal").toFile()
        try {
            File(root, "Artist - Track.mp3").writeBytes(byteArrayOf(1, 2, 3))
            val storage = File(root, "library.json")
            storage.writeText(
                """
                {
                  "roots": [
                    "${root.absolutePath.replace("\\", "\\\\")}",
                    "${root.absolutePath.replace("\\", "\\\\")}\\\\",
                    "${File(root, "missing").absolutePath.replace("\\", "\\\\")}"
                  ],
                  "snapshot": {
                    "tracks": [],
                    "playlists": []
                  },
                  "playlists": [
                    {
                      "id": "playlist-1",
                      "name": "Inbox",
                      "artworkHint": "IN",
                      "tone": "#E7C669",
                      "description": "",
                      "trackIds": ["ghost-track"],
                      "createdAtEpochMs": 1
                    },
                    {
                      "id": "library-all",
                      "name": "Old",
                      "artworkHint": "OL",
                      "tone": "#95F15A",
                      "description": "",
                      "trackIds": [],
                      "createdAtEpochMs": 7
                    }
                  ]
                }
                """.trimIndent(),
            )

            val repository = DesktopLibraryRepository(
                storageFile = storage,
                configuredDefaultRoots = emptyList(),
            )

            val loaded = repository.load()

            assertEquals(1, loaded.roots.size)
            assertEquals(root.canonicalPath, loaded.roots.single())
            assertEquals("library-all", loaded.snapshot.playlists.first().id)
            assertEquals(7L, loaded.snapshot.playlists.first().createdAtEpochMs)
            assertTrue(loaded.snapshot.playlists.first().trackIds.isNotEmpty())
            assertTrue(loaded.snapshot.playlists.first { it.id == "playlist-1" }.trackIds.isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
