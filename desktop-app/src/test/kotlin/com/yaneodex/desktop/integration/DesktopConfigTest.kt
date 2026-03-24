package com.yaneodex.desktop.integration

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopConfigTest {
    @Test
    fun `loads values from dotenv file`() {
        val root = createTempDirectory("yaneodex-config").toFile()
        try {
            File(root, ".env").writeText(
                """
                YANEODEX_LIBRARY_PATH=C:\Music\YaNeoDex
                YANEODEX_OCR_BASE_URL=https://ocr.local
                YANEODEX_OCR_TOKEN=test-token
                YANEODEX_DOWNLOAD_DIR=C:\Music\YaNeoDex\Downloads
                """.trimIndent(),
            )

            val config = DesktopConfig.load(root)

            assertEquals("C:\\Music\\YaNeoDex", config.libraryPath)
            assertEquals("https://ocr.local", config.ocrBaseUrl)
            assertEquals("test-token", config.ocrToken)
            assertEquals("C:\\Music\\YaNeoDex\\Downloads", config.downloadDir)
        } finally {
            root.deleteRecursively()
        }
    }
}
