package com.xuweikai.tomatoclock.domain.stats

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DateKeysTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun daysBetweenInclusiveReturnsContinuousDateKeys() {
        val dateKeys = DateKeys.daysBetweenInclusive(
            startDateKey = "2026-05-28",
            endDateKey = "2026-06-01",
            timeZone = utc,
        )

        assertEquals(
            listOf(
                "2026-05-28",
                "2026-05-29",
                "2026-05-30",
                "2026-05-31",
                "2026-06-01",
            ),
            dateKeys,
        )
    }

    @Test
    fun monthDateKeysReturnsAllDaysInMonth() {
        val dateKeys = DateKeys.monthDateKeys(2026, 2, utc)

        assertEquals("2026-02-01", dateKeys.first())
        assertEquals("2026-02-28", dateKeys.last())
        assertEquals(28, dateKeys.size)
    }
}
