package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val aspectRatio: String = "16:9", // "16:9", "9:16", "1:1", "4:3", "21:9"
    val resolution: String = "1080p", // "720p", "1080p", "4K"
    val defaultStyle: String = "Cinematic",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
