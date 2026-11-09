package com.example.feature.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SplitRepository
import com.example.domain.model.SplitMode
import com.example.domain.model.SplitOverview
import com.example.domain.model.SplitShare
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SplitViewModel(private val repository: SplitRepository) : ViewModel() {

    val overview: StateFlow<SplitOverview> = repository.overview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SplitOverview())

    fun addFriend(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addFriend(name) }
    }

    fun deleteFriend(friendId: Long) {
        viewModelScope.launch { repository.deleteFriend(friendId) }
    }

    fun addExpense(description: String, total: Double, paidBy: Long?, mode: SplitMode, shares: List<SplitShare>) {
        viewModelScope.launch { repository.addExpense(description, total, paidBy, mode, shares) }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch { repository.deleteExpense(id) }
    }

    /** [balance] > 0: the friend owes me, so they paid me. [balance] < 0: I pay them. */
    fun settleUp(friendId: Long, amount: Double, balance: Double) {
        if (amount <= 0.0) return
        viewModelScope.launch { repository.settleUp(friendId, amount, friendPaidMe = balance > 0) }
    }

    fun deleteSettlement(id: Long) {
        viewModelScope.launch { repository.deleteSettlement(id) }
    }

    class Factory(private val repository: SplitRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SplitViewModel(repository) as T
    }
}
