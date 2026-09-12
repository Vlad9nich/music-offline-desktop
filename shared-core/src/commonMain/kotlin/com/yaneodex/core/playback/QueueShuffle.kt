package com.yaneodex.core.playback

import com.yaneodex.core.model.TrackRecord
import kotlin.random.Random

/**
 * How many recently played artists are held back before they can be picked again.
 *
 * 1 means "never twice in a row", which is the strongest guarantee the greedy can make:
 * with a larger window a long artist run can be pushed into the tail and end up adjacent
 * anyway, so 1 is both the safest and the most predictable default. Raise it explicitly if
 * you want a wider spread and can accept that trade-off.
 */
const val DEFAULT_ARTIST_WINDOW: Int = 1

/**
 * Builds the play order for a queue.
 *
 * When shuffle is enabled the current track is pinned to the front so playback never
 * jumps away from what the listener just picked, and the remainder is spread across
 * artists instead of being a plain random permutation.
 *
 * @param recentArtistWindow how many recently used artists are skipped when possible.
 */
fun buildPlaybackQueue(
    tracks: List<TrackRecord>,
    currentTrackId: String?,
    shuffleEnabled: Boolean,
    random: Random = Random.Default,
    recentArtistWindow: Int = DEFAULT_ARTIST_WINDOW,
): List<TrackRecord> {
    if (tracks.isEmpty()) return emptyList()
    if (!shuffleEnabled) return tracks

    val pinnedTrack = tracks.firstOrNull { it.id == currentTrackId } ?: tracks.random(random)
    val remainder = spreadByArtist(
        tracks = tracks.filterNot { it.id == pinnedTrack.id },
        previousArtist = pinnedTrack.artist,
        random = random,
        recentArtistWindow = recentArtistWindow,
    )
    return listOf(pinnedTrack) + remainder
}

/**
 * Builds a fresh shuffled order for the whole source set.
 *
 * Called when the queue runs out, so playback continues instead of dead-ending on the last
 * track. The track that just finished is moved off the front when possible, so a repeat-all
 * cycle does not restart on the same song.
 */
fun reshufflePlaybackQueue(
    tracks: List<TrackRecord>,
    previousTrackId: String?,
    random: Random = Random.Default,
    recentArtistWindow: Int = DEFAULT_ARTIST_WINDOW,
): List<TrackRecord> {
    if (tracks.size <= 1) return tracks.toList()

    val previousArtist = tracks.firstOrNull { it.id == previousTrackId }?.artist
    val ordered = spreadByArtist(tracks, previousArtist, random, recentArtistWindow)
    if (ordered.size <= 1 || ordered.first().id != previousTrackId) return ordered

    // Don't open the new cycle with the track that just ended.
    val swapIndex = 1 + random.nextInt(ordered.size - 1)
    return ordered.toMutableList().apply {
        val first = this[0]
        this[0] = this[swapIndex]
        this[swapIndex] = first
    }
}

/**
 * Restores the unshuffled order when shuffle is switched off.
 *
 * The player re-anchors on the current track by id, so that track does not have to be moved to
 * the front — the listener keeps hearing the same song and simply gets the original sequence as
 * "up next". The current track is only prepended when it is missing from [originalOrder]
 * entirely, so turning shuffle off can never make playback jump to a different song.
 */
fun unshufflePlaybackQueue(
    originalOrder: List<TrackRecord>,
    currentTrack: TrackRecord?,
): List<TrackRecord> {
    if (originalOrder.isEmpty()) return originalOrder
    val current = currentTrack ?: return originalOrder
    if (originalOrder.any { it.id == current.id }) return originalOrder
    return listOf(current) + originalOrder
}

/**
 * Artist-aware spread.
 *
 * Buckets are drained through a max-heap keyed on the number of tracks each artist still has
 * left, so the largest group is spread out first. That keeps the "avoid back-to-back same
 * artist" guarantee of the previous implementation at O(n log k) instead of rescanning every
 * bucket on every pick.
 */
