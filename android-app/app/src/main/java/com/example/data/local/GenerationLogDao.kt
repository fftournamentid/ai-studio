package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.GenerationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerationLogDao {
    @Query("SELECT * FROM generation_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<GenerationLogEntity>>

    @Query("SELECT * FROM generation_logs WHERE status = 'FAILED' ORDER BY timestamp DESC")
    fun getFailedLogsFlow(): Flow<List<GenerationLogEntity>>

    @Query("SELECT * FROM generation_logs WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getLogsForProjectFlow(projectId: String): Flow<List<GenerationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: GenerationLogEntity)

    @Query("DELETE FROM generation_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM generation_logs WHERE projectId = :projectId")
    suspend fun clearLogsForProject(projectId: String)
}
