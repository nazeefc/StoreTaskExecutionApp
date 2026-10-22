# AI Usage Documentation

**Project:** Store Task Execution App  
**Platform:** Android  
**Technology:** Kotlin, Jetpack Compose

## 1. Development Approach

My primary development experience is in web applications, particularly JavaScript, React, and Next.js. For this native Android assessment, I used AI coding tools extensively to help build and understand the Kotlin and Jetpack Compose application. I provided the assessment requirements, worked with the generated project, configured the development environment, and launched the application in an Android emulator.

AI generated a substantial portion of the Android implementation. This document describes that contribution openly rather than presenting the code as entirely handwritten.

## 2. AI Tools Used

- **Cursor AI:** Initial AI-assisted creation of the Android project and application code.
- **GitHub Copilot in VS Code:** Analysis of the existing project and continuation of development after Cursor's usage limit was reached.
- **ChatGPT:** Assistance with Android Studio and emulator setup, development workflow, troubleshooting, and planning how to continue an existing Kotlin project in VS Code.

## 3. Five Important AI Prompts

The following five prompts document the key instructions used to guide AI-assisted development. They are **representative reconstructions**, not a verbatim export of Cursor or Copilot chat history.

### Prompt 1 — Initial Android Application

> Build a Store Task Execution Android application using Kotlin and Jetpack Compose. Employees should enter an Employee ID, see their assigned tasks, open task details, capture Before and After photos, and mark work as completed. Use a clean, understandable project structure.

### Prompt 2 — API Integration and UI States

> Fetch employee-assigned tasks from a mock REST API using Retrofit. Display tasks and details in Jetpack Compose. Handle loading, empty, offline, and error states with clear messages and retry actions. Keep networking separate from UI components.

### Prompt 3 — Offline Storage and Synchronization

> Support saving unfinished task work locally using Room. When a completed task cannot be sent because the device is offline, keep it pending and use WorkManager to retry synchronization when connectivity returns. Avoid duplicate submissions and preserve progress after app restarts.

### Prompt 4 — Review the Partially Completed Project

> Analyze my existing StoreTaskExecutionApp Kotlin and Jetpack Compose project. Identify which screens and features are complete, incomplete, or missing. Review navigation, ViewModels, repositories, dependencies, and placeholders. Do not modify working files until I approve the plan.

### Prompt 5 — Continue Development in VS Code with Copilot

> Continue the existing Android application after Cursor AI reached its usage limit. Preserve the current project, package names, architecture, and working functionality. Implement missing requirements one feature at a time, explain changes, and verify the build and emulator behavior before proceeding.

## 4. AI-Generated Implementation

AI assistance was used for the Android-specific implementation. One reviewed source file in `com.example.storetaskexecutionapp.ui.components` contains reusable Jetpack Compose UI elements:

- `LoadingState`: progress indicator and loading message.
- `EmptyState`: empty-state message with an optional action.
- `OfflineState`: offline message with an optional retry action.
- `ErrorState`: error message and retry action.
- `StatusState`: shared layout used by the empty, offline, and error states.
- `OfflineBanner`: offline-operation message and the number of completed tasks awaiting synchronization.
- `TextRetry`: reusable retry button.

These components establish how the interface can *display* application states. This single source file does **not** establish whether API integration, Room persistence, photo capture, or WorkManager synchronization are complete or functional; those must be verified in the rest of the project.

## 5. Issues Encountered and Resolutions

### Issue 1 — Android Emulator Virtualization Setup

**Observed issue:** The emulator setup reported that Windows Hypervisor Platform was not enabled.  
**Action taken:** Enabled the Windows virtualization feature and restarted the computer.  
**Result:** The Android emulator subsequently started and displayed its screen.

### Issue 2 — Cursor AI Usage Limit During Development

**Observed issue:** Cursor's AI usage limit was reached while the Android application was still being developed.  
**Action taken:** Opened the same project in VS Code and switched to GitHub Copilot to inspect and continue the existing codebase rather than starting a new project.  
**Result:** The project was preserved for continued development.

> **Assessment clarification:** These are genuine workflow/environment issues, **not verified AI coding mistakes**. The assessment separately asks for at least two AI mistakes and how they were corrected. That requirement remains outstanding until two real examples are identified from development history or testing. Do not relabel the issues above as AI coding mistakes.

## 6. Testing and Verification

- Opened the existing Android project in Android Studio.
- Resolved the emulator virtualization prerequisite.
- Started an Android virtual device.
- Successfully launched the application in the emulator.

**Not yet verified by the provided evidence:** End-to-end employee login, API retrieval, photo capture, local persistence, background synchronization, and behavior under all offline/error conditions. Record their results after testing.

## 7. What I Learned

This assessment introduced me to a native Android development workflow beyond my usual web stack. I worked with Android Studio, an emulator, Kotlin/Jetpack Compose source files, and a workflow in which GitHub Copilot in VS Code was used to continue an existing project. The process highlighted the need to check generated code against requirements and verify application behavior instead of assuming that generated functionality works.