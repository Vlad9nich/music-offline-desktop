package com.yaneodex.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SourceDescriptor(
    val id: String,
    val name: String,
    val status: String,
    val description: String,
    val isEnabled: Boolean,
)

@Serializable
data class RemoteTrackCandidate(
    val sourceId: String,
    val title: String,
    val artist: String,
    val detailUrl: String,
    val downloadUrl: String? = null,
)

@Serializable
data class DownloadBlueprint(
    val title: String,
    val artist: String,
    val url: String,
    val suggestedFilename: String,
    val refererUrl: String? = null,
)
