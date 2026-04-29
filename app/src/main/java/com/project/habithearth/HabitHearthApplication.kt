package com.project.habithearth

import android.app.Application
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.data.datastore.userProgressProtoDataStore
import com.project.habithearth.data.task.TaskRepository
import com.project.habithearth.notifications.TaskReminderScheduler

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

    override fun onCreate() {
        super.onCreate()
        userProgressRepository = UserProgressRepository(this)
        taskRepository = TaskRepository(userProgressProtoDataStore)
        TaskReminderScheduler.ensureChannel(this)
    }
}
