package com.example.storetaskexecutionapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE employeeId = :employeeId ORDER BY dueDate ASC, title ASC")
    fun observeTasks(employeeId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun observeTask(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTask(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE employeeId = :employeeId")
    suspend fun getTasksForEmployee(employeeId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' AND isSynced = 0")
    suspend fun getUnsyncedCompleted(): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE employeeId = :employeeId AND status = 'COMPLETED' AND isSynced = 0")
    fun observePendingSyncCount(employeeId: String): Flow<Int>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM tasks WHERE employeeId = :employeeId AND isSynced = 1")
    suspend fun deleteSyncedForEmployee(employeeId: String)

    @Query(
        "DELETE FROM tasks WHERE employeeId = :employeeId AND isSynced = 1 AND id NOT IN (:keepIds)"
    )
    suspend fun deleteStaleSynced(employeeId: String, keepIds: List<String>)
}
