package com.example.feature.home

import androidx.compose.foundation.background
import com.example.ui.theme.asAmount
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.AppleIconButton
import com.example.core.ui.EmptyState
import com.example.core.ui.FlowlyDimens
import com.example.core.ui.SectionHeader
import com.example.core.ui.SpendingRingHero
import com.example.core.ui.TransactionItemRow
import com.example.core.ui.UpcomingPaymentCard
import com.example.core.util.CategoryIconHelper
import com.example.core.util.CurrencyFormatter
import com.example.ui.theme.SystemYellow
import com.example.ui.theme.pressable
import com.example.ui.theme.revealOnAppear
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCustomizeHome: () -> Unit = {},
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // The entrance plays once per session; scrolling back never replays it.
    var introDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(900)
        introDone = true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        // 1. Greeting + toolbar
        item(key = "header_greeting") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = FlowlyDimens.ScreenMargin, end = 8.dp, top = 12.dp, bottom = 4.dp)
                    .revealOnAppear(0, enabled = !introDone),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = uiState.userName,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("user_name_greeting")
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppleIconButton(
                        icon = Icons.Default.Tune,
                        contentDescription = "Customize Home",
                        onClick = onNavigateToCustomizeHome,
                        filled = false,
                        modifier = Modifier.testTag("home_customize_button")
                    )
                    AppleIconButton(
                        icon = if (uiState.hideAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Privacy",
                        onClick = { viewModel.togglePrivacyMode() },
                        active = uiState.hideAmounts,
                        filled = false,
                        modifier = Modifier.testTag("home_privacy_toggle_button")
                    )
                    AppleIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Global Search",
                        onClick = onNavigateToSearch,
                        filled = false,
                        modifier = Modifier.testTag("home_search_button")
                    )
                    AppleIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        onClick = onNavigateToSettings,
                        filled = false,
                        modifier = Modifier.testTag("home_settings_button")
                    )
                }
            }
        }

        // Dynamic section dispatcher (order and visibility are user-controlled)
        uiState.sectionsOrder.forEach { sectionId ->
            if (!uiState.hiddenSections.contains(sectionId)) {
                when (sectionId) {
                    "spending" -> {
                        item(key = "section_spending") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp, bottom = 12.dp, start = FlowlyDimens.ScreenMargin, end = FlowlyDimens.ScreenMargin)
                                    .revealOnAppear(1, enabled = !introDone),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SpendingRingHero(
                                    todaySpending = uiState.todaySpending,
                                    dailyBudget = uiState.dailyBudget,
                                    currencySymbol = uiState.defaultCurrency,
                                    size = 224.dp,
                                    strokeWidth = 14.dp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Budget context: a quiet gray capsule
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .pressable(pressedScale = 0.97f, role = Role.Button, onClick = onNavigateToBudgets)
                                ) {
                                    Text(
                                        text = "${CurrencyFormatter.format(uiState.todaySpending, uiState.defaultCurrency)} of ${CurrencyFormatter.format(uiState.dailyBudget, uiState.defaultCurrency)} daily budget",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                            .testTag("daily_budget_context")
                                    )
                                }
                            }
                        }
                    }

                    "accounts" -> {
                        if (uiState.accounts.isNotEmpty()) {
                            item(key = "section_accounts_header") {
                                SectionHeader(
                                    title = "Accounts",
                                    actionTitle = "Manage",
                                    onActionClick = onNavigateToAccounts,
                                    modifier = Modifier.revealOnAppear(2, enabled = !introDone)
                                )
                            }

                            item(key = "section_accounts_carousel") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = FlowlyDimens.ScreenMargin, vertical = 4.dp)
                                        .revealOnAppear(3, enabled = !introDone),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    uiState.accounts.forEach { account ->
                                        Surface(
                                            shape = RoundedCornerShape(FlowlyDimens.CardRadius),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(FlowlyDimens.CardRadius))
                                                .pressable(pressedScale = 0.97f, role = Role.Button, onClick = onNavigateToAccounts)
                                                .testTag("home_account_${account.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .width(160.dp)
                                                    .padding(16.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = CategoryIconHelper.getIconByName(account.iconName),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(14.dp))
                                                Text(
                                                    text = account.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = CurrencyFormatter.format(account.balance, uiState.defaultCurrency),
                                                    style = MaterialTheme.typography.titleMedium.asAmount(),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "recent" -> {
                        item(key = "section_recent_header") {
                            SectionHeader(
                                title = "Recent",
                                actionTitle = if (uiState.recentTransactions.isNotEmpty()) "See All" else null,
                                onActionClick = onNavigateToTransactions
                            )
                        }

                        if (uiState.recentTransactions.isEmpty()) {
                            item(key = "empty_recent_tx") {
                                EmptyState(
                                    title = "No Transactions Today",
                                    subtitle = "Tap + to record your first one.",
                                    icon = Icons.Outlined.ReceiptLong
                                )
                            }
                        } else {
                            items(
                                items = uiState.recentTransactions,
                                key = { "tx_${it.transaction.id}" }
                            ) { item ->
                                TransactionItemRow(
                                    item = item,
                                    currencySymbol = uiState.defaultCurrency,
                                    onClick = { onTransactionClick(item.transaction.id) }
                                )
                            }
                        }
                    }

                    "upcoming" -> {
                        if (uiState.upcomingRecurring.isNotEmpty()) {
                            item(key = "header_upcoming_section") {
                                SectionHeader(
                                    title = "Upcoming",
                                    actionTitle = "View All",
                                    onActionClick = onNavigateToRecurring
                                )
                            }

                            items(
                                items = uiState.upcomingRecurring,
                                key = { "rec_${it.recurring.id}" }
                            ) { upcomingItem ->
                                Box(modifier = Modifier.padding(horizontal = FlowlyDimens.ScreenMargin, vertical = 4.dp)) {
                                    UpcomingPaymentCard(
                                        item = upcomingItem,
                                        currencySymbol = uiState.defaultCurrency,
                                        onClick = onNavigateToRecurring
                                    )
                                }
                            }
                        }
                    }

                    "insight" -> {
                        uiState.dailyInsight?.let { insight ->
                            item(key = "section_insight") {
                                SectionHeader(title = "Daily Insight")
                                Surface(
                                    shape = RoundedCornerShape(FlowlyDimens.CardRadius),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = FlowlyDimens.ScreenMargin, vertical = 4.dp)
                                        .testTag("home_daily_insight_card")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = SystemYellow,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = insight.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = insight.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
