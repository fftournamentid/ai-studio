package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VideoTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoTaskDao {
    @Query("SELECT * FROM video_tasks WHERE projectId = :projectId ORDER BY sequenceNumber ASC")
    fun getTasksForProjectFlow(projectId: String): Flow<List<VideoTaskEntity>>

    @Query("SELECT * FROM video_tasks WHERE projectId = :projectId ORDER BY sequenceNumber ASC")
    suspend fun getTasksForProject(projectId: String): List<VideoTaskEntity>

    @Query("SELECT * FROM video_tasks ORDER BY createdAt DESC")
    fun getAllTasksFlow(): Flow<List<VideoTaskEntity>>

    @Query("SELECT * FROM video_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): VideoTaskEntity?

    @Query("SELECT MAX(sequenceNumber) FROM video_tasks WHERE projectId = :projectId")
    suspend fun getMaxSequenceNumber(projectId: String): Int?

    @Query("SELECT * FROM video_tasks WHERE status = 'FAILED' ORDER BY createdAt DESC")
    fun getFailedTasksFlow(): Flow<List<VideoTaskEntity>>

    @Query("SELECT * FROM video_tasks WHERE projectId = :projectId AND status = 'FAILED' ORDER BY sequenceNumber ASC")
    suspend fun getFailedTasksForProject(projectId: String): List<VideoTaskEntity>

    @Query("SELECT * FROM video_tasks WHERE status IN ('QUEUED', 'RUNNING') ORDER BY sequenceNumber ASC")
    fun getActiveTasksFlow(): Flow<List<VideoTaskEntity>>

    @Query("SELECT * FROM video_tasks WHERE status IN ('QUEUED', 'RUNNING') ORDER BY sequenceNumber ASC")
    suspend fun getActiveTasks(): List<VideoTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: VideoTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<VideoTaskEntity>)

    @Update
    suspend fun updateTask(task: VideoTaskEntity)

    @Query("UPDATE video_tasks SET status = :status, progressPercent = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, progress: Int)

    @Query("UPDATE video_tasks SET status = :status, progressPercent = 100, outputPath = :outputPath, outputFileName = :outputFileName, fileSizeBytes = :fileSizeBytes, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(
        id: String,
        status: String,
        outputPath: String,
        outputFileName: String,
        fileSizeBytes: Long,
        completedAt: Long
    )

    @Query("UPDATE video_tasks SET status = 'FAILED', errorMessage = :errorMessage, errorCode = :errorCode WHERE id = :id")
    suspend fun markFailed(id: String, errorMessage: String, errorCode: String)

    @Query("UPDATE video_tasks SET status = 'CANCELLED' WHERE status IN ('QUEUED', 'RUNNING')")
    suspend fun cancelAllActiveTasks()

    @Delete
    suspend fun deleteTask(task: VideoTaskEntity)

    @Query("DELETE FROM video_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)
}
