package com.example.paisa.parser

import com.example.paisa.data.local.entity.TransactionType

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

enum class RejectionReason {
    OTP_MESSAGE,
    PROMOTIONAL_OR_OFFER,
    PAYMENT_REQUEST,
    TRANSACTION_FAILED_OR_DECLINED,
    BALANCE_INQUIRY_ONLY,
    NO_TRANSACTION_AMOUNT,
    NO_ACCOUNT_IDENTIFIER,
    AMBIGUOUS_TRANSACTION_TYPE,
    UNSUPPORTED_FORMAT
}

enum class PaymentChannel {
    UPI,
    CARD,
    ATM,
    NET_BANKING,
    NEFT_IMPS,
    UNKNOWN
}

data class ParsedBankTransaction(
    val amountPaise: Long,
    val type: TransactionType,
    val accountLastFourDigits: String,
    val availableBalancePaise: Long? = null,
    val reference: String? = null,
    val merchantOrBeneficiary: String? = null,
    val timestamp: Long? = null,
    val paymentChannel: PaymentChannel = PaymentChannel.UNKNOWN,
    val deduplicationKey: String
)

sealed class SmsParseResult {
    data class Success(
        val transaction: ParsedBankTransaction,
        val confidence: ConfidenceLevel,
        val rawMessage: String
    ) : SmsParseResult()

    data class Rejected(
        val reason: RejectionReason,
        val messageSnippet: String,
        val rawMessage: String
    ) : SmsParseResult()
}
