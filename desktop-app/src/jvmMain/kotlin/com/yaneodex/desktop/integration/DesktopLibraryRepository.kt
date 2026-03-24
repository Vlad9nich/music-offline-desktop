package com.yaneodex.desktop.integration

import com.yaneodex.core.contracts.LibraryRepository
import com.yaneodex.core.contracts.StoredLibraryState
import com.yaneodex.core.model.LibrarySnapshot
import com.yaneodex.core.model.PlaylistRecord
import com.yaneodex.core.model.TrackRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.UUID

class DesktopLibraryRepository(
    private val storageFile: File = File(System.getProperty("user.home"), ".yaneodex-desktop/library.json"),
    private val configuredDefaultRoots: List<String> = emptyList(),
) : LibraryRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override fun load(): StoredLibraryState {
        val stored = runCatching {
            if (!storageFile.exists()) null else json.decodeFromString<StoredLibrary>(storageFile.readText())
        }.getOrNull()

        val roots = stored?.roots?.filter { it.isNotBlank() }?.distinct()?.ifEmpty { defaultRoots() } ?: defaultRoots()
        val scannedTracks = scanRoots(roots)
        val playlists = reconcilePlaylists(stored?.playlists.orEmpty(), scannedTracks)
        return StoredLibraryState(roots, LibrarySnapshot(scannedTracks, playlists))
    }

    override fun importRoots(paths: List<String>): StoredLibraryState {
        val current = load()
        val mergedRoots = (current.roots + paths.map(::normalizePath)).filter { it.isNotBlank() }.distinct()
        return persist(mergedRoots, current.snapshot.playlists)
    }

    override fun refresh(): StoredLibraryState {
        val current = load()
        return persist(current.roots, current.snapshot.playlists)
    }

    override fun createPlaylist(name: String): StoredLibraryState {
        val current = load()
        val trimmed = name.trim().ifBlank { "New Playlist" }
        val newPlaylist = PlaylistRecord(
            id = "playlist-${UUID.randomUUID()}",
            name = trimmed,
            artworkHint = trimmed.take(2).uppercase().padEnd(2, ' ').trim(),
            tone = playlistTone(current.snapshot.playlists.size),
            description = "Created on Windows desktop.",
            trackIds = emptyList(),
            createdAtEpochMs = System.currentTimeMillis(),
        )
        return persist(current.roots, current.snapshot.playlists + newPlaylist)
    }

    override fun renamePlaylist(playlistId: String, name: String): StoredLibraryState {
        val current = load()
        val trimmed = name.trim()
        if (trimmed.isBlank()) return current
        val updated = current.snapshot.playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(
                    name = trimmed,
                    artworkHint = trimmed.take(2).uppercase().padEnd(2, ' ').trim(),
                )
            } else {
                playlist
            }
        }
        return persist(current.roots, updated)
    }

    override fun addTrackToPlaylist(trackId: String, playlistId: String): StoredLibraryState {
        val current = load()
        val updated = current.snapshot.playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackIds = (playlist.trackIds + trackId).distinct())
            } else {
                playlist
            }
        }
        return persist(current.roots, updated)
    }

    override fun removeTrackFromPlaylist(trackId: String, playlistId: String): StoredLibraryState {
        val current = load()
        val updated = current.snapshot.playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackIds = playlist.trackIds.filterNot { it == trackId })
            } else {
                playlist
            }
        }
        return persist(current.roots, updated)
    }

    private fun persist(roots: List<String>, existingPlaylists: List<PlaylistRecord>): StoredLibraryState {
        val scannedTracks = scanRoots(roots)
        val playlists = reconcilePlaylists(existingPlaylists, scannedTracks)
        val snapshot = LibrarySnapshot(scannedTracks, playlists)
        val stored = StoredLibrary(roots = roots, snapshot = snapshot, playlists = playlists)
        storageFile.parentFile?.mkdirs()
        storageFile.writeText(json.encodeToString(StoredLibrary.serializer(), stored))
        return StoredLibraryState(roots = roots, snapshot = snapshot)
    }

    private fun reconcilePlaylists(existing: List<PlaylistRecord>, tracks: List<TrackRecord>): List<PlaylistRecord> {
        val trackIds = tracks.map { it.id }.toSet()
        val cleaned = existing.map { playlist ->
            playlist.copy(trackIds = playlist.trackIds.filter { it in trackIds })
        }
        if (cleaned.isNotEmpty()) return cleaned
        return listOf(
            PlaylistRecord(
                id = "library-all",
                name = "All Tracks",
                artworkHint = "AT",
                tone = "#95F15A",
                description = "All playable tracks discovered on this Windows machine.",
                trackIds = tracks.map { it.id },
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun scanRoots(roots: List<String>): List<TrackRecord> {
        return roots
            .map(::File)
            .filter { it.exists() }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && supportedExtensions.contains(it.extension.lowercase()) }
                    .map(::toTrackRecord)
                    .toList()
            }
            .distinctBy { it.sourceUri }
            .sortedWith(compareBy<TrackRecord> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
    }

    private fun toTrackRecord(file: File): TrackRecord {
        val normalizedPath = normalizePath(file.absolutePath)
        val fallback = parseArtistTitleFromFilename(file.nameWithoutExtension)
        val metadata = readTagMetadata(file)

        val title = metadata.title.ifBlank { fallback.second.ifBlank { file.nameWithoutExtension.trim() } }
        val artist = metadata.artist.ifBlank { fallback.first.ifBlank { "Unknown artist" } }
        return TrackRecord(
            id = UUID.nameUUIDFromBytes(normalizedPath.toByteArray()).toString(),
            uri = file.toURI().toString(),
            sourceUri = normalizedPath,
            title = title,
            artist = artist,
            durationMs = metadata.durationMs,
            importedAtEpochMs = file.lastModified(),
        )
    }

    private fun readTagMetadata(file: File): TagMetadata {
        return runCatching {
            val audio = AudioFileIO.read(file)
            val tag = audio.tag
            TagMetadata(
                title = tag?.getFirst(FieldKey.TITLE).orEmpty().trim(),
                artist = tag?.getFirst(FieldKey.ARTIST).orEmpty().trim(),
                durationMs = (audio.audioHeader?.trackLength ?: 0).toLong() * 1000L,
            )
        }.getOrElse { TagMetadata() }
    }

    private fun parseArtistTitleFromFilename(nameWithoutExtension: String): Pair<String, String> {
        val baseName = nameWithoutExtension.trim()
        val artistTitle = baseName.split(" - ", limit = 2)
        return if (artistTitle.size > 1) {
            artistTitle[0].trim() to artistTitle[1].trim()
        } else {
            "" to baseName
        }
    }

    private fun defaultRoots(): List<String> {
        val configured = configuredDefaultRoots.map(::normalizePath).filter { it.isNotBlank() }
        if (configured.isNotEmpty()) return configured.distinct()
        val music = File(System.getProperty("user.home"), "Music")
        val yaneodex = File(music, "YaNeoDex")
        return listOf(yaneodex, music).filter { it.exists() }.map { normalizePath(it.absolutePath) }.distinct()
    }

    private fun normalizePath(path: String): String = File(path).absolutePath

    private fun playlistTone(index: Int): String {
        val tones = listOf("#95F15A", "#E7C669", "#7CC8FF", "#E58B6B")
        return tones[index % tones.size]
    }

    companion object {
        private val supportedExtensions = setOf("mp3", "wav", "m4a", "aac", "flac", "ogg")
    }
}

private data class TagMetadata(
    val title: String = "",
    val artist: String = "",
    val durationMs: Long = 0L,
)

@Serializable
data class StoredLibrary(
    val roots: List<String>,
    val snapshot: LibrarySnapshot,
    val playlists: List<PlaylistRecord>,
)
