package com.example.gpt011024

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Task::class, Photo::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class) // Добавим конвертер для URI
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun photoDao(): PhotoDao
}