package com.project.habithearth.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.habithearth.data.task.TaskRepository
import com.project.habithearth.model.HabitTask
import com.project.habithearth.model.toEpochMs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Screen state holder for task lists (HomeScreen, BuildingDetailScreen,
 * TaskMakerScreen). Wired into all task-consuming screens as of Phase 5
 * (see PLAN.md): task persistence is owned by [TaskRepository] on the
 * typed DataStore, not by the legacy
 * [com.project.habithearth.ui.state.GameStateViewModel]. Reward-pool
 * bookkeeping (gem/coin deltas on completion) still routes through the
 * legacy VM until `ProgressRepository` lands in the second milestone.
 *
 * Hot vs cold flow notes:
 *   - [tasks] is hot ([SharingStarted.WhileSubscribed]) so multiple screens
 *     observing simultaneously share one DataStore subscription. The 5s grace
 *     period avoids re-reading the proto on configuration changes.
 *   - [tasksForBuilding] returns a cold [Flow] rather than caching a StateFlow
 *     per building id; otherwise this VM would leak a flow per visited
 *     building over a long session.
 */
class TaskListViewModel(
    private val repo: TaskRepository,
) : ViewModel() {

    val tasks: StateFlow<List<HabitTask>> = repo.observeTasks()
        .map { list ->
            val now = System.currentTimeMillis()
            list.sortedBy { task ->
                val lastMs = task.collectionLog.lastOrNull()?.timestamp?.toEpochMs() ?: 0L
                now - lastMs <= 24L * 60 * 60 * 1000  // on cooldown → true → sinks to bottom
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    fun tasksForBuilding(buildingId: String): Flow<List<HabitTask>> =
        repo.observeTasksForBuilding(buildingId).distinctUntilChanged()

    /**
     * Toggles completion via the repository. [onTransition] fires only when the
     * persisted state actually changed (not on no-op taps) and receives the task
     * snapshot and the signed gem/coin delta so the caller can route it to
     * [com.project.habithearth.ui.state.GameStateViewModel] without
     * double-crediting.
     *
     * On completion: calls [TaskRepository.collectCurrency] to apply streak
     * logic; the credited amount (rewardAmount × streakMultiplier) is passed to
     * [onTransition] as a positive delta.
     * On de-completion: delta is -rewardAmount; streak is not affected.
     */
    fun setCompleted(
        taskId: String,
        completed: Boolean,
        onTransition: ((HabitTask, Int) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            // Read the task from the repo, not from `tasks.value`. `tasks` is a
            // hot StateFlow shared with WhileSubscribed, so when only a sibling
            // flow (e.g. tasksForBuilding) is being collected, `tasks.value`
            // can stay at its initial empty value. Reading off the repo ensures
            // the toggle is correct regardless of which screen is active.
            val before = repo.observeTask(taskId).first() ?: return@launch
            val changed = repo.setTaskCompleted(taskId, completed)
            if (changed) {
                val delta = if (completed) {
                    repo.collectCurrency(taskId)
                } else {
                    -before.rewardAmount
                }
                onTransition?.invoke(before, delta)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class TaskListViewModelFactory(
    private val repo: TaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TaskListViewModel(repo) as T
}
