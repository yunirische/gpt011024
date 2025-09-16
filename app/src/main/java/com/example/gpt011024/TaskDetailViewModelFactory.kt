package com.example.gpt011024

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

// Этот класс отвечает за создание TaskDetailViewModel и передачу в него зависимостей
class TaskDetailViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(TaskDetailViewModel::class.java)) {
            val database = DatabaseProvider.getDatabase(context.applicationContext)
            val savedStateHandle = extras.createSavedStateHandle()

            @Suppress("UNCHECKED_CAST")
            return TaskDetailViewModel(
                taskDao = database.taskDao(),
                photoDao = database.photoDao(),
                savedStateHandle = savedStateHandle
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
