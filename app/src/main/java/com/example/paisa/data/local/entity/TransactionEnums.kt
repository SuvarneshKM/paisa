package com.example.paisa.data.local.entity

enum class TransactionType {
    DEBIT,
    CREDIT;

    val isDebit: Boolean get() = this == DEBIT
    val isCredit: Boolean get() = this == CREDIT
}

enum class TransactionSource {
    MANUAL,
    SMS
}

enum class TransactionStatus {
    CONFIRMED,
    PENDING,
    CANCELLED,
    REVIEW_NEEDED
}
