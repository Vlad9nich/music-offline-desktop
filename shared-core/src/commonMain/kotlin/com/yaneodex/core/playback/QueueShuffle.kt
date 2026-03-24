package com.yaneodex.core.playback

import com.yaneodex.core.model.TrackRecord
import kotlin.random.Random

fun buildPlaybackQueue(
    tracks: List<TrackRecord>,
    currentTrackId: String?,
    shuffleEnabled: Boolean,
    random: Random = Random.Default,
): List<TrackRecord> {
    if (tracks.isEmpty()) return emptyList()
    if (!shuffleEnabled) return tracks
    val pinnedTrack = tracks.firstOrNull { it.id == currentTrackId } ?: tracks.random(random)
    val remainder = buildArtistAwareShuffle(
        tracks = tracks.filterNot { it.id == pinnedTrack.id },
        previousArtist = pinnedTrack.artist,
        random = random,
    )
    return listOf(pinnedTrack) + remainder
}

private fun buildArtistAwareShuffle(
    tracks: List<TrackRecord>,
    previousArtist: String?,
    random: Random,
): List<TrackRecord> {
    if (tracks.isEmpty()) return emptyList()

    val buckets = tracks
        .groupBy { it.artist }
        .mapValues { (_, items) -> items.shuffled(random).toMutableList() }
        .toMutableMap()

    val result = mutableListOf<TrackRecord>()
    var lastArtist = previousArtist

    while (buckets.isNotEmpty()) {
        val preferredArtists = buckets.filterKeys { it != lastArtist }.ifEmpty { buckets }
        val maxBucketSize = preferredArtists.maxOf { it.value.size }
        val candidateArtists = preferredArtists.filterValues { it.size == maxBucketSize }.keys.toList()
        val nextArtist = candidateArtists.random(random)

        val bucket = requireNotNull(buckets[nextArtist])
        val nextTrack = bucket.removeAt(0)
        result += nextTrack
        lastArtist = nextTrack.artist
        if (bucket.isEmpty()) buckets.remove(nextArtist)
    }

    return result
}
