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
                YANEODEX_DOWNLOAD_DIR=C:\Music\YaNeoDex\Downloads
                """.trimIndent(),
            )

            val config = DesktopConfig.load(root)

            assertEquals("C:\\Music\\YaNeoDex", config.libraryPath)
            assertEquals("C:\\Music\\YaNeoDex\\Downloads", config.downloadDir)
        } finally {
            root.deleteRecursively()
        }
    }
}
