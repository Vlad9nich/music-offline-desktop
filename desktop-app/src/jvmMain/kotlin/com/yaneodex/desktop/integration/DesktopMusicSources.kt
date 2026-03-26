package com.yaneodex.desktop.integration

import com.yaneodex.core.contracts.MusicSource
import com.yaneodex.core.contracts.MusicSourceCatalog
import com.yaneodex.core.model.DownloadBlueprint
import com.yaneodex.core.model.RemoteTrackCandidate
import com.yaneodex.core.model.SourceDescriptor
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

abstract class HtmlMusicSourceTemplate(
    override val descriptor: SourceDescriptor,
) : MusicSource {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    override suspend fun search(query: String): List<RemoteTrackCandidate> {
        if (query.isBlank()) return emptyList()
        return mapSearchDocument(requestDocument(buildSearchUrl(query.trim())))
    }

    override suspend fun resolve(track: RemoteTrackCandidate): DownloadBlueprint {
        return mapTrackDetails(track, requestDocument(track.detailUrl, referer = DEFAULT_REFERER))
    }

    protected abstract fun buildSearchUrl(query: String): String
    protected abstract fun mapSearchDocument(document: Document): List<RemoteTrackCandidate>
    protected abstract fun mapTrackDetails(track: RemoteTrackCandidate, document: Document): DownloadBlueprint

    protected fun requestDocument(url: String, referer: String? = null): Document {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 YaNeoDexDesktop/0.1")
            .header("Referer", referer ?: DEFAULT_REFERER)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        require(response.statusCode() in 200..299) {
            "Parser HTTP ${response.statusCode()}: ${response.body().take(160)}"
        }
        return Jsoup.parse(response.body(), url)
    }

    protected fun Element.textOrEmpty(selector: String): String = selectFirst(selector)?.text()?.trim().orEmpty()

    companion object {
        protected const val DEFAULT_REFERER = "https://web.ligaudio.ru/"
    }
}

