package com.example.feature.creditcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Account
import com.example.domain.model.CreditCard
import com.example.domain.model.CreditCardStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CreditCardsUiState(
    val cards: List<CreditCard> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currency: String = "₹",
    val showAddEditDialog: Boolean = false,
    val editingCard: CreditCard? = null,
    val showPaymentDialog: Boolean = false,
    val payingCard: CreditCard? = null,
    val cardToDelete: CreditCard? = null,
    // Add/Edit form state
    val nameInput: String = "",
    val bankInput: String = "",
    val last4Input: String = "",
    val limitInput: String = "",
    val balanceInput: String = "",
    val billingCycleInput: String = "15",
    val dueDayInput: String = "5",
    val selectedAccountId: Long? = null,
    val cardColorHex: String = "#1E293B",
    // Payment form state
    val paymentAmountInput: String = "",
    val paymentSourceAccountId: Long? = null,
    val paymentNoteInput: String = "Credit Card Bill Repayment",
    val errorMessage: String? = null
)

class CreditCardsViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardsUiState())

    val uiState: StateFlow<CreditCardsUiState> = combine(
        repository.getAllCreditCards(),
        repository.getAllAccounts(),
        repository.getUserSettings(),
        _uiState
    ) { cards, accounts, settings, formState ->
        formState.copy(
            cards = cards,
            accounts = accounts,
            currency = settings.defaultCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreditCardsUiState()
    )

    fun openAddCardDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingCard = null,
            nameInput = "",
            bankInput = "",
            last4Input = "",
            limitInput = "",
            balanceInput = "0",
            billingCycleInput = "15",
            dueDayInput = "5",
            selectedAccountId = null,
            cardColorHex = "#1E293B",
            errorMessage = null
        )
    }

    fun openEditCardDialog(card: CreditCard) {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingCard = card,
            nameInput = card.name,
            bankInput = card.bankName,
            last4Input = card.cardNumberMasked.replace("•", "").trim(),
            limitInput = if (card.creditLimit > 0) card.creditLimit.toLong().toString() else "",
            balanceInput = card.outstandingBalance.toString(),
            billingCycleInput = card.billingCycleDay.toString(),
            dueDayInput = card.paymentDueDay.toString(),
            selectedAccountId = card.linkedAccountId,
            cardColorHex = card.cardColorHex,
            errorMessage = null
        )
    }

    fun dismissAddEditDialog() {
        _uiState.value = _uiState.value.copy(showAddEditDialog = false, editingCard = null)
    }

    fun onNameChanged(v: String) { _uiState.value = _uiState.value.copy(nameInput = v, errorMessage = null) }
    fun onBankChanged(v: String) { _uiState.value = _uiState.value.copy(bankInput = v, errorMessage = null) }
    fun onLast4Changed(v: String) {
        val digits = v.filter { it.isDigit() }.take(4)
        _uiState.value = _uiState.value.copy(last4Input = digits, errorMessage = null)
    }
    fun onLimitChanged(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(limitInput = cleaned, errorMessage = null)
    }
    fun onBalanceChanged(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(balanceInput = cleaned, errorMessage = null)
    }
    fun onBillingCycleChanged(v: String) {
        val cleaned = v.filter { it.isDigit() }.take(2)
        _uiState.value = _uiState.value.copy(billingCycleInput = cleaned)
    }
    fun onDueDayChanged(v: String) {
        val cleaned = v.filter { it.isDigit() }.take(2)
        _uiState.value = _uiState.value.copy(dueDayInput = cleaned)
    }
    fun onLinkedAccountChanged(accId: Long?) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accId)
    }
    fun onCardColorChanged(hex: String) {
        _uiState.value = _uiState.value.copy(cardColorHex = hex)
    }

    fun saveCard() {
        val s = _uiState.value
        val name = s.nameInput.trim()
        if (name.isBlank()) {
            _uiState.value = s.copy(errorMessage = "Please enter a card name")
            return
        }

        val limit = s.limitInput.toDoubleOrNull() ?: 0.0
        if (limit <= 0) {
            _uiState.value = s.copy(errorMessage = "Please enter a valid credit limit")
            return
        }

        val balance = s.balanceInput.toDoubleOrNull() ?: 0.0
        val cycle = (s.billingCycleInput.toIntOrNull() ?: 15).coerceIn(1, 31)
        val due = (s.dueDayInput.toIntOrNull() ?: 5).coerceIn(1, 31)
        val masked = if (s.last4Input.isNotBlank()) "•••• ${s.last4Input}" else "•••• ••••"

        viewModelScope.launch {
            if (s.editingCard != null) {
                val updated = s.editingCard.copy(
                    name = name,
                    bankName = s.bankInput.trim().ifBlank { "Bank" },
                    cardNumberMasked = masked,
                    creditLimit = limit,
                    outstandingBalance = balance,
                    billingCycleDay = cycle,
                    paymentDueDay = due,
                    linkedAccountId = s.selectedAccountId,
                    cardColorHex = s.cardColorHex
                )
                repository.updateCreditCard(updated)
            } else {
                val newCard = CreditCard(
                    id = 0,
                    name = name,
                    bankName = s.bankInput.trim().ifBlank { "Bank" },
                    cardNumberMasked = masked,
                    creditLimit = limit,
                    outstandingBalance = balance,
                    billingCycleDay = cycle,
                    paymentDueDay = due,
                    linkedAccountId = s.selectedAccountId,
                    currency = s.currency,
                    cardColorHex = s.cardColorHex,
                    status = CreditCardStatus.ACTIVE
                )
                repository.addCreditCard(newCard)
            }
            dismissAddEditDialog()
        }
    }

    // Payment Flow
    fun openPaymentDialog(card: CreditCard) {
        val defaultAccount = _uiState.value.accounts.firstOrNull { it.id != card.linkedAccountId }?.id
        _uiState.value = _uiState.value.copy(
            showPaymentDialog = true,
            payingCard = card,
            paymentAmountInput = if (card.outstandingBalance > 0) card.outstandingBalance.toString() else "",
            paymentSourceAccountId = defaultAccount,
            paymentNoteInput = "Payment for ${card.name}",
            errorMessage = null
        )
    }

    fun dismissPaymentDialog() {
        _uiState.value = _uiState.value.copy(showPaymentDialog = false, payingCard = null)
    }

    fun onPaymentAmountChanged(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(paymentAmountInput = cleaned, errorMessage = null)
    }

    fun onPaymentSourceAccountChanged(id: Long) {
        _uiState.value = _uiState.value.copy(paymentSourceAccountId = id)
    }

    fun onPaymentNoteChanged(v: String) {
        _uiState.value = _uiState.value.copy(paymentNoteInput = v)
    }

    fun recordPayment() {
        val s = _uiState.value
        val card = s.payingCard ?: return
        val amount = s.paymentAmountInput.toDoubleOrNull() ?: 0.0

        if (amount <= 0.0) {
            _uiState.value = s.copy(errorMessage = "Please enter a valid payment amount")
            return
        }

        val sourceAccId = s.paymentSourceAccountId
        if (sourceAccId == null) {
            _uiState.value = s.copy(errorMessage = "Please select a bank account to pay from")
            return
        }

        viewModelScope.launch {
            repository.recordCreditCardPayment(
                cardId = card.id,
                amount = amount,
                sourceAccountId = sourceAccId,
                note = s.paymentNoteInput
            )
            dismissPaymentDialog()
        }
    }

    // Delete
    fun requestDeleteCard(card: CreditCard) {
        _uiState.value = _uiState.value.copy(cardToDelete = card)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(cardToDelete = null)
    }

    fun confirmDeleteCard() {
        val card = _uiState.value.cardToDelete ?: return
        viewModelScope.launch {
            repository.deleteCreditCard(card)
            dismissDeleteDialog()
        }
    }

    class Factory(private val repository: FlowlyRepository) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreditCardsViewModel(repository) as T
        }
    }
}
