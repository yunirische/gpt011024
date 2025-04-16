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


@OptIn(ExperimentalMaterial3Api::class)
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
    val photos = remember { mutableStateListOf(*task.photos.toTypedArray()) }
    var showPhotoViewer by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }

    var showExitDialog by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }


    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraUri.value != null) {
            addPhotoToGallery(context, cameraUri.value!!)
            val fileName = "photo_${System.currentTimeMillis()}.jpg"
//            savePhotoToPublicStorage(context, cameraUri.value!!, fileName) ?.let { saveduri ->

//            }
            val newPhoto = Photo(
                taskId = task.id,
                uri = cameraUri.value!!, // Извлекаем значение Uri из cameraUri
                description = "Снято с камеры",
                order = (photos.maxOfOrNull { it.order } ?: 0) + 1 // Увеличиваем order на 1
            )
            CoroutineScope(Dispatchers.IO).launch {
                photoDao.insertPhoto(newPhoto)
                photos.add(newPhoto) // Добавляем фото в список
            }
            Toast.makeText(context, "Фото добавлено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Фото не сохранено", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            photos.clear()
            val loadedPhotos = photoDao.getPhotosForTask(task.id)
            loadedPhotos.forEach{ photo ->
                context.contentResolver.takePersistableUriPermission(
                    photo.uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            photos.addAll(photoDao.getPhotosForTask(task.id))
            task.photos = photos.toList()
        }

    }

    LaunchedEffect(reportText, photos) {
        val photosEqual =
            photos.sortedBy { it.uri.toString() } == task.photos.sortedBy { it.uri.toString() }
        hasUnsavedChanges = reportText != task.report || !photosEqual
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val maxOrder = photos.maxOfOrNull { it.order } ?: 0
            val newPhoto = Photo(
                taskId = task.id,
                uri = uri,
                description = "Добавлено из галереи",
                order = maxOrder + 1 // Увеличиваем order на 1
            )
            CoroutineScope(Dispatchers.IO).launch {
                photoDao.insertPhoto(newPhoto)
                if (photos.none { it.uri == newPhoto.uri }) {
                    photos.add(newPhoto)
                }
            }
            Toast.makeText(context, "Фото добавлено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Фото не выбрано", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
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
                            contentDescription = "Back"
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
                            "Task Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ID: ${task.id}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Date: ${formatUnixTime(task.date)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Description: ${task.description}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Address: ${task.address}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = reportText,
                    onValueChange = { reportText = it },
                    label = { Text("Report") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (photos.isNotEmpty()) {
                    PhotoGrid(
                        photos = photos,
                        onPhotoClick = { index ->
                            selectedPhotoIndex = index
                            showPhotoViewer = true
                        }
                    )
                } else {
                    Text(
                        "No photos available.",
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                                } else {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add Photo")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val uri = createImageFileUri(context)
                                if (uri != null) {
                                    cameraUri.value = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Не удалось создать файл для фото",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Take Photo")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                if (reportText != task.report || photos != task.photos) {
                                    showExitDialog = true
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                task.report = reportText

                                CoroutineScope(Dispatchers.IO).launch {
                                    val taskDao = database.taskDao()
                                    taskDao.update(task)
                                }
                                onSaveClick()
                                navController.popBackStack()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    )


    if (showPhotoViewer) {
        PhotoViewer(
            photos = photos,
            initialIndex = selectedPhotoIndex,
            onPhotoDelete = { photo ->
                CoroutineScope(Dispatchers.IO).launch {
                    photoDao.deletePhoto(photo)
                    photos.clear()
                    photos.addAll(photoDao.getPhotosForTask(task.id))
                }
            },
            onClose = { showPhotoViewer = false },
        )
    }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to discard them and exit?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.popBackStack()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun savePhotoToPublicStorage(context: Context, uri: Uri, fileName: String): Uri? {
    return try {
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(picturesDir, fileName)

        val inputStream = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        outputStream.close()
        inputStream?.close()

        val savedUri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        addPhotoToGallery(context, savedUri)

        savedUri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex in 0 until size && toIndex in 0 until size && fromIndex != toIndex) {
        val item = removeAt(fromIndex)
        add(toIndex, item)
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
            .height(300.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        itemsIndexed(photos, key = { index, photo -> photo.uri.toString() }) { index, photo ->
            Log.d("PhotoGrid", "Loading photo: ${photo.uri}")
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(1f)
                    .clickable { onPhotoClick(index) },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(photo.uri)
                        .diskCacheKey(photo.uri.toString())
                        .build(),
                    contentDescription = photo.description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


@Composable
fun PhotoViewer(
    photos: List<Photo>,
    onPhotoDelete: (Photo) -> Unit,
    onClose: () -> Unit,
    initialIndex: Int = 0
) {
    val photosState = remember { mutableStateOf(photos) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { photosState.value.size }
    )

    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<Photo?>(null) }

    Dialog(onDismissRequest = { onClose() }) {
        Surface(
            color = Color.Black,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 600.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }

                    if (photosState.value.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            key = { index -> photosState.value[index].uri.toString() }
                        ) { page ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = photosState.value[page].uri,
                                    contentDescription = "Foto ${page + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
                            val currentPage = pagerState.currentPage
                            if (currentPage in photosState.value.indices) {
                                photoToDelete = photosState.value[currentPage]
                                showDeleteDialog = true

                            }
                        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Delete Photo")
                        }
                    } else {
                        Text(
                            text = "No photos available",
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Photo") },
            text = { Text("Are you sure you want to delete this photo?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        photoToDelete?.let { photo ->
                            val updatedPhotos = photosState.value.toMutableStateList().apply {
                                remove(photo)
                            }
                            photosState.value = updatedPhotos
                            onPhotoDelete(photo)

                            coroutineScope.launch {
                                if (updatedPhotos.isNotEmpty()) {
                                    val targetPage =
                                        pagerState.currentPage.coerceAtMost(updatedPhotos.size - 1)
                                    pagerState.scrollToPage(targetPage)
                                } else {
                                    onClose()
                                }
                            }
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}

fun addPhotoToGallery(context: Context, photoUri: Uri) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, File(photoUri.path!!).name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }
    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
        resolver.openOutputStream(uri)?.use { outputStream ->
            resolver.openInputStream(photoUri)?.use { inputStream ->
                inputStream.copyTo(outputStream)
            }
            outputStream.flush()
        }
    }
}

fun createImageFileUri(context: Context): Uri? {
    return try {
        val timestamp = System.currentTimeMillis()
        val fileName = "photo_$timestamp.jpg"
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (!picturesDir?.exists()!!) {
            picturesDir.mkdirs()
        }
        val file = File(picturesDir, fileName)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
