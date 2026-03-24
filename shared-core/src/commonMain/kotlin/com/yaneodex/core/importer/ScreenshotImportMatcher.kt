package com.yaneodex.core.importer

import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.model.TrackRecord

object ScreenshotImportMatcher {
    private const val LOW_CONFIDENCE_THRESHOLD = 0.54f
    private const val MATCH_THRESHOLD = 0.72f

    fun deduplicate(candidates: List<RecognizedTrackCandidate>): List<RecognizedTrackCandidate> {
        return candidates
            .map(::normalizeRecognizedCandidate)
            .groupBy { uniqueImportKey(it) }
            .values
            .map { group -> group.maxByOrNull { it.confidence } ?: group.first() }
            .sortedWith(compareBy<RecognizedTrackCandidate> { it.screenshotIndex }.thenBy { normalizeLoose(it.rawText) })
    }

    fun pickBestMatch(
        candidate: RecognizedTrackCandidate,
        results: List<RemoteTrackCandidate>,
        playlistTracks: List<TrackRecord>,
    ): MatchedTrackCandidate {
        val normalized = normalizeRecognizedCandidate(candidate)
        if (results.isEmpty()) {
            return MatchedTrackCandidate(normalized, null, 0f, ScreenshotImportItemStatus.NOT_FOUND, "No source matches.")
        }

        val scored = results
            .distinctBy { "${it.sourceId}|${it.title}|${it.artist}|${it.downloadUrl ?: it.detailUrl}" }
            .map { track -> track to score(normalized, track) }
            .sortedByDescending { it.second }

        val (bestMatch, bestScore) = scored.first()
        val alreadyInPlaylist = playlistTracks.any { track ->
            titleSimilarity(normalizeLoose(normalized.titleGuess), normalizeLoose(track.title)) >= 0.92f &&
                artistSimilarity(normalizeLoose(normalized.artistGuess), normalizeLoose(track.artist)) >= 0.78f
        }

        return when {
            alreadyInPlaylist -> MatchedTrackCandidate(normalized, bestMatch, bestScore, ScreenshotImportItemStatus.ALREADY_IN_PLAYLIST, "Already in playlist.")
            bestScore >= MATCH_THRESHOLD -> MatchedTrackCandidate(normalized, bestMatch, bestScore, ScreenshotImportItemStatus.MATCHED)
            bestScore >= LOW_CONFIDENCE_THRESHOLD -> MatchedTrackCandidate(normalized, bestMatch, bestScore, ScreenshotImportItemStatus.LOW_CONFIDENCE_MATCH, "Looks close, review it.")
            else -> MatchedTrackCandidate(normalized, null, bestScore, ScreenshotImportItemStatus.NOT_FOUND, "No reliable match.")
        }
    }

