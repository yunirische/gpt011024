package com.example.gpt011024

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator // Добавлено для индикатора загрузки
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest


@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.pager.ExperimentalFoundationApi::class) // Добавлено ExperimentalFoundationApi
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TaskDetailScreen(
    task: Task,
    navController: NavController,
    context: Context,
    onSaveClick: () -> Unit
) {
    var reportText by remember { mutableStateOf(task.report ?: "") }
    val database = DatabaseProvider.getDatabase(context)
    val photoDao = database.photoDao()
    val photos = remember { mutableStateListOf<Photo>() } // Инициализация пустым списком
    var showPhotoViewer by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }

    var showExitDialog by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) } // Состояние для индикатора загрузки
    var isLoadingPhotos by remember { mutableStateOf(true) } // Состояние для начальной загрузки фото

    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val coroutineScope = rememberCoroutineScope() // Используем rememberCoroutineScope

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val currentCameraUri = cameraUri.value
        if (success && currentCameraUri != null) {
            // addPhotoToGallery(context, currentCameraUri) // Сохранение в общую галерею, можно раскомментировать если нужно
            val newPhoto = Photo(
                taskId = task.id,
                uri = currentCameraUri, // Используем currentCameraUri
                description = "Снято с камеры",
                order = (photos.maxOfOrNull { it.order } ?: 0) + 1
            )
            coroutineScope.launch(Dispatchers.IO) { // Используем coroutineScope
                try {
                    photoDao.insertPhoto(newPhoto)
                    // Обновляем список в основном потоке
                    withContext(Dispatchers.Main) {
                        photos.add(newPhoto)
                        showToast(context, "Фото добавлено")
                    }
                } catch (e: Exception) {
                    Log.e("TaskDetailScreen", "Ошибка при сохранении фото с камеры: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        showToast(context, "Ошибка при сохранении фото")
                    }
                }
            }
            cameraUri.value = null // Сбрасываем URI после использования
        } else {
            showToast(context, "Фото не сохранено или отменено")
            cameraUri.value = null // Сбрасываем URI
        }
    }

    LaunchedEffect(task.id) { // Перезагружаем фото при смене task.id
        isLoadingPhotos = true
        withContext(Dispatchers.IO) {
            try {
                val loadedPhotos = photoDao.getPhotosForTask(task.id).sortedBy { it.order }
                val accessiblePhotos = mutableListOf<Photo>()
                loadedPhotos.forEach { photo ->
                    try {
                        // Проверяем доступность URI перед запросом постоянного разрешения
                        context.contentResolver.openInputStream(photo.uri)?.close() // Простая проверка доступа
                        // Запрашиваем постоянное разрешение (лучше делать при выборе/съемке)
                         context.contentResolver.takePersistableUriPermission(
                             photo.uri,
                             Intent.FLAG_GRANT_READ_URI_PERMISSION
                         )
                        accessiblePhotos.add(photo)
                    } catch (e: SecurityException) {
                         Log.w("TaskDetailScreen", "Нет постоянного разрешения для URI: ${photo.uri}. Фото не будет отображено.", e)
                         // Можно удалить фото из БД, если оно недоступно
                         // photoDao.deletePhoto(photo)
                    } catch (e: Exception) {
                        Log.e("TaskDetailScreen", "Ошибка доступа к URI: ${photo.uri}", e)
                         // Фото недоступно, пропускаем
                    }
                }
                 withContext(Dispatchers.Main) {
                    photos.clear()
                    photos.addAll(accessiblePhotos) // Добавляем только доступные фото
                    task.photos = accessiblePhotos // Обновляем фото в объекте task
                    isLoadingPhotos = false
                }
            } catch (e: Exception) {
                 Log.e("TaskDetailScreen", "Ошибка загрузки фото из БД: ${e.message}", e)
                 withContext(Dispatchers.Main) {
                    showToast(context, "Ошибка загрузки фотографий")
                    isLoadingPhotos = false
                 }
            }
        }
    }


    // Отслеживание изменений
    LaunchedEffect(reportText, photos.toList()) { // Преобразуем photos в List для сравнения
        val initialPhotosSorted = task.photos.sortedBy { it.uri.toString() }
        val currentPhotosSorted = photos.toList().sortedBy { it.uri.toString() }
        val photosEqual = initialPhotosSorted == currentPhotosSorted
        hasUnsavedChanges = reportText != (task.report ?: "") || !photosEqual
        Log.d("TaskDetailScreen", "Has Unsaved Changes: $hasUnsavedChanges (reportChanged: ${reportText != (task.report ?: "")}, photosChanged: ${!photosEqual})")
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Получаем постоянное разрешение на чтение
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                val maxOrder = photos.maxOfOrNull { it.order } ?: 0
                val newPhoto = Photo(
                    taskId = task.id,
                    uri = uri,
                    description = "Добавлено из галереи",
                    order = maxOrder + 1
                )
                coroutineScope.launch(Dispatchers.IO) { // Используем coroutineScope
                    try {
                        photoDao.insertPhoto(newPhoto)
                        withContext(Dispatchers.Main) {
                           if (photos.none { it.uri == newPhoto.uri }) { // Проверка на дубликат URI
                                photos.add(newPhoto) // Добавляем в конец списка UI
                                showToast(context, "Фото добавлено")
                            } else {
                                showToast(context, "Это фото уже добавлено")
                           }
                        }
                    } catch (e: Exception) {
                         Log.e("TaskDetailScreen", "Ошибка при сохранении фото из галереи в БД: ${e.message}", e)
                         withContext(Dispatchers.Main) {
                            showToast(context, "Ошибка при сохранении фото")
                         }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("TaskDetailScreen", "Ошибка прав доступа к фото из галереи: ${e.message}", e)
                showToast(context, "Ошибка: Не удалось получить доступ к фото")
            } catch (e: Exception) {
                 Log.e("TaskDetailScreen", "Неизвестная ошибка при добавлении фото из галереи: ${e.message}", e)
                 showToast(context, "Произошла ошибка при добавлении фото")
            }
        } else {
            showToast(context, "Фото не выбрано")
        }
    }

    // Запрос разрешений (для галереи)
    val requestGalleryPermissionLauncher = rememberLauncherForActivityResult(
         ActivityResultContracts.RequestPermission()
     ) { isGranted: Boolean ->
         if (isGranted) {
             galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
         } else {
             showToast(context, "Разрешение на доступ к галерее не предоставлено")
         }
     }

     // Запрос разрешений (для камеры)
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Разрешение получено, создаем URI и запускаем камеру
            coroutineScope.launch { // Используем coroutineScope
                 val uri = createImageFileUri(context)
                 if (uri != null) {
                     cameraUri.value = uri // Устанавливаем URI перед запуском камеры
                     cameraLauncher.launch(uri)
                 } else {
                     showToast(context, "Не удалось создать файл для фото")
                 }
             }
        } else {
            showToast(context, "Разрешение на использование камеры не предоставлено")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали Задачи") }, // Перевод
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            showExitDialog = true
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад" // Перевод
                        )
                    }
                })
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Информация о задаче", // Перевод
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ID: ${task.id}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Дата: ${formatUnixTime(task.date)}", // Перевод
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Описание: ${task.description}", // Перевод
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Адрес: ${task.address}", // Перевод
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = reportText,
                    onValueChange = { reportText = it },
                    label = { Text("Отчет") }, // Перевод
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp) // Можно увеличить высоту
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Фотографии", // Перевод
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (isLoadingPhotos) {
                     Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator()
                     }
                 } else if (photos.isNotEmpty()) {
                    PhotoGrid(
                        photos = photos.toList(), // Передаем неизменяемую копию
                        onPhotoClick = { index ->
                             if (index in photos.indices) { // Добавлена проверка индекса
                                selectedPhotoIndex = index
                                showPhotoViewer = true
                             } else {
                                 Log.w("TaskDetailScreen", "Попытка открыть фото с неверным индексом: $index")
                             }
                        }
                    )
                } else {
                    Text(
                        "Нет доступных фотографий.", // Перевод
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f)) // Занимает оставшееся место, чтобы кнопки были внизу

                // Кнопки действий
                 Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                // Определяем нужное разрешение в зависимости от версии Android
                                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Manifest.permission.READ_MEDIA_IMAGES
                                } else {
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                }
                                requestGalleryPermissionLauncher.launch(permission)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Добавить фото") // Перевод
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                // Запрашиваем разрешение на камеру
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Сделать фото") // Перевод
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                         Button(
                             onClick = {
                                 if (hasUnsavedChanges) {
                                     showExitDialog = true
                                 } else {
                                     navController.popBackStack()
                                 }
                             },
                             modifier = Modifier.weight(1f)
                         ) {
                             Text("Отмена") // Перевод
                         }

                        Spacer(modifier = Modifier.width(8.dp))

                         Button(
                             onClick = {
                                 isSaving = true // Показываем индикатор
                                 val finalReport = reportText // Фиксируем значение отчета
                                 val finalPhotos = photos.toList() // Фиксируем список фото

                                 coroutineScope.launch(Dispatchers.IO) { // Используем coroutineScope
                                     try {
                                         val taskDao = database.taskDao()
                                         val updatedTask = task.copy(report = finalReport, photos = finalPhotos) // Обновляем копию
                                         taskDao.update(updatedTask) // Сохраняем обновленную задачу

                                         withContext(Dispatchers.Main) {
                                             isSaving = false // Скрываем индикатор
                                             onSaveClick() // Вызываем колбэк сохранения
                                             navController.popBackStack() // Возвращаемся назад
                                             showToast(context, "Задача сохранена")
                                         }
                                     } catch (e: Exception) {
                                         Log.e("TaskDetailScreen", "Ошибка сохранения задачи: ${e.message}", e)
                                         withContext(Dispatchers.Main) {
                                             isSaving = false // Скрываем индикатор
                                             showToast(context, "Ошибка сохранения задачи")
                                         }
                                     }
                                 }
                             },
                             modifier = Modifier.weight(1f),
                             enabled = !isSaving && hasUnsavedChanges // Кнопка активна только если нет сохранения и есть изменения
                         ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(24.dp).width(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp // Делаем индикатор тоньше
                                )
                             } else {
                                Text("Сохранить") // Перевод
                             }
                         }
                     }
                }
            }
        }
    )


    // Диалоги
    if (showPhotoViewer) {
        PhotoViewer(
            photos = photos.toList(), // Передаем неизменяемую копию списка
            initialIndex = selectedPhotoIndex,
            onPhotoDelete = { photoToDelete ->
                // Удаляем из базы данных в фоновом потоке
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        photoDao.deletePhoto(photoToDelete)
                        // Обновляем UI в главном потоке
                        withContext(Dispatchers.Main) {
                           val indexToRemove = photos.indexOfFirst { it.id == photoToDelete.id }
                           if (indexToRemove != -1) {
                               photos.removeAt(indexToRemove)
                           }
                           showToast(context, "Фото удалено")
                            // Закрываем просмотрщик, если фото больше нет
                           if (photos.isEmpty()) {
                               showPhotoViewer = false
                           }
                        }
                    } catch (e: Exception) {
                         Log.e("TaskDetailScreen", "Ошибка удаления фото из БД: ${e.message}", e)
                         withContext(Dispatchers.Main) {
                            showToast(context, "Ошибка удаления фото")
                         }
                    }
                }
            },
            onClose = { showPhotoViewer = false },
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Несохраненные изменения") }, // Перевод
            text = { Text("У вас есть несохраненные изменения. Выйти без сохранения?") }, // Перевод
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.popBackStack() // Выход без сохранения
                }) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error) // Перевод
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { // Просто закрыть диалог
                    Text("Отмена") // Перевод
                }
            }
        )
    }
}

