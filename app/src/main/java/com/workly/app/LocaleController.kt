package com.workly.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.workly.app.data.prefs.AppLanguage

/**
 * Applies the user's language choice to the whole app.
 *
 * Workly uses `AppCompatDelegate.setApplicationLocales`, which is the API Google
 * recommends for per-app language: on Android 13 and newer it delegates to the
 * platform (so the choice also shows up in the system "App language" screen and
 * survives a reinstall), and on older releases AppCompat stores and re-applies it
 * itself.
 */
object LocaleController {

    /** Applies [language], recreating the running activity if needed. */
    fun apply(language: AppLanguage) {
        val desired = language.tag
            ?.let { LocaleListCompat.forLanguageTags(it) }
            ?: LocaleListCompat.getEmptyLocaleList()
        if (AppCompatDelegate.getApplicationLocales() != desired) {
            AppCompatDelegate.setApplicationLocales(desired)
        }
    }

    /** What the platform currently has stored for this app. */
    fun current(): AppLanguage =
        AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    /**
     * Keeps the stored preference and the platform in step.
     *
     * The platform wins, so a language chosen from the system settings screen is
     * picked up too. Returns the language the app should now use; the caller is
     * responsible for storing it when it differs from [stored].
     */
    fun reconcile(stored: AppLanguage): AppLanguage {
        val platform = current()
        if (platform != stored) return platform
        apply(stored)
        return stored
    }
}
