package com.example.feature.networth

import androidx.compose.animation.AnimatedVisibility
import com.example.ui.theme.asAmount
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.AppleIconButton
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.NetWorthPoint
import com.example.domain.model.NetWorthSummary
import com.example.domain.model.NetWorthTimeRange
import com.example.domain.model.label
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.heroBalanceStyle
import com.example.ui.theme.financialAmountStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun NetWorthScreen(
    viewModel: NetWorthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCreditCards: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.showSnapshotSavedToast) {
        if (uiState.showSnapshotSavedToast) {
            snackbarHostState.showSnackbar("Net Worth snapshot recorded successfully")
            viewModel.dismissToast()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                modifier = Modifier.testTag("net_worth_back_button")
            )

                Text(
                    text = "Net Worth",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { viewModel.takeSnapshot() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                        .testTag("net_worth_snapshot_button")
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Take Snapshot",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        val summary = uiState.summary

        if (summary == null) {
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
                // 1. Hero Net Worth Card
                item {
                    NetWorthHeroCard(
                        summary = summary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // 2. Trend Chart Section
                item {
                    NetWorthChartCard(
                        historyPoints = uiState.historyPoints,
                        selectedRange = uiState.selectedTimeRange,
                        onRangeSelected = { viewModel.onTimeRangeSelected(it) },
                        currency = summary.currency,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // 3. Asset & Liability Ratio Bar
                item {
                    AssetLiabilityRatioCard(
                        summary = summary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // 4. Assets Breakdown
                item {
                    BreakdownSectionHeader(
                        title = "Assets",
                        totalFormatted = CurrencyFormatter.format(summary.totalAssets, summary.currency),
                        totalColor = IncomeGreen,
                        onManageClick = onNavigateToAccounts,
                        manageText = "Manage Accounts"
                    )
                }

                items(summary.assetAccounts) { account ->
                    val percentage = if (summary.totalAssets > 0) (account.balance / summary.totalAssets) * 100 else 0.0
                    AssetAccountItem(
                        name = account.name,
                        category = account.type.name.replace("_", " "),
                        amount = CurrencyFormatter.format(account.balance, summary.currency),
                        percentage = String.format(Locale.getDefault(), "%.1f%%", percentage),
                        colorHex = account.colorHex
                    )
                }

                // 5. Liabilities Breakdown
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    BreakdownSectionHeader(
                        title = "Liabilities",
                        totalFormatted = CurrencyFormatter.format(summary.totalLiabilities, summary.currency),
                        totalColor = ExpenseRed,
                        onManageClick = onNavigateToLoans,
                        manageText = "Manage Loans"
                    )
                }

                if (summary.creditCards.isEmpty() && summary.loans.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "No liabilities or active loans logged.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // Credit Cards
                items(summary.creditCards) { card ->
                    LiabilityItem(
                        name = card.name,
                        subText = "Credit Card • Due ${card.paymentDueDay}th",
                        amount = CurrencyFormatter.format(card.outstandingBalance, summary.currency),
                        isCreditCard = true,
                        onClick = onNavigateToCreditCards
                    )
                }

                // Loans
                items(summary.loans) { loan ->
                    LiabilityItem(
                        name = loan.name,
                        subText = "${loan.loanType.name.replace("_", " ")} • ${loan.interestRate}% APR",
                        amount = CurrencyFormatter.format(loan.remainingBalance, summary.currency),
                        isCreditCard = false,
                        onClick = onNavigateToLoans
                    )
                }

                // 6. Navigation Quick Links
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToCreditCards,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Credit Cards", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onNavigateToLoans,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Loans & EMI", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetWorthHeroCard(
    summary: NetWorthSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("net_worth_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                    text = "Total net worth",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Growth badge
                val isPositive = summary.isPositiveChange
                val badgeBg = if (isPositive) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
                val badgeTextColor = if (isPositive) IncomeGreen else ExpenseRed
                val icon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%s%.1f%%", if (summary.changePercentage >= 0) "+" else "", summary.changePercentage),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = CurrencyFormatter.format(summary.netWorth, summary.currency),
                style = heroBalanceStyle(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = if (summary.netWorth >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Assets vs Liabilities Cards inside Hero
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Assets Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(IncomeGreen.copy(alpha = 0.08f))
                        .border(BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Assets",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(summary.totalAssets, summary.currency),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                            color = IncomeGreen
                        )
                    }
                }

                // Liabilities Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ExpenseRed.copy(alpha = 0.08f))
                        .border(BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Liabilities",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(summary.totalLiabilities, summary.currency),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                            color = ExpenseRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetWorthChartCard(
    historyPoints: List<NetWorthPoint>,
    selectedRange: NetWorthTimeRange,
    onRangeSelected: (NetWorthTimeRange) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("net_worth_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Net Worth Trajectory",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Time range selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NetWorthTimeRange.values().forEach { range ->
                    val isSelected = range == selectedRange
                    FilterChip(
                        selected = isSelected,
                        onClick = { onRangeSelected(range) },
                        label = {
                            Text(
                                text = range.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
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

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Line Chart
            if (historyPoints.size >= 2) {
                var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val points = historyPoints
                    val minVal = points.minOf { it.netWorth }
                    val maxVal = points.maxOf { it.netWorth }
                    val range = (maxVal - minVal).coerceAtLeast(1.0)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(points) {
                                detectTapGestures { tapOffset ->
                                    val stepWidth = size.width / (points.size - 1).coerceAtLeast(1)
                                    val tappedIndex = ((tapOffset.x + (stepWidth / 2)) / stepWidth).toInt().coerceIn(0, points.size - 1)
                                    selectedPointIndex = tappedIndex
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingBottom = 24.dp.toPx()
                        val chartHeight = height - paddingBottom
                        val stepX = width / (points.size - 1).coerceAtLeast(1)

                        val path = Path()
                        val fillPath = Path()

                        val coords = points.mapIndexed { index, point ->
                            val x = index * stepX
                            val normalized = ((point.netWorth - minVal) / range).toFloat().coerceIn(0f, 1f)
                            val y = chartHeight - (normalized * (chartHeight * 0.75f)) - (chartHeight * 0.1f)
                            Offset(x, y)
                        }

                        // Draw Grid lines
                        val gridColor = Color.White.copy(alpha = 0.05f)
                        drawLine(gridColor, Offset(0f, chartHeight * 0.25f), Offset(width, chartHeight * 0.25f), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, chartHeight * 0.75f), Offset(width, chartHeight * 0.75f), strokeWidth = 1f)

                        // Build line and fill paths
                        coords.forEachIndexed { i, offset ->
                            if (i == 0) {
                                path.moveTo(offset.x, offset.y)
                                fillPath.moveTo(offset.x, chartHeight)
                                fillPath.lineTo(offset.x, offset.y)
                            } else {
                                val prev = coords[i - 1]
                                val controlX = (prev.x + offset.x) / 2f
                                path.cubicTo(controlX, prev.y, controlX, offset.y, offset.x, offset.y)
                                fillPath.cubicTo(controlX, prev.y, controlX, offset.y, offset.x, offset.y)
                            }
                        }

                        fillPath.lineTo(coords.last().x, chartHeight)
                        fillPath.close()

                        // Gradient fill under curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(EmeraldPrimary.copy(alpha = 0.35f), EmeraldPrimary.copy(alpha = 0.0f)),
                                startY = 0f,
                                endY = chartHeight
                            )
                        )

                        // Line stroke
                        drawPath(
                            path = path,
                            color = EmeraldPrimary,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Data points
                        coords.forEachIndexed { i, offset ->
                            val isSelected = selectedPointIndex == i
                            drawCircle(
                                color = if (isSelected) Color.White else EmeraldPrimary,
                                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                center = offset
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = EmeraldPrimary,
                                    radius = 10.dp.toPx(),
                                    center = offset,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // X-Axis Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayLabels = listOf(
                        historyPoints.firstOrNull()?.label ?: "",
                        historyPoints.getOrNull(historyPoints.size / 2)?.label ?: "",
                        historyPoints.lastOrNull()?.label ?: ""
                    )
                    displayLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Selected Point tooltip if tapped
                selectedPointIndex?.let { idx ->
                    val pt = historyPoints.getOrNull(idx)
                    if (pt != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pt.label,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.format(pt.netWorth, currency),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                                color = if (pt.netWorth >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Historical points will show up as your accounts evolve.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AssetLiabilityRatioCard(
    summary: NetWorthSummary,
    modifier: Modifier = Modifier
) {
    val total = (summary.totalAssets + summary.totalLiabilities).coerceAtLeast(1.0)
    val assetRatio = (summary.totalAssets / total).toFloat().coerceIn(0f, 1f)
    val liabRatio = (summary.totalLiabilities / total).toFloat().coerceIn(0f, 1f)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Asset to Debt Ratio",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.0f%% Assets", assetRatio * 100),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = IncomeGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (assetRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(assetRatio)
                            .height(12.dp)
                            .background(IncomeGreen)
                    )
                }
                if (liabRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ExpenseRed)
                    )
                }
            }
        }
    }
}

@Composable
fun BreakdownSectionHeader(
    title: String,
    totalFormatted: String,
    totalColor: Color,
    onManageClick: () -> Unit,
    manageText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = totalFormatted,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold).asAmount(),
                color = totalColor
            )
        }

        Text(
            text = manageText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = EmeraldPrimary,
            modifier = Modifier.clickable { onManageClick() }
        )
    }
}

@Composable
fun AssetAccountItem(
    name: String,
    category: String,
    amount: String,
    percentage: String,
    colorHex: String
) {
    val accColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        EmeraldPrimary
    }

    Card(
        modifier = Modifier
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
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = percentage,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LiabilityItem(
    name: String,
    subText: String,
    amount: String,
    isCreditCard: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCreditCard) Color(0xFFF43F5E).copy(alpha = 0.15f) else Color(0xFFE11D48).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCreditCard) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = ExpenseRed,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ExpenseRed
            )
        }
    }
}
