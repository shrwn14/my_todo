package com.shrwn.todo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TodoTask>>

    @Query("SELECT * FROM todo_tasks ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getTasksPaged(limit: Int, offset: Int): List<TodoTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TodoTask)

    @Update
    suspend fun updateTask(task: TodoTask)

    @Delete
    suspend fun deleteTask(task: TodoTask)

    @Query("SELECT COUNT(*) FROM todo_tasks WHERE createdAt >= :startOfDay")
    fun getTasksForDayCount(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_tasks WHERE isCompleted = 1")
    fun getCompletedTasksCount(): Flow<Int>
}
