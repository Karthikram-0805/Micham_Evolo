package com.example.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    private val MONTH_SHORT_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.US)

    fun today(): String = LocalDate.now().format(DATE_FORMATTER)

    fun nowTime(): String = LocalTime.now().format(TIME_FORMATTER)

    fun currentMonthYearPrefix(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US))

    fun currentYear(): Int = LocalDate.now().year

    fun currentMonth(): Int = LocalDate.now().monthValue

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, DATE_FORMATTER)
            val today = LocalDate.now()
            when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> date.format(DISPLAY_DATE_FORMATTER)
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatMonthYear(month: Int, year: Int): String {
        val ym = YearMonth.of(year, month)
        return ym.format(MONTH_YEAR_FORMATTER)
    }

    fun formatMonthShort(month: Int): String {
        val ym = YearMonth.of(2026, month)
        return ym.format(MONTH_SHORT_FORMATTER)
    }

    data class CycleDateRange(
        val startDate: String,
        val endDate: String,
        val totalDays: Int,
        val elapsedDays: Int,
        val remainingDays: Int,
        val cycleMonth: Int,
        val cycleYear: Int
    )

    fun calculateCycle(
        cycleType: String, // "CALENDAR" or "CUSTOM"
        cycleDay: Int,      // e.g. 5
        referenceDate: LocalDate = LocalDate.now()
    ): CycleDateRange {
        if (cycleType == "CALENDAR" || cycleDay <= 1) {
            val yearMonth = YearMonth.from(referenceDate)
            val start = yearMonth.atDay(1)
            val end = yearMonth.atEndOfMonth()
            val totalDays = yearMonth.lengthOfMonth()
            val elapsed = ChronoUnit.DAYS.between(start, referenceDate).toInt() + 1
            val remaining = (totalDays - elapsed).coerceAtLeast(1)
            return CycleDateRange(
                startDate = start.format(DATE_FORMATTER),
                endDate = end.format(DATE_FORMATTER),
                totalDays = totalDays,
                elapsedDays = elapsed.coerceIn(1, totalDays),
                remainingDays = remaining,
                cycleMonth = referenceDate.monthValue,
                cycleYear = referenceDate.year
            )
        } else {
            // Custom cycle day, e.g. 5
            val currentDay = referenceDate.dayOfMonth
            val startYearMonth = if (currentDay >= cycleDay) {
                YearMonth.from(referenceDate)
            } else {
                YearMonth.from(referenceDate).minusMonths(1)
            }

            val validStartDay = cycleDay.coerceAtMost(startYearMonth.lengthOfMonth())
            val start = startYearMonth.atDay(validStartDay)

            val endYearMonth = startYearMonth.plusMonths(1)
            val validEndDay = (cycleDay - 1).coerceAtMost(endYearMonth.lengthOfMonth()).coerceAtLeast(1)
            val end = endYearMonth.atDay(validEndDay)

            val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
            val elapsed = ChronoUnit.DAYS.between(start, referenceDate).toInt() + 1
            val remaining = (totalDays - elapsed).coerceAtLeast(1)

            return CycleDateRange(
                startDate = start.format(DATE_FORMATTER),
                endDate = end.format(DATE_FORMATTER),
                totalDays = totalDays,
                elapsedDays = elapsed.coerceIn(1, totalDays),
                remainingDays = remaining,
                cycleMonth = startYearMonth.monthValue,
                cycleYear = startYearMonth.year
            )
        }
    }
}
