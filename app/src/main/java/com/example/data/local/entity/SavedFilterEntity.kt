package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.FilterDatePreset
import com.example.domain.model.FilterSource
import com.example.domain.model.SavedTransactionFilter
import com.example.domain.model.TransactionType

@Entity(tableName = "saved_transaction_filters")
data class SavedFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val datePreset: String,
    val customStartDate: Long?,
    val customEndDate: Long?,
    val transactionType: String?,
    val minAmount: Double?,
    val maxAmount: Double?,
    val categoryIds: String, // Comma-separated IDs: "1,2,3"
    val accountIds: String, // Comma-separated IDs: "1,2"
    val sourceFilter: String,
    val hasNote: Boolean,
    val searchQuery: String
) {
    fun toDomain(): SavedTransactionFilter = SavedTransactionFilter(
        id = id,
        name = name,
        datePreset = try { FilterDatePreset.valueOf(datePreset) } catch (e: Exception) { FilterDatePreset.ALL },
        customStartDate = customStartDate,
        customEndDate = customEndDate,
        transactionType = transactionType?.let {
            try { TransactionType.valueOf(it) } catch (e: Exception) { null }
        },
        minAmount = minAmount,
        maxAmount = maxAmount,
        categoryIds = if (categoryIds.isBlank()) emptyList() else categoryIds.split(",").mapNotNull { it.trim().toLongOrNull() },
        accountIds = if (accountIds.isBlank()) emptyList() else accountIds.split(",").mapNotNull { it.trim().toLongOrNull() },
        sourceFilter = try { FilterSource.valueOf(sourceFilter) } catch (e: Exception) { FilterSource.ALL },
        hasNote = hasNote,
        searchQuery = searchQuery
    )

    companion object {
        fun fromDomain(filter: SavedTransactionFilter): SavedFilterEntity = SavedFilterEntity(
            id = filter.id,
            name = filter.name,
            datePreset = filter.datePreset.name,
            customStartDate = filter.customStartDate,
            customEndDate = filter.customEndDate,
            transactionType = filter.transactionType?.name,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            categoryIds = filter.categoryIds.joinToString(","),
            accountIds = filter.accountIds.joinToString(","),
            sourceFilter = filter.sourceFilter.name,
            hasNote = filter.hasNote,
            searchQuery = filter.searchQuery
        )
    }
}
