package com.workly.app.domain

import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.incomeMinor
import com.workly.app.data.local.entity.workedMinutes
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Aggregated numbers for a period of time.
 *
 * A session belongs to the day it *started*, so a shift that runs from 23:00 to
 * 02:00 is counted entirely on the day it began. This keeps daily totals stable
 * and easy to reason about.
 */
data class PeriodStats(
    val range: WorkRange,
    val totalMinutes: Long = 0L,
    val totalIncomeMinor: Long = 0L,
    val workDays: Int = 0,
    val sessionCount: Int = 0,
) {
    val averageDailyMinutes: Long
        get() = if (workDays == 0) 0L else totalMinutes / workDays

    val averageDailyIncomeMinor: Long
        get() = if (workDays == 0) 0L else totalIncomeMinor / workDays

    /** Total income divided by total hours; 0 when nothing was worked. */
    val averageHourlyRateMinor: Long
        get() = if (totalMinutes == 0L) 0L else Money.incomeMinor(totalIncomeMinor, 60) * 60 / totalMinutes

    val isEmpty: Boolean get() = sessionCount == 0
}

/** One bar/point on a chart. */
data class TimeBucket(
    val labelDate: LocalDate,
    val minutes: Long,
    val incomeMinor: Long,
)

object StatsCalculator {

    /** Only finished sessions count towards statistics. */
    fun completed(sessions: List<WorkSessionEntity>): List<WorkSessionEntity> =
        sessions.filter { it.endTime != null }

    fun summarize(
        sessions: List<WorkSessionEntity>,
        range: WorkRange,
        zone: ZoneId,
    ): PeriodStats {
        val relevant = sessionsInRange(sessions, range, zone)
        if (relevant.isEmpty()) return PeriodStats(range = range)

        var minutes = 0L
        var income = 0L
        val days = HashSet<LocalDate>()
        relevant.forEach { session ->
            minutes += session.workedMinutes
            income += session.incomeMinor
            days += session.startTime.atZone(zone).toLocalDate()
        }
        return PeriodStats(
            range = range,
            totalMinutes = minutes,
            totalIncomeMinor = income,
            workDays = days.size,
            sessionCount = relevant.size,
        )
    }

    /** Sessions whose start date falls inside [range]. */
    fun sessionsInRange(
        sessions: List<WorkSessionEntity>,
        range: WorkRange,
        zone: ZoneId,
    ): List<WorkSessionEntity> = completed(sessions).filter { session ->
        range.contains(session.startTime.atZone(zone).toLocalDate())
    }

    /** One bucket per day of [range], including days without any work. */
    fun dailyBuckets(
        sessions: List<WorkSessionEntity>,
        range: WorkRange,
        zone: ZoneId,
    ): List<TimeBucket> {
        val byDay = sessionsInRange(sessions, range, zone)
            .groupBy { it.startTime.atZone(zone).toLocalDate() }
        return range.dates().map { date ->
            val daySessions = byDay[date].orEmpty()
            TimeBucket(
                labelDate = date,
                minutes = daySessions.sumOf { it.workedMinutes },
                incomeMinor = daySessions.sumOf { it.incomeMinor },
            )
        }
    }

    /** One bucket per month of [range]; the label date is the first of the month. */
    fun monthlyBuckets(
        sessions: List<WorkSessionEntity>,
        range: WorkRange,
        zone: ZoneId,
    ): List<TimeBucket> {
        val byMonth = sessionsInRange(sessions, range, zone)
            .groupBy { YearMonth.from(it.startTime.atZone(zone).toLocalDate()) }
        val months = generateSequence(YearMonth.from(range.start)) { it.plusMonths(1) }
            .takeWhile { it.atDay(1).isBefore(range.endExclusive) }
            .toList()
        return months.map { month ->
            val monthSessions = byMonth[month].orEmpty()
            TimeBucket(
                labelDate = month.atDay(1),
                minutes = monthSessions.sumOf { it.workedMinutes },
                incomeMinor = monthSessions.sumOf { it.incomeMinor },
            )
        }
    }
}
