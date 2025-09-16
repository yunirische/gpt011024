package com.example.gpt011024

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewer(
    photos: List<Photo>,
    onPhotoDelete: ((Photo) -> Unit)? = null, // Make delete optional
    onClose: () -> Unit,
    initialIndex: Int = 0
) {
    val validInitialIndex = initialIndex.coerceIn(0, maxOf(0, photos.size - 1))
    val pagerState = rememberPagerState(initialPage = validInitialIndex) { photos.size }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<Photo?>(null) }

    LaunchedEffect(photos) {
        if (photos.isEmpty()) {
            onClose()
        } else if (pagerState.currentPage >= photos.size) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(photos.size - 1)
            }
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.8f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (photos.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { index -> photos.getOrNull(index)?.uri?.toString() ?: index }
                    ) { page ->
                        val photo = photos.getOrNull(page)
                        if (photo != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 80.dp), // Оставляем место для кнопок
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
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

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

                // Show delete button only if onPhotoDelete is provided
                if (onPhotoDelete != null && photos.isNotEmpty()) {
                    Button(
                        onClick = {
                            val currentPage = pagerState.currentPage
                            if (currentPage in photos.indices) {
                                photoToDelete = photos[currentPage]
                                showDeleteDialog = true
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Text("Удалить фото")
                    }
                }
            }
        }
    }

    if (showDeleteDialog && onPhotoDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                photoToDelete = null
            },
            title = { Text("Удалить фото") },
            text = { Text("Вы уверены, что хотите удалить это фото?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        photoToDelete?.let { photo ->
                            onPhotoDelete(photo)
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
                    Text("Отмена")
                }
            }
        )
    }
}
