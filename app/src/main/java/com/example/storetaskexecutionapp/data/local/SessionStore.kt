package com.example.storetaskexecutionapp.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _employeeId = MutableStateFlow(prefs.getString(KEY_EMPLOYEE_ID, null))
    val employeeId: StateFlow<String?> = _employeeId.asStateFlow()

    fun currentEmployeeId(): String? = _employeeId.value

    fun login(employeeId: String) {
        val normalized = employeeId.trim().uppercase()
        prefs.edit().putString(KEY_EMPLOYEE_ID, normalized).apply()
        _employeeId.value = normalized
    }

    fun logout() {
        prefs.edit().remove(KEY_EMPLOYEE_ID).apply()
        _employeeId.value = null
    }

    companion object {
        private const val PREFS = "store_task_session"
        private const val KEY_EMPLOYEE_ID = "employee_id"
    }
}
