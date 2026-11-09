package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Frequency
import com.example.domain.model.RecurringTransaction
import com.example.domain.model.TransactionType

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val accountId: Long,
    val frequency: String,
    val nextExecutionDate: Long,
    val active: Boolean,
    val note: String
) {
    fun toDomain(): RecurringTransaction = RecurringTransaction(
        id = id,
        name = name,
        amount = amount,
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.EXPENSE },
        categoryId = categoryId,
        accountId = accountId,
        frequency = try { Frequency.valueOf(frequency) } catch (e: Exception) { Frequency.MONTHLY },
        nextExecutionDate = nextExecutionDate,
        active = active,
        note = note
    )

    companion object {
        fun fromDomain(recurring: RecurringTransaction): RecurringTransactionEntity =
            RecurringTransactionEntity(
                id = recurring.id,
                name = recurring.name,
                amount = recurring.amount,
                type = recurring.type.name,
                categoryId = recurring.categoryId,
                accountId = recurring.accountId,
                frequency = recurring.frequency.name,
                nextExecutionDate = recurring.nextExecutionDate,
                active = recurring.active,
                note = recurring.note
            )
    }
}
