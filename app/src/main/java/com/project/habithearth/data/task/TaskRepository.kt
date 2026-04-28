package com.project.habithearth.data.task

import androidx.datastore.core.DataStore
import com.project.habithearth.data.proto.UserProgressProto
import com.project.habithearth.model.HabitTask
import com.project.habithearth.model.TaskCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Single owner of habit-task persistence on top of `DataStore<UserProgressProto>`.
 *
 * Phase 3 of the DataStore refactor (see PLAN.md): replaces the task slice of
 * the legacy [com.project.habithearth.data.UserProgressRepository] JSON blob,
 * but does *not* yet rewire ViewModels or screens - those are Phase 4/5.
 *
 * Mutations follow the [DataStore.updateData] pattern, which serializes writes
 * and gives us read-modify-write atomicity for free; that matters because two
 * concurrent task edits or a complete-toggle racing with a building purchase
 * could otherwise drop one of the writes.
 *
 * Reward bookkeeping (gem/coin deltas on completion) is **deliberately not**
 * handled here. That math touches [com.project.habithearth.model.ResourceProgress]
 * and lives behind `ProgressRepository` in the second milestone. Until that
 * exists, callers (see [com.project.habithearth.ui.state.GameStateViewModel])
 * coordinate the reward delta themselves around [setTaskCompleted].
 */
class TaskRepository(
    private val dataStore: DataStore<UserProgressProto>,
) {

    /** All tasks in persisted order; emits on any task-list change. */
    val tasks: Flow<List<HabitTask>> = dataStore.data
        .map { proto -> proto.tasks.map { it.toDomain() } }
        .distinctUntilChanged()

    fun observeTasks(): Flow<List<HabitTask>> = tasks

    /**
     * Single-task stream. Emits `null` if the task was deleted out from under
     * an open editor, so the UI can navigate back instead of rendering stale
     * state. [distinctUntilChanged] avoids redundant recompositions when an
     * unrelated task field changes.
     */
    fun observeTask(taskId: String): Flow<HabitTask?> = dataStore.data
        .map { proto -> proto.tasks.firstOrNull { it.id == taskId }?.toDomain() }
        .distinctUntilChanged()

    fun observeTasksForBuilding(buildingId: String): Flow<List<HabitTask>> = dataStore.data
        .map { proto ->
            proto.tasks
                .filter { it.buildingId == buildingId }
                .map { it.toDomain() }
        }
        .distinctUntilChanged()

    /**
     * Creates and appends a new task. Returns the persisted [HabitTask] so
     * callers (e.g. an editor that wants to navigate to the new id) don't have
     * to re-derive it from the next stream emission.
     *
     * `buildingId` is normalized to null when blank: the proto stores empty
     * string, but every consumer treats blank == Home, and keeping the
     * domain-side type nullable matches [HabitTask].
     */
    suspend fun createTask(
        title: String,
        note: String,
        category: TaskCategory,
        rewardAmount: Int = 1,
        buildingId: String? = null,
    ): HabitTask {
        val task = HabitTask(
            title = title.trim(),
            note = note.trim(),
            category = category,
            rewardAmount = rewardAmount.coerceAtLeast(1),
            buildingId = buildingId?.trim()?.takeIf { it.isNotEmpty() },
        )
        dataStore.updateData { current ->
            current.copy(tasks = current.tasks + task.toProto())
        }
        return task
    }

    /**
     * Updates the editable fields of an existing task. No-op if [taskId] no
     * longer exists - silently dropping the edit is preferable to throwing,
     * because a delete-from-elsewhere racing with the editor would otherwise
     * crash on save.
     *
     * `isCompleted` and `rewardAmount` are intentionally not editable here:
     * completion goes through [setTaskCompleted] so the (future) reward delta
     * lives next to the toggle, and reward amount isn't user-tunable yet.
     */
    suspend fun updateTask(
        taskId: String,
        title: String,
        note: String,
        category: TaskCategory,
        buildingId: String?,
    ) {
        val normalizedTitle = title.trim()
        val normalizedNote = note.trim()
        val normalizedCategory = category.name
        val normalizedBuilding = buildingId?.trim()?.takeIf { it.isNotEmpty() }.orEmpty()
        dataStore.updateData { current ->
            // Walk the list and only rebuild it if a real edit landed. The
            // updateData transform is the wrong place for short-circuit-style
            // side effects (it can be re-run on retries), so we derive the
            // "anything changed" decision from `current` alone on each pass.
            var anyChanged = false
            val newTasks = current.tasks.map { proto ->
                if (proto.id == taskId) {
                    val taskChanged =
                        proto.title != normalizedTitle ||
                            proto.note != normalizedNote ||
                            proto.category != normalizedCategory ||
                            proto.buildingId != normalizedBuilding
                    if (taskChanged) {
                        anyChanged = true
                        proto.copy(
                            title = normalizedTitle,
                            note = normalizedNote,
                            category = normalizedCategory,
                            buildingId = normalizedBuilding,
                        )
                    } else {
                        proto
                    }
                } else {
                    proto
                }
            }
            if (anyChanged) current.copy(tasks = newTasks) else current
        }
    }

    /**
     * Toggles completion. Returns true iff the persisted state changed, so
     * callers can gate reward-pool math on a real transition (idempotent
     * re-checks shouldn't double-credit gems).
     */
    suspend fun setTaskCompleted(taskId: String, completed: Boolean): Boolean {
        // Avoid mutating an external `var` from inside the updateData transform:
        // DataStore may re-run the transform on contention, and a side effect
        // captured on the first pass can outlive a retry that ends up returning
        // `current` unchanged. Instead, snapshot the post-state at the end of
        // updateData and derive the return value from comparing pre/post.
        var localChanged = false
        dataStore.updateData { current ->
            val needsFlip = current.tasks.any { it.id == taskId && it.isCompleted != completed }
            localChanged = needsFlip
            if (!needsFlip) {
                current
            } else {
                val newTasks = current.tasks.map { proto ->
                    if (proto.id == taskId) proto.copy(isCompleted = completed) else proto
                }
                current.copy(tasks = newTasks)
            }
        }
        return localChanged
    }
}
