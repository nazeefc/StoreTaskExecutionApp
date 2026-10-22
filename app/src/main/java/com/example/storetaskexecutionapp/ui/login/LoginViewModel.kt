package com.example.storetaskexecutionapp.ui.login

import androidx.lifecycle.ViewModel
import com.example.storetaskexecutionapp.data.local.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val employeeId: String = "",
    val error: String? = null,
    val loggedIn: Boolean = false
)

class LoginViewModel(
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(loggedIn = sessionStore.currentEmployeeId() != null)
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmployeeIdChanged(value: String) {
        _uiState.update { it.copy(employeeId = value, error = null) }
    }

    fun login() {
        val id = _uiState.value.employeeId.trim()
        val error = validate(id)
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        sessionStore.login(id)
        _uiState.update { it.copy(error = null, loggedIn = true) }
    }

    private fun validate(id: String): String? = when {
        id.isBlank() -> "Enter your employee ID"
        id.length < 3 -> "Employee ID must be at least 3 characters"
        id.length > 20 -> "Employee ID is too long"
        !id.matches(Regex("^[A-Za-z0-9_-]+$")) -> "Use letters, numbers, - or _ only"
        else -> null
    }
}
