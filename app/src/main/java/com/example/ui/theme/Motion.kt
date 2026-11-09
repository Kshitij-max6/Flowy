package com.example.ui.theme

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import kotlinx.coroutines.launch
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Motion language
 * - One easing curve (Apple's standard) for everything that is not interactive.
 * - Springs for anything driven by touch.
 * - Only transform + alpha are animated, so work stays on the GPU render thread.
 * - Everything collapses to a short fade when the system asks to remove animations.
 */
val AppleEasing = CubicBezierEasing(0.28f, 0.11f, 0.32f, 1f)

object FlowlyMotion {
    const val Quick = 180
    const val Standard = 320
    const val Page = 380
    const val Reveal = 480
    const val Stagger = 36
    const val MaxStaggerItems = 8

    fun <T> standard(): FiniteAnimationSpec<T> = tween(Standard, easing = AppleEasing)
    fun <T> quick(): FiniteAnimationSpec<T> = tween(Quick, easing = AppleEasing)
    fun <T> page(): FiniteAnimationSpec<T> = tween(Page, easing = AppleEasing)

    /** Settles fast, no overshoot. */
    fun <T> press(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)

    /** Gentle, barely-there bounce for state changes (selection, toggles). */
    fun <T> responsive(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.86f, stiffness = 420f)
}

/** True when the user disabled system animations (Developer options / Accessibility "Remove animations"). */
val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

// ---- Page transitions (push / pop) ----------------------------------------
// Push: new page slides in from the trailing edge by a fraction of the width
// while the old one drifts back and fades. Pop mirrors it.

private const val PushOffsetFraction = 0.28f

fun pushEnter(reduce: Boolean): EnterTransition =
    if (reduce) fadeIn(tween(FlowlyMotion.Quick))
    else slideInHorizontally(FlowlyMotion.page()) { (it * PushOffsetFraction).toInt() } +
        fadeIn(tween(FlowlyMotion.Page, easing = AppleEasing))

fun pushExit(reduce: Boolean): ExitTransition =
    if (reduce) fadeOut(tween(FlowlyMotion.Quick))
    else slideOutHorizontally(FlowlyMotion.page()) { -(it * 0.12f).toInt() } +
        fadeOut(tween(FlowlyMotion.Page, easing = AppleEasing), targetAlpha = 0.6f)

fun popEnter(reduce: Boolean): EnterTransition =
    if (reduce) fadeIn(tween(FlowlyMotion.Quick))
    else slideInHorizontally(FlowlyMotion.page()) { -(it * 0.12f).toInt() } +
        fadeIn(tween(FlowlyMotion.Page, easing = AppleEasing), initialAlpha = 0.6f)

fun popExit(reduce: Boolean): ExitTransition =
    if (reduce) fadeOut(tween(FlowlyMotion.Quick))
    else slideOutHorizontally(FlowlyMotion.page()) { (it * PushOffsetFraction).toInt() } +
        fadeOut(tween(FlowlyMotion.Page, easing = AppleEasing))

fun tabEnter(): EnterTransition = fadeIn(tween(FlowlyMotion.Standard, delayMillis = 40, easing = AppleEasing))
fun tabExit(): ExitTransition = fadeOut(tween(FlowlyMotion.Quick, easing = AppleEasing))

// ---- Micro-interactions ------------------------------------------------------

/**
 * Press feedback: a springy scale + dim, never a ripple. Reads state from an
 * InteractionSource and applies it in the graphics layer (no recomposition per frame).
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    pressedAlpha: Float = 0.85f,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduce) pressedScale else 1f,
        animationSpec = FlowlyMotion.press(),
        label = "pressScale"
    )
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed) pressedAlpha else 1f,
        animationSpec = FlowlyMotion.quick(),
        label = "pressAlpha"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = pressAlpha
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * One-time entrance: rises 12dp and fades in, staggered by [index] (capped so long
 * lists never feel slow). Runs once per item composition, on the GPU layer only.
 */
fun Modifier.revealOnAppear(index: Int = 0, enabled: Boolean = true): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val skip = reduce || !enabled
    val progress = remember { Animatable(if (skip) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!skip) {
            progress.animateTo(
                1f,
                tween(
                    FlowlyMotion.Reveal,
                    delayMillis = index.coerceIn(0, FlowlyMotion.MaxStaggerItems) * FlowlyMotion.Stagger,
                    easing = AppleEasing
                )
            )
        }
    }
    this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 12.dp.toPx()
    }
}

// ---- App-wide press feedback (replaces Material ripple) --------------------
// Every clickable, button and list row inherits this through LocalIndication:
// content dips to 98% and dims slightly while pressed, then eases back.
// Drawn in the draw phase only, so no recomposition and no layout work per frame.
object ApplePressIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(
        interactionSource: androidx.compose.foundation.interaction.InteractionSource
    ): androidx.compose.ui.node.DelegatableNode = ApplePressNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === ApplePressIndication
    override fun hashCode(): Int = -1
}

private class ApplePressNode(
    private val interactionSource: androidx.compose.foundation.interaction.InteractionSource
) : androidx.compose.ui.Modifier.Node(), androidx.compose.ui.node.DrawModifierNode {

    private val amount = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            var active = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is androidx.compose.foundation.interaction.PressInteraction.Press -> {
                        active++
                        launch { amount.animateTo(1f, tween(90, easing = LinearEasing)) }
                    }
                    is androidx.compose.foundation.interaction.PressInteraction.Release,
                    is androidx.compose.foundation.interaction.PressInteraction.Cancel -> {
                        active = (active - 1).coerceAtLeast(0)
                        if (active == 0) launch { amount.animateTo(0f, tween(240, easing = AppleEasing)) }
                    }
                }
            }
        }
    }

    override fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.draw() {
        val p = amount.value
        if (p <= 0.001f) {
            drawContent()
        } else {
            val s = 1f - 0.02f * p
            scale(s, s) {
                this@draw.drawContent()
                drawRect(color = androidx.compose.ui.graphics.Color.Gray, alpha = 0.14f * p)
            }
        }
    }
}
