package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val userName: String = "Bro",
    val currencySymbol: String = "₹",
    val salaryCycleType: String = "CALENDAR", // CALENDAR or CUSTOM
    val salaryCycleDay: Int = 1,
    val defaultSalary: Double = 50000.0,
    val funnyReactionsEnabled: Boolean = true,
    val reactionIntensity: String = "NORMAL", // SOFT, NORMAL, ROAST
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val monthlySavingsGoal: Double = 0.0,
    val autoSmsDetectionEnabled: Boolean = true,
    val autoSmsNotificationEnabled: Boolean = true,
    val lastSmsScanTimestamp: Long = 0L,
    val lastSmsScanDate: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val USER_NAME = stringPreferencesKey("user_name")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val SALARY_CYCLE_TYPE = stringPreferencesKey("salary_cycle_type")
        val SALARY_CYCLE_DAY = intPreferencesKey("salary_cycle_day")
        val DEFAULT_SALARY = doublePreferencesKey("default_salary")
        val FUNNY_REACTIONS_ENABLED = booleanPreferencesKey("funny_reactions_enabled")
        val REACTION_INTENSITY = stringPreferencesKey("reaction_intensity")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val MONTHLY_SAVINGS_GOAL = doublePreferencesKey("monthly_savings_goal")
        val AUTO_SMS_DETECTION_ENABLED = booleanPreferencesKey("auto_sms_detection_enabled")
        val AUTO_SMS_NOTIFICATION_ENABLED = booleanPreferencesKey("auto_sms_notification_enabled")
        val LAST_SMS_SCAN_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("last_sms_scan_timestamp")
        val LAST_SMS_SCAN_DATE = stringPreferencesKey("last_sms_scan_date")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            userName = preferences[PreferencesKeys.USER_NAME] ?: "Bro",
            currencySymbol = preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: "₹",
            salaryCycleType = preferences[PreferencesKeys.SALARY_CYCLE_TYPE] ?: "CALENDAR",
            salaryCycleDay = preferences[PreferencesKeys.SALARY_CYCLE_DAY] ?: 1,
            defaultSalary = preferences[PreferencesKeys.DEFAULT_SALARY] ?: 50000.0,
            funnyReactionsEnabled = preferences[PreferencesKeys.FUNNY_REACTIONS_ENABLED] ?: true,
            reactionIntensity = preferences[PreferencesKeys.REACTION_INTENSITY] ?: "NORMAL",
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            appLockEnabled = preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false,
            appLockPin = preferences[PreferencesKeys.APP_LOCK_PIN] ?: "",
            monthlySavingsGoal = preferences[PreferencesKeys.MONTHLY_SAVINGS_GOAL] ?: 0.0,
            autoSmsDetectionEnabled = preferences[PreferencesKeys.AUTO_SMS_DETECTION_ENABLED] ?: true,
            autoSmsNotificationEnabled = preferences[PreferencesKeys.AUTO_SMS_NOTIFICATION_ENABLED] ?: true,
            lastSmsScanTimestamp = preferences[PreferencesKeys.LAST_SMS_SCAN_TIMESTAMP] ?: 0L,
            lastSmsScanDate = preferences[PreferencesKeys.LAST_SMS_SCAN_DATE] ?: ""
        )
    }

    suspend fun updateLastSmsScanTime(timestamp: Long, dateString: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SMS_SCAN_TIMESTAMP] = timestamp
            preferences[PreferencesKeys.LAST_SMS_SCAN_DATE] = dateString
        }
    }

    suspend fun setAutoSmsDetection(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SMS_DETECTION_ENABLED] = enabled
        }
    }

    suspend fun setAutoSmsNotification(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SMS_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateSalaryConfig(salary: Double, cycleType: String, cycleDay: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_SALARY] = salary
            preferences[PreferencesKeys.SALARY_CYCLE_TYPE] = cycleType
            preferences[PreferencesKeys.SALARY_CYCLE_DAY] = cycleDay
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun setFunnyReactionsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FUNNY_REACTIONS_ENABLED] = enabled
        }
    }

    suspend fun setReactionIntensity(intensity: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REACTION_INTENSITY] = intensity
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setAppLock(enabled: Boolean, pin: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_ENABLED] = enabled
            preferences[PreferencesKeys.APP_LOCK_PIN] = pin
        }
    }

    suspend fun setMonthlySavingsGoal(goal: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONTHLY_SAVINGS_GOAL] = goal
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
