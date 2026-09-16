package com.workly.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class WorkRangeTest {

    @Test
    fun `week starts on monday by default`() {
        val wednesday = LocalDate.of(2026, 9, 16)

        val range = WorkRange.ofWeek(wednesday, DayOfWeek.MONDAY)

        assertEquals(LocalDate.of(2026, 9, 14), range.start)
        assertEquals(LocalDate.of(2026, 9, 20), range.endInclusive)
        assertEquals(7L, range.dayCount)
    }

    @Test
    fun `week can start on sunday`() {
        val wednesday = LocalDate.of(2026, 9, 16)

        val range = WorkRange.ofWeek(wednesday, DayOfWeek.SUNDAY)

        assertEquals(LocalDate.of(2026, 9, 13), range.start)
        assertEquals(LocalDate.of(2026, 9, 19), range.endInclusive)
    }

    @Test
    fun `week containing a monday starts that monday`() {
        val monday = LocalDate.of(2026, 9, 14)

        val range = WorkRange.ofWeek(monday, DayOfWeek.MONDAY)

        assertEquals(monday, range.start)
    }

    @Test
    fun `month range covers the whole month`() {
        val range = WorkRange.ofMonth(YearMonth.of(2026, 2))

        assertEquals(LocalDate.of(2026, 2, 1), range.start)
        assertEquals(LocalDate.of(2026, 2, 28), range.endInclusive)
        assertEquals(28L, range.dayCount)
    }

    @Test
    fun `custom range tolerates swapped dates`() {
        val range = WorkRange.custom(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 10))

        assertEquals(LocalDate.of(2026, 9, 10), range.start)
        assertEquals(LocalDate.of(2026, 9, 20), range.endInclusive)
    }

    @Test
    fun `contains is inclusive of the start and exclusive of the end`() {
        val range = WorkRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8))

        assertTrue(range.contains(LocalDate.of(2026, 9, 1)))
        assertTrue(range.contains(LocalDate.of(2026, 9, 7)))
        assertFalse(range.contains(LocalDate.of(2026, 9, 8)))
        assertFalse(range.contains(LocalDate.of(2026, 8, 31)))
    }

    @Test
    fun `dates lists every day once`() {
        val range = WorkRange.ofMonth(YearMonth.of(2026, 4))

        val dates = range.dates()

        assertEquals(30, dates.size)
        assertEquals(LocalDate.of(2026, 4, 1), dates.first())
        assertEquals(LocalDate.of(2026, 4, 30), dates.last())
    }

    @Test
    fun `instant range starts at midnight local time`() {
        val range = WorkRange.ofDay(LocalDate.of(2026, 9, 16))
        val zone = java.time.ZoneId.of("Asia/Tokyo")

        val (from, to) = range.asInstantRange(zone)

        assertEquals(24 * 60 * 60L, java.time.Duration.between(from, to).seconds)
    }
}
