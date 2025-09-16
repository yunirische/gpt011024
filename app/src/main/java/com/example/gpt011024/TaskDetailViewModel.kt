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
    val newlyAddedPhotos: List<Photo> = emptyList() // Временное хранилище для новых фото
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
            // 1. Загружаем задачу и инициализируем состояние один раз
            val task = taskDao.getById(taskId)
            _uiState.update {
                it.copy(
                    task = task,
                    reportText = task?.report ?: ""
                )
            }

            // 2. Начинаем наблюдать за изменениями фотографий в БД
            photoDao.getPhotosForTask(taskId).collect { photosFromDb ->
                _uiState.update { currentState ->
                    val reportChanged = currentState.reportText != (task?.report ?: "")
                    val newPhotosAdded = currentState.newlyAddedPhotos.isNotEmpty()
                    currentState.copy(
                        photos = photosFromDb + currentState.newlyAddedPhotos, // Показываем объединенный список
                        hasUnsavedChanges = reportChanged || newPhotosAdded
                    )
                }
            }
        }
    }

    fun onReportTextChanged(text: String) {
        _uiState.update {
            val reportChanged = text != (it.task?.report ?: "")
            val newPhotosAdded = it.newlyAddedPhotos.isNotEmpty()
            it.copy(reportText = text, hasUnsavedChanges = reportChanged || newPhotosAdded)
        }
    }

    fun addPhoto(uri: Uri, description: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Вызываем takePersistableUriPermission только если URI не от нашего FileProvider
                val fileProviderAuthority = "${context.packageName}.fileprovider"
                if (uri.authority != fileProviderAuthority) {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                }

                if (uiState.value.photos.any { it.uri == uri }) {
                    _effect.emit(UiEffect.ShowToast("Это фото уже добавлено"))
                    return@launch
                }

                val maxOrder = uiState.value.photos.maxOfOrNull { it.order } ?: 0
                val newPhoto = Photo(taskId = taskId, uri = uri, description = description, order = maxOrder + 1)

                // НЕ сохраняем в БД, а добавляем во временный список в состоянии
                _uiState.update {
                    val updatedNewPhotos = it.newlyAddedPhotos + newPhoto
                    it.copy(
                        newlyAddedPhotos = updatedNewPhotos,
                        photos = it.photos + newPhoto, // ОБНОВЛЯЕМ ОСНОВНОЙ СПИСОК
                        hasUnsavedChanges = true
                    )
                }

                _effect.emit(UiEffect.ShowToast("Фото добавлено"))
            } catch (e: Exception) {
                Log.e("ViewModel", "Error adding photo: ${e.message}", e)
                _effect.emit(UiEffect.ShowToast("Ошибка при добавлении фото"))
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        // Если у фото id=0, значит оно еще не сохранено в БД (новое)
        if (photo.id.toLong() == 0L) {
            _uiState.update { currentState ->
                val updatedNewPhotos = currentState.newlyAddedPhotos.filterNot { p -> p.uri == photo.uri }
                val reportChanged = currentState.reportText != (currentState.task?.report ?: "")
                currentState.copy(
                    newlyAddedPhotos = updatedNewPhotos,
                    photos = currentState.photos.filterNot { p -> p.uri == photo.uri }, // ОБНОВЛЯЕМ ОСНОВНОЙ СПИСОК
                    hasUnsavedChanges = reportChanged || updatedNewPhotos.isNotEmpty()
                )
            }
        } else { // Иначе удаляем из БД
            viewModelScope.launch(Dispatchers.IO) {
                photoDao.deletePhoto(photo)
                _effect.emit(UiEffect.ShowToast("Фото удалено"))
            }
        }
    }

    fun saveTask() {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentState = _uiState.value

                // 1. Сохраняем новые фотографии в базу данных
                if (currentState.newlyAddedPhotos.isNotEmpty()) {
                    val newIds = photoDao.insertPhotos(currentState.newlyAddedPhotos)
                    if (newIds.isNotEmpty()) {
                        // Очищаем временный список, так как фото теперь в БД
                        // и будут получены через основной Flow в init {}
                        _uiState.update { it.copy(newlyAddedPhotos = emptyList()) }
                    }
                }

                currentState.task?.let {
                    // 2. Обновляем отчет в задаче
                    val updatedTask = it.copy(report = currentState.reportText.trim())
                    taskDao.update(updatedTask)
                    _effect.emit(UiEffect.ShowToast("Задача сохранена"))
                    _effect.emit(UiEffect.NavigateBack)
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