    fun normalizeLoose(value: String): String {
        return value
            .lowercase()
            .replace(Regex("""[|/\\]+"""), " ")
            .replace(Regex("""[(){}\[\],.!?;:+*=_~`"'“”]+"""), " ")
            .replace(Regex("""\b(feat|ft|prod|remix|version|edit|single|explicit|spedup)\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun normalizeRecognizedCandidate(candidate: RecognizedTrackCandidate): RecognizedTrackCandidate {
        val title = cleanup(candidate.titleGuess.ifBlank { candidate.rawText.substringBefore('|') })
        val artist = cleanup(candidate.artistGuess.ifBlank { candidate.rawText.substringAfter('|', "") })
        val raw = if (title.isNotBlank() && artist.isNotBlank()) "$title | $artist" else cleanup(candidate.rawText)
        return candidate.copy(rawText = raw, titleGuess = title, artistGuess = artist)
    }

    private fun uniqueImportKey(candidate: RecognizedTrackCandidate): String {
        return "${normalizeLoose(candidate.artistGuess)}|${normalizeLoose(candidate.titleGuess)}|${normalizeLoose(candidate.rawText)}"
    }

    private fun score(candidate: RecognizedTrackCandidate, track: RemoteTrackCandidate): Float {
        val candidateTitle = normalizeLoose(candidate.titleGuess)
        val candidateArtist = normalizeLoose(candidate.artistGuess)
        val trackTitle = normalizeLoose(track.title)
        val trackArtist = normalizeLoose(track.artist)
        val candidateRaw = normalizeLoose(candidate.rawText.replace("|", " "))
        val combinedCandidate = listOf(candidateTitle, candidateArtist).filter { it.isNotBlank() }.joinToString(" ")
        val combinedTrack = listOf(trackTitle, trackArtist).filter { it.isNotBlank() }.joinToString(" ")

        val titleScore = titleSimilarity(candidateTitle, trackTitle)
        val artistScore = artistSimilarity(candidateArtist, trackArtist)
        val orderedTitleScore = orderSimilarity(candidateTitle, trackTitle)
        val combinedOrderScore = orderSimilarity(combinedCandidate, combinedTrack)
        val rawScore = containsSimilarity(candidateRaw, combinedTrack)
        val exactTitleBonus = if (candidateTitle == trackTitle && candidateTitle.isNotBlank()) 0.10f else 0f
        val exactArtistBonus = if (candidateArtist == trackArtist && candidateArtist.isNotBlank()) 0.06f else 0f

        return (
            titleScore * 0.40f +
                orderedTitleScore * 0.22f +
                artistScore * 0.18f +
                combinedOrderScore * 0.14f +
                rawScore * 0.06f +
                exactTitleBonus +
                exactArtistBonus
            ).coerceIn(0f, 1f)
    }

    private fun titleSimilarity(left: String, right: String): Float {
        if (left.isBlank() || right.isBlank()) return 0f
        return maxOf(tokenSimilarity(left, right), containsSimilarity(left, right), orderSimilarity(left, right))
    }

    private fun artistSimilarity(left: String, right: String): Float {
        if (left.isBlank() || right.isBlank()) return 0.16f
        return maxOf(tokenSimilarity(left, right), containsSimilarity(left, right), orderSimilarity(left, right))
    }

    private fun containsSimilarity(left: String, right: String): Float {
        if (left.isBlank() || right.isBlank()) return 0f
        return when {
            left == right -> 1f
            left.contains(right) || right.contains(left) -> minOf(left.length, right.length).toFloat() / maxOf(left.length, right.length).toFloat()
            else -> 0f
        }
    }

    private fun tokenSimilarity(left: String, right: String): Float {
        val leftTokens = tokenize(left).toSet()
        val rightTokens = tokenize(right).toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0f
        return leftTokens.intersect(rightTokens).size.toFloat() / leftTokens.union(rightTokens).size.toFloat().coerceAtLeast(1f)
    }

    private fun orderSimilarity(left: String, right: String): Float {
        val leftTokens = tokenize(left)
        val rightTokens = tokenize(right)
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0f
        if (leftTokens == rightTokens) return 1f
        val lcsLength = longestCommonSubsequence(leftTokens, rightTokens)
        val lcsScore = lcsLength.toFloat() / maxOf(leftTokens.size, rightTokens.size).toFloat()
        return lcsScore.coerceIn(0f, 1f)
    }

    private fun longestCommonSubsequence(left: List<String>, right: List<String>): Int {
        val dp = Array(left.size + 1) { IntArray(right.size + 1) }
        for (i in left.indices) {
            for (j in right.indices) {
                dp[i + 1][j + 1] = if (left[i] == right[j]) dp[i][j] + 1 else maxOf(dp[i][j + 1], dp[i + 1][j])
            }
        }
        return dp[left.size][right.size]
    }

    private fun tokenize(value: String): List<String> = normalizeLoose(value).split(' ').filter { it.length >= 2 }

    private fun cleanup(value: String): String = value.replace(Regex("""\s+"""), " ").trim(' ', '-', '—', '–', '|', ',', '.')
}
