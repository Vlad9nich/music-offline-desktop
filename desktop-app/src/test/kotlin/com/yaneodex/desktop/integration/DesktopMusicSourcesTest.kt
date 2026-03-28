package com.yaneodex.desktop.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.yaneodex.core.model.RemoteTrackCandidate

class DesktopMusicSourcesTest {
    @Test
    fun `parseSearchHtml extracts candidates from ligaudio cards`() {
        val html = """
            <html>
              <body>
                <div id="result">
                  <div class="item" itemprop="track">
                    <div>
                      <span class="title" itemprop="name">Afterglow</span>
                      <span class="autor" itemprop="byArtist">Aurora Lane</span>
                      <a class="down" href="https://storage.lightaudio.ru/audio/afterglow.mp3">download</a>
                    </div>
                  </div>
                </div>
              </body>
            </html>
        """.trimIndent()

        val results = LigaudioSource().parseSearchHtml(html, "https://web.ligaudio.ru/mp3/test")

        assertEquals(1, results.size)
        assertEquals("Afterglow", results.first().title)
        assertEquals("Aurora Lane", results.first().artist)
        assertTrue(results.first().downloadUrl!!.contains("afterglow.mp3"))
    }

    @Test
    fun `rankResults filters unrelated parser matches and keeps relevant ones first`() {
        val catalog = DesktopMusicSourceCatalog(emptyList())
        val results = listOf(
            RemoteTrackCandidate("ligaudio", "Lose Yourself", "Eminem", "https://example.com/1"),
            RemoteTrackCandidate("ligaudio", "Mockingbird", "Eminem", "https://example.com/2"),
            RemoteTrackCandidate("ligaudio", "Random Summer", "Totally Different", "https://example.com/3"),
        )

        val ranked = catalog.rankResults("eminem", results)

        assertEquals(2, ranked.size)
        assertTrue(ranked.all { it.artist.equals("Eminem", ignoreCase = true) })
        assertEquals("Lose Yourself", ranked.first().title)
    }
}
