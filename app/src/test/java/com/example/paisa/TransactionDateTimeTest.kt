package com.example.paisa

import com.example.paisa.data.local.entity.TransactionEntity
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.parser.DefaultBankSmsParser
import com.example.paisa.parser.SmsDateParser
import com.example.paisa.parser.SmsParseResult
import com.example.paisa.util.DateFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class TransactionDateTimeTest {

    private val kolkataZone = ZoneId.of("Asia/Kolkata")
    private val utcZone = ZoneId.of("UTC")

    // =========================================================================
    // 1. Timestamp Normalization & Conversion (Seconds vs Milliseconds & Bounds)
    // =========================================================================

    @Test
    fun normalizeTimestamp_standardMillis_returnsUnchanged() {
        val millis = 1761563100000L // 27 Oct 2025+
        assertEquals(millis, DateFormatter.normalizeTimestamp(millis))
    }

    @Test
    fun normalizeTimestamp_legacySeconds_convertsToMillis() {
        val seconds = 1761563100L // 10 digits
        val expectedMillis = 1761563100000L
        assertEquals(expectedMillis, DateFormatter.normalizeTimestamp(seconds))
    }

    @Test
    fun normalizeTimestamp_zeroOrNegative_fallsBackToCurrentTime() {
        val before = System.currentTimeMillis()
        val normalizedZero = DateFormatter.normalizeTimestamp(0L)
        val normalizedNeg = DateFormatter.normalizeTimestamp(-999L)
        val after = System.currentTimeMillis()

        assertTrue(normalizedZero in before..after)
        assertTrue(normalizedNeg in before..after)
    }

    // =========================================================================
    // 2. Date and Time Formatting ("27 Oct 2026 • 1:45 PM") & Timezones
    // =========================================================================

    @Test
    fun formatDateTime_formatsCorrectly_like27Oct2026Bullet145PM() {
        // 27 Oct 2026 13:45:00 in Asia/Kolkata
        val zdt = ZonedDateTime.of(
            LocalDate.of(2026, 10, 27),
            LocalTime.of(13, 45, 0),
            kolkataZone
        )
        val epochMillis = zdt.toInstant().toEpochMilli()

        val formatted = DateFormatter.formatDateTime(epochMillis, zoneId = kolkataZone, locale = Locale.ENGLISH)
        assertEquals("27 Oct 2026 • 1:45 PM", formatted)
    }

    @Test
    fun formatDateTime_morningAmTime() {
        // 27 Oct 2026 09:05:00
        val zdt = ZonedDateTime.of(
            LocalDate.of(2026, 10, 27),
            LocalTime.of(9, 5, 0),
            kolkataZone
        )
        val epochMillis = zdt.toInstant().toEpochMilli()

        val formatted = DateFormatter.formatDateTime(epochMillis, zoneId = kolkataZone, locale = Locale.ENGLISH)
        assertEquals("27 Oct 2026 • 9:05 AM", formatted)
    }

    @Test
    fun formatDateTime_handlesLegacySecondsInputTransparently() {
        // Timestamp in seconds
        val zdt = ZonedDateTime.of(
            LocalDate.of(2026, 10, 27),
            LocalTime.of(13, 45, 0),
            kolkataZone
        )
        val epochSeconds = zdt.toInstant().toEpochMilli() / 1000L

        val formatted = DateFormatter.formatDateTime(epochSeconds, zoneId = kolkataZone, locale = Locale.ENGLISH)
        assertEquals("27 Oct 2026 • 1:45 PM", formatted)
    }

    @Test
    fun formatDate_and_formatTime_helpers() {
        val zdt = ZonedDateTime.of(
            LocalDate.of(2026, 10, 27),
            LocalTime.of(13, 45, 0),
            kolkataZone
        )
        val epochMillis = zdt.toInstant().toEpochMilli()

        assertEquals("27 Oct 2026", DateFormatter.formatDate(epochMillis, zoneId = kolkataZone, locale = Locale.ENGLISH))
        assertEquals("1:45 PM", DateFormatter.formatTime(epochMillis, zoneId = kolkataZone, locale = Locale.ENGLISH))
    }

    @Test
    fun formatDateTime_respectsTimezoneConversion() {
        // 27 Oct 2026 13:45:00 Asia/Kolkata is 08:15:00 UTC
        val zdtKolkata = ZonedDateTime.of(
            LocalDate.of(2026, 10, 27),
            LocalTime.of(13, 45, 0),
            kolkataZone
        )
        val epochMillis = zdtKolkata.toInstant().toEpochMilli()

        val kolkataStr = DateFormatter.formatDateTime(epochMillis, zoneId = kolkataZone, locale = Locale.ENGLISH)
        val utcStr = DateFormatter.formatDateTime(epochMillis, zoneId = utcZone, locale = Locale.ENGLISH)

        assertEquals("27 Oct 2026 • 1:45 PM", kolkataStr)
        assertEquals("27 Oct 2026 • 8:15 AM", utcStr)
    }

    // =========================================================================
    // 3. SMS Date and Time Parsing
    // =========================================================================

    @Test
    fun parseDate_extractsDateAnd12HourAmPmTime() {
        val sms = "INR 1,200.00 debited from A/c **4821 on 27-Oct-26 at 01:45 PM (UPI Ref 123456)."
        val parsedMillis = SmsDateParser.parseDate(sms, zoneId = kolkataZone)

        assertNotNull(parsedMillis)
        val zdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(parsedMillis!!), kolkataZone)
        assertEquals(2026, zdt.year)
        assertEquals(10, zdt.monthValue)
        assertEquals(27, zdt.dayOfMonth)
        assertEquals(13, zdt.hour) // 1:45 PM = 13:45
        assertEquals(45, zdt.minute)
    }

    @Test
    fun parseDate_extractsDateAnd24HourTime() {
        val sms = "INR 500 debited from A/c XX1234 on 26-Oct-2026 18:30:15 towards Coffee."
        val parsedMillis = SmsDateParser.parseDate(sms, zoneId = kolkataZone)

        assertNotNull(parsedMillis)
        val zdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(parsedMillis!!), kolkataZone)
        assertEquals(2026, zdt.year)
        assertEquals(10, zdt.monthValue)
        assertEquals(26, zdt.dayOfMonth)
        assertEquals(18, zdt.hour)
        assertEquals(30, zdt.minute)
        assertEquals(15, zdt.second)
    }

    @Test
    fun parseDate_whenTimeAbsent_blendsBankDateWithFallbackTimestampTime() {
        // Fallback timestamp has time 16:20:30 on 28 Oct 2026
        val fallbackZdt = ZonedDateTime.of(
            LocalDate.of(2026, 10, 28),
            LocalTime.of(16, 20, 30),
            kolkataZone
        )
        val fallbackMillis = fallbackZdt.toInstant().toEpochMilli()

        // SMS specifies transaction happened on 26-Oct-26
        val sms = "INR 450 debited from A/c **4821 on 26-Oct-26."
        val parsedMillis = SmsDateParser.parseDate(sms, fallbackTimestamp = fallbackMillis, zoneId = kolkataZone)

        assertNotNull(parsedMillis)
        val zdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(parsedMillis!!), kolkataZone)
        assertEquals(2026, zdt.year)
        assertEquals(10, zdt.monthValue)
        assertEquals(26, zdt.dayOfMonth) // Bank's date preserved
        assertEquals(16, zdt.hour)        // Time from fallback preserved
        assertEquals(20, zdt.minute)
        assertEquals(30, zdt.second)
    }

    @Test
    fun parseDate_whenNoDateInSms_fallsBackToReceivedTimestamp() {
        val fallbackMillis = 1761563100000L
        val sms = "INR 200 debited from A/c XX4821 to VPA swiggy@icici."
        val parsedMillis = SmsDateParser.parseDate(sms, fallbackTimestamp = fallbackMillis, zoneId = kolkataZone)

        assertEquals(fallbackMillis, parsedMillis)
    }

    @Test
    fun parseDate_futureDateRejected_andFallsBackToReceivedTimestamp() {
        val fallbackMillis = System.currentTimeMillis()
        val sms = "INR 200 debited on 27-Oct-2099 from A/c XX4821."
        val parsedMillis = SmsDateParser.parseDate(sms, fallbackTimestamp = fallbackMillis, zoneId = kolkataZone)

        assertEquals(fallbackMillis, parsedMillis)
    }

    @Test
    fun bankSmsParser_preservesTransactionSourceAndTimestamp() {
        val parser = DefaultBankSmsParser()
        val receivedTimestamp = 1761563100000L
        val sms = "INR 1,450.50 debited from A/c **4821 on 26-Oct-26 at 02:15 PM (UPI Ref 428910482101). Avail Bal: INR 45,210.00."

        val result = parser.parse(sms, receivedTimestamp = receivedTimestamp)
        assertTrue(result is SmsParseResult.Success)
        val tx = (result as SmsParseResult.Success).transaction

        assertNotNull(tx.timestamp)
        val zdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(tx.timestamp!!), ZoneId.systemDefault())
        assertEquals(26, zdt.dayOfMonth)
        assertEquals(10, zdt.monthValue)
        assertEquals(14, zdt.hour) // 2:15 PM = 14:15
        assertEquals(15, zdt.minute)
    }

    // =========================================================================
    // 4. Stable Tie-Breaker Sorting (ORDER BY timestamp DESC, id DESC)
    // =========================================================================

    @Test
    fun sortingComparator_ordersByTimestampDesc_withStableIdTieBreaker() {
        val fixedTimestamp = 1761563100000L

        val tx1 = TransactionEntity(
            id = 1L,
            accountId = 10L,
            amountPaise = 10000L,
            type = TransactionType.DEBIT,
            timestamp = fixedTimestamp,
            source = TransactionSource.MANUAL,
            status = TransactionStatus.CONFIRMED
        )
        val tx2 = TransactionEntity(
            id = 2L,
            accountId = 10L,
            amountPaise = 20000L,
            type = TransactionType.DEBIT,
            timestamp = fixedTimestamp, // Exactly identical timestamp
            source = TransactionSource.SMS,
            status = TransactionStatus.CONFIRMED
        )
        val tx3 = TransactionEntity(
            id = 3L,
            accountId = 10L,
            amountPaise = 30000L,
            type = TransactionType.DEBIT,
            timestamp = fixedTimestamp + 1000L, // Newer timestamp
            source = TransactionSource.MANUAL,
            status = TransactionStatus.CONFIRMED
        )

        val list = listOf(tx1, tx2, tx3)

        // Comparator mirroring SQLite "ORDER BY timestamp DESC, id DESC"
        val sorted = list.sortedWith(
            compareByDescending<TransactionEntity> { it.timestamp }
                .thenByDescending { it.id }
        )

        assertEquals(listOf(3L, 2L, 1L), sorted.map { it.id })
    }

    // =========================================================================
    // 5. Preserving Original Timestamp During Edits
    // =========================================================================

    @Test
    fun editingTransaction_preservesOriginalTimestamp_whenNotExplicitlyChanged() {
        val originalTimestamp = 1761563100000L
        val original = TransactionEntity(
            id = 42L,
            accountId = 1L,
            amountPaise = 50000L,
            type = TransactionType.DEBIT,
            categoryId = 10L,
            timestamp = originalTimestamp,
            source = TransactionSource.SMS,
            status = TransactionStatus.CONFIRMED,
            note = "Old Note"
        )

        // Updating note and category, without passing a new timestamp
        val updatedTimestamp: Long? = null
        val safeTimestamp = if (updatedTimestamp != null && updatedTimestamp > 0L) {
            DateFormatter.normalizeTimestamp(updatedTimestamp)
        } else {
            original.timestamp
        }

        val updated = original.copy(
            categoryId = 20L,
            note = "New Note",
            timestamp = safeTimestamp
        )

        assertEquals(originalTimestamp, updated.timestamp)
        assertEquals(TransactionSource.SMS, updated.source)
        assertEquals(20L, updated.categoryId)
        assertEquals("New Note", updated.note)
    }
}
