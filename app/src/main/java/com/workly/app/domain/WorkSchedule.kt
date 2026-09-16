package com.workly.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * The user's working pattern: which weekdays are days off and how many hours a
 * week they aim to work.
 *
 * Everything here is pure so the arithmetic behind the progress bars is unit
 * tested instead of being spread across the UI.
 */
object WorkSchedule {

    const val DAYS_PER_WEEK = 7

    /** Working days in a normal week, i.e. seven minus the rest days. */
    fun workingDaysPerWeek(restDays: Set<DayOfWeek>): Int =
        (DAYS_PER_WEEK - restDays.size).coerceAtLeast(0)

    /** Days in [range] that are not rest days. */
    fun plannedWorkDays(range: WorkRange, restDays: Set<DayOfWeek>): Int =
        range.dates().count { it.dayOfWeek !in restDays }

    /** Days in [range] the user does not work. */
    fun plannedRestDays(range: WorkRange, restDays: Set<DayOfWeek>): Int =
        range.dayCount.toInt() - plannedWorkDays(range, restDays)

    /**
     * How many minutes the user aims to work in [range].
     *
     * The weekly target is spread evenly over the week's working days, so a month
     * or a custom range gets a proportional target. Returns 0 when there is no
     * target or every day is a rest day.
     */
    fun targetMinutes(
        range: WorkRange,
        weeklyTargetMinutes: Long,
        restDays: Set<DayOfWeek>,
    ): Long {
        val perWeek = workingDaysPerWeek(restDays)
        if (perWeek == 0 || weeklyTargetMinutes <= 0L) return 0L
        val perDay = weeklyTargetMinutes / perWeek
        return perDay * plannedWorkDays(range, restDays)
    }

    /** Progress towards [targetMinutes], clamped to `0f..1f`. */
    fun progress(workedMinutes: Long, targetMinutes: Long): Float {
        if (targetMinutes <= 0L) return 0f
        return (workedMinutes.toFloat() / targetMinutes.toFloat()).coerceIn(0f, 1f)
    }

    /** True when [date] is a day the user does not work. */
    fun isRestDay(date: LocalDate, restDays: Set<DayOfWeek>): Boolean = date.dayOfWeek in restDays

    /** True when [time] falls on the following day for [startTime]. */
    fun crossesMidnight(startTime: LocalTime, endTime: LocalTime): Boolean = endTime < startTime
}
