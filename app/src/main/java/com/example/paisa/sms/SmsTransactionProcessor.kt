package com.example.paisa.sms

import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.parser.BankSmsParser
import com.example.paisa.parser.DefaultBankSmsParser
import com.example.paisa.parser.RejectionReason
import com.example.paisa.parser.SmsParseResult

sealed class SmsProcessingOutcome {
    data class Created(
        val transactionId: Long,
        val accountNickname: String,
        val amountPaise: Long,
        val type: com.example.paisa.data.local.entity.TransactionType = com.example.paisa.data.local.entity.TransactionType.DEBIT,
        val accountLastFourDigits: String = "",
        val merchantOrBeneficiary: String? = null,
        val isReviewNeeded: Boolean = false,
        val suggestions: List<String> = emptyList()
    ) : SmsProcessingOutcome()

    data class Duplicate(
        val deduplicationKey: String
    ) : SmsProcessingOutcome()

    data class Rejected(
        val reason: RejectionReason
    ) : SmsProcessingOutcome()

    data class NoAccountAvailable(
        val lastFourDigits: String
    ) : SmsProcessingOutcome()
}

class SmsTransactionProcessor(
    private val repository: ExpenseTrackerRepository,
    private val parser: BankSmsParser = DefaultBankSmsParser(),
    private val suggestionEngine: CategorySuggestionEngine = CategorySuggestionEngine.getInstance()
) {

    suspend fun processIncomingSms(
        smsBody: String,
        sender: String? = null,
        receivedTimestamp: Long = System.currentTimeMillis()
    ): SmsProcessingOutcome {
        // 1. Parse SMS using pure Kotlin parser
        val parseResult = parser.parse(
            smsBody = smsBody,
            sender = sender,
            receivedTimestamp = receivedTimestamp
        )

        when (parseResult) {
            is SmsParseResult.Rejected -> {
                // Privacy: Do not log or persist the SMS body or snippet (may be OTP)
                return SmsProcessingOutcome.Rejected(parseResult.reason)
            }
            is SmsParseResult.Success -> {
                val parsed = parseResult.transaction

                // 2. Deduplication check before inserting into Room
                val existing = repository.findByDeduplicationKey(parsed.deduplicationKey)
                if (existing != null) {
                    return SmsProcessingOutcome.Duplicate(parsed.deduplicationKey)
                }

                // 3. Account matching
                val accounts = repository.getAllAccounts()
                if (accounts.isEmpty()) {
                    return SmsProcessingOutcome.NoAccountAvailable(parsed.accountLastFourDigits)
                }

                val matchResult = AccountMatcher.match(
                    savedAccounts = accounts,
                    accountLastFour = parsed.accountLastFourDigits,
                    sender = sender
                )

                val targetAccount: BankAccountEntity
                val status: TransactionStatus
                val note: String?

                when (matchResult) {
                    is AccountMatchResult.Exact -> {
                        targetAccount = matchResult.account
                        status = TransactionStatus.PENDING
                        note = parsed.merchantOrBeneficiary
                    }
                    is AccountMatchResult.Ambiguous -> {
                        // Mark for manual review rather than guessing
                        targetAccount = matchResult.candidates.first()
                        status = TransactionStatus.REVIEW_NEEDED
                        val candidatesList = matchResult.candidates.joinToString { it.nickname }
                        val extra = if (!parsed.merchantOrBeneficiary.isNullOrBlank()) " • ${parsed.merchantOrBeneficiary}" else ""
                        note = "[Needs Review] Ambiguous account match ($candidatesList)$extra"
                    }
                    is AccountMatchResult.NoMatch -> {
                        // Automatically discover and create an account for this card/account
                        // so that transactions are NEVER mistakenly attributed to or deducted from
                        // the user's primary or other accounts.
                        val bankInfo = AccountMatcher.getBankDetails(sender, parsed.accountLastFourDigits)
                        val newAccountId = repository.createAccount(
                            bankName = bankInfo.bankName,
                            nickname = bankInfo.nickname,
                            lastFourDigits = parsed.accountLastFourDigits,
                            openingBalancePaise = 0L,
                            reconciledBalancePaise = parsed.availableBalancePaise,
                            accountType = "SAVINGS",
                            isPrimary = false,
                            bankIdentifier = bankInfo.bankIdentifier,
                            themeId = bankInfo.themeId
                        )
                        targetAccount = repository.getAccountById(newAccountId)
                            ?: BankAccountEntity(
                                id = newAccountId,
                                bankName = bankInfo.bankName,
                                nickname = bankInfo.nickname,
                                lastFourDigits = parsed.accountLastFourDigits,
                                openingBalancePaise = 0L,
                                reconciledBalancePaise = parsed.availableBalancePaise,
                                accountType = "SAVINGS",
                                isPrimary = false,
                                bankIdentifier = bankInfo.bankIdentifier,
                                themeId = bankInfo.themeId
                            )
                        status = TransactionStatus.PENDING
                        note = parsed.merchantOrBeneficiary
                    }
                }

                // 4. Do not auto-categorize: store with pending category state (categoryId = null)
                val categoryId: Long? = null

                // Compute deterministic offline suggestions for notification quick actions
                val suggestions = suggestionEngine.getSuggestions(
                    amountPaise = parsed.amountPaise,
                    type = parsed.type,
                    merchantOrPayee = parsed.merchantOrBeneficiary,
                    accounts = accounts,
                    currentAccountLastFour = targetAccount.lastFourDigits,
                    smsSnippet = null
                )

                // 5. Store transaction in Room (strictly never storing raw SMS text or OTP)
                val transactionId = repository.createTransaction(
                    accountId = targetAccount.id,
                    amountPaise = parsed.amountPaise,
                    type = parsed.type,
                    categoryId = categoryId,
                    timestamp = parsed.timestamp ?: receivedTimestamp,
                    source = TransactionSource.SMS,
                    status = status,
                    reference = parsed.reference,
                    deduplicationKey = parsed.deduplicationKey,
                    note = note
                )

                // 6. Balance reconciliation if exact match or newly created account and available balance present
                if ((matchResult is AccountMatchResult.Exact || matchResult is AccountMatchResult.NoMatch) && parsed.availableBalancePaise != null) {
                    repository.reconcileAccount(targetAccount.id, parsed.availableBalancePaise)
                }

                return SmsProcessingOutcome.Created(
                    transactionId = transactionId,
                    accountNickname = targetAccount.nickname,
                    amountPaise = parsed.amountPaise,
                    type = parsed.type,
                    accountLastFourDigits = parsed.accountLastFourDigits,
                    merchantOrBeneficiary = parsed.merchantOrBeneficiary,
                    isReviewNeeded = (status == TransactionStatus.REVIEW_NEEDED),
                    suggestions = suggestions
                )
            }
        }
    }
}
