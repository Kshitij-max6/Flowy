package com.example.feature.merchant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.MerchantSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class MerchantDetailUiState(
    val merchantName: String = "",
    val summary: MerchantSummary? = null,
    val defaultCurrency: String = "₹",
    val isLoading: Boolean = true
)

class MerchantDetailViewModel(
    private val repository: FlowlyRepository,
    private val merchantName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MerchantDetailUiState(merchantName = merchantName))
    val uiState: StateFlow<MerchantDetailUiState> = _uiState.asStateFlow()

    init {
        loadMerchantData()
    }

    private fun loadMerchantData() {
        viewModelScope.launch {
            combine(
                repository.getMerchantDetail(merchantName),
                repository.getUserSettings()
            ) { summary, settings ->
                _uiState.value = _uiState.value.copy(
                    summary = summary,
                    defaultCurrency = settings.defaultCurrency,
                    isLoading = false
                )
            }.collectLatest {}
        }
    }

    class Factory(
        private val repository: FlowlyRepository,
        private val merchantName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MerchantDetailViewModel(repository, merchantName) as T
        }
    }
}
