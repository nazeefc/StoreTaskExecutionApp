package com.example.storetaskexecutionapp.ui.taskdetail

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.storetaskexecutionapp.domain.model.PhotoType
import com.example.storetaskexecutionapp.domain.model.StoreTask
import com.example.storetaskexecutionapp.ui.components.EmptyState
import com.example.storetaskexecutionapp.ui.components.LoadingState
import com.example.storetaskexecutionapp.ui.tasklist.StatusChip
import java.io.File

@Composable
fun TaskDetailRoute(
    viewModel: TaskDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pendingCapture by remember { mutableStateOf<PhotoType?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onCaptureResult(success)
        pendingCapture = null
        captureUri = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val type = pendingCapture
        if (granted && type != null) {
            val uri = viewModel.prepareCapture(type)
            captureUri = uri
            takePicture.launch(uri)
        } else {
            pendingCapture = null
        }
    }

    fun requestCapture(type: PhotoType) {
        pendingCapture = type
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = viewModel.prepareCapture(type)
            captureUri = uri
            takePicture.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        val message = state.errorMessage ?: state.infoMessage
        if (!message.isNullOrBlank()) {
            snackbar.showSnackbar(message)
            viewModel.consumeMessages()
        }
    }

    TaskDetailScreen(
        state = state,
        snackbarHostState = snackbar,
        onBack = onBack,
        onCaptureBefore = { requestCapture(PhotoType.BEFORE) },
        onCaptureAfter = { requestCapture(PhotoType.AFTER) },
        onComplete = viewModel::markCompleted
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    state: TaskDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCaptureBefore: () -> Unit,
    onCaptureAfter: () -> Unit,
    onComplete: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.task?.title ?: "Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val task = state.task
            if (!state.isLoading && task != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    tonalElevation = 2.dp
                ) {
                    if (task.isComplete) {
                        val syncLabel = if (task.isSynced) {
                            "Completed and synced"
                        } else {
                            "Completed · saved locally until sync"
                        }
                        Text(
                            text = syncLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    } else {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = onComplete,
                                enabled = task.canComplete && !state.isCompleting,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (state.isCompleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Mark as completed")
                                }
                            }
                            if (!task.canComplete) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Capture both before and after photos to complete this task.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(modifier = Modifier.padding(innerPadding), message = "Loading task…")
            state.task == null -> EmptyState(
                title = "Task not found",
                message = "This task is no longer available on this device.",
                modifier = Modifier.padding(innerPadding),
                actionLabel = "Go back",
                onAction = onBack
            )
            else -> TaskDetailContent(
                task = state.task,
                isOffline = state.isOffline,
                onCaptureBefore = onCaptureBefore,
                onCaptureAfter = onCaptureAfter,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun TaskDetailContent(
    task: StoreTask,
    isOffline: Boolean,
    onCaptureBefore: () -> Unit,
    onCaptureAfter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(task.status)
            if (isOffline) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Offline", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(text = task.storeName, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${task.aisle} · Due ${task.dueDate}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(text = task.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Text(text = "Photo evidence", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        PhotoSlot(
            title = "Before",
            path = task.beforePhotoPath,
            enabled = !task.isComplete,
            onCapture = onCaptureBefore
        )
        Spacer(Modifier.height(12.dp))
        PhotoSlot(
            title = "After",
            path = task.afterPhotoPath,
            enabled = !task.isComplete,
            onCapture = onCaptureAfter
        )
    }
}

@Composable
private fun PhotoSlot(
    title: String,
    path: String?,
    enabled: Boolean,
    onCapture: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            if (!path.isNullOrBlank()) {
                AsyncImage(
                    model = File(path),
                    contentDescription = "$title photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = onCapture,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (path.isNullOrBlank()) "Capture $title photo" else "Retake $title photo")
            }
        }
    }
}
