package com.yaneodex.desktop.integration

import com.yaneodex.core.state.DemoLibrary
import com.yaneodex.core.state.DesktopSection
import com.yaneodex.core.state.OcrSettings
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DesktopPersistenceTest {
    @Test
    fun `save and load persisted desktop state`() {
        val file = File.createTempFile("yaneodex-desktop", ".json")
        try {
            val persistence = DesktopPersistence(file)
            val state = DemoLibrary.initialState().copy(
                selectedSection = DesktopSection.IMPORT,
                searchQuery = "night drive",
                ocrSettings = OcrSettings(serverUrl = "https://ocr.example", authToken = "token-1"),
            )

            persistence.save(state)
            val loaded = persistence.load()

            assertNotNull(loaded)
            assertEquals(DesktopSection.IMPORT, loaded.selectedSection)
            assertEquals("night drive", loaded.searchQuery)
            assertEquals("https://ocr.example", loaded.ocrSettings.serverUrl)
        } finally {
            file.delete()
        }
    }
}
