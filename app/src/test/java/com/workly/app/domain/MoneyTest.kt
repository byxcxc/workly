package com.workly.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MoneyTest {

    @Test
    fun `income is exact for a whole number of hours`() {
        // 1,600.00 per hour for 8 hours
        assertEquals(1_280_000L, Money.incomeMinor(160_000L, 480L))
    }

    @Test
    fun `income for seven hours thirty two minutes`() {
        // 1600.00 * 452 / 60 = 12053.333... -> 12053.33
        assertEquals(1_205_333L, Money.incomeMinor(160_000L, 452L))
    }

    @Test
    fun `income rounds half up`() {
        // 1 cent per hour for 30 minutes = 0.5 cents -> 1 cent
        assertEquals(1L, Money.incomeMinor(1L, 30L))
        // 1 cent per hour for 29 minutes = 0.483 cents -> 0 cents
        assertEquals(0L, Money.incomeMinor(1L, 29L))
        assertEquals(0L, Money.incomeMinor(100L, 0L))
    }

    @Test
    fun `income never drifts with many small sessions`() {
        // 10 sessions of 6 minutes at 100.00/h each: 10 * 1000 minor
        val perSession = Money.incomeMinor(10_000L, 6L)
        assertEquals(1_000L, perSession)
        assertEquals(10_000L, (1..10).sumOf { perSession })
    }

    @Test
    fun `yen has no decimals`() {
        assertEquals(0, Money.fractionDigits("JPY"))
        assertEquals("¥12,480", Money.format(12_480L, "JPY", Locale.US))
    }

    @Test
    fun `dollar amounts show two decimals`() {
        assertEquals(2, Money.fractionDigits("USD"))
        assertEquals("$12,480.00", Money.format(1_248_000L, "USD", Locale.US))
        assertEquals("$12,480.50", Money.format(1_248_050L, "USD", Locale.US))
    }

    @Test
    fun `negative amounts keep the sign in front of the symbol`() {
        assertEquals("-$5.00", Money.format(-500L, "USD", Locale.US))
    }

    @Test
    fun `unknown currency falls back to two decimals`() {
        assertEquals(2, Money.fractionDigits("ZZZ"))
    }

    @Test
    fun `parsing accepts separators and symbols`() {
        assertEquals(160_000L, Money.parseToMinor("1600", "USD"))
        assertEquals(160_050L, Money.parseToMinor("1600.50", "USD"))
        assertEquals(160_000L, Money.parseToMinor("1,600", "USD"))
        assertEquals(1_600L, Money.parseToMinor("¥1,600", "JPY"))
        assertEquals(1_600L, Money.parseToMinor(" 1600 ", "JPY"))
    }

    @Test
    fun `parsing rejects nonsense`() {
        assertNull(Money.parseToMinor("", "USD"))
        assertNull(Money.parseToMinor("abc", "USD"))
        assertNull(Money.parseToMinor("-", "USD"))
    }

    @Test
    fun `round trip between editable text and minor units`() {
        assertEquals("1600", Money.toEditableString(1_600L, "JPY"))
        assertEquals("1600.5", Money.toEditableString(160_050L, "USD"))
        assertEquals("0", Money.toEditableString(0L, "USD"))
        assertEquals(160_050L, Money.parseToMinor(Money.toEditableString(160_050L, "USD"), "USD"))
    }

    @Test
    fun `compact formatting is used for chart labels`() {
        assertEquals("¥12.5k", Money.formatCompact(12_500L, "JPY", Locale.US))
        assertTrue(Money.formatCompact(1_250_000_000L, "JPY", Locale.US).endsWith("M"))
    }
}
