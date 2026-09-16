package com.workly.app.data.prefs

import java.time.DayOfWeek
import java.util.Currency
import java.util.Locale

/** How the app decides between the light and the dark colour scheme. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: SYSTEM
    }
}

/** User preferences, stored in a DataStore file (not in the Room database). */
data class AppSettings(
    val defaultHourlyRateMinor: Long = 0L,
    val currency: String = defaultCurrencyCode(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val defaultWorkTypeId: Long? = null,
    /** True once the four starter work types have been created. */
    val defaultsSeeded: Boolean = false,
) {
    val hasDefaultHourlyRate: Boolean get() = defaultHourlyRateMinor > 0L
}

/** Currency of the device locale, falling back to USD. */
fun defaultCurrencyCode(): String =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("USD")

/** Currencies offered in Settings. Deliberately short and easy to scan. */
val SUPPORTED_CURRENCIES: List<String> = listOf(
    "USD", "EUR", "GBP", "JPY", "CNY", "KRW", "TWD", "HKD", "SGD", "AUD", "CAD", "CHF", "INR", "BRL",
)
