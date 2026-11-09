package com.example.core.ui

import androidx.compose.animation.animateColorAsState
import com.example.ui.theme.asAmount
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.CurrencyFormatter
import com.example.ui.theme.AppleEasing
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.FlowlyMotion
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.RingModerate
import com.example.ui.theme.RingSafe
import androidx.compose.animation.core.tween

/**
 * Today's spending as a single, calm ring.
 * Flat colour, no halo, no gradient. The sweep draws in once on first appearance and
 * afterwards only animates between values. Colour changes also carry a text label,
 * so state is never conveyed by colour alone.
 */
@Composable
fun SpendingRingHero(
    todaySpending: Double,
    dailyBudget: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    size: Dp = 215.dp,
    strokeWidth: Dp = 12.dp
) {
    val progressRatio = if (dailyBudget > 0) {
        (todaySpending / dailyBudget).toFloat().coerceIn(0f, 1.5f)
    } else 0f
    val percentInt = (progressRatio * 100).toInt()
    val targetSweep = progressRatio.coerceAtMost(1f) * 360f

    val reduce = LocalReduceMotion.current
    var lastSweep by rememberSaveable { mutableFloatStateOf(0f) }
    val sweep = remember { Animatable(if (reduce) targetSweep else lastSweep) }
    LaunchedEffect(targetSweep) {
        if (reduce) {
            sweep.snapTo(targetSweep)
        } else {
            sweep.animateTo(targetSweep, tween(durationMillis = 900, easing = AppleEasing))
        }
        lastSweep = targetSweep
    }

    val ringColor by animateColorAsState(
        targetValue = when {
            progressRatio <= 0.70f -> RingSafe
            progressRatio <= 1.0f -> RingModerate
            else -> ExpenseRed
        },
        animationSpec = FlowlyMotion.standard(),
        label = "ringColor"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .size(size)
            .testTag("spending_ring_hero"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = this.size.minDimension - strokePx
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )
            if (sweep.value > 0.5f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = sweep.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Spent today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = CurrencyFormatter.format(todaySpending, currencySymbol),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp).asAmount(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("today_spending_amount")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(ringColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$percentInt% of budget",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
