package com.example.gpt011024

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

data class TaskContainer(val tasks: List<Task>)

fun loadTaskFromJson(context: Context): List<Task> {
    val inputStream = context.assets.open("example.json")
    val reader = InputStreamReader(inputStream)

    val taskContainer = Gson().fromJson<TaskContainer>(
        reader,
        object : TypeToken<TaskContainer>() {}.type
    )
    reader.close()
    return taskContainer.tasks
}