private fun spreadByArtist(
    tracks: List<TrackRecord>,
    previousArtist: String?,
    random: Random,
    recentArtistWindow: Int,
): List<TrackRecord> {
    if (tracks.size <= 1) return tracks.toList()

    val buckets = tracks
        .groupBy { it.artist }
        .map { (artist, items) -> ArtistBucket(artist, items.shuffled(random).toMutableList()) }
        .shuffled(random)

    val heap = ArtistBucketHeap(buckets, random)
    val result = ArrayList<TrackRecord>(tracks.size)
    val recentArtists = ArrayDeque<String>()
    if (!previousArtist.isNullOrBlank()) recentArtists.addLast(previousArtist)

    while (!heap.isEmpty()) {
        var bucket = heap.pop() ?: break
        var deferred: ArtistBucket? = null

        // Step over an artist we heard too recently, if another one is available.
        if (recentArtists.contains(bucket.artist)) {
            val alternative = heap.pop()
            if (alternative != null) {
                deferred = bucket
                bucket = alternative
            }
        }

        result += bucket.take()
        rememberArtist(recentArtists, bucket.artist, recentArtistWindow)

        if (!bucket.isEmpty()) heap.push(bucket)
        deferred?.let(heap::push)
    }

    return result
}

private fun rememberArtist(recentArtists: ArrayDeque<String>, artist: String, window: Int) {
    if (window <= 0) return
    recentArtists.addLast(artist)
    while (recentArtists.size > window) recentArtists.removeFirst()
}

private class ArtistBucket(
    val artist: String,
    private val tracks: MutableList<TrackRecord>,
) {
    private var nextIndex: Int = 0

    /**
     * Random tie-breaker. Without it a library where every artist has one track would always
     * come out in the same order, no matter the seed.
     */
    var salt: Int = 0

    val remaining: Int get() = tracks.size - nextIndex

    fun isEmpty(): Boolean = remaining <= 0

    fun take(): TrackRecord = tracks[nextIndex++]
}

/** Minimal max-heap on [ArtistBucket.remaining], with a random salt for equal sizes. */
private class ArtistBucketHeap(items: List<ArtistBucket>, private val random: Random) {
    private val data = ArrayList<ArtistBucket>(items.size)

    init {
        items.forEach { it.salt = random.nextInt() }
        data.addAll(items)
        for (index in data.size / 2 - 1 downTo 0) siftDown(index)
    }

    fun isEmpty(): Boolean = data.isEmpty()

    fun push(item: ArtistBucket) {
        item.salt = random.nextInt()
        data.add(item)
        siftUp(data.lastIndex)
    }

    fun pop(): ArtistBucket? {
        if (data.isEmpty()) return null
        val top = data[0]
        val last = data.removeAt(data.lastIndex)
        if (data.isNotEmpty()) {
            data[0] = last
            siftDown(0)
        }
        return top
    }

    private fun wins(a: ArtistBucket, b: ArtistBucket): Boolean =
        a.remaining > b.remaining || (a.remaining == b.remaining && a.salt > b.salt)

    private fun siftUp(startIndex: Int) {
        var index = startIndex
        while (index > 0) {
            val parent = (index - 1) / 2
            if (!wins(data[index], data[parent])) break
            data.swap(parent, index)
            index = parent
        }
    }

    private fun siftDown(startIndex: Int) {
        var index = startIndex
        while (true) {
            val left = index * 2 + 1
            val right = left + 1
            var largest = index
            if (left < data.size && wins(data[left], data[largest])) largest = left
            if (right < data.size && wins(data[right], data[largest])) largest = right
            if (largest == index) break
            data.swap(index, largest)
            index = largest
        }
    }

    private fun ArrayList<ArtistBucket>.swap(a: Int, b: Int) {
        val tmp = this[a]
        this[a] = this[b]
        this[b] = tmp
    }
}
