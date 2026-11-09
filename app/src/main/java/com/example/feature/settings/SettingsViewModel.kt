package com.example.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val userSettings: UserSettings = UserSettings(),
    val userNameInput: String = "Kshitij",
    val dailyBudgetInput: String = "1000",
    val monthlyBudgetInput: String = "30000",
    val selectedCurrency: String = "₹",
    val showCurrencyDialog: Boolean = false,
    val showBudgetDialog: Boolean = false,
    val showProfileDialog: Boolean = false,
    val showPrestigeStudio: Boolean = false,
    val isSaved: Boolean = false
)

class SettingsViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getUserSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    userSettings = settings,
                    userNameInput = settings.userName,
                    dailyBudgetInput = if (settings.dailyBudget % 1.0 == 0.0) settings.dailyBudget.toLong().toString() else settings.dailyBudget.toString(),
                    monthlyBudgetInput = if (settings.monthlyBudget % 1.0 == 0.0) settings.monthlyBudget.toLong().toString() else settings.monthlyBudget.toString(),
                    selectedCurrency = settings.defaultCurrency
                )
            }
        }
    }

    fun openCurrencyDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCurrencyDialog = show)
    }

    fun openBudgetDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showBudgetDialog = show)
    }

    fun openProfileDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showProfileDialog = show)
    }

    fun openPrestigeStudio(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPrestigeStudio = show)
    }

    fun setPrestigeTheme(theme: String) {
        viewModelScope.launch {
            val current = _uiState.value.userSettings
            val updated = current.copy(prestigeTheme = theme)
            repository.updateUserSettings(updated)
        }
    }

    fun setHeroFont(font: String) {
        viewModelScope.launch {
            val current = _uiState.value.userSettings
            val updated = current.copy(heroFont = font)
            repository.updateUserSettings(updated)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            val current = _uiState.value.userSettings
            val updated = current.copy(themeMode = mode)
            repository.updateUserSettings(updated)
        }
    }

    fun onUserNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(userNameInput = name)
    }

    fun onDailyBudgetChanged(budget: String) {
        val cleaned = budget.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(dailyBudgetInput = cleaned)
    }

    fun onMonthlyBudgetChanged(budget: String) {
        val cleaned = budget.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(monthlyBudgetInput = cleaned)
    }

    fun selectCurrency(currencySymbol: String) {
        viewModelScope.launch {
            val updated = _uiState.value.userSettings.copy(defaultCurrency = currencySymbol)
            repository.updateUserSettings(updated)
            _uiState.value = _uiState.value.copy(selectedCurrency = currencySymbol, showCurrencyDialog = false)
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val name = _uiState.value.userNameInput.trim()
            if (name.isNotBlank()) {
                val updated = _uiState.value.userSettings.copy(userName = name)
                repository.updateUserSettings(updated)
            }
            _uiState.value = _uiState.value.copy(showProfileDialog = false)
        }
    }

    fun saveBudgets() {
        val daily = _uiState.value.dailyBudgetInput.toDoubleOrNull() ?: 1000.0
        val monthly = _uiState.value.monthlyBudgetInput.toDoubleOrNull() ?: 30000.0
        viewModelScope.launch {
            val updated = _uiState.value.userSettings.copy(dailyBudget = daily, monthlyBudget = monthly)
            repository.updateUserSettings(updated)
            _uiState.value = _uiState.value.copy(showBudgetDialog = false)
        }
    }

    fun toggleHideAmounts(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.userSettings
            val updated = current.copy(hideAmounts = enabled)
            repository.updateUserSettings(updated)
            com.example.core.util.CurrencyFormatter.isPrivacyActive = enabled
        }
    }

    fun toggleHideAmountsPersist(persist: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.userSettings
            val updated = current.copy(hideAmountsPersist = persist)
            repository.updateUserSettings(updated)
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.userSettings
            val updated = current.copy(appLockEnabled = enabled)
            repository.updateUserSettings(updated)
            if (!enabled) {
                com.example.core.util.AppLockManager.unlock()
            }
        }
    }

    fun setAppLockTimeout(minutes: Int) {
        viewModelScope.launch {
            val current = _uiState.value.userSettings
            val updated = current.copy(appLockTimeoutMinutes = minutes)
            repository.updateUserSettings(updated)
        }
    }

    class Factory(private val repository: FlowlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
