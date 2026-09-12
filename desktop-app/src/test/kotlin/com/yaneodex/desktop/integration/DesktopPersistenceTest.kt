package com.yaneodex.desktop.integration

import com.yaneodex.core.state.DemoLibrary
import com.yaneodex.core.state.DesktopSection
import com.yaneodex.core.state.AppLanguage
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
                selectedSection = DesktopSection.LIBRARY,
                language = AppLanguage.EN,
                searchQuery = "night drive",
            )

            persistence.save(state)
            val loaded = persistence.load()

            assertNotNull(loaded)
            assertEquals(DesktopSection.LIBRARY, loaded.selectedSection)
            assertEquals(AppLanguage.EN, loaded.language)
            assertEquals("night drive", loaded.searchQuery)
        } finally {
            file.delete()
        }
    }
}
