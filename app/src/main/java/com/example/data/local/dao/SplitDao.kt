package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.SplitExpenseEntity
import com.example.data.local.entity.SplitFriendEntity
import com.example.data.local.entity.SplitSettlementEntity
import com.example.data.local.entity.SplitShareEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {

    @Query("SELECT * FROM split_friends ORDER BY name COLLATE NOCASE ASC")
    fun getFriends(): Flow<List<SplitFriendEntity>>

    @Query("SELECT * FROM split_expenses ORDER BY dateTime DESC, id DESC")
    fun getExpenses(): Flow<List<SplitExpenseEntity>>

    @Query("SELECT * FROM split_shares")
    fun getShares(): Flow<List<SplitShareEntity>>

    @Query("SELECT * FROM split_settlements ORDER BY dateTime DESC, id DESC")
    fun getSettlements(): Flow<List<SplitSettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: SplitFriendEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: SplitExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<SplitShareEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SplitSettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(list: List<SplitFriendEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(list: List<SplitExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(list: List<SplitSettlementEntity>)

    @Query("DELETE FROM split_shares WHERE expenseId = :expenseId")
    suspend fun deleteSharesForExpense(expenseId: Long)

    @Query("DELETE FROM split_expenses WHERE id = :expenseId")
    suspend fun deleteExpenseRow(expenseId: Long)

    @Query("DELETE FROM split_settlements WHERE id = :id")
    suspend fun deleteSettlement(id: Long)

    @Query("DELETE FROM split_settlements WHERE friendId = :friendId")
    suspend fun deleteSettlementsForFriend(friendId: Long)

    @Query("DELETE FROM split_shares WHERE friendId = :friendId")
    suspend fun deleteSharesForFriend(friendId: Long)

    @Query("DELETE FROM split_friends WHERE id = :friendId")
    suspend fun deleteFriendRow(friendId: Long)

    @Query("SELECT id FROM split_expenses WHERE paidByFriendId = :friendId")
    suspend fun expenseIdsPaidBy(friendId: Long): List<Long>

    @Transaction
    suspend fun addExpenseWithShares(expense: SplitExpenseEntity, shares: List<SplitShareEntity>): Long {
        val id = insertExpense(expense)
        insertShares(shares.map { it.copy(id = 0, expenseId = id) })
        return id
    }

    @Transaction
    suspend fun deleteExpense(expenseId: Long) {
        deleteSharesForExpense(expenseId)
        deleteExpenseRow(expenseId)
    }

    @Transaction
    suspend fun deleteFriendCompletely(friendId: Long) {
        for (id in expenseIdsPaidBy(friendId)) deleteExpense(id)
        deleteSharesForFriend(friendId)
        deleteSettlementsForFriend(friendId)
        deleteFriendRow(friendId)
    }
}
