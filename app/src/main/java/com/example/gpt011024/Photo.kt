package com.example.gpt011024

import android.net.Uri
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(
        entity = Task::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["taskId"])]
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val uri: Uri,
    val description: String? = null,
    var order: Int
)
