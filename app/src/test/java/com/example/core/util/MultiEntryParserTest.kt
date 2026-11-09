package com.example.core.util

import com.example.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiEntryParserTest {

    @Test
    fun splitsTwoEntriesAndSharesTrailingDay() {
        val r = MultiEntryParser.parse("chai 40 and auto 120 yesterday")
        assertEquals(2, r.size)
        assertEquals("Chai", r[0].description); assertEquals(40.0, r[0].amount, 0.0); assertEquals(1, r[0].daysAgo)
        assertEquals("Auto", r[1].description); assertEquals(120.0, r[1].amount, 0.0); assertEquals(1, r[1].daysAgo)
    }

    @Test
    fun singleEntryIsNotMulti() {
        assertTrue(MultiEntryParser.parse("dinner at dhaba 450").isEmpty())
    }

    @Test
    fun wordsWithoutAmountJoinTheNextPiece() {
        val r = MultiEntryParser.parse("bread and milk 60, petrol 500")
        assertEquals(2, r.size)
        assertEquals(60.0, r[0].amount, 0.0)
        assertEquals(500.0, r[1].amount, 0.0)
    }

    @Test
    fun incomeIsDetected() {
        val r = MultiEntryParser.parse("salary 50000 and chai 20")
        assertEquals(TransactionType.INCOME, r[0].type)
        assertEquals(TransactionType.EXPENSE, r[1].type)
    }

    @Test
    fun thousandsSeparatorsAndK() {
        val r = MultiEntryParser.parse("rent 12,000 and wifi 1.2k")
        assertEquals(12000.0, r[0].amount, 0.0)
        assertEquals(1200.0, r[1].amount, 0.0)
    }
}
