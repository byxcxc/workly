package com.workly.app.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure time arithmetic for work sessions. Everything here is side-effect free and
 * unit tested, including the "23:00 -> 02:00" cross-midnight case.
 */
object WorkTime {

    const val MINUTES_PER_DAY = 24 * 60

    /**
     * Builds the end instant for a manual record from the date the work started
     * plus the two clock times the user typed.
     *
     * If the end time is earlier than the start time the work is assumed to have
     * finished the following day, which is what makes `23:00 -> 02:00` a valid
     * three hour session. Identical times are returned unchanged so that the
     * validator can report them instead of silently creating a 24 hour record.
     */
    fun resolveEndInstant(
        startDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        zone: ZoneId,
    ): Instant {
        // Identical times are not a 24 hour session: they are a mistake the
        // validator reports. Anything earlier than the start rolls over to the
        // next day, which is what makes 23:00 -> 02:00 a three hour session.
        val endDate = when {
            endTime == startTime -> startDate
            endTime > startTime -> startDate
            else -> startDate.plusDays(1)
        }
        return endDate.atTime(endTime).atZone(zone).toInstant()
    }

    /** Elapsed minutes between two instants. Negative when [end] is before [start]. */
    fun grossMinutes(start: Instant, end: Instant): Long =
        Duration.between(start, end).toMinutes()

    /** Elapsed minutes minus the break. Can be negative for invalid input. */
    fun netMinutes(grossMinutes: Long, breakMinutes: Int): Long =
        grossMinutes - breakMinutes.toLong()

    /** True when an end clock time belongs to the day after the start clock time. */
    fun endsNextDay(startTime: LocalTime, endTime: LocalTime): Boolean = endTime < startTime

    /** Formats a duration as `7h 32m`; hours are never padded. */
    fun formatDuration(minutes: Long): String {
        val safe = minutes.coerceAtLeast(0L)
        return "${safe / 60}h ${(safe % 60).toString().padStart(2, '0')}m"
    }
}
