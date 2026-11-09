package com.example.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeSectionItem(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isVisible: Boolean
)

data class HomeCustomizeUiState(
    val sections: List<HomeSectionItem> = emptyList(),
    val isLoading: Boolean = true
)

class HomeCustomizeViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeCustomizeUiState())
    val uiState: StateFlow<HomeCustomizeUiState> = _uiState.asStateFlow()

    companion object {
        val ALL_AVAILABLE_SECTIONS = listOf(
            HomeSectionItem(
                id = "spending",
                title = "Spending Ring & Today's Target",
                description = "Animated spending progress ring and daily budget target",
                iconName = "DonutLarge",
                isVisible = true
            ),
            HomeSectionItem(
                id = "accounts",
                title = "Accounts & Net Liquid",
                description = "Quick view of all account balances and cards",
                iconName = "AccountBalance",
                isVisible = true
            ),
            HomeSectionItem(
                id = "recent",
                title = "Recent Transactions",
                description = "Latest money flow activity with quick edit access",
                iconName = "ReceiptLong",
                isVisible = true
            ),
            HomeSectionItem(
                id = "upcoming",
                title = "Upcoming Bills & Subscriptions",
                description = "Due dates and auto-renewing payments for next 7 days",
                iconName = "EventRepeat",
                isVisible = true
            ),
            HomeSectionItem(
                id = "insight",
                title = "Daily Smart Insight",
                description = "Actionable financial highlights and trend alerts",
                iconName = "Lightbulb",
                isVisible = true
            )
        )
    }

    init {
        loadCustomization()
    }

    private fun loadCustomization() {
        viewModelScope.launch {
            repository.getUserSettings().collectLatest { settings ->
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

                val itemMap = ALL_AVAILABLE_SECTIONS.associateBy { it.id }
                val orderedList = mutableListOf<HomeSectionItem>()

                // Add sections in saved order
                orderKeys.forEach { key ->
                    itemMap[key]?.let { item ->
                        orderedList.add(item.copy(isVisible = !hiddenSet.contains(key)))
                    }
                }

                // Add any missing sections at the end
                ALL_AVAILABLE_SECTIONS.forEach { item ->
                    if (orderedList.none { it.id == item.id }) {
                        orderedList.add(item.copy(isVisible = !hiddenSet.contains(item.id)))
                    }
                }

                _uiState.value = HomeCustomizeUiState(
                    sections = orderedList,
                    isLoading = false
                )
            }
        }
    }

    fun toggleVisibility(sectionId: String) {
        val currentSections = _uiState.value.sections.toMutableList()
        val index = currentSections.indexOfFirst { it.id == sectionId }
        if (index != -1) {
            val item = currentSections[index]
            currentSections[index] = item.copy(isVisible = !item.isVisible)
            _uiState.value = _uiState.value.copy(sections = currentSections)
            saveChanges(currentSections)
        }
    }

    fun moveUp(index: Int) {
        if (index > 0) {
            val list = _uiState.value.sections.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _uiState.value = _uiState.value.copy(sections = list)
            saveChanges(list)
        }
    }

    fun moveDown(index: Int) {
        val list = _uiState.value.sections.toMutableList()
        if (index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _uiState.value = _uiState.value.copy(sections = list)
            saveChanges(list)
        }
    }

    fun resetToDefault() {
        val defaultList = ALL_AVAILABLE_SECTIONS
        _uiState.value = _uiState.value.copy(sections = defaultList)
        saveChanges(defaultList)
    }

    private fun saveChanges(sections: List<HomeSectionItem>) {
        viewModelScope.launch {
            val orderString = sections.joinToString(",") { it.id }
            val hiddenString = sections.filter { !it.isVisible }.joinToString(",") { it.id }
            repository.updateHomeCustomization(orderString, hiddenString)
        }
    }

    class Factory(private val repository: FlowlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeCustomizeViewModel(repository) as T
        }
    }
}
