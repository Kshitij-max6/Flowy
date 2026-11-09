package com.example.feature.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.AppleIconButton
import com.example.ui.theme.AppleEasing
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.pressable

// =============================================================================
// APPEARANCE
//
// One preference, three choices. The saved value is UserSettings.themeMode
// ("LIGHT" | "DARK" | "SYSTEM"); MainActivity already resolves it into the
// app-wide theme, so this screen only reads and writes that single value.
// =============================================================================

private enum class ThemeChoice(
    val mode: String,
    val title: String,
    val caption: String,
    val tag: String
) {
    LIGHT("LIGHT", "Light", "Always light", "appearance_theme_light"),
    DARK("DARK", "Dark", "Always dark", "appearance_theme_dark"),
    SYSTEM("SYSTEM", "System", "Matches device", "appearance_theme_system")
}

/** Colours of one miniature Flowly interface. Built from the real design tokens. */
private class MiniPalette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val subtle: Color,
    val border: Color,
    val accent: Color
)

private val LightMini = MiniPalette(
    background = LightBackground,
    surface = LightSurface,
    text = LightTextPrimary,
    subtle = LightTextSecondary.copy(alpha = 0.45f),
    border = LightBorder,
    accent = EmeraldPrimary
)

private val DarkMini = MiniPalette(
    background = DarkBackground,
    surface = DarkSurface,
    text = DarkTextPrimary,
    subtle = DarkTextSecondary.copy(alpha = 0.55f),
    border = DarkBorder,
    accent = EmeraldPrimary
)

