package com.workly.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The user's working pattern: weekly rest days, a weekly hours target, and
 * per-day overrides set from the calendar.
 *
 * Everything here is pure so the arithmetic behind the progress bars is unit
 * tested instead of being spread across the UI.
 */
object WorkSchedule {

    const val DAYS_PER_WEEK = 7

    /** Working days in a normal week, i.e. seven minus the rest days. */
    fun workingDaysPerWeek(restDays: Set<DayOfWeek>): Int =
        (DAYS_PER_WEEK - restDays.size).coerceAtLeast(0)

    /**
     * Whether [date] is a day off, honouring any per-day override first and the
     * weekly pattern second.
     */
    fun isRestDay(
        date: LocalDate,
        restDays: Set<DayOfWeek>,
        overrides: Map<LocalDate, DayOverride> = emptyMap(),
    ): Boolean = overrides[date]?.isRest ?: (date.dayOfWeek in restDays)

    /** Days in [range] that are not rest days. */
    fun plannedWorkDays(
        range: WorkRange,
        restDays: Set<DayOfWeek>,
        overrides: Map<LocalDate, DayOverride> = emptyMap(),
    ): Int = range.dates().count { !isRestDay(it, restDays, overrides) }

    /** Days in [range] the user does not work. */
    fun plannedRestDays(
        range: WorkRange,
        restDays: Set<DayOfWeek>,
        overrides: Map<LocalDate, DayOverride> = emptyMap(),
    ): Int = range.dayCount.toInt() - plannedWorkDays(range, restDays, overrides)

    /**
     * The target (minutes) for a single day: the per-day override wins, otherwise
     * the weekly target is spread evenly over the week's working days.
     */
    fun dayTargetMinutes(
        date: LocalDate,
        weeklyTargetMinutes: Long,
        restDays: Set<DayOfWeek>,
        overrides: Map<LocalDate, DayOverride> = emptyMap(),
    ): Long {
        overrides[date]?.targetMinutes?.let { return it.coerceAtLeast(0L) }
        val perWeek = workingDaysPerWeek(restDays)
        if (perWeek == 0) return 0L
        return weeklyTargetMinutes.coerceAtLeast(0L) / perWeek
    }

    /**
     * How many minutes the user aims to work in [range].
     *
     * Every planned working day contributes its own target, so a month or a custom
     * range gets a proportional total. Explicit per-day targets are honoured even
     * when no weekly target is set.
     */
    fun targetMinutes(
        range: WorkRange,
        weeklyTargetMinutes: Long,
        restDays: Set<DayOfWeek>,
        overrides: Map<LocalDate, DayOverride> = emptyMap(),
    ): Long = range.dates()
        .filter { !isRestDay(it, restDays, overrides) }
        .sumOf { dayTargetMinutes(it, weeklyTargetMinutes, restDays, overrides) }

    /** Progress towards [targetMinutes], clamped to `0f..1f`. */
    fun progress(workedMinutes: Long, targetMinutes: Long): Float {
        if (targetMinutes <= 0L) return 0f
        return (workedMinutes.toFloat() / targetMinutes.toFloat()).coerceIn(0f, 1f)
    }
}
