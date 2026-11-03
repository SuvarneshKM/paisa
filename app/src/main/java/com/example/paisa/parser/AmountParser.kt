package com.example.paisa.parser

object AmountParser {

    /**
     * Parses an amount string (e.g., "1,450.50", "95,000", "450.0", "0.50", "2,50,000.75")
     * into exact Long integer paise without any floating-point imprecision.
     * Returns null if the format is invalid or non-numeric.
     */
    fun parseToPaise(amountString: String): Long? {
        val clean = amountString
            .replace(",", "")
            .replace("₹", "")
            .replace("(?i)inr".toRegex(), "")
            .replace("(?i)rs\\.?".toRegex(), "")
            .trim()

        if (clean.isEmpty()) return null

        val parts = clean.split(".")
        if (parts.size > 2) return null

        val rupeesPart = parts[0].trim()
        val rupees = if (rupeesPart.isEmpty()) 0L else rupeesPart.toLongOrNull() ?: return null
        if (rupees < 0) return null

        val paise = if (parts.size == 2) {
            val decimalStr = parts[1].trim()
            if (!decimalStr.all { it.isDigit() }) return null
            when (decimalStr.length) {
                0 -> 0L
                1 -> "${decimalStr}0".toLong()
                2 -> decimalStr.toLong()
                else -> decimalStr.take(2).toLong() // truncate beyond 2 decimal digits
            }
        } else {
            0L
        }

        return (rupees * 100) + paise
    }
}
