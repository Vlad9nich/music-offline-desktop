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

        val roots = sanitizeRoots(stored?.roots ?: defaultRoots())
        val scannedTracks = scanRoots(roots)
        val playlists = reconcilePlaylists(stored?.playlists.orEmpty(), scannedTracks)
        val snapshot = LibrarySnapshot(scannedTracks, playlists)
        if (stored != null && (stored.roots != roots || stored.playlists != playlists || stored.snapshot != snapshot)) {
            return persist(roots, playlists)
        }
        return StoredLibraryState(roots, snapshot)
    }

    override fun importRoots(paths: List<String>): StoredLibraryState {
        val current = load()
        val mergedRoots = sanitizeRoots(current.roots + paths)
        return persist(mergedRoots, current.snapshot.playlists)
    }

    override fun refresh(): StoredLibraryState {
        val current = load()
        return persist(sanitizeRoots(current.roots), current.snapshot.playlists)
    }

    override fun createPlaylist(name: String): StoredLibraryState {
        val current = load()
        val trimmed = name.trim().ifBlank { "New Playlist" }
        val newPlaylist = PlaylistRecord(
            id = "playlist-${UUID.randomUUID()}",
            name = trimmed,
            artworkHint = trimmed.take(2).uppercase().padEnd(2, ' ').trim(),
            tone = playlistTone(current.snapshot.playlists.size),
            description = "",
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
        val sanitizedRoots = sanitizeRoots(roots)
        val scannedTracks = scanRoots(sanitizedRoots)
        val playlists = reconcilePlaylists(existingPlaylists, scannedTracks)
        val snapshot = LibrarySnapshot(scannedTracks, playlists)
        val stored = StoredLibrary(roots = sanitizedRoots, snapshot = snapshot, playlists = playlists)
        storageFile.parentFile?.mkdirs()
        storageFile.writeText(json.encodeToString(StoredLibrary.serializer(), stored))
        return StoredLibraryState(roots = sanitizedRoots, snapshot = snapshot)
    }

    private fun reconcilePlaylists(existing: List<PlaylistRecord>, tracks: List<TrackRecord>): List<PlaylistRecord> {
        val trackIds = tracks.map { it.id }.toSet()
        val cleaned = existing
            .filterNot { it.id == "library-all" }
            .map { playlist ->
            playlist.copy(trackIds = playlist.trackIds.filter { it in trackIds })
        }
        val libraryAll = PlaylistRecord(
            id = "library-all",
            name = "All Tracks",
            artworkHint = "AT",
            tone = "#95F15A",
            description = "",
            trackIds = tracks.map { it.id },
            createdAtEpochMs = existing.firstOrNull { it.id == "library-all" }?.createdAtEpochMs ?: System.currentTimeMillis(),
        )
        return listOf(libraryAll) + cleaned
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
        return sanitizeRoots(configuredDefaultRoots)
    }

    private fun sanitizeRoots(paths: List<String>): List<String> {
        return paths
            .mapNotNull { raw ->
                raw.trim()
                    .takeIf { it.isNotBlank() }
                    ?.let(::normalizePath)
                    ?.takeIf { File(it).exists() }
            }
            .distinctBy { it.lowercase() }
    }

    private fun normalizePath(path: String): String =
        runCatching { File(path).canonicalFile.absolutePath }.getOrElse { File(path).absoluteFile.absolutePath }

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
