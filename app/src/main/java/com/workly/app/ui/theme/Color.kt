package com.workly.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.workly.app.data.prefs.ThemePalette

/*
 * Workly palette.
 *
 * Low saturation, calm and neutral, with a single blue-violet brand colour.
 * Saturated colour is reserved for the three things that carry meaning:
 * income, the running timer and chart bars.
 */

// Light ---------------------------------------------------------------------
val LightPrimary = Color(0xFF4A5A9E)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDEE1FF)
val LightOnPrimaryContainer = Color(0xFF101A4C)
val LightSecondary = Color(0xFF5A6076)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE2E4F0)
val LightOnSecondaryContainer = Color(0xFF1B1F2C)
val LightTertiary = Color(0xFF3C6A5E)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFD3EFE4)
val LightOnTertiaryContainer = Color(0xFF08251C)

val LightBackground = Color(0xFFF8F8FB)
val LightOnBackground = Color(0xFF1B1C20)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1B1C20)
val LightSurfaceVariant = Color(0xFFE7E8F0)
val LightOnSurfaceVariant = Color(0xFF474A54)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF4F4F9)
val LightSurfaceContainer = Color(0xFFEFEFF6)
val LightSurfaceContainerHigh = Color(0xFFE9EAF2)
val LightSurfaceContainerHighest = Color(0xFFE3E4ED)
val LightOutline = Color(0xFF797B85)
val LightOutlineVariant = Color(0xFFC9CAD4)
val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)
val LightInverseSurface = Color(0xFF303036)
val LightInverseOnSurface = Color(0xFFF2F2F7)

// Dark ----------------------------------------------------------------------
val DarkPrimary = Color(0xFFB4C0F5)
val DarkOnPrimary = Color(0xFF1E2A5E)
val DarkPrimaryContainer = Color(0xFF36437A)
val DarkOnPrimaryContainer = Color(0xFFDEE1FF)
val DarkSecondary = Color(0xFFC3C6D6)
val DarkOnSecondary = Color(0xFF2C3040)
val DarkSecondaryContainer = Color(0xFF424656)
val DarkOnSecondaryContainer = Color(0xFFDFE1F0)
val DarkTertiary = Color(0xFFA6D0C2)
val DarkOnTertiary = Color(0xFF0B372C)
val DarkTertiaryContainer = Color(0xFF234E42)
val DarkOnTertiaryContainer = Color(0xFFC2EEDF)

val DarkBackground = Color(0xFF101114)
val DarkOnBackground = Color(0xFFE4E4EA)
val DarkSurface = Color(0xFF16171B)
val DarkOnSurface = Color(0xFFE4E4EA)
val DarkSurfaceVariant = Color(0xFF2A2C33)
val DarkOnSurfaceVariant = Color(0xFFC6C7D0)
val DarkSurfaceContainerLowest = Color(0xFF0C0D10)
val DarkSurfaceContainerLow = Color(0xFF131418)
val DarkSurfaceContainer = Color(0xFF17181D)
val DarkSurfaceContainerHigh = Color(0xFF1D1E24)
val DarkSurfaceContainerHighest = Color(0xFF24252C)
val DarkOutline = Color(0xFF8F919B)
val DarkOutlineVariant = Color(0xFF3A3C44)
val DarkError = Color(0xFFF2B8B5)
val DarkOnError = Color(0xFF601410)
val DarkErrorContainer = Color(0xFF8C1D18)
val DarkOnErrorContainer = Color(0xFFF9DEDC)
val DarkInverseSurface = Color(0xFFE4E4EA)
val DarkInverseOnSurface = Color(0xFF303036)

// Semantic accents ----------------------------------------------------------
val LightAccentIncome = Color(0xFF1F7A5C)
val LightAccentWorking = Color(0xFFB26A00)
val LightAccentWorkingContainer = Color(0xFFFFEBCB)
val LightAccentOnWorkingContainer = Color(0xFF3A2400)
val LightAccentChart = Color(0xFF4A5A9E)
val LightAccentChartSecondary = Color(0xFF7C89C9)

val DarkAccentIncome = Color(0xFF7FD8B0)
val DarkAccentWorking = Color(0xFFFFC46B)
val DarkAccentWorkingContainer = Color(0xFF3E2C05)
val DarkAccentOnWorkingContainer = Color(0xFFFFE3B0)
val DarkAccentChart = Color(0xFF9AA8E8)
val DarkAccentChartSecondary = Color(0xFF5C6AA8)

// Theme palettes ------------------------------------------------------------
//
// Only the brand colour (buttons, selection, charts) changes between palettes.
// Backgrounds, surfaces and the semantic accents stay put, so switching theme
// colour never changes what a colour *means*.

/** The four brand colours of one palette. */
@Immutable
data class PaletteBrand(
    val lightPrimary: Color,
    val lightPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkPrimaryContainer: Color,
)

/** Text on a light brand container. */
val OnLightBrandContainer = Color(0xFF141821)

/** Text on a dark brand container. */
val OnDarkBrandContainer = Color(0xFFE4E6EE)

fun paletteBrand(palette: ThemePalette): PaletteBrand = when (palette) {
    ThemePalette.INDIGO -> PaletteBrand(
        lightPrimary = Color(0xFF4A5A9E),
        lightPrimaryContainer = Color(0xFFDEE1FF),
        darkPrimary = Color(0xFFB4C0F5),
        darkPrimaryContainer = Color(0xFF36437A),
    )

    ThemePalette.TEAL -> PaletteBrand(
        lightPrimary = Color(0xFF2F6F73),
        lightPrimaryContainer = Color(0xFFC9E9EA),
        darkPrimary = Color(0xFF8FD3D6),
        darkPrimaryContainer = Color(0xFF27585B),
    )

    ThemePalette.FOREST -> PaletteBrand(
        lightPrimary = Color(0xFF3B6B45),
        lightPrimaryContainer = Color(0xFFCDEBD1),
        darkPrimary = Color(0xFF9FD3A3),
        darkPrimaryContainer = Color(0xFF2E5436),
    )

    ThemePalette.SUNSET -> PaletteBrand(
        lightPrimary = Color(0xFF8A5A2B),
        lightPrimaryContainer = Color(0xFFF5DFC4),
        darkPrimary = Color(0xFFE8B57F),
        darkPrimaryContainer = Color(0xFF6B4322),
    )

    ThemePalette.ROSE -> PaletteBrand(
        lightPrimary = Color(0xFF7D4A63),
        lightPrimaryContainer = Color(0xFFF3D7E3),
        darkPrimary = Color(0xFFE3AFC5),
        darkPrimaryContainer = Color(0xFF5F3549),
    )

    ThemePalette.MONO -> PaletteBrand(
        lightPrimary = Color(0xFF4A4E57),
        lightPrimaryContainer = Color(0xFFDFE1E6),
        darkPrimary = Color(0xFFC6C9D1),
        darkPrimaryContainer = Color(0xFF3A3E46),
    )
}
