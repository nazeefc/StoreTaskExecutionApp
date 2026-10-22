package com.example.storetaskexecutionapp.ui.tasklist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storetaskexecutionapp.data.local.SessionStore
import com.example.storetaskexecutionapp.data.remote.NetworkMonitor
import com.example.storetaskexecutionapp.data.repository.ApiException
import com.example.storetaskexecutionapp.data.repository.OfflineException
import com.example.storetaskexecutionapp.data.repository.TaskRepository
import com.example.storetaskexecutionapp.data.sync.SyncCompletedTasksWorker
import com.example.storetaskexecutionapp.domain.model.StoreTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskListUiState(
    val employeeId: String = "",
    val tasks: List<StoreTask> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val pendingSyncCount: Int = 0,
    val loggedOut: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModel(
    private val sessionStore: SessionStore,
    private val repository: TaskRepository,
    networkMonitor: NetworkMonitor,
    private val appContext: Context
) : ViewModel() {

    private val loadState = MutableStateFlow(LoadState())

    val uiState: StateFlow<TaskListUiState> = combine(
        sessionStore.employeeId,
        sessionStore.employeeId.flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList()) else repository.observeTasks(id)
        },
        sessionStore.employeeId.flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(0) else repository.observePendingSyncCount(id)
        },
        networkMonitor.isOnline,
        loadState
    ) { employeeId, tasks, pendingSync, isOnline, load ->
        TaskListUiState(
            employeeId = employeeId.orEmpty(),
            tasks = tasks,
            isLoading = load.isInitialLoading && tasks.isEmpty(),
            isRefreshing = load.isRefreshing,
            isOffline = !isOnline,
            errorMessage = load.errorMessage,
            pendingSyncCount = pendingSync,
            loggedOut = employeeId.isNullOrBlank()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskListUiState()
    )

    init {
        refresh(initial = true)
    }

    fun refresh(initial: Boolean = false) {
        val employeeId = sessionStore.currentEmployeeId() ?: return
        viewModelScope.launch {
            loadState.update {
                it.copy(
                    isInitialLoading = initial && uiState.value.tasks.isEmpty(),
                    isRefreshing = !initial,
                    errorMessage = null
                )
            }
            try {
                repository.refreshAssignedTasks(employeeId)
                SyncCompletedTasksWorker.enqueueNow(appContext)
                loadState.update { it.copy(isInitialLoading = false, isRefreshing = false, errorMessage = null) }
            } catch (_: OfflineException) {
                loadState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        errorMessage = if (uiState.value.tasks.isEmpty()) {
                            "You're offline. Connect to the internet to load assigned tasks."
                        } else {
                            "You're offline. Showing saved tasks."
                        }
                    )
                }
            } catch (error: ApiException) {
                loadState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Unable to load tasks."
                    )
                }
            } catch (_: Exception) {
                loadState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        errorMessage = "Something went wrong while loading tasks."
                    )
                }
            }
        }
    }

    fun logout() {
        sessionStore.logout()
    }

    private data class LoadState(
        val isInitialLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null
    )
}
