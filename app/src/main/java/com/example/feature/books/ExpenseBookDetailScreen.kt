package com.example.feature.books

import androidx.compose.animation.AnimatedVisibility
import com.example.ui.theme.asAmount
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.CategoryIconHelper
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.ExpenseBook
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PrestigeGold
import com.example.ui.theme.RoyalGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseBookDetailScreen(
    bookId: Long,
    viewModel: ExpenseBooksViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToAddTransaction: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactionsFlow = remember(bookId) { viewModel.getTransactionsForBook(bookId) }
    val transactions by transactionsFlow.collectAsState(initial = emptyList())

    val summary = uiState.summaries.find { it.book.id == bookId }
    val book = summary?.book
    val currency = uiState.userSettings?.defaultCurrency ?: "₹"

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val accentColor = book?.let { parseColorSafe(it.colorHex) } ?: RoyalGold
    val icon = book?.let { getBookIcon(it.iconName) } ?: Icons.Default.MenuBook

    val totalExpense = transactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
    val totalIncome = transactions.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }

    // Category breakdown
    val categoryTotals = remember(transactions) {
        transactions.filter { it.transaction.type == TransactionType.EXPENSE }
            .groupBy { it.category?.name ?: "Other" }
            .mapValues { entry -> entry.value.sumOf { it.transaction.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        modifier = Modifier.testTag("expense_book_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = book?.name ?: "Expense Book",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("book_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Book") },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        book?.let { b ->
                            if (!b.isDefault) {
                                DropdownMenuItem(
                                    text = { Text("Set as Default") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.setDefaultBook(b.id)
                                    },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (b.isArchived) "Unarchive" else "Archive") },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleArchive(b)
                                },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                            )
                            if (!b.isDefault) {
                                DropdownMenuItem(
                                    text = { Text("Delete Book", color = ExpenseRed) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed) }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddTransaction(bookId) },
                containerColor = accentColor,
                contentColor = ObsidianBlack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("book_add_transaction_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Entry", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    ) { paddingValues ->
        if (book == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Expense Book not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    // Header Overview Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("book_detail_overview_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = book.name,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    )
                                    if (book.description.isNotBlank()) {
                                        Text(
                                            text = book.description,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Total Spent vs Income
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total spent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(totalExpense, currency),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp).asAmount(),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (totalIncome > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Income / credits",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(totalIncome, currency),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp).asAmount(),
                                            color = IncomeGreen
                                        )
                                    }
                                }
                            }

                            // Budget bar if budget set
                            if (book.budget > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val progress = (totalExpense / book.budget).toFloat().coerceIn(0f, 1f)
                                val pct = ((totalExpense / book.budget) * 100).toInt()
                                val barColor = if (totalExpense > book.budget) ExpenseRed else accentColor

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Budget: ${CurrencyFormatter.format(book.budget, currency)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$pct% utilized",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (pct > 100) ExpenseRed else if (pct > 80) PrestigeGold else IncomeGreen
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }

                // Category Spending Breakdown in this Book
                if (categoryTotals.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Category breakdown",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                categoryTotals.take(4).forEach { (catName, amount) ->
                                    val catPct = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = catName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            )
                                            Text(
                                                text = "${CurrencyFormatter.format(amount, currency)} (${(catPct * 100).toInt()}%)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        LinearProgressIndicator(
                                            progress = { catPct },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = accentColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Transactions Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transactions (${transactions.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        )
                    }
                }

                if (transactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No transactions in this book yet",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    items(transactions, key = { it.transaction.id }) { item ->
                        val tx = item.transaction
                        val cat = item.category
                        val isExp = tx.type == TransactionType.EXPENSE
                        val txColor = if (isExp) ExpenseRed else IncomeGreen

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToEditTransaction(tx.id) }
                                .testTag("book_transaction_item_${tx.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                cat?.let { parseColorSafe(it.colorHex) }?.copy(alpha = 0.18f) ?: RoyalGold.copy(alpha = 0.18f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            CategoryIconHelper.getIcon(cat?.iconName ?: "Payment"),
                                            contentDescription = null,
                                            tint = cat?.let { parseColorSafe(it.colorHex) } ?: RoyalGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tx.note.ifBlank { cat?.name ?: "Transaction" },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(tx.dateTime))} • ${item.sourceAccount?.name ?: "Account"}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = (if (isExp) "- " else "+ ") + CurrencyFormatter.format(tx.amount, currency),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = txColor
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog && book != null) {
        AddEditExpenseBookDialog(
            book = book,
            currency = currency,
            onDismiss = { showEditDialog = false },
            onSave = { name, desc, iconName, colorHex, budget, isDef ->
                viewModel.updateBook(
                    book.copy(
                        name = name,
                        description = desc,
                        iconName = iconName,
                        colorHex = colorHex,
                        budget = budget,
                        isDefault = isDef,
                        updatedAt = System.currentTimeMillis()
                    ),
                    setAsDefault = isDef
                )
                showEditDialog = false
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog && book != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense Book") },
            text = { Text("Are you sure you want to delete '${book.name}'? Transactions will remain in the system.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBook(book)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