class LigaudioSource : HtmlMusicSourceTemplate(
    descriptor = SourceDescriptor(
        id = "ligaudio",
        name = "Ligaudio",
        status = "Active",
        description = "Remote search and download resolution via web.ligaudio.ru",
        isEnabled = true,
    ),
) {
    internal fun parseSearchHtml(html: String, baseUrl: String): List<RemoteTrackCandidate> {
        return mapSearchDocument(Jsoup.parse(html, baseUrl))
    }

    override fun buildSearchUrl(query: String): String {
        return "https://web.ligaudio.ru/mp3/${URLEncoder.encode(query, StandardCharsets.UTF_8.toString()).replace("+", "%20")}"
    }

    override fun mapSearchDocument(document: Document): List<RemoteTrackCandidate> {
        return parseTrackCards(document).map {
            RemoteTrackCandidate(
                sourceId = descriptor.id,
                title = it.title,
                artist = it.artist,
                detailUrl = it.detailUrl,
                downloadUrl = it.directDownloadUrl,
            )
        }
    }

    override fun mapTrackDetails(track: RemoteTrackCandidate, document: Document): DownloadBlueprint {
        val directUrl = track.downloadUrl?.takeIf(::looksLikeDownloadUrl)
        if (!directUrl.isNullOrBlank()) {
            return DownloadBlueprint(track.title, track.artist, directUrl, "${track.artist} - ${track.title}.mp3", DEFAULT_REFERER)
        }

        val pageBaseUrl = document.location().ifBlank { track.detailUrl }
        val listing = parseTrackCards(document).firstOrNull { it.matches(track) }
        val resolvedUrl = firstNotBlank(
            listing?.directDownloadUrl?.takeIf(::looksLikeDownloadUrl),
            extractDirectDownloadUrl(document.body(), document.body(), pageBaseUrl),
        )?.takeIf(::looksLikeDownloadUrl)
            ?: error("Unable to resolve direct download URL.")

        return DownloadBlueprint(track.title, track.artist, resolvedUrl, "${track.artist} - ${track.title}.mp3", DEFAULT_REFERER)
    }

    private fun parseTrackCards(document: Document): List<LigaudioListing> {
        val items = document.select("#result .item[itemprop='track']").ifEmpty { document.select("#result .item") }
        if (items.isEmpty()) return emptyList()
        val baseUrl = document.location().ifBlank { "https://web.ligaudio.ru/" }
        val listings = linkedMapOf<String, LigaudioListing>()

        for (item in items) {
            val container = item.selectFirst("div") ?: item
            val title = firstNotBlank(
                item.textOrEmpty(".title .title"),
                item.textOrEmpty("span.title[itemprop='name']"),
                container.textOrEmpty(".title"),
                parseArtistTitlePair(container.text()).second,
            ).orEmpty()
            val artist = firstNotBlank(
                item.textOrEmpty(".title .autor"),
                item.textOrEmpty("span.autor[itemprop='byArtist']"),
                item.select(".autor a[href*='/mp3/']").joinToString(", ") { it.text().trim() }.takeIf { text -> text.isNotBlank() },
                parseArtistTitlePair(container.text()).first,
            ).orEmpty()
            val directDownloadUrl = extractDirectDownloadUrl(item, container, baseUrl)
            if (title.isBlank() || directDownloadUrl.isNullOrBlank()) continue

            val detailUrl = normalizeUrlCandidate(item.selectFirst(".autor a[href*='/mp3/']")?.attr("href"), baseUrl)
                ?: "https://web.ligaudio.ru/"

            val listing = LigaudioListing(title, artist.ifBlank { "Unknown artist" }, detailUrl, directDownloadUrl)
            listings.putIfAbsent(listing.uniqueKey(), listing)
        }

        return listings.values.toList()
    }

    private fun extractDirectDownloadUrl(item: Element, container: Element, baseUrl: String): String? {
        val selectors = listOf(
            "a.down[href]",
            "a.download[href]",
            "a[href*='storage'][href]",
            "a[href*='.mp3'][href]",
            "a[href*='.m4a'][href]",
            "a[download]",
            "[data-url]",
            "[data-file]",
            "[data-download]",
        )
        selectors.forEach { selector ->
            val direct = listOf(item, container).firstNotNullOfOrNull { element ->
                element.selectFirst(selector)?.let { candidate ->
                    normalizeUrlCandidate(
                        firstNotBlank(
                            candidate.absUrl("href"),
                            candidate.attr("href"),
                            candidate.attr("data-url"),
                            candidate.attr("data-download"),
                            candidate.attr("data-file"),
                            candidate.attr("download"),
                        ),
                        baseUrl,
                    )?.takeIf(::looksLikeDownloadUrl)
                }
            }
            if (!direct.isNullOrBlank()) return direct
        }
        return null
    }

    private fun normalizeUrlCandidate(value: String?, baseUrl: String): String? {
        val trimmed = value?.trim().orEmpty().removePrefix("javascript:").trim('"', '\'', ' ').replace("\\/", "/").replace("&amp;", "&")
        if (trimmed.isBlank()) return null
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> URI.create(baseUrl).resolve(trimmed).toString()
        }
    }

    private fun looksLikeDownloadUrl(url: String): Boolean {
        val lowered = url.lowercase()
        return lowered.contains("storage.lightaudio.ru") || lowered.contains(".mp3") || lowered.contains(".m4a") || lowered.contains(".ogg")
    }

    private fun parseArtistTitlePair(rawText: String): Pair<String, String> {
        val cleaned = rawText.replace(Regex("\\s+"), " ").replace(Regex("\\b\\d{1,2}:\\d{2}\\b"), "").trim(' ', '-', '–', '—', '•')
        if (cleaned.isBlank()) return "" to ""
        listOf(" - ", " – ", " — ").forEach { delimiter ->
            val index = cleaned.indexOf(delimiter)
            if (index > 0) {
                return cleaned.substring(0, index).trim() to cleaned.substring(index + delimiter.length).trim()
            }
        }
        return "" to cleaned
    }

    private fun firstNotBlank(vararg values: String?): String? = values.firstOrNull { !it.isNullOrBlank() }?.trim()

    private data class LigaudioListing(
        val title: String,
        val artist: String,
        val detailUrl: String,
        val directDownloadUrl: String?,
    ) {
        fun uniqueKey(): String = "${artist.lowercase()}|${title.lowercase()}|${directDownloadUrl.orEmpty()}|${detailUrl}"
        fun matches(track: RemoteTrackCandidate): Boolean = title.equals(track.title, true) && artist.equals(track.artist, true)
    }
}

class DesktopMusicSourceCatalog(
    private val sources: List<MusicSource> = listOf(LigaudioSource()),
) : MusicSourceCatalog {
    override val descriptors: List<SourceDescriptor>
        get() = sources.map { it.descriptor }

    override suspend fun search(query: String): List<RemoteTrackCandidate> {
        if (query.isBlank()) return emptyList()
        return sources.filter { it.descriptor.isEnabled }.flatMap { source -> source.search(query) }
    }

    override suspend fun resolve(track: RemoteTrackCandidate): DownloadBlueprint {
        val source = sources.firstOrNull { it.descriptor.id == track.sourceId }
            ?: error("Source ${track.sourceId} not found.")
        return source.resolve(track)
    }
}
