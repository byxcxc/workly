package com.workly.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * All money in Workly is stored as a [Long] number of **minor units** (cents,
 * pence, sen, ...) together with an ISO-4217 currency code.
 *
 * Integer minor units make calculations exact: no binary floating point value is
 * ever used for money, so totals never drift by a cent.
 */
object Money {

    private val formatCache = ConcurrentHashMap<String, NumberFormat>()
    private val fractionCache = ConcurrentHashMap<String, Int>()

    /** Number of decimal places the currency uses (0 for JPY, 2 for USD, 3 for KWD). */
    fun fractionDigits(currencyCode: String): Int = fractionCache.getOrPut(currencyCode) {
        runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }
            .getOrDefault(DEFAULT_FRACTION_DIGITS)
            .coerceAtLeast(0)
    }

    /**
     * Currency symbol for the given locale.
     *
     * The currencies Workly offers all have a widely recognised symbol, and using
     * it directly keeps amounts looking the same on every device (the platform
     * symbol is "JP¥" on an English phone, for example). Anything else falls back
     * to the platform symbol and finally to the raw code.
     */
    fun symbolOf(currencyCode: String, locale: Locale = Locale.getDefault()): String =
        PREFERRED_SYMBOLS[currencyCode.uppercase()]
            ?: runCatching { Currency.getInstance(currencyCode).getSymbol(locale) }
                .getOrDefault(currencyCode)

    /**
     * Income for [minutes] worked at [hourlyRateMinor] per hour, rounded half-up
     * to the smallest unit of the currency.
     *
     * The multiplication is done in exact integer arithmetic, so the result is
     * identical on every device. `hourlyRateMinor * minutes` fits comfortably in a
     * Long for any realistic input (10^9 * 10^6 << 2^63).
     */
    fun incomeMinor(hourlyRateMinor: Long, minutes: Long): Long {
        val product = hourlyRateMinor * minutes
        val quotient = product / MINUTES_PER_HOUR
        val remainder = product % MINUTES_PER_HOUR
        return if (remainder * 2 >= MINUTES_PER_HOUR) quotient + 1 else quotient
    }

    /**
     * Formats minor units for display, e.g. `¥12,480` (JPY) or `$1,234.50` (USD).
     * The number of decimals always follows the currency's own rules.
     */
    fun format(
        minorUnits: Long,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val digits = fractionDigits(currencyCode)
        val value = BigDecimal.valueOf(minorUnits, digits)
        val numberFormat = numberFormatFor(currencyCode, locale, digits)
        val rendered = synchronized(numberFormat) { numberFormat.format(value.abs()) }
        val sign = if (value.signum() < 0) "-" else ""
        return sign + symbolOf(currencyCode, locale) + rendered
    }

    /**
     * Parses user input such as `1,600`, `1600.5` or `¥1600` into minor units.
     * Returns `null` when the text is not a valid amount.
     */
    fun parseToMinor(text: String, currencyCode: String): Long? {
        val cleaned = text.trim()
            .replace(Regex("[^0-9.,\\-]"), "")
            .replace(",", "")
        if (cleaned.isEmpty() || cleaned == "-") return null
        return runCatching {
            BigDecimal(cleaned)
                .movePointRight(fractionDigits(currencyCode))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrNull()
    }

    /** Short representation used by charts, e.g. `¥12.5k` or `$1.3M`. */
    fun formatCompact(minorUnits: Long, currencyCode: String, locale: Locale): String {
        val digits = fractionDigits(currencyCode)
        val value = BigDecimal.valueOf(minorUnits, digits)
        val symbol = symbolOf(currencyCode, locale)
        val thousand = BigDecimal(1_000)
        val million = BigDecimal(1_000_000)
        return when {
            value.abs() >= million ->
                symbol + value.divide(million, 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "M"

            value.abs() >= thousand ->
                symbol + value.divide(thousand, 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "k"

            else -> symbol + value.setScale(0, RoundingMode.HALF_UP).toPlainString()
        }
    }

    /** Symbols used for display, keyed by ISO-4217 code. */
    private val PREFERRED_SYMBOLS = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "CNY" to "¥",
        "KRW" to "₩",
        "TWD" to "NT$",
        "HKD" to "HK$",
        "SGD" to "S$",
        "AUD" to "A$",
        "CAD" to "C$",
        "CHF" to "CHF",
        "INR" to "₹",
        "BRL" to "R$",
    )

    /**
     * Renders minor units as a plain editable string, e.g. `1600` for JPY or
     * `16.5` for USD. Used to pre-fill rate input fields.
     */
    fun toEditableString(minorUnits: Long, currencyCode: String): String =
        BigDecimal.valueOf(minorUnits, fractionDigits(currencyCode))
            .stripTrailingZeros()
            .toPlainString()

    private fun numberFormatFor(
        currencyCode: String,
        locale: Locale,
        digits: Int,
    ): NumberFormat = formatCache.getOrPut("$currencyCode|${locale.toLanguageTag()}|$digits") {
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
    }

    private const val MINUTES_PER_HOUR = 60L
    private const val DEFAULT_FRACTION_DIGITS = 2
}
