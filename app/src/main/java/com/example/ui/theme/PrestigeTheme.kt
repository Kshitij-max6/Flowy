package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Visual Prestige Themes for Flowly
 */
enum class PrestigeTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val primaryColorHex: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val darkBackground: Color,
    val darkSurface: Color,
    val darkSurfaceVariant: Color,
    val darkCard: Color,
    val darkBorder: Color,
    val darkBorderSubtle: Color,
    val lightBackground: Color,
    val lightSurface: Color,
    val lightSurfaceVariant: Color,
    val lightCard: Color,
    val lightBorder: Color,
    val accentGlow: Color
) {
    OBSIDIAN_GOLD(
        id = "OBSIDIAN_GOLD",
        title = "Blue",
        subtitle = "The system default",
        tag = "DEFAULT",
        primaryColorHex = "#0A84FF",
        primaryColor = Color(0xFF0A84FF),
        secondaryColor = Color(0xFF5E5CE6),
        darkBackground = Color(0xFF000000),
        darkSurface = Color(0xFF1C1C1E),
        darkSurfaceVariant = Color(0xFF2C2C2E),
        darkCard = Color(0xFF1C1C1E),
        darkBorder = Color(0xFF38383A),
        darkBorderSubtle = Color(0xFF2C2C2E),
        lightBackground = Color(0xFFF2F2F7),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFEBEBF0),
        lightCard = Color(0xFFFFFFFF),
        lightBorder = Color(0xFFD1D1D6),
        accentGlow = Color(0xFF0A84FF)
    ),
    EMERALD_VAULT(
        id = "EMERALD_VAULT",
        title = "Green",
        subtitle = "Fresh and calm",
        tag = "GREEN",
        primaryColorHex = "#30D158",
        primaryColor = Color(0xFF30D158),
        secondaryColor = Color(0xFF0A84FF),
        darkBackground = Color(0xFF000000),
        darkSurface = Color(0xFF1C1C1E),
        darkSurfaceVariant = Color(0xFF2C2C2E),
        darkCard = Color(0xFF1C1C1E),
        darkBorder = Color(0xFF38383A),
        darkBorderSubtle = Color(0xFF2C2C2E),
        lightBackground = Color(0xFFF2F2F7),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFEBEBF0),
        lightCard = Color(0xFFFFFFFF),
        lightBorder = Color(0xFFD1D1D6),
        accentGlow = Color(0xFF30D158)
    ),
    ROYAL_NAVY(
        id = "ROYAL_NAVY",
        title = "Indigo",
        subtitle = "Quiet and focused",
        tag = "INDIGO",
        primaryColorHex = "#5E5CE6",
        primaryColor = Color(0xFF5E5CE6),
        secondaryColor = Color(0xFF0A84FF),
        darkBackground = Color(0xFF000000),
        darkSurface = Color(0xFF1C1C1E),
        darkSurfaceVariant = Color(0xFF2C2C2E),
        darkCard = Color(0xFF1C1C1E),
        darkBorder = Color(0xFF38383A),
        darkBorderSubtle = Color(0xFF2C2C2E),
        lightBackground = Color(0xFFF2F2F7),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFEBEBF0),
        lightCard = Color(0xFFFFFFFF),
        lightBorder = Color(0xFFD1D1D6),
        accentGlow = Color(0xFF5E5CE6)
    ),
    MATTE_CARBON(
        id = "MATTE_CARBON",
        title = "Orange",
        subtitle = "Warm and energetic",
        tag = "ORANGE",
        primaryColorHex = "#FF9F0A",
        primaryColor = Color(0xFFFF9F0A),
        secondaryColor = Color(0xFFFF453A),
        darkBackground = Color(0xFF000000),
        darkSurface = Color(0xFF1C1C1E),
        darkSurfaceVariant = Color(0xFF2C2C2E),
        darkCard = Color(0xFF1C1C1E),
        darkBorder = Color(0xFF38383A),
        darkBorderSubtle = Color(0xFF2C2C2E),
        lightBackground = Color(0xFFF2F2F7),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFEBEBF0),
        lightCard = Color(0xFFFFFFFF),
        lightBorder = Color(0xFFD1D1D6),
        accentGlow = Color(0xFFFF9F0A)
    );

    val heroGradient: Brush
        get() = Brush.linearGradient(listOf(primaryColor, primaryColor))

    val cardAccentGradient: Brush
        get() = Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))

    companion object {
        fun fromString(name: String?): PrestigeTheme {
            return entries.firstOrNull { it.id.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                ?: OBSIDIAN_GOLD
        }
    }
}

