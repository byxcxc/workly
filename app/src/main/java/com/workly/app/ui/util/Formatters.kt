package com.workly.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.workly.app.R
import com.workly.app.data.prefs.DurationStyle
import com.workly.app.domain.Money
import com.workly.app.ui.theme.LocalDurationStyle
import java.math.BigDecimal
import java.math.RoundingMode
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

/**
 * A duration in the format the user picked: `7h 32m` or `7.53h`.
 *
 * The choice is read from the theme, so every screen follows it without having
 * to thread a parameter through.
 */
@Composable
fun formatDuration(minutes: Long): String {
    val safe = minutes.coerceAtLeast(0L)
    return when (LocalDurationStyle.current) {
        DurationStyle.HOURS_MINUTES -> stringResource(R.string.duration_hm, safe / 60, safe % 60)
        DurationStyle.DECIMAL_HOURS ->
            stringResource(R.string.duration_decimal_hours, decimalHours(safe))
    }
}

/**
 * Minutes as decimal hours without the unit: 390 -> `6.5`, 480 -> `8`.
 * Trailing zeros are dropped so whole hours stay short.
 */
fun decimalHours(minutes: Long): String =
    BigDecimal(minutes.coerceAtLeast(0L))
        .divide(BigDecimal(60), 2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

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
