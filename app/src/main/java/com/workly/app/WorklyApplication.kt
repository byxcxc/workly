package com.workly.app

import android.app.Application
import com.workly.app.data.prefs.AppLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Workly is a small single-module app, so dependencies are wired by hand in
 * [AppGraph] instead of using a dependency injection framework. This keeps the
 * project easy to follow and to maintain.
 */
class WorklyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        AppGraph.applicationScope.launch { syncLanguage() }
    }

    /**
     * Keeps the stored language and the platform in step.
     *
     * This runs off the main thread: AppCompat (and on Android 13+ the platform
     * itself) has already applied the language before the first activity attaches,
     * so the first frame is correct without blocking startup on a file read. The
     * work here is only to notice a choice made in the system settings screen.
     */
    private suspend fun syncLanguage() {
        runCatching {
            val stored = AppGraph.settingsRepository.settings.first().language
            val effective = LocaleController.reconcile(stored)
            if (effective != stored) {
                AppGraph.settingsRepository.setLanguage(effective)
            }
        }.onFailure {
            // Never let a preferences problem stop the app from starting.
            LocaleController.apply(AppLanguage.SYSTEM)
        }
    }
}
