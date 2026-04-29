package com.project.habithearth

import android.app.Application
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.data.datastore.userProgressProtoDataStore
import com.project.habithearth.data.story.Chapter1ProgressRepository
import com.project.habithearth.data.task.TaskRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class HabitHearthApplication : Application() {

    lateinit var userProgressRepository: UserProgressRepository
        private set

    /**
     * Process-singleton task repository backed by the typed
     * [com.project.habithearth.data.datastore.userProgressProtoDataStore].
     *
     * Constructed here (not lazily inside ViewModels) so every consumer shares
     * the same DataStore subscription - DataStore is already a singleton via
     * its delegate, but threading the same repository instance also avoids
     * duplicating the in-memory state flow per ViewModel.
     *
     * As of Phase 5, the task-consuming screens (HomeScreen,
     * BuildingDetailScreen, TaskMakerScreen) all read this through
     * [com.project.habithearth.ui.tasks.TaskListViewModel] /
     * [com.project.habithearth.ui.tasks.TaskEditorViewModel].
     */
    lateinit var taskRepository: TaskRepository
        private set

    // Persists chapter 1 page history, locked-in choices, and ending flag so a
    // process kill in the middle of an arc doesn't drop the player back to the
    // begin screen. Lives in its own Preferences DataStore for now; the larger
    // PLAN.md refactor can fold it into the unified proto later.
    lateinit var chapter1ProgressRepository: Chapter1ProgressRepository
        private set

    // Process-wide signal that the debug "Reset progress" button fired. Any
    // ViewModel holding cached state derived from a repository can collect
    // this and reload itself; without it, debug-reset would clear disk while
    // long-lived in-memory state on other VMs (StoryViewModel) keeps showing
    // stale pages until the screen is destroyed.
    val debugResetEvents: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun onCreate() {
        super.onCreate()
        userProgressRepository = UserProgressRepository(this)
        taskRepository = TaskRepository(userProgressProtoDataStore)
        chapter1ProgressRepository = Chapter1ProgressRepository(this)
    }
}
