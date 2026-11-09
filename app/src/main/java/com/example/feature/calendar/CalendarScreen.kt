package com.example.feature.calendar

import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.asAmount
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.AppleIconButton
import com.example.core.ui.EmptyState
import com.example.core.util.CategoryIconHelper
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import com.example.domain.model.CalendarDaySummary
import com.example.domain.model.SpendingIntensity
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDaySummary = uiState.dayDataMap[uiState.selectedDayOfMonth]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("calendar_screen")
    ) {
        // Top App Bar
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
                onClick = onNavigateBack
            )

            Text(
                text = "Financial Calendar",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp))
                    .clickable { viewModel.onTodayClicked() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("calendar_today_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Month Selector & Totals
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Month Navigation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.onPreviousMonth() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = MaterialTheme.colorScheme.onSurface)
                            }

                            Text(
                                text = uiState.monthTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = { viewModel.onNextMonth() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Month Totals Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.format(uiState.totalMonthIncome, uiState.defaultCurrency),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold).asAmount(),
                                    color = EmeraldPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.format(uiState.totalMonthExpense, uiState.defaultCurrency),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold).asAmount(),
                                    color = ExpenseRed
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Net Cashflow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.format(uiState.netMonthCashFlow, uiState.defaultCurrency),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold).asAmount(),
                                    color = if (uiState.netMonthCashFlow >= 0) EmeraldPrimary else ExpenseRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Days of week header
                        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid (weeks)
                        val totalCells = uiState.firstDayOfWeekOffset + uiState.daysInMonth
                        val numRows = (totalCells + 6) / 7

                        for (row in 0 until numRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNumber = cellIndex - uiState.firstDayOfWeekOffset + 1

                                    if (dayNumber in 1..uiState.daysInMonth) {
                                        val daySummary = uiState.dayDataMap[dayNumber]
                                        val isSelected = uiState.selectedDayOfMonth == dayNumber

                                        CalendarDayCell(
                                            dayNumber = dayNumber,
                                            isSelected = isSelected,
                                            summary = daySummary,
                                            onClick = { viewModel.onDaySelected(dayNumber) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Section Header
            item {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, uiState.selectedYear)
                    set(Calendar.MONTH, uiState.selectedMonth)
                    set(Calendar.DAY_OF_MONTH, uiState.selectedDayOfMonth)
                }
                val dateTitle = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(cal.time).uppercase()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedDaySummary != null && (selectedDaySummary.totalExpense > 0 || selectedDaySummary.totalIncome > 0)) {
                        Text(
                            text = "Net: ${CurrencyFormatter.format(selectedDaySummary.netCashFlow, uiState.defaultCurrency)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedDaySummary.netCashFlow >= 0) EmeraldPrimary else ExpenseRed
                        )
                    }
                }
            }

            // Upcoming recurring due on selected day
            if (selectedDaySummary != null && selectedDaySummary.upcomingRecurring.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        selectedDaySummary.upcomingRecurring.forEach { recDetail ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Repeat, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${recDetail.recurring.name} (Due Subscription)",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Scheduled Renewal",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.format(recDetail.recurring.amount, uiState.defaultCurrency),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold).asAmount(),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transactions list on selected day
            if (selectedDaySummary == null || selectedDaySummary.transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No spending or income recorded on this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = selectedDaySummary.transactions,
                    key = { "cal_tx_${it.transaction.id}" }
                ) { item ->
                    CalendarTransactionCard(
                        detail = item,
                        currencySymbol = uiState.defaultCurrency
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dayNumber: Int,
    isSelected: Boolean,
    summary: CalendarDaySummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val intensityColor = when (summary?.intensity) {
        SpendingIntensity.HIGH -> ExpenseRed
        SpendingIntensity.MEDIUM -> Color(0xFFF59E0B)
        SpendingIntensity.LOW -> EmeraldPrimary
        SpendingIntensity.NONE, null -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) EmeraldPrimary else Color.Transparent
                ),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .testTag("calendar_day_$dayNumber"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Indicators row
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (summary != null && summary.intensity != SpendingIntensity.NONE) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(intensityColor)
                    )
                }

                if (summary?.hasIncome == true) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6))
                    )
                }

                if (summary?.hasRecurring == true) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6))
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarTransactionCard(
    detail: TransactionDetail,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val tx = detail.transaction
    val category = detail.category
    val isExpense = tx.type == TransactionType.EXPENSE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconName = category?.iconName ?: "AccountBalanceWallet"
            val iconColorHex = category?.colorHex ?: if (isExpense) "#F59E0B" else "#10B981"
            val catColor = try {
                Color(android.graphics.Color.parseColor(iconColorHex))
            } catch (e: Exception) {
                EmeraldPrimary
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconHelper.getIconByName(iconName),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.note.ifBlank { category?.name ?: "Transaction" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${detail.sourceAccount?.name ?: "Account"} • ${DateUtils.formatTime(tx.dateTime)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${if (isExpense) "-" else "+"}${CurrencyFormatter.format(tx.amount, currencySymbol)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isExpense) MaterialTheme.colorScheme.onSurface else EmeraldPrimary
            )
        }
    }
}
