package com.example.feature.analytics

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyState
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.InsightType
import com.example.domain.model.SpendingInsight
import com.example.ui.theme.FlowlyMotion
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.SystemBlue
import com.example.ui.theme.TransferCyan
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.asAmount
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

// =============================================================================
// ANALYTICS 2.0
//
// Typography: Manrope for the title, section headings and every financial figure
// (via asAmount()); Inter for labels, captions and supporting text.
// Colour is used only where it carries meaning: the accent for interaction,
// green / amber for spending going down / up, everything else stays neutral.
// =============================================================================

private val OtherSliceColor = Color(0xFF9A9AA0)
private val WeekdayShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val WeekdayFull = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToInsights: () -> Unit = {},
    onNavigateToMerchant: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showRangePicker by remember { mutableStateOf(false) }
    val currency = state.defaultCurrency

    if (showRangePicker) {
        RangePickerDialog(
            onDismiss = { showRangePicker = false },
            onConfirm = { start, end ->
                viewModel.onCustomRangeSelected(start, end)
                showRangePicker = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- Header + period chips ---------------------------------------------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Analytics",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = state.rangeLabel.ifBlank { " " },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AnalyticsPeriod.WEEK to "Week",
                        AnalyticsPeriod.MONTH to "Month",
                        AnalyticsPeriod.YEAR to "Year",
                        AnalyticsPeriod.CUSTOM to "Custom"
                    ).forEach { (period, label) ->
                        PeriodChip(
                            label = label,
                            selected = state.period == period,
                            onClick = {
                                if (period == AnalyticsPeriod.CUSTOM) showRangePicker = true
                                else viewModel.onPeriodSelected(period)
                            },
                            modifier = Modifier.weight(1f).testTag("time_filter_${label.lowercase()}")
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            items(3) { SkeletonCard() }
        } else {
            // ---- Hero: total spent, daily trend, key figures -------------------
            item { HeroCard(state, currency) }

            // ---- Income vs spending ----------------------------------------------
            item { IncomeVsSpendingCard(state, currency) }

            // ---- Where your money went -------------------------------------------
            item { CategoryCard(state, currency) }

            // ---- Weekday pattern ---------------------------------------------------
            item { WeekdayCard(state) }

            // ---- Insights ------------------------------------------------------------
            item { InsightsCard(state.insights, onNavigateToInsights) }

            // ---- Top merchants (follows the selected period) ------------------------
            if (state.topMerchants.isNotEmpty()) {
                item { MerchantsCard(state.topMerchants, currency, onNavigateToMerchant) }
            }
        }
    }
}

// =============================================================================
// Header pieces
// =============================================================================

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = FlowlyMotion.quick(),
        label = "chipBackground"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = FlowlyMotion.quick(),
        label = "chipContent"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(onDismiss: () -> Unit, onConfirm: (Long, Long) -> Unit) {
    val pickerState = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedStartDateMillis != null,
                onClick = {
                    val start = pickerState.selectedStartDateMillis
                    if (start != null) onConfirm(start, pickerState.selectedEndDateMillis ?: start)
                }
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DateRangePicker(
            state = pickerState,
            modifier = Modifier.height(520.dp),
            showModeToggle = false
        )
    }
}

// =============================================================================
// Hero
// =============================================================================

