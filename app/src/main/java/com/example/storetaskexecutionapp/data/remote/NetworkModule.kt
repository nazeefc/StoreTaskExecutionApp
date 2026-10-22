package com.example.storetaskexecutionapp.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    fun createApi(networkMonitor: NetworkMonitor): TaskApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(MockApiInterceptor { networkMonitor.isCurrentlyOnline() })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.storetasks.example/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskApiService::class.java)
    }
}
