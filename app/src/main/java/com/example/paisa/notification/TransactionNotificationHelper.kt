package com.example.paisa.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.paisa.R
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.model.CategoryIconRegistry
import com.example.paisa.sms.CategorySuggestionEngine
import com.example.paisa.sms.SmsPreferences
import com.example.paisa.util.CurrencyFormatter

object TransactionNotificationHelper {

    const val CHANNEL_ID = "paisa_transactions"
    const val CHANNEL_NAME = "Transaction Alerts"
    const val CHANNEL_DESC = "Alerts for incoming transactions with quick categorization actions"

    const val ACTION_CATEGORIZE = "com.example.paisa.action.CATEGORIZE"
    const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    const val EXTRA_CATEGORY_NAME = "extra_category_name"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    // Action code offsets for unique PendingIntent request codes
    const val CODE_SUGGESTION_1 = 1
    const val CODE_SUGGESTION_2 = 2
    const val CODE_MORE = 3
    const val CODE_CONTENT = 4

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun getNotificationId(transactionId: Long): Int {
        return (transactionId and 0x7FFFFFFF).toInt()
    }

    fun getActionRequestCode(transactionId: Long, actionCode: Int): Int {
        return ((transactionId.hashCode() * 31 + actionCode) and 0x7FFFFFFF)
    }

    fun maskAccountDigits(lastFourDigits: String): String {
        val clean = lastFourDigits.trim()
        return if (clean.isNotEmpty()) "•••• $clean" else "••••"
    }

    /**
     * Checks if notification permissions and user preferences allow posting.
     */
    fun canPostNotifications(context: Context): Boolean {
        if (!SmsPreferences.isNotificationsEnabled(context)) {
            return false
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    fun showTransactionCategorizationNotification(
        context: Context,
        transactionId: Long,
        amountPaise: Long,
        type: TransactionType,
        accountNickname: String,
        lastFourDigits: String,
        merchantOrNote: String? = null,
        suggestions: List<String>? = null
    ): Int? {
        if (!canPostNotifications(context)) {
            return null
        }

        createNotificationChannel(context)

        val notificationId = getNotificationId(transactionId)
        val formattedAmount = CurrencyFormatter.formatPaise(amountPaise)
        val maskedAccount = maskAccountDigits(lastFourDigits)

        // Strict Privacy: Never expose full bank account numbers or raw SMS content
        val title = if (type == TransactionType.DEBIT) {
            "Debit: $formattedAmount"
        } else {
            "Credit: $formattedAmount"
        }

        val bodyText = if (!merchantOrNote.isNullOrBlank()) {
            "$merchantOrNote • $accountNickname ($maskedAccount)"
        } else {
            "$accountNickname ($maskedAccount)"
        }

        // Tap on notification body opens the category picker
        val contentIntent = Intent(context, CategoryPickerActivity::class.java).apply {
            data = Uri.parse("paisa://transaction/$transactionId/content/$notificationId")
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            getActionRequestCode(transactionId, CODE_CONTENT),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Resolve top 2 ranked suggestions via SuggestionEngine
        val rankedSuggestions = if (!suggestions.isNullOrEmpty()) {
            suggestions
        } else {
            CategorySuggestionEngine.getInstance(context).getSuggestions(
                amountPaise = amountPaise,
                type = type,
                merchantOrPayee = merchantOrNote,
                currentAccountLastFour = lastFourDigits
            )
        }

        val firstSuggestion = rankedSuggestions.getOrNull(0)
            ?: if (type == TransactionType.DEBIT) "Food" else "Salary"
        val secondSuggestion = rankedSuggestions.getOrNull(1)
            ?: if (type == TransactionType.DEBIT) "Shopping" else "Refund"

        // Add 2 highest-ranked category action buttons + "More" action with native vector icons
        builder.addAction(
            createCategoryAction(context, transactionId, notificationId, firstSuggestion, CODE_SUGGESTION_1)
        )
        builder.addAction(
            createCategoryAction(context, transactionId, notificationId, secondSuggestion, CODE_SUGGESTION_2)
        )
        builder.addAction(
            createMoreAction(context, transactionId, notificationId, "More", CODE_MORE)
        )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            return notificationId
        } catch (e: SecurityException) {
            // Graceful handling of unexpected security restrictions
            return null
        }
    }

    fun createCategoryAction(
        context: Context,
        transactionId: Long,
        notificationId: Int,
        categoryName: String,
        actionCode: Int
    ): NotificationCompat.Action {
        val intent = Intent(context, TransactionActionReceiver::class.java).apply {
            action = ACTION_CATEGORIZE
            data = Uri.parse("paisa://transaction/$transactionId/action/$actionCode/$categoryName")
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra(EXTRA_CATEGORY_NAME, categoryName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getActionRequestCode(transactionId, actionCode),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val iconRes = CategoryIconRegistry.getIcon(categoryName)
        return NotificationCompat.Action.Builder(iconRes, categoryName, pendingIntent).build()
    }

    fun createMoreAction(
        context: Context,
        transactionId: Long,
        notificationId: Int,
        label: String = "More",
        actionCode: Int = CODE_MORE
    ): NotificationCompat.Action {
        val intent = Intent(context, CategoryPickerActivity::class.java).apply {
            data = Uri.parse("paisa://transaction/$transactionId/more/$notificationId")
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            getActionRequestCode(transactionId, actionCode),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val moreIconRes = CategoryIconRegistry.DEFAULT_FALLBACK_ICON
        return NotificationCompat.Action.Builder(moreIconRes, label, pendingIntent).build()
    }

    /**
     * Updates notification upon category assignment to give clear visual confirmation,
     * clearing the action buttons and auto-dismissing after 2.5 seconds.
     */
    fun updateNotificationAsCategorized(
        context: Context,
        notificationId: Int,
        categoryName: String
    ) {
        if (!canPostNotifications(context)) return

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Categorized as $categoryName ✓")
            .setContentText("Transaction updated successfully")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(2500L) // Dismisses automatically after 2.5s

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Ignored
        }
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (e: Exception) {
            // Ignored
        }
    }
}
