package com.workly.app.domain

/**
 * An override for one specific day, set by long-pressing a date in the calendar.
 *
 * Both fields are optional: `null` means "follow the weekly pattern". A day can
 * be forced to be a rest day, given its own work-hours target, or both.
 */
data class DayOverride(
    /** `true` forces a rest day, `false` forces a working day, `null` follows the week. */
    val isRest: Boolean? = null,
    /** Custom work-hours target (minutes) for this day only. */
    val targetMinutes: Long? = null,
) {
    val isDefault: Boolean get() = isRest == null && targetMinutes == null
}
