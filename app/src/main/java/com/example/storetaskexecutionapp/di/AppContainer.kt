package com.example.storetaskexecutionapp.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storetaskexecutionapp.data.local.AppDatabase
import com.example.storetaskexecutionapp.data.local.PhotoStore
import com.example.storetaskexecutionapp.data.local.SessionStore
import com.example.storetaskexecutionapp.data.remote.NetworkModule
import com.example.storetaskexecutionapp.data.remote.NetworkMonitor
import com.example.storetaskexecutionapp.data.repository.TaskRepository
import com.example.storetaskexecutionapp.ui.login.LoginViewModel
import com.example.storetaskexecutionapp.ui.taskdetail.TaskDetailViewModel
import com.example.storetaskexecutionapp.ui.tasklist.TaskListViewModel

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val sessionStore = SessionStore(appContext)
    val networkMonitor = NetworkMonitor(appContext)
    private val database = AppDatabase.create(appContext)
    private val photoStore = PhotoStore(appContext)
    private val api = NetworkModule.createApi(networkMonitor)
    val taskRepository = TaskRepository(
        api = api,
        taskDao = database.taskDao(),
        photoStore = photoStore
    )

    fun loginFactory(): ViewModelProvider.Factory = factory {
        LoginViewModel(sessionStore)
    }

    fun taskListFactory(): ViewModelProvider.Factory = factory {
        TaskListViewModel(sessionStore, taskRepository, networkMonitor, appContext)
    }

    fun taskDetailFactory(taskId: String): ViewModelProvider.Factory = factory {
        TaskDetailViewModel(taskId, taskRepository, networkMonitor, appContext)
    }

    private inline fun <reified VM : ViewModel> factory(
        crossinline create: () -> VM
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(VM::class.java))
            return create() as T
        }
    }
}
