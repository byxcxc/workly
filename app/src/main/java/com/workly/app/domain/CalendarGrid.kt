package com.workly.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Builds the month grid shown by the calendar view.
 *
 * The result is a list of weeks, each with exactly seven entries. Entries are
 * `null` for the padding days before the first and after the last day of the
 * month, so the grid always has a stable shape.
 */
object CalendarGrid {

    const val DAYS_IN_WEEK = 7

    fun weeks(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate?>> {
        val firstOfMonth = month.atDay(1)
        val leadingBlanks = ((firstOfMonth.dayOfWeek.value - firstDayOfWeek.value) + DAYS_IN_WEEK) % DAYS_IN_WEEK
        val daysInMonth = month.lengthOfMonth()
        val cells = ArrayList<LocalDate?>(leadingBlanks + daysInMonth)
        repeat(leadingBlanks) { cells += null }
        for (day in 1..daysInMonth) cells += month.atDay(day)
        while (cells.size % DAYS_IN_WEEK != 0) cells += null
        return cells.chunked(DAYS_IN_WEEK)
    }

    /** Weekday order matching [weeks], starting from [firstDayOfWeek]. */
    fun weekdayOrder(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
        (0 until DAYS_IN_WEEK).map { firstDayOfWeek.plus(it.toLong()) }
}
