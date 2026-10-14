package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generation_logs")
data class GenerationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val projectId: String,
    val projectName: String,
    val mediaType: String, // "VIDEO" or "VOICE"
    val itemNumber: Int, // Video #004
    val itemLabel: String, // "#004"
    val status: String, // "SUCCESS" or "FAILED"
    val message: String, // e.g., "Video #004 failed due to quota/network error: 429 Quota Exceeded"
    val taskId: String,
    val canResume: Boolean = true
)
