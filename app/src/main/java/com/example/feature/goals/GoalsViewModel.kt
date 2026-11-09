package com.example.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Account
import com.example.domain.model.Goal
import com.example.domain.model.GoalContribution
import com.example.domain.model.GoalDetail
import com.example.domain.model.GoalStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class GoalsUiState(
    val goals: List<GoalDetail> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val defaultCurrency: String = "₹",
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0,
    val overallPercentage: Double = 0.0,
    val showAddEditDialog: Boolean = false,
    val editingGoal: Goal? = null,
    val nameInput: String = "",
    val targetAmountInput: String = "",
    val initialAmountInput: String = "",
    val selectedTargetDate: Long? = null,
    val selectedAccountId: Long? = null,
    val selectedIconName: String = "LaptopMac",
    val showContributionDialog: Boolean = false,
    val contributionGoal: GoalDetail? = null,
    val contributionAmountInput: String = "",
    val contributionNoteInput: String = "",
    val withdrawFromAccount: Boolean = false,
    val selectedContributionAccountId: Long? = null,
    val showDetailDialog: Boolean = false,
    val detailGoal: GoalDetail? = null,
    val goalToDelete: GoalDetail? = null,
    val errorMessage: String? = null
)

class GoalsViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllGoalsFlow(),
                repository.getAllAccounts(),
                repository.getUserSettings()
            ) { goals, accounts, settings ->
                val totalSaved = goals.sumOf { it.goal.currentAmount }
                val totalTarget = goals.sumOf { it.goal.targetAmount }
                val overallPct = if (totalTarget > 0) (totalSaved / totalTarget) * 100.0 else 0.0

                _uiState.update { current ->
                    current.copy(
                        goals = goals,
                        accounts = accounts,
                        defaultCurrency = settings.defaultCurrency,
                        totalSaved = totalSaved,
                        totalTarget = totalTarget,
                        overallPercentage = overallPct,
                        // update detailGoal reference if active
                        detailGoal = current.detailGoal?.let { cur -> goals.find { it.goal.id == cur.goal.id } }
                    )
                }
            }.collect {}
        }
    }

    fun openAddGoalDialog() {
        val defaultAcc = _uiState.value.accounts.firstOrNull()?.id
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }

        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingGoal = null,
                nameInput = "",
                targetAmountInput = "",
                initialAmountInput = "",
                selectedTargetDate = cal.timeInMillis,
                selectedAccountId = defaultAcc,
                selectedIconName = "LaptopMac",
                errorMessage = null
            )
        }
    }

    fun openEditGoalDialog(detail: GoalDetail) {
        val goal = detail.goal
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingGoal = goal,
                nameInput = goal.name,
                targetAmountInput = if (goal.targetAmount > 0) goal.targetAmount.toInt().toString() else "",
                initialAmountInput = if (goal.currentAmount > 0) goal.currentAmount.toInt().toString() else "",
                selectedTargetDate = goal.targetDate,
                selectedAccountId = goal.linkedAccountId,
                selectedIconName = goal.iconName,
                errorMessage = null
            )
        }
    }

    fun dismissAddEditDialog() {
        _uiState.update { it.copy(showAddEditDialog = false, editingGoal = null, errorMessage = null) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(nameInput = name, errorMessage = null) }
    }

    fun onTargetAmountChanged(amount: String) {
        _uiState.update { it.copy(targetAmountInput = amount, errorMessage = null) }
    }

    fun onInitialAmountChanged(amount: String) {
        _uiState.update { it.copy(initialAmountInput = amount, errorMessage = null) }
    }

    fun onTargetDateChanged(timestamp: Long?) {
        _uiState.update { it.copy(selectedTargetDate = timestamp) }
    }

    fun onAccountIdChanged(accountId: Long?) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun onIconSelected(icon: String) {
        _uiState.update { it.copy(selectedIconName = icon) }
    }

    fun saveGoal() {
        val state = _uiState.value
        val name = state.nameInput.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a goal name") }
            return
        }

        val target = state.targetAmountInput.toDoubleOrNull()
        if (target == null || target <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid target amount") }
            return
        }

        val initial = state.initialAmountInput.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val goal = Goal(
                id = state.editingGoal?.id ?: 0,
                name = name,
                targetAmount = target,
                currentAmount = if (state.editingGoal != null) state.editingGoal.currentAmount else initial,
                currency = state.defaultCurrency,
                targetDate = state.selectedTargetDate,
                createdDate = state.editingGoal?.createdDate ?: System.currentTimeMillis(),
                linkedAccountId = state.selectedAccountId,
                iconName = state.selectedIconName,
                status = if ((state.editingGoal?.currentAmount ?: initial) >= target) GoalStatus.COMPLETED else GoalStatus.ACTIVE
            )

            if (state.editingGoal != null) {
                repository.updateGoal(goal)
            } else {
                repository.addGoal(goal)
            }

            dismissAddEditDialog()
        }
    }

    // Add Money / Contribution
    fun openAddContributionDialog(detail: GoalDetail) {
        _uiState.update {
            it.copy(
                showContributionDialog = true,
                contributionGoal = detail,
                contributionAmountInput = "",
                contributionNoteInput = "",
                withdrawFromAccount = false,
                selectedContributionAccountId = detail.goal.linkedAccountId ?: it.accounts.firstOrNull()?.id,
                errorMessage = null
            )
        }
    }

    fun dismissContributionDialog() {
        _uiState.update { it.copy(showContributionDialog = false, contributionGoal = null, errorMessage = null) }
    }

    fun onContributionAmountChanged(amount: String) {
        _uiState.update { it.copy(contributionAmountInput = amount, errorMessage = null) }
    }

    fun onContributionNoteChanged(note: String) {
        _uiState.update { it.copy(contributionNoteInput = note) }
    }

    fun onWithdrawToggle(withdraw: Boolean) {
        _uiState.update { it.copy(withdrawFromAccount = withdraw) }
    }

    fun onContributionAccountChanged(accountId: Long?) {
        _uiState.update { it.copy(selectedContributionAccountId = accountId) }
    }

    fun saveContribution() {
        val state = _uiState.value
        val goalDetail = state.contributionGoal ?: return
        val amount = state.contributionAmountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }

        viewModelScope.launch {
            val note = state.contributionNoteInput.ifBlank { "Savings contribution" }
            val withdrawAcc = if (state.withdrawFromAccount) state.selectedContributionAccountId else null
            repository.addGoalContribution(
                goalId = goalDetail.goal.id,
                amount = amount,
                note = note,
                withdrawFromAccountId = withdrawAcc
            )
            dismissContributionDialog()
        }
    }

    // Detail & History
    fun openGoalDetail(detail: GoalDetail) {
        _uiState.update { it.copy(showDetailDialog = true, detailGoal = detail) }
    }

    fun dismissGoalDetail() {
        _uiState.update { it.copy(showDetailDialog = false, detailGoal = null) }
    }

    fun deleteContribution(contribution: GoalContribution) {
        viewModelScope.launch {
            repository.deleteGoalContribution(contribution)
        }
    }

    fun promptDelete(detail: GoalDetail) {
        _uiState.update { it.copy(goalToDelete = detail) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(goalToDelete = null) }
    }

    fun confirmDelete() {
        val detail = _uiState.value.goalToDelete ?: return
        viewModelScope.launch {
            repository.deleteGoal(detail.goal)
            dismissDelete()
        }
    }
}
