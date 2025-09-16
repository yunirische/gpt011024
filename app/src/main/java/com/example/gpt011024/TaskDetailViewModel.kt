package com.example.gpt011024

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Класс для описания состояния UI
data class TaskDetailUiState(
    val task: Task? = null,
    val photos: List<Photo> = emptyList(),
    val reportText: String = "",
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val initialPhotos: List<Photo>? = null // Для отслеживания изменений
)

// Класс для "одноразовых" событий (показать Toast, перейти назад)
sealed class UiEffect {
    data class ShowToast(val message: String) : UiEffect()
    object NavigateBack : UiEffect()
}

class TaskDetailViewModel(
    private val taskDao: TaskDao,
    private val photoDao: PhotoDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: Int = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<UiEffect>()
    val effect = _effect.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getById(taskId)
            val photos = photoDao.getPhotosForTask(taskId).first()
            _uiState.update {
                it.copy(
                    task = task,
                    photos = photos,
                    reportText = task?.report ?: "",
                    initialPhotos = photos
                )
            }
            // Начинаем отслеживать изменения после начальной загрузки
            observeChanges()
        }
    }

    private fun observeChanges() {
        viewModelScope.launch {
            // Комбинируем поток фотографий и текущее состояние UI
            combine(photoDao.getPhotosForTask(taskId), _uiState) { photos, state ->
                val photosChanged = photos.map { it.uri.toString() }.sorted() != state.initialPhotos?.map { it.uri.toString() }?.sorted()
                val reportChanged = state.reportText != (state.task?.report ?: "")

                _uiState.update {
                    it.copy(
                        photos = photos,
                        hasUnsavedChanges = photosChanged || reportChanged
                    )
                }
            }.collect { /* Просто собираем поток, чтобы он работал */ }
        }
    }

    fun onReportTextChanged(text: String) {
        _uiState.update { it.copy(reportText = text) }
    }

    fun addPhoto(uri: Uri, description: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Получаем постоянное разрешение на доступ к файлу
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                if (_uiState.value.photos.any { it.uri == uri }) {
                    _effect.emit(UiEffect.ShowToast("Это фото уже добавлено"))
                    return@launch
                }

                val maxOrder = _uiState.value.photos.maxOfOrNull { it.order } ?: 0
                val newPhoto = Photo(taskId = taskId, uri = uri, description = description, order = maxOrder + 1)
                photoDao.insertPhoto(newPhoto)
                _effect.emit(UiEffect.ShowToast("Фото добавлено"))
            } catch (e: Exception) {
                Log.e("ViewModel", "Error adding photo: ${e.message}", e)
                _effect.emit(UiEffect.ShowToast("Ошибка при добавлении фото"))
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch(Dispatchers.IO) {
            photoDao.deletePhoto(photo)
            _effect.emit(UiEffect.ShowToast("Фото удалено"))
        }
    }

    fun saveTask() {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentState = _uiState.value
                currentState.task?.let {
                    val updatedTask = it.copy(report = currentState.reportText)
                    taskDao.update(updatedTask)
                    _effect.emit(UiEffect.ShowToast("Задача сохранена"))
                    _effect.emit(UiEffect.NavigateBack) // Сигнал для навигации назад
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error saving task: ${e.message}", e)
                _effect.emit(UiEffect.ShowToast("Ошибка сохранения задачи"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
