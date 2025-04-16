package com.example.gpt011024

import android.annotation.SuppressLint
import android.provider.CalendarContract.Colors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    tasks: List<Task>,
    onTaskClick: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit // Новый параметр для удаления задачи
) {
    if (tasks.isEmpty()) {
        Text(text = "No tasks available", modifier = Modifier.fillMaxSize())
    } else {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Text(
                        "Task List",
                        style = MaterialTheme.typography.headlineSmall
                    )
                })
            },
            content = { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    items(tasks) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onActionClick = { taskId -> onTaskDelete(taskId) } // Передаем функцию удаления
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onActionClick: (Int) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Notifications,
                    contentDescription = if (task.isCompleted) "Completed" else "Pending",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = "ID: ${task.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Address: ${task.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val formattedDate = formatUnixTime(task.date)
                    Text(
                        text = "Date: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(onClick = { showDialog.value = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = "ID: ${task.id}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    if (showDialog.value) {
        ConfirmDeleteDialog(
            onConfirm = {
                showDialog.value = false
                onActionClick(task.id)
            },
            onDismiss = {
                showDialog.value = false
            }
        )
    }
}
@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Delete Task")
        },
        text = {
            Text("Are you sure you want to delete this task? This action cannot be undone.")
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            androidx.compose.material3.Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



fun formatUnixTime(unixTime: String): String {
    return try {
        val timestamp = unixTime.toLong() * 1000 // Преобразование в миллисекунды
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        dateFormat.format(Date(timestamp))
    } catch (e: Exception) {
        "Invalid date"
    }
}
