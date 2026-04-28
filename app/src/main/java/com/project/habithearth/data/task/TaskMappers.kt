package com.project.habithearth.data.task

import com.project.habithearth.data.proto.TaskProto
import com.project.habithearth.model.HabitTask
import com.project.habithearth.model.TaskCategory

/**
 * Conversion between the wire-format [TaskProto] and the domain [HabitTask].
 *
 * Kept in its own file (per PLAN.md target structure) so [TaskRepository] reads
 * as pure flow/edit logic and isn't cluttered by per-field mapping. Both
 * directions live here together because they describe the same contract -
 * splitting them would make a renumbering or new field easy to update on one
 * side and forget on the other.
 *
 * Category is stored as a string in the proto (PLAN.md risk #6) for forward-
 * compat with new categories. Unknown / corrupt category strings fall back to
 * [TaskCategory.UNSORTED] rather than throwing, so a future category removal
 * doesn't crash the app on an old install.
 *
 * `buildingId` uses empty-string-as-null in the proto because protobuf scalars
 * are non-nullable on the wire; the domain model exposes a real nullable so
 * UI code doesn't have to repeat the `isBlank()` dance.
 */
internal fun TaskProto.toDomain(): HabitTask =
    HabitTask(
        id = id,
        title = title,
        note = note,
        category = TaskCategory.entries.firstOrNull { it.name == category }
            ?: TaskCategory.UNSORTED,
        rewardAmount = rewardAmount.coerceAtLeast(1),
        isCompleted = isCompleted,
        buildingId = buildingId.takeIf { it.isNotBlank() },
    )

internal fun HabitTask.toProto(): TaskProto =
    TaskProto(
        id = id,
        title = title,
        note = note,
        category = category.name,
        rewardAmount = rewardAmount,
        isCompleted = isCompleted,
        buildingId = buildingId.orEmpty(),
    )
