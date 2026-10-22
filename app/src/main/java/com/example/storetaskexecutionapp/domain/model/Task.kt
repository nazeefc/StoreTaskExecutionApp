package com.example.storetaskexecutionapp.domain.model

data class StoreTask(
    val id: String,
    val employeeId: String,
    val title: String,
    val description: String,
    val storeName: String,
    val aisle: String,
    val status: TaskStatus,
    val dueDate: String,
    val beforePhotoPath: String?,
    val afterPhotoPath: String?,
    val completedAt: Long?,
    val isSynced: Boolean,
    val updatedAt: Long
) {
    val hasBeforePhoto: Boolean get() = !beforePhotoPath.isNullOrBlank()
    val hasAfterPhoto: Boolean get() = !afterPhotoPath.isNullOrBlank()
    val canComplete: Boolean get() = status != TaskStatus.COMPLETED && hasBeforePhoto && hasAfterPhoto
    val isComplete: Boolean get() = status == TaskStatus.COMPLETED
}

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

enum class PhotoType {
    BEFORE,
    AFTER
}
