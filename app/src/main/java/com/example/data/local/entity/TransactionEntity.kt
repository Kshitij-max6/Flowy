package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("dateTime"),
        Index("bookId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val categoryId: Long,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val bookId: Long = 1L,
    val dateTime: Long,
    val note: String,
    val attachmentUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        amount = amount,
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.EXPENSE },
        categoryId = categoryId,
        accountId = accountId,
        destinationAccountId = destinationAccountId,
        bookId = bookId,
        dateTime = dateTime,
        note = note,
        attachmentUri = attachmentUri,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(transaction: Transaction): TransactionEntity = TransactionEntity(
            id = transaction.id,
            amount = transaction.amount,
            type = transaction.type.name,
            categoryId = transaction.categoryId,
            accountId = transaction.accountId,
            destinationAccountId = transaction.destinationAccountId,
            bookId = transaction.bookId,
            dateTime = transaction.dateTime,
            note = transaction.note,
            attachmentUri = transaction.attachmentUri,
            createdAt = transaction.createdAt,
            updatedAt = transaction.updatedAt
        )
    }
}
