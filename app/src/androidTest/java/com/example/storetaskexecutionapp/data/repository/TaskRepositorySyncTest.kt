package com.example.storetaskexecutionapp.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.storetaskexecutionapp.data.local.AppDatabase
import com.example.storetaskexecutionapp.data.local.PhotoStore
import com.example.storetaskexecutionapp.data.local.TaskDao
import com.example.storetaskexecutionapp.data.local.TaskEntity
import com.example.storetaskexecutionapp.data.remote.TaskApiService
import com.example.storetaskexecutionapp.data.remote.dto.SyncRequest
import com.example.storetaskexecutionapp.data.remote.dto.SyncResponse
import com.example.storetaskexecutionapp.data.remote.dto.TasksResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class TaskRepositorySyncTest {
    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var api: FakeTaskApiService
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        api = FakeTaskApiService()
        repository = TaskRepository(api, taskDao, PhotoStore(context))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun emptyAcknowledgmentLeavesPendingTasksUnsynced() = runBlocking {
        insertPendingTask("task-1")

        val syncedCount = repository.syncCompletedTasks()

        assertEquals(0, syncedCount)
        assertFalse(requireNotNull(taskDao.getTask("task-1")).isSynced)
    }

    @Test
    fun partialAcknowledgmentMarksOnlyAcknowledgedTasksSynced() = runBlocking {
        insertPendingTask("task-1")
        insertPendingTask("task-2")
        api.response = SyncResponse(syncedIds = listOf("task-1"))

        val syncedCount = repository.syncCompletedTasks()

        assertEquals(1, syncedCount)
        assertTrue(requireNotNull(taskDao.getTask("task-1")).isSynced)
        assertFalse(requireNotNull(taskDao.getTask("task-2")).isSynced)
    }

    @Test
    fun acknowledgmentForTaskOutsidePendingBatchIsIgnored() = runBlocking {
        insertPendingTask("task-1")
        api.response = SyncResponse(syncedIds = listOf("other-task"))

        val syncedCount = repository.syncCompletedTasks()

        assertEquals(0, syncedCount)
        assertFalse(requireNotNull(taskDao.getTask("task-1")).isSynced)
    }

    private suspend fun insertPendingTask(id: String) {
        taskDao.upsert(
            TaskEntity(
                id = id,
                employeeId = "employee-1",
                title = "Task $id",
                description = "",
                storeName = "Store",
                aisle = "Aisle 1",
                status = "COMPLETED",
                dueDate = "2026-10-22",
                beforePhotoPath = null,
                afterPhotoPath = null,
                completedAt = 1L,
                isSynced = false,
                updatedAt = 1L
            )
        )
    }

    private class FakeTaskApiService : TaskApiService {
        var response = SyncResponse()

        override suspend fun getAssignedTasks(employeeId: String) = TasksResponse()

        override suspend fun syncCompletedTasks(request: SyncRequest) = response
    }
}
