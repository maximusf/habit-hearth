package com.project.habithearth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.project.habithearth.ui.map.MainHubBuildingIds
import com.project.habithearth.ui.state.GameUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userProgressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "habit_hearth_progress",
)

private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
private val KEY_GAME_STATE_JSON = stringPreferencesKey("game_state_json")
private val KEY_PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications")
private val KEY_VACATION_MODE = booleanPreferencesKey("vacation_mode")
private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
private val KEY_LANGUAGE = stringPreferencesKey("language")
private val KEY_TEXT_SIZE = stringPreferencesKey("text_size")
private val KEY_PROFILE_AVATAR_ID = intPreferencesKey("profile_avatar_id")
private val KEY_NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
private val KEY_NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")

data class AccountSettings(
    val displayName: String,
    val pushNotifications: Boolean,
    val vacationMode: Boolean,
    val themeMode: String,
    val language: String,
    val textSize: String,
    val profileAvatarId: Int,
    val notificationHour: Int,
    val notificationMinute: Int,
) {
    companion object {
        val DEFAULT = AccountSettings(
            displayName = "",
            pushNotifications = false,
            vacationMode = false,
            themeMode = "System default",
            language = "English",
            textSize = "Default",
            profileAvatarId = 0,
            notificationHour = 20,
            notificationMinute = 0,
        )
    }
}

class UserProgressRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.userProgressDataStore
    private val gson = Gson()
    private val gameStateType = object : TypeToken<GameUiState>() {}.type

    val accountSettings: Flow<AccountSettings> = dataStore.data.map { prefs ->
        AccountSettings(
            displayName = prefs[KEY_DISPLAY_NAME].orEmpty(),
            pushNotifications = prefs[KEY_PUSH_NOTIFICATIONS] ?: AccountSettings.DEFAULT.pushNotifications,
            vacationMode = prefs[KEY_VACATION_MODE] ?: AccountSettings.DEFAULT.vacationMode,
            themeMode = prefs[KEY_THEME_MODE] ?: AccountSettings.DEFAULT.themeMode,
            language = prefs[KEY_LANGUAGE] ?: AccountSettings.DEFAULT.language,
            textSize = prefs[KEY_TEXT_SIZE] ?: AccountSettings.DEFAULT.textSize,
            profileAvatarId = prefs[KEY_PROFILE_AVATAR_ID] ?: AccountSettings.DEFAULT.profileAvatarId,
            notificationHour = prefs[KEY_NOTIFICATION_HOUR] ?: AccountSettings.DEFAULT.notificationHour,
            notificationMinute = prefs[KEY_NOTIFICATION_MINUTE] ?: AccountSettings.DEFAULT.notificationMinute,
        )
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

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[KEY_NOTIFICATION_HOUR] = hour.coerceIn(0, 23)
            it[KEY_NOTIFICATION_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun loadGameState(): GameUiState? {
        val json = dataStore.data.map { it[KEY_GAME_STATE_JSON] }.first()
        if (json.isNullOrBlank()) return null
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            // Seed starter hubs when the field is missing, null, or an empty
            // array. The empty-array case shows up on installs that pre-date
            // MainHubBuildingIds defaulting: the JSON has the key but the list
            // is [], so without this branch the user lands on a board where
            // even the starter hubs render locked.
            val owned = root.get("ownedBuildingIds")
            val needsSeed = owned == null ||
                owned.isJsonNull ||
                (owned.isJsonArray && owned.asJsonArray.size() == 0)
            if (needsSeed) {
                val arr = JsonArray()
                MainHubBuildingIds.forEach { arr.add(it) }
                root.add("ownedBuildingIds", arr)
            }
            @Suppress("UNCHECKED_CAST")
            (gson.fromJson(root, gameStateType) as? GameUiState)
        }.getOrNull()
    }

    suspend fun saveGameState(state: GameUiState) {
        val json = gson.toJson(state, gameStateType)
        dataStore.edit { it[KEY_GAME_STATE_JSON] = json }
    }
}
