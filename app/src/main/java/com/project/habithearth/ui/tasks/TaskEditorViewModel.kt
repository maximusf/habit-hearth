package com.project.habithearth.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.project.habithearth.data.task.TaskRepository
import com.project.habithearth.model.HabitTask
import com.project.habithearth.model.TaskCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Match the navigation arg names declared in HabitHearthApp.kt's task_maker
// routes. Reading them straight off SavedStateHandle lets the nav back-stack
// entry's arguments bundle flow in without a custom CreationExtras helper.
private const val ARG_TASK_ID = "taskId"
private const val ARG_BUILDING_ID = "buildingId"

/**
 * Screen state holder for [TaskMakerScreen] (create + edit).
 *
 * Why [SavedStateHandle]:
 *   - Editor route arguments (`taskId`, `buildingId`) live in the navigation
 *     back stack entry, which the lifecycle library exposes through saved
 *     state automatically. Reading them here means the same VM works for both
 *     "new task" and "edit task" navigations without a separate factory call.
 *   - Survives process death; the editor reopens on the right task after a
 *     low-memory kill.
 *
 * The mode (new vs edit) is fixed by whether the route supplied a `taskId`
 * when the VM is constructed. Switching modes mid-session is not supported -
 * the navigation graph routes new and edit through different destinations,
 * so each gets its own VM instance.
 */
class TaskEditorViewModel(
    private val repo: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val taskId: String? = savedStateHandle[ARG_TASK_ID]
    val initialBuildingId: String? = savedStateHandle[ARG_BUILDING_ID]

    val isEditMode: Boolean = taskId != null

    private val _isEditingTaskLoaded = MutableStateFlow(!isEditMode)

    /**
     * True once the editor has observed at least one emission from the
     * underlying repo flow (or immediately, in new-task mode where there is
     * no flow to wait on). Lets the UI distinguish "DataStore hasn't replied
     * yet" from "the task we were asked to edit really doesn't exist", which
     * matters because an invalid `taskId` route arg should snap navigation
     * back rather than leave a permanently-blank editor.
     */
    val isEditingTaskLoaded: StateFlow<Boolean> = _isEditingTaskLoaded.asStateFlow()

    /**
     * Stream of the task being edited, or `null` for new-task mode and after a
     * concurrent delete. Cold flow upgraded to a [StateFlow] so the screen can
     * read an initial value synchronously when prefilling form fields.
     */
    val editingTask: StateFlow<HabitTask?> = (
        taskId?.let { repo.observeTask(it).onEach { _isEditingTaskLoaded.value = true } }
            ?: flowOf(null)
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
 * provide [SavedStateHandle] from the back stack entry's nav arguments. The
 * [createSavedStateHandle] extension on [CreationExtras] is the modern
 * replacement for AbstractSavedStateViewModelFactory and works inside
 * `viewModel(factory = ...)` without any CreationExtras plumbing on the call
 * site - the nav library populates the entry's extras for us.
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
