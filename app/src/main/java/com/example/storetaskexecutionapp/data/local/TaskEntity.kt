package com.example.storetaskexecutionapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.storetaskexecutionapp.domain.model.StoreTask
import com.example.storetaskexecutionapp.domain.model.TaskStatus

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["employeeId"])]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val employeeId: String,
    val title: String,
    val description: String,
    val storeName: String,
    val aisle: String,
    val status: String,
    val dueDate: String,
    val beforePhotoPath: String?,
    val afterPhotoPath: String?,
    val completedAt: Long?,
    val isSynced: Boolean,
    val updatedAt: Long
) {
    fun toDomain(): StoreTask = StoreTask(
        id = id,
        employeeId = employeeId,
        title = title,
        description = description,
        storeName = storeName,
        aisle = aisle,
        status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.PENDING),
        dueDate = dueDate,
        beforePhotoPath = beforePhotoPath,
        afterPhotoPath = afterPhotoPath,
        completedAt = completedAt,
        isSynced = isSynced,
        updatedAt = updatedAt
    )
}

fun StoreTask.toEntity(): TaskEntity = TaskEntity(
    id = id,
    employeeId = employeeId,
    title = title,
    description = description,
    storeName = storeName,
    aisle = aisle,
    status = status.name,
    dueDate = dueDate,
    beforePhotoPath = beforePhotoPath,
    afterPhotoPath = afterPhotoPath,
    completedAt = completedAt,
    isSynced = isSynced,
    updatedAt = updatedAt
)
