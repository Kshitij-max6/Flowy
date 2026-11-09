package com.example.feature.books

import androidx.compose.animation.AnimatedVisibility
import com.example.ui.theme.asAmount
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Wallet
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.ExpenseBook
import com.example.domain.model.ExpenseBookSummary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PrestigeGold
import com.example.ui.theme.RoyalGold

private val BOOK_COLORS = listOf(
    "#E5C378", // Gold
    "#38BDF8", // Sky Blue
    "#00E599", // Mint Emerald
    "#A78BFA", // Purple
    "#F43F5E", // Rose Coral
    "#FB923C", // Sunset Orange
    "#34D399", // Teal
    "#818CF8", // Indigo
    "#FBBF24", // Amber
    "#94A3B8"  // Slate
)

private val BOOK_ICONS = listOf(
    "Wallet" to Icons.Default.Wallet,
    "Flight" to Icons.Default.Flight,
    "Home" to Icons.Default.Home,
    "Work" to Icons.Default.BusinessCenter,
    "Shopping" to Icons.Default.ShoppingCart,
    "Car" to Icons.Default.DirectionsCar,
    "Health" to Icons.Default.Favorite,
    "School" to Icons.Default.School,
    "Star" to Icons.Default.Star,
    "Gift" to Icons.Default.CardGiftcard
)

fun getBookIcon(name: String): ImageVector {
    return BOOK_ICONS.find { it.first.equals(name, ignoreCase = true) }?.second ?: Icons.Default.MenuBook
}

