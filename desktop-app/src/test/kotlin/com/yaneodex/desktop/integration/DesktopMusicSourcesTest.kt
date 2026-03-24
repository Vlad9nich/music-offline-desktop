package com.yaneodex.desktop.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
}
