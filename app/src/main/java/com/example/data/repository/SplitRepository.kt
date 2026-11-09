package com.example.data.repository

import com.example.data.local.dao.SplitDao
import com.example.data.local.entity.SplitExpenseEntity
import com.example.data.local.entity.SplitFriendEntity
import com.example.data.local.entity.SplitSettlementEntity
import com.example.data.local.entity.SplitShareEntity
import com.example.domain.model.SplitExpense
import com.example.domain.model.SplitFriend
import com.example.domain.model.SplitMath
import com.example.domain.model.SplitMode
import com.example.domain.model.SplitOverview
import com.example.domain.model.SplitSettlement
import com.example.domain.model.SplitShare
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SplitRepository(private val dao: SplitDao) {

    fun overview(): Flow<SplitOverview> = combine(
        dao.getFriends(), dao.getExpenses(), dao.getShares(), dao.getSettlements()
    ) { friendRows, expenseRows, shareRows, settlementRows ->
        val friends = friendRows.map { SplitFriend(it.id, it.name) }
        val sharesByExpense = shareRows.groupBy { it.expenseId }
        val expenses = expenseRows.map { e ->
            SplitExpense(
                id = e.id,
                description = e.description,
                totalAmount = e.totalAmount,
                paidByFriendId = e.paidByFriendId,
                mode = runCatching { SplitMode.valueOf(e.splitType) }.getOrDefault(SplitMode.EQUAL),
                dateTime = e.dateTime,
                shares = (sharesByExpense[e.id] ?: emptyList()).map { SplitShare(it.friendId, it.shareAmount) }
            )
        }
        val settlements = settlementRows.map {
            SplitSettlement(it.id, it.friendId, it.amount, it.friendPaidMe, it.dateTime, it.note)
        }
        SplitOverview(friends, expenses, settlements, SplitMath.balances(friends, expenses, settlements))
    }

    suspend fun addFriend(name: String): Long =
        dao.insertFriend(SplitFriendEntity(name = name.trim()))

    suspend fun deleteFriend(friendId: Long) = dao.deleteFriendCompletely(friendId)

    suspend fun addExpense(
        description: String,
        total: Double,
        paidByFriendId: Long?,
        mode: SplitMode,
        shares: List<SplitShare>,
        dateTime: Long = System.currentTimeMillis()
    ): Long = dao.addExpenseWithShares(
        SplitExpenseEntity(
            description = description.trim(),
            totalAmount = total,
            paidByFriendId = paidByFriendId,
            splitType = mode.name,
            dateTime = dateTime
        ),
        shares.map { SplitShareEntity(expenseId = 0, friendId = it.friendId, shareAmount = it.amount) }
    )

    suspend fun deleteExpense(expenseId: Long) = dao.deleteExpense(expenseId)

    suspend fun settleUp(friendId: Long, amount: Double, friendPaidMe: Boolean, note: String = "") {
        dao.insertSettlement(
            SplitSettlementEntity(friendId = friendId, amount = amount, friendPaidMe = friendPaidMe, note = note)
        )
    }

    suspend fun deleteSettlement(id: Long) = dao.deleteSettlement(id)
}
