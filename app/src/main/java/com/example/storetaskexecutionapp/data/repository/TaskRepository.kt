package com.example.storetaskexecutionapp.data.repository

import com.example.storetaskexecutionapp.data.local.PhotoStore
import com.example.storetaskexecutionapp.data.local.TaskDao
import com.example.storetaskexecutionapp.data.local.TaskEntity
import com.example.storetaskexecutionapp.data.remote.TaskApiService
import com.example.storetaskexecutionapp.data.remote.dto.CompletedTaskDto
import com.example.storetaskexecutionapp.data.remote.dto.SyncRequest
import com.example.storetaskexecutionapp.domain.model.PhotoType
import com.example.storetaskexecutionapp.domain.model.StoreTask
import com.example.storetaskexecutionapp.domain.model.TaskStatus
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

class TaskRepository(
    private val api: TaskApiService,
    private val taskDao: TaskDao,
    private val photoStore: PhotoStore
) {
    fun observeTasks(employeeId: String): Flow<List<StoreTask>> =
        taskDao.observeTasks(employeeId).map { list -> list.map { it.toDomain() } }

    fun observeTask(taskId: String): Flow<StoreTask?> =
        taskDao.observeTask(taskId).map { it?.toDomain() }

    fun observePendingSyncCount(employeeId: String): Flow<Int> =
        taskDao.observePendingSyncCount(employeeId)

    suspend fun refreshAssignedTasks(employeeId: String) {
        val response = try {
            api.getAssignedTasks(employeeId)
        } catch (error: HttpException) {
            throw ApiException(error.message ?: "Unable to load tasks", error.code())
        } catch (error: IOException) {
            throw OfflineException()
        }

        val now = System.currentTimeMillis()
        val local = taskDao.getTasksForEmployee(employeeId)
        val localById = local.associateBy { it.id }
        val merged = response.tasks.map { dto ->
            merge(localById[dto.id], dto.toEntity(employeeId, now), now)
        }
        val remoteIds = merged.map { it.id }.toSet()
        val localOnly = local.filter { entity ->
            entity.id !in remoteIds && shouldKeepLocalOnly(entity)
        }

        val keepIds = (merged.map { it.id } + localOnly.map { it.id }).distinct()
        if (keepIds.isEmpty()) {
            taskDao.deleteSyncedForEmployee(employeeId)
        } else {
            taskDao.deleteStaleSynced(employeeId, keepIds)
        }
        taskDao.upsertAll(merged + localOnly)
    }

    suspend fun savePhoto(taskId: String, type: PhotoType, path: String) {
        val current = taskDao.getTask(taskId) ?: return
        val nextStatus = when {
            current.status == TaskStatus.COMPLETED.name -> current.status
            else -> TaskStatus.IN_PROGRESS.name
        }
        val updated = when (type) {
            PhotoType.BEFORE -> current.copy(
                beforePhotoPath = path,
                status = nextStatus,
                updatedAt = System.currentTimeMillis()
            )
            PhotoType.AFTER -> current.copy(
                afterPhotoPath = path,
                status = nextStatus,
                updatedAt = System.currentTimeMillis()
            )
        }
        taskDao.upsert(updated)
    }

    suspend fun markCompleted(taskId: String) {
        val current = taskDao.getTask(taskId) ?: error("Task not found")
        if (current.beforePhotoPath.isNullOrBlank() || current.afterPhotoPath.isNullOrBlank()) {
            error("Before and after photos are required")
        }
        if (current.status == TaskStatus.COMPLETED.name) return
        taskDao.upsert(
            current.copy(
                status = TaskStatus.COMPLETED.name,
                completedAt = System.currentTimeMillis(),
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun syncCompletedTasks(): Int {
        val pending = taskDao.getUnsyncedCompleted()
        if (pending.isEmpty()) return 0
        val request = SyncRequest(
            tasks = pending.map { entity ->
                CompletedTaskDto(
                    id = entity.id,
                    employeeId = entity.employeeId,
                    completedAt = entity.completedAt ?: entity.updatedAt,
                    hasBeforePhoto = !entity.beforePhotoPath.isNullOrBlank(),
                    hasAfterPhoto = !entity.afterPhotoPath.isNullOrBlank()
                )
            }
        )
        val response = try {
            api.syncCompletedTasks(request)
        } catch (_: IOException) {
            throw OfflineException()
        } catch (error: HttpException) {
            throw ApiException("Sync failed", error.code())
        }
        val pendingIds = pending.mapTo(mutableSetOf()) { it.id }
        val ids = response.syncedIds.filter { it in pendingIds }.distinct()
        if (ids.isNotEmpty()) {
            taskDao.markSynced(ids)
        }
        return ids.size
    }

    fun createPhotoFile(taskId: String, type: PhotoType) = photoStore.createPhotoFile(taskId, type)

    fun uriFor(file: java.io.File) = photoStore.uriFor(file)

    private fun shouldKeepLocalOnly(entity: TaskEntity): Boolean {
        val completedUnsynced = entity.status == TaskStatus.COMPLETED.name && !entity.isSynced
        val hasLocalWork = !entity.beforePhotoPath.isNullOrBlank() || !entity.afterPhotoPath.isNullOrBlank()
        return completedUnsynced || hasLocalWork
    }

    private fun merge(local: TaskEntity?, remote: TaskEntity, now: Long): TaskEntity {
        if (local == null) return remote
        val localStatus = runCatching { TaskStatus.valueOf(local.status) }.getOrDefault(TaskStatus.PENDING)
        val status = when {
            localStatus == TaskStatus.COMPLETED -> TaskStatus.COMPLETED.name
            localStatus == TaskStatus.IN_PROGRESS -> TaskStatus.IN_PROGRESS.name
            else -> remote.status
        }
        return remote.copy(
            beforePhotoPath = local.beforePhotoPath,
            afterPhotoPath = local.afterPhotoPath,
            status = status,
            completedAt = local.completedAt,
            isSynced = if (localStatus == TaskStatus.COMPLETED) local.isSynced else true,
            updatedAt = now
        )
    }
}

class OfflineException : IOException("No internet connection")

class ApiException(message: String, val code: Int) : IOException(message)
