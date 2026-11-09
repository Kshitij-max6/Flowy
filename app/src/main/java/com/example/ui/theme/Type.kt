package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.R

// =============================================================================
// FLOWLY 2.0 TYPOGRAPHY — MANROPE + INTER
//
// Both families are bundled as variable fonts in res/font (SIL Open Font
// License 1.1 — licence texts ship in assets/licenses). Nothing is downloaded
// at runtime, so typography is identical offline and on a cold start.
//
//   Manrope — screen/section headings and every prominent financial figure.
//   Inter   — navigation, buttons, transaction text, forms and supporting text.
// =============================================================================

private val familyWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold
)

@OptIn(ExperimentalTextApi::class)
val ManropeFamily = FontFamily(
    familyWeights.map { weight ->
        Font(
            resId = R.font.manrope_variable,
            weight = weight,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(weight, FontStyle.Normal)
        )
    }
)

@OptIn(ExperimentalTextApi::class)
val InterFamily = FontFamily(
    familyWeights.map { weight ->
        Font(
            resId = R.font.inter_variable,
            weight = weight,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(weight, FontStyle.Normal)
        )
    }
)

// ---- Legacy family names (screens not yet redesigned) ------------------------
// All former families (Plus Jakarta Sans, Playfair Display, Space Grotesk) now
// resolve to the approved pair.
val PlusJakartaSansFamily: FontFamily = InterFamily
val LuxurySerifFamily: FontFamily = ManropeFamily
val SpaceGroteskFamily: FontFamily = ManropeFamily
val HeroDisplayFontFamily: FontFamily = ManropeFamily

@Suppress("UNUSED_PARAMETER")
fun getFontFamilyForHero(option: HeroFontOption): FontFamily = ManropeFamily

/** Tabular numerals keep financial figures, chart labels and columns aligned. */
const val TabularFiguresFeature = "tnum"

// =============================================================================
// MATERIAL TYPE SCALE (blueprint §5)
//   Screen title 30 · Ring figure 34–38 · Section heading 20 · Card amount 22–26
//   Transaction title 15 · Body 14 · Supporting 12–13 · Navigation label 11
// =============================================================================
val Typography = Typography(
    // Budget-ring figure
    displayLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.6).sp,
        fontFeatureSettings = TabularFiguresFeature
    ),
    // Screen title
    displayMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = TabularFiguresFeature
    ),
    // Prominent card amount
    displaySmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = TabularFiguresFeature
    ),
    headlineLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    ),
    // Section heading
    headlineMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    // Transaction title
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    // Standard body
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // Supporting text
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    // Buttons
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    // Navigation label
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.1.sp
    )
)

// =============================================================================
// FINANCIAL FIGURE STYLES
// =============================================================================

@Suppress("UNUSED_PARAMETER")
@Composable
fun heroBalanceStyle(
    fontSize: TextUnit = 38.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    letterSpacing: TextUnit = (-0.6).sp,
    fontOption: HeroFontOption = HeroFontOption.LUXURY_SERIF
): TextStyle = staticHeroBalanceStyle(fontSize, fontWeight, letterSpacing, fontOption)

@Suppress("UNUSED_PARAMETER")
fun staticHeroBalanceStyle(
    fontSize: TextUnit = 38.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    letterSpacing: TextUnit = (-0.6).sp,
    fontOption: HeroFontOption = HeroFontOption.LUXURY_SERIF
): TextStyle = TextStyle(
    fontFamily = ManropeFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    letterSpacing = letterSpacing,
    fontFeatureSettings = TabularFiguresFeature
)

/** Prominent / aligned financial amounts: Manrope with tabular numerals. */
fun financialAmountStyle(
    fontSize: TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    letterSpacing: TextUnit = 0.sp
): TextStyle = TextStyle(
    fontFamily = ManropeFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    letterSpacing = letterSpacing,
    fontFeatureSettings = TabularFiguresFeature
)

/**
 * Turns any text style into a financial-figure style: Manrope + tabular
 * numerals. Everything else (labels, names, captions) stays on Inter.
 */
fun TextStyle.asAmount(): TextStyle = copy(
    fontFamily = ManropeFamily,
    fontFeatureSettings = TabularFiguresFeature
)