// Вспомогательная функция для отображения Toast
private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

// Функция создания URI для файла изображения (в приватном хранилище приложения)
fun createImageFileUri(context: Context): Uri? {
    return try {
        val timestamp = System.currentTimeMillis()
        val fileName = "photo_$timestamp.jpg"
        // Используем приватную директорию для изображений
        val imagePath = File(context.filesDir, "images")
        if (!imagePath.exists()) {
            imagePath.mkdirs()
        }
        val file = File(imagePath, fileName)
        // Используем FileProvider для получения content:// URI
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // Authority должен совпадать с AndroidManifest.xml
            file
        )
    } catch (e: Exception) {
        Log.e("CreateImageFileUri", "Ошибка создания URI для файла: ${e.message}", e)
        null
    }
}

// Функция добавления фото в общую галерею (может потребоваться разрешение WRITE_EXTERNAL_STORAGE для старых версий)
fun addPhotoToGallery(context: Context, photoUri: Uri) {
    val resolver = context.contentResolver
    val fileName = "photo_${System.currentTimeMillis()}.jpg" // Генерируем имя файла

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1) // Файл ожидает записи
        }
    }

    var galleryUri: Uri? = null
    try {
        galleryUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (galleryUri == null) {
            throw Exception("Не удалось создать запись в MediaStore")
        }

        // Копируем данные из временного файла (photoUri) в файл галереи (galleryUri)
        resolver.openOutputStream(galleryUri)?.use { outputStream ->
            resolver.openInputStream(photoUri)?.use { inputStream ->
                inputStream.copyTo(outputStream)
            } ?: throw Exception("Не удалось открыть InputStream для исходного URI: $photoUri")
        } ?: throw Exception("Не удалось открыть OutputStream для URI галереи: $galleryUri")

        // Если Android Q или выше, снимаем флаг IS_PENDING
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(galleryUri, contentValues, null, null)
        }
         Log.d("AddPhotoGallery", "Фото успешно добавлено в галерею: $galleryUri")

    } catch (e: Exception) {
        Log.e("AddPhotoGallery", "Ошибка добавления фото в галерею: ${e.message}", e)
        // Если произошла ошибка и URI был создан, удаляем запись из MediaStore
        galleryUri?.let { resolver.delete(it, null, null) }
        // Показываем ошибку пользователю (опционально)
        // showToast(context, "Не удалось сохранить фото в галерею")
    }
}


