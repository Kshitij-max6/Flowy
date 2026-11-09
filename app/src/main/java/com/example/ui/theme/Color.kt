package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================================================
// Flowly · Apple-inspired palette
// Near-monochrome surfaces, one tint. Values follow the HIG system colours.
// Legacy names (EmeraldPrimary, ChampagneGold…) are kept as aliases so that
// every existing screen picks up the new palette without API changes.
// ==========================================================================

// ---- System tints ---------------------------------------------------------
val SystemBlue = Color(0xFF0A84FF)
val SystemBlueDeep = Color(0xFF0071E3)
val SystemGreen = Color(0xFF30D158)
val SystemRed = Color(0xFFFF453A)
val SystemOrange = Color(0xFFFF9F0A)
val SystemYellow = Color(0xFFFFD60A)
val SystemIndigo = Color(0xFF5E5CE6)
val SystemPurple = Color(0xFFBF5AF2)
val SystemTeal = Color(0xFF40C8E0)
val SystemMint = Color(0xFF63E6E2)
val SystemPink = Color(0xFFFF375F)

/** Text/icon colour used on top of a tinted (filled) control. */
val OnTint = Color.White

// ---- Legacy accent aliases (now Apple blue) -------------------------------
val ChampagneGold = SystemBlue
val ChampagneGoldLight = Color(0xFF64B5FF)
val ChampagneGoldDeep = SystemBlueDeep
val ChampagneGoldSoft = Color(0xFF0B2A4A)
val ChampagneGoldSubtle = Color(0xFF0A1B2E)
val ChampagneGoldBorder = Color(0xFF123A66)

val RoyalGold = SystemBlue
val PrestigeGold = SystemBlue
val ObsidianBlack = Color(0xFF000000)
val DarkCanvas = Color(0xFF000000)

val EmeraldPrimary = SystemBlue
val EmeraldDark = SystemBlueDeep
val EmeraldDeep = Color(0xFF0059B3)
val EmeraldLight = ChampagneGoldLight
val EmeraldSoft = ChampagneGoldSoft
val EmeraldSubtle = ChampagneGoldSubtle

// ---- Semantic finance colours --------------------------------------------
val IncomeGreen = SystemGreen
val ExpenseRed = SystemRed
val TransferCyan = SystemIndigo
val WarningAmber = SystemOrange
val InfoBlue = SystemBlue
val PurpleAccent = SystemPurple
val OrangeAccent = SystemOrange

// ---- Dark surfaces (system set) ------------------------------------------
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF1C1C1E)
val DarkSurfaceVariant = Color(0xFF2C2C2E)
val DarkSurfaceCard = Color(0xFF1C1C1E)
val DarkSurfaceElevated = Color(0xFF2C2C2E)
val DarkBorder = Color(0xFF38383A)
val DarkBorderSubtle = Color(0xFF2C2C2E)
val DarkTextPrimary = Color(0xFFF5F5F7)
val DarkTextSecondary = Color(0xFF8E8E93)
val DarkTextTertiary = Color(0xFF636366)

// ---- Light surfaces (grouped set) ----------------------------------------
val LightBackground = Color(0xFFF2F2F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEBEBF0)
val LightSurfaceCard = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFD1D1D6)
val LightTextPrimary = Color(0xFF1D1D1F)
val LightTextSecondary = Color(0xFF6C6C70)
val LightTextTertiary = Color(0xFFAEAEB2)

// ---- Spending ring ---------------------------------------------------------
val RingSafe = SystemGreen
val RingModerate = SystemOrange
val RingOverBudget = SystemRed
val RingTrackDark = Color(0xFF2C2C2E)

// ---- Flat fills (no glow, no shimmer) --------------------------------------
val GoldShimmerBrush = Brush.horizontalGradient(listOf(SystemBlue, SystemBlue))
val ObsidianCardGradient = Brush.verticalGradient(listOf(DarkSurface, DarkSurface))
val HeroGlowGradient = Brush.radialGradient(listOf(Color.Transparent, Color.Transparent))
