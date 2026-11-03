package com.example.paisa.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.paisa.sms.SmsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.paisaDataStore: DataStore<Preferences> by preferencesDataStore(name = "paisa_user_preferences")

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val smsDetectionEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val manualOnlyMode: Boolean = false,
    val userConsented: Boolean = false
)

class PaisaPreferences(private val context: Context) {

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_SMS_DETECTION_ENABLED = booleanPreferencesKey("sms_detection_enabled")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_MANUAL_ONLY_MODE = booleanPreferencesKey("manual_only_mode")
        val KEY_USER_CONSENTED = booleanPreferencesKey("user_consented")

        @Volatile
        private var INSTANCE: PaisaPreferences? = null

        fun getInstance(context: Context): PaisaPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = PaisaPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.paisaDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                onboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED] ?: false,
                smsDetectionEnabled = preferences[KEY_SMS_DETECTION_ENABLED] ?: false,
                notificationsEnabled = preferences[KEY_NOTIFICATIONS_ENABLED] ?: false,
                manualOnlyMode = preferences[KEY_MANUAL_ONLY_MODE] ?: false,
                userConsented = preferences[KEY_USER_CONSENTED] ?: false
            )
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.paisaDataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
        SmsPreferences.setOnboardingCompleted(context, completed)
    }

    suspend fun setSmsDetectionEnabled(enabled: Boolean) {
        context.paisaDataStore.edit { preferences ->
            preferences[KEY_SMS_DETECTION_ENABLED] = enabled
            if (enabled) {
                preferences[KEY_USER_CONSENTED] = true
            }
        }
        SmsPreferences.setSmsDetectionEnabled(context, enabled)
        if (enabled) {
            SmsPreferences.setUserConsented(context, true)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.paisaDataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
        SmsPreferences.setNotificationsEnabled(context, enabled)
    }

    suspend fun setManualOnlyMode(manualOnly: Boolean) {
        context.paisaDataStore.edit { preferences ->
            preferences[KEY_MANUAL_ONLY_MODE] = manualOnly
            if (manualOnly) {
                preferences[KEY_SMS_DETECTION_ENABLED] = false
                preferences[KEY_NOTIFICATIONS_ENABLED] = false
            }
        }
        SmsPreferences.setManualOnlyMode(context, manualOnly)
    }

    suspend fun setUserConsented(consented: Boolean) {
        context.paisaDataStore.edit { preferences ->
            preferences[KEY_USER_CONSENTED] = consented
        }
        SmsPreferences.setUserConsented(context, consented)
    }
}
