package com.workly.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridTest {

    @Test
    fun `every week has exactly seven cells`() {
        val weeks = CalendarGrid.weeks(YearMonth.of(2026, 9), DayOfWeek.MONDAY)

        weeks.forEach { assertEquals(7, it.size) }
    }

    @Test
    fun `september 2026 starts on a tuesday`() {
        val weeks = CalendarGrid.weeks(YearMonth.of(2026, 9), DayOfWeek.MONDAY)

        // 1 September 2026 is a Tuesday, so Monday is padding and Tuesday holds day 1.
        assertNull(weeks.first()[0])
        assertEquals(LocalDate.of(2026, 9, 1), weeks.first()[1])
    }

    @Test
    fun `changing the first day of the week shifts the padding`() {
        val mondayFirst = CalendarGrid.weeks(YearMonth.of(2026, 9), DayOfWeek.MONDAY)
        val sundayFirst = CalendarGrid.weeks(YearMonth.of(2026, 9), DayOfWeek.SUNDAY)

        assertNull(sundayFirst.first()[0])
        assertEquals(LocalDate.of(2026, 9, 1), sundayFirst.first()[2])
        assertNull(mondayFirst.first()[0])
        assertEquals(LocalDate.of(2026, 9, 1), mondayFirst.first()[1])
    }

    @Test
    fun `a month starting on the first day of the week has no leading padding`() {
        // 1 June 2026 is a Monday.
        val weeks = CalendarGrid.weeks(YearMonth.of(2026, 6), DayOfWeek.MONDAY)

        assertEquals(LocalDate.of(2026, 6, 1), weeks.first().first())
    }

    @Test
    fun `every day of the month appears exactly once`() {
        val month = YearMonth.of(2026, 2)

        val days = CalendarGrid.weeks(month, DayOfWeek.MONDAY).flatten().filterNotNull()

        assertEquals(28, days.size)
        assertEquals((1..28).map { month.atDay(it) }, days)
    }

    @Test
    fun `weekday order follows the configured first day`() {
        assertEquals(
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
            ),
            CalendarGrid.weekdayOrder(DayOfWeek.MONDAY),
        )
        assertEquals(DayOfWeek.SUNDAY, CalendarGrid.weekdayOrder(DayOfWeek.SUNDAY).first())
    }
}
