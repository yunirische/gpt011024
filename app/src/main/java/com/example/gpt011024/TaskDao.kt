package com.example.gpt011024

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getById(taskId: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tasks: List<Task>)

    @Update
    fun update(task: Task)

    @Delete
    fun delete(task: Task)
}
