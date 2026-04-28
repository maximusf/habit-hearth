package com.project.habithearth

import android.app.Application
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.data.datastore.userProgressDataStore
import com.project.habithearth.data.task.TaskRepository

class HabitHearthApplication : Application() {

    lateinit var userProgressRepository: UserProgressRepository
        private set

    /**
     * Process-singleton task repository backed by the typed
     * [com.project.habithearth.data.datastore.userProgressDataStore].
     *
     * Constructed here (not lazily inside ViewModels) so every consumer shares
     * the same DataStore subscription - DataStore is already a singleton via
     * its delegate, but threading the same repository instance also avoids
     * duplicating the in-memory state flow per ViewModel.
     *
     * Phase 4 wires this up but does not yet route screens through it; that
     * happens in Phase 5.
     */
    lateinit var taskRepository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        userProgressRepository = UserProgressRepository(this, userProgressDataStore)
        taskRepository = TaskRepository(userProgressDataStore)
    }
}
