package com.workly.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workly.app.data.prefs.AppSettings
import com.workly.app.data.prefs.BackgroundMode
import com.workly.app.ui.background.WorklyBackground
import com.workly.app.ui.background.rememberBackgroundRender
import com.workly.app.ui.WorklyApp
import com.workly.app.ui.theme.WorklyTheme
import com.workly.app.ui.theme.shouldUseDarkTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val settings by AppGraph.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = shouldUseDarkTheme(settings.themeMode)

            // Keep the system bar icons readable when the user forces a theme
            // that differs from the system setting.
            val view = LocalView.current
            LaunchedEffect(darkTheme) {
                val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            // The picture is decoded (and its accent extracted) once per URI.
            val background = rememberBackgroundRender(
                uri = settings.backgroundImageUri
                    ?.takeIf { settings.backgroundMode == BackgroundMode.IMAGE },
                resolver = AppGraph.appContext.contentResolver,
            )

            WorklyTheme(
                themeMode = settings.themeMode,
                palette = settings.themePalette,
                durationStyle = settings.durationStyle,
                backgroundMode = settings.backgroundMode,
                adaptiveColorArgb = background.accentArgb.takeIf { settings.adaptToImage },
            ) {
                WorklyBackground(settings = settings, render = background) {
                    WorklyApp()
                }
            }
        }
    }
}
