package com.example.storetaskexecutionapp.data.remote.dto

import com.example.storetaskexecutionapp.data.local.TaskEntity
import com.example.storetaskexecutionapp.domain.model.TaskStatus
import com.google.gson.annotations.SerializedName

data class TasksResponse(
    @SerializedName("tasks") val tasks: List<TaskDto> = emptyList()
)

data class TaskDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("storeName") val storeName: String,
    @SerializedName("aisle") val aisle: String,
    @SerializedName("status") val status: String,
    @SerializedName("dueDate") val dueDate: String
) {
    fun toEntity(employeeId: String, now: Long): TaskEntity = TaskEntity(
        id = id,
        employeeId = employeeId,
        title = title,
        description = description,
        storeName = storeName,
        aisle = aisle,
        status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.PENDING).name,
        dueDate = dueDate,
        beforePhotoPath = null,
        afterPhotoPath = null,
        completedAt = null,
        isSynced = true,
        updatedAt = now
    )
}

data class SyncRequest(
    @SerializedName("tasks") val tasks: List<CompletedTaskDto>
)

data class CompletedTaskDto(
    @SerializedName("id") val id: String,
    @SerializedName("employeeId") val employeeId: String,
    @SerializedName("completedAt") val completedAt: Long,
    @SerializedName("hasBeforePhoto") val hasBeforePhoto: Boolean,
    @SerializedName("hasAfterPhoto") val hasAfterPhoto: Boolean
)

data class SyncResponse(
    @SerializedName("syncedIds") val syncedIds: List<String> = emptyList()
)
