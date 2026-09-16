package com.workly.app.domain

import com.workly.app.TestData
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.incomeMinor
import com.workly.app.data.local.entity.workedMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class StatsCalculatorTest {

    private val zone = TestData.zone
    private val monday = LocalDate.of(2026, 9, 14)

    private fun eightHourDay(date: LocalDate, rate: Long = 1_000L): WorkSessionEntity =
        TestData.session(
            date = date,
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            breakMinutes = 60,
            hourlyRateMinor = rate,
        )

    @Test
    fun `worked minutes and income of a single record`() {
        val session = eightHourDay(monday)

        assertEquals(420L, session.workedMinutes)
        assertEquals(7_000L, session.incomeMinor)
    }

    @Test
    fun `sessions that are still running are excluded`() {
        val running = TestData.session(
            date = monday,
            start = LocalTime.of(9, 0),
            end = null,
            isActive = true,
        )

        assertEquals(0L, running.workedMinutes)
        assertTrue(StatsCalculator.completed(listOf(running)).isEmpty())
    }

    @Test
    fun `daily summary adds up multiple records on the same day`() {
        val sessions = listOf(
            eightHourDay(monday),
            TestData.session(
                date = monday,
                start = LocalTime.of(19, 0),
                end = LocalTime.of(21, 0),
                hourlyRateMinor = 2_000L,
            ),
        )

        val stats = StatsCalculator.summarize(sessions, WorkRange.ofDay(monday), zone)

        assertEquals(540L, stats.totalMinutes)
        assertEquals(7_000L + 4_000L, stats.totalIncomeMinor)
        assertEquals(1, stats.workDays)
        assertEquals(2, stats.sessionCount)
    }

    @Test
    fun `weekly summary respects the first day of the week`() {
        val sessions = listOf(
            // Sunday 13 September: belongs to the previous week when weeks start on Monday
            eightHourDay(LocalDate.of(2026, 9, 13)),
            eightHourDay(monday),
            eightHourDay(LocalDate.of(2026, 9, 19)),
        )

        val mondayFirst = StatsCalculator.summarize(
            sessions,
            WorkRange.ofWeek(monday, DayOfWeek.MONDAY),
            zone,
        )
        val sundayFirst = StatsCalculator.summarize(
            sessions,
            WorkRange.ofWeek(monday, DayOfWeek.SUNDAY),
            zone,
        )

        assertEquals(2, mondayFirst.sessionCount)
        assertEquals(3, sundayFirst.sessionCount)
    }

    @Test
    fun `weekly totals and averages`() {
        val sessions = listOf(
            eightHourDay(monday), // 7h
            eightHourDay(monday.plusDays(2)), // 7h
            eightHourDay(monday.plusDays(4), rate = 2_000L), // 7h
        )

        val stats = StatsCalculator.summarize(
            sessions,
            WorkRange.ofWeek(monday, DayOfWeek.MONDAY),
            zone,
        )

        assertEquals(3, stats.workDays)
        assertEquals(1260L, stats.totalMinutes)
        assertEquals(7_000L + 7_000L + 14_000L, stats.totalIncomeMinor)
        assertEquals(420L, stats.averageDailyMinutes)
        assertEquals((7_000L + 7_000L + 14_000L) / 3, stats.averageDailyIncomeMinor)
        // 28,000 minor over 21 hours = 1,333.33 per hour
        assertEquals(1_333L, stats.averageHourlyRateMinor)
    }

    @Test
    fun `monthly summary only counts the requested month`() {
        val sessions = listOf(
            eightHourDay(LocalDate.of(2026, 8, 31)),
            eightHourDay(LocalDate.of(2026, 9, 1)),
            eightHourDay(LocalDate.of(2026, 9, 30)),
            eightHourDay(LocalDate.of(2026, 10, 1)),
        )

        val stats = StatsCalculator.summarize(
            sessions,
            WorkRange.ofMonth(YearMonth.of(2026, 9)),
            zone,
        )

        assertEquals(2, stats.sessionCount)
        assertEquals(840L, stats.totalMinutes)
    }

    @Test
    fun `yearly summary counts every month of the year`() {
        val sessions = (1..12).map { month ->
            eightHourDay(LocalDate.of(2026, month, 15))
        }

        val stats = StatsCalculator.summarize(sessions, WorkRange.ofYear(2026), zone)

        assertEquals(12, stats.sessionCount)
        assertEquals(12, stats.workDays)
        assertEquals(12 * 420L, stats.totalMinutes)
    }

    @Test
    fun `a session belongs to the day it started`() {
        val nightShift = TestData.session(
            date = monday,
            start = LocalTime.of(23, 0),
            end = LocalTime.of(2, 0),
            endOnNextDay = true,
        )

        val stats = StatsCalculator.summarize(listOf(nightShift), WorkRange.ofDay(monday), zone)

        assertEquals(1, stats.sessionCount)
        assertEquals(180L, stats.totalMinutes)
    }

    @Test
    fun `daily buckets include days without work`() {
        val sessions = listOf(eightHourDay(monday))

        val buckets = StatsCalculator.dailyBuckets(
            sessions,
            WorkRange.ofWeek(monday, DayOfWeek.MONDAY),
            zone,
        )

        assertEquals(7, buckets.size)
        assertEquals(420L, buckets.first().minutes)
        assertEquals(0L, buckets[1].minutes)
    }

    @Test
    fun `monthly buckets cover every month of the range`() {
        val sessions = listOf(eightHourDay(LocalDate.of(2026, 3, 10)))

        val buckets = StatsCalculator.monthlyBuckets(sessions, WorkRange.ofYear(2026), zone)

        assertEquals(12, buckets.size)
        assertEquals(3, buckets[2].labelDate.monthValue)
        assertEquals(420L, buckets[2].minutes)
        assertEquals(0L, buckets[0].minutes)
    }

    @Test
    fun `empty range produces empty statistics`() {
        val stats = StatsCalculator.summarize(emptyList(), WorkRange.ofWeek(monday, DayOfWeek.MONDAY), zone)

        assertEquals(0L, stats.totalMinutes)
        assertEquals(0L, stats.totalIncomeMinor)
        assertEquals(0, stats.workDays)
        assertEquals(0L, stats.averageDailyMinutes)
        assertEquals(0L, stats.averageHourlyRateMinor)
        assertTrue(stats.isEmpty)
    }
}
