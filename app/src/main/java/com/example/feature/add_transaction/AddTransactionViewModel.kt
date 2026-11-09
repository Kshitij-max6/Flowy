package com.example.feature.add_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.ExpenseBook
import com.example.domain.model.FrequentTransactionPattern
import com.example.domain.model.QuickAddSuggestions
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.core.util.ParsedVoiceExpense
import com.example.core.util.ReceiptOcrScanner
import com.example.core.util.ScannedReceiptData
import android.content.Context
import android.net.Uri
import com.example.core.util.CategorySuggester
import com.example.core.util.MultiEntryParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** One editable row in the "several entries from one sentence" preview. */
data class EntryDraft(
    val key: Int,
    val description: String,
    val amountText: String,
    val type: TransactionType,
    val categoryId: Long?,
    val daysAgo: Int
)

data class AddTransactionUiState(
    val editId: Long = -1L,
    val amountInput: String = "",
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedCategoryId: Long? = null,
    val selectedParentCategoryId: Long? = null,
    val categorySearchQuery: String = "",
    val selectedAccountId: Long? = null,
    val selectedDestinationAccountId: Long? = null,
    val selectedBookId: Long? = null,
    val noteInput: String = "",
    val attachmentUri: String? = null,
    val selectedTimestamp: Long = System.currentTimeMillis(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val books: List<ExpenseBook> = emptyList(),
    val quickAddSuggestions: QuickAddSuggestions = QuickAddSuggestions(),
    val merchantSuggestions: List<String> = emptyList(),
    /** Set while the selected category came from auto-suggestion (drives the "✨ Suggested" label). */
    val suggestedCategoryId: Long? = null,
    /** How many separate entries the typed sentence contains (shows the button when >= 2). */
    val multiEntryCount: Int = 0,
    /** Non-null while the editable preview is open. */
    val multiDrafts: List<EntryDraft>? = null,
    val defaultCurrency: String = "₹",
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val isScanningReceipt: Boolean = false,
    val scannedReceiptMessage: String? = null,
    val showCustomCategoryDialog: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryIcon: String = "ShoppingBag",
    val newCategoryColor: String = "#10B981",
    val newCategoryParentId: Long? = null
)

sealed class AddTransactionEvent {
    object SaveSuccess : AddTransactionEvent()
}

class AddTransactionViewModel(
    private val repository: FlowlyRepository,
    private val editId: Long = -1L
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState(editId = editId))
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddTransactionEvent>()
    val eventFlow: SharedFlow<AddTransactionEvent> = _eventFlow.asSharedFlow()

    private var existingTransaction: Transaction? = null

    // Jobs are held so a new keystroke cancels the previous work instead of piling up listeners.
    private var merchantJob: Job? = null
    private var suggestJob: Job? = null

    /** True once the user (or a scan/voice/quick-fill) picked a category; auto-suggest then stays out of the way. */
    private var categoryChosenByUser = false

    private var historyCache: List<Transaction> = emptyList()

    init {
        loadInitialData()
        loadQuickAddSuggestions()
        loadHistoryForSuggestions()
    }

    private fun loadHistoryForSuggestions() {
        viewModelScope.launch {
            repository.getAllTransactions().collectLatest { all ->
                historyCache = all.sortedByDescending { it.dateTime }.take(1500)
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            combine(
                repository.getAllCategories(),
                repository.getAllAccounts(),
                repository.getActiveExpenseBooks(),
                repository.getUserSettings()
            ) { categories, accounts, books, settings ->
                val current = _uiState.value
                val defaultAccount = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
                val defaultCategory = categories.firstOrNull { it.type == CategoryType.EXPENSE }
                val defaultBook = books.firstOrNull { it.isDefault } ?: books.firstOrNull()

                val nextState = current.copy(
                    categories = categories,
                    accounts = accounts,
                    books = books,
                    defaultCurrency = settings.defaultCurrency,
                    selectedAccountId = current.selectedAccountId ?: defaultAccount?.id,
                    selectedCategoryId = current.selectedCategoryId ?: defaultCategory?.id,
                    selectedBookId = current.selectedBookId ?: defaultBook?.id ?: 1L
                )
                _uiState.value = nextState
            }.collect {}
        }

        if (editId > 0) {
            viewModelScope.launch {
                val tx = repository.getTransactionById(editId)
                if (tx != null) {
                    existingTransaction = tx
                    categoryChosenByUser = true
                    _uiState.value = _uiState.value.copy(
                        amountInput = if (tx.amount % 1.0 == 0.0) tx.amount.toLong().toString() else tx.amount.toString(),
                        selectedType = tx.type,
                        selectedCategoryId = tx.categoryId,
                        selectedAccountId = tx.accountId,
                        selectedDestinationAccountId = tx.destinationAccountId,
                        selectedBookId = tx.bookId,
                        noteInput = tx.note,
                        attachmentUri = tx.attachmentUri,
                        selectedTimestamp = tx.dateTime
                    )
                }
            }
        }
    }

    fun onBookSelected(bookId: Long) {
        _uiState.value = _uiState.value.copy(selectedBookId = bookId)
    }

    fun prefillFromVoice(parsed: ParsedVoiceExpense) {
        val formattedAmount = parsed.amount?.let { amt ->
            if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
        } ?: ""

        if (parsed.matchedCategory != null) categoryChosenByUser = true
        _uiState.value = _uiState.value.copy(
            suggestedCategoryId = if (parsed.matchedCategory != null) null else _uiState.value.suggestedCategoryId,
            amountInput = formattedAmount,
            selectedType = parsed.type,
            selectedCategoryId = parsed.matchedCategory?.id ?: _uiState.value.selectedCategoryId,
            selectedAccountId = parsed.matchedAccount?.id ?: _uiState.value.selectedAccountId,
            selectedBookId = parsed.matchedBook?.id ?: _uiState.value.selectedBookId,
            noteInput = parsed.merchantOrNote
        )
    }

    private fun loadQuickAddSuggestions() {
        viewModelScope.launch {
            repository.getQuickAddSuggestions().collectLatest { suggestions ->
                _uiState.value = _uiState.value.copy(quickAddSuggestions = suggestions)
            }
        }
    }

    fun onAmountChanged(amount: String) {
        val cleaned = amount.filter { it.isDigit() || it == '.' }
        if (cleaned.count { it == '.' } <= 1) {
            _uiState.value = _uiState.value.copy(amountInput = cleaned, errorMessage = null)
        }
    }

    fun onTypeSelected(type: TransactionType) {
        val currentCategories = _uiState.value.categories
        val targetCatType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        val firstMatchingCategory = currentCategories.firstOrNull { it.type == targetCatType }

        categoryChosenByUser = false
        _uiState.value = _uiState.value.copy(
            selectedType = type,
            selectedCategoryId = firstMatchingCategory?.id ?: _uiState.value.selectedCategoryId,
            suggestedCategoryId = null,
            errorMessage = null
        )
        scheduleCategorySuggestion()
    }

    fun onCategorySelected(categoryId: Long) {
        val cat = _uiState.value.categories.firstOrNull { it.id == categoryId }
        val parentId = if (cat?.parentCategoryId != null && cat.parentCategoryId > 0) cat.parentCategoryId else categoryId
        categoryChosenByUser = true
        suggestJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId,
            selectedParentCategoryId = parentId,
            suggestedCategoryId = null,
            errorMessage = null
        )
    }

    fun onParentCategoryFilterSelected(parentId: Long?) {
        _uiState.value = _uiState.value.copy(
            selectedParentCategoryId = parentId
        )
    }

    fun onCategorySearchChanged(query: String) {
        _uiState.value = _uiState.value.copy(categorySearchQuery = query)
    }

    fun onAccountSelected(accountId: Long) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId, errorMessage = null)
    }

    fun onDestinationAccountSelected(accountId: Long) {
        _uiState.value = _uiState.value.copy(selectedDestinationAccountId = accountId, errorMessage = null)
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(
            noteInput = note,
            multiEntryCount = if (editId > 0) 0 else MultiEntryParser.parse(note).size
        )

        // Cancel the previous listener first; previously each keystroke left one running forever.
        merchantJob?.cancel()
        merchantJob = viewModelScope.launch {
            repository.getSuggestedMerchants(note).collectLatest { suggestions ->
                _uiState.value = _uiState.value.copy(merchantSuggestions = suggestions)
            }
        }
        scheduleCategorySuggestion()
    }

    fun onMerchantSuggestionSelected(merchant: String) {
        merchantJob?.cancel()
        _uiState.value = _uiState.value.copy(
            noteInput = merchant,
            merchantSuggestions = emptyList()
        )
        scheduleCategorySuggestion()
    }

    /** Waits ~0.5 s after the last keystroke, then suggests a category + subcategory. */
    private fun scheduleCategorySuggestion() {
        suggestJob?.cancel()
        if (categoryChosenByUser) return
        suggestJob = viewModelScope.launch {
            delay(500)
            applyCategorySuggestion()
        }
    }

    private fun applyCategorySuggestion() {
        if (categoryChosenByUser) return
        val state = _uiState.value
        if (state.selectedType == TransactionType.TRANSFER) return

        val suggestion = CategorySuggester.suggest(
            text = state.noteInput,
            type = state.selectedType,
            categories = state.categories,
            history = historyCache
        )

        if (suggestion == null) {
            // Text no longer matches anything: drop our own earlier suggestion, keep user-visible state tidy.
            if (state.suggestedCategoryId != null) {
                val targetType = if (state.selectedType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                val fallback = state.categories.firstOrNull { it.type == targetType }
                _uiState.value = state.copy(
                    selectedCategoryId = fallback?.id ?: state.selectedCategoryId,
                    selectedParentCategoryId = null,
                    suggestedCategoryId = null
                )
            }
            return
        }

        val cat = state.categories.firstOrNull { it.id == suggestion.categoryId } ?: return
        val parentId = if (cat.parentCategoryId != null && cat.parentCategoryId > 0) cat.parentCategoryId else cat.id
        _uiState.value = state.copy(
            selectedCategoryId = cat.id,
            selectedParentCategoryId = parentId,
            suggestedCategoryId = cat.id
        )
    }

    fun onAttachmentChanged(uri: String?) {
        _uiState.value = _uiState.value.copy(
            attachmentUri = uri,
            scannedReceiptMessage = if (uri == null) null else _uiState.value.scannedReceiptMessage
        )
    }

    fun onReceiptImageSelected(context: Context, uri: Uri) {
        val uriString = uri.toString()
        _uiState.value = _uiState.value.copy(
            attachmentUri = uriString,
            isScanningReceipt = true,
            scannedReceiptMessage = "Scanning receipt details..."
        )

        viewModelScope.launch {
            try {
                val scanResult = ReceiptOcrScanner.scanReceipt(
                    context = context,
                    uri = uri,
                    categories = _uiState.value.categories
                )

                val current = _uiState.value
                val newAmount = if (scanResult.amount != null && scanResult.amount > 0) {
                    if (scanResult.amount % 1.0 == 0.0) scanResult.amount.toLong().toString() else scanResult.amount.toString()
                } else current.amountInput

                val newNote = if (current.noteInput.isBlank() && scanResult.merchant.isNotBlank()) {
                    scanResult.merchant
                } else current.noteInput

                val newCategoryId = scanResult.matchedCategoryId ?: current.selectedCategoryId
                if (scanResult.matchedCategoryId != null) categoryChosenByUser = true

                val status = if (scanResult.amount != null) {
                    "Scanned: ${scanResult.merchant} • ${current.defaultCurrency}${scanResult.amount}"
                } else if (scanResult.merchant.isNotBlank()) {
                    "Scanned merchant: ${scanResult.merchant}"
                } else {
                    "Receipt attached successfully"
                }

                _uiState.value = current.copy(
                    amountInput = newAmount,
                    noteInput = newNote,
                    selectedCategoryId = newCategoryId,
                    suggestedCategoryId = if (scanResult.matchedCategoryId != null) null else current.suggestedCategoryId,
                    isScanningReceipt = false,
                    scannedReceiptMessage = status
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanningReceipt = false,
                    scannedReceiptMessage = "Receipt attached"
                )
            }
        }
    }

    fun onQuickFillPattern(pattern: FrequentTransactionPattern) {
        val formattedAmount = if (pattern.amount % 1.0 == 0.0) pattern.amount.toLong().toString() else pattern.amount.toString()
        categoryChosenByUser = true
        _uiState.value = _uiState.value.copy(
            suggestedCategoryId = null,
            amountInput = formattedAmount,
            selectedType = pattern.type,
            selectedCategoryId = pattern.categoryId,
            selectedAccountId = pattern.accountId,
            noteInput = pattern.note.ifBlank { pattern.title }
        )
    }

    fun onDuplicateLastTransaction() {
        val last = _uiState.value.quickAddSuggestions.lastTransaction ?: return
        val tx = last.transaction
        val formattedAmount = if (tx.amount % 1.0 == 0.0) tx.amount.toLong().toString() else tx.amount.toString()
        categoryChosenByUser = true
        _uiState.value = _uiState.value.copy(
            suggestedCategoryId = null,
            amountInput = formattedAmount,
            selectedType = tx.type,
            selectedCategoryId = tx.categoryId,
            selectedAccountId = tx.accountId,
            selectedDestinationAccountId = tx.destinationAccountId,
            noteInput = tx.note
        )
    }

    fun onTimestampChanged(timestamp: Long) {
        _uiState.value = _uiState.value.copy(selectedTimestamp = timestamp)
    }

    fun showAddCategoryDialog(show: Boolean) {
        val parentId = _uiState.value.selectedParentCategoryId
        _uiState.value = _uiState.value.copy(
            showCustomCategoryDialog = show,
            newCategoryName = "",
            newCategoryIcon = "ShoppingBag",
            newCategoryColor = "#10B981",
            newCategoryParentId = parentId
        )
    }

    fun onNewCategoryNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(newCategoryName = name)
    }

    fun onNewCategoryIconChanged(icon: String) {
        _uiState.value = _uiState.value.copy(newCategoryIcon = icon)
    }

    fun onNewCategoryColorChanged(color: String) {
        _uiState.value = _uiState.value.copy(newCategoryColor = color)
    }

    fun onNewCategoryParentSelected(parentId: Long?) {
        _uiState.value = _uiState.value.copy(newCategoryParentId = parentId)
    }

    fun createCustomCategory() {
        val state = _uiState.value
        val name = state.newCategoryName.trim()
        if (name.isBlank()) return

        val catType = if (state.selectedType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        viewModelScope.launch {
            val newCategory = Category(
                name = name,
                iconName = state.newCategoryIcon,
                type = catType,
                colorHex = state.newCategoryColor,
                isCustom = true,
                parentCategoryId = state.newCategoryParentId
            )
            val newId = repository.addCategory(newCategory)
            _uiState.value = _uiState.value.copy(
                showCustomCategoryDialog = false,
                selectedCategoryId = newId,
                selectedParentCategoryId = state.newCategoryParentId ?: newId
            )
        }
    }

    fun applyVoiceInput(spokenText: String) {
        if (spokenText.isBlank()) return
        val current = _uiState.value
        val parsed = com.example.core.util.VoiceExpenseParser.parse(
            spokenText = spokenText,
            categories = current.categories,
            accounts = current.accounts,
            books = current.books
        )

        val newAmount = if (parsed.amount != null && parsed.amount > 0) {
            if (parsed.amount % 1.0 == 0.0) parsed.amount.toLong().toString() else parsed.amount.toString()
        } else current.amountInput

        val newType = parsed.type
        val newCategory = parsed.matchedCategory?.id ?: current.selectedCategoryId
        val newAccount = parsed.matchedAccount?.id ?: current.selectedAccountId
        val newBook = parsed.matchedBook?.id ?: current.selectedBookId
        val newNote = if (parsed.merchantOrNote.isNotBlank()) parsed.merchantOrNote else spokenText
        if (parsed.matchedCategory != null) categoryChosenByUser = true

        _uiState.value = current.copy(
            suggestedCategoryId = if (parsed.matchedCategory != null) null else current.suggestedCategoryId,
            amountInput = newAmount,
            selectedType = newType,
            selectedCategoryId = newCategory,
            selectedAccountId = newAccount,
            selectedBookId = newBook,
            noteInput = newNote,
            errorMessage = null
        )
    }

    fun applySpokenDescription(description: String) {
        if (description.isNotBlank()) {
            _uiState.value = _uiState.value.copy(noteInput = description)
            scheduleCategorySuggestion()
        }
    }

    // ------------------------------------------------------------------
    // Several entries from one sentence
    // ------------------------------------------------------------------

    fun openMultiPreview() {
        val state = _uiState.value
        val parsed = MultiEntryParser.parse(state.noteInput)
        if (parsed.size < 2) return
        val drafts = parsed.mapIndexed { i, e ->
            val wantedType = if (e.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
            val suggested = CategorySuggester.suggest(e.description, e.type, state.categories, historyCache)?.categoryId
            val fallback = state.categories.firstOrNull { it.type == wantedType }?.id
            EntryDraft(
                key = i,
                description = e.description,
                amountText = if (e.amount % 1.0 == 0.0) e.amount.toLong().toString() else e.amount.toString(),
                type = e.type,
                categoryId = suggested ?: fallback,
                daysAgo = e.daysAgo
            )
        }
        _uiState.value = state.copy(multiDrafts = drafts)
    }

    fun closeMultiPreview() {
        _uiState.value = _uiState.value.copy(multiDrafts = null)
    }

    fun updateDraft(key: Int, change: (EntryDraft) -> EntryDraft) {
        val drafts = _uiState.value.multiDrafts ?: return
        _uiState.value = _uiState.value.copy(multiDrafts = drafts.map { if (it.key == key) change(it) else it })
    }

    fun removeDraft(key: Int) {
        val drafts = _uiState.value.multiDrafts ?: return
        val left = drafts.filterNot { it.key == key }
        _uiState.value = _uiState.value.copy(multiDrafts = if (left.isEmpty()) null else left)
    }

    /** Saves every valid row as its own transaction. */
    fun saveMultiEntries() {
        val state = _uiState.value
        val drafts = state.multiDrafts ?: return
        val accountId = state.selectedAccountId
        if (accountId == null) {
            _uiState.value = state.copy(errorMessage = "Please select an account")
            return
        }
        val valid = drafts.filter { (it.amountText.toDoubleOrNull() ?: 0.0) > 0.0 }
        if (valid.isEmpty()) return
        val bookId = state.selectedBookId ?: 1L
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val now = System.currentTimeMillis()
                for (d in valid) {
                    val whenMillis = now - d.daysAgo * 24L * 60 * 60 * 1000
                    repository.addTransaction(
                        Transaction(
                            amount = d.amountText.toDouble(),
                            type = d.type,
                            categoryId = d.categoryId ?: 1L,
                            accountId = accountId,
                            bookId = bookId,
                            dateTime = whenMillis,
                            note = d.description.trim(),
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
                _eventFlow.emit(AddTransactionEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Error saving entries: ${e.localizedMessage}"
                )
            }
        }
    }

    fun saveTransaction() {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull()

        if (amount == null || amount <= 0.0) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid amount greater than 0")
            return
        }

        if (state.selectedAccountId == null) {
            _uiState.value = state.copy(errorMessage = "Please select an account")
            return
        }

        if (state.selectedType == TransactionType.TRANSFER) {
            if (state.selectedDestinationAccountId == null) {
                _uiState.value = state.copy(errorMessage = "Please select a destination account")
                return
            }
            if (state.selectedAccountId == state.selectedDestinationAccountId) {
                _uiState.value = state.copy(errorMessage = "Source and destination accounts must be different")
                return
            }
        }

        val categoryId = state.selectedCategoryId ?: 1L
        val bookId = state.selectedBookId ?: 1L

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            try {
                if (editId > 0 && existingTransaction != null) {
                    val updated = existingTransaction!!.copy(
                        amount = amount,
                        type = state.selectedType,
                        categoryId = categoryId,
                        accountId = state.selectedAccountId,
                        destinationAccountId = if (state.selectedType == TransactionType.TRANSFER) state.selectedDestinationAccountId else null,
                        bookId = bookId,
                        dateTime = state.selectedTimestamp,
                        note = state.noteInput.trim(),
                        attachmentUri = state.attachmentUri,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateTransaction(updated, existingTransaction!!)
                } else {
                    val newTx = Transaction(
                        amount = amount,
                        type = state.selectedType,
                        categoryId = categoryId,
                        accountId = state.selectedAccountId,
                        destinationAccountId = if (state.selectedType == TransactionType.TRANSFER) state.selectedDestinationAccountId else null,
                        bookId = bookId,
                        dateTime = state.selectedTimestamp,
                        note = state.noteInput.trim(),
                        attachmentUri = state.attachmentUri,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.addTransaction(newTx)
                }
                _eventFlow.emit(AddTransactionEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Error saving transaction: ${e.localizedMessage}"
                )
            }
        }
    }

    class Factory(
        private val repository: FlowlyRepository,
        private val editId: Long = -1L
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTransactionViewModel(repository, editId) as T
        }
    }
}
