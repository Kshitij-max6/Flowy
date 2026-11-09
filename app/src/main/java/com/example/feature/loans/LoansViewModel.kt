package com.example.feature.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Account
import com.example.domain.model.Loan
import com.example.domain.model.LoanDetail
import com.example.domain.model.LoanPayment
import com.example.domain.model.LoanStatus
import com.example.domain.model.LoanType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class LoansUiState(
    val loans: List<Loan> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currency: String = "₹",
    val selectedLoanDetail: LoanDetail? = null,
    // Dialogs
    val showAddEditDialog: Boolean = false,
    val editingLoan: Loan? = null,
    val showPaymentDialog: Boolean = false,
    val payingLoan: Loan? = null,
    val loanToDelete: Loan? = null,
    // Add/Edit form state
    val nameInput: String = "",
    val lenderInput: String = "",
    val typeInput: LoanType = LoanType.PERSONAL,
    val originalAmountInput: String = "",
    val remainingBalanceInput: String = "",
    val interestRateInput: String = "",
    val monthlyPaymentInput: String = "",
    val selectedAccountId: Long? = null,
    // Payment form state
    val paymentAmountInput: String = "",
    val principalAmountInput: String = "",
    val interestAmountInput: String = "",
    val paymentSourceAccountId: Long? = null,
    val paymentNoteInput: String = "Monthly EMI Payment",
    val errorMessage: String? = null
)

class LoansViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState())
    private val _selectedLoanId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<LoansUiState> = combine(
        repository.getAllLoans(),
        repository.getAllAccounts(),
        repository.getUserSettings(),
        _selectedLoanId.flatMapLatest { id ->
            if (id != null) repository.getLoanDetail(id) else kotlinx.coroutines.flow.flowOf(null)
        },
        _uiState
    ) { loans, accounts, settings, loanDetail, formState ->
        formState.copy(
            loans = loans,
            accounts = accounts,
            currency = settings.defaultCurrency,
            selectedLoanDetail = loanDetail
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoansUiState()
    )

    fun selectLoan(loanId: Long) {
        _selectedLoanId.value = loanId
    }

    fun clearSelectedLoan() {
        _selectedLoanId.value = null
    }

    fun openAddLoanDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingLoan = null,
            nameInput = "",
            lenderInput = "",
            typeInput = LoanType.PERSONAL,
            originalAmountInput = "",
            remainingBalanceInput = "",
            interestRateInput = "9.5",
            monthlyPaymentInput = "",
            selectedAccountId = null,
            errorMessage = null
        )
    }

    fun openEditLoanDialog(loan: Loan) {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingLoan = loan,
            nameInput = loan.name,
            lenderInput = loan.lender,
            typeInput = loan.loanType,
            originalAmountInput = loan.originalAmount.toLong().toString(),
            remainingBalanceInput = loan.remainingBalance.toLong().toString(),
            interestRateInput = loan.interestRate.toString(),
            monthlyPaymentInput = loan.monthlyPayment.toLong().toString(),
            selectedAccountId = loan.linkedAccountId,
            errorMessage = null
        )
    }

    fun dismissAddEditDialog() {
        _uiState.value = _uiState.value.copy(showAddEditDialog = false, editingLoan = null)
    }

    fun onNameChanged(v: String) { _uiState.value = _uiState.value.copy(nameInput = v, errorMessage = null) }
    fun onLenderChanged(v: String) { _uiState.value = _uiState.value.copy(lenderInput = v, errorMessage = null) }
    fun onTypeChanged(t: LoanType) { _uiState.value = _uiState.value.copy(typeInput = t) }
    fun onOriginalAmountChanged(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(
            originalAmountInput = cleaned,
            remainingBalanceInput = if (_uiState.value.editingLoan == null) cleaned else _uiState.value.remainingBalanceInput,
            errorMessage = null
        )
    }
    fun onRemainingBalanceChanged(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(remainingBalanceInput = cleaned, errorMessage = null)
    }
    fun onInterestRateChanged(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(interestRateInput = cleaned)
    }
    fun onMonthlyPaymentChanged(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(monthlyPaymentInput = cleaned)
    }
    fun onAccountSelected(id: Long?) {
        _uiState.value = _uiState.value.copy(selectedAccountId = id)
    }

    fun saveLoan() {
        val s = _uiState.value
        val name = s.nameInput.trim()
        if (name.isBlank()) {
            _uiState.value = s.copy(errorMessage = "Please enter a loan title")
            return
        }

        val original = s.originalAmountInput.toDoubleOrNull() ?: 0.0
        if (original <= 0) {
            _uiState.value = s.copy(errorMessage = "Please enter a valid loan amount")
            return
        }

        val remaining = s.remainingBalanceInput.toDoubleOrNull() ?: original
        val rate = s.interestRateInput.toDoubleOrNull() ?: 0.0
        val emi = s.monthlyPaymentInput.toDoubleOrNull() ?: (original * 0.03)

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 30) }

        viewModelScope.launch {
            if (s.editingLoan != null) {
                val updated = s.editingLoan.copy(
                    name = name,
                    lender = s.lenderInput.trim().ifBlank { "Lender" },
                    loanType = s.typeInput,
                    originalAmount = original,
                    remainingBalance = remaining,
                    interestRate = rate,
                    monthlyPayment = emi,
                    linkedAccountId = s.selectedAccountId
                )
                repository.updateLoan(updated)
            } else {
                val newLoan = Loan(
                    id = 0,
                    name = name,
                    lender = s.lenderInput.trim().ifBlank { "Bank" },
                    loanType = s.typeInput,
                    originalAmount = original,
                    remainingBalance = remaining,
                    interestRate = rate,
                    monthlyPayment = emi,
                    nextPaymentDate = cal.timeInMillis,
                    startDate = System.currentTimeMillis(),
                    endDate = null,
                    currency = s.currency,
                    status = LoanStatus.ACTIVE,
                    linkedAccountId = s.selectedAccountId
                )
                repository.addLoan(newLoan)
            }
            dismissAddEditDialog()
        }
    }

    // Payment Flow
    fun openPaymentDialog(loan: Loan) {
        val defaultAccount = _uiState.value.accounts.firstOrNull()?.id
        val monthlyInterest = (loan.remainingBalance * (loan.interestRate / 100.0) / 12.0)
        val defaultPrincipal = (loan.monthlyPayment - monthlyInterest).coerceAtLeast(0.0)

        _uiState.value = _uiState.value.copy(
            showPaymentDialog = true,
            payingLoan = loan,
            paymentAmountInput = loan.monthlyPayment.toString(),
            principalAmountInput = String.format(java.util.Locale.US, "%.2f", defaultPrincipal),
            interestAmountInput = String.format(java.util.Locale.US, "%.2f", monthlyInterest),
            paymentSourceAccountId = loan.linkedAccountId ?: defaultAccount,
            paymentNoteInput = "Monthly EMI Payment for ${loan.name}",
            errorMessage = null
        )
    }

    fun dismissPaymentDialog() {
        _uiState.value = _uiState.value.copy(showPaymentDialog = false, payingLoan = null)
    }

    fun onPaymentAmountChanged(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }
        val amount = cleaned.toDoubleOrNull() ?: 0.0
        val loan = _uiState.value.payingLoan
        val monthlyInterest = if (loan != null) (loan.remainingBalance * (loan.interestRate / 100.0) / 12.0) else 0.0
        val principal = (amount - monthlyInterest).coerceAtLeast(0.0)

        _uiState.value = _uiState.value.copy(
            paymentAmountInput = cleaned,
            principalAmountInput = String.format(java.util.Locale.US, "%.2f", principal),
            interestAmountInput = String.format(java.util.Locale.US, "%.2f", monthlyInterest),
            errorMessage = null
        )
    }

    fun onPrincipalAmountChanged(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(principalAmountInput = cleaned)
    }

    fun onInterestAmountChanged(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(interestAmountInput = cleaned)
    }

    fun onPaymentSourceAccountChanged(id: Long) {
        _uiState.value = _uiState.value.copy(paymentSourceAccountId = id)
    }

    fun onPaymentNoteChanged(v: String) {
        _uiState.value = _uiState.value.copy(paymentNoteInput = v)
    }

    fun recordPayment() {
        val s = _uiState.value
        val loan = s.payingLoan ?: return
        val total = s.paymentAmountInput.toDoubleOrNull() ?: 0.0

        if (total <= 0) {
            _uiState.value = s.copy(errorMessage = "Please enter a valid payment amount")
            return
        }

        val principal = s.principalAmountInput.toDoubleOrNull() ?: total
        val interest = s.interestAmountInput.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val payment = LoanPayment(
                id = 0,
                loanId = loan.id,
                amount = total,
                principalAmount = principal,
                interestAmount = interest,
                paymentDate = System.currentTimeMillis(),
                accountId = s.paymentSourceAccountId,
                note = s.paymentNoteInput
            )
            repository.recordLoanPayment(payment, s.paymentSourceAccountId)
            dismissPaymentDialog()
        }
    }

    // Delete
    fun requestDeleteLoan(loan: Loan) {
        _uiState.value = _uiState.value.copy(loanToDelete = loan)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(loanToDelete = null)
    }

    fun confirmDeleteLoan() {
        val loan = _uiState.value.loanToDelete ?: return
        viewModelScope.launch {
            repository.deleteLoan(loan)
            dismissDeleteDialog()
        }
    }

    fun loadLoanDetail(loanId: Long) {
        _selectedLoanId.value = loanId
    }

    class Factory(private val repository: FlowlyRepository) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoansViewModel(repository) as T
        }
    }
}
