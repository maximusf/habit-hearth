package com.project.habithearth.model

/**
 * Persisted profile + app settings shape.
 *
 * Mirrors `SettingsProto` in `data/proto/UserProgressProto.kt`. Created in
 * Phase 2 so model boundaries are settled before Phase 3 introduces
 * `SettingsRepository` and the `ProgressMappers` proto-to-model conversions.
 */
data class ProfileSettings(
    val pushNotifications: Boolean = true,
    val vacationMode: Boolean = false,
    val themeMode: String = "system",
    val language: String = "en",
    val textSize: String = "default",
    val profileAvatarId: Int = 0,
    val displayName: String = "",
)
