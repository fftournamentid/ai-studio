package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.model.ProjectAssetEntity
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProjectRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val projectDao = database.projectDao()
    private val assetDao = database.projectAssetDao()

    val allProjectsFlow: Flow<List<ProjectEntity>> = projectDao.getAllProjectsFlow()

    fun getProjectByIdFlow(id: String): Flow<ProjectEntity?> = projectDao.getProjectByIdFlow(id)

    suspend fun getProjectById(id: String): ProjectEntity? = projectDao.getProjectById(id)

    fun getAssetsForProjectFlow(projectId: String): Flow<List<ProjectAssetEntity>> =
        assetDao.getAssetsForProjectFlow(projectId)

    suspend fun createProject(
        name: String,
        aspectRatio: String,
        resolution: String,
        defaultStyle: String,
        description: String = ""
    ): ProjectEntity = withContext(Dispatchers.IO) {
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            aspectRatio = aspectRatio,
            resolution = resolution,
            defaultStyle = defaultStyle,
            description = description,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.insertProject(project)
        project
    }

    suspend fun updateProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        // Delete project asset files on disk
        val projectDir = File(context.filesDir, "projects/$projectId")
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
        val downloadDir = File(context.filesDir, "downloads/$projectId")
        if (downloadDir.exists()) {
            downloadDir.deleteRecursively()
        }
        projectDao.deleteProjectById(projectId)
    }

    /**
     * Saves an image asset permanently to local project storage and registers it in Room DB.
     * The user does NOT need to re-upload for subsequent prompts.
     */
    suspend fun saveProjectAssetFromUri(
        projectId: String,
        tag: String,
        sourceUri: Uri
    ): ProjectAssetEntity = withContext(Dispatchers.IO) {
        val cleanTag = tag.trim().replace("@", "").replace("[", "").replace("]", "")
        val assetsDir = File(context.filesDir, "projects/$projectId/assets").apply { mkdirs() }
        val targetFile = File(assetsDir, "${cleanTag}_${System.currentTimeMillis()}.png")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        val asset = ProjectAssetEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            tag = cleanTag,
            filePath = targetFile.absolutePath,
            fileName = targetFile.name,
            mimeType = "image/png",
            fileSizeBytes = targetFile.length(),
            createdAt = System.currentTimeMillis()
        )
        assetDao.insertAsset(asset)
        asset
    }

    /**
     * Creates a synthetic sample asset (e.g. for default demo project or when user picks a preset).
     */
    suspend fun createSyntheticAsset(
        projectId: String,
        tag: String,
        label: String,
        bgColor: Int
    ): ProjectAssetEntity = withContext(Dispatchers.IO) {
        val cleanTag = tag.trim().replace("@", "")
        val assetsDir = File(context.filesDir, "projects/$projectId/assets").apply { mkdirs() }
        val targetFile = File(assetsDir, "${cleanTag}.png")

        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("@$cleanTag", 256f, 240f, paint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, 256f, 290f, subPaint)

        FileOutputStream(targetFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        val asset = ProjectAssetEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            tag = cleanTag,
            filePath = targetFile.absolutePath,
            fileName = targetFile.name,
            mimeType = "image/png",
            fileSizeBytes = targetFile.length(),
            createdAt = System.currentTimeMillis()
        )
        assetDao.insertAsset(asset)
        asset
    }

    suspend fun deleteAsset(asset: ProjectAssetEntity) = withContext(Dispatchers.IO) {
        val file = File(asset.filePath)
        if (file.exists()) {
            file.delete()
        }
        assetDao.deleteAsset(asset)
    }

    /**
     * Ensures at least one rich sample project exists so the user can immediately test
     * video and voice features without setting up from scratch.
     */
    suspend fun seedInitialProjectIfEmpty(): ProjectEntity = withContext(Dispatchers.IO) {
        val existing = projectDao.getProjectById("default_cyberpunk")
        if (existing != null) return@withContext existing

        val project = ProjectEntity(
            id = "default_cyberpunk",
            name = "Neon Odyssey",
            aspectRatio = "16:9",
            resolution = "1080p",
            defaultStyle = "Cyberpunk",
            description = "Futuristic cinematic trailer scenes with neon lighting and dynamic motion",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.insertProject(project)

        createSyntheticAsset(
            projectId = project.id,
            tag = "img_hero",
            label = "Cyber Ronin",
            bgColor = Color.parseColor("#4F46E5")
        )
        createSyntheticAsset(
            projectId = project.id,
            tag = "img_bg",
            label = "Neon Megacity Rain",
            bgColor = Color.parseColor("#0F172A")
        )
        createSyntheticAsset(
            projectId = project.id,
            tag = "img_vehicle",
            label = "Hover Speeder",
            bgColor = Color.parseColor("#059669")
        )

        project
    }
}
