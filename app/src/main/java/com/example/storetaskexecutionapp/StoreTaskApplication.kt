package com.example.storetaskexecutionapp

import android.app.Application
import com.example.storetaskexecutionapp.data.sync.SyncCompletedTasksWorker
import com.example.storetaskexecutionapp.di.AppContainer

class StoreTaskApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.networkMonitor.start()
        SyncCompletedTasksWorker.enqueuePeriodic(this)
    }
}
