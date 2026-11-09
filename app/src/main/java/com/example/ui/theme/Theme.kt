package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = EmeraldLight,
    secondary = TransferCyan,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = WarningAmber,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle,
    error = ExpenseRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldSubtle,
    onPrimaryContainer = EmeraldDeep,
    secondary = TransferCyan,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = WarningAmber,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = ExpenseRed,
    onError = Color.White
)

@Composable
fun FlowlyTheme(
    prestigeTheme: PrestigeTheme = PrestigeTheme.OBSIDIAN_GOLD,
    heroFont: HeroFontOption = HeroFontOption.LUXURY_SERIF,
    darkTheme: Boolean = true, // Default to Sophisticated Dark Theme
    dynamicColor: Boolean = false, // Keep custom curated theme
    content: @Composable () -> Unit
) {
    val targetScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getPrestigeDarkColorScheme(prestigeTheme)
        else -> getPrestigeLightColorScheme(prestigeTheme)
    }

    // Light <-> dark cross-fades over 220 ms; with reduced motion it switches instantly.
    val reduceMotion = rememberReduceMotion()
    val colorScheme = if (reduceMotion) targetScheme else targetScheme.animated()

    SideEffect { if (AccentColor.color != prestigeTheme.primaryColor) AccentColor.color = prestigeTheme.primaryColor }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalPrestigeTheme provides prestigeTheme,
        LocalHeroFontOption provides heroFont,
        LocalReduceMotion provides reduceMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(20.dp),
                extraLarge = RoundedCornerShape(28.dp)
            ),
            content = {
                CompositionLocalProvider(LocalIndication provides ApplePressIndication) {
                    content()
                }
            }
        )
    }
}

/** Animates the surface and text colours that change between light and dark. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = tween<Color>(durationMillis = 220, easing = AppleEasing)
    @Composable
    fun anim(color: Color): Color = animateColorAsState(color, spec, label = "themeColor").value
    return copy(
        background = anim(background),
        onBackground = anim(onBackground),
        surface = anim(surface),
        onSurface = anim(onSurface),
        surfaceVariant = anim(surfaceVariant),
        onSurfaceVariant = anim(onSurfaceVariant),
        outline = anim(outline),
        outlineVariant = anim(outlineVariant),
        primaryContainer = anim(primaryContainer),
        onPrimaryContainer = anim(onPrimaryContainer)
    )
}
