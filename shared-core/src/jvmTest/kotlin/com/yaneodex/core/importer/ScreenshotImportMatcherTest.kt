package com.yaneodex.core.importer

import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.model.TrackRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenshotImportMatcherTest {
    @Test
    fun `deduplicate keeps highest confidence candidate`() {
        val input = listOf(
            RecognizedTrackCandidate("1", 0, "Kino | Pachka sigaret", "Kino", "Pachka sigaret", 0.51f),
            RecognizedTrackCandidate("2", 1, "Kino | Pachka sigaret", "Kino", "Pachka sigaret", 0.93f),
        )

        val deduplicated = ScreenshotImportMatcher.deduplicate(input)

        assertEquals(1, deduplicated.size)
        assertEquals("2", deduplicated.first().candidateId)
    }

    @Test
    fun `pickBestMatch marks track already in playlist`() {
        val candidate = RecognizedTrackCandidate("1", 0, "Night Drive | Polar Youth", "Polar Youth", "Night Drive", 0.88f)
        val result = RemoteTrackCandidate("ligaudio", "Night Drive", "Polar Youth", "https://example/detail", "https://example/night-drive.mp3")
        val playlistTrack = TrackRecord(
            id = "local-1",
            uri = "content://media/test",
            sourceUri = "https://example/night-drive.mp3",
            title = "Night Drive",
            artist = "Polar Youth",
            durationMs = 0L,
            importedAtEpochMs = 0L,
        )

        val matched = ScreenshotImportMatcher.pickBestMatch(candidate, listOf(result), listOf(playlistTrack))

        assertEquals(ScreenshotImportItemStatus.ALREADY_IN_PLAYLIST, matched.status)
        assertTrue(matched.matchScore > 0.7f)
    }

    @Test
    fun `pickBestMatch returns matched status for exact candidate`() {
        val candidate = RecognizedTrackCandidate("1", 0, "Afterglow | Aurora Lane", "Aurora Lane", "Afterglow", 0.95f)
        val result = RemoteTrackCandidate("ligaudio", "Afterglow", "Aurora Lane", "https://example/detail", "https://example/afterglow.mp3")

        val matched = ScreenshotImportMatcher.pickBestMatch(candidate, listOf(result), emptyList())

        assertEquals(ScreenshotImportItemStatus.MATCHED, matched.status)
        assertEquals(result, matched.bestMatch)
    }
}
