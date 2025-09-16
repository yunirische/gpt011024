package com.example.gpt011024

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var guid: String? = null,
    val date: String,
    val counteragent: String,
    val coordinates: String,
    val address: String,
    val auto: String,
    val contacts: String,
    val description: String,
    var comment: String? = null,
    val isSynchronized: Long,
    val createdAt: Long,
    val updatedAt: Long? = null,
    var report: String? = null,
    var isCompleted: Boolean = false
)
