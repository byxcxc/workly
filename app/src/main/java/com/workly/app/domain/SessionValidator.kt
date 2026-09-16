package com.workly.app.domain

import java.time.Instant

/**
 * Validation rules for a work session. Kept pure so every rule is covered by
 * plain JVM unit tests.
 */
object SessionValidator {

    sealed interface Result {
        /** @param workedMinutes minutes actually worked after the break. */
        data class Valid(val workedMinutes: Long) : Result

        data class Invalid(val error: WorklyError) : Result
    }

    /**
     * Validates a session that has an end time.
     *
     * Cross-midnight sessions are normal and valid: the caller is expected to have
     * already resolved the end instant to the following day (see
     * [WorkTime.resolveEndInstant]). An end instant that is not after the start is
     * therefore an error, not something to silently fix here.
     */
    fun validate(
        start: Instant,
        end: Instant,
        breakMinutes: Int,
        hourlyRateMinor: Long,
    ): Result {
        if (hourlyRateMinor < 0) return Result.Invalid(WorklyError.NEGATIVE_RATE)
        if (breakMinutes < 0) return Result.Invalid(WorklyError.BREAK_LONGER_THAN_WORK)
        if (end == start) return Result.Invalid(WorklyError.END_TIME_EQUALS_START)
        if (end.isBefore(start)) return Result.Invalid(WorklyError.END_BEFORE_START)

        val gross = WorkTime.grossMinutes(start, end)
        val net = WorkTime.netMinutes(gross, breakMinutes)
        if (net < 0) return Result.Invalid(WorklyError.BREAK_LONGER_THAN_WORK)
        return Result.Valid(net)
    }

    /** Validates a work type name. */
    fun validateWorkTypeName(name: String): WorklyError? =
        if (name.isBlank()) WorklyError.EMPTY_WORK_TYPE_NAME else null
}