/**
 * Hero Balance Typography Options
 */
enum class HeroFontOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val sampleText: String
) {
    LUXURY_SERIF(
        id = "LUXURY_SERIF",
        title = "Inter",
        subtitle = "Clean, precise figures. The closest match to San Francisco",
        sampleText = "₹1,24,500.00"
    ),
    GEOMETRIC_SANS(
        id = "GEOMETRIC_SANS",
        title = "System Sans",
        subtitle = "Your device's default sans-serif",
        sampleText = "₹1,24,500.00"
    ),
    SPACE_GROTESK(
        id = "SPACE_GROTESK",
        title = "System Serif",
        subtitle = "An editorial serif for large balances",
        sampleText = "₹1,24,500.00"
    );

    companion object {
        fun fromString(name: String?): HeroFontOption {
            return entries.firstOrNull { it.id.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                ?: LUXURY_SERIF
        }
    }
}

/**
 * Build M3 ColorScheme dynamically based on chosen Prestige Theme
 */
fun getPrestigeDarkColorScheme(theme: PrestigeTheme): ColorScheme = darkColorScheme(
    primary = theme.primaryColor,
    onPrimary = Color.White,
    primaryContainer = theme.primaryColor.copy(alpha = 0.18f),
    onPrimaryContainer = theme.primaryColor,
    secondary = theme.secondaryColor,
    onSecondary = Color.White,
    secondaryContainer = theme.darkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = WarningAmber,
    onTertiary = Color.White,
    background = theme.darkBackground,
    onBackground = DarkTextPrimary,
    surface = theme.darkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = theme.darkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = theme.darkBorder,
    outlineVariant = theme.darkBorderSubtle,
    error = ExpenseRed,
    onError = Color.White,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = theme.darkBackground,
    surfaceContainerLow = theme.darkSurface,
    surfaceContainer = theme.darkSurface,
    surfaceContainerHigh = theme.darkSurfaceVariant,
    surfaceContainerHighest = Color(0xFF3A3A3C),
    surfaceBright = theme.darkSurfaceVariant,
    surfaceDim = theme.darkBackground,
    scrim = Color.Black
)

fun getPrestigeLightColorScheme(theme: PrestigeTheme): ColorScheme = lightColorScheme(
    primary = theme.primaryColor,
    onPrimary = Color.White,
    primaryContainer = theme.primaryColor.copy(alpha = 0.15f),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = theme.secondaryColor,
    onSecondary = Color.White,
    secondaryContainer = theme.lightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = WarningAmber,
    background = theme.lightBackground,
    onBackground = LightTextPrimary,
    surface = theme.lightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = theme.lightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = theme.lightBorder,
    outlineVariant = Color(0xFFE5E5EA),
    error = ExpenseRed,
    onError = Color.White,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = theme.lightSurfaceVariant,
    surfaceContainerHighest = Color(0xFFE5E5EA),
    surfaceBright = Color.White,
    surfaceDim = theme.lightBackground,
    scrim = Color.Black
)

val LocalPrestigeTheme = compositionLocalOf { PrestigeTheme.OBSIDIAN_GOLD }
val LocalHeroFontOption = compositionLocalOf { HeroFontOption.LUXURY_SERIF }
