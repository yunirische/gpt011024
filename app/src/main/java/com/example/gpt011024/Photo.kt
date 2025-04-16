package com.example.gpt011024

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.net.URI

@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val uri: Uri,
    val description: String? = null,
    var order: Int
)
