package com.workly.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.workly.app.data.prefs.ThemeMode

/**
 * Colours that carry meaning in Workly but have no equivalent Material 3 role.
 */
@Immutable
data class WorklyAccents(
    /** Money amounts. */
    val income: Color,
    /** The "working now" state. */
    val working: Color,
    val workingContainer: Color,
    val onWorkingContainer: Color,
    /** Chart fills. */
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

private val LightAccents = WorklyAccents(
    income = LightAccentIncome,
    working = LightAccentWorking,
    workingContainer = LightAccentWorkingContainer,
    onWorkingContainer = LightAccentOnWorkingContainer,
    chart = LightAccentChart,
    chartSecondary = LightAccentChartSecondary,
)

private val DarkAccents = WorklyAccents(
    income = DarkAccentIncome,
    working = DarkAccentWorking,
    workingContainer = DarkAccentWorkingContainer,
    onWorkingContainer = DarkAccentOnWorkingContainer,
    chart = DarkAccentChart,
    chartSecondary = DarkAccentChartSecondary,
)

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
    content: @Composable () -> Unit,
) {
    val darkTheme = shouldUseDarkTheme(themeMode)
    CompositionLocalProvider(
        LocalWorklyAccents provides if (darkTheme) DarkAccents else LightAccents,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = WorklyTypography,
            shapes = WorklyShapes,
            content = content,
        )
    }
}
