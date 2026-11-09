package com.example.feature.cashflow

import androidx.compose.animation.AnimatedVisibility
import com.example.ui.theme.asAmount
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.AppleIconButton
import com.example.domain.model.CashFlowForecast
import com.example.domain.model.ForecastEvent
import com.example.domain.model.ForecastPeriod
import com.example.domain.model.TransactionType
import com.example.domain.model.label
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.core.util.CategoryIconHelper
import com.example.core.util.CurrencyFormatter
import java.util.Locale

@Composable
fun CashFlowForecastScreen(
    viewModel: CashFlowViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecurring: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack,
                modifier = Modifier.testTag("cash_flow_back_button")
            )

                Text(
                    text = "Cash Flow Forecast",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.size(40.dp))
            }
        }
    ) { innerPadding ->
        val forecast = uiState.forecast

        if (forecast == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // 1. Period Selector Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ForecastPeriod.values().forEach { period ->
                            val isSelected = uiState.selectedPeriod == period
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onPeriodSelected(period) },
                                label = {
                                    Text(
                                        text = period.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // 2. Projected Balance Hero Card
                item {
                    ForecastHeroCard(
                        forecast = forecast,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // 3. Safe Daily Spending Recommendation Card
                item {
                    SafeSpendingCard(
                        forecast = forecast,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // 4. Balance Trajectory Line Chart
                if (forecast.timelineEvents.isNotEmpty()) {
                    item {
                        ForecastTrajectoryCard(
                            forecast = forecast,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }

                // 5. Timeline Events Header
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Projected Inflows & Outflows",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${forecast.timelineEvents.size} events",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 6. Timeline Events List
                if (forecast.timelineEvents.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No recurring transactions scheduled in this window.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onNavigateToRecurring,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Recurring Transaction", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(forecast.timelineEvents) { event ->
                        ForecastEventItem(
                            event = event,
                            currency = forecast.currency
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastHeroCard(
    forecast: CashFlowForecast,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("cash_flow_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projected closing balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Cash Flow Health Badge
                val isPositive = forecast.projectedBalance >= 0
                val badgeColor = if (isPositive) IncomeGreen else ExpenseRed
                val badgeText = if (isPositive) "Healthy" else "Risk: Negative"
                val badgeIcon = if (isPositive) Icons.Default.CheckCircle else Icons.Default.Warning

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        badgeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = CurrencyFormatter.format(forecast.projectedBalance, forecast.currency),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ).asAmount(),
                color = if (forecast.projectedBalance >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Breakdown Grid: Starting | Expected Inflow | Expected Outflow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Starting
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text("Current", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        CurrencyFormatter.format(forecast.currentBalance, forecast.currency),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Expected Inflows
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(IncomeGreen.copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Text("+ Inflows", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        CurrencyFormatter.format(forecast.expectedIncome, forecast.currency),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                        color = IncomeGreen
                    )
                }

                // Expected Outflows
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ExpenseRed.copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Text("- Outflows", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        CurrencyFormatter.format(forecast.expectedExpenses, forecast.currency),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
fun SafeSpendingCard(
    forecast: CashFlowForecast,
    modifier: Modifier = Modifier
) {
    val safeDailySpend = (forecast.projectedBalance / forecast.periodDays).coerceAtLeast(0.0)

    Card(
        modifier = modifier.testTag("safe_spending_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Safe Daily Spending Allowance",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Spend up to ${CurrencyFormatter.format(safeDailySpend, forecast.currency)}/day without risking overdraft during the ${forecast.periodLabel.lowercase()}.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ForecastTrajectoryCard(
    forecast: CashFlowForecast,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Projected Balance Curve",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            val events = forecast.timelineEvents
            val minBal = minOf(forecast.currentBalance, events.minOf { it.runningBalance })
            val maxBal = maxOf(forecast.currentBalance, events.maxOf { it.runningBalance })
            val range = (maxBal - minBal).coerceAtLeast(1.0)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (events.size).coerceAtLeast(1)

                    val path = Path()
                    val allPoints = mutableListOf(Offset(0f, height - (((forecast.currentBalance - minBal) / range).toFloat() * (height * 0.75f)) - (height * 0.1f)))

                    events.forEachIndexed { i, event ->
                        val x = (i + 1) * stepX
                        val normalized = ((event.runningBalance - minBal) / range).toFloat().coerceIn(0f, 1f)
                        val y = height - (normalized * (height * 0.75f)) - (height * 0.1f)
                        allPoints.add(Offset(x, y))
                    }

                    // Draw path
                    allPoints.forEachIndexed { idx, pt ->
                        if (idx == 0) {
                            path.moveTo(pt.x, pt.y)
                        } else {
                            val prev = allPoints[idx - 1]
                            val cx = (prev.x + pt.x) / 2f
                            path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = if (forecast.projectedBalance >= 0) EmeraldPrimary else ExpenseRed,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    allPoints.forEach { pt ->
                        drawCircle(
                            color = if (forecast.projectedBalance >= 0) EmeraldPrimary else ExpenseRed,
                            radius = 3.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastEventItem(
    event: ForecastEvent,
    currency: String,
    modifier: Modifier = Modifier
) {
    val isIncome = event.type == TransactionType.INCOME
    val itemColor = if (isIncome) IncomeGreen else ExpenseRed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Badge
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = event.dateFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(itemColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconHelper.getIconByName(event.categoryIcon),
                    contentDescription = event.title,
                    tint = itemColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Account
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${event.categoryName} • ${event.accountName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount & Running Balance
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.format(event.amount, currency)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = itemColor
                )
                Text(
                    text = "Bal: ${CurrencyFormatter.format(event.runningBalance, currency)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
