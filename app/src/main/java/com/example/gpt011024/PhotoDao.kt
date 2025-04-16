package com.example.gpt011024

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE taskId = :taskId ORDER BY 'order' ASC")
    fun getPhotosForTask(taskId: Int): List<Photo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPhoto(photo: Photo)

    @Delete
    fun deletePhoto(photo: Photo)

    @Update
    suspend fun update(photo: Photo)
}