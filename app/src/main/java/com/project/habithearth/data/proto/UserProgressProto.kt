@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.project.habithearth.data.proto

// @ProtoNumber is part of kotlinx.serialization's experimental ProtoBuf surface.
// File-level opt-in above keeps every data class below from individually annotating.
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Persistence schema for every restart-surviving piece of Habit Hearth state.
 *
 * Encoded as protobuf wire format via kotlinx.serialization (see
 * [com.project.habithearth.data.datastore.UserProgressSerializer]). The
 * @[ProtoNumber] tags are part of the on-disk contract: do **not** renumber or
 * reuse them, and only ever add new fields with a higher number, otherwise
 * existing installs will fail to decode after upgrade.
 *
 * Naming notes:
 *   - `category` stays a string so adding a new TaskCategory does not require
 *     a schema change. Promote to enum later if categories stabilize.
 *   - empty `buildingId` means the task is filed under Home (no specific
 *     building). Kept as empty string rather than null because protobuf scalars
 *     are non-nullable and adding optionality later would be a wire change.
 *
 * Originally specified as a `.proto` file built by protobuf-gradle-plugin, but
 * that plugin (0.9.5) is incompatible with AGP 9.x. PLAN.md risk #2 pre-approved
 * the kotlinx.serialization fallback, which produces the same wire format.
 */
@Serializable
data class TaskProto(
    @ProtoNumber(1) val id: String = "",
    @ProtoNumber(2) val title: String = "",
    @ProtoNumber(3) val note: String = "",
    @ProtoNumber(4) val category: String = "",
    @ProtoNumber(5) val rewardAmount: Int = 0,
    @ProtoNumber(6) val isCompleted: Boolean = false,
    @ProtoNumber(7) val buildingId: String = "",
)

@Serializable
data class SettingsProto(
    @ProtoNumber(1) val pushNotifications: Boolean = false,
    @ProtoNumber(2) val vacationMode: Boolean = false,
    @ProtoNumber(3) val themeMode: String = "",
    @ProtoNumber(4) val language: String = "",
    @ProtoNumber(5) val textSize: String = "",
    @ProtoNumber(6) val profileAvatarId: Int = 0,
    @ProtoNumber(7) val displayName: String = "",
)

@Serializable
data class ProgressProto(
    @ProtoNumber(1) val strengthGems: Int = 0,
    @ProtoNumber(2) val wisdomGems: Int = 0,
    @ProtoNumber(3) val vitalityGems: Int = 0,
    @ProtoNumber(4) val spiritGems: Int = 0,
    @ProtoNumber(5) val coins: Int = 0,
    @ProtoNumber(6) val xpProgress: Float = 0f,
    @ProtoNumber(7) val ownedBuildingIds: List<String> = emptyList(),
)

/**
 * Top-level container persisted by `DataStore<UserProgressProto>`. All other
 * messages are nested fields of this single root so reads/writes stay atomic.
 */
@Serializable
data class UserProgressProto(
    @ProtoNumber(1) val settings: SettingsProto = SettingsProto(),
    @ProtoNumber(2) val progress: ProgressProto = ProgressProto(),
    @ProtoNumber(3) val tasks: List<TaskProto> = emptyList(),
) {
    companion object {
        /** Empty-default instance returned by the serializer on fresh install. */
        val DEFAULT: UserProgressProto = UserProgressProto()
    }
}
