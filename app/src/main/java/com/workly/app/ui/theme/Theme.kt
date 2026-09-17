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
import androidx.compose.ui.graphics.lerp
import com.workly.app.data.prefs.BackgroundMode
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

/**
 * The theme's opaque background colour.
 *
 * `colorScheme.background` becomes transparent once the user picks their own
 * backdrop, so anything that needs a solid base (the background layer itself, the
 * scrim over a picture) reads it from here instead.
 */
val LocalBaseBackgroundColor = staticCompositionLocalOf { LightBackground }

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
    backgroundMode: BackgroundMode = BackgroundMode.DEFAULT,
    adaptiveColorArgb: Long? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = shouldUseDarkTheme(themeMode)
    // Remembered so a screen change — or the once-a-second timer tick — never
    // hands Material a brand new colour scheme, which would invalidate every
    // composable below it.
    val brand = remember(palette) { paletteBrand(palette) }
    val baseScheme = remember(brand, darkTheme) {
        if (darkTheme) darkScheme(brand) else lightScheme(brand)
    }
    val adaptive = remember(adaptiveColorArgb) { adaptiveColorArgb?.let(::Color) }

    val colorScheme = remember(baseScheme, adaptive, backgroundMode) {
        var scheme = if (adaptive != null) {
            withAdaptivePrimary(baseScheme, adaptive, darkTheme)
        } else {
            baseScheme
        }
        if (backgroundMode != BackgroundMode.DEFAULT) scheme = withGlassSurfaces(scheme, darkTheme)
        scheme
    }

    val accents = remember(brand, darkTheme, adaptive) {
        accentsFor(brand, darkTheme).let { if (adaptive != null) it.copy(chart = adaptive) else it }
    }

    CompositionLocalProvider(
        LocalWorklyAccents provides accents,
        LocalDurationStyle provides durationStyle,
        LocalBaseBackgroundColor provides if (darkTheme) DarkBackground else LightBackground,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WorklyTypography,
            shapes = WorklyShapes,
            content = content,
        )
    }
}

/**
 * Derives a readable brand colour from an image's dominant colour, so the UI
 * "picks up" the wallpaper. The exact amount of lightening/darkening keeps text
 * on primary legible in both modes.
 */
private fun withAdaptivePrimary(scheme: ColorScheme, accent: Color, dark: Boolean): ColorScheme {
    val primary = if (dark) lerp(accent, Color.White, 0.32f) else lerp(accent, Color.Black, 0.10f)
    val onPrimary = if (dark) Color(0xFF14161C) else Color.White
    val container = if (dark) lerp(accent, Color.Black, 0.55f) else lerp(accent, Color.White, 0.82f)
    val onContainer = if (dark) lerp(accent, Color.White, 0.75f) else lerp(accent, Color.Black, 0.38f)
    return scheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
    )
}

/**
 * A frosted-glass look: when a custom background is showing, every surface becomes
 * slightly translucent so the (already blurred) backdrop shines through, while the
 * background itself is transparent so the backdrop is not painted over.
 */
private fun withGlassSurfaces(scheme: ColorScheme, dark: Boolean): ColorScheme {
    val surfaceAlpha = if (dark) 0.72f else 0.84f
    val containerAlpha = if (dark) 0.66f else 0.82f
    return scheme.copy(
        background = Color.Transparent,
        surface = scheme.surface.copy(alpha = surfaceAlpha),
        surfaceContainerLowest = scheme.surfaceContainerLowest.copy(alpha = if (dark) 0.50f else 0.72f),
        surfaceContainerLow = scheme.surfaceContainerLow.copy(alpha = containerAlpha),
        surfaceContainer = scheme.surfaceContainer.copy(alpha = containerAlpha),
        surfaceContainerHigh = scheme.surfaceContainerHigh.copy(alpha = if (dark) 0.72f else 0.88f),
        surfaceContainerHighest = scheme.surfaceContainerHighest.copy(alpha = if (dark) 0.80f else 0.92f),
    )
}
