package com.example.gpt011024

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gpt011024.ui.theme.Gpt011024Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val database = DatabaseProvider.getDatabase(applicationContext)
            val taskDao = database.taskDao()

            val tasksWithPhotos by taskDao.getTasksWithPhotos().collectAsState(initial = emptyList())

            LaunchedEffect(Unit) {
                coroutineScope.launch(Dispatchers.IO) {
                    if (taskDao.getTasksWithPhotos().first().isEmpty()) {
                        val tasksFromJson = loadTaskFromJson(applicationContext)
                        taskDao.insertAll(tasksFromJson)
                    }
                }
            }

            val navController = rememberNavController()

            Gpt011024Theme {
                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") {
                        LoginScreen(navController = navController)
                    }

                    composable("task_list") {
                        TaskListScreen(
                            tasksWithPhotos = tasksWithPhotos,
                            onTaskClick = { taskId ->
                                navController.navigate("task_detail/$taskId")
                            },
                            onTaskDelete = { taskId ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    taskDao.getById(taskId)?.let { taskToDelete ->
                                        taskDao.delete(taskToDelete)
                                    }
                                }
                            }
                        )
                    }

                    composable(
                        route = "task_detail/{taskId}",
                        arguments = listOf(navArgument("taskId") { type = NavType.IntType })
                    ) {
                        TaskDetailScreen(navController = navController)
                    }
                }
            }
        }
    }
}
