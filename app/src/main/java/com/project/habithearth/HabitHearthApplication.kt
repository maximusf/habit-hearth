package com.project.habithearth

import android.app.Application
import com.project.habithearth.data.UserProgressRepository

class HabitHearthApplication : Application() {

    lateinit var userProgressRepository: UserProgressRepository
        private set

    override fun onCreate() {
        super.onCreate()
        userProgressRepository = UserProgressRepository(this)
    }
}
