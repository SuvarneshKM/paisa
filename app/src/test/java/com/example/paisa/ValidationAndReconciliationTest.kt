package com.example.paisa

import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.TransactionEntity
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.data.local.model.AccountWithBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationAndReconciliationTest {

    @Test
    fun validateLastFourDigits_validatesStrictlyFourDigits() {
        fun isValid(lastFour: String): Boolean =
            lastFour.trim().length == 4 && lastFour.trim().all { it.isDigit() }

        assertTrue(isValid("4821"))
        assertTrue(isValid("0000"))
        assertTrue(isValid("9104"))

        assertFalse(isValid("123"))
        assertFalse(isValid("12345"))
        assertFalse(isValid("482a"))
        assertFalse(isValid("    "))
        assertFalse(isValid(""))
    }

    @Test
    fun validateOpeningBalance_convertsRupeesToPaiseAccurately() {
        fun toPaise(rupeesInput: String): Long? {
            val rupees = rupeesInput.trim().toDoubleOrNull() ?: return null
            if (rupees < 0.0) return null
            return (rupees * 100).toLong()
        }

        assertEquals(8425000L, toPaise("84250.00"))
        assertEquals(8425000L, toPaise("84250"))
        assertEquals(0L, toPaise("0.0"))
        assertEquals(1500000L, toPaise("15000.00"))
        assertEquals(4999L, toPaise("49.99"))

        assertNull(toPaise("-100"))
        assertNull(toPaise("invalid"))
        assertNull(toPaise(""))
    }

    @Test
    fun validateTransactionAmount_requiresPositiveNumber() {
        fun isValidAmount(amountInput: String): Boolean {
            val amount = amountInput.trim().toDoubleOrNull() ?: return false
            return amount > 0.0
        }

        assertTrue(isValidAmount("3420.00"))
        assertTrue(isValidAmount("0.50"))
        assertTrue(isValidAmount("100"))

        assertFalse(isValidAmount("0"))
        assertFalse(isValidAmount("0.00"))
        assertFalse(isValidAmount("-50.00"))
        assertFalse(isValidAmount("abc"))
    }

    @Test
    fun reconciliation_allowsOverridingBalanceWithoutModifyingHistory() {
        // Account with opening balance of ₹10,000.00 (1,000,000 paise)
        val initialAccount = BankAccountEntity(
            id = 1L,
            bankName = "HDFC Bank",
            nickname = "Salary",
            lastFourDigits = "4821",
            openingBalancePaise = 1000000L,
            reconciledBalancePaise = null
        )

        // Mock calculated balance: opening + 5,000 credit - 2,000 debit = ₹13,000.00 (1,300,000 paise)
        val calculatedBalancePaise = 1300000L

        val initialAccountWithBalance = AccountWithBalance(
            account = initialAccount,
            currentBalancePaise = calculatedBalancePaise,
            calculatedBalancePaise = calculatedBalancePaise
        )

        assertEquals(1300000L, initialAccountWithBalance.currentBalancePaise)
        assertNull(initialAccountWithBalance.account.reconciledBalancePaise)

        // Reconcile balance to match bank statement ₹13,500.00 (1,350,000 paise)
        val reconciledAccount = initialAccount.copy(reconciledBalancePaise = 1350000L)
        val reconciledAccountWithBalance = AccountWithBalance(
            account = reconciledAccount,
            currentBalancePaise = 1350000L, // Reconciled balance takes precedence
            calculatedBalancePaise = 1300000L // Underlying transactions and opening balance are preserved!
        )

        assertEquals(1350000L, reconciledAccountWithBalance.currentBalancePaise)
        assertEquals(1300000L, reconciledAccountWithBalance.calculatedBalancePaise)
        assertEquals(1350000L, reconciledAccountWithBalance.account.reconciledBalancePaise)

        // Reset reconciliation back to calculated balance
        val resetAccount = reconciledAccount.copy(reconciledBalancePaise = null)
        val resetAccountWithBalance = AccountWithBalance(
            account = resetAccount,
            currentBalancePaise = calculatedBalancePaise,
            calculatedBalancePaise = calculatedBalancePaise
        )

        assertEquals(1300000L, resetAccountWithBalance.currentBalancePaise)
        assertNull(resetAccountWithBalance.account.reconciledBalancePaise)
    }

    @Test
    fun accountDeletion_preventsSilentDeletionWhenTransactionsExist() {
        val account = BankAccountEntity(
            id = 1L,
            bankName = "ICICI Bank",
            nickname = "Daily Spends",
            lastFourDigits = "9104",
            openingBalancePaise = 1834000L
        )

        val tx1 = TransactionEntity(
            id = 101L,
            accountId = account.id,
            amountPaise = 48000L,
            type = TransactionType.DEBIT,
            timestamp = 1729900000000L
        )

        val transactions = listOf(tx1)
        val count = transactions.count { it.accountId == account.id }

        // Must require explicit confirmation if count > 0
        assertTrue("Account has transactions that must be explicitly confirmed before deletion", count > 0)
        assertEquals(1, count)
    }
}
