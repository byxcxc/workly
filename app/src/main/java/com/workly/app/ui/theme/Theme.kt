package com.workly.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.workly.app.data.prefs.DurationStyle
import com.workly.app.data.prefs.ThemeMode
import com.workly.app.data.prefs.ThemePalette

/**
 * Colours that carry meaning in Workly but have no equivalent Material 3 role.
 */
@Immutable
data class WorklyAccents(
    /** Money amounts. Constant across palettes: green always means income. */
    val income: Color,
    /** The "working now" state. */
    val working: Color,
    val workingContainer: Color,
    val onWorkingContainer: Color,
    /** Chart fills, which follow the chosen palette. */
    val chart: Color,
    val chartSecondary: Color,
)

val LocalWorklyAccents = staticCompositionLocalOf {
    WorklyAccents(
        income = LightAccentIncome,
        working = LightAccentWorking,
        workingContainer = LightAccentWorkingContainer,
        onWorkingContainer = LightAccentOnWorkingContainer,
        chart = LightAccentChart,
        chartSecondary = LightAccentChartSecondary,
    )
}

/** How durations are written, e.g. `6h 30m` or `6.5h`. */
val LocalDurationStyle = staticCompositionLocalOf { DurationStyle.HOURS_MINUTES }

/** Shortcut: `WorklyTheme.accents.income`. */
object WorklyTheme {
    val accents: WorklyAccents
        @Composable get() = LocalWorklyAccents.current
}

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
)

/**
 * The colour scheme of a palette.
 *
 * Everything except the brand colour is shared, so changing the palette is a
 * small, predictable change instead of a whole new design.
 */
private fun lightScheme(brand: PaletteBrand): ColorScheme = LightColors.copy(
    primary = brand.lightPrimary,
    primaryContainer = brand.lightPrimaryContainer,
    onPrimaryContainer = OnLightBrandContainer,
)

private fun darkScheme(brand: PaletteBrand): ColorScheme = DarkColors.copy(
    primary = brand.darkPrimary,
    primaryContainer = brand.darkPrimaryContainer,
    onPrimaryContainer = OnDarkBrandContainer,
)

private fun accentsFor(brand: PaletteBrand, darkTheme: Boolean): WorklyAccents = if (darkTheme) {
    WorklyAccents(
        income = DarkAccentIncome,
        working = DarkAccentWorking,
        workingContainer = DarkAccentWorkingContainer,
        onWorkingContainer = DarkAccentOnWorkingContainer,
        chart = brand.darkPrimary,
        chartSecondary = DarkAccentChartSecondary,
    )
} else {
    WorklyAccents(
        income = LightAccentIncome,
        working = LightAccentWorking,
        workingContainer = LightAccentWorkingContainer,
        onWorkingContainer = LightAccentOnWorkingContainer,
        chart = brand.lightPrimary,
        chartSecondary = LightAccentChartSecondary,
    )
}

/**
 * Does the user want a dark UI right now?
 *
 * Split out from [WorklyTheme] so the status bar icons can be updated from the
 * same source of truth.
 */
@Composable
fun shouldUseDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun WorklyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: ThemePalette = ThemePalette.INDIGO,
    durationStyle: DurationStyle = DurationStyle.HOURS_MINUTES,
    content: @Composable () -> Unit,
) {
    val darkTheme = shouldUseDarkTheme(themeMode)
    // Remembered so a screen change — or the once-a-second timer tick — never
    // hands Material a brand new colour scheme, which would invalidate every
    // composable below it.
    val brand = remember(palette) { paletteBrand(palette) }
    val colorScheme = remember(brand, darkTheme) {
        if (darkTheme) darkScheme(brand) else lightScheme(brand)
    }
    val accents = remember(brand, darkTheme) { accentsFor(brand, darkTheme) }

    CompositionLocalProvider(
        LocalWorklyAccents provides accents,
        LocalDurationStyle provides durationStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WorklyTypography,
            shapes = WorklyShapes,
            content = content,
        )
    }
}
