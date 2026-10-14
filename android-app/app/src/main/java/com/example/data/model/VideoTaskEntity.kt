package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "video_tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["projectId", "sequenceNumber"], unique = true),
        Index(value = ["status"])
    ]
)
data class VideoTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val projectName: String,
    val sequenceNumber: Int, // 1, 2, 3...
    val sequenceLabel: String, // "001", "002"...
    val prompt: String,
    val referencedAssetTags: String = "", // comma-separated tags e.g. "img_hero,img_bg"
    val aspectRatio: String = "16:9",
    val resolution: String = "1080p",
    val style: String = "Cinematic",
    val durationSeconds: Int = 5,
    val status: String = TaskStatus.QUEUED.name,
    val progressPercent: Int = 0,
    val outputPath: String? = null,
    val outputFileName: String? = null, // e.g., "ProjectName_001.mp4"
    val fileSizeBytes: Long = 0L,
    val errorMessage: String? = null,
    val errorCode: String? = null, // "QUOTA_EXHAUSTED", "NETWORK_ERROR", "INVALID_KEY", etc.
    val batchId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val isCompleted: Boolean get() = status == TaskStatus.COMPLETED.name
    val isFailed: Boolean get() = status == TaskStatus.FAILED.name
    val isRunning: Boolean get() = status == TaskStatus.RUNNING.name
    val isQueued: Boolean get() = status == TaskStatus.QUEUED.name
}
