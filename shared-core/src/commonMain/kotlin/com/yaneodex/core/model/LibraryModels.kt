package com.yaneodex.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackRecord(
    val id: String,
    val uri: String,
    val sourceUri: String = uri,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val importedAtEpochMs: Long,
)

@Serializable
data class PlaylistRecord(
    val id: String,
    val name: String,
    val artworkHint: String,
    val tone: String,
    val description: String,
    val trackIds: List<String>,
    val createdAtEpochMs: Long,
)

@Serializable
data class LibrarySnapshot(
    val tracks: List<TrackRecord> = emptyList(),
    val playlists: List<PlaylistRecord> = emptyList(),
)
