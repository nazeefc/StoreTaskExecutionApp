package com.example.storetaskexecutionapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.storetaskexecutionapp.StoreTaskApplication
import com.example.storetaskexecutionapp.ui.login.LoginRoute
import com.example.storetaskexecutionapp.ui.login.LoginViewModel
import com.example.storetaskexecutionapp.ui.taskdetail.TaskDetailRoute
import com.example.storetaskexecutionapp.ui.taskdetail.TaskDetailViewModel
import com.example.storetaskexecutionapp.ui.tasklist.TaskListRoute
import com.example.storetaskexecutionapp.ui.tasklist.TaskListViewModel

object Routes {
    const val LOGIN = "login"
    const val TASKS = "tasks"
    const val TASK_DETAIL = "task/{taskId}"

    fun taskDetail(taskId: String) = "task/$taskId"
}

@Composable
fun StoreTaskNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as StoreTaskApplication).container
    }
    val startDestination = if (container.sessionStore.currentEmployeeId() == null) {
        Routes.LOGIN
    } else {
        Routes.TASKS
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = viewModel(factory = container.loginFactory())
            LoginRoute(
                viewModel = viewModel,
                onLoggedIn = {
                    navController.navigate(Routes.TASKS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.TASKS) {
            val viewModel: TaskListViewModel = viewModel(factory = container.taskListFactory())
            TaskListRoute(
                viewModel = viewModel,
                onTaskClick = { taskId -> navController.navigate(Routes.taskDetail(taskId)) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.TASKS) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId").orEmpty()
            val viewModel: TaskDetailViewModel = viewModel(
                key = taskId,
                factory = container.taskDetailFactory(taskId)
            )
            TaskDetailRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
