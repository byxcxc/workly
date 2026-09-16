package com.workly.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test
    fun `six and a half hours reads as 6 point 5`() {
        assertEquals("6.5", decimalHours(6 * 60 + 30))
    }

    @Test
    fun `whole hours drop the decimal part`() {
        assertEquals("8", decimalHours(8 * 60))
        assertEquals("0", decimalHours(0))
    }

    @Test
    fun `minutes are rounded to two decimals`() {
        // 7h20m = 7.333... hours
        assertEquals("7.33", decimalHours(7 * 60 + 20))
        // 22m = 0.3666... hours
        assertEquals("0.37", decimalHours(22))
    }

    @Test
    fun `negative input never produces a negative duration`() {
        assertEquals("0", decimalHours(-30))
    }
}
