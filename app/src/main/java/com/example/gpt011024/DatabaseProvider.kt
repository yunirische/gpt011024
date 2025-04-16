package com.example.gpt011024

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.internal.synchronized

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
        return instance!!
    }
}