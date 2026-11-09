package com.example.core.util

import java.util.Calendar

/**
 * Calendar-month boundaries in the device time zone. All ranges are [start, end): start inclusive, end exclusive.
 * "This month" is 1st 00:00 of the current month up to 1st 00:00 of the next month; "last month" is the whole previous month.
 */
object PeriodRanges {

    data class Range(val start: Long, val end: Long) {
        val length: Long get() = end - start
        operator fun contains(t: Long) = t >= start && t < end
    }

    /** Start of the month containing [now], moved by [monthOffset] months (0 = this month, -1 = last month, 1 = next). */
    fun monthStart(now: Long, monthOffset: Int = 0): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, monthOffset)
        }
        return cal.timeInMillis
    }

    fun thisMonth(now: Long) = Range(monthStart(now, 0), monthStart(now, 1))
    fun lastMonth(now: Long) = Range(monthStart(now, -1), monthStart(now, 0))
}