@Composable
fun AppearanceScreen(
    themeMode: String,
    onThemeModeSelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedChoice = ThemeChoice.entries.firstOrNull { it.mode == themeMode } ?: ThemeChoice.SYSTEM

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("appearance_screen")
    ) {
        // ---- Top bar ----------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack,
                modifier = Modifier.testTag("appearance_back_button")
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            Text(
                text = "Make Flowly feel like yours.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = "THEME",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeChoice.entries.forEach { choice ->
                    ThemeCard(
                        choice = choice,
                        selected = choice == selectedChoice,
                        onClick = { if (choice != selectedChoice) onThemeModeSelected(choice.mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Plain-language explanation of the active choice.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                val deviceIsDark = isSystemInDarkTheme()
                Crossfade(
                    targetState = selectedChoice,
                    animationSpec = if (LocalReduceMotion.current) snap() else tween(200, easing = AppleEasing),
                    label = "appearanceNote"
                ) { choice ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                        Text(
                            text = when (choice) {
                                ThemeChoice.LIGHT -> "Light"
                                ThemeChoice.DARK -> "Dark"
                                ThemeChoice.SYSTEM -> "System"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (choice) {
                                ThemeChoice.LIGHT -> "Flowly stays light, whatever your device is set to."
                                ThemeChoice.DARK -> "Flowly stays dark, whatever your device is set to."
                                ThemeChoice.SYSTEM ->
                                    "Flowly follows your device setting and switches automatically. " +
                                        "Right now your device is using ${if (deviceIsDark) "dark" else "light"}."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Theme card
// -----------------------------------------------------------------------------

@Composable
private fun ThemeCard(
    choice: ThemeChoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduce = LocalReduceMotion.current
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    // 220 ms, the same easing as the rest of Flowly; instant when motion is reduced.
    fun <T> spec() = if (reduce) snap<T>() else tween<T>(220, easing = AppleEasing)

    val borderColor by animateColorAsState(if (selected) accent else outline, spec(), label = "themeBorder")
    val borderWidth by animateDpAsState(if (selected) 1.5.dp else 0.5.dp, spec(), label = "themeBorderWidth")
    val indicatorFill by animateColorAsState(if (selected) accent else Color.Transparent, spec(), label = "themeIndicator")
    val indicatorRing by animateColorAsState(if (selected) accent else outline, spec(), label = "themeIndicatorRing")
    val checkAlpha by animateFloatAsState(if (selected) 1f else 0f, spec(), label = "themeCheck")

    Column(
        modifier = modifier
            .pressable(pressedScale = 0.985f, pressedAlpha = 0.92f, role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag(choice.tag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.66f)
                .clip(RoundedCornerShape(18.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                when (choice) {
                    ThemeChoice.LIGHT -> drawMiniFlowly(LightMini)
                    ThemeChoice.DARK -> drawMiniFlowly(DarkMini)
                    ThemeChoice.SYSTEM -> {
                        drawMiniFlowly(LightMini)
                        clipRect(left = size.width / 2f) { drawMiniFlowly(DarkMini) }
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.35f),
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = 0.75.dp.toPx()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = choice.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = choice.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(indicatorFill)
                    .border(1.dp, indicatorRing, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = checkAlpha),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Miniature Flowly interface: header, balance card, two transactions, tab bar.
// -----------------------------------------------------------------------------

private fun DrawScope.drawMiniFlowly(p: MiniPalette) {
    val w = size.width
    val h = size.height
    val pad = w * 0.10f
    val hair = 0.5.dp.toPx()

    fun bar(x: Float, y: Float, bw: Float, bh: Float, color: Color) =
        drawRoundRect(color, Offset(x, y), Size(bw, bh), CornerRadius(bh / 2f, bh / 2f))

    drawRect(p.background)

    // Header
    bar(pad, h * 0.075f, w * 0.34f, h * 0.034f, p.text)
    bar(pad, h * 0.130f, w * 0.22f, h * 0.020f, p.subtle)

    // Balance card
    val cardTop = h * 0.20f
    val cardH = h * 0.26f
    val cardW = w - 2 * pad
    val radius = CornerRadius(w * 0.07f, w * 0.07f)
    drawRoundRect(p.surface, Offset(pad, cardTop), Size(cardW, cardH), radius)
    drawRoundRect(p.border, Offset(pad, cardTop), Size(cardW, cardH), radius, style = Stroke(hair))
    val inner = w * 0.07f
    bar(pad + inner, cardTop + cardH * 0.17f, cardW * 0.28f, h * 0.018f, p.subtle)
    bar(pad + inner, cardTop + cardH * 0.38f, cardW * 0.52f, h * 0.036f, p.text)
    val trackY = cardTop + cardH * 0.76f
    val trackW = cardW - 2 * inner
    bar(pad + inner, trackY, trackW, h * 0.016f, p.subtle.copy(alpha = 0.35f))
    bar(pad + inner, trackY, trackW * 0.62f, h * 0.016f, p.accent)

    // Two transaction rows
    for (i in 0..1) {
        val y = h * (0.52f + i * 0.115f)
        val rowH = h * 0.095f
        drawRoundRect(p.surface, Offset(pad, y), Size(cardW, rowH), CornerRadius(w * 0.05f, w * 0.05f))
        drawRoundRect(p.border, Offset(pad, y), Size(cardW, rowH), CornerRadius(w * 0.05f, w * 0.05f), style = Stroke(hair))
        val cy = y + rowH / 2f
        drawCircle(p.accent.copy(alpha = 0.22f), radius = rowH * 0.28f, center = Offset(pad + inner * 1.1f, cy))
        bar(pad + inner * 2.3f, cy - h * 0.014f, cardW * 0.30f, h * 0.018f, p.text)
        bar(pad + inner * 2.3f, cy + h * 0.008f, cardW * 0.18f, h * 0.013f, p.subtle)
        bar(pad + cardW - inner - cardW * 0.18f, cy - h * 0.009f, cardW * 0.18f, h * 0.018f, p.text)
    }

    // Tab bar
    val navH = h * 0.095f
    val navY = h * 0.865f
    val navRadius = CornerRadius(navH / 2f, navH / 2f)
    drawRoundRect(p.surface, Offset(pad, navY), Size(cardW, navH), navRadius)
    drawRoundRect(p.border, Offset(pad, navY), Size(cardW, navH), navRadius, style = Stroke(hair))
    for (i in 0..4) {
        val cx = pad + cardW * (i + 0.5f) / 5f
        val cy = navY + navH / 2f
        if (i == 2) drawCircle(p.accent, radius = navH * 0.2f, center = Offset(cx, cy))
        else drawCircle(p.subtle, radius = navH * 0.12f, center = Offset(cx, cy))
    }
}