@Composable
fun PhotoGrid(
    photos: List<Photo>,
    modifier: Modifier = Modifier,
    onPhotoClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp), // Ограничиваем высоту
        contentPadding = PaddingValues(4.dp), // Уменьшаем отступы
        horizontalArrangement = Arrangement.spacedBy(4.dp), // Пространство между колонками
        verticalArrangement = Arrangement.spacedBy(4.dp) // Пространство между рядами
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.uri.toString() }) { index, photo ->
            Card(
                modifier = Modifier
                    //.padding(4.dp) // Убираем, т.к. есть spacedBy
                    .aspectRatio(1f) // Квадратные превью
                    .clickable { onPhotoClick(index) },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                 AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.uri)
                        .error(R.drawable.ic_launcher_background) // Заглушка при ошибке
                        .placeholder(R.drawable.ic_launcher_foreground) // Заглушка при загрузке
                        .crossfade(true)
                        .diskCacheKey(photo.uri.toString()) // Ключ для кэширования
                        .memoryCacheKey(photo.uri.toString()) // Ключ для кэширования в памяти
                        .build(),
                    contentDescription = photo.description ?: "Фото ${index + 1}", // Описание или номер
                    contentScale = ContentScale.Crop, // Обрезаем для заполнения
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


@OptIn(androidx.compose.foundation.pager.ExperimentalFoundationApi::class)
@Composable
fun PhotoViewer(
    photos: List<Photo>,
    onPhotoDelete: (Photo) -> Unit,
    onClose: () -> Unit,
    initialIndex: Int = 0
) {
    val photosState = remember { mutableStateListOf<Photo>() }
    LaunchedEffect(photos) {
        photosState.clear()
        photosState.addAll(photos)
    }

    val validInitialIndex = initialIndex.coerceIn(0, maxOf(0, photosState.size - 1)) // Безопасный индекс

    val pagerState = rememberPagerState(
        initialPage = validInitialIndex,
        pageCount = { photosState.size }
    )

    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<Photo?>(null) }

    LaunchedEffect(photosState.size) {
        // Закрываем просмотрщик, если все фото удалены
        if (photosState.isEmpty()) {
            onClose()
        }
        // Корректируем текущую страницу, если она стала невалидной после удаления
        else if (pagerState.currentPage >= photosState.size) {
             coroutineScope.launch {
                 pagerState.animateScrollToPage(photosState.size - 1)
             }
        }
    }

    Dialog(onDismissRequest = onClose) { // Диалог для просмотра
        Surface(
            modifier = Modifier.fillMaxSize(), // Занимает весь экран
            color = Color.Black.copy(alpha = 0.9f) // Полупрозрачный фон
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (photosState.isNotEmpty()) { // Показываем Pager только если есть фото
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { index -> photosState.getOrNull(index)?.uri?.toString() ?: index }
                    ) { page ->
                        val photo = photosState.getOrNull(page)
                        if (photo != null) {
                             Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 80.dp), // Оставляем место для кнопки снизу
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photo.uri)
                                        .error(R.drawable.ic_launcher_background)
                                        .placeholder(R.drawable.ic_launcher_foreground)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Фото ${page + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f), // Сохраняем пропорции
                                    contentScale = ContentScale.Fit // Вписываем изображение
                                )
                            }
                        }
                    }
                }

                // Кнопка закрытия в правом верхнем углу
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }

                // Кнопка удаления внизу по центру (только если есть фото)
                if (photosState.isNotEmpty()) {
                    Button(
                        onClick = {
                            val currentPage = pagerState.currentPage
                            if (currentPage in photosState.indices) {
                                photoToDelete = photosState[currentPage]
                                showDeleteDialog = true
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp) // Отступ снизу
                    ) {
                        Text("Удалить фото")
                    }
                }
            }
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                photoToDelete = null // Сбрасываем
            },
            title = { Text("Удалить фото") },
            text = { Text("Вы уверены, что хотите удалить это фото?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        photoToDelete?.let { photo ->
                            onPhotoDelete(photo) // Вызываем колбэк удаления
                        }
                        showDeleteDialog = false
                        photoToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    photoToDelete = null
                }) {
                    Text("Отмена", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

// Функция форматирования времени (если она еще не определена где-то)
// Необходимо добавить ее или импортировать, если она есть
@SuppressLint("SimpleDateFormat")
fun formatUnixTime(unixTime: Long): String {
     if (unixTime == 0L) return "N/A" // Обработка нулевого времени
     try {
         val date = java.util.Date(unixTime * 1000) // Умножаем на 1000 для миллисекунд
         val format = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm") // Формат даты и времени
         return format.format(date)
     } catch (e: Exception) {
         Log.e("FormatTime", "Ошибка форматирования времени: $unixTime", e)
         return "Invalid Date"
     }
}


// Расширение для перемещения элементов в MutableList (если нужно)
fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex in indices && toIndex in indices && fromIndex != toIndex) {
        val element = removeAt(fromIndex)
        add(toIndex, element)
    }
}

// Убедитесь, что у вас определены ресурсы R.drawable.ic_launcher_background и R.drawable.ic_launcher_foreground
// или замените их на свои ресурсы для заглушек изображений.
// Также убедитесь, что authority в FileProvider (`${context.packageName}.fileprovider`)
// совпадает с тем, что указано в AndroidManifest.xml в <provider> теге.
