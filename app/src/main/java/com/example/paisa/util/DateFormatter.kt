package com.example.paisa.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateFormatter {

    // Threshold between unix seconds and unix milliseconds:
    // 10 digits in seconds maxes out at Nov 2286 (~9_999_999_999L).
    // Epoch millis for year 2020+ are >= 1_577_836_800_000L (13 digits).
    private const val MAX_SECONDS_THRESHOLD = 9_999_999_999L

    /**
     * Safely normalizes timestamps:
     * - If non-positive (<= 0), fall back to current time.
     * - If provided in seconds (<= 9,999,999,999L), converts to milliseconds.
     * - Otherwise returns timestamp unchanged as epoch milliseconds.
     */
    fun normalizeTimestamp(timestamp: Long): Long {
        return when {
            timestamp <= 0L -> System.currentTimeMillis()
            timestamp <= MAX_SECONDS_THRESHOLD -> timestamp * 1000L
            else -> timestamp
        }
    }

    /**
     * Formats timestamp into readable format like "27 Oct 2026 • 1:45 PM".
     * Respects the specified or default device local timezone.
     */
    fun formatDateTime(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.ENGLISH
    ): String {
        val safeTimestamp = normalizeTimestamp(timestamp)
        val zonedDateTime = Instant.ofEpochMilli(safeTimestamp).atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy • h:mm a", locale)
        return zonedDateTime.format(formatter)
    }

    /**
     * Formats date only, e.g. "27 Oct 2026".
     */
    fun formatDate(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.ENGLISH
    ): String {
        val safeTimestamp = normalizeTimestamp(timestamp)
        val zonedDateTime = Instant.ofEpochMilli(safeTimestamp).atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        return zonedDateTime.format(formatter)
    }

    /**
     * Formats time only, e.g. "1:45 PM".
     */
    fun formatTime(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.ENGLISH
    ): String {
        val safeTimestamp = normalizeTimestamp(timestamp)
        val zonedDateTime = Instant.ofEpochMilli(safeTimestamp).atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("h:mm a", locale)
        return zonedDateTime.format(formatter)
    }

    /**
     * Formats a label such as "Today, 1:45 PM", "Yesterday, 1:45 PM", or "27 Oct 2026 • 1:45 PM".
     */
    fun formatDateLabel(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.ENGLISH
    ): String {
        val safeTimestamp = normalizeTimestamp(timestamp)
        val zonedDateTime = Instant.ofEpochMilli(safeTimestamp).atZone(zoneId)
        val txDate = zonedDateTime.toLocalDate()
        val today = LocalDate.now(zoneId)
        val daysBetween = ChronoUnit.DAYS.between(txDate, today)

        val timeStr = zonedDateTime.format(DateTimeFormatter.ofPattern("h:mm a", locale))
        return when (daysBetween) {
            0L -> "Today, $timeStr"
            1L -> "Yesterday, $timeStr"
            else -> formatDateTime(safeTimestamp, zoneId, locale)
        }
    }

    /**
     * Groups transactions by relative time buckets:
     * "Today", "Yesterday", "This Week", "Earlier this Month", or "October 2026".
     */
    fun formatGroupDate(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.ENGLISH
    ): String {
        val safeTimestamp = normalizeTimestamp(timestamp)
        val zonedDateTime = Instant.ofEpochMilli(safeTimestamp).atZone(zoneId)
        val txDate = zonedDateTime.toLocalDate()
        val today = LocalDate.now(zoneId)
        val daysBetween = ChronoUnit.DAYS.between(txDate, today)

        return when {
            daysBetween == 0L -> "Today"
            daysBetween == 1L -> "Yesterday"
            daysBetween in 2..6 -> "This Week"
            txDate.month == today.month && txDate.year == today.year -> "Earlier this Month"
            else -> zonedDateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        }
    }

    /**
     * Formats current month and year for Overview header, e.g. "November 2026".
     */
    fun formatCurrentMonthYear(
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.ENGLISH
    ): String {
        return formatMonthYear(System.currentTimeMillis(), zoneId, locale)
    }

    /**
     * Formats month and year from a timestamp, e.g. "October 2026".
     */
    fun formatMonthYear(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.ENGLISH
    ): String {
        val safeTimestamp = normalizeTimestamp(timestamp)
        val zonedDateTime = Instant.ofEpochMilli(safeTimestamp).atZone(zoneId)
        return zonedDateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }
}
