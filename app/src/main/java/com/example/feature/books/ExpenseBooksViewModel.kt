package com.example.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.ExpenseBook
import com.example.domain.model.ExpenseBookSummary
import com.example.domain.model.TransactionDetail
import com.example.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpenseBooksUiState(
    val summaries: List<ExpenseBookSummary> = emptyList(),
    val activeBooks: List<ExpenseBook> = emptyList(),
    val archivedBooks: List<ExpenseBook> = emptyList(),
    val userSettings: UserSettings? = null,
    val selectedBookDetail: ExpenseBook? = null,
    val selectedBookTransactions: List<TransactionDetail> = emptyList(),
    val isLoading: Boolean = false
)

class ExpenseBooksViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _selectedBookId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<ExpenseBooksUiState> = combine(
        repository.getExpenseBookSummaries(),
        repository.getUserSettings(),
        _selectedBookId
    ) { summaries, settings, selectedId ->
        val active = summaries.filter { !it.book.isArchived }.map { it.book }
        val archived = summaries.filter { it.book.isArchived }.map { it.book }
        val selectedBook = summaries.find { it.book.id == selectedId }?.book

        ExpenseBooksUiState(
            summaries = summaries,
            activeBooks = active,
            archivedBooks = archived,
            userSettings = settings,
            selectedBookDetail = selectedBook,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseBooksUiState(isLoading = true)
    )

    fun selectBook(bookId: Long) {
        _selectedBookId.value = bookId
    }

    fun getTransactionsForBook(bookId: Long) = repository.getTransactionsForBook(bookId)

    fun addBook(
        name: String,
        description: String,
        iconName: String,
        colorHex: String,
        budget: Double,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            val id = repository.addExpenseBook(
                ExpenseBook(
                    name = name.trim(),
                    description = description.trim(),
                    iconName = iconName,
                    colorHex = colorHex,
                    budget = budget,
                    isDefault = isDefault
                )
            )
            if (isDefault) {
                repository.setDefaultExpenseBook(id)
            }
        }
    }

    fun updateBook(book: ExpenseBook, setAsDefault: Boolean) {
        viewModelScope.launch {
            repository.updateExpenseBook(book)
            if (setAsDefault) {
                repository.setDefaultExpenseBook(book.id)
            }
        }
    }

    fun deleteBook(book: ExpenseBook) {
        viewModelScope.launch {
            repository.deleteExpenseBook(book)
        }
    }

    fun toggleArchive(book: ExpenseBook) {
        viewModelScope.launch {
            repository.setExpenseBookArchived(book.id, !book.isArchived)
        }
    }

    fun setDefaultBook(bookId: Long) {
        viewModelScope.launch {
            repository.setDefaultExpenseBook(bookId)
        }
    }

    class Factory(private val repository: FlowlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExpenseBooksViewModel(repository) as T
        }
    }
}
