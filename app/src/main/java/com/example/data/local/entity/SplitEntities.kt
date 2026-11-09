package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A person you share expenses with. "Me" is never stored; it is represented by a null friend id. */
@Entity(tableName = "split_friends")
data class SplitFriendEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "split_expenses")
data class SplitExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val totalAmount: Double,
    /** Friend id who paid, or null when I paid. */
    val paidByFriendId: Long? = null,
    /** EQUAL, AMOUNT or PERCENT: how the shares were entered. */
    val splitType: String = "EQUAL",
    val dateTime: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "split_shares",
    indices = [Index("expenseId"), Index("friendId")]
)
data class SplitShareEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    /** Friend id, or null for my own share. */
    val friendId: Long? = null,
    val shareAmount: Double
)

@Entity(tableName = "split_settlements", indices = [Index("friendId")])
data class SplitSettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val friendId: Long,
    val amount: Double,
    /** true: the friend paid me; false: I paid the friend. */
    val friendPaidMe: Boolean,
    val dateTime: Long = System.currentTimeMillis(),
    val note: String = ""
)
