package com.workly.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.workly.app.domain.DayOverride
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "workly_settings",
)

/**
 * Reads and writes [AppSettings].
 *
 * Settings live in a DataStore file instead of a Room table: they are a handful of
 * primitive values, and DataStore gives us an observable [Flow] plus atomic writes
 * without any schema or DAO boilerplate.
 */
class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { throwable ->
            // A corrupted preferences file must not crash the app: fall back to
            // defaults and keep the previous values on disk intact.
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            AppSettings(
                defaultHourlyRateMinor = preferences[Keys.DEFAULT_HOURLY_RATE_MINOR] ?: 0L,
                currency = preferences[Keys.CURRENCY] ?: defaultCurrencyCode(),
                themeMode = ThemeMode.fromKey(preferences[Keys.THEME_MODE]),
                themePalette = ThemePalette.fromKey(preferences[Keys.THEME_PALETTE]),
                durationStyle = DurationStyle.fromKey(preferences[Keys.DURATION_STYLE]),
                restDays = preferences[Keys.REST_DAYS]
                    ?.mapNotNull { name -> runCatching { DayOfWeek.valueOf(name) }.getOrNull() }
                    ?.toSet()
                    ?: DEFAULT_REST_DAYS,
                weeklyTargetMinutes = preferences[Keys.WEEKLY_TARGET_MINUTES] ?: 0L,
                dayOverrides = decodeOverrides(preferences[Keys.DAY_OVERRIDES]),
                backgroundMode = BackgroundMode.fromKey(preferences[Keys.BACKGROUND_MODE]),
                backgroundColorArgb = preferences[Keys.BACKGROUND_COLOR] ?: DEFAULT_BACKGROUND_COLOR_ARGB,
                backgroundImageUri = preferences[Keys.BACKGROUND_IMAGE_URI],
                backgroundBlurPercent = (preferences[Keys.BACKGROUND_BLUR] ?: 25).coerceIn(0, 100),
                adaptToImage = preferences[Keys.ADAPT_TO_IMAGE] ?: true,
                firstDayOfWeek = runCatching {
                    DayOfWeek.valueOf(preferences[Keys.FIRST_DAY_OF_WEEK] ?: DayOfWeek.MONDAY.name)
                }.getOrDefault(DayOfWeek.MONDAY),
                defaultWorkTypeId = preferences[Keys.DEFAULT_WORK_TYPE_ID],
                language = AppLanguage.fromTag(preferences[Keys.LANGUAGE]),
                defaultsSeeded = preferences[Keys.DEFAULTS_SEEDED] ?: false,
            )
        }

    suspend fun setDefaultHourlyRateMinor(value: Long) = edit { it[Keys.DEFAULT_HOURLY_RATE_MINOR] = value.coerceAtLeast(0L) }

    suspend fun setCurrency(code: String) = edit { it[Keys.CURRENCY] = code }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setThemePalette(palette: ThemePalette) =
        edit { it[Keys.THEME_PALETTE] = palette.name }

    suspend fun setDurationStyle(style: DurationStyle) =
        edit { it[Keys.DURATION_STYLE] = style.name }

    /** Days off. An empty set means the user works every day. */
    suspend fun setRestDays(days: Set<DayOfWeek>) =
        edit { it[Keys.REST_DAYS] = days.map(DayOfWeek::name).toSet() }

    suspend fun setWeeklyTargetMinutes(minutes: Long) =
        edit { it[Keys.WEEKLY_TARGET_MINUTES] = minutes.coerceAtLeast(0L) }

    /** Stores an override for one day, removing it when it becomes the default. */
    suspend fun setBackgroundMode(mode: BackgroundMode) =
        edit { it[Keys.BACKGROUND_MODE] = mode.name }

    suspend fun setBackgroundColor(argb: Long) =
        edit { it[Keys.BACKGROUND_COLOR] = argb }

    suspend fun setBackgroundImageUri(uri: String?) = edit { preferences ->
        if (uri == null) preferences.remove(Keys.BACKGROUND_IMAGE_URI) else preferences[Keys.BACKGROUND_IMAGE_URI] = uri
    }

    suspend fun setBackgroundBlurPercent(percent: Int) =
        edit { it[Keys.BACKGROUND_BLUR] = percent.coerceIn(0, 100) }

    suspend fun setAdaptToImage(adapt: Boolean) =
        edit { it[Keys.ADAPT_TO_IMAGE] = adapt }

    suspend fun setDayOverride(date: LocalDate, override: DayOverride) {
        edit { preferences ->
            val map = decodeOverrides(preferences[Keys.DAY_OVERRIDES]).toMutableMap()
            if (override.isDefault) map.remove(date) else map[date] = override
            preferences[Keys.DAY_OVERRIDES] = encodeOverrides(map)
        }
    }

    suspend fun setFirstDayOfWeek(day: DayOfWeek) = edit { it[Keys.FIRST_DAY_OF_WEEK] = day.name }

    suspend fun setDefaultWorkTypeId(id: Long?) = edit { preferences ->
        if (id == null) preferences.remove(Keys.DEFAULT_WORK_TYPE_ID) else preferences[Keys.DEFAULT_WORK_TYPE_ID] = id
    }

    suspend fun setLanguage(language: AppLanguage) =
        edit { it[Keys.LANGUAGE] = language.tag.orEmpty() }

    suspend fun setDefaultsSeeded(seeded: Boolean) = edit { it[Keys.DEFAULTS_SEEDED] = seeded }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit { block(it) }
    }

    /** `epochDay|rest|targetMinutes` per entry, where rest is `1`, `0` or blank. */
    private fun encodeOverrides(map: Map<LocalDate, DayOverride>): Set<String> =
        map.entries.map { (date, override) ->
            val rest = override.isRest?.let { if (it) "1" else "0" } ?: ""
            "${date.toEpochDay()}|$rest|${override.targetMinutes ?: ""}"
        }.toSet()

    private fun decodeOverrides(set: Set<String>?): Map<LocalDate, DayOverride> {
        if (set.isNullOrEmpty()) return emptyMap()
        val result = HashMap<LocalDate, DayOverride>()
        set.forEach { entry ->
            val parts = entry.split('|')
            if (parts.size != 3) return@forEach
            val date = runCatching { LocalDate.ofEpochDay(parts[0].toLong()) }.getOrNull() ?: return@forEach
            val isRest = when (parts[1]) {
                "1" -> true
                "0" -> false
                else -> null
            }
            val target = parts[2].toLongOrNull()?.coerceAtLeast(0L)
            if (isRest == null && target == null) return@forEach
            result[date] = DayOverride(isRest = isRest, targetMinutes = target)
        }
        return result
    }

    private object Keys {
        val DEFAULT_HOURLY_RATE_MINOR = longPreferencesKey("default_hourly_rate_minor")
        val CURRENCY = stringPreferencesKey("currency")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val DURATION_STYLE = stringPreferencesKey("duration_style")
        val REST_DAYS = stringSetPreferencesKey("rest_days")
        val WEEKLY_TARGET_MINUTES = longPreferencesKey("weekly_target_minutes")
        val DAY_OVERRIDES = stringSetPreferencesKey("day_overrides")
        val BACKGROUND_MODE = stringPreferencesKey("background_mode")
        val BACKGROUND_COLOR = longPreferencesKey("background_color")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val BACKGROUND_BLUR = intPreferencesKey("background_blur")
        val ADAPT_TO_IMAGE = booleanPreferencesKey("adapt_to_image")
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val DEFAULT_WORK_TYPE_ID = longPreferencesKey("default_work_type_id")
        val LANGUAGE = stringPreferencesKey("language")
        val DEFAULTS_SEEDED = booleanPreferencesKey("defaults_seeded")
    }
}
