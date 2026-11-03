package com.example.paisa.parser

import com.example.paisa.data.local.entity.TransactionType

interface BankSmsParser {
    fun parse(
        smsBody: String,
        sender: String? = null,
        receivedTimestamp: Long? = null
    ): SmsParseResult
}

class DefaultBankSmsParser : BankSmsParser {

    companion object {
        // --- 1. Rejection Patterns ---

        // OTP detection
        private val OTP_REGEX = Regex(
            """(?i)\b(?:otp|one[\s-]?time[\s-]?password|verification[\s-]?code|secret[\s-]?code|login[\s-]?otp)\b|(?:\buse\s+(?:code|otp)\b)|(?:\bnever\s+share\b)|(?:\bdo\s+not\s+share\b)|(?:\bvalid\s+for\s+\d+\s*(?:mins?|minutes?|secs?)\b)"""
        )

        // Failed / Declined transactions
        private val FAILED_REGEX = Regex(
            """(?i)\b(?:failed|declined|unsuccessful|could\s+not\s+be\s+processed|insufficient\s+funds|inadequate\s+balance|transaction\s+declined)\b"""
        )

        // Payment Requests / Collect Requests
        private val PAYMENT_REQUEST_REGEX = Regex(
            """(?i)\b(?:has\s+requested|requested\s+(?:money|rs|inr|payment)|collect\s+request|authorise\s+payment|authorize\s+payment|request\s+to\s+pay|approve\s+payment|approval\s+pending)\b"""
        )

        // Promotional / Loan offers
        private val PROMOTIONAL_REGEX = Regex(
            """(?i)\b(?:pre-approved|apply\s+now|congratulations|claim\s+now|special\s+offer|upgrade\s+your\s+card|flat\s+\d+%\s+off|exclusive\s+offer|instant\s+loan|eligible\s+for\s+(?:loan|card))\b"""
        )

        // --- 2. Available Balance Pattern ---
        // Matches "Avail Bal: INR 45,210.00", "Bal: Rs 52,000.00", "Available limit: Rs 50,000",
        // "available balance for A/C XX4821 is INR 45,210.00", etc.
        private val BALANCE_REGEX = Regex(
            """(?i)(?:avail(?:able)?\s*(?:bal(?:ance)?|limit)|avl\s*bal|bal(?:ance)?|total\s*bal(?:ance)?)(?:\s+(?:for|in)\s+[a-zA-Z0-9*xX/ -]+?)?\s*(?:is|:)?\s*(?:inr|rs\.?|₹)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)"""
        )

        // --- 3. Transaction Types ---
        private val DEBIT_VERBS = Regex(
            """(?i)\b(?:debited|debit|withdrawn|spent|paid|sent|transferred\s+to|purchase\s+of|charged)\b"""
        )

        private val CREDIT_VERBS = Regex(
            """(?i)\b(?:credited|credit|received|deposited|refunded|added\s+to|transferred\s+from|cashback\s+received)\b"""
        )

        // Balance Inquiry Only (no debit or credit action)
        private val BALANCE_INQUIRY_ONLY_REGEX = Regex(
            """(?i)\b(?:your\s+(?:available\s+)?bal(?:ance)?\s+is|clear\s+balance\s+is|mini\s+statement|account\s+statement\s+generated)\b"""
        )

        // --- 4. Amount Patterns in Sanitized Message ---
        // Pattern A: Currency followed by number: INR 1,450.50, Rs. 500, ₹1,200
        private val CURRENCY_AMOUNT_REGEX = Regex(
            """(?i)(?:inr|rs\.?|₹)\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)"""
        )

        // Pattern B: Verb followed by amount: debited by 450.0, credited with 95,000
        private val VERB_AMOUNT_REGEX = Regex(
            """(?i)(?:debited\s*(?:with|by|for)?|credited\s*(?:with|by|for)?|withdrawn|spent|paid)\s*(?:inr|rs\.?|₹)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)"""
        )

        // Pattern C: Amount followed by debit/credit verb: 450.0 debited, 1000 debited
        private val AMOUNT_THEN_VERB_REGEX = Regex(
            """(?i)([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)\s*(?:inr|rs\.?|₹)?\s*(?:is\s*)?(?:debited|credited|withdrawn)"""
        )

        // --- 5. Account Last 4 Digits Patterns ---
        private val ACCOUNT_PATTERNS = listOf(
            // A/c **4821, Acct XX9104, Card XX1159, Account ending with 6231
            Regex("""(?i)(?:a/c|acct|account|card)\s*(?:no\.?)?\s*(?:ending\s*(?:with|in)?\s*)?[*xX-]*\s*([0-9]{4})\b"""),
            // **4821 or XX9104
            Regex("""\b[*xX]{2,}([0-9]{4})\b"""),
            // ending with 4821
            Regex("""(?i)\b(?:ending\s*(?:with|in)?)\s*([0-9]{4})\b""")
        )

        // --- 6. Reference Patterns ---
        private val REFERENCE_PATTERNS = listOf(
            // UPI Ref 428910482101, UPI: ref 429910482910, UPI/428910482101
            Regex("""(?i)(?:upi\s*(?:ref(?:erence)?|txn)?\s*(?:no\.?)?[:\s]*|upi/)([0-9A-Za-z]{6,20})"""),
            // Ref no 428910482101, UTR: 123456789, RRN: 981247192
            Regex("""(?i)(?:ref\s*(?:no\.?|num)?|utr|rrn|txn\s*(?:id|no\.?|ref))[:\s]*([0-9A-Za-z]{6,20})"""),
            // NEFT/IMPS-CMS4821 or IMPS/123456
            Regex("""(?i)(?:imps|neft)[/:\s-]*([0-9A-Za-z]{6,22})""")
        )

        // --- 7. Merchant / Beneficiary Patterns ---
        private val BENEFICIARY_REGEX = Regex(
            """(?i)\b(?:to\s+vpa\s+|to\s+|at\s+|towards\s+|info:\s*)([A-Za-z0-9@._* -]{2,28}?)(?:\s+(?:on|ref|upi|avail|bal|\.|\band\b|$))"""
        )
    }

