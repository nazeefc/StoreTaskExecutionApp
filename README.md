# StoreTaskExecutionApp

A Kotlin Android app for store employees to view assigned tasks, capture before-and-after photo evidence, and complete tasks. The app uses Jetpack Compose and keeps task work on-device so tasks can still be completed while offline.

## Features

- Employee ID demo login with basic validation and a locally saved session.
- Assigned task list with refresh, task status, and loading, empty, offline, and API error states.
- Task details with store information, due date, and before/after camera capture.
- Task completion is enabled after both photos have been saved.
- Room stores assigned tasks, task progress, photo file paths, and sync status.
- Completed tasks are queued locally while offline.
- WorkManager attempts to sync pending completed tasks when connectivity is available.

Photos are saved in the app's private files directory. The current sync request sends task completion details and photo-presence flags; it does not upload the photo files.

## Demo API behavior

The app currently uses a mock Retrofit interceptor; it does not connect to a production backend. Enter any valid employee ID to see five sample tasks:

- `EMPTY` returns no assigned tasks.
- `FAIL` simulates an API error when loading assigned tasks.
- Other valid IDs receive the sample task list.

The mock sync endpoint acknowledges submitted task IDs. Employee IDs must be 3–20 characters and contain only letters, numbers, hyphens, or underscores.

## Architecture

The app follows an MVVM-style structure with a manually wired dependency container:

- `ui/` — Compose screens, ViewModels, navigation, reusable UI components, and theme.
- `domain/model/` — Task and task-status models.
- `data/local/` — Room database/DAO, employee session storage, and photo file storage.
- `data/remote/` — Retrofit API, DTOs, connectivity monitor, and mock API interceptor.
- `data/repository/` — Task loading, local persistence, completion, and sync coordination.
- `data/sync/` — WorkManager worker for completed-task synchronization.
- `di/` — Application dependency container and ViewModel factories.

Main navigation: Login → Assigned Tasks → Task Details.

## Requirements

- Android Studio with a compatible Android SDK.
- JDK configured for the Gradle wrapper.
- An Android emulator or device for running the app and instrumentation tests.

The app module is configured with `minSdk 26`, `compileSdk 37`, and `targetSdk 37`.

## Build and test

From Windows PowerShell, in the project root:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

The connected instrumentation test task requires a running emulator or connected Android device.

## Current limitations

- Login is a demo identity flow, not server authentication.
- API responses are mocked; configure a real backend before production use.
- Photo files are stored locally and are not uploaded by the current sync implementation.
- Room currently uses destructive fallback migration; a future schema change without a migration can erase local database data.
- The repository currently has only template unit tests plus focused instrumentation coverage for sync acknowledgments.
