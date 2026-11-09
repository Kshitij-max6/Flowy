package com.example.feature.loans

import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.asAmount
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.AppleIconButton
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.LoanDetail
import com.example.domain.model.LoanPayment
import com.example.domain.model.LoanStatus
import com.example.domain.model.totalInterestPaid
import com.example.domain.model.totalPrincipalPaid
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoanDetailScreen(
    loanId: Long = 0L,
    viewModel: LoansViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(loanId) {
        if (loanId > 0) {
            viewModel.loadLoanDetail(loanId)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val detail = uiState.selectedLoanDetail

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
                modifier = Modifier.testTag("loan_detail_back_button")
            )

                Text(
                    text = detail?.loan?.name ?: "Loan Details",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (detail != null) {
                    IconButton(
                        onClick = { viewModel.openEditLoanDialog(detail.loan) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    ) { innerPadding ->
        if (detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            val loan = detail.loan
            val isPaidOff = loan.status == LoanStatus.PAID_OFF || loan.remainingBalance <= 0
            val progress = (loan.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 60.dp)
            ) {
                // 1. Hero Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .testTag("loan_detail_hero_card"),
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
                                    text = "Remaining balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isPaidOff) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isPaidOff) "PAID OFF" else "${String.format(Locale.getDefault(), "%.0f%%", loan.progressPercentage)} Cleared",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isPaidOff) IncomeGreen else ExpenseRed
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = CurrencyFormatter.format(loan.remainingBalance, loan.currency),
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp).asAmount(),
                                color = if (isPaidOff) IncomeGreen else MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Progress Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (progress > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .height(8.dp)
                                            .background(IncomeGreen)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Principal Paid vs Interest Paid Breakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(IncomeGreen.copy(alpha = 0.08f))
                                        .padding(12.dp)
                                ) {
                                    Text("Principal Repaid", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        CurrencyFormatter.format(detail.totalPrincipalPaid, loan.currency),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                                        color = IncomeGreen
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(ExpenseRed.copy(alpha = 0.08f))
                                        .padding(12.dp)
                                ) {
                                    Text("Interest Paid", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        CurrencyFormatter.format(detail.totalInterestPaid, loan.currency),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                                        color = ExpenseRed
                                    )
                                }
                            }

                            if (!isPaidOff) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.openPaymentDialog(loan) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("record_emi_button")
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Record EMI Payment", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 2. Loan Parameters Grid Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Loan Parameters",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            LoanParamRow("Lender", loan.lender)
                            LoanParamRow("Loan Type", loan.loanType.name.replace("_", " "))
                            LoanParamRow("Original Amount", CurrencyFormatter.format(loan.originalAmount, loan.currency))
                            LoanParamRow("Interest Rate", "${loan.interestRate}% APR")
                            LoanParamRow("Monthly EMI", CurrencyFormatter.format(loan.monthlyPayment, loan.currency))
                        }
                    }
                }

                // 3. Payment History Section
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Repayment History",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${detail.payments.size} payments logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (detail.payments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "No payment records logged for this loan yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(detail.payments) { payment ->
                        LoanPaymentItem(
                            payment = payment,
                            currency = loan.currency
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog & Payment Dialog
    if (uiState.showAddEditDialog) {
        AddEditLoanDialog(
            uiState = uiState,
            onDismiss = { viewModel.dismissAddEditDialog() },
            onSave = { viewModel.saveLoan() },
            onNameChange = { viewModel.onNameChanged(it) },
            onLenderChange = { viewModel.onLenderChanged(it) },
            onTypeChange = { viewModel.onTypeChanged(it) },
            onOriginalChange = { viewModel.onOriginalAmountChanged(it) },
            onRemainingChange = { viewModel.onRemainingBalanceChanged(it) },
            onRateChange = { viewModel.onInterestRateChanged(it) },
            onEmiChange = { viewModel.onMonthlyPaymentChanged(it) },
            onAccountSelect = { viewModel.onAccountSelected(it) }
        )
    }

    if (uiState.showPaymentDialog) {
        RecordLoanPaymentDialog(
            uiState = uiState,
            onDismiss = { viewModel.dismissPaymentDialog() },
            onConfirm = { viewModel.recordPayment() },
            onTotalAmountChange = { viewModel.onPaymentAmountChanged(it) },
            onPrincipalChange = { viewModel.onPrincipalAmountChanged(it) },
            onInterestChange = { viewModel.onInterestAmountChanged(it) },
            onAccountSelect = { viewModel.onPaymentSourceAccountChanged(it) },
            onNoteChange = { viewModel.onPaymentNoteChanged(it) }
        )
    }
}

@Composable
fun LoanParamRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold).asAmount(), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun LoanPaymentItem(
    payment: LoanPayment,
    currency: String,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(payment.paymentDate) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(payment.paymentDate))
    }

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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IncomeGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Principal: ${CurrencyFormatter.format(payment.principalAmount, currency)} • Interest: ${CurrencyFormatter.format(payment.interestAmount, currency)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = CurrencyFormatter.format(payment.amount, currency),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold).asAmount(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
