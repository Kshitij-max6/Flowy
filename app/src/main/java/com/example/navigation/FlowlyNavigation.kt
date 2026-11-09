package com.example.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.util.AppLockManager
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.UserSettings
import com.example.feature.accounts.AccountsScreen
import com.example.feature.accounts.AccountsViewModel
import com.example.feature.add_transaction.AddTransactionScreen
import com.example.feature.add_transaction.AddTransactionViewModel
import com.example.feature.analytics.AnalyticsScreen
import com.example.feature.analytics.AnalyticsViewModel
import com.example.feature.backup.BackupRestoreScreen
import com.example.feature.backup.BackupRestoreViewModel
import com.example.feature.books.ExpenseBookDetailScreen
import com.example.feature.books.ExpenseBooksScreen
import com.example.feature.books.ExpenseBooksViewModel
import com.example.feature.budgets.BudgetScreen
import com.example.feature.budgets.BudgetViewModel
import com.example.feature.calendar.CalendarScreen
import com.example.feature.calendar.CalendarViewModel
import com.example.feature.cashflow.CashFlowForecastScreen
import com.example.feature.cashflow.CashFlowViewModel
import com.example.feature.creditcards.CreditCardsScreen
import com.example.feature.creditcards.CreditCardsViewModel
import com.example.feature.export.ExportScreen
import com.example.feature.export.ExportViewModel
import com.example.feature.goals.GoalsScreen
import com.example.feature.goals.GoalsViewModel
import com.example.feature.home.HomeScreen
import com.example.feature.home.HomeViewModel
import com.example.feature.home.HomeCustomizeScreen
import com.example.feature.home.HomeCustomizeViewModel
import com.example.feature.insights.InsightsScreen
import com.example.feature.insights.InsightsViewModel
import com.example.feature.loans.LoanDetailScreen
import com.example.feature.loans.LoansScreen
import com.example.feature.loans.LoansViewModel
import com.example.feature.merchant.MerchantDetailScreen
import com.example.feature.merchant.MerchantDetailViewModel
import com.example.feature.networth.NetWorthScreen
import com.example.feature.networth.NetWorthViewModel
import com.example.feature.recurring.RecurringScreen
import com.example.feature.recurring.RecurringViewModel
import com.example.feature.search.SearchScreen
import com.example.feature.search.SearchViewModel
import com.example.feature.security.AppLockScreen
import com.example.feature.settings.AppearanceScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import com.example.feature.transactions.TransactionsScreen
import com.example.feature.transactions.TransactionsViewModel
import com.example.feature.voice_expense.VoiceExpenseBottomSheet
import com.example.ui.theme.FlowlyMotion
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.OnTint
import com.example.ui.theme.popEnter
import com.example.ui.theme.popExit
import com.example.ui.theme.pressable
import com.example.ui.theme.pushEnter
import com.example.ui.theme.pushExit
import com.example.ui.theme.tabEnter
import com.example.ui.theme.tabExit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowlyApp(
    repository: FlowlyRepository,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
    initialShowVoiceInput: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    val isAppLocked by AppLockManager.isLocked.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showVoiceExpenseSheet by remember { mutableStateOf(initialShowVoiceInput) }

    val categories by repository.getAllCategories().collectAsState(initial = emptyList())
    val accounts by repository.getAllAccounts().collectAsState(initial = emptyList())
    val books by repository.getActiveExpenseBooks().collectAsState(initial = emptyList())
    val userSettings by repository.getUserSettings().collectAsState(initial = UserSettings())

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Transactions.route,
        Screen.Analytics.route,
        Screen.More.route
    ) && !isAppLocked

    Box(modifier = Modifier.fillMaxSize()) {
        com.example.feature.update.AppUpdateHost()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(FlowlyMotion.standard()) { it / 2 } + fadeIn(FlowlyMotion.standard()),
                    exit = slideOutVertically(FlowlyMotion.quick()) { it / 2 } + fadeOut(FlowlyMotion.quick())
                ) {
                    FlowlyBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            when (route) {
                                "add_action" -> {
                                    navController.navigate(Screen.AddTransaction.createRoute(-1L))
                                }
                                "voice_action" -> {
                                    showVoiceExpenseSheet = true
                                }
                                else -> {
                                    navController.navigate(route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            val reduceMotion = LocalReduceMotion.current
            val tabRoutes = remember {
                setOf(Screen.Home.route, Screen.Transactions.route, Screen.Analytics.route, Screen.More.route)
            }
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                enterTransition = {
                    if (initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes) tabEnter()
                    else pushEnter(reduceMotion)
                },
                exitTransition = {
                    if (initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes) tabExit()
                    else pushExit(reduceMotion)
                },
                popEnterTransition = {
                    if (initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes) tabEnter()
                    else popEnter(reduceMotion)
                },
                popExitTransition = {
                    if (initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes) tabExit()
                    else popExit(reduceMotion)
                }
            ) {
                // 1. Home
                composable(Screen.Home.route) {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.Factory(repository)
                    )
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                        onNavigateToSettings = { navController.navigate(Screen.More.route) },
                        onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                        onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                        onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                        onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                        onNavigateToCustomizeHome = { navController.navigate(Screen.HomeCustomize.route) },
                        onTransactionClick = { txId ->
                            navController.navigate(Screen.AddTransaction.createRoute(txId))
                        }
                    )
                }

                // 2. Transactions
                composable(Screen.Transactions.route) {
                    val txViewModel: TransactionsViewModel = viewModel(
                        factory = TransactionsViewModel.Factory(repository)
                    )
                    TransactionsScreen(
                        viewModel = txViewModel,
                        onEditTransaction = { txId ->
                            navController.navigate(Screen.AddTransaction.createRoute(txId))
                        }
                    )
                }

                // 3. Add / Edit Transaction
                composable(
                    route = Screen.AddTransaction.route,
                    arguments = listOf(
                        navArgument("editId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { backStackEntry ->
                    val editId = backStackEntry.arguments?.getLong("editId") ?: -1L
                    val addViewModel: AddTransactionViewModel = viewModel(
                        factory = AddTransactionViewModel.Factory(repository, editId)
                    )
                    AddTransactionScreen(
                        viewModel = addViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 4. Analytics & Flow
                composable(Screen.Analytics.route) {
                    val analyticsViewModel: AnalyticsViewModel = viewModel(
                        factory = AnalyticsViewModel.Factory(repository)
                    )
                    AnalyticsScreen(
                        viewModel = analyticsViewModel,
                        onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                        onNavigateToMerchant = { merchantName ->
                            navController.navigate(Screen.MerchantDetail.createRoute(merchantName))
                        }
                    )
                }

                // 5. More & Settings
                composable(Screen.More.route) {
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.Factory(repository)
                    )
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                        onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                        onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                        onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                        onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                        onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                        onNavigateToNetWorth = { navController.navigate(Screen.NetWorth.route) },
                        onNavigateToCashFlow = { navController.navigate(Screen.CashFlow.route) },
                        onNavigateToCreditCards = { navController.navigate(Screen.CreditCards.route) },
                        onNavigateToLoans = { navController.navigate(Screen.Loans.route) },
                        onNavigateToExport = { navController.navigate(Screen.Export.route) },
                        onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                        onNavigateToCustomizeHome = { navController.navigate(Screen.HomeCustomize.route) },
                        onNavigateToBooks = { navController.navigate(Screen.ExpenseBooks.route) },
                        onNavigateToAppearance = { navController.navigate(Screen.Appearance.route) },
                        onNavigateToSplit = { navController.navigate(Screen.SplitFriends.route) }
                    )
                }

                // 5b. Appearance (light / dark / system)
                composable(Screen.Appearance.route) {
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.Factory(repository)
                    )
                    AppearanceScreen(
                        themeMode = userSettings.themeMode,
                        onThemeModeSelected = { settingsViewModel.setThemeMode(it) },
                        accentTheme = userSettings.prestigeTheme,
                        onAccentSelected = { settingsViewModel.setPrestigeTheme(it) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 5c. Split with Friends (reached from Settings only)
                composable(Screen.SplitFriends.route) {
                    val splitContext = androidx.compose.ui.platform.LocalContext.current
                    val splitViewModel: com.example.feature.split.SplitViewModel = viewModel(
                        factory = com.example.feature.split.SplitViewModel.Factory(
                            com.example.data.repository.SplitRepository(
                                com.example.data.local.AppDatabase.getDatabase(splitContext).splitDao()
                            )
                        )
                    )
                    com.example.feature.split.SplitScreen(
                        viewModel = splitViewModel,
                        currency = userSettings.defaultCurrency,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 6. Expense Books (Multiple Ledgers)
                composable(Screen.ExpenseBooks.route) {
                    val booksViewModel: ExpenseBooksViewModel = viewModel(
                        factory = ExpenseBooksViewModel.Factory(repository)
                    )
                    ExpenseBooksScreen(
                        viewModel = booksViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToBookDetail = { bookId ->
                            navController.navigate(Screen.ExpenseBookDetail.createRoute(bookId))
                        },
                        onNavigateToAddTransactionWithBook = { bookId ->
                            navController.navigate(Screen.AddTransaction.createRoute(-1L))
                        }
                    )
                }

                // 7. Expense Book Detail
                composable(
                    route = Screen.ExpenseBookDetail.route,
                    arguments = listOf(
                        navArgument("bookId") {
                            type = NavType.LongType
                        }
                    )
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
                    val booksViewModel: ExpenseBooksViewModel = viewModel(
                        factory = ExpenseBooksViewModel.Factory(repository)
                    )
                    ExpenseBookDetailScreen(
                        bookId = bookId,
                        viewModel = booksViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEditTransaction = { txId ->
                            navController.navigate(Screen.AddTransaction.createRoute(txId))
                        },
                        onNavigateToAddTransaction = { bId ->
                            navController.navigate(Screen.AddTransaction.createRoute(-1L))
                        }
                    )
                }

                // 8. Accounts Screen
                composable(Screen.Accounts.route) {
                    val accountsViewModel: AccountsViewModel = viewModel(
                        factory = AccountsViewModel.Factory(repository)
                    )
                    AccountsScreen(
                        viewModel = accountsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 9. Recurring / Subscriptions Screen
                composable(Screen.Recurring.route) {
                    val recurringViewModel: RecurringViewModel = viewModel(
                        factory = RecurringViewModel.Factory(repository)
                    )
                    RecurringScreen(
                        viewModel = recurringViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 10. Budgets Screen
                composable(Screen.Budgets.route) {
                    val budgetViewModel: BudgetViewModel = viewModel(
                        initializer = { BudgetViewModel(repository) }
                    )
                    BudgetScreen(
                        viewModel = budgetViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 11. Financial Goals Screen
                composable(Screen.Goals.route) {
                    val goalsViewModel: GoalsViewModel = viewModel(
                        initializer = { GoalsViewModel(repository) }
                    )
                    GoalsScreen(
                        viewModel = goalsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 12. Financial Calendar Screen
                composable(Screen.Calendar.route) {
                    val calendarViewModel: CalendarViewModel = viewModel(
                        initializer = { CalendarViewModel(repository) }
                    )
                    CalendarScreen(
                        viewModel = calendarViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 13. Smart Spending Insights Screen
                composable(Screen.Insights.route) {
                    val insightsViewModel: InsightsViewModel = viewModel(
                        factory = InsightsViewModel.Factory(repository)
                    )
                    InsightsScreen(
                        viewModel = insightsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 14. Net Worth Screen
                composable(Screen.NetWorth.route) {
                    val netWorthViewModel: NetWorthViewModel = viewModel(
                        factory = NetWorthViewModel.Factory(repository)
                    )
                    NetWorthScreen(
                        viewModel = netWorthViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 15. Cash Flow Forecast Screen
                composable(Screen.CashFlow.route) {
                    val cashFlowViewModel: CashFlowViewModel = viewModel(
                        factory = CashFlowViewModel.Factory(repository)
                    )
                    CashFlowForecastScreen(
                        viewModel = cashFlowViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 16. Credit Cards Screen
                composable(Screen.CreditCards.route) {
                    val creditCardsViewModel: CreditCardsViewModel = viewModel(
                        factory = CreditCardsViewModel.Factory(repository)
                    )
                    CreditCardsScreen(
                        viewModel = creditCardsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 17. Loans Screen
                composable(Screen.Loans.route) {
                    val loansViewModel: LoansViewModel = viewModel(
                        factory = LoansViewModel.Factory(repository)
                    )
                    LoansScreen(
                        viewModel = loansViewModel,
                        onLoanClick = { loanId ->
                            navController.navigate(Screen.LoanDetail.createRoute(loanId))
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 18. Loan Detail Screen
                composable(
                    route = Screen.LoanDetail.route,
                    arguments = listOf(
                        navArgument("loanId") {
                            type = NavType.LongType
                        }
                    )
                ) { backStackEntry ->
                    val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L
                    val loansViewModel: LoansViewModel = viewModel(
                        factory = LoansViewModel.Factory(repository)
                    )
                    LoanDetailScreen(
                        loanId = loanId,
                        viewModel = loansViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 19. Global Search Screen
                composable(Screen.Search.route) {
                    val searchViewModel: SearchViewModel = viewModel(
                        initializer = { SearchViewModel(repository) }
                    )
                    SearchScreen(
                        viewModel = searchViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTransactionDetail = { txId ->
                            navController.navigate(Screen.AddTransaction.createRoute(txId))
                        },
                        onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                        onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                        onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                        onNavigateToCreditCards = { navController.navigate(Screen.CreditCards.route) },
                        onNavigateToLoans = { navController.navigate(Screen.Loans.route) }
                    )
                }

                // 20. Data Export Screen
                composable(Screen.Export.route) {
                    val exportViewModel: ExportViewModel = viewModel(
                        initializer = { ExportViewModel(repository) }
                    )
                    ExportScreen(
                        viewModel = exportViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 21. Backup & Restore Screen
                composable(Screen.BackupRestore.route) {
                    val backupViewModel: BackupRestoreViewModel = viewModel(
                        initializer = { BackupRestoreViewModel(repository) }
                    )
                    BackupRestoreScreen(
                        viewModel = backupViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // 22. Merchant Detail Screen
                composable(
                    route = Screen.MerchantDetail.route,
                    arguments = listOf(
                        navArgument("merchantName") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val encodedName = backStackEntry.arguments?.getString("merchantName") ?: ""
                    val merchantName = android.net.Uri.decode(encodedName)
                    val merchantViewModel: MerchantDetailViewModel = viewModel(
                        factory = MerchantDetailViewModel.Factory(repository, merchantName)
                    )
                    MerchantDetailScreen(
                        viewModel = merchantViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onTransactionClick = { txId ->
                            navController.navigate(Screen.AddTransaction.createRoute(txId))
                        }
                    )
                }

                // 23. Home Customization Screen
                composable(Screen.HomeCustomize.route) {
                    val homeCustomizeViewModel: HomeCustomizeViewModel = viewModel(
                        factory = HomeCustomizeViewModel.Factory(repository)
                    )
                    HomeCustomizeScreen(
                        viewModel = homeCustomizeViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // Voice Expense Bottom Sheet
        if (showVoiceExpenseSheet) {
            VoiceExpenseBottomSheet(
                onDismissRequest = { showVoiceExpenseSheet = false },
                categories = categories,
                accounts = accounts,
                books = books,
                defaultCurrency = userSettings.defaultCurrency,
                onSaveTransaction = { transaction ->
                    coroutineScope.launch {
                        repository.addTransaction(transaction)
                        showVoiceExpenseSheet = false
                    }
                },
                onOpenInFullEditor = { parsed ->
                    showVoiceExpenseSheet = false
                    navController.navigate(Screen.AddTransaction.createRoute(-1L))
                }
            )
        }

        // App Lock Overlay
        if (isAppLocked) {
            AppLockScreen(
                onUnlocked = { AppLockManager.unlock() }
            )
        }
    }
}

@Composable
fun FlowlyBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Five equal slots, all built the same way:
        // Home | History | Add | Analytics | Settings.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.14f)
                ),
            shape = RoundedCornerShape(34.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavTab(
                    selected = currentRoute == Screen.Home.route,
                    label = "Home",
                    selectedIcon = Icons.Default.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onNavigate(Screen.Home.route) },
                    testTag = "nav_home",
                    modifier = Modifier.weight(1f)
                )
                BottomNavTab(
                    selected = currentRoute == Screen.Transactions.route,
                    label = "History",
                    selectedIcon = Icons.Default.ReceiptLong,
                    unselectedIcon = Icons.Outlined.ReceiptLong,
                    onClick = { onNavigate(Screen.Transactions.route) },
                    testTag = "nav_transactions",
                    modifier = Modifier.weight(1f)
                )
                // Add: same size, icon and label layout as the other tabs. It is an
                // action rather than a destination, so it never shows a selected
                // state and is always drawn in the accent colour.
                BottomNavTab(
                    selected = false,
                    label = "Add",
                    selectedIcon = Icons.Default.AddCircle,
                    unselectedIcon = Icons.Default.AddCircle,
                    onClick = { onNavigate("add_action") },
                    testTag = "nav_add_button",
                    modifier = Modifier.weight(1f),
                    alwaysAccent = true,
                    contentDescription = "Add Transaction"
                )
                BottomNavTab(
                    selected = currentRoute == Screen.Analytics.route,
                    label = "Analytics",
                    selectedIcon = Icons.Default.Insights,
                    unselectedIcon = Icons.Outlined.Insights,
                    onClick = { onNavigate(Screen.Analytics.route) },
                    testTag = "nav_analytics",
                    modifier = Modifier.weight(1f)
                )
                BottomNavTab(
                    selected = currentRoute == Screen.More.route,
                    label = "Settings",
                    selectedIcon = Icons.Default.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    onClick = { onNavigate(Screen.More.route) },
                    testTag = "nav_more",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BottomNavTab(
    selected: Boolean,
    label: String,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    alwaysAccent: Boolean = false,
    contentDescription: String? = null
) {
    val tint by animateColorAsState(
        targetValue = if (selected || alwaysAccent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = FlowlyMotion.quick(),
        label = "tabTint"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .pressable(pressedScale = 0.94f, pressedAlpha = 0.7f, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = contentDescription ?: label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = tint,
            maxLines = 1
        )
    }
}
