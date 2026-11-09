package com.example.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Transactions : Screen("transactions")
    object AddTransaction : Screen("add_transaction?editId={editId}") {
        fun createRoute(editId: Long = -1L) = "add_transaction?editId=$editId"
    }
    object Analytics : Screen("analytics")
    object More : Screen("more")
    object Accounts : Screen("accounts")
    object Recurring : Screen("recurring")
    object Budgets : Screen("budgets")
    object Goals : Screen("goals")
    object Calendar : Screen("calendar")
    object Insights : Screen("insights")
    object NetWorth : Screen("net_worth")
    object CashFlow : Screen("cash_flow")
    object CreditCards : Screen("credit_cards")
    object Loans : Screen("loans")
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }
    object Search : Screen("search")
    object Export : Screen("export")
    object BackupRestore : Screen("backup_restore")
    object Settings : Screen("settings")
    object Appearance : Screen("appearance")
    object Onboarding : Screen("onboarding")
    object MerchantDetail : Screen("merchant_detail/{merchantName}") {
        fun createRoute(merchantName: String) = "merchant_detail/${android.net.Uri.encode(merchantName)}"
    }
    object HomeCustomize : Screen("home_customize")
    object ExpenseBooks : Screen("expense_books")
    object SplitFriends : Screen("split_friends")
    object ExpenseBookDetail : Screen("expense_book_detail/{bookId}") {
        fun createRoute(bookId: Long) = "expense_book_detail/$bookId"
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: String,
    val unselectedIcon: String,
    val isCenterAction: Boolean = false
) {
    object Home : BottomNavItem("home", "Home", "Home", "Home")
    object Transactions : BottomNavItem("transactions", "Transactions", "ReceiptLong", "ReceiptLong")
    object Add : BottomNavItem("add_action", "Add", "Add", "Add", isCenterAction = true)
    object Analytics : BottomNavItem("analytics", "Analytics", "Insights", "Insights")
    object More : BottomNavItem("more", "More", "MoreHoriz", "MoreHoriz")

    companion object {
        val items = listOf(Home, Transactions, Add, Analytics, More)
    }
}
