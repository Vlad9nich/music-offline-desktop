package com.yaneodex.core.playback

import com.yaneodex.core.model.TrackRecord
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class QueueShuffleTest {
    @Test
    fun `pins current track when shuffle enabled`() {
        val tracks = sampleTracks()
        val queue = buildPlaybackQueue(tracks, "track-2", true, Random(42))

        assertEquals(tracks.size, queue.size)
        assertEquals("track-2", queue.first().id)
        assertEquals(tracks.map { it.id }.sorted(), queue.map { it.id }.sorted())
    }

    @Test
    fun `returns base order when shuffle disabled`() {
        val tracks = sampleTracks()
        val queue = buildPlaybackQueue(tracks, "track-3", false, Random(42))
        assertEquals(tracks, queue)
    }

    @Test
    fun `creates different orders for different random seeds`() {
        val tracks = sampleTracks()
        val first = buildPlaybackQueue(tracks, null, true, Random(5))
        val second = buildPlaybackQueue(tracks, null, true, Random(99))

        assertEquals(tracks.map { it.id }.sorted(), first.map { it.id }.sorted())
        assertEquals(tracks.map { it.id }.sorted(), second.map { it.id }.sorted())
        assertNotEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun `avoids adjacent same artist when alternatives exist`() {
        val tracks = listOf(
            sampleTrack("a-1", "Artist A"),
            sampleTrack("a-2", "Artist A"),
            sampleTrack("b-1", "Artist B"),
            sampleTrack("c-1", "Artist C"),
            sampleTrack("d-1", "Artist D"),
        )

        val queue = buildPlaybackQueue(tracks, "a-1", true, Random(42))
        assertEquals("a-1", queue.first().id)
        queue.zipWithNext().forEach { (left, right) -> assertNotEquals(left.artist, right.artist) }
    }

    @Test
    fun `falls back to any track when pinned track missing`() {
        val tracks = sampleTracks()
        val queue = buildPlaybackQueue(tracks, "missing", true, Random(7))
        assertEquals(tracks.size, queue.size)
        assertTrue(queue.first().id in tracks.map { it.id })
    }

    private fun sampleTracks(): List<TrackRecord> = List(4) { index ->
        TrackRecord(
            id = "track-$index",
            uri = "content://track-$index",
            title = "Track $index",
            artist = "Artist $index",
            durationMs = 180_000,
            importedAtEpochMs = 1_700_000_000_000L,
        )
    }

    private fun sampleTrack(id: String, artist: String): TrackRecord {
        return TrackRecord(
            id = id,
            uri = "content://$id",
            title = id,
            artist = artist,
            durationMs = 180_000,
            importedAtEpochMs = 1_700_000_000_000L,
        )
    }
}
