package com.example.paisa.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.paisa.data.local.PaisaDatabase
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.notification.TransactionNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        // 1. Manual-only fallback, opt-in & runtime permission check: If manual-only mode is active, detection disabled, or permission missing, do nothing
        if (SmsPreferences.isManualOnlyMode(context) ||
            !SmsPreferences.isSmsDetectionEnabled(context) ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECEIVE_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 2. Safe multipart SMS extraction
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        // 3. Non-blocking asynchronous processing using goAsync()
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Group multipart messages by originating address
                val groupedMessages = messages.groupBy { it.originatingAddress ?: "UNKNOWN" }

                val database = PaisaDatabase.getInstance(context)
                val repository = ExpenseTrackerRepository(
                    bankAccountDao = database.bankAccountDao(),
                    categoryDao = database.categoryDao(),
                    transactionDao = database.transactionDao()
                )
                val processor = SmsTransactionProcessor(repository)

                for ((sender, parts) in groupedMessages) {
                    val fullBody = parts.joinToString("") { it.messageBody ?: "" }
                    val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                    val outcome = processor.processIncomingSms(
                        smsBody = fullBody,
                        sender = sender,
                        receivedTimestamp = timestamp
                    )

                    if (outcome is SmsProcessingOutcome.Created) {
                        if (SmsPreferences.isNotificationsEnabled(context)) {
                            TransactionNotificationHelper.showTransactionCategorizationNotification(
                                context = context,
                                transactionId = outcome.transactionId,
                                amountPaise = outcome.amountPaise,
                                type = outcome.type,
                                accountNickname = outcome.accountNickname,
                                lastFourDigits = outcome.accountLastFourDigits,
                                merchantOrNote = outcome.merchantOrBeneficiary,
                                suggestions = outcome.suggestions
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Security & Privacy: Never log raw SMS content or exception details that might leak SMS
                Log.e(TAG, "Error processing incoming SMS transaction: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PaisaSmsReceiver"
    }
}
