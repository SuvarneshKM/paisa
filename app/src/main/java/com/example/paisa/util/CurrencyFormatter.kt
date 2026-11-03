package com.example.paisa.util

import java.text.DecimalFormat
import kotlin.math.abs

object CurrencyFormatter {
    private val formatter = DecimalFormat("#,##,##0.00")

    fun formatPaise(paise: Long, symbol: String = "₹"): String {
        val rupees = abs(paise) / 100.0
        val formatted = formatter.format(rupees)
        return if (paise < 0) "-$symbol$formatted" else "$symbol$formatted"
    }

    fun formatPaiseWithSign(paise: Long, isExpense: Boolean, symbol: String = "₹"): String {
        val rupees = abs(paise) / 100.0
        val formatted = formatter.format(rupees)
        return if (isExpense) "- $symbol$formatted" else "+ $symbol$formatted"
    }

    fun format(amount: Double, symbol: String = "₹"): String =
        formatPaise((amount * 100).toLong(), symbol)

    fun formatWithSign(amount: Double, isExpense: Boolean, symbol: String = "₹"): String =
        formatPaiseWithSign((amount * 100).toLong(), isExpense, symbol)
}
