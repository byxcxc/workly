package com.workly.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek

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

    private object Keys {
        val DEFAULT_HOURLY_RATE_MINOR = longPreferencesKey("default_hourly_rate_minor")
        val CURRENCY = stringPreferencesKey("currency")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val DURATION_STYLE = stringPreferencesKey("duration_style")
        val REST_DAYS = stringSetPreferencesKey("rest_days")
        val WEEKLY_TARGET_MINUTES = longPreferencesKey("weekly_target_minutes")
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val DEFAULT_WORK_TYPE_ID = longPreferencesKey("default_work_type_id")
        val LANGUAGE = stringPreferencesKey("language")
        val DEFAULTS_SEEDED = booleanPreferencesKey("defaults_seeded")
    }
}
