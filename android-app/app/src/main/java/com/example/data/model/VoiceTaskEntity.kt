package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "voice_tasks")
data class VoiceTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String? = null,
    val projectName: String? = null,
    val title: String,
    val script: String,
    val voiceName: String = "Puck (Studio Male)",
    val speed: Float = 1.0f,
    val pitch: Float = 0.0f,
    val status: String = TaskStatus.QUEUED.name,
    val outputPath: String? = null,
    val outputFileName: String? = null,
    val durationSeconds: Float = 0.0f,
    val fileSizeBytes: Long = 0L,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val isCompleted: Boolean get() = status == TaskStatus.COMPLETED.name
    val isFailed: Boolean get() = status == TaskStatus.FAILED.name
    val isRunning: Boolean get() = status == TaskStatus.RUNNING.name
}
