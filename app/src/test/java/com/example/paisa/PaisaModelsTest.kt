package com.example.paisa

import com.example.paisa.data.local.PaisaDatabase
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.TransactionEntity
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.data.local.model.MonthlyTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaisaModelsTest {

    @Test
    fun bankAccount_correctlyConstructsAndMasksAccountNumber() {
        val account = BankAccountEntity(
            id = 1L,
            bankName = "HDFC Bank",
            nickname = "Salary Account",
            lastFourDigits = "4821",
            openingBalancePaise = 8425000L,
            reconciledBalancePaise = null,
            accountType = "SALARY",
            isPrimary = true
        )

        assertEquals("•••• 4821", account.maskedAccountNumber)
        assertEquals(8425000L, account.openingBalancePaise)
        assertNull(account.reconciledBalancePaise)
        assertTrue(account.isPrimary)
    }

    @Test
    fun transactionEntity_storesPaiseAndEnumsCorrectly() {
        val tx = TransactionEntity(
            id = 10L,
            accountId = 1L,
            amountPaise = 342000L,
            type = TransactionType.DEBIT,
            categoryId = 2L,
            timestamp = 1729900000000L,
            source = TransactionSource.SMS,
            status = TransactionStatus.CONFIRMED,
            reference = "UPI/MERCHANT/REF123",
            deduplicationKey = "SMS_HDFC_4821_3420_001",
            note = "Organic groceries"
        )

        assertEquals(342000L, tx.amountPaise)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals(TransactionSource.SMS, tx.source)
        assertEquals(TransactionStatus.CONFIRMED, tx.status)
        assertEquals("UPI/MERCHANT/REF123", tx.reference)
        assertEquals("SMS_HDFC_4821_3420_001", tx.deduplicationKey)
    }

    @Test
    fun monthlyTotals_computesNetSavingsCorrectly() {
        val totals = MonthlyTotals(
            totalIncomePaise = 12720000L, // ₹1,27,200.00
            totalExpensePaise = 2537900L, // ₹25,379.00
            transactionCount = 12
        )

        assertEquals(10182100L, totals.netSavingsPaise) // ₹1,01,821.00 net savings
        assertEquals(12, totals.transactionCount)
    }

    @Test
    fun migrationsStrategy_isConfigured() {
        assertNotNull(PaisaDatabase.ALL_MIGRATIONS)
        assertTrue(PaisaDatabase.ALL_MIGRATIONS.isNotEmpty())
        assertEquals(1, PaisaDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, PaisaDatabase.MIGRATION_1_2.endVersion)
    }
}
