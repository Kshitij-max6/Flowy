package com.example.feature.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.AppleSegmentedControl
import com.example.core.ui.EmptyState
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.InsightType
import com.example.domain.model.MerchantSummary
import com.example.domain.model.SpendingInsight
import com.example.domain.model.TimeFilter
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.FlowlyMotion
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.SystemBlue
import com.example.ui.theme.SystemGreen
import com.example.ui.theme.SystemIndigo
import com.example.ui.theme.SystemOrange
import com.example.ui.theme.SystemPink
import com.example.ui.theme.SystemTeal
import com.example.ui.theme.TransferCyan
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.asAmount
import kotlin.math.abs
import kotlin.math.roundToInt

// =============================================================================
// ANALYTICS — premium analytical page
//
// Typography: Manrope for the title, section headings and every financial
// figure (via asAmount()); Inter for labels, captions and supporting text.
// =============================================================================

private val CategoryPalette = listOf(SystemBlue, SystemIndigo, SystemTeal, SystemOrange, SystemPink, SystemGreen)

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToInsights: () -> Unit = {},
    onNavigateToMerchant: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filters = listOf(
        TimeFilter.THIS_WEEK to "Week",
        TimeFilter.THIS_MONTH to "Month",
        TimeFilter.LAST_MONTH to "Last Mo",
        TimeFilter.ALL_TIME to "All Time"
    )
    val currency = state.defaultCurrency

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- Header ----------------------------------------------------------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Analytics",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "A clearer picture of your spending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AppleSegmentedControl(
                    options = filters.map { it.second },
                    selectedIndex = filters.indexOfFirst { it.first == state.selectedTimeFilter }.coerceAtLeast(0),
                    onSelect = { viewModel.onTimeFilterSelected(filters[it].first) },
                    testTags = filters.map { "time_filter_${it.second.lowercase()}" }
                )
            }
        }

        // ---- Hero: total spent + interactive trend ---------------------------
        item {
            AnalyticsPanel(corner = 28.dp, padding = 22.dp) {
                Text(
                    text = "TOTAL SPENT · ${periodTitle(state.selectedTimeFilter).uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = CurrencyFormatter.format(state.totalExpense, currency),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                ChangePill(
                    current = state.totalExpense,
                    previous = state.previousPeriodExpense,
                    allTime = state.selectedTimeFilter == TimeFilter.ALL_TIME
                )
                Spacer(Modifier.height(22.dp))
                if (state.spendingTrend.none { it > 0.0 }) {
                    Box(
                        Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No spending recorded in this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    SpendingTrendChart(
                        values = state.spendingTrend,
                        labels = state.trendLabels,
                        currency = currency
                    )
                }
            }
        }

        // ---- Cash in vs cash out --------------------------------------------
        item {
            AnalyticsPanel {
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

        // ---- Key figures -----------------------------------------------------
        item {
            val top = state.categoryBreakdown.firstOrNull()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "DAILY AVERAGE",
                        value = CurrencyFormatter.format(state.dailyAverage, currency),
                        caption = "Per day, this period",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "TRANSACTIONS",
                        value = state.transactionCount.toString(),
                        caption = "Expenses recorded",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "LARGEST EXPENSE",
                        value = CurrencyFormatter.format(state.largestExpense, currency),
                        caption = state.largestExpenseCategory ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "TOP CATEGORY",
                        value = top?.let { "${(it.percentage * 100).roundToInt()}%" } ?: "—",
                        caption = top?.category?.name ?: "No spending yet",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---- Category donut ----------------------------------------------------
        item {
            AnalyticsPanel {
                PanelTitle("Where your money went", "Share of spending by category")
                Spacer(Modifier.height(20.dp))
                if (state.categoryBreakdown.isEmpty()) {
                    Text(
                        "No expenses recorded in this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val slices = buildSlices(state.categoryBreakdown, MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CategoryDonut(
                            slices = slices,
                            centerAmount = CurrencyFormatter.format(state.totalExpense, currency),
                            modifier = Modifier.size(188.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    slices.forEach { slice ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(slice.color))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                slice.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${(slice.fraction * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                CurrencyFormatter.format(slice.amount, currency),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold).asAmount()
                            )
                        }
                    }
                }
            }
        }

        // ---- Top merchants ---------------------------------------------------
        if (state.topMerchants.isNotEmpty()) {
            item { SectionTitle("Top merchants") }
            item {
                AnalyticsPanel(padding = 8.dp) {
                    val maxSpent = state.topMerchants.maxOf { it.totalSpent }.coerceAtLeast(1.0)
                    state.topMerchants.forEachIndexed { index, merchant ->
                        MerchantRow(
                            rank = index + 1,
                            merchant = merchant,
                            fraction = (merchant.totalSpent / maxSpent).toFloat(),
                            currency = currency,
                            onClick = { onNavigateToMerchant(merchant.name) }
                        )
                    }
                }
            }
        }

        // ---- Smart insights --------------------------------------------------
        item { SectionTitle("Smart insights", actionTitle = "Deep Dive", onActionClick = onNavigateToInsights) }
        if (state.insights.isEmpty()) {
            item {
                EmptyState(
                    title = "Not enough data yet",
                    subtitle = "Record transactions to unlock insights.",
                    icon = Icons.Default.Insights
                )
            }
        } else {
            items(state.insights, key = { "insight_${it.title}_${it.type.name}" }) { insight ->
                InsightCard(insight = insight)
            }
        }
    }
}

// =============================================================================
// Building blocks
// =============================================================================

@Composable
private fun AnalyticsPanel(
    modifier: Modifier = Modifier,
    corner: androidx.compose.ui.unit.Dp = 24.dp,
    padding: androidx.compose.ui.unit.Dp = 20.dp,
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
private fun SectionTitle(
    title: String,
    actionTitle: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        if (actionTitle != null) {
            Text(
                text = actionTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ChangePill(current: Double, previous: Double, allTime: Boolean) {
    if (allTime) {
        Text(
            "Across all recorded transactions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    if (previous <= 0.0) {
        Text(
            "No previous-period comparison yet",
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
                imageVector = if (change <= 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
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
            text = if (good) "lower than previous period" else "higher than previous period",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

@Composable
private fun StatTile(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp).asAmount(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MerchantRow(
    rank: Int,
    merchant: MerchantSummary,
    fraction: Float,
    currency: String,
    onClick: () -> Unit
) {
    val reveal = rememberRevealProgress(fraction)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelMedium.asAmount(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(22.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    merchant.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${merchant.transactionCount} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                CurrencyFormatter.format(merchant.totalSpent, currency),
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
// Trend chart — smooth area line, drag or press to read a value
// =============================================================================

@Composable
private fun SpendingTrendChart(
    values: List<Double>,
    labels: List<String>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
    val reveal = rememberRevealProgress(values)
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val peakIndex = remember(values) { values.indices.maxByOrNull { values[it] } ?: 0 }
    val shownIndex = selected ?: peakIndex
    val shownLabel = labels.getOrNull(shownIndex).orEmpty()

    Column(modifier) {
        // Read-out
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (selected == null) "PEAK" else "SELECTED",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = shownLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyFormatter.format(values.getOrElse(shownIndex) { 0.0 }, currency),
                style = MaterialTheme.typography.titleLarge.asAmount(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(12.dp))

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(values) {
                    detectTapGestures(
                        onPress = { offset ->
                            selected = indexAt(offset.x, size.width.toFloat(), values.size)
                            tryAwaitRelease()
                            selected = null
                        }
                    )
                }
                .pointerInput(values) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> selected = indexAt(offset.x, size.width.toFloat(), values.size) },
                        onHorizontalDrag = { change, _ -> selected = indexAt(change.position.x, size.width.toFloat(), values.size) },
                        onDragEnd = { selected = null },
                        onDragCancel = { selected = null }
                    )
                }
        ) {
            val pad = 10.dp.toPx()
            val maxValue = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))

            for (i in 0..3) {
                val y = pad + (size.height - 2 * pad) * i / 3f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
            }
            if (values.size < 2) return@Canvas

            val points = values.mapIndexed { index, value ->
                val x = size.width * index / (values.size - 1)
                val y = size.height - pad - (value / maxValue).toFloat() * reveal * (size.height - 2 * pad)
                Offset(x, y)
            }
            val line = smoothPath(points)
            val area = smoothPath(points).apply {
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }
            drawPath(
                area,
                Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0f)),
                    startY = 0f,
                    endY = size.height
                )
            )
            drawPath(line, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            val focus = points[shownIndex.coerceIn(points.indices)]
            if (selected != null) {
                drawLine(lineColor.copy(alpha = 0.35f), Offset(focus.x, 0f), Offset(focus.x, size.height), strokeWidth = 1.dp.toPx())
            }
            drawCircle(lineColor.copy(alpha = 0.18f), radius = 11.dp.toPx(), center = focus)
            drawCircle(Color.White, radius = 6.dp.toPx(), center = focus)
            drawCircle(lineColor, radius = 4.dp.toPx(), center = focus)
        }

        Spacer(Modifier.height(10.dp))
        if (labels.size >= 3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(labels.first(), labels[labels.size / 2], labels.last()).forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun indexAt(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    return ((x / width) * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

/** Smooth cubic curve through [points] using horizontal tangents (never overshoots). */
private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val cur = points[i]
        val midX = (prev.x + cur.x) / 2f
        path.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
    }
    return path
}

// =============================================================================
// Category donut
// =============================================================================

private data class DonutSlice(val name: String, val amount: Double, val fraction: Float, val color: Color)

private fun buildSlices(items: List<CategorySpendItem>, otherColor: Color): List<DonutSlice> {
    val top = items.take(5).mapIndexed { index, item ->
        DonutSlice(item.category?.name ?: "Other", item.amount, item.percentage, categoryColor(item.colorHex, index))
    }
    val rest = items.drop(5)
    if (rest.isEmpty()) return top
    return top + DonutSlice("Other", rest.sumOf { it.amount }, rest.sumOf { it.percentage.toDouble() }.toFloat(), otherColor)
}

@Composable
private fun CategoryDonut(slices: List<DonutSlice>, centerAmount: String, modifier: Modifier = Modifier) {
    val reveal = rememberRevealProgress(slices)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 22.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth))

            val gap = if (slices.size > 1) 3f else 0f
            var start = -90f
            slices.forEach { slice ->
                val sweep = (360f * slice.fraction * reveal - gap).coerceAtLeast(0.5f)
                drawArc(
                    color = slice.color,
                    startAngle = start + gap / 2f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                )
                start += 360f * slice.fraction * reveal
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "SPENT",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                centerAmount,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp).asAmount(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

private fun categoryColor(hex: String, index: Int): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: IllegalArgumentException) {
    CategoryPalette[index % CategoryPalette.size]
}

private fun periodTitle(filter: TimeFilter): String = when (filter) {
    TimeFilter.THIS_WEEK -> "This week"
    TimeFilter.THIS_MONTH -> "This month"
    TimeFilter.LAST_MONTH -> "Last month"
    TimeFilter.ALL_TIME -> "All time"
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

// =============================================================================
// Insight card
// =============================================================================

@Composable
fun InsightCard(
    insight: SpendingInsight,
    modifier: Modifier = Modifier
) {
    val (iconBg, iconTint) = when (insight.type) {
        InsightType.POSITIVE -> Pair(IncomeGreen.copy(alpha = 0.14f), IncomeGreen)
        InsightType.WARNING -> Pair(WarningAmber.copy(alpha = 0.14f), WarningAmber)
        InsightType.INFO -> Pair(SystemBlue.copy(alpha = 0.14f), SystemBlue)
        InsightType.TREND -> Pair(TransferCyan.copy(alpha = 0.14f), TransferCyan)
    }

    val iconVector = when (insight.iconName) {
        "TrendingDown" -> Icons.AutoMirrored.Filled.TrendingDown
        "TrendingUp" -> Icons.AutoMirrored.Filled.TrendingUp
        "Savings" -> Icons.Default.Savings
        "PieChart" -> Icons.Default.PieChart
        else -> Icons.Default.Insights
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("insight_card_${insight.title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = iconVector, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
