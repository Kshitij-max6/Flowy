package com.example.feature.transactions

import androidx.compose.animation.AnimatedVisibility
import com.example.ui.theme.asAmount
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.AppleIconButton
import com.example.core.ui.ConfirmDeleteDialog
import com.example.core.ui.EmptyState
import com.example.core.ui.TransactionItemRow
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.SavedTransactionFilter
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.financialAmountStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDetailItem by remember { mutableStateOf<TransactionDetail?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("transactions_screen")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.activeFilterCount > 0) {
                    AppleIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Clear All",
                        onClick = { viewModel.clearAllFilters() }
                    )
                }
            }
            if (uiState.totalIncome > 0 || uiState.totalExpense > 0) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.totalIncome > 0) {
                        Text(
                            text = "+${CurrencyFormatter.format(uiState.totalIncome, uiState.defaultCurrency)}",
                            style = MaterialTheme.typography.bodyMedium.asAmount(),
                            color = IncomeGreen
                        )
                    }
                    if (uiState.totalExpense > 0) {
                        Text(
                            text = "\u2212${CurrencyFormatter.format(uiState.totalExpense, uiState.defaultCurrency)}",
                            style = MaterialTheme.typography.bodyMedium.asAmount(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Search Bar & Filter Button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search note, category, account...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    cursorColor = EmeraldPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("transactions_search_input")
            )

            // Advanced Filters Trigger
            val hasActiveFilters = uiState.activeFilterCount > 0
            IconButton(
                onClick = { viewModel.openFilterSheet() },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (hasActiveFilters) EmeraldPrimary else MaterialTheme.colorScheme.surface)
                    .testTag("open_filters_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "Filter",
                        tint = if (hasActiveFilters) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasActiveFilters) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.activeFilterCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }

        // Saved Filter Presets Row
        if (uiState.savedFilters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRESETS:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                uiState.savedFilters.forEach { preset ->
                    val isSelected = uiState.activeSavedFilterId == preset.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) viewModel.clearAllFilters() else viewModel.applySavedFilter(preset)
                        },
                        label = {
                            Text(
                                text = preset.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Preset",
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { viewModel.deleteSavedFilter(preset) }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            selectedTrailingIconColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Quick Filter Chips Row (Type & Date)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.selectedType == null && uiState.selectedDateFilter == DateFilterType.ALL,
                onClick = { viewModel.clearAllFilters() },
                label = { Text("All", fontWeight = if (uiState.selectedType == null && uiState.selectedDateFilter == DateFilterType.ALL) FontWeight.Bold else FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, if (uiState.selectedType == null && uiState.selectedDateFilter == DateFilterType.ALL) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = uiState.selectedType == TransactionType.EXPENSE,
                onClick = {
                    viewModel.onTypeFilterSelected(
                        if (uiState.selectedType == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                    )
                },
                label = { Text("Expenses", fontWeight = if (uiState.selectedType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, if (uiState.selectedType == TransactionType.EXPENSE) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = uiState.selectedType == TransactionType.INCOME,
                onClick = {
                    viewModel.onTypeFilterSelected(
                        if (uiState.selectedType == TransactionType.INCOME) null else TransactionType.INCOME
                    )
                },
                label = { Text("Income", fontWeight = if (uiState.selectedType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, if (uiState.selectedType == TransactionType.INCOME) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = uiState.selectedDateFilter == DateFilterType.THIS_MONTH,
                onClick = {
                    viewModel.onDateFilterSelected(
                        if (uiState.selectedDateFilter == DateFilterType.THIS_MONTH) DateFilterType.ALL else DateFilterType.THIS_MONTH
                    )
                },
                label = { Text("This Month") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, if (uiState.selectedDateFilter == DateFilterType.THIS_MONTH) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = uiState.selectedDateFilter == DateFilterType.LAST_MONTH,
                onClick = {
                    viewModel.onDateFilterSelected(
                        if (uiState.selectedDateFilter == DateFilterType.LAST_MONTH) DateFilterType.ALL else DateFilterType.LAST_MONTH
                    )
                },
                label = { Text("Last Month") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, if (uiState.selectedDateFilter == DateFilterType.LAST_MONTH) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Transactions List Grouped by Date
        if (uiState.filteredGroupedTransactions.isEmpty()) {
            EmptyState(
                title = "No transactions found",
                subtitle = "Try clearing filters or search query.",
                icon = Icons.Outlined.ReceiptLong,
                modifier = Modifier.padding(top = 48.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                uiState.filteredGroupedTransactions.forEach { (dateGroup, itemsForDate) ->
                    item(key = "header_$dateGroup") {
                        val dayExpense = itemsForDate
                            .filter { it.transaction.type == TransactionType.EXPENSE }
                            .sumOf { it.transaction.amount }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateGroup,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (dayExpense > 0) {
                                Text(
                                    text = "-${CurrencyFormatter.format(dayExpense, uiState.defaultCurrency)}",
                                    style = financialAmountStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    items(
                        items = itemsForDate,
                        key = { "tx_${it.transaction.id}" }
                    ) { item ->
                        TransactionItemRow(
                            item = item,
                            currencySymbol = uiState.defaultCurrency,
                            onClick = { selectedDetailItem = item }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Advanced Filter Options
    if (uiState.showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissFilterSheet() },
            sheetState = filterSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        ) {
            AdvancedFilterBottomSheetContent(
                uiState = uiState,
                onTypeSelect = { viewModel.onTypeFilterSelected(it) },
                onDateFilterSelect = { viewModel.onDateFilterSelected(it) },
                onCategorySelect = { viewModel.onCategoryFilterSelected(it) },
                onAccountSelect = { viewModel.onAccountFilterSelected(it) },
                onMinAmountChange = { min -> viewModel.onAmountRangeSelected(min, uiState.maxAmount) },
                onMaxAmountChange = { max -> viewModel.onAmountRangeSelected(uiState.minAmount, max) },
                onSortOrderSelect = { viewModel.onSortOrderChanged(it) },
                onReset = { viewModel.clearAllFilters() },
                onSavePresetClick = {
                    viewModel.dismissFilterSheet()
                    viewModel.openSaveFilterDialog()
                },
                onApply = { viewModel.dismissFilterSheet() }
            )
        }
    }

    // Save Preset Dialog
    if (uiState.showSaveFilterDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveFilterDialog() },
            title = { Text("Save Filter Preset", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Give this filter configuration a name for quick 1-tap access.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = uiState.saveFilterNameInput,
                        onValueChange = { viewModel.onSaveFilterNameChanged(it) },
                        placeholder = { Text("e.g. Groceries > 1k") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("preset_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveCurrentFilterPreset() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_save_preset_button")
                ) {
                    Text("Save Preset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSaveFilterDialog() }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modal Bottom Sheet for Transaction Details & Actions (Edit / Delete)
    selectedDetailItem?.let { detail ->
        ModalBottomSheet(
            onDismissRequest = { selectedDetailItem = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (detail.transaction.note.isNotBlank()) detail.transaction.note else (detail.category?.name ?: "Transaction"),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = CurrencyFormatter.format(detail.transaction.amount, uiState.defaultCurrency),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (detail.transaction.type) {
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.TRANSFER -> EmeraldPrimary
                        }
                    ).asAmount()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Type", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(detail.transaction.type.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                        if (detail.category != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Category", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(detail.category.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Account", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(detail.sourceAccount?.name ?: "Account", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                        if (detail.destinationAccount != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("To Account", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(detail.destinationAccount.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Date & Time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(com.example.core.util.DateUtils.formatFullDateTime(detail.transaction.dateTime), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Edit & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                val txId = detail.transaction.id
                                selectedDetailItem = null
                                onEditTransaction(txId)
                            }
                            .testTag("edit_transaction_action"),
                        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                val itemToDelete = detail
                                selectedDetailItem = null
                                viewModel.promptDeleteTransaction(itemToDelete)
                            }
                            .testTag("delete_transaction_action"),
                        colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete", fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }
                    }
                }
            }
        }
    }

    // Confirm Delete Dialog
    if (uiState.transactionToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Transaction",
            message = "Are you sure you want to delete this ${uiState.transactionToDelete?.transaction?.type?.name?.lowercase()} of ${CurrencyFormatter.format(uiState.transactionToDelete?.transaction?.amount ?: 0.0, uiState.defaultCurrency)}? Account balance will be restored.",
            onConfirm = { viewModel.confirmDeleteTransaction() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
}

@Composable
fun AdvancedFilterBottomSheetContent(
    uiState: TransactionsUiState,
    onTypeSelect: (TransactionType?) -> Unit,
    onDateFilterSelect: (DateFilterType) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    onAccountSelect: (Long?) -> Unit,
    onMinAmountChange: (Double?) -> Unit,
    onMaxAmountChange: (Double?) -> Unit,
    onSortOrderSelect: (SortOrder) -> Unit,
    onReset: () -> Unit,
    onSavePresetClick: () -> Unit,
    onApply: () -> Unit
) {
    var minText by remember { mutableStateOf(uiState.minAmount?.toString() ?: "") }
    var maxText by remember { mutableStateOf(uiState.maxAmount?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter Transactions",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(onClick = onReset) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = ExpenseRed)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset All", color = ExpenseRed, fontWeight = FontWeight.SemiBold)
            }
        }

        // Transaction Type
        Text("Transaction Type", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedType == null,
                onClick = { onTypeSelect(null) },
                label = { Text("All Types") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary, selectedLabelColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            )
            TransactionType.values().forEach { type ->
                val isSelected = uiState.selectedType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelect(if (isSelected) null else type) },
                    label = { Text(type.name) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary, selectedLabelColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Date Range
        Text("Date Range", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateFilterType.values().forEach { df ->
                val isSelected = uiState.selectedDateFilter == df
                FilterChip(
                    selected = isSelected,
                    onClick = { onDateFilterSelect(df) },
                    label = { Text(df.name.replace("_", " ")) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary, selectedLabelColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Amount Range (Min - Max)
        Text("Amount Range (${uiState.defaultCurrency})", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = minText,
                onValueChange = {
                    minText = it
                    onMinAmountChange(it.toDoubleOrNull())
                },
                label = { Text("Min Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                modifier = Modifier.weight(1f).testTag("filter_min_amount")
            )

            OutlinedTextField(
                value = maxText,
                onValueChange = {
                    maxText = it
                    onMaxAmountChange(it.toDoubleOrNull())
                },
                label = { Text("Max Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                modifier = Modifier.weight(1f).testTag("filter_max_amount")
            )
        }

        // Account Filter
        Text("Account", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedAccountId == null,
                onClick = { onAccountSelect(null) },
                label = { Text("All Accounts") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary, selectedLabelColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            )
            uiState.accounts.forEach { acc ->
                val isSelected = uiState.selectedAccountId == acc.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onAccountSelect(if (isSelected) null else acc.id) },
                    label = { Text(acc.name) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary, selectedLabelColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Category Filter
        Text("Category", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All Categories") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary, selectedLabelColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            )
            uiState.categories.forEach { cat ->
                val isSelected = uiState.selectedCategoryId == cat.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(if (isSelected) null else cat.id) },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary, selectedLabelColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Sort Order
        Text("Sort Order", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortOrder.values().forEach { so ->
                val isSelected = uiState.sortOrder == so
                val label = when (so) {
                    SortOrder.DATE_DESC -> "Newest First"
                    SortOrder.DATE_ASC -> "Oldest First"
                    SortOrder.AMOUNT_DESC -> "Highest Amount"
                    SortOrder.AMOUNT_ASC -> "Lowest Amount"
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onSortOrderSelect(so) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary, selectedLabelColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Save preset & Apply Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSavePresetClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Preset", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply Filters", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
