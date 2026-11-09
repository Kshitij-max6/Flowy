package com.example.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Account
import com.example.domain.model.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val defaultCurrency: String = "₹",
    val showAddEditDialog: Boolean = false,
    val editingAccount: Account? = null,
    val accountNameInput: String = "",
    val accountBalanceInput: String = "",
    val accountTypeInput: AccountType = AccountType.BANK,
    val accountIconInput: String = "AccountBalance",
    val accountToDelete: Account? = null,
    val errorMessage: String? = null
)

class AccountsViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            combine(
                repository.getAllAccounts(),
                repository.getUserSettings()
            ) { accounts, settings ->
                val netWorth = accounts.sumOf { it.balance }
                _uiState.value = _uiState.value.copy(
                    accounts = accounts,
                    totalNetWorth = netWorth,
                    defaultCurrency = settings.defaultCurrency
                )
            }.collect {}
        }
    }

    fun openAddAccountDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingAccount = null,
            accountNameInput = "",
            accountBalanceInput = "",
            accountTypeInput = AccountType.BANK,
            accountIconInput = "AccountBalance",
            errorMessage = null
        )
    }

    fun openEditAccountDialog(account: Account) {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingAccount = account,
            accountNameInput = account.name,
            accountBalanceInput = account.balance.toString(),
            accountTypeInput = account.type,
            accountIconInput = account.iconName,
            errorMessage = null
        )
    }

    fun dismissAddEditDialog() {
        _uiState.value = _uiState.value.copy(showAddEditDialog = false, editingAccount = null)
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(accountNameInput = name, errorMessage = null)
    }

    fun onBalanceChanged(balance: String) {
        val cleaned = balance.filter { it.isDigit() || it == '.' || it == '-' }
        _uiState.value = _uiState.value.copy(accountBalanceInput = cleaned, errorMessage = null)
    }

    fun onTypeChanged(type: AccountType) {
        val defaultIcon = when (type) {
            AccountType.BANK -> "AccountBalance"
            AccountType.UPI -> "QrCode"
            AccountType.WALLET -> "AccountBalanceWallet"
            AccountType.CASH -> "Payments"
            AccountType.SAVINGS -> "Savings"
            AccountType.CREDIT_CARD -> "CreditCard"
            AccountType.LOAN -> "AccountBalance"
            AccountType.INVESTMENT -> "TrendingUp"
            AccountType.OTHER -> "AccountBalance"
        }
        _uiState.value = _uiState.value.copy(accountTypeInput = type, accountIconInput = defaultIcon)
    }

    fun onIconChanged(icon: String) {
        _uiState.value = _uiState.value.copy(accountIconInput = icon)
    }

    fun saveAccount() {
        val state = _uiState.value
        val name = state.accountNameInput.trim()
        if (name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter an account name")
            return
        }

        val balance = state.accountBalanceInput.toDoubleOrNull() ?: 0.0
        val colorHex = when (state.accountTypeInput) {
            AccountType.BANK -> "#3B82F6"
            AccountType.UPI -> "#8B5CF6"
            AccountType.WALLET -> "#8B5CF6"
            AccountType.CASH -> "#10B981"
            AccountType.SAVINGS -> "#059669"
            AccountType.CREDIT_CARD -> "#F43F5E"
            AccountType.LOAN -> "#E11D48"
            AccountType.INVESTMENT -> "#F59E0B"
            AccountType.OTHER -> "#64748B"
        }

        viewModelScope.launch {
            if (state.editingAccount != null) {
                val updated = state.editingAccount.copy(
                    name = name,
                    balance = balance,
                    type = state.accountTypeInput,
                    iconName = state.accountIconInput,
                    colorHex = colorHex
                )
                repository.updateAccount(updated)
            } else {
                val newAcc = Account(
                    name = name,
                    type = state.accountTypeInput,
                    balance = balance,
                    currency = state.defaultCurrency,
                    iconName = state.accountIconInput,
                    colorHex = colorHex,
                    isDefault = state.accounts.isEmpty()
                )
                repository.addAccount(newAcc)
            }
            _uiState.value = _uiState.value.copy(showAddEditDialog = false, editingAccount = null)
        }
    }

    fun promptDeleteAccount(account: Account) {
        _uiState.value = _uiState.value.copy(accountToDelete = account)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(accountToDelete = null)
    }

    fun confirmDeleteAccount() {
        val acc = _uiState.value.accountToDelete ?: return
        viewModelScope.launch {
            repository.deleteAccount(acc)
            _uiState.value = _uiState.value.copy(accountToDelete = null)
        }
    }

    class Factory(private val repository: FlowlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountsViewModel(repository) as T
        }
    }
}
