package com.example.paisa

import com.example.paisa.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun formatPaise_formatsStandardRupeesCorrectly() {
        assertEquals("₹84,250.00", CurrencyFormatter.formatPaise(8425000L))
        assertEquals("₹18,340.00", CurrencyFormatter.formatPaise(1834000L))
        assertEquals("₹52,000.00", CurrencyFormatter.formatPaise(5200000L))
        assertEquals("₹2,800.00", CurrencyFormatter.formatPaise(280000L))
        assertEquals("₹0.00", CurrencyFormatter.formatPaise(0L))
    }

    @Test
    fun formatPaise_formatsSmallAndOddPaise() {
        assertEquals("₹0.50", CurrencyFormatter.formatPaise(50L))
        assertEquals("₹0.05", CurrencyFormatter.formatPaise(5L))
        assertEquals("₹1.25", CurrencyFormatter.formatPaise(125L))
    }

    @Test
    fun formatPaiseWithSign_appliesCorrectPrefix() {
        assertEquals("- ₹3,420.00", CurrencyFormatter.formatPaiseWithSign(342000L, isExpense = true))
        assertEquals("+ ₹95,000.00", CurrencyFormatter.formatPaiseWithSign(9500000L, isExpense = false))
    }

    @Test
    fun formatPaise_handlesNegativeAmounts() {
        assertEquals("-₹500.00", CurrencyFormatter.formatPaise(-50000L))
    }
}
