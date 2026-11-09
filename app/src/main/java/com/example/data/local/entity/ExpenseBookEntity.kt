package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ExpenseBook

@Entity(tableName = "expense_books")
data class ExpenseBookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconName: String = "Wallet",
    val colorHex: String = "#E5C378",
    val budget: Double = 0.0,
    val isArchived: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ExpenseBook = ExpenseBook(
        id = id,
        name = name,
        description = description,
        iconName = iconName,
        colorHex = colorHex,
        budget = budget,
        isArchived = isArchived,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(book: ExpenseBook): ExpenseBookEntity = ExpenseBookEntity(
            id = book.id,
            name = book.name,
            description = book.description,
            iconName = book.iconName,
            colorHex = book.colorHex,
            budget = book.budget,
            isArchived = book.isArchived,
            isDefault = book.isDefault,
            createdAt = book.createdAt,
            updatedAt = book.updatedAt
        )
    }
}