fun parseColorSafe(hex: String, fallback: Color = RoyalGold): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseBooksScreen(
    viewModel: ExpenseBooksViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBookDetail: (Long) -> Unit,
    onNavigateToAddTransactionWithBook: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currency = uiState.userSettings?.defaultCurrency ?: "₹"

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var bookToEdit by remember { mutableStateOf<ExpenseBook?>(null) }
    var bookToDelete by remember { mutableStateOf<ExpenseBook?>(null) }

    val activeSummaries = uiState.summaries.filter { !it.book.isArchived }
    val archivedSummaries = uiState.summaries.filter { it.book.isArchived }

    Scaffold(
        modifier = Modifier.testTag("expense_books_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Expense Books",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        )
                        Text(
                            text = "Separate ledgers for trips, projects & business",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("expense_books_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    bookToEdit = null
                    showAddEditDialog = true
                },
                containerColor = RoyalGold,
                contentColor = ObsidianBlack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_expense_book_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Book")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Book",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Stats Banner
            val totalBudget = activeSummaries.sumOf { it.book.budget }
            val totalSpent = activeSummaries.sumOf { it.totalExpense }
            val totalBooksCount = activeSummaries.size

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("books_summary_banner"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total active ledgers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalBooksCount Books",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Active spending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(totalSpent, currency),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RoyalGold
                            ).asAmount()
                        )
                        if (totalBudget > 0) {
                            Text(
                                text = "of ${CurrencyFormatter.format(totalBudget, currency)} budget",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tabs (Active / Archived)
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active (${activeSummaries.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Archived (${archivedSummaries.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            val currentList = if (selectedTab == 0) activeSummaries else archivedSummaries

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) "No active expense books" else "No archived books",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedTab == 0) "Tap + New Book to organize your trips, projects or business expenses" else "Archived ledgers will appear here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList, key = { it.book.id }) { summary ->
                        ExpenseBookCard(
                            summary = summary,
                            currency = currency,
                            onClick = { onNavigateToBookDetail(summary.book.id) },
                            onEdit = {
                                bookToEdit = summary.book
                                showAddEditDialog = true
                            },
                            onDelete = {
                                bookToDelete = summary.book
                            },
                            onToggleArchive = { viewModel.toggleArchive(summary.book) },
                            onSetDefault = { viewModel.setDefaultBook(summary.book.id) },
                            onQuickAddTransaction = { onNavigateToAddTransactionWithBook(summary.book.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditExpenseBookDialog(
            book = bookToEdit,
            currency = currency,
            onDismiss = {
                showAddEditDialog = false
                bookToEdit = null
            },
            onSave = { name, desc, iconName, colorHex, budget, isDef ->
                if (bookToEdit == null) {
                    viewModel.addBook(name, desc, iconName, colorHex, budget, isDef)
                } else {
                    viewModel.updateBook(
                        bookToEdit!!.copy(
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
                }
                showAddEditDialog = false
                bookToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Expense Book") },
            text = { Text("Are you sure you want to delete '${book.name}'? Existing transactions will remain in the database but will be detached from this book.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBook(book)
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExpenseBookCard(
    summary: ExpenseBookSummary,
    currency: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleArchive: () -> Unit,
    onSetDefault: () -> Unit,
    onQuickAddTransaction: () -> Unit
) {
    val book = summary.book
    val accentColor = parseColorSafe(book.colorHex)
    val icon = getBookIcon(book.iconName)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("expense_book_card_${book.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = book.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                                maxLines = 1
                            )
                            if (book.isDefault) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = RoyalGold.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "DEFAULT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalGold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (book.description.isNotBlank()) {
                            Text(
                                text = book.description,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("book_menu_button_${book.id}")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Quick Add Expense") },
                            onClick = {
                                showMenu = false
                                onQuickAddTransaction()
                            },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                        if (!book.isDefault && !book.isArchived) {
                            DropdownMenuItem(
                                text = { Text("Set as Default") },
                                onClick = {
                                    showMenu = false
                                    onSetDefault()
                                },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Edit Book") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (book.isArchived) "Unarchive" else "Archive") },
                            onClick = {
                                showMenu = false
                                onToggleArchive()
                            },
                            leadingIcon = {
                                Icon(
                                    if (book.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null
                                )
                            }
                        )
                        if (!book.isDefault) {
                            DropdownMenuItem(
                                text = { Text("Delete Book", color = ExpenseRed) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Spending & Budget Numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Total Spent",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(summary.totalExpense, currency),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ).asAmount()
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (book.budget > 0) {
                        val pct = ((summary.totalExpense / book.budget) * 100).toInt()
                        Text(
                            text = "Budget: ${CurrencyFormatter.format(book.budget, currency)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$pct% used",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (pct > 100) ExpenseRed else if (pct > 85) PrestigeGold else IncomeGreen
                            )
                        )
                    } else {
                        Text(
                            text = "${summary.transactionCount} transactions",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Budget Progress Bar if budget set
            if (book.budget > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val progress = (summary.totalExpense / book.budget).toFloat().coerceIn(0f, 1f)
                val barColor = if (summary.totalExpense > book.budget) ExpenseRed else accentColor
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseBookDialog(
    book: ExpenseBook?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, iconName: String, colorHex: String, budget: Double, isDefault: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(book?.name ?: "") }
    var description by remember { mutableStateOf(book?.description ?: "") }
    var budgetText by remember { mutableStateOf(if (book != null && book.budget > 0) book.budget.toInt().toString() else "") }
    var selectedIcon by remember { mutableStateOf(book?.iconName ?: "Wallet") }
    var selectedColor by remember { mutableStateOf(book?.colorHex ?: "#E5C378") }
    var isDefault by remember { mutableStateOf(book?.isDefault ?: false) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (book == null) "Create Expense Book" else "Edit Expense Book",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Book Name *") },
                    placeholder = { Text("e.g. Goa Trip, Renovation, Freelance") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Name is required") } } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("book_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Flight, hotel & dinner ledger") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("book_description_input")
                )

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("Spending Budget ($currency) (Optional)") },
                    placeholder = { Text("e.g. 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("book_budget_input")
                )

                // Icon Picker
                Text(
                    text = "SELECT ICON",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BOOK_ICONS.forEach { (iconKey, vector) ->
                        val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) RoyalGold else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIcon = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                vector,
                                contentDescription = iconKey,
                                tint = if (isSelected) ObsidianBlack else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Color Picker
                Text(
                    text = "THEME COLOR",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BOOK_COLORS.forEach { hex ->
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        val color = parseColorSafe(hex)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = ObsidianBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Set as Default Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Set as Default Book",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Auto-selected for new transactions",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBlack,
                            checkedTrackColor = RoyalGold
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val budget = budgetText.toDoubleOrNull() ?: 0.0
                    onSave(name, description, selectedIcon, selectedColor, budget, isDefault)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = ObsidianBlack),
                modifier = Modifier.testTag("save_expense_book_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
