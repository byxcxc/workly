package com.workly.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * A half open range of calendar days, `[start, endExclusive)`.
 *
 * The range itself is zone independent; call [asInstantRange] to convert it to
 * instants using the device zone at query time.
 */
data class WorkRange(
    val start: LocalDate,
    val endExclusive: LocalDate,
) {
    val endInclusive: LocalDate get() = endExclusive.minusDays(1)

    val dayCount: Long get() = ChronoUnit.DAYS.between(start, endExclusive)

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && date.isBefore(endExclusive)

    fun asInstantRange(zone: ZoneId): Pair<Instant, Instant> =
        start.atStartOfDay(zone).toInstant() to endExclusive.atStartOfDay(zone).toInstant()

    fun dates(): List<LocalDate> =
        (0 until dayCount).map { start.plusDays(it) }

    companion object {
        fun ofDay(date: LocalDate): WorkRange = WorkRange(date, date.plusDays(1))

        fun ofWeek(date: LocalDate, firstDayOfWeek: DayOfWeek): WorkRange {
            val start = date.minusDays(
                ((date.dayOfWeek.value - firstDayOfWeek.value) + 7L) % 7L,
            )
            return WorkRange(start, start.plusDays(7))
        }

        fun ofMonth(month: YearMonth): WorkRange = WorkRange(month.atDay(1), month.atEndOfMonth().plusDays(1))

        fun ofYear(year: Int): WorkRange = WorkRange(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1))

        /** Custom range from two inclusive dates, tolerating a swapped selection. */
        fun custom(from: LocalDate, toInclusive: LocalDate): WorkRange {
            val (start, end) = if (from.isAfter(toInclusive)) toInclusive to from else from to toInclusive
            return WorkRange(start, end.plusDays(1))
        }
    }
}
