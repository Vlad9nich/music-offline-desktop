package com.yaneodex.core.importer

import com.yaneodex.core.model.RemoteTrackCandidate
import kotlinx.serialization.Serializable

@Serializable
data class RecognizedTrackCandidate(
    val candidateId: String,
    val screenshotIndex: Int,
    val rawText: String,
    val artistGuess: String,
    val titleGuess: String,
    val confidence: Float,
    val bbox: List<Int>? = null,
)

enum class ScreenshotImportItemStatus {
    RECOGNIZED,
    MATCHED,
    LOW_CONFIDENCE_MATCH,
    NOT_FOUND,
    ALREADY_IN_PLAYLIST,
}

data class MatchedTrackCandidate(
    val recognized: RecognizedTrackCandidate,
    val bestMatch: RemoteTrackCandidate?,
    val matchScore: Float,
    val status: ScreenshotImportItemStatus,
    val message: String? = null,
)
