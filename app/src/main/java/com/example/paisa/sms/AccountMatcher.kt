package com.example.paisa.sms

import com.example.paisa.data.local.entity.BankAccountEntity

sealed class AccountMatchResult {
    data class Exact(val account: BankAccountEntity) : AccountMatchResult()
    data class Ambiguous(val candidates: List<BankAccountEntity>) : AccountMatchResult()
    data class NoMatch(val lastFour: String, val senderEvidenceBank: String?) : AccountMatchResult()
}

object AccountMatcher {

    private val SENDER_BANK_KEYWORDS = mapOf(
        "HDFC" to listOf("HDFC", "HDFCBK"),
        "ICICI" to listOf("ICICI", "ICICIB"),
        "SBI" to listOf("SBI", "SBIINB", "SBIUPI"),
        "AXIS" to listOf("AXIS", "AXISBK"),
        "KOTAK" to listOf("KOTAK", "KOTAKB"),
        "PNB" to listOf("PNB", "PNBBNK"),
        "BOB" to listOf("BOB", "BARODA"),
        "CANARA" to listOf("CANARA", "CNRB"),
        "INDUSIND" to listOf("INDUSIND", "INDB")
    )

    fun match(
        savedAccounts: List<BankAccountEntity>,
        accountLastFour: String,
        sender: String?
    ): AccountMatchResult {
        val matches = savedAccounts.filter { it.lastFourDigits == accountLastFour }

        return when {
            matches.size == 1 -> {
                AccountMatchResult.Exact(matches.first())
            }
            matches.size > 1 -> {
                // Multiple accounts have the same last four digits.
                // Attempt disambiguation using sender header evidence.
                val senderClean = sender?.uppercase() ?: ""
                val disambiguated = matches.filter { account ->
                    isBankEvidenceMatching(account.bankName, senderClean)
                }

                if (disambiguated.size == 1) {
                    AccountMatchResult.Exact(disambiguated.first())
                } else {
                    // Ambiguous! Never guess. Mark for manual review.
                    AccountMatchResult.Ambiguous(matches)
                }
            }
            else -> {
                AccountMatchResult.NoMatch(
                    lastFour = accountLastFour,
                    senderEvidenceBank = detectBankFromSender(sender)
                )
            }
        }
    }

    fun detectBankFromSender(sender: String?): String? {
        if (sender == null) return null
        val upper = sender.uppercase()
        for ((bank, keywords) in SENDER_BANK_KEYWORDS) {
            if (keywords.any { upper.contains(it) }) {
                return bank
            }
        }
        return null
    }

    private fun isBankEvidenceMatching(bankName: String, sender: String): Boolean {
        val bankUpper = bankName.uppercase()
        for ((bankKey, keywords) in SENDER_BANK_KEYWORDS) {
            if (bankUpper.contains(bankKey)) {
                if (keywords.any { sender.contains(it) }) return true
            }
        }
        return bankUpper.isNotEmpty() && sender.contains(bankUpper)
    }
}
