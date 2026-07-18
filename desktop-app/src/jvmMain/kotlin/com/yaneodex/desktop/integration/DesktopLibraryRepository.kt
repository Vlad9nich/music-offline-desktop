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
import java.util.concurrent.ConcurrentHashMap

class DesktopLibraryRepository(
    private val storageFile: File = File(System.getProperty("user.home"), ".yaneodex-desktop/library.json"),
    private val configuredDefaultRoots: List<String> = emptyList(),
) : LibraryRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /** In-memory library; playlist ops never re-walk the disk. */
    @Volatile
    private var memory: StoredLibrary? = null

    /** path → (mtime, TrackRecord) to skip re-tagging unchanged files. */
    private val metadataCache = ConcurrentHashMap<String, Pair<Long, TrackRecord>>()

    override fun load(): StoredLibraryState {
        val stored = readStored()
        val roots = sanitizeRoots(stored?.roots ?: defaultRoots())
        val scannedTracks = scanRoots(roots, stored?.hiddenTrackSourceUris.orEmpty())
        val playlists = reconcilePlaylists(stored?.playlists.orEmpty(), scannedTracks)
        val snapshot = LibrarySnapshot(scannedTracks, playlists)
        val next = StoredLibrary(
            roots = roots,
            snapshot = snapshot,
            playlists = playlists,
            hiddenTrackSourceUris = stored?.hiddenTrackSourceUris.orEmpty(),
        )
        memory = next
        if (stored != next) {
            writeStored(next)
        }
        return StoredLibraryState(roots, snapshot)
    }

    override fun importRoots(paths: List<String>): StoredLibraryState {
        val current = ensureMemory()
        val mergedRoots = sanitizeRoots(current.roots + paths)
        return rescanAndPersist(mergedRoots, current.playlists, current.hiddenTrackSourceUris)
    }

    override fun refresh(): StoredLibraryState {
        val current = ensureMemory()
        return rescanAndPersist(sanitizeRoots(current.roots), current.playlists, current.hiddenTrackSourceUris)
    }

    override fun createPlaylist(name: String, artworkHint: String): StoredLibraryState {
        val current = ensureMemory()
        val trimmed = name.trim().ifBlank { "New Playlist" }
        val newPlaylist = PlaylistRecord(
            id = "playlist-${UUID.randomUUID()}",
            name = trimmed,
            artworkHint = resolveArtworkHint(trimmed, artworkHint),
            tone = playlistTone(current.playlists.size),
            description = "",
            trackIds = emptyList(),
            createdAtEpochMs = System.currentTimeMillis(),
        )
        return persistPlaylistsOnly(current.playlists + newPlaylist, current.hiddenTrackSourceUris)
    }

    override fun renamePlaylist(playlistId: String, name: String, artworkHint: String): StoredLibraryState {
        val current = ensureMemory()
        val trimmed = name.trim()
        if (trimmed.isBlank()) return StoredLibraryState(current.roots, current.snapshot)
        val updated = current.playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(
                    name = trimmed,
                    artworkHint = resolveArtworkHint(trimmed, artworkHint),
                )
            } else {
                playlist
            }
        }
        return persistPlaylistsOnly(updated, current.hiddenTrackSourceUris)
    }

    override fun addTracksToPlaylist(trackIds: List<String>, playlistId: String): StoredLibraryState {
        val current = ensureMemory()
        val normalizedTrackIds = trackIds.distinct()
        val updated = current.playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackIds = (playlist.trackIds + normalizedTrackIds).distinct())
            } else {
                playlist
            }
        }
        return persistPlaylistsOnly(updated, current.hiddenTrackSourceUris)
    }

    override fun removeTracksFromPlaylist(trackIds: List<String>, playlistId: String): StoredLibraryState {
        val current = ensureMemory()
        val removedIds = trackIds.toSet()
        val updated = current.playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackIds = playlist.trackIds.filterNot { it in removedIds })
            } else {
                playlist
            }
        }
        return persistPlaylistsOnly(updated, current.hiddenTrackSourceUris)
    }

    override fun removeTracksFromLibrary(trackIds: List<String>): StoredLibraryState {
        val current = ensureMemory()
        val removedIds = trackIds.toSet()
        val removedSourceUris = current.snapshot.tracks
            .filter { it.id in removedIds }
            .map { it.sourceUri }
        val remainingTracks = current.snapshot.tracks.filterNot { it.id in removedIds }
        val updatedPlaylists = current.playlists.map { playlist ->
            playlist.copy(trackIds = playlist.trackIds.filterNot { it in removedIds })
        }
        val hidden = (current.hiddenTrackSourceUris + removedSourceUris).distinct()
        val playlists = reconcilePlaylists(updatedPlaylists, remainingTracks)
        val next = StoredLibrary(
            roots = current.roots,
            snapshot = LibrarySnapshot(remainingTracks, playlists),
            playlists = playlists,
            hiddenTrackSourceUris = hidden,
        )
        memory = next
        writeStored(next)
        return StoredLibraryState(next.roots, next.snapshot)
    }

    /** Playlist mutations: keep tracks, no disk walk. */
    private fun persistPlaylistsOnly(
        existingPlaylists: List<PlaylistRecord>,
        hiddenTrackSourceUris: List<String>,
    ): StoredLibraryState {
        val current = ensureMemory()
        val tracks = current.snapshot.tracks
        val playlists = reconcilePlaylists(existingPlaylists, tracks)
        val next = StoredLibrary(
            roots = current.roots,
            snapshot = LibrarySnapshot(tracks, playlists),
            playlists = playlists,
            hiddenTrackSourceUris = hiddenTrackSourceUris.map(::normalizePath).distinct(),
        )
        memory = next
        writeStored(next)
        return StoredLibraryState(next.roots, next.snapshot)
    }

    private fun rescanAndPersist(
        roots: List<String>,
        existingPlaylists: List<PlaylistRecord>,
        hiddenTrackSourceUris: List<String>,
    ): StoredLibraryState {
        val sanitizedRoots = sanitizeRoots(roots)
        val hidden = hiddenTrackSourceUris.map(::normalizePath).distinct()
        val scannedTracks = scanRoots(sanitizedRoots, hidden)
        val playlists = reconcilePlaylists(existingPlaylists, scannedTracks)
        val next = StoredLibrary(
            roots = sanitizedRoots,
            snapshot = LibrarySnapshot(scannedTracks, playlists),
            playlists = playlists,
            hiddenTrackSourceUris = hidden,
        )
        memory = next
        writeStored(next)
        return StoredLibraryState(roots = sanitizedRoots, snapshot = next.snapshot)
    }

    private fun ensureMemory(): StoredLibrary {
        memory?.let { return it }
        load()
        return memory ?: StoredLibrary(emptyList(), LibrarySnapshot(), emptyList())
    }

    private fun readStored(): StoredLibrary? {
        return runCatching {
            if (!storageFile.exists()) null else json.decodeFromString(StoredLibrary.serializer(), storageFile.readText())
        }.getOrNull()
    }

    private fun writeStored(stored: StoredLibrary) {
        storageFile.parentFile?.mkdirs()
        storageFile.writeText(json.encodeToString(StoredLibrary.serializer(), stored))
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

    private fun scanRoots(roots: List<String>, hiddenTrackSourceUris: List<String>): List<TrackRecord> {
        val hidden = hiddenTrackSourceUris.map(::normalizePath).toSet()
        return roots
            .map(::File)
            .filter { it.exists() }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && supportedExtensions.contains(it.extension.lowercase()) }
                    .map(::toTrackRecordCached)
                    .toList()
            }
            .distinctBy { it.sourceUri }
            .filterNot { it.sourceUri in hidden }
            .sortedWith(compareBy<TrackRecord> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
    }

    private fun toTrackRecordCached(file: File): TrackRecord {
        val normalizedPath = normalizePath(file.absolutePath)
        val mtime = file.lastModified()
        metadataCache[normalizedPath]?.let { (cachedMtime, track) ->
            if (cachedMtime == mtime) return track
        }
        val track = toTrackRecord(file)
        metadataCache[normalizedPath] = mtime to track
        return track
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

    private fun resolveArtworkHint(name: String, explicitHint: String): String {
        val normalized = explicitHint.trim().ifBlank { name.take(2) }
        return normalized.uppercase().take(2).padEnd(2, ' ').trim()
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
    val hiddenTrackSourceUris: List<String> = emptyList(),
)
