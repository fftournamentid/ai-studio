package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.GenerationLogEntity
import com.example.data.model.TaskStatus
import com.example.data.model.VideoTaskEntity
import com.example.data.model.VoiceTaskEntity
import com.example.data.remote.AiStudioApiService
import com.example.data.remote.ApiResult
import com.example.data.security.SecureStorageManager
import com.example.service.MediaGenerationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class GenerationRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val secureStorage: SecureStorageManager
) {
    private val videoTaskDao = database.videoTaskDao()
    private val voiceTaskDao = database.voiceTaskDao()
    private val logDao = database.generationLogDao()
    private val apiService = AiStudioApiService()

    val allVideoTasksFlow: Flow<List<VideoTaskEntity>> = videoTaskDao.getAllTasksFlow()
    val activeVideoTasksFlow: Flow<List<VideoTaskEntity>> = videoTaskDao.getActiveTasksFlow()
    val failedVideoTasksFlow: Flow<List<VideoTaskEntity>> = videoTaskDao.getFailedTasksFlow()

    val allVoiceTasksFlow: Flow<List<VoiceTaskEntity>> = voiceTaskDao.getAllVoiceTasksFlow()
    val allLogsFlow: Flow<List<GenerationLogEntity>> = logDao.getAllLogsFlow()
    val failedLogsFlow: Flow<List<GenerationLogEntity>> = logDao.getFailedLogsFlow()

    fun getVideoTasksForProjectFlow(projectId: String): Flow<List<VideoTaskEntity>> =
        videoTaskDao.getTasksForProjectFlow(projectId)

    fun getVoiceTasksForProjectFlow(projectId: String): Flow<List<VoiceTaskEntity>> =
        voiceTaskDao.getVoiceTasksForProjectFlow(projectId)

    fun getLogsForProjectFlow(projectId: String): Flow<List<GenerationLogEntity>> =
        logDao.getLogsForProjectFlow(projectId)

    /**
     * Enqueues up to 15 parallel video generation requests with sequential numbering (e.g. 001, 002)
     * and dispatches them to the persistent background foreground service.
     */
    suspend fun enqueueVideoBatch(
        projectId: String,
        projectName: String,
        prompt: String,
        aspectRatio: String,
        resolution: String,
        style: String,
        durationSeconds: Int,
        batchCount: Int,
        referencedAssetTags: String = ""
    ): String = withContext(Dispatchers.IO) {
        val currentMaxSeq = videoTaskDao.getMaxSequenceNumber(projectId) ?: 0
        val batchId = UUID.randomUUID().toString()
        val count = batchCount.coerceIn(1, 15)

        val newTasks = (1..count).map { offset ->
            val seqNum = currentMaxSeq + offset
            val seqLabel = String.format("%03d", seqNum)
            val variationPrompt = if (count > 1) {
                "$prompt (Variation #$seqLabel)"
            } else {
                prompt
            }

            VideoTaskEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                projectName = projectName,
                sequenceNumber = seqNum,
                sequenceLabel = seqLabel,
                prompt = variationPrompt,
                referencedAssetTags = referencedAssetTags,
                aspectRatio = aspectRatio,
                resolution = resolution,
                style = style,
                durationSeconds = durationSeconds,
                status = TaskStatus.QUEUED.name,
                progressPercent = 0,
                outputFileName = "${projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}_$seqLabel.mp4",
                batchId = batchId,
                createdAt = System.currentTimeMillis()
            )
        }

        videoTaskDao.insertTasks(newTasks)

        // Trigger persistent Foreground Service to run in background
        MediaGenerationService.startVideoBatch(context, projectId, batchId)

        batchId
    }

    suspend fun retryFailedVideos(projectId: String) = withContext(Dispatchers.IO) {
        MediaGenerationService.retryFailed(context, projectId)
    }

    suspend fun cancelAllActiveTasks() = withContext(Dispatchers.IO) {
        MediaGenerationService.cancelAll(context)
        videoTaskDao.cancelAllActiveTasks()
    }

    suspend fun deleteVideoTask(task: VideoTaskEntity) = withContext(Dispatchers.IO) {
        task.outputPath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) file.delete()
        }
        videoTaskDao.deleteTask(task)
    }

    suspend fun enqueueVoiceTask(
        projectId: String?,
        projectName: String?,
        title: String,
        script: String,
        voiceName: String,
        speed: Float,
        pitch: Float
    ): String = withContext(Dispatchers.IO) {
        val taskId = UUID.randomUUID().toString()
        val task = VoiceTaskEntity(
            id = taskId,
            projectId = projectId,
            projectName = projectName,
            title = title.ifBlank { "Voice Clip" },
            script = script,
            voiceName = voiceName,
            speed = speed,
            pitch = pitch,
            status = TaskStatus.QUEUED.name,
            createdAt = System.currentTimeMillis()
        )
        voiceTaskDao.insertVoiceTask(task)

        MediaGenerationService.startVoiceTask(context, taskId)
        taskId
    }

    suspend fun deleteVoiceTask(task: VoiceTaskEntity) = withContext(Dispatchers.IO) {
        task.outputPath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) file.delete()
        }
        voiceTaskDao.deleteVoiceTask(task)
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        logDao.clearAllLogs()
    }

    suspend fun enhancePrompt(prompt: String, style: String, aspectRatio: String): String =
        withContext(Dispatchers.IO) {
            val apiKey = secureStorage.getVideoApiKey()
            when (val res = apiService.enhancePrompt(apiKey, prompt, style, aspectRatio)) {
                is ApiResult.Success -> res.data
                is ApiResult.Error -> "$prompt, ultra-cinematic $style, photorealistic lighting, 4k render."
            }
        }

    val apiKeyPoolFlow = secureStorage.apiKeyPoolFlow

    fun addApiKeyToPool(key: String, label: String) {
        secureStorage.addApiKeyToPool(key, label)
    }

    fun removeApiKeyFromPool(id: String) {
        secureStorage.removeApiKeyFromPool(id)
    }

    fun resetExhaustedKeys() {
        secureStorage.resetExhaustedKeys()
    }

    suspend fun testApiKey(apiKey: String): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        apiService.validateApiKey(apiKey)
    }
}
