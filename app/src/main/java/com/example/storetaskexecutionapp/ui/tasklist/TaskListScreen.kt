package com.example.storetaskexecutionapp.ui.tasklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storetaskexecutionapp.domain.model.StoreTask
import com.example.storetaskexecutionapp.domain.model.TaskStatus
import com.example.storetaskexecutionapp.ui.components.EmptyState
import com.example.storetaskexecutionapp.ui.components.ErrorState
import com.example.storetaskexecutionapp.ui.components.LoadingState
import com.example.storetaskexecutionapp.ui.components.OfflineState

@Composable
fun TaskListRoute(
    viewModel: TaskListViewModel,
    onTaskClick: (String) -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loggedOut) {
        onLoggedOut()
        return
    }
    TaskListScreen(
        state = state,
        onRefresh = { viewModel.refresh() },
        onTaskClick = onTaskClick,
        onLogout = viewModel::logout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    state: TaskListUiState,
    onRefresh: () -> Unit,
    onTaskClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Assigned tasks") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Log out")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.employeeId.isNotBlank()) {
                Text(
                    text = "Employee ${state.employeeId}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.isOffline) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null)
                        Text(
                            text = if (state.pendingSyncCount > 0) {
                                "Offline · ${state.pendingSyncCount} completed task(s) will sync later"
                            } else {
                                "Offline · showing locally saved work"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading -> LoadingState()
                    state.tasks.isEmpty() && state.isOffline -> OfflineState(
                        message = state.errorMessage
                            ?: "Connect to the internet to download your assigned tasks.",
                        onRetry = onRefresh
                    )
                    state.tasks.isEmpty() && state.errorMessage != null && !state.isOffline -> ErrorState(
                        message = state.errorMessage,
                        onRetry = onRefresh
                    )
                    state.tasks.isEmpty() -> EmptyState(
                        title = "No tasks assigned",
                        message = "You’re all caught up. Pull to refresh or check back later.",
                        actionLabel = "Refresh",
                        onAction = onRefresh
                    )
                    else -> TaskListContent(
                        tasks = state.tasks,
                        onTaskClick = onTaskClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskListContent(
    tasks: List<StoreTask>,
    onTaskClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskCard(task = task, onClick = { onTaskClick(task.id) })
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TaskCard(task: StoreTask, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusChip(task.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${task.storeName} · ${task.aisle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Due ${task.dueDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (task.isComplete && !task.isSynced) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Saved locally · waiting to sync",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: TaskStatus) {
    val (label, container) = when (status) {
        TaskStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.surfaceVariant
        TaskStatus.IN_PROGRESS -> "In progress" to MaterialTheme.colorScheme.tertiaryContainer
        TaskStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.primaryContainer
    }
    Surface(color = container, shape = MaterialTheme.shapes.small) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
