package com.workly.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class WorkScheduleTest {

    private val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    private val monday = LocalDate.of(2026, 9, 14)

    @Test
    fun `a two day weekend leaves five working days`() {
        assertEquals(5, WorkSchedule.workingDaysPerWeek(weekend))
        assertEquals(7, WorkSchedule.workingDaysPerWeek(emptySet()))
        assertEquals(0, WorkSchedule.workingDaysPerWeek(DayOfWeek.entries.toSet()))
    }

    @Test
    fun `planned work days skip the rest days`() {
        val week = WorkRange.ofWeek(monday, DayOfWeek.MONDAY)

        assertEquals(5, WorkSchedule.plannedWorkDays(week, weekend))
        assertEquals(2, WorkSchedule.plannedRestDays(week, weekend))
    }

    @Test
    fun `a custom pattern is honoured`() {
        // Friday and Saturday off instead of the weekend.
        val restDays = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
        val week = WorkRange.ofWeek(monday, DayOfWeek.MONDAY)

        assertEquals(5, WorkSchedule.plannedWorkDays(week, restDays))
        assertTrue(
            week.dates().filter { WorkSchedule.isRestDay(it, restDays) }
                .all { it.dayOfWeek == DayOfWeek.FRIDAY || it.dayOfWeek == DayOfWeek.SATURDAY },
        )
    }

    @Test
    fun `a forty hour week is eight hours per working day`() {
        val week = WorkRange.ofWeek(monday, DayOfWeek.MONDAY)

        val target = WorkSchedule.targetMinutes(week, weeklyTargetMinutes = 40 * 60, restDays = weekend)

        assertEquals(40 * 60L, target)
    }

    @Test
    fun `the target scales with the length of the range`() {
        val week = WorkRange.ofWeek(monday, DayOfWeek.MONDAY)
        val month = WorkRange.ofMonth(YearMonth.of(2026, 9))

        val weekTarget = WorkSchedule.targetMinutes(week, 40 * 60, weekend)
        val monthTarget = WorkSchedule.targetMinutes(month, 40 * 60, weekend)

        // September 2026 has 22 weekdays and five full weeks' worth of working days.
        assertEquals(22 * 8 * 60L, monthTarget)
        assertTrue(monthTarget > weekTarget)
    }

    @Test
    fun `no target and no working days both produce zero`() {
        val week = WorkRange.ofWeek(monday, DayOfWeek.MONDAY)

        assertEquals(0L, WorkSchedule.targetMinutes(week, 0L, weekend))
        assertEquals(0L, WorkSchedule.targetMinutes(week, 40 * 60, DayOfWeek.entries.toSet()))
    }

    @Test
    fun `progress is clamped and safe when there is no target`() {
        assertEquals(0f, WorkSchedule.progress(120, 0), 0.0001f)
        assertEquals(0.5f, WorkSchedule.progress(240, 480), 0.0001f)
        assertEquals(1f, WorkSchedule.progress(600, 480), 0.0001f)
        assertEquals(1f, WorkSchedule.progress(480, 480), 0.0001f)
    }

    @Test
    fun `rest day detection uses the weekday`() {
        assertFalse(WorkSchedule.isRestDay(monday, weekend))
        assertTrue(WorkSchedule.isRestDay(LocalDate.of(2026, 9, 19), weekend))
        assertTrue(WorkSchedule.isRestDay(LocalDate.of(2026, 9, 20), weekend))
    }

    @Test
    fun `midnight is detected from the clock times`() {
        assertTrue(WorkSchedule.crossesMidnight(java.time.LocalTime.of(23, 0), java.time.LocalTime.of(2, 0)))
        assertFalse(WorkSchedule.crossesMidnight(java.time.LocalTime.of(9, 0), java.time.LocalTime.of(17, 0)))
    }
}