@Composable
private fun HeroCard(state: AnalyticsUiState, currency: String) {
    val animatedTotal = rememberCountUp(state.totalExpense)
    AnalyticsCard(corner = 28.dp, padding = 22.dp) {
        Text(
            "Total spent",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = CurrencyFormatter.format(animatedTotal, currency),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))
        ChangePill(state.totalExpense, state.previousExpense, state.comparisonLabel)
        Spacer(Modifier.height(22.dp))

        if (state.trend.none { it.amount > 0.0 }) {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No spending recorded in this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            TrendBars(points = state.trend, currency = currency)
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), thickness = 0.5.dp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat("Daily average", CurrencyFormatter.format(state.dailyAverage, currency), Modifier.weight(1f))
            MiniStat("Transactions", state.transactionCount.toString(), Modifier.weight(1f))
            MiniStat("Largest", CurrencyFormatter.format(state.largestExpense, currency), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold).asAmount(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChangePill(current: Double, previous: Double, comparisonLabel: String) {
    if (previous <= 0.0) {
        Text(
            "No earlier data to compare",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val change = (current - previous) / previous * 100
    // For spending, lower is better.
    val good = change <= 0
    val tint = if (good) IncomeGreen else WarningAmber
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(tint.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (good) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "${"%.1f".format(abs(change))}%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold).asAmount(),
                color = tint
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "vs $comparisonLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Counts smoothly from the figure on screen to the new one. Instant when motion is reduced. */
@Composable
private fun rememberCountUp(target: Double): Double {
    val reduce = LocalReduceMotion.current
    var from by remember { mutableDoubleStateOf(target) }
    var to by remember { mutableDoubleStateOf(target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target, reduce) {
        if (reduce || target == to) {
            from = target
            to = target
            progress.snapTo(1f)
        } else {
            from = from + (to - from) * progress.value
            to = target
            progress.snapTo(0f)
            progress.animateTo(1f, FlowlyMotion.page())
        }
    }
    return from + (to - from) * progress.value
}

// =============================================================================
// Trend bars — one bar per day / week / month, tap or drag to read a value
// =============================================================================

@Composable
private fun TrendBars(points: List<TrendPoint>, currency: String, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val barColor = MaterialTheme.colorScheme.primary
    val reveal = rememberRevealProgress(points)
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val peak = remember(points) { points.indices.maxByOrNull { points[it].amount } ?: 0 }
    val shown = (selected ?: peak).coerceIn(0, points.lastIndex)

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = (if (selected == null) "Peak · " else "Selected · ") + points[shown].label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyFormatter.format(points[shown].amount, currency),
                style = MaterialTheme.typography.titleMedium.asAmount(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(10.dp))

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val index = barIndexAt(offset.x, size.width.toFloat(), points.size)
                        selected = if (selected == index) null else index
                    }
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> selected = barIndexAt(offset.x, size.width.toFloat(), points.size) },
                        onHorizontalDrag = { change, _ -> selected = barIndexAt(change.position.x, size.width.toFloat(), points.size) }
                    )
                }
        ) {
            val count = points.size
            val slot = size.width / count
            val barWidth = (slot * 0.6f).coerceAtLeast(2.dp.toPx()).coerceAtMost(slot)
            val maxValue = points.maxOf { it.amount }.coerceAtLeast(1.0)
            val minHeight = 3.dp.toPx()
            val corner = minOf(4.dp.toPx(), barWidth / 2f)
            points.forEachIndexed { index, point ->
                val height = minHeight + (point.amount / maxValue).toFloat() * (size.height - minHeight) * reveal
                drawRoundRect(
                    color = barColor.copy(alpha = if (index == shown) 1f else 0.28f),
                    topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(corner, corner)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        val labelIndexes = listOfNotNull(
            0,
            if (points.size >= 3) points.size / 2 else null,
            if (points.size >= 2) points.lastIndex else null
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labelIndexes.forEach {
                Text(
                    points[it].label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun barIndexAt(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    return ((x / width) * count).toInt().coerceIn(0, count - 1)
}

// =============================================================================
// Income vs spending
// =============================================================================

@Composable
private fun IncomeVsSpendingCard(state: AnalyticsUiState, currency: String) {
    AnalyticsCard {
        PanelTitle("Income vs spending", "How much of what came in went out")
        Spacer(Modifier.height(18.dp))
        val maxValue = maxOf(state.totalIncome, state.totalExpense, 1.0)
        FlowBar("Income", state.totalIncome, maxValue, currency, IncomeGreen)
        Spacer(Modifier.height(14.dp))
        FlowBar("Spending", state.totalExpense, maxValue, currency, MaterialTheme.colorScheme.primary)
        if (state.totalIncome > 0.0) {
            Spacer(Modifier.height(16.dp))
            val spentShare = (state.totalExpense / state.totalIncome * 100).roundToInt()
            Text(
                text = "You spent $spentShare% of your income in this period.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FlowBar(label: String, value: Double, maxValue: Double, currency: String, color: Color) {
    val reveal = rememberRevealProgress(value to maxValue)
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                CurrencyFormatter.format(value, currency),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold).asAmount(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value / maxValue).toFloat().coerceIn(0f, 1f) * reveal)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

// =============================================================================
// Where your money went — thin ring, rounded ends, tap to focus
// =============================================================================

private data class DonutSlice(
    val name: String,
    val amount: Double,
    val fraction: Float,
    val color: Color,
    val previousAmount: Double
) {
    val changePercent: Double?
        get() = if (previousAmount > 0.0) (amount - previousAmount) / previousAmount * 100.0 else null
}

/** A calm palette built from the app accent: darker and lighter tints of it, with neutral grey for "Other". */
@Composable
private fun donutPalette(): List<Color> {
    val accent = MaterialTheme.colorScheme.primary
    return listOf(
        lerp(accent, Color.Black, 0.30f),
        accent,
        lerp(accent, Color.White, 0.30f),
        lerp(accent, Color.White, 0.52f),
        lerp(accent, Color.White, 0.68f)
    )
}

private fun buildSlices(items: List<CategorySpend>, palette: List<Color>): List<DonutSlice> {
    val top = items.take(5).mapIndexed { index, item ->
        DonutSlice(item.name, item.amount, item.share, palette[index % palette.size], item.previousAmount)
    }
    val rest = items.drop(5)
    if (rest.isEmpty()) return top
    return top + DonutSlice(
        name = "Other",
        amount = rest.sumOf { it.amount },
        fraction = rest.sumOf { it.share.toDouble() }.toFloat(),
        color = OtherSliceColor,
        previousAmount = rest.sumOf { it.previousAmount }
    )
}

@Composable
private fun CategoryCard(state: AnalyticsUiState, currency: String) {
    AnalyticsCard {
        PanelTitle("Where your money went", "Tap a slice or a row to focus")
        Spacer(Modifier.height(18.dp))
        if (state.categories.isEmpty()) {
            Text(
                "No expenses recorded in this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@AnalyticsCard
        }
        val palette = donutPalette()
        val slices = remember(state.categories, palette) { buildSlices(state.categories, palette) }
        var selectedSlice by remember(slices) { mutableStateOf<Int?>(null) }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CategoryDonut(
                slices = slices,
                selected = selectedSlice,
                onTap = { hit -> selectedSlice = if (hit == selectedSlice) null else hit },
                totalAmount = CurrencyFormatter.format(state.totalExpense, currency),
                currency = currency,
                modifier = Modifier.size(196.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        slices.forEachIndexed { index, slice ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), thickness = 0.5.dp)
            }
            LegendRow(
                slice = slice,
                dimmed = selectedSlice != null && selectedSlice != index,
                currency = currency,
                onClick = { selectedSlice = if (selectedSlice == index) null else index }
            )
        }
    }
}

@Composable
private fun CategoryDonut(
    slices: List<DonutSlice>,
    selected: Int?,
    onTap: (Int?) -> Unit,
    totalAmount: String,
    currency: String,
    modifier: Modifier = Modifier
) {
    val reveal = rememberRevealProgress(slices)
    val dim by animateFloatAsState(
        targetValue = if (selected == null) 1f else 0.22f,
        animationSpec = FlowlyMotion.quick(),
        label = "donutDim"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(slices) {
                    detectTapGestures { offset ->
                        onTap(hitSlice(offset, Size(size.width.toFloat(), size.height.toFloat()), slices))
                    }
                }
        ) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val radius = diameter / 2f
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth))

            // Rounded caps reach half a stroke past each end, so the arc-length gap is stroke width + 5dp.
            val gapDegrees = if (slices.size > 1) {
                Math.toDegrees(((strokeWidth + 5.dp.toPx()) / radius).toDouble()).toFloat()
            } else 0f
            var start = -90f
            slices.forEachIndexed { index, slice ->
                val full = 360f * slice.fraction * reveal
                val sweep = if (reveal >= 1f) (full - gapDegrees).coerceAtLeast(0.1f) else full - gapDegrees
                if (sweep > 0f) {
                    drawArc(
                        color = slice.color.copy(alpha = if (selected == null || selected == index) 1f else dim),
                        startAngle = start + gapDegrees / 2f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
                start += full
            }
        }

        Crossfade(targetState = selected, animationSpec = FlowlyMotion.quick(), label = "donutCenter") { index ->
            val slice = index?.let { slices.getOrNull(it) }
            Column(
                modifier = Modifier.padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = slice?.name ?: "Spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (slice != null) CurrencyFormatter.format(slice.amount, currency) else totalAmount,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp).asAmount(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = if (slice != null) {
                        "${(slice.fraction * 100).roundToInt()}% of spending"
                    } else if (slices.size == 1) "1 category" else "${slices.size} categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/** Which slice a tap landed on; null when the tap was in the middle or outside the ring. */
private fun hitSlice(offset: Offset, size: Size, slices: List<DonutSlice>): Int? {
    val dx = offset.x - size.width / 2f
    val dy = offset.y - size.height / 2f
    val distance = hypot(dx, dy)
    val outer = size.minDimension / 2f
    if (distance < outer * 0.5f || distance > outer * 1.05f) return null
    var degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
    if (degrees < 0f) degrees += 360f
    val position = degrees / 360f
    var covered = 0f
    slices.forEachIndexed { index, slice ->
        covered += slice.fraction
        if (position <= covered) return index
    }
    return slices.lastIndex
}

@Composable
private fun LegendRow(slice: DonutSlice, dimmed: Boolean, currency: String, onClick: () -> Unit) {
    val rowAlpha by animateFloatAsState(
        targetValue = if (dimmed) 0.4f else 1f,
        animationSpec = FlowlyMotion.quick(),
        label = "legendAlpha"
    )
    val rounded = slice.changePercent?.roundToInt()
    val changeText = when {
        rounded == null || rounded == 0 -> "—"
        rounded > 0 -> "+$rounded%"
        else -> "−${abs(rounded)}%"
    }
    val changeColor = when {
        rounded == null || rounded == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        rounded < 0 -> IncomeGreen
        else -> WarningAmber
    }
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(slice.color))
        Spacer(Modifier.width(12.dp))
        Text(
            slice.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            changeText,
            modifier = Modifier.widthIn(min = 44.dp),
            style = MaterialTheme.typography.labelMedium,
            color = changeColor,
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(8.dp))
        Text(
            CurrencyFormatter.format(slice.amount, currency),
            modifier = Modifier.widthIn(min = 76.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold).asAmount(),
            textAlign = TextAlign.End
        )
    }
}

// =============================================================================
// Weekday pattern
// =============================================================================

@Composable
private fun WeekdayCard(state: AnalyticsUiState) {
    val values = state.weekdayAverages
    val maxValue = values.maxOrNull() ?: 0.0
    val peak = if (maxValue > 0.0) values.indexOf(maxValue) else -1
    val reveal = rememberRevealProgress(values)
    val accent = MaterialTheme.colorScheme.primary

    AnalyticsCard {
        PanelTitle(
            "Weekday pattern",
            if (peak >= 0) "${WeekdayFull[peak]} is your biggest spending day" else "No spending recorded in this period."
        )
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().height(92.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEachIndexed { index, value ->
                val fraction = if (maxValue > 0.0) (value / maxValue).toFloat() else 0f
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp + 58.dp * (fraction * reveal))
                            .clip(RoundedCornerShape(6.dp))
                            .background(accent.copy(alpha = if (index == peak) 1f else 0.28f))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        WeekdayShort[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// =============================================================================
// Insights
// =============================================================================

@Composable
private fun InsightsCard(insights: List<SpendingInsight>, onDeepDive: () -> Unit) {
    AnalyticsCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Insights",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Deep dive",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDeepDive)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        if (insights.isEmpty()) {
            EmptyState(
                title = "Not enough data yet",
                subtitle = "Record a few transactions to see insights.",
                icon = Icons.Default.Insights
            )
        } else {
            insights.forEachIndexed { index, insight ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), thickness = 0.5.dp)
                }
                InsightRow(insight)
            }
        }
    }
}

@Composable
private fun InsightRow(insight: SpendingInsight) {
    val tint = when (insight.type) {
        InsightType.POSITIVE -> IncomeGreen
        InsightType.WARNING -> WarningAmber
        InsightType.INFO -> SystemBlue
        InsightType.TREND -> TransferCyan
    }
    val icon = when (insight.iconName) {
        "TrendingDown" -> Icons.AutoMirrored.Filled.TrendingDown
        "TrendingUp" -> Icons.AutoMirrored.Filled.TrendingUp
        "Savings" -> Icons.Default.Savings
        "PieChart" -> Icons.Default.PieChart
        else -> Icons.Default.Insights
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("insight_card_${insight.title.lowercase().replace(" ", "_")}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                insight.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =============================================================================
// Top merchants
// =============================================================================

@Composable
private fun MerchantsCard(merchants: List<MerchantSpend>, currency: String, onMerchantClick: (String) -> Unit) {
    AnalyticsCard {
        PanelTitle("Top merchants", "Where you spent most in this period")
        Spacer(Modifier.height(8.dp))
        val maxSpent = merchants.maxOf { it.total }.coerceAtLeast(1.0)
        merchants.forEachIndexed { index, merchant ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), thickness = 0.5.dp)
            }
            MerchantRow(
                rank = index + 1,
                merchant = merchant,
                fraction = (merchant.total / maxSpent).toFloat(),
                currency = currency,
                onClick = { onMerchantClick(merchant.name) }
            )
        }
    }
}

@Composable
private fun MerchantRow(rank: Int, merchant: MerchantSpend, fraction: Float, currency: String, onClick: () -> Unit) {
    val reveal = rememberRevealProgress(fraction)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                rank.toString(),
                modifier = Modifier.width(22.dp),
                style = MaterialTheme.typography.labelMedium.asAmount(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(Modifier.weight(1f)) {
                Text(
                    merchant.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (merchant.count == 1) "1 transaction" else "${merchant.count} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                CurrencyFormatter.format(merchant.total, currency),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold).asAmount()
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .padding(start = 22.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f) * reveal)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            )
        }
    }
}

// =============================================================================
// Shared building blocks
// =============================================================================

@Composable
private fun AnalyticsCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(Modifier.fillMaxWidth().padding(padding), content = content)
    }
}

@Composable
private fun PanelTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(2.dp))
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SkeletonCard() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    )
}

/** 0 → 1 reveal animation, restarted whenever [key] changes. Instant when motion is reduced. */
@Composable
private fun rememberRevealProgress(key: Any?): Float {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(key, reduce) {
        if (reduce) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, FlowlyMotion.page())
        }
    }
    return progress.value
}
