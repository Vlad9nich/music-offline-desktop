package com.yaneodex.desktop.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopLayoutStateTest {
    @Test
    fun `timeline position is clamped to valid bounds`() {
        assertEquals(0L, clampTimelinePosition(-250L, 180_000L))
        assertEquals(180_000L, clampTimelinePosition(250_000L, 180_000L))
        assertEquals(42_000L, clampTimelinePosition(42_000L, 180_000L))
    }

    @Test
    fun `timeline keyboard step stays in polished desktop range`() {
        assertEquals(3_000L, timelineKeyboardStepMs(20_000L))
        assertEquals(7_500L, timelineKeyboardStepMs(180_000L))
        assertEquals(12_000L, timelineKeyboardStepMs(500_000L))
    }

    @Test
    fun `compact breakpoints preserve dense layout on smaller widths`() {
        assertTrue(useCompactPlayerLayout(920))
        assertFalse(useCompactPlayerLayout(1040))
        assertTrue(useCompactTrackRowLayout(720))
        assertFalse(useCompactTrackRowLayout(840))
    }
}
