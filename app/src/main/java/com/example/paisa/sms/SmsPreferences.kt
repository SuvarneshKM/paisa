package com.example.paisa.sms

import android.content.Context
import android.content.SharedPreferences

object SmsPreferences {
    private const val PREFS_NAME = "paisa_sms_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_SMS_DETECTION_ENABLED = "sms_detection_enabled"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_MANUAL_ONLY_MODE = "manual_only_mode"
    private const val KEY_USER_CONSENTED = "user_consented"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * First launch onboarding status: defaults to FALSE.
     */
    fun isOnboardingCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    /**
     * Manual-only fallback: completely isolates the app, disallowing any SMS detection or listeners.
     */
    fun isManualOnlyMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MANUAL_ONLY_MODE, false)
    }

    fun setManualOnlyMode(context: Context, manualOnly: Boolean) {
        val editor = getPrefs(context).edit().putBoolean(KEY_MANUAL_ONLY_MODE, manualOnly)
        if (manualOnly) {
            editor.putBoolean(KEY_SMS_DETECTION_ENABLED, false)
            editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        }
        editor.apply()
    }

    /**
     * Privacy-first: Defaults to FALSE. Explicit user opt-in is required before category notifications are shown.
     */
    fun isNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    /**
     * Privacy-first: Defaults to FALSE. Explicit user opt-in is required before any SMS detection takes place.
     */
    fun isSmsDetectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SMS_DETECTION_ENABLED, false)
    }

    fun setSmsDetectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SMS_DETECTION_ENABLED, enabled).apply()
    }

    fun hasUserConsented(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_USER_CONSENTED, false)
    }

    fun setUserConsented(context: Context, consented: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_USER_CONSENTED, consented).apply()
    }
}
