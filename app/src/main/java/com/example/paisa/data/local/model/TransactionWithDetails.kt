package com.example.paisa.data.local.model

import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.model.CategoryIconRegistry

data class TransactionWithDetails(
    val id: Long,
    val accountId: Long,
    val amountPaise: Long,
    val type: TransactionType,
    val categoryId: Long?,
    val timestamp: Long,
    val source: TransactionSource,
    val status: TransactionStatus,
    val reference: String?,
    val deduplicationKey: String?,
    val note: String?,
    val bankName: String,
    val accountNickname: String,
    val accountLastFourDigits: String,
    val categoryName: String?,
    val categoryEmoji: String?
) {
    val isDebit: Boolean get() = type == TransactionType.DEBIT
    val isCredit: Boolean get() = type == TransactionType.CREDIT
    val isTransfer: Boolean get() = categoryName.equals("Transfer", ignoreCase = true)
    val isExpense: Boolean get() = isDebit && !isTransfer
    val isIncome: Boolean get() = isCredit && !isTransfer
    val isReviewNeeded: Boolean get() = status == TransactionStatus.REVIEW_NEEDED || status == TransactionStatus.PENDING || categoryId == null
    val displayTitle: String get() = note?.takeIf { it.isNotBlank() } ?: categoryName ?: reference ?: "Uncategorized Transaction"
    val displayCategoryEmoji: String get() = ""
    val categoryIconRes: Int get() = CategoryIconRegistry.getIconForTransaction(categoryName, isCredit)

    val typeLabel: String get() = when {
        isTransfer -> "Transfer"
        isIncome -> "Income"
        else -> "Expense"
    }
}
