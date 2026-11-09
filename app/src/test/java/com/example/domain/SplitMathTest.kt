package com.example.domain

import com.example.domain.model.SplitExpense
import com.example.domain.model.SplitFriend
import com.example.domain.model.SplitMath
import com.example.domain.model.SplitMode
import com.example.domain.model.SplitSettlement
import com.example.domain.model.SplitShare
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitMathTest {
    @Test fun equalSplitKeepsEveryPaisa() {
        val s = SplitMath.equalShares(100.0, listOf(null, 1L, 2L))
        assertEquals(100.0, s.sumOf { it.amount }, 0.001)
        assertEquals(33.34, s[0].amount, 0.001)
    }

    @Test fun balancesAndSettlement() {
        val a = SplitFriend(1, "Asha")
        val dinner = SplitExpense(1, "Dinner", 600.0, null, SplitMode.EQUAL, 0L,
            listOf(SplitShare(null, 300.0), SplitShare(1, 300.0)))
        val cab = SplitExpense(2, "Cab", 200.0, 1, SplitMode.EQUAL, 0L,
            listOf(SplitShare(null, 100.0), SplitShare(1, 100.0)))
        // Asha owes 300, I owe 100 -> she owes me 200
        assertEquals(200.0, SplitMath.balances(listOf(a), listOf(dinner, cab), emptyList())[0].amount, 0.001)
        val paid = SplitSettlement(1, 1, 200.0, true, 0L, "")
        assertEquals(0.0, SplitMath.balances(listOf(a), listOf(dinner, cab), listOf(paid))[0].amount, 0.001)
    }

    @Test fun percentSplitAbsorbsRounding() {
        val s = SplitMath.percentShares(100.0, listOf(null to 33.33, 1L to 33.33, 2L to 33.34))
        assertEquals(100.0, s.sumOf { it.amount }, 0.001)
    }
}
