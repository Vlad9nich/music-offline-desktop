package com.yaneodex.core.contracts

import com.yaneodex.core.importer.RecognizedTrackCandidate
import com.yaneodex.core.model.DownloadBlueprint
import com.yaneodex.core.model.LibrarySnapshot
import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.model.SourceDescriptor
import com.yaneodex.core.model.TrackRecord

data class StoredLibraryState(
    val roots: List<String>,
    val snapshot: LibrarySnapshot,
)

interface LibraryRepository {
    fun load(): StoredLibraryState
    fun importRoots(paths: List<String>): StoredLibraryState
    fun refresh(): StoredLibraryState
    fun createPlaylist(name: String): StoredLibraryState
    fun renamePlaylist(playlistId: String, name: String): StoredLibraryState
    fun addTrackToPlaylist(trackId: String, playlistId: String): StoredLibraryState
    fun removeTrackFromPlaylist(trackId: String, playlistId: String): StoredLibraryState
}

interface PlaybackBackend {
    fun playQueue(queue: List<TrackRecord>, startTrackId: String?, onState: (PlaybackSnapshot) -> Unit)
    fun togglePlayPause(onState: (PlaybackSnapshot) -> Unit)
    fun playNext(onState: (PlaybackSnapshot) -> Unit)
    fun playPrevious(onState: (PlaybackSnapshot) -> Unit)
    fun stop()
}

data class PlaybackSnapshot(
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,
    val errorMessage: String? = null,
)

interface MusicSource {
    val descriptor: SourceDescriptor
    suspend fun search(query: String): List<RemoteTrackCandidate>
    suspend fun resolve(track: RemoteTrackCandidate): DownloadBlueprint
}

interface MusicSourceCatalog {
    val descriptors: List<SourceDescriptor>
    suspend fun search(query: String): List<RemoteTrackCandidate>
    suspend fun resolve(track: RemoteTrackCandidate): DownloadBlueprint
}

data class OcrCreateJobResult(
    val jobId: String,
)

data class OcrJobResult(
    val jobId: String = "",
    val status: String = "",
    val processedFiles: Int = 0,
    val totalFiles: Int = 0,
    val candidates: List<RecognizedTrackCandidate> = emptyList(),
    val error: String? = null,
)

interface OcrImportClient {
    fun submitSingleImage(serverUrl: String, authToken: String, fileBytes: ByteArray, filename: String): OcrJobResult
    fun submitJob(serverUrl: String, authToken: String, files: List<Pair<String, ByteArray>>): OcrCreateJobResult
    fun pollJob(serverUrl: String, authToken: String, jobId: String): OcrJobResult
}
