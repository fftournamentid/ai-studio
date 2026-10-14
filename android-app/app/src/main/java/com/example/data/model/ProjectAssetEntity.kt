package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "project_assets",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["projectId", "tag"], unique = true)]
)
data class ProjectAssetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val tag: String, // e.g., "img_hero", "img_bg", "img_character"
    val filePath: String,
    val fileName: String,
    val mimeType: String = "image/png",
    val fileSizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
