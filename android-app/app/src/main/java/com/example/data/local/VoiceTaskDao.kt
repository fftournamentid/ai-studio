package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VoiceTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceTaskDao {
    @Query("SELECT * FROM voice_tasks ORDER BY createdAt DESC")
    fun getAllVoiceTasksFlow(): Flow<List<VoiceTaskEntity>>

    @Query("SELECT * FROM voice_tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getVoiceTasksForProjectFlow(projectId: String): Flow<List<VoiceTaskEntity>>

    @Query("SELECT * FROM voice_tasks WHERE id = :id LIMIT 1")
    suspend fun getVoiceTaskById(id: String): VoiceTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceTask(task: VoiceTaskEntity)

    @Update
    suspend fun updateVoiceTask(task: VoiceTaskEntity)

    @Delete
    suspend fun deleteVoiceTask(task: VoiceTaskEntity)

    @Query("DELETE FROM voice_tasks WHERE id = :id")
    suspend fun deleteVoiceTaskById(id: String)
}
