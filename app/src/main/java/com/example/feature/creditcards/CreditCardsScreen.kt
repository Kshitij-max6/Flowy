package com.example.feature.creditcards

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.AppleIconButton
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.CreditCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.util.Locale

@Composable
fun CreditCardsScreen(
    viewModel: CreditCardsViewModel,
    onNavigateBack: () -> Unit,
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
                modifier = Modifier.testTag("credit_cards_back_button")
            )

                Text(
                    text = "Credit Cards",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { viewModel.openAddCardDialog() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(EmeraldPrimary)
                        .testTag("add_credit_card_button")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Card",
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
            // 1. Overview Summary Card
            if (uiState.cards.isNotEmpty()) {
                item {
                    val totalLimit = uiState.cards.sumOf { it.creditLimit }
                    val totalBalance = uiState.cards.sumOf { it.outstandingBalance }
                    val totalAvailable = (totalLimit - totalBalance).coerceAtLeast(0.0)
                    val overallUtilization = if (totalLimit > 0) (totalBalance / totalLimit) * 100 else 0.0

                    CreditOverviewCard(
                        totalLimit = totalLimit,
                        totalOutstanding = totalBalance,
                        totalAvailable = totalAvailable,
                        utilization = overallUtilization,
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
                        text = "Your Cards",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.cards.size} active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. Cards List or Empty
            if (uiState.cards.isEmpty()) {
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
                                Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Credit Cards Added",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Track limits, dues, and keep your credit utilization in the green zone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { viewModel.openAddCardDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Credit Card", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(uiState.cards) { card ->
                    CreditCardItem(
                        card = card,
                        onPayClick = { viewModel.openPaymentDialog(card) },
                        onEditClick = { viewModel.openEditCardDialog(card) },
                        onDeleteClick = { viewModel.requestDeleteCard(card) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (uiState.showAddEditDialog) {
        AddEditCreditCardDialog(
            uiState = uiState,
            onDismiss = { viewModel.dismissAddEditDialog() },
            onSave = { viewModel.saveCard() },
            onNameChange = { viewModel.onNameChanged(it) },
            onBankChange = { viewModel.onBankChanged(it) },
            onLast4Change = { viewModel.onLast4Changed(it) },
            onLimitChange = { viewModel.onLimitChanged(it) },
            onBalanceChange = { viewModel.onBalanceChanged(it) },
            onCycleChange = { viewModel.onBillingCycleChanged(it) },
            onDueChange = { viewModel.onDueDayChanged(it) },
            onAccountSelect = { viewModel.onLinkedAccountChanged(it) },
            onColorSelect = { viewModel.onCardColorChanged(it) }
        )
    }

    // Record Payment Dialog
    if (uiState.showPaymentDialog) {
        RecordCreditCardPaymentDialog(
            uiState = uiState,
            onDismiss = { viewModel.dismissPaymentDialog() },
            onConfirm = { viewModel.recordPayment() },
            onAmountChange = { viewModel.onPaymentAmountChanged(it) },
            onAccountSelect = { viewModel.onPaymentSourceAccountChanged(it) },
            onNoteChange = { viewModel.onPaymentNoteChanged(it) }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.cardToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("Delete Credit Card", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Are you sure you want to delete ${uiState.cardToDelete?.name}? Existing transactions associated with this card will remain.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteCard() },
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
fun CreditOverviewCard(
    totalLimit: Double,
    totalOutstanding: Double,
    totalAvailable: Double,
    utilization: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("credit_overview_card"),
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
                    text = "Total credit debt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Utilization badge
                val utilColor = when {
                    utilization < 30.0 -> IncomeGreen
                    utilization < 60.0 -> Color(0xFFF59E0B)
                    else -> ExpenseRed
                }
                val utilText = when {
                    utilization < 30.0 -> "Optimal (<30%)"
                    utilization < 60.0 -> "Moderate"
                    else -> "High (>60%)"
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(utilColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(utilColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$utilText • ${String.format(Locale.getDefault(), "%.1f%%", utilization)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = utilColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = CurrencyFormatter.format(totalOutstanding, currency),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp).asAmount(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Total Limit vs Available
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
                    Text("Available Credit", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        CurrencyFormatter.format(totalAvailable, currency),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                        color = IncomeGreen
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text("Combined Limit", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        CurrencyFormatter.format(totalLimit, currency),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CreditCardItem(
    card: CreditCard,
    onPayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val baseColor = try {
        Color(android.graphics.Color.parseColor(card.cardColorHex))
    } catch (e: Exception) {
        Color(0xFF1E293B)
    }

    val utilPercent = card.utilizationPercentage
    val utilColor = when {
        utilPercent < 30.0 -> IncomeGreen
        utilPercent < 60.0 -> Color(0xFFF59E0B)
        else -> ExpenseRed
    }

    Card(
        modifier = modifier.testTag("credit_card_item_${card.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Visual Card Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(baseColor, baseColor.copy(alpha = 0.75f), Color(0xFF0F172A))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = card.bankName.uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = card.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        // EMV Chip Simulation
                        Box(
                            modifier = Modifier
                                .size(width = 34.dp, height = 24.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFD97706).copy(alpha = 0.85f))
                                .border(BorderStroke(0.5.dp, Color(0xFFFDE68A)), RoundedCornerShape(5.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = card.cardNumberMasked,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = com.example.ui.theme.InterFamily,
                                letterSpacing = 2.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Text(
                            text = "DUE: ${card.paymentDueDay}TH",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Card Balance Details & Utilization Bar
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
                    Column {
                        Text(
                            text = "Outstanding Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(card.outstandingBalance, card.currency),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                            color = if (card.outstandingBalance > 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Available Limit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(card.availableCredit, card.currency),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).asAmount(),
                            color = IncomeGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Utilization Progress Bar
                val progress = (utilPercent / 100.0).toFloat().coerceIn(0f, 1f)
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
                                .background(utilColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Limit: ${CurrencyFormatter.format(card.creditLimit, card.currency)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f%%", utilPercent)} used",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                        color = utilColor
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("pay_card_button_${card.id}")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay Card Bill", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Details", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Card", color = ExpenseRed) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCreditCardDialog(
    uiState: CreditCardsUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onBankChange: (String) -> Unit,
    onLast4Change: (String) -> Unit,
    onLimitChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onCycleChange: (String) -> Unit,
    onDueChange: (String) -> Unit,
    onAccountSelect: (Long?) -> Unit,
    onColorSelect: (String) -> Unit
) {
    val isEditing = uiState.editingCard != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Credit Card" else "Add Credit Card",
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
                    label = { Text("Card Name (e.g. Regalia Gold)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("card_name_input")
                )

                OutlinedTextField(
                    value = uiState.bankInput,
                    onValueChange = onBankChange,
                    label = { Text("Bank / Issuer Name (e.g. HDFC)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("card_bank_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.last4Input,
                        onValueChange = onLast4Change,
                        label = { Text("Last 4 Digits") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("card_last4_input")
                    )

                    OutlinedTextField(
                        value = uiState.limitInput,
                        onValueChange = onLimitChange,
                        label = { Text("Limit (${uiState.currency})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("card_limit_input")
                    )
                }

                OutlinedTextField(
                    value = uiState.balanceInput,
                    onValueChange = onBalanceChange,
                    label = { Text("Current Outstanding Balance (${uiState.currency})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("card_balance_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.billingCycleInput,
                        onValueChange = onCycleChange,
                        label = { Text("Cycle Day (1-31)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.dueDayInput,
                        onValueChange = onDueChange,
                        label = { Text("Due Day (1-31)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Card Color Preset Picker
                Text("Card Theme Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                val cardColors = listOf("#1E293B", "#065F46", "#1E3A8A", "#4C1D95", "#831843", "#7C2D12", "#0F172A")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    cardColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = uiState.cardColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    BorderStroke(if (isSelected) 3.dp else 1.dp, if (isSelected) EmeraldPrimary else Color.Transparent),
                                    CircleShape
                                )
                                .clickable { onColorSelect(hex) }
                        )
                    }
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
                modifier = Modifier.testTag("save_credit_card_button")
            ) {
                Text("Save Card", fontWeight = FontWeight.Bold)
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
fun RecordCreditCardPaymentDialog(
    uiState: CreditCardsUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onAmountChange: (String) -> Unit,
    onAccountSelect: (Long) -> Unit,
    onNoteChange: (String) -> Unit
) {
    val card = uiState.payingCard ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pay ${card.name}",
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
                    text = "Current outstanding: ${CurrencyFormatter.format(card.outstandingBalance, card.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = uiState.paymentAmountInput,
                    onValueChange = onAmountChange,
                    label = { Text("Payment Amount (${card.currency})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                Text("Pay From Bank Account", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
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
                    label = { Text("Note / Reference") },
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
                modifier = Modifier.testTag("confirm_payment_button")
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
