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

    @Test
    fun `reshuffle keeps every track and does not reopen on the finished one`() {
        val tracks = sampleTracks()
        val queue = reshufflePlaybackQueue(tracks, "track-2", Random(11))

        assertEquals(tracks.size, queue.size)
        assertEquals(tracks.map { it.id }.sorted(), queue.map { it.id }.sorted())
        assertNotEquals("track-2", queue.first().id)
    }

    @Test
    fun `reshuffle spreads artists and avoids back-to-back repeats`() {
        val tracks = listOf(
            sampleTrack("a-1", "Artist A"),
            sampleTrack("a-2", "Artist A"),
            sampleTrack("a-3", "Artist A"),
            sampleTrack("b-1", "Artist B"),
            sampleTrack("b-2", "Artist B"),
            sampleTrack("c-1", "Artist C"),
            sampleTrack("d-1", "Artist D"),
        )

        repeat(20) { seed ->
            val queue = reshufflePlaybackQueue(tracks, "a-1", Random(seed))
            assertEquals(tracks.map { it.id }.sorted(), queue.map { it.id }.sorted())
            queue.zipWithNext().forEach { (left, right) ->
                assertNotEquals(left.artist, right.artist, "seed $seed produced adjacent ${left.artist} tracks")
            }
        }
    }

    @Test
    fun `unshuffle restores the source order without moving the current track`() {
        val tracks = sampleTracks()
        val shuffled = buildPlaybackQueue(tracks, "track-2", true, Random(3))
        val current = tracks.first { it.id == shuffled.first().id }

        val restored = unshufflePlaybackQueue(tracks, current)

        // Turning shuffle off must not change what is playing — only what comes next.
        assertEquals(tracks.map { it.id }, restored.map { it.id })
    }

    @Test
    fun `unshuffle keeps a current track that is not part of the source`() {
        val tracks = sampleTracks()
        val stranger = sampleTrack("preview-1", "Some Artist")

        val restored = unshufflePlaybackQueue(tracks, stranger)

        assertEquals(listOf("preview-1") + tracks.map { it.id }, restored.map { it.id })
    }

    @Test
    fun `unshuffle is a no-op when shuffle was never applied`() {
        val tracks = sampleTracks()
        assertEquals(tracks, unshufflePlaybackQueue(tracks, tracks.first()))
    }

    @Test
    fun `shuffle handles a large library without quadratic blow-up`() {
        val artists = 250
        val tracks = List(5_000) { index ->
            sampleTrack("track-$index", "Artist ${index % artists}")
        }

        val startedAt = System.nanoTime()
        val queue = buildPlaybackQueue(tracks, "track-0", true, Random(1))
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

        assertEquals(tracks.size, queue.size)
        assertEquals(tracks.map { it.id }.sorted(), queue.map { it.id }.sorted())
        assertTrue(elapsedMs < 5_000, "shuffle took ${elapsedMs}ms for 5000 tracks")
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
