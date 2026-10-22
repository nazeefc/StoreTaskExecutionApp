package com.example.storetaskexecutionapp.data.remote

import com.example.storetaskexecutionapp.data.remote.dto.SyncRequest
import com.example.storetaskexecutionapp.data.remote.dto.SyncResponse
import com.example.storetaskexecutionapp.data.remote.dto.TasksResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TaskApiService {
    @GET("employees/{employeeId}/tasks")
    suspend fun getAssignedTasks(@Path("employeeId") employeeId: String): TasksResponse

    @POST("tasks/sync")
    suspend fun syncCompletedTasks(@Body request: SyncRequest): SyncResponse
}
