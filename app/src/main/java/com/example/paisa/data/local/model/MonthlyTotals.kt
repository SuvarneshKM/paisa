package com.example.paisa.data.local.model

data class MonthlyTotals(
    val totalIncomePaise: Long = 0L,
    val totalExpensePaise: Long = 0L,
    val transactionCount: Int = 0
) {
    val netSavingsPaise: Long get() = totalIncomePaise - totalExpensePaise
}
