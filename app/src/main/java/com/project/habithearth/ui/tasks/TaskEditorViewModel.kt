package com.project.habithearth.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.project.habithearth.data.task.TaskRepository
import com.project.habithearth.model.HabitTask
import com.project.habithearth.model.TaskCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SAVED_TASK_ID_KEY = "task_editor.task_id"
private const val SAVED_INITIAL_BUILDING_KEY = "task_editor.initial_building_id"

/**
 * Screen state holder for [TaskMakerScreen] (create + edit).
 *
 * Phase 4 of the DataStore refactor (see PLAN.md): created but not yet wired
 * - Phase 5 swaps [TaskMakerScreen] off [com.project.habithearth.ui.state.GameStateViewModel]
 * onto this VM.
 *
 * Why [SavedStateHandle]:
 *   - Editor route arguments (`taskId`, `initialBuildingId`) live in the back
 *     stack, so the navigation library hands them in as saved state. Reading
 *     them here means the same VM works for both "new task" and "edit task"
 *     navigations without a separate factory call site.
 *   - Survives process death; the editor reopens on the right task after a
 *     low-memory kill.
 *
 * The mode (new vs edit) is fixed by whether [SAVED_TASK_ID_KEY] is set when
 * the VM is constructed. Switching modes mid-session is not supported - the
 * navigation graph routes new and edit through different destinations, so
 * each gets its own VM instance.
 */
class TaskEditorViewModel(
    private val repo: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val taskId: String? = savedStateHandle[SAVED_TASK_ID_KEY]
    val initialBuildingId: String? = savedStateHandle[SAVED_INITIAL_BUILDING_KEY]

    val isEditMode: Boolean = taskId != null

    /**
     * Stream of the task being edited, or `null` for new-task mode and after a
     * concurrent delete. Cold flow upgraded to a [StateFlow] so the screen can
     * read an initial value synchronously when prefilling form fields.
     */
    val editingTask: StateFlow<HabitTask?> = (
        taskId?.let { repo.observeTask(it) } ?: flowOf(null)
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    /**
     * Persists the form. Returns the resulting task id for navigation back.
     * Edit-mode no-ops (returns the same id) when the underlying task no
     * longer exists - matches [TaskRepository.updateTask] semantics.
     */
    suspend fun save(
        title: String,
        note: String,
        category: TaskCategory,
        buildingId: String?,
    ): String {
        return if (taskId != null) {
            repo.updateTask(
                taskId = taskId,
                title = title,
                note = note,
                category = category,
                buildingId = buildingId,
            )
            taskId
        } else {
            val created = repo.createTask(
                title = title,
                note = note,
                category = category,
                buildingId = buildingId,
            )
            created.id
        }
    }

    /** Fire-and-forget variant for callers that don't need the resulting id. */
    fun saveAsync(
        title: String,
        note: String,
        category: TaskCategory,
        buildingId: String?,
        onSaved: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            val id = save(title, note, category, buildingId)
            onSaved(id)
        }
    }
}

/**
 * Factory that injects [TaskRepository] while letting the ViewModel system
 * provide [SavedStateHandle] from the route arguments. Implemented via
 * [CreationExtras] (the modern AbstractSavedStateViewModelFactory replacement)
 * so the route's nav arguments flow into the saved state automatically when
 * constructed inside `viewModel(factory = ...)`.
 *
 * Callers should put the route's `taskId` / `initialBuildingId` into the
 * defaultArgs of the back stack entry (see [taskEditorCreationExtras]) before
 * resolving the VM.
 */
@Suppress("UNCHECKED_CAST")
class TaskEditorViewModelFactory(
    private val repo: TaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        return TaskEditorViewModel(repo, savedStateHandle) as T
    }
}

/**
 * Builds [CreationExtras] carrying the editor's route arguments so the
 * factory can hand them to [SavedStateHandle]. Use from a composable that
 * already resolved the nav route's args.
 */
fun taskEditorCreationExtras(
    base: CreationExtras,
    taskId: String?,
    initialBuildingId: String?,
): CreationExtras = MutableCreationExtras(base).apply {
    set(
        androidx.lifecycle.DEFAULT_ARGS_KEY,
        android.os.Bundle().apply {
            taskId?.let { putString(SAVED_TASK_ID_KEY, it) }
            initialBuildingId?.let { putString(SAVED_INITIAL_BUILDING_KEY, it) }
        },
    )
}
