package com.example.gpt011024

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TaskDetailScreen(
    navController: NavController,
    viewModel: TaskDetailViewModel = viewModel(
        factory = TaskDetailViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val task = uiState.task ?: return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showPhotoViewer by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UiEffect.ShowToast -> showToast(context, effect.message)
                UiEffect.NavigateBack -> navController.popBackStack()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraUri != null) {
            viewModel.addPhoto(cameraUri!!, "Снято с камеры", context)
            cameraUri = null
        } else {
            showToast(context, "Фото не сохранено или отменено")
            cameraUri = null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addPhoto(uri, "Добавлено из галереи", context)
        } else {
            showToast(context, "Фото не выбрано")
        }
    }

    val requestGalleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            showToast(context, "Разрешение на доступ к галерее не предоставлено")
        }
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            coroutineScope.launch {
                val uri = createImageFileUri(context)
                if (uri != null) {
                    cameraUri = uri
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
                title = { Text("Детали Задачи") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasUnsavedChanges) {
                            showExitDialog = true
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
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
                            "Информация о задаче",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ID: ${task.id}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Дата: ${formatUnixTime(task.date)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Описание: ${task.description}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Адрес: ${task.address}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.reportText,
                    onValueChange = { viewModel.onReportTextChanged(it) },
                    label = { Text("Отчет") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Фотографии",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (uiState.photos.isNotEmpty()) {
                    PhotoGrid(
                        photos = uiState.photos,
                        onPhotoClick = { index ->
                            if (index in uiState.photos.indices) {
                                selectedPhotoIndex = index
                                showPhotoViewer = true
                            } else {
                                Log.w("TaskDetailScreen", "Попытка открыть фото с неверным индексом: $index")
                            }
                        }
                    )
                } else {
                    Text(
                        "Нет доступных фотографий.",
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

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
                                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Manifest.permission.READ_MEDIA_IMAGES
                                } else {
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                }
                                requestGalleryPermissionLauncher.launch(permission)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Добавить фото")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Сделать фото")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                if (uiState.hasUnsavedChanges) {
                                    showExitDialog = true
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.saveTask() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSaving && uiState.hasUnsavedChanges
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(24.dp).width(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Сохранить")
                            }
                        }
                    }
                }
            }
        }
    )

    if (showPhotoViewer) {
        PhotoViewer(
            photos = uiState.photos,
            initialIndex = selectedPhotoIndex,
            onPhotoDelete = { photoToDelete -> viewModel.deletePhoto(photoToDelete) },
            onClose = { showPhotoViewer = false },
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Несохраненные изменения") },
            text = { Text("У вас есть несохраненные изменения. Выйти без сохранения?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.popBackStack()
                }) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun createImageFileUri(context: Context): Uri? {
    return try {
        val timestamp = System.currentTimeMillis()
        val fileName = "photo_$timestamp.jpg"
        val imagePath = File(context.filesDir, "images")
        if (!imagePath.exists()) {
            imagePath.mkdirs()
        }
        val file = File(imagePath, fileName)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        Log.e("CreateImageFileUri", "Ошибка создания URI для файла: ${e.message}", e)
        null
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
            .heightIn(max = 350.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.uri.toString() }) { index, photo ->
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onPhotoClick(index) },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.uri)
                        .error(R.drawable.ic_launcher_background)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .crossfade(true)
                        .diskCacheKey(photo.uri.toString())
                        .memoryCacheKey(photo.uri.toString())
                        .build(),
                    contentDescription = photo.description ?: "Фото ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}