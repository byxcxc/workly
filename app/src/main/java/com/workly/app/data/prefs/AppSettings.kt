package com.workly.app.data.prefs

import androidx.compose.runtime.Immutable
import com.workly.app.domain.DayOverride
import java.time.DayOfWeek
import java.time.LocalDate
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

/**
 * The accent colour of the app.
 *
 * Only the brand colour and the charts change; the semantic colours (income, the
 * running timer) stay the same in every palette so they keep their meaning.
 */
enum class ThemePalette {
    INDIGO,
    TEAL,
    FOREST,
    SUNSET,
    ROSE,
    MONO,
    ;

    companion object {
        fun fromKey(key: String?): ThemePalette =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: INDIGO
    }
}

/** What the app draws behind the UI. */
enum class BackgroundMode {
    /** The theme's own background colour. */
    DEFAULT,

    /** A solid colour the user picked. */
    COLOR,

    /** The user's own picture, optionally blurred. */
    IMAGE,
    ;

    companion object {
        fun fromKey(key: String?): BackgroundMode =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: DEFAULT
    }
}

/** How durations are written: `6h 30m` or `6.5h`. */
enum class DurationStyle {
    HOURS_MINUTES,
    DECIMAL_HOURS,
    ;

    companion object {
        fun fromKey(key: String?): DurationStyle =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: HOURS_MINUTES
    }
}

/**
 * Language the user picked for the app.
 *
 * [tag] is the BCP-47 tag handed to the platform; `null` means "follow the
 * system", which is the default. The language names themselves are intentionally
 * not translated: a picker should always read in the language it offers.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    JAPANESE("ja"),
    CHINESE("zh-Hans"),
    ;

    companion object {
        /**
         * Maps anything the platform reports (`en`, `ja-JP`, `zh-Hans-CN`, ...)
         * onto one of the languages the app ships.
         */
        fun fromTag(tag: String?): AppLanguage {
            val language = tag?.substringBefore('-')?.lowercase() ?: return SYSTEM
            return when (language) {
                "en" -> ENGLISH
                "ja" -> JAPANESE
                "zh" -> CHINESE
                else -> SYSTEM
            }
        }
    }
}

/** User preferences, stored in a DataStore file (not in the Room database). */
@Immutable
data class AppSettings(
    val defaultHourlyRateMinor: Long = 0L,
    val currency: String = defaultCurrencyCode(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themePalette: ThemePalette = ThemePalette.INDIGO,
    val durationStyle: DurationStyle = DurationStyle.HOURS_MINUTES,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    /**
     * Days the user does not work. They are kept out of the "planned work days"
     * arithmetic and are marked in the calendar.
     */
    val restDays: Set<DayOfWeek> = DEFAULT_REST_DAYS,
    /** Total hours the user aims to work in a week; 0 means "no target". */
    val weeklyTargetMinutes: Long = 0L,
    /** Per-day overrides set by long-pressing a date in the calendar. */
    val dayOverrides: Map<LocalDate, DayOverride> = emptyMap(),

    // ---- appearance / background ----
    val backgroundMode: BackgroundMode = BackgroundMode.DEFAULT,
    /** ARGB of the solid background colour, only used in [BackgroundMode.COLOR]. */
    val backgroundColorArgb: Long = DEFAULT_BACKGROUND_COLOR_ARGB,
    /** Persisted content URI of the user's picture, only used in [BackgroundMode.IMAGE]. */
    val backgroundImageUri: String? = null,
    /** 0..100, how strongly the background picture is blurred. */
    val backgroundBlurPercent: Int = 25,
    /** When true the theme takes its accent from the picture's dominant colour. */
    val adaptToImage: Boolean = true,
    val defaultWorkTypeId: Long? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
    /** True once the four starter work types have been created. */
    val defaultsSeeded: Boolean = false,
) {
    val hasDefaultHourlyRate: Boolean get() = defaultHourlyRateMinor > 0L

    val hasWeeklyTarget: Boolean get() = weeklyTargetMinutes > 0L
}

/** Saturday and Sunday: a neutral starting point that the user can change. */
val DEFAULT_REST_DAYS: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** A calm blue-grey used as the solid background until the user picks their own. */
const val DEFAULT_BACKGROUND_COLOR_ARGB: Long = 0xFFE9EDF5

/** Currency of the device locale, falling back to USD. */
fun defaultCurrencyCode(): String =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("USD")

/** Currencies offered in Settings. Deliberately short and easy to scan. */
val SUPPORTED_CURRENCIES: List<String> = listOf(
    "USD", "EUR", "GBP", "JPY", "CNY", "KRW", "TWD", "HKD", "SGD", "AUD", "CAD", "CHF", "INR", "BRL",
)
