package com.example.paisa.parser

import com.example.paisa.data.local.entity.TransactionType
import java.security.MessageDigest

object SmsDeduplicationHelper {

    fun generateKey(
        accountLastFour: String,
        amountPaise: Long,
        type: TransactionType,
        reference: String?,
        timestamp: Long?,
        rawSnippet: String
    ): String {
        val cleanRef = reference?.trim()?.uppercase()?.replace("[^A-Z0-9]".toRegex(), "")
        if (!cleanRef.isNullOrBlank()) {
            return "SMS_${accountLastFour}_${cleanRef}_${amountPaise}_${type.name}"
        }

        if (timestamp != null && timestamp > 0L) {
            return "SMS_${accountLastFour}_${timestamp}_${amountPaise}_${type.name}"
        }

        // Fallback deterministic digest based on content
        val normalized = rawSnippet.lowercase().replace("\\s+".toRegex(), " ").trim()
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(normalized.toByteArray())
        val hash = digest.joinToString("") { "%02x".format(it) }.take(12).uppercase()

        return "SMS_${accountLastFour}_${amountPaise}_${type.name}_$hash"
    }
}
