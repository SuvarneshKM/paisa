package com.example.paisa.parser

import com.example.paisa.util.DateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Year
import java.time.ZoneId
import java.time.ZonedDateTime

object SmsDateParser {

    private val MONTH_MAP = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
        "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
        "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )

    // Patterns:
    // 26-Oct-26, 26-OCT-2026, 26/Oct/2026, 26 Oct 2026, 26Oct26, 26OCT2026
    private val DATE_WITH_MONTH_NAME = Regex(
        """\b(\d{1,2})[-/ ]?([A-Za-z]{3})[-/ ]?(\d{2,4})?\b""",
        RegexOption.IGNORE_CASE
    )

    // 26/10/26, 26/10/2026, 26-10-26, 26-10-2026
    private val DATE_NUMERIC = Regex(
        """\b(\d{1,2})[-/](\d{1,2})[-/](\d{2,4})\b"""
    )

    // ISO: 2026-10-26, 2026/10/26
    private val DATE_ISO = Regex(
        """\b(\d{4})[-/](\d{1,2})[-/](\d{1,2})\b"""
    )

    // 14:35, 14:35:20, 1:45 PM, 01:45 PM, 1:45pm, 12:30 AM
    private val TIME_REGEX = Regex(
        """\b(\d{1,2}):(\d{2})(?::(\d{2}))?\s*(am|pm|AM|PM)?\b"""
    )

    fun parseTime(text: String): LocalTime? {
        val match = TIME_REGEX.find(text) ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        val second = match.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
        val amPm = match.groupValues.getOrNull(4)?.lowercase()

        if (amPm != null && amPm.isNotEmpty()) {
            if (amPm == "pm" && hour in 1..11) {
                hour += 12
            } else if (amPm == "am" && hour == 12) {
                hour = 0
            }
        }

        return if (hour in 0..23 && minute in 0..59 && second in 0..59) {
            LocalTime.of(hour, minute, second)
        } else {
            null
        }
    }

    fun parseDateOnly(text: String, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate? {
        val baseYear = Year.now(zoneId).value

        // 1. Try date with month name (e.g., 26-Oct-26, 26Oct26)
        val monthNameMatch = DATE_WITH_MONTH_NAME.find(text)
        if (monthNameMatch != null) {
            val day = monthNameMatch.groupValues[1].toIntOrNull()
            val monthStr = monthNameMatch.groupValues[2].lowercase()
            val month = MONTH_MAP[monthStr]
            val yearRaw = monthNameMatch.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }?.toIntOrNull()

            val year = when {
                yearRaw == null -> baseYear
                yearRaw < 100 -> 2000 + yearRaw
                else -> yearRaw
            }

            if (day != null && month != null && day in 1..31) {
                try {
                    return LocalDate.of(year, month, day)
                } catch (_: Exception) {
                    // Invalid date components (e.g. 31 Feb)
                }
            }
        }

        // 2. Try ISO date (e.g. 2026-10-26)
        val isoMatch = DATE_ISO.find(text)
        if (isoMatch != null) {
            val year = isoMatch.groupValues[1].toIntOrNull()
            val month = isoMatch.groupValues[2].toIntOrNull()
            val day = isoMatch.groupValues[3].toIntOrNull()
            if (year != null && month != null && day != null && month in 1..12 && day in 1..31) {
                try {
                    return LocalDate.of(year, month, day)
                } catch (_: Exception) {
                }
            }
        }

        // 3. Try standard numeric date (e.g. 26/10/26, 20-10-2026)
        val numericMatch = DATE_NUMERIC.find(text)
        if (numericMatch != null) {
            val day = numericMatch.groupValues[1].toIntOrNull()
            val month = numericMatch.groupValues[2].toIntOrNull()
            val yearRaw = numericMatch.groupValues[3].toIntOrNull()

            if (day != null && month != null && yearRaw != null && month in 1..12 && day in 1..31) {
                val year = if (yearRaw < 100) 2000 + yearRaw else yearRaw
                try {
                    return LocalDate.of(year, month, day)
                } catch (_: Exception) {
                }
            }
        }

        return null
    }

    /**
     * Parses transaction timestamp from bank SMS text.
     * When reliable bank date is found, combines it with the bank's time or the time from
     * [fallbackTimestamp]. If bank date is absent or unreliable, falls back to [fallbackTimestamp].
     */
    fun parseDate(
        text: String,
        fallbackTimestamp: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val normalizedFallback = fallbackTimestamp?.let { DateFormatter.normalizeTimestamp(it) }
        val parsedDate = parseDateOnly(text, zoneId)
        val parsedTime = parseTime(text)

        if (parsedDate != null) {
            val time = when {
                parsedTime != null -> parsedTime
                normalizedFallback != null -> {
                    Instant.ofEpochMilli(normalizedFallback)
                        .atZone(zoneId)
                        .toLocalTime()
                }
                else -> LocalTime.of(12, 0, 0)
            }

            val zonedDateTime = ZonedDateTime.of(parsedDate, time, zoneId)
            val epochMillis = zonedDateTime.toInstant().toEpochMilli()

            // Reliability validation:
            // Reject dates absurdly in the past (before 2000) or > 24 hours into the future.
            val now = System.currentTimeMillis()
            val maxAllowedFuture = now + (24 * 60 * 60 * 1000L) // 24h skew margin
            val minRealisticYear = 2000

            if (parsedDate.year >= minRealisticYear && epochMillis <= maxAllowedFuture) {
                return epochMillis
            } else {
                // Unreliable bank date, safely fallback
                return normalizedFallback
            }
        }

        return normalizedFallback
    }
}
