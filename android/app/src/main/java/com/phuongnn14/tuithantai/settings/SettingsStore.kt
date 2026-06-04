package com.phuongnn14.tuithantai.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.phuongnn14.tuithantai.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

data class UserSettings(
    val language: String = "system",
    val backendUrl: String = BuildConfig.DEFAULT_API_BASE_URL,
    val accessToken: String = "",
    val refreshToken: String = "",
    val userEmail: String = "",
    val onboardingDone: Boolean = false
)

class SettingsStore(private val context: Context) {
    private val languageKey = stringPreferencesKey("language")
    private val backendUrlKey = stringPreferencesKey("backend_url")
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            language = prefs[languageKey] ?: "system",
            backendUrl = prefs[backendUrlKey] ?: BuildConfig.DEFAULT_API_BASE_URL,
            accessToken = prefs[accessTokenKey] ?: "",
            refreshToken = prefs[refreshTokenKey] ?: "",
            userEmail = prefs[userEmailKey] ?: "",
            onboardingDone = prefs[onboardingDoneKey] ?: false
        )
    }

    suspend fun setLanguage(value: String) {
        context.settingsDataStore.edit { it[languageKey] = value }
    }

    suspend fun setBackendUrl(value: String) {
        context.settingsDataStore.edit { it[backendUrlKey] = value }
    }

    suspend fun setAuth(accessToken: String, refreshToken: String, userEmail: String) {
        context.settingsDataStore.edit {
            it[accessTokenKey] = accessToken
            it[refreshTokenKey] = refreshToken
            it[userEmailKey] = userEmail
        }
    }

    suspend fun clearAuth() {
        context.settingsDataStore.edit {
            it.remove(accessTokenKey)
            it.remove(refreshTokenKey)
            it.remove(userEmailKey)
        }
    }

    suspend fun setOnboardingDone(value: Boolean) {
        context.settingsDataStore.edit { it[onboardingDoneKey] = value }
    }
}
