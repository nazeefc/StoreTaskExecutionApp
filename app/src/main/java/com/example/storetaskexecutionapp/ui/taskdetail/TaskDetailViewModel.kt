package com.example.storetaskexecutionapp.ui.taskdetail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storetaskexecutionapp.data.remote.NetworkMonitor
import com.example.storetaskexecutionapp.data.repository.TaskRepository
import com.example.storetaskexecutionapp.data.sync.SyncCompletedTasksWorker
import com.example.storetaskexecutionapp.domain.model.PhotoType
import com.example.storetaskexecutionapp.domain.model.StoreTask
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val task: StoreTask? = null,
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val isCompleting: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class TaskDetailViewModel(
    private val taskId: String,
    private val repository: TaskRepository,
    networkMonitor: NetworkMonitor,
    private val appContext: Context
) : ViewModel() {

    private val messages = MutableStateFlow(Messages())
    private var pendingPhoto: Pair<PhotoType, File>? = null

    val uiState: StateFlow<TaskDetailUiState> = combine(
        repository.observeTask(taskId),
        networkMonitor.isOnline,
        messages
    ) { task, isOnline, msg ->
        TaskDetailUiState(
            task = task,
            isLoading = false,
            isOffline = !isOnline,
            isCompleting = msg.isCompleting,
            errorMessage = msg.errorMessage,
            infoMessage = msg.infoMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskDetailUiState()
    )

    fun prepareCapture(type: PhotoType): Uri {
        val file = repository.createPhotoFile(taskId, type)
        pendingPhoto = type to file
        return repository.uriFor(file)
    }

    fun onCaptureResult(success: Boolean) {
        val pending = pendingPhoto
        pendingPhoto = null
        if (pending == null) return
        val (type, file) = pending
        if (!success || !file.exists() || file.length() == 0L) {
            if (file.exists()) file.delete()
            messages.update { it.copy(errorMessage = "Photo was not saved. Try again.") }
            return
        }
        viewModelScope.launch {
            repository.savePhoto(taskId, type, file.absolutePath)
            messages.update {
                it.copy(
                    errorMessage = null,
                    infoMessage = if (type == PhotoType.BEFORE) "Before photo saved" else "After photo saved"
                )
            }
        }
    }

    fun markCompleted() {
        val task = uiState.value.task ?: return
        if (!task.canComplete) {
            messages.update { it.copy(errorMessage = "Capture before and after photos first.") }
            return
        }
        viewModelScope.launch {
            messages.update { it.copy(isCompleting = true, errorMessage = null) }
            try {
                repository.markCompleted(taskId)
                SyncCompletedTasksWorker.enqueueNow(appContext)
                val info = if (uiState.value.isOffline) {
                    "Marked complete and saved locally. It will sync when you're back online."
                } else {
                    "Task completed. Syncing to the server."
                }
                messages.update { it.copy(isCompleting = false, infoMessage = info) }
            } catch (error: Exception) {
                messages.update {
                    it.copy(
                        isCompleting = false,
                        errorMessage = error.message ?: "Unable to complete this task."
                    )
                }
            }
        }
    }

    fun consumeMessages() {
        messages.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    private data class Messages(
        val isCompleting: Boolean = false,
        val errorMessage: String? = null,
        val infoMessage: String? = null
    )
}
