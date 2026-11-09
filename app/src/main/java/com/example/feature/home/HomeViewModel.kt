package com.example.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.util.DateUtils
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Account
import com.example.domain.model.RecurringDetail
import com.example.domain.model.SpendingInsight
import com.example.domain.model.TimeFilter
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val userName: String = "Kshitij",
    val greeting: String = "Good Morning",
    val defaultCurrency: String = "₹",
    val dailyBudget: Double = 1000.0,
    val todaySpending: Double = 0.0,
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<TransactionDetail> = emptyList(),
    val upcomingRecurring: List<RecurringDetail> = emptyList(),
    val dailyInsight: SpendingInsight? = null,
    val sectionsOrder: List<String> = listOf("spending", "accounts", "recent", "upcoming", "insight"),
    val hiddenSections: Set<String> = emptySet(),
    val hideAmounts: Boolean = false,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getUserSettings(),
                repository.getTransactionDetailsFlow(),
                repository.getAllAccounts(),
                repository.getUpcomingRecurring(withinDays = 7),
                repository.getSpendingInsights(TimeFilter.THIS_MONTH)
            ) { settings, allTransactions, accounts, upcoming, insights ->
                val startOfToday = DateUtils.getStartOfToday()
                val endOfToday = DateUtils.getEndOfToday()

                // Calculate today's spending (EXPENSE only)
                val todaySpending = allTransactions
                    .filter { it.transaction.dateTime in startOfToday..endOfToday && it.transaction.type == TransactionType.EXPENSE }
                    .sumOf { it.transaction.amount }

                val recent = allTransactions.take(5)

                val orderKeys = if (settings.homeSectionsOrder.isNotBlank()) {
                    settings.homeSectionsOrder.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    listOf("spending", "accounts", "recent", "upcoming", "insight")
                }

                val hiddenSet = if (settings.hiddenHomeSections.isNotBlank()) {
                    settings.hiddenHomeSections.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                } else {
                    emptySet()
                }

                HomeUiState(
                    userName = settings.userName,
                    greeting = DateUtils.getGreeting(),
                    defaultCurrency = settings.defaultCurrency,
                    dailyBudget = settings.dailyBudget,
                    todaySpending = todaySpending,
                    accounts = accounts,
                    recentTransactions = recent,
                    upcomingRecurring = upcoming,
                    dailyInsight = insights.firstOrNull(),
                    sectionsOrder = orderKeys,
                    hiddenSections = hiddenSet,
                    hideAmounts = settings.hideAmounts,
                    isLoading = false
                )
            }.collectLatest { newState ->
                _uiState.value = newState
            }
        }
    }

    fun togglePrivacyMode() {
        viewModelScope.launch {
            val current = _uiState.value.hideAmounts
            val newHideState = !current
            repository.updatePrivacyAndLockSettings(
                hideAmounts = newHideState,
                hideAmountsPersist = true,
                appLockEnabled = _uiState.value.hideAmounts,
                appLockTimeoutMinutes = 0
            )
            com.example.core.util.CurrencyFormatter.isPrivacyActive = newHideState
        }
    }

    class Factory(private val repository: FlowlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
