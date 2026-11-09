package com.example.feature.search

import androidx.compose.animation.AnimatedVisibility
import com.example.ui.theme.asAmount
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.FlowlyCard
import com.example.core.ui.TransactionItemRow
import com.example.core.util.CategoryIconHelper
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.CreditCard
import com.example.domain.model.GlobalSearchResults
import com.example.domain.model.GoalDetail
import com.example.domain.model.Loan
import com.example.domain.model.RecurringDetail
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToCreditCards: () -> Unit,
    onNavigateToLoans: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .testTag("search_input_field"),
                        placeholder = {
                            Text(
                                text = "Search transactions, accounts, bills...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = EmeraldPrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = viewModel::clearQuery,
                                    modifier = Modifier.testTag("search_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedIndicatorColor = EmeraldPrimary,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.recordSearch(searchQuery)
                                focusManager.clearFocus()
                            }
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("search_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (searchQuery.isBlank()) {
                // Empty query view: Recent searches & Quick Search suggestions
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent searches",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                TextButton(
                                    onClick = viewModel::clearAllRecentSearches,
                                    modifier = Modifier.testTag("clear_recent_searches_button")
                                ) {
                                    Text(
                                        text = "Clear all",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }

                        items(recentSearches, key = { it.id }) { item ->
                            RecentSearchRow(
                                query = item.query,
                                onSelect = {
                                    viewModel.selectRecentSearch(item.query)
                                    focusManager.clearFocus()
                                },
                                onDelete = { viewModel.deleteRecentSearch(item.query) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowlyCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Universal Offline Search",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Instant real-time search across all your records: transaction notes, merchants, category names, bank accounts, recurring payments, credit cards, loans, and goals.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // Results view
                if (searchResults.isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No results found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "We couldn't find anything matching \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "${searchResults.totalResultCount} RESULTS FOUND",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary
                            )
                        }

                        // TRANSACTIONS
                        if (searchResults.transactions.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "Transactions",
                                    count = searchResults.transactions.size,
                                    icon = Icons.Default.ReceiptLong
                                )
                            }

                            items(searchResults.transactions, key = { "tx_${it.transaction.id}" }) { item ->
                                TransactionItemRow(
                                    item = item,
                                    currencySymbol = settings.defaultCurrency,
                                    onClick = { onNavigateToTransactionDetail(item.transaction.id) }
                                )
                            }
                        }

                        // ACCOUNTS
                        if (searchResults.accounts.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "Accounts",
                                    count = searchResults.accounts.size,
                                    icon = Icons.Default.AccountBalance
                                )
                            }

                            items(searchResults.accounts, key = { "acc_${it.id}" }) { account ->
                                AccountSearchResultItem(
                                    account = account,
                                    currencySymbol = settings.defaultCurrency,
                                    onClick = onNavigateToAccounts
                                )
                            }
                        }

                        // CATEGORIES
                        if (searchResults.categories.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "Categories",
                                    count = searchResults.categories.size,
                                    icon = Icons.Default.Category
                                )
                            }

                            items(searchResults.categories, key = { "cat_${it.id}" }) { cat ->
                                CategorySearchResultItem(category = cat)
                            }
                        }

                        // RECURRING & SUBSCRIPTIONS
                        if (searchResults.subscriptions.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "Subscriptions & recurring",
                                    count = searchResults.subscriptions.size,
                                    icon = Icons.Default.SwapHoriz
                                )
                            }

                            items(searchResults.subscriptions, key = { "rec_${it.recurring.id}" }) { rec ->
                                RecurringSearchResultItem(
                                    item = rec,
                                    currencySymbol = settings.defaultCurrency,
                                    onClick = onNavigateToRecurring
                                )
                            }
                        }

                        // GOALS
                        if (searchResults.goals.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "Financial goals",
                                    count = searchResults.goals.size,
                                    icon = Icons.Default.Flag
                                )
                            }

                            items(searchResults.goals, key = { "goal_${it.goal.id}" }) { goal ->
                                GoalSearchResultItem(
                                    goal = goal,
                                    currencySymbol = settings.defaultCurrency,
                                    onClick = onNavigateToGoals
                                )
                            }
                        }

                        // CREDIT CARDS
                        if (searchResults.creditCards.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "Credit cards",
                                    count = searchResults.creditCards.size,
                                    icon = Icons.Default.CreditCard
                                )
                            }

                            items(searchResults.creditCards, key = { "card_${it.id}" }) { card ->
                                CreditCardSearchResultItem(
                                    card = card,
                                    currencySymbol = settings.defaultCurrency,
                                    onClick = onNavigateToCreditCards
                                )
                            }
                        }

                        // LOANS
                        if (searchResults.loans.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = "Loans & borrowing",
                                    count = searchResults.loans.size,
                                    icon = Icons.Default.ReceiptLong
                                )
                            }

                            items(searchResults.loans, key = { "loan_${it.id}" }) { loan ->
                                LoanSearchResultItem(
                                    loan = loan,
                                    currencySymbol = settings.defaultCurrency,
                                    onClick = onNavigateToLoans
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSearchRow(
    query: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("recent_search_item_$query"),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SearchSectionHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EmeraldPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AccountSearchResultItem(
    account: Account,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowlyCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("account_search_item_${account.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = try {
                        Color(android.graphics.Color.parseColor(account.colorHex)).copy(alpha = 0.15f)
                    } catch (e: Exception) {
                        EmeraldPrimary.copy(alpha = 0.15f)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = CategoryIconHelper.getIconByName(account.iconName),
                            contentDescription = null,
                            tint = try {
                                Color(android.graphics.Color.parseColor(account.colorHex))
                            } catch (e: Exception) {
                                EmeraldPrimary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = account.type.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = CurrencyFormatter.format(account.balance, currencySymbol),
                style = MaterialTheme.typography.titleMedium.asAmount(),
                fontWeight = FontWeight.Bold,
                color = if (account.balance >= 0) EmeraldPrimary else ExpenseRed
            )
        }
    }
}

@Composable
fun CategorySearchResultItem(
    category: Category,
    modifier: Modifier = Modifier
) {
    FlowlyCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_search_item_${category.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = try {
                        Color(android.graphics.Color.parseColor(category.colorHex)).copy(alpha = 0.15f)
                    } catch (e: Exception) {
                        EmeraldPrimary.copy(alpha = 0.15f)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = CategoryIconHelper.getIconByName(category.iconName),
                            contentDescription = null,
                            tint = try {
                                Color(android.graphics.Color.parseColor(category.colorHex))
                            } catch (e: Exception) {
                                EmeraldPrimary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (category.type.name == "EXPENSE") ExpenseRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f)
            ) {
                Text(
                    text = category.type.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (category.type.name == "EXPENSE") ExpenseRed else IncomeGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RecurringSearchResultItem(
    item: RecurringDetail,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowlyCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recurring_search_item_${item.recurring.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = TransferCyan.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = TransferCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.recurring.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${item.recurring.frequency.name} • Due ${DateUtils.formatDate(item.recurring.nextExecutionDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = CurrencyFormatter.format(item.recurring.amount, currencySymbol),
                style = MaterialTheme.typography.titleMedium.asAmount(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GoalSearchResultItem(
    goal: GoalDetail,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = if (goal.goal.targetAmount > 0) {
        ((goal.goal.currentAmount / goal.goal.targetAmount) * 100).toInt()
    } else 0

    FlowlyCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("goal_search_item_${goal.goal.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = CategoryIconHelper.getIconByName(goal.goal.iconName),
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = goal.goal.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$pct% of ${CurrencyFormatter.format(goal.goal.targetAmount, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = CurrencyFormatter.format(goal.goal.currentAmount, currencySymbol),
                style = MaterialTheme.typography.titleMedium.asAmount(),
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )
        }
    }
}

@Composable
fun CreditCardSearchResultItem(
    card: CreditCard,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowlyCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("card_search_item_${card.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = ExpenseRed.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${card.bankName} • ${card.cardNumberMasked}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = CurrencyFormatter.format(card.outstandingBalance, currencySymbol),
                style = MaterialTheme.typography.titleMedium.asAmount(),
                fontWeight = FontWeight.Bold,
                color = ExpenseRed
            )
        }
    }
}

@Composable
fun LoanSearchResultItem(
    loan: Loan,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowlyCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("loan_search_item_${loan.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = loan.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${loan.lender} • ${loan.loanType.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = CurrencyFormatter.format(loan.remainingBalance, currencySymbol),
                style = MaterialTheme.typography.titleMedium.asAmount(),
                fontWeight = FontWeight.Bold,
                color = ExpenseRed
            )
        }
    }
}
