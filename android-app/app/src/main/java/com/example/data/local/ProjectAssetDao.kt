package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ProjectAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectAssetDao {
    @Query("SELECT * FROM project_assets WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun getAssetsForProjectFlow(projectId: String): Flow<List<ProjectAssetEntity>>

    @Query("SELECT * FROM project_assets WHERE projectId = :projectId ORDER BY createdAt ASC")
    suspend fun getAssetsForProject(projectId: String): List<ProjectAssetEntity>

    @Query("SELECT * FROM project_assets WHERE projectId = :projectId AND tag = :tag LIMIT 1")
    suspend fun getAssetByTag(projectId: String, tag: String): ProjectAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: ProjectAssetEntity)

    @Delete
    suspend fun deleteAsset(asset: ProjectAssetEntity)

    @Query("DELETE FROM project_assets WHERE id = :id")
    suspend fun deleteAssetById(id: String)
}