    override fun parse(
        smsBody: String,
        sender: String?,
        receivedTimestamp: Long?
    ): SmsParseResult {
        val trimmed = smsBody.trim()
        if (trimmed.isEmpty()) {
            return SmsParseResult.Rejected(
                reason = RejectionReason.UNSUPPORTED_FORMAT,
                messageSnippet = "",
                rawMessage = smsBody
            )
        }

        // --- Phase 1: Rejections ---
        if (OTP_REGEX.containsMatchIn(trimmed)) {
            return SmsParseResult.Rejected(
                reason = RejectionReason.OTP_MESSAGE,
                messageSnippet = trimmed.take(60),
                rawMessage = smsBody
            )
        }

        if (FAILED_REGEX.containsMatchIn(trimmed)) {
            return SmsParseResult.Rejected(
                reason = RejectionReason.TRANSACTION_FAILED_OR_DECLINED,
                messageSnippet = trimmed.take(60),
                rawMessage = smsBody
            )
        }

        if (PAYMENT_REQUEST_REGEX.containsMatchIn(trimmed)) {
            return SmsParseResult.Rejected(
                reason = RejectionReason.PAYMENT_REQUEST,
                messageSnippet = trimmed.take(60),
                rawMessage = smsBody
            )
        }

        if (PROMOTIONAL_REGEX.containsMatchIn(trimmed)) {
            return SmsParseResult.Rejected(
                reason = RejectionReason.PROMOTIONAL_OR_OFFER,
                messageSnippet = trimmed.take(60),
                rawMessage = smsBody
            )
        }

        val hasDebit = DEBIT_VERBS.containsMatchIn(trimmed)
        val hasCredit = CREDIT_VERBS.containsMatchIn(trimmed)

        if (!hasDebit && !hasCredit) {
            val hasBalanceMention = Regex("""(?i)\b(?:bal(?:ance)?|avl\s*bal|statement)\b""").containsMatchIn(trimmed)
            val reason = if (hasBalanceMention || BALANCE_REGEX.containsMatchIn(trimmed)) {
                RejectionReason.BALANCE_INQUIRY_ONLY
            } else {
                RejectionReason.AMBIGUOUS_TRANSACTION_TYPE
            }
            return SmsParseResult.Rejected(
                reason = reason,
                messageSnippet = trimmed.take(60),
                rawMessage = smsBody
            )
        }

        // --- Phase 2: Extract Available Balance & Exclude from Transaction Search Space ---
        var availableBalancePaise: Long? = null
        var sanitizedForTxn = trimmed

        val balanceMatch = BALANCE_REGEX.find(trimmed)
        if (balanceMatch != null) {
            val balanceString = balanceMatch.groupValues[1]
            availableBalancePaise = AmountParser.parseToPaise(balanceString)
            // Strip out the balance portion to guarantee it will never be confused with transaction amount!
            sanitizedForTxn = trimmed.removeRange(balanceMatch.range)
        }

        // --- Phase 3: Transaction Type ---
        val type: TransactionType = when {
            hasDebit && !hasCredit -> TransactionType.DEBIT
            hasCredit && !hasDebit -> TransactionType.CREDIT
            else -> {
                // Both appeared, resolve by closest action to amount or verb precedence
                val debitIdx = DEBIT_VERBS.find(sanitizedForTxn)?.range?.first ?: Int.MAX_VALUE
                val creditIdx = CREDIT_VERBS.find(sanitizedForTxn)?.range?.first ?: Int.MAX_VALUE
                if (debitIdx < creditIdx) TransactionType.DEBIT else TransactionType.CREDIT
            }
        }

        // --- Phase 4: Transaction Amount Extraction ---
        val amountPaise: Long? = extractTransactionAmount(sanitizedForTxn)

        if (amountPaise == null || amountPaise <= 0L) {
            return SmsParseResult.Rejected(
                reason = RejectionReason.NO_TRANSACTION_AMOUNT,
                messageSnippet = trimmed.take(60),
                rawMessage = smsBody
            )
        }

        // --- Phase 5: Account Last Four Digits ---
        val accountLastFour: String? = extractAccountLastFour(trimmed)

        if (accountLastFour == null) {
            return SmsParseResult.Rejected(
                reason = RejectionReason.NO_ACCOUNT_IDENTIFIER,
                messageSnippet = trimmed.take(60),
                rawMessage = smsBody
            )
        }

        // --- Phase 6: Reference Number ---
        val reference: String? = extractReference(trimmed)

        // --- Phase 7: Merchant / Beneficiary ---
        val merchantOrBeneficiary: String? = extractMerchant(sanitizedForTxn)

        // --- Phase 8: Date Parsing ---
        val timestamp: Long? = SmsDateParser.parseDate(trimmed, fallbackTimestamp = receivedTimestamp)

        // --- Phase 9: Payment Channel ---
        val channel = detectPaymentChannel(trimmed)

        // --- Phase 10: Deduplication Key ---
        val deduplicationKey = SmsDeduplicationHelper.generateKey(
            accountLastFour = accountLastFour,
            amountPaise = amountPaise,
            type = type,
            reference = reference,
            timestamp = timestamp,
            rawSnippet = trimmed
        )

        // Confidence calculation
        val confidence = when {
            reference != null || availableBalancePaise != null -> ConfidenceLevel.HIGH
            timestamp != null -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        val parsedTransaction = ParsedBankTransaction(
            amountPaise = amountPaise,
            type = type,
            accountLastFourDigits = accountLastFour,
            availableBalancePaise = availableBalancePaise,
            reference = reference,
            merchantOrBeneficiary = merchantOrBeneficiary,
            timestamp = timestamp,
            paymentChannel = channel,
            deduplicationKey = deduplicationKey
        )

        return SmsParseResult.Success(
            transaction = parsedTransaction,
            confidence = confidence,
            rawMessage = smsBody
        )
    }

    private fun extractTransactionAmount(text: String): Long? {
        // Try verb + amount first (strongest intent: "debited with INR 1,450.50")
        val verbMatch = VERB_AMOUNT_REGEX.find(text)
        if (verbMatch != null) {
            val candidate = AmountParser.parseToPaise(verbMatch.groupValues[1])
            if (candidate != null && candidate > 0L) return candidate
        }

        // Try currency amount: "INR 1,450.50", "Rs. 500"
        val currencyMatches = CURRENCY_AMOUNT_REGEX.findAll(text).toList()
        for (match in currencyMatches) {
            val candidate = AmountParser.parseToPaise(match.groupValues[1])
            if (candidate != null && candidate > 0L) return candidate
        }

        // Try amount followed by verb: "450.0 debited"
        val amountThenVerbMatch = AMOUNT_THEN_VERB_REGEX.find(text)
        if (amountThenVerbMatch != null) {
            val candidate = AmountParser.parseToPaise(amountThenVerbMatch.groupValues[1])
            if (candidate != null && candidate > 0L) return candidate
        }

        return null
    }

    private fun extractAccountLastFour(text: String): String? {
        for (pattern in ACCOUNT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val digits = match.groupValues[1]
                if (digits.length == 4 && digits.all { it.isDigit() }) {
                    return digits
                }
            }
        }
        return null
    }

    private fun extractReference(text: String): String? {
        for (pattern in REFERENCE_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val ref = match.groupValues[1].trim()
                if (ref.isNotEmpty()) return ref
            }
        }
        return null
    }

    private fun extractMerchant(text: String): String? {
        val match = BENEFICIARY_REGEX.find(text) ?: return null
        val merchant = match.groupValues[1]
            .replace("[*,;:.]+$".toRegex(), "")
            .trim()
        return if (merchant.length >= 2) merchant else null
    }

    private fun detectPaymentChannel(text: String): PaymentChannel {
        val lower = text.lowercase()
        return when {
            lower.contains("upi") || lower.contains("@") -> PaymentChannel.UPI
            lower.contains("atm") || lower.contains("withdrawn from atm") -> PaymentChannel.ATM
            lower.contains("card") || lower.contains("pos") -> PaymentChannel.CARD
            lower.contains("net banking") || lower.contains("netbanking") -> PaymentChannel.NET_BANKING
            lower.contains("neft") || lower.contains("imps") || lower.contains("rtgs") -> PaymentChannel.NEFT_IMPS
            else -> PaymentChannel.UNKNOWN
        }
    }
}
