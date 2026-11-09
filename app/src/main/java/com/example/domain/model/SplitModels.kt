package com.example.domain.model

enum class SplitMode { EQUAL, AMOUNT, PERCENT }

data class SplitFriend(val id: Long, val name: String)

data class SplitShare(val friendId: Long?, val amount: Double)

data class SplitExpense(
    val id: Long,
    val description: String,
    val totalAmount: Double,
    val paidByFriendId: Long?,
    val mode: SplitMode,
    val dateTime: Long,
    val shares: List<SplitShare>
)

data class SplitSettlement(
    val id: Long,
    val friendId: Long,
    val amount: Double,
    val friendPaidMe: Boolean,
    val dateTime: Long,
    val note: String
)

/** Positive [amount]: the friend owes me. Negative: I owe the friend. */
data class FriendBalance(val friend: SplitFriend, val amount: Double)

data class SplitOverview(
    val friends: List<SplitFriend> = emptyList(),
    val expenses: List<SplitExpense> = emptyList(),
    val settlements: List<SplitSettlement> = emptyList(),
    val balances: List<FriendBalance> = emptyList()
) {
    val totalOwedToMe: Double get() = balances.filter { it.amount > 0.005 }.sumOf { it.amount }
    val totalIOwe: Double get() = balances.filter { it.amount < -0.005 }.sumOf { -it.amount }
}

/** Pure helpers so the maths is testable without Android. */
object SplitMath {

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0

    /** Equal split over [participantIds] (null = me); leftover paise go to the first participants. */
    fun equalShares(total: Double, participantIds: List<Long?>): List<SplitShare> {
        if (participantIds.isEmpty()) return emptyList()
        val cents = Math.round(total * 100.0)
        val base = cents / participantIds.size
        var remainder = (cents - base * participantIds.size).toInt()
        return participantIds.map { id ->
            val extra = if (remainder > 0) { remainder--; 1 } else 0
            SplitShare(id, (base + extra) / 100.0)
        }
    }

    /** [percents] must add up to 100 (within 0.01). Last participant absorbs rounding. */
    fun percentShares(total: Double, percents: List<Pair<Long?, Double>>): List<SplitShare> {
        if (percents.isEmpty()) return emptyList()
        var running = 0.0
        return percents.mapIndexed { i, (id, pct) ->
            val amt = if (i == percents.lastIndex) round2(total - running) else round2(total * pct / 100.0)
            running += amt
            SplitShare(id, amt)
        }
    }

    /** What each friend owes me (positive) or I owe them (negative), counting only my own ledger with them. */
    fun balances(
        friends: List<SplitFriend>,
        expenses: List<SplitExpense>,
        settlements: List<SplitSettlement>
    ): List<FriendBalance> = friends.map { f ->
        var net = 0.0
        for (e in expenses) {
            if (e.paidByFriendId == null) {
                // I paid: this friend owes me their share
                net += e.shares.filter { it.friendId == f.id }.sumOf { it.amount }
            } else if (e.paidByFriendId == f.id) {
                // This friend paid: I owe them my share
                net -= e.shares.filter { it.friendId == null }.sumOf { it.amount }
            }
        }
        for (s in settlements) {
            if (s.friendId != f.id) continue
            net += if (s.friendPaidMe) -s.amount else s.amount
        }
        FriendBalance(f, round2(net))
    }
}
