package com.example.gpt011024

import android.content.Context
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

data class TaskContainer(val tasks: List<Task>)

fun loadTaskFromJson(context: Context): List<Task> {
    val gson = GsonProvider.gson

    return context.assets.open("example.json").use { inputStream ->
        InputStreamReader(inputStream).use { reader ->
            val taskContainer = gson.fromJson<TaskContainer>(
                reader,
                object : TypeToken<TaskContainer>() {}.type
            )
            taskContainer.tasks
        }
    }
}