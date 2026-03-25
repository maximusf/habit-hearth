package com.project.habithearth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.project.habithearth.ui.state.GameUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userProgressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "habit_hearth_progress",
)

private val KEY_ACCOUNT_COMPLETE = booleanPreferencesKey("account_setup_complete")
private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
private val KEY_USERNAME = stringPreferencesKey("username")
private val KEY_PASSWORD_SALT = stringPreferencesKey("password_salt")
private val KEY_PASSWORD_HASH = stringPreferencesKey("password_hash")
private val KEY_SESSION_LOCKED = booleanPreferencesKey("session_locked")
private val KEY_GAME_STATE_JSON = stringPreferencesKey("game_state_json")
private val KEY_PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications")
private val KEY_VACATION_MODE = booleanPreferencesKey("vacation_mode")
private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
private val KEY_LANGUAGE = stringPreferencesKey("language")
private val KEY_TEXT_SIZE = stringPreferencesKey("text_size")
private val KEY_PROFILE_AVATAR_ID = intPreferencesKey("profile_avatar_id")

data class AccountSettings(
    val username: String,
    val hasLoginCredentials: Boolean,
    val displayName: String,
    val pushNotifications: Boolean,
    val vacationMode: Boolean,
    val themeMode: String,
    val language: String,
    val textSize: String,
    val profileAvatarId: Int,
) {
    companion object {
        val DEFAULT = AccountSettings(
            username = "",
            hasLoginCredentials = false,
            displayName = "",
            pushNotifications = false,
            vacationMode = false,
            themeMode = "System default",
            language = "English",
            textSize = "Default",
            profileAvatarId = 0,
        )
    }
}

class UserProgressRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.userProgressDataStore
    private val gson = Gson()
    private val gameStateType = object : TypeToken<GameUiState>() {}.type

    val accountSetupComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ACCOUNT_COMPLETE] ?: false
    }

    val accountSettings: Flow<AccountSettings> = dataStore.data.map { prefs ->
        val passwordHash = prefs[KEY_PASSWORD_HASH].orEmpty()
        AccountSettings(
            username = prefs[KEY_USERNAME].orEmpty(),
            hasLoginCredentials = passwordHash.isNotEmpty(),
            displayName = prefs[KEY_DISPLAY_NAME].orEmpty(),
            pushNotifications = prefs[KEY_PUSH_NOTIFICATIONS] ?: AccountSettings.DEFAULT.pushNotifications,
            vacationMode = prefs[KEY_VACATION_MODE] ?: AccountSettings.DEFAULT.vacationMode,
            themeMode = prefs[KEY_THEME_MODE] ?: AccountSettings.DEFAULT.themeMode,
            language = prefs[KEY_LANGUAGE] ?: AccountSettings.DEFAULT.language,
            textSize = prefs[KEY_TEXT_SIZE] ?: AccountSettings.DEFAULT.textSize,
            profileAvatarId = prefs[KEY_PROFILE_AVATAR_ID] ?: AccountSettings.DEFAULT.profileAvatarId,
        )
    }

    suspend fun isAccountSetupComplete(): Boolean =
        accountSetupComplete.first()

    suspend fun shouldShowLoginGate(): Boolean {
        val prefs = dataStore.data.first()
        val hash = prefs[KEY_PASSWORD_HASH].orEmpty()
        val locked = prefs[KEY_SESSION_LOCKED] ?: false
        return hash.isNotEmpty() && locked
    }

    suspend fun completeAccountSetup(username: String, password: String, displayName: String) {
        val salt = CredentialHasher.generateSalt()
        val hash = CredentialHasher.hash(password, salt)
        dataStore.edit { prefs ->
            prefs[KEY_ACCOUNT_COMPLETE] = true
            prefs[KEY_DISPLAY_NAME] = displayName.trim().ifBlank { "Traveler" }
            prefs[KEY_USERNAME] = username.trim()
            prefs[KEY_PASSWORD_SALT] = salt
            prefs[KEY_PASSWORD_HASH] = hash
            prefs[KEY_SESSION_LOCKED] = false

            // Reset all in-game resources when a new account is created.
            // Keep seed tasks (default [GameUiState.tasks]) but zero out gems/coins/progress.
            prefs[KEY_GAME_STATE_JSON] =
                gson.toJson(
                    GameUiState(
                        strengthGems = 0,
                        wisdomGems = 0,
                        vitalityGems = 0,
                        spiritGems = 0,
                        coins = 0,
                        xpProgress = 0f,
                    ),
                )
        }
    }

    /**
     * Locks the session so [shouldShowLoginGate] is true. Returns false if no password was set yet.
     */
    suspend fun logout(): Boolean {
        val prefs = dataStore.data.first()
        if (prefs[KEY_PASSWORD_HASH].isNullOrBlank()) return false
        dataStore.edit { it[KEY_SESSION_LOCKED] = true }
        return true
    }

    suspend fun tryUnlockSession(username: String, password: String): Boolean {
        val prefs = dataStore.data.first()
        val storedUser = prefs[KEY_USERNAME].orEmpty()
        val salt = prefs[KEY_PASSWORD_SALT] ?: return false
        val storedHash = prefs[KEY_PASSWORD_HASH] ?: return false
        if (storedUser != username.trim()) return false
        if (!CredentialHasher.verify(password, salt, storedHash)) return false
        dataStore.edit { it[KEY_SESSION_LOCKED] = false }
        return true
    }

    suspend fun setUsername(username: String) {
        val u = username.trim()
        if (u.isBlank()) return
        dataStore.edit { it[KEY_USERNAME] = u }
    }

    suspend fun setLoginCredentials(username: String, password: String): Boolean {
        if (username.isBlank() || password.length < MIN_PASSWORD_LENGTH) return false
        val salt = CredentialHasher.generateSalt()
        val hash = CredentialHasher.hash(password, salt)
        dataStore.edit {
            it[KEY_USERNAME] = username.trim()
            it[KEY_PASSWORD_SALT] = salt
            it[KEY_PASSWORD_HASH] = hash
            it[KEY_SESSION_LOCKED] = false
        }
        return true
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Boolean {
        if (newPassword.length < MIN_PASSWORD_LENGTH) return false
        val prefs = dataStore.data.first()
        val salt = prefs[KEY_PASSWORD_SALT] ?: return false
        val storedHash = prefs[KEY_PASSWORD_HASH] ?: return false
        if (!CredentialHasher.verify(currentPassword, salt, storedHash)) return false
        val newSalt = CredentialHasher.generateSalt()
        val newHash = CredentialHasher.hash(newPassword, newSalt)
        dataStore.edit {
            it[KEY_PASSWORD_SALT] = newSalt
            it[KEY_PASSWORD_HASH] = newHash
        }
        return true
    }

    suspend fun setDisplayName(displayName: String) {
        dataStore.edit { prefs ->
            prefs[KEY_DISPLAY_NAME] = displayName.trim().ifBlank { "Traveler" }
        }
    }

    suspend fun setPushNotifications(enabled: Boolean) {
        dataStore.edit { it[KEY_PUSH_NOTIFICATIONS] = enabled }
    }

    suspend fun setVacationMode(enabled: Boolean) {
        dataStore.edit { it[KEY_VACATION_MODE] = enabled }
    }

    suspend fun setThemeMode(value: String) {
        dataStore.edit { it[KEY_THEME_MODE] = value }
    }

    suspend fun setLanguage(value: String) {
        dataStore.edit { it[KEY_LANGUAGE] = value }
    }

    suspend fun setTextSize(value: String) {
        dataStore.edit { it[KEY_TEXT_SIZE] = value }
    }

    suspend fun setProfileAvatarId(id: Int) {
        dataStore.edit { it[KEY_PROFILE_AVATAR_ID] = id }
    }

    suspend fun loadGameState(): GameUiState? {
        val json = dataStore.data.map { it[KEY_GAME_STATE_JSON] }.first()
        if (json.isNullOrBlank()) return null
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            (gson.fromJson(json, gameStateType) as? GameUiState)
        }.getOrNull()
    }

    suspend fun saveGameState(state: GameUiState) {
        val json = gson.toJson(state, gameStateType)
        dataStore.edit { it[KEY_GAME_STATE_JSON] = json }
    }

    /** Wipes all stored preferences (account, game, settings). Used when starting a new account from the sign-in screen. */
    suspend fun clearAllLocalData() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
