package com.example.gpt011024

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithPhotos(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val photos: List<Photo>
)
