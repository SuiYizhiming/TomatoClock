package com.xuweikai.tomatoclock.domain.stats

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateKeys {
    private const val DATE_PATTERN = "yyyy-MM-dd"

    fun fromMillis(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String = formatter(timeZone).format(millis)

    fun today(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String = fromMillis(nowMillis, timeZone)

    fun plusDays(
        dateKey: String,
        days: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String {
        val calendar = calendarFor(dateKey, timeZone)
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return formatter(timeZone).format(calendar.time)
    }

    fun daysBetweenInclusive(
        startDateKey: String,
        endDateKey: String,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<String> {
        val start = calendarFor(startDateKey, timeZone)
        val end = calendarFor(endDateKey, timeZone)
        require(!start.after(end)) { "startDateKey must be on or before endDateKey." }

        val result = mutableListOf<String>()
        while (!start.after(end)) {
            result += formatter(timeZone).format(start.time)
            start.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }

    fun monthDateKeys(
        year: Int,
        month: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<String> {
        require(month in 1..12) { "month must be in 1..12." }
        val calendar = Calendar.getInstance(timeZone, Locale.US).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val dayCount = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (1..dayCount).map { day ->
            calendar.set(Calendar.DAY_OF_MONTH, day)
            formatter(timeZone).format(calendar.time)
        }
    }

    fun dayOfMonth(dateKey: String): Int = dateKey.substring(8, 10).toInt()

    private fun calendarFor(
        dateKey: String,
        timeZone: TimeZone,
    ): Calendar {
        val parsed = try {
            formatter(timeZone).parse(dateKey)
        } catch (error: ParseException) {
            null
        } ?: throw IllegalArgumentException("Invalid dateKey: $dateKey")

        return Calendar.getInstance(timeZone, Locale.US).apply {
            time = parsed
        }
    }

    private fun formatter(timeZone: TimeZone): SimpleDateFormat =
        SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            this.timeZone = timeZone
            isLenient = false
        }
}
