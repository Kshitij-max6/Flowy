package com.example.core.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodRangesTest {
    private fun at(y: Int, m: Int, d: Int, h: Int = 12) =
        Calendar.getInstance().apply { clear(); set(y, m, d, h, 0, 0) }.timeInMillis

    @Test fun thisMonthIsTheCalendarMonthNotThirtyDays() {
        val now = at(2026, Calendar.NOVEMBER, 9)
        val r = PeriodRanges.thisMonth(now)
        assertEquals(at(2026, Calendar.NOVEMBER, 1, 0), r.start)
        assertEquals(at(2026, Calendar.DECEMBER, 1, 0), r.end)
        assertTrue(at(2026, Calendar.NOVEMBER, 1, 0) in r)
        assertFalse(at(2026, Calendar.OCTOBER, 9) in r)      // was wrongly included by the rolling 30 days
        assertFalse(at(2026, Calendar.OCTOBER, 31, 23) in r)
    }

    @Test fun lastMonthIsTheWholePreviousMonth() {
        val now = at(2026, Calendar.NOVEMBER, 9)
        val r = PeriodRanges.lastMonth(now)
        assertTrue(at(2026, Calendar.OCTOBER, 1, 0) in r)
        assertTrue(at(2026, Calendar.OCTOBER, 31, 23) in r)
        assertFalse(at(2026, Calendar.NOVEMBER, 1, 0) in r)
        assertFalse(at(2026, Calendar.SEPTEMBER, 30) in r)
    }

    @Test fun januaryRollsBackIntoPreviousYear() {
        val r = PeriodRanges.lastMonth(at(2027, Calendar.JANUARY, 15))
        assertEquals(at(2026, Calendar.DECEMBER, 1, 0), r.start)
    }
}
