package com.example.feature.loans

import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.asAmount
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.AppleIconButton
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.Loan
import com.example.domain.model.LoanStatus
import com.example.domain.model.LoanType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.util.Locale

@Composable
fun LoansScreen(
    viewModel: LoansViewModel,
    onNavigateBack: () -> Unit,
    onLoanSelected: (Long) -> Unit = {},
    onLoanClick: (Long) -> Unit = onLoanSelected,
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
                modifier = Modifier.testTag("loans_back_button")
            )

                Text(
                    text = "Loans & Liabilities",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { viewModel.openAddLoanDialog() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(EmeraldPrimary)
                        .testTag("add_loan_button")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Loan",
                        tint = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // 1. Debt & EMI Summary Card
            if (uiState.loans.isNotEmpty()) {
                item {
                    val totalRemaining = uiState.loans.filter { it.status == LoanStatus.ACTIVE }.sumOf { it.remainingBalance }
                    val totalEmi = uiState.loans.filter { it.status == LoanStatus.ACTIVE }.sumOf { it.monthlyPayment }
                    val totalOriginal = uiState.loans.sumOf { it.originalAmount }

                    LoansOverviewCard(
                        totalRemaining = totalRemaining,
                        totalMonthlyEmi = totalEmi,
                        totalOriginal = totalOriginal,
                        activeLoansCount = uiState.loans.count { it.status == LoanStatus.ACTIVE },
                        currency = uiState.currency,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            // 2. Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active & Past Loans",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.loans.size} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. Loans List or Empty
            if (uiState.loans.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Loans or Debts Tracked",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Keep track of EMIs, principal balances, and interest amortization clearly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { viewModel.openAddLoanDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Loan / EMI", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(uiState.loans) { loan ->
                    LoanCardItem(
                        loan = loan,
                        onPayClick = { viewModel.openPaymentDialog(loan) },
                        onEditClick = { viewModel.openEditLoanDialog(loan) },
                        onDeleteClick = { viewModel.requestDeleteLoan(loan) },
                        onCardClick = {
                            viewModel.selectLoan(loan.id)
                            onLoanSelected(loan.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
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

    // Record Payment Dialog
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

    // Delete Confirmation Dialog
    if (uiState.loanToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("Delete Loan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Are you sure you want to delete ${uiState.loanToDelete?.name}? Payment records associated with this loan will be removed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteLoan() },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun LoansOverviewCard(
    totalRemaining: Double,
    totalMonthlyEmi: Double,
    totalOriginal: Double,
    activeLoansCount: Int,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("loans_overview_card"),
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
                    text = "Total remaining debt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(ExpenseRed.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeLoansCount Active Loans",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = CurrencyFormatter.format(totalRemaining, currency),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp).asAmount(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text("Monthly EMI Obligation", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        CurrencyFormatter.format(totalMonthlyEmi, currency),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                        color = ExpenseRed
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text("Initial Principal", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        CurrencyFormatter.format(totalOriginal, currency),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun LoanCardItem(
    loan: Loan,
    onPayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val progress = (loan.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f)
    val isPaidOff = loan.status == LoanStatus.PAID_OFF || loan.remainingBalance <= 0

    Card(
        modifier = modifier
            .testTag("loan_card_item_${loan.id}")
            .clickable { onCardClick() },
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPaidOff) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPaidOff) Icons.Default.CheckCircle else Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (isPaidOff) IncomeGreen else ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = loan.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${loan.lender} • ${loan.loanType.name.replace("_", " ")}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Loan", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Loan", color = ExpenseRed) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Remaining vs Original
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Remaining Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (isPaidOff) "PAID OFF" else CurrencyFormatter.format(loan.remainingBalance, loan.currency),
                        style = MaterialTheme.typography.headlineMedium.asAmount(),
                        color = if (isPaidOff) IncomeGreen else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Monthly EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.format(loan.monthlyPayment, loan.currency),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold).asAmount(),
                        color = ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .background(IncomeGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${loan.interestRate}% Interest",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format(Locale.getDefault(), "%.0f%%", loan.progressPercentage)} Paid Off",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                    color = IncomeGreen
                )
            }

            if (!isPaidOff) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onPayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pay_emi_button_${loan.id}")
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record EMI Payment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AddEditLoanDialog(
    uiState: LoansUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onLenderChange: (String) -> Unit,
    onTypeChange: (LoanType) -> Unit,
    onOriginalChange: (String) -> Unit,
    onRemainingChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onEmiChange: (String) -> Unit,
    onAccountSelect: (Long?) -> Unit
) {
    val isEditing = uiState.editingLoan != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Loan" else "Add Loan / Liability",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.nameInput,
                    onValueChange = onNameChange,
                    label = { Text("Loan Title (e.g. Home Loan)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("loan_name_input")
                )

                OutlinedTextField(
                    value = uiState.lenderInput,
                    onValueChange = onLenderChange,
                    label = { Text("Lender / Bank (e.g. SBI, HDFC)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("loan_lender_input")
                )

                Text("Loan Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LoanType.values().forEach { type ->
                        val isSelected = uiState.typeInput == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTypeChange(type) },
                            label = { Text(type.name.replace("_", " "), fontSize = 11.5.sp) },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.originalAmountInput,
                        onValueChange = onOriginalChange,
                        label = { Text("Original Principal (${uiState.currency})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("loan_original_input")
                    )

                    OutlinedTextField(
                        value = uiState.remainingBalanceInput,
                        onValueChange = onRemainingChange,
                        label = { Text("Remaining (${uiState.currency})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("loan_remaining_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.interestRateInput,
                        onValueChange = onRateChange,
                        label = { Text("Interest Rate (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("loan_rate_input")
                    )

                    OutlinedTextField(
                        value = uiState.monthlyPaymentInput,
                        onValueChange = onEmiChange,
                        label = { Text("Monthly EMI (${uiState.currency})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("loan_emi_input")
                    )
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                modifier = Modifier.testTag("save_loan_button")
            ) {
                Text("Save Loan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun RecordLoanPaymentDialog(
    uiState: LoansUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onTotalAmountChange: (String) -> Unit,
    onPrincipalChange: (String) -> Unit,
    onInterestChange: (String) -> Unit,
    onAccountSelect: (Long) -> Unit,
    onNoteChange: (String) -> Unit
) {
    val loan = uiState.payingLoan ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record EMI: ${loan.name}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Remaining balance: ${CurrencyFormatter.format(loan.remainingBalance, loan.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = uiState.paymentAmountInput,
                    onValueChange = onTotalAmountChange,
                    label = { Text("Total EMI Amount (${loan.currency})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("loan_payment_amount_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.principalAmountInput,
                        onValueChange = onPrincipalChange,
                        label = { Text("Principal Portion") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.interestAmountInput,
                        onValueChange = onInterestChange,
                        label = { Text("Interest Portion") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Pay From Account", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.accounts.forEach { acc ->
                        val isSelected = uiState.paymentSourceAccountId == acc.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onAccountSelect(acc.id) },
                            label = { Text("${acc.name} (${CurrencyFormatter.format(acc.balance, uiState.currency)})", fontSize = 11.5.sp) },
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

                OutlinedTextField(
                    value = uiState.paymentNoteInput,
                    onValueChange = onNoteChange,
                    label = { Text("Note / Receipt #") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_loan_payment_button")
            ) {
                Text("Confirm Payment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
