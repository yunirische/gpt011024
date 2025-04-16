package com.example.gpt011024

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gpt011024.ui.theme.Gpt011024Theme
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val database = DatabaseProvider.getDatabase(applicationContext)
            val taskDao = database.taskDao()

            val taskList = remember { mutableStateOf(listOf<Task>()) }

            LaunchedEffect(Unit) {
                GlobalScope.launch(Dispatchers.IO) {
                    val tasks = taskDao.getAllTasks()
                    Log.d("MainActivity", "Tasks in DB: $tasks")

                    if (tasks.isEmpty()) {
                        val tasksFromJson = loadTaskFromJson(applicationContext)

                        Log.d("MainActivity", "Loaded from JSON: $tasksFromJson")
                        taskDao.insertAll(tasksFromJson)
                        taskList.value = taskDao.getAllTasks()
                    } else {
                        taskList.value = tasks
                    }
                }
            }


            val navController = rememberNavController()

            Gpt011024Theme {
                NavHost(
                    navController = navController,
                    startDestination = "task_list"
                ) {
                    composable("task_list") {
                        TaskListScreen(
                            tasks = taskList.value,
                            onTaskClick = { taskId ->
                                navController.navigate("task_detail/$taskId")
                            },
                            onTaskDelete = { taskId ->
                                GlobalScope.launch(Dispatchers.IO) {
                                    taskDao.delete(taskDao.getById(taskId)!!)
                                    taskList.value = taskDao.getAllTasks()
                                }
                            }
                        )
                    }

                    composable("task_detail/{taskId}") { navBackStackEntry ->
                        val taskId =
                            navBackStackEntry.arguments?.getString("taskId")?.toIntOrNull()
                        val task = taskList.value.find { it.id == taskId }

                        if (task != null) {
                            TaskDetailScreen(
                                task = task,
                                navController = navController,
                                context = applicationContext,
                                onSaveClick = {/* todo: implement save logic */ }
                            )
                        } else {
                            Text("Task not found")


                        }

                    }
                }

            }
        }
    }
}
