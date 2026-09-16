package com.workly.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.workly.app.R
import com.workly.app.domain.Money
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formatting helpers.
 *
 * Every timestamp is converted to the *device's current time zone* at the moment
 * it is displayed, while the stored value stays an absolute [Instant]. Changing
 * the phone's time zone therefore never moves a record's stored time.
 */

@Composable
fun rememberAppLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.locales[0] ?: Locale.getDefault() }
}

fun deviceZone(): ZoneId = ZoneId.systemDefault()

@Composable
fun formatMoney(minorUnits: Long, currencyCode: String): String {
    val locale = rememberAppLocale()
    return remember(minorUnits, currencyCode, locale) {
        Money.format(minorUnits, currencyCode, locale)
    }
}

@Composable
fun formatMoneyCompact(minorUnits: Long, currencyCode: String): String {
    val locale = rememberAppLocale()
    return remember(minorUnits, currencyCode, locale) {
        Money.formatCompact(minorUnits, currencyCode, locale)
    }
}

@Composable
fun formatRate(minorUnits: Long, currencyCode: String): String {
    val money = formatMoney(minorUnits, currencyCode)
    return stringResource(R.string.rate_per_hour, money)
}

/** `7h 32m` */
@Composable
fun formatDuration(minutes: Long): String {
    val safe = minutes.coerceAtLeast(0L)
    return stringResource(R.string.duration_hm, safe / 60, safe % 60)
}

/** `7h 32m 12s`, used by the live timer. */
@Composable
fun formatDurationWithSeconds(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    return stringResource(
        R.string.duration_hms,
        safe / 3600,
        (safe % 3600) / 60,
        safe % 60,
    )
}

/** Hours with one decimal, for chart axis labels: `7.5h`. */
fun formatHoursShort(minutes: Long): String {
    val hours = minutes / 60.0
    return if (hours >= 10) String.format(Locale.ROOT, "%.0fh", hours) else String.format(Locale.ROOT, "%.1fh", hours)
}

@Composable
fun formatTime(instant: Instant): String {
    val locale = rememberAppLocale()
    return remember(instant, locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .format(instant.atZone(deviceZone()))
    }
}

@Composable
fun formatTimeRange(start: Instant, end: Instant?): String {
    val startText = formatTime(start)
    if (end == null) return startText
    return "$startText – ${formatTime(end)}"
}

/** `September 16` */
@Composable
fun formatDateMedium(date: LocalDate): String {
    val locale = rememberAppLocale()
    return remember(date, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
    }
}

/** `Tuesday, September 16, 2026` */
@Composable
fun formatDateFull(date: LocalDate): String {
    val locale = rememberAppLocale()
    return remember(date, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date)
    }
}

/** `Tue, Sep 16` */
@Composable
fun formatDateShort(date: LocalDate): String {
    val locale = rememberAppLocale()
    return remember(date, locale) {
        DateTimeFormatter.ofPattern("EEE, MMM d", locale).format(date)
    }
}

/** `Sep` */
@Composable
fun formatMonthShort(date: LocalDate): String {
    val locale = rememberAppLocale()
    return remember(date, locale) {
        DateTimeFormatter.ofPattern("LLL", locale).format(date)
    }
}

/** `Tue` */
@Composable
fun formatWeekdayShort(date: LocalDate): String {
    val locale = rememberAppLocale()
    return remember(date, locale) {
        DateTimeFormatter.ofPattern("EEE", locale).format(date)
    }
}

/** `September 2026` */
@Composable
fun formatMonthYear(month: YearMonth): String {
    val locale = rememberAppLocale()
    return remember(month, locale) {
        DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month)
    }
}

/** Single letter used on the calendar header row. */
@Composable
fun formatWeekdayNarrow(locale: Locale): List<String> {
    return remember(locale) {
        val formatter = DateTimeFormatter.ofPattern("EEEEE", locale)
        (1..7).map { formatter.format(LocalDate.of(2024, 1, it)) }
    }
}
