package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.WaveformSynthesizer
import com.example.data.local.AppDatabase
import com.example.data.model.GenerationLogEntity
import com.example.data.model.TaskStatus
import com.example.data.model.VideoTaskEntity
import com.example.data.model.VoiceTaskEntity
import com.example.data.remote.AiStudioApiService
import com.example.data.remote.ApiResult
import com.example.data.security.SecureStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class MediaGenerationService : Service() {

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var database: AppDatabase
    private lateinit var secureStorage: SecureStorageManager
    private val apiService = AiStudioApiService()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeCount = AtomicInteger(0)
    private val completedCount = AtomicInteger(0)
    private val totalCount = AtomicInteger(0)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(applicationContext)
        secureStorage = SecureStorageManager.getInstance(applicationContext)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_GENERATE_VIDEOS -> {
                val projectId = intent.getStringExtra(EXTRA_PROJECT_ID) ?: return START_NOT_STICKY
                val batchId = intent.getStringExtra(EXTRA_BATCH_ID)
                startForeground(NOTIFICATION_ID_FOREGROUND, buildProgressNotification("Starting parallel video generation...", 0, 100))
                processVideoBatch(projectId, batchId)
            }
            ACTION_GENERATE_VOICE -> {
                val voiceTaskId = intent.getStringExtra(EXTRA_VOICE_TASK_ID) ?: return START_NOT_STICKY
                startForeground(NOTIFICATION_ID_FOREGROUND, buildProgressNotification("Generating speech audio clip...", 0, 100))
                processVoiceTask(voiceTaskId)
            }
            ACTION_RETRY_FAILED -> {
                val projectId = intent.getStringExtra(EXTRA_PROJECT_ID) ?: return START_NOT_STICKY
                startForeground(NOTIFICATION_ID_FOREGROUND, buildProgressNotification("Retrying failed videos...", 0, 100))
                retryFailedVideos(projectId)
            }
            ACTION_CANCEL_ALL -> {
                cancelAll()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun processVideoBatch(projectId: String, batchId: String?) {
        scope.launch {
            val queuedTasks = database.videoTaskDao().getActiveTasks()
                .filter { it.projectId == projectId && (batchId == null || it.batchId == batchId) }

            if (queuedTasks.isEmpty()) {
                checkCompletionAndStop()
                return@launch
            }

            totalCount.set(queuedTasks.size)
            completedCount.set(0)
            activeCount.set(queuedTasks.size)

            val concurrencyLimit = secureStorage.getConcurrencyLimit().coerceIn(1, 15)
            val semaphore = Semaphore(concurrencyLimit)
            val videoApiKey = secureStorage.getVideoApiKey()

            for (task in queuedTasks) {
                val job = launch {
                    semaphore.withPermit {
                        executeSingleVideoGeneration(task, videoApiKey)
                    }
                }
                activeJobs[task.id] = job
            }
        }
    }

    private fun retryFailedVideos(projectId: String) {
        scope.launch {
            val failedTasks = database.videoTaskDao().getFailedTasksForProject(projectId)
            if (failedTasks.isEmpty()) {
                checkCompletionAndStop()
                return@launch
            }

            // Reset tasks to QUEUED
            val resetTasks = failedTasks.map {
                it.copy(
                    status = TaskStatus.QUEUED.name,
                    progressPercent = 0,
                    errorMessage = null,
                    errorCode = null
                )
            }
            database.videoTaskDao().insertTasks(resetTasks)

            processVideoBatch(projectId, null)
        }
    }

    private suspend fun executeSingleVideoGeneration(task: VideoTaskEntity, apiKey: String) {
        val videoDao = database.videoTaskDao()
        val logDao = database.generationLogDao()

        // 1. Mark Running
        videoDao.updateProgress(task.id, TaskStatus.RUNNING.name, 10)
        updateForegroundNotification(
            "Generating #${task.sequenceLabel}: ${task.projectName}",
            completedCount.get(),
            totalCount.get().coerceAtLeast(1)
        )

        // 2. Validate Key / Check Quota with Multi-Key Auto Failover
        var currentKey = apiKey
        var keyItem = secureStorage.getApiKeyPool().find { it.key == currentKey }
            ?: secureStorage.getNextAvailableKey()

        while (currentKey.isNotBlank()) {
            val validation = apiService.validateApiKey(currentKey)
            if (validation is ApiResult.Error && validation.isQuotaExhausted) {
                // Current key exhausted! Mark it in pool
                secureStorage.markKeyExhausted(currentKey)
                val backupKey = secureStorage.getNextAvailableKey()

                if (backupKey != null && backupKey.key != currentKey) {
                    // Automatically failover to backup key!
                    logDao.insertLog(
                        GenerationLogEntity(
                            projectId = task.projectId,
                            projectName = task.projectName,
                            mediaType = "VIDEO",
                            itemNumber = task.sequenceNumber,
                            itemLabel = "#${task.sequenceLabel}",
                            status = "SUCCESS",
                            message = "Key '${keyItem?.label ?: "Active"}' quota exhausted. Automatically rotated to backup key '${backupKey.label}'!",
                            taskId = task.id,
                            canResume = false
                        )
                    )
                    currentKey = backupKey.key
                    keyItem = backupKey
                    continue // Retry validation with new backup key!
                } else {
                    // All keys exhausted
                    val errMsg = "Video #${task.sequenceLabel} failed: All API keys in your pool have exhausted their quotas (HTTP 429). Please add a fresh key in Settings."
                    videoDao.markFailed(task.id, errMsg, "QUOTA_EXHAUSTED")
                    logDao.insertLog(
                        GenerationLogEntity(
                            projectId = task.projectId,
                            projectName = task.projectName,
                            mediaType = "VIDEO",
                            itemNumber = task.sequenceNumber,
                            itemLabel = "#${task.sequenceLabel}",
                            status = "FAILED",
                            message = errMsg,
                            taskId = task.id,
                            canResume = true
                        )
                    )
                    showFailureNotification("All API Keys Exhausted (Video #${task.sequenceLabel})", errMsg)
                    onTaskFinished(task.id)
                    return
                }
            } else {
                // Key is valid or network error
                break
            }
        }

        // 3. Prepare Target File: downloads/{projectName}/{projectName}_{sequenceLabel}.mp4
        val safeProjectName = task.projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val downloadsDir = File(applicationContext.filesDir, "downloads/$safeProjectName").apply { mkdirs() }
        val outputFileName = "${safeProjectName}_${task.sequenceLabel}.mp4"
        val outputFile = File(downloadsDir, outputFileName)

        try {
            // Retrieve referenced assets
            val assetPaths = database.projectAssetDao().getAssetsForProject(task.projectId)
                .map { it.filePath }

            videoDao.updateProgress(task.id, TaskStatus.RUNNING.name, 35)

            // Generate H.264 MP4 with animated frames, sequence number badge, and prompt
            val fileSize = VideoFileGenerator.generateMp4Video(
                context = applicationContext,
                projectName = task.projectName,
                sequenceLabel = task.sequenceLabel,
                prompt = task.prompt,
                aspectRatio = task.aspectRatio,
                resolution = task.resolution,
                durationSeconds = task.durationSeconds,
                referencedAssetPaths = assetPaths,
                outputFile = outputFile,
                onProgress = { progress ->
                    scope.launch {
                        videoDao.updateProgress(task.id, TaskStatus.RUNNING.name, (35 + progress * 0.6).toInt())
                    }
                }
            )

            // 4. Mark Completed
            videoDao.markCompleted(
                id = task.id,
                status = TaskStatus.COMPLETED.name,
                outputPath = outputFile.absolutePath,
                outputFileName = outputFileName,
                fileSizeBytes = fileSize,
                completedAt = System.currentTimeMillis()
            )

            logDao.insertLog(
                GenerationLogEntity(
                    projectId = task.projectId,
                    projectName = task.projectName,
                    mediaType = "VIDEO",
                    itemNumber = task.sequenceNumber,
                    itemLabel = "#${task.sequenceLabel}",
                    status = "SUCCESS",
                    message = "Video #${task.sequenceLabel} ($outputFileName) generated and downloaded successfully.",
                    taskId = task.id,
                    canResume = false
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Video generation failed for task ${task.id}", e)
            val errMsg = "Video #${task.sequenceLabel} failed due to error: ${e.localizedMessage ?: "Unknown error"}"
            videoDao.markFailed(task.id, errMsg, "NETWORK_ERROR")
            logDao.insertLog(
                GenerationLogEntity(
                    projectId = task.projectId,
                    projectName = task.projectName,
                    mediaType = "VIDEO",
                    itemNumber = task.sequenceNumber,
                    itemLabel = "#${task.sequenceLabel}",
                    status = "FAILED",
                    message = errMsg,
                    taskId = task.id,
                    canResume = true
                )
            )
            showFailureNotification("Video #${task.sequenceLabel} Generation Failed", errMsg)
        } finally {
            completedCount.incrementAndGet()
            onTaskFinished(task.id)
        }
    }

    private fun processVoiceTask(voiceTaskId: String) {
        scope.launch {
            val voiceDao = database.voiceTaskDao()
            val task = voiceDao.getVoiceTaskById(voiceTaskId) ?: return@launch

            voiceDao.updateVoiceTask(task.copy(status = TaskStatus.RUNNING.name))

            val voiceDir = File(applicationContext.filesDir, "downloads/voice").apply { mkdirs() }
            val safeTitle = task.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val outputFileName = "${safeTitle}_${System.currentTimeMillis()}.wav"
            val outputFile = File(voiceDir, outputFileName)

            try {
                val fileSize = WaveformSynthesizer.synthesizeSpeechToFile(
                    context = applicationContext,
                    script = task.script,
                    voiceName = task.voiceName,
                    pitch = task.pitch,
                    speed = task.speed,
                    outputFile = outputFile
                )

                val durationSeconds = (task.script.split("\\s+".toRegex()).size * 0.45f + 1.5f) / task.speed.coerceAtLeast(0.5f)

                voiceDao.updateVoiceTask(
                    task.copy(
                        status = TaskStatus.COMPLETED.name,
                        outputPath = outputFile.absolutePath,
                        outputFileName = outputFileName,
                        durationSeconds = durationSeconds,
                        fileSizeBytes = fileSize,
                        completedAt = System.currentTimeMillis()
                    )
                )

                showCompletionNotification("Voice Clip Ready", "Generated audio clip: ${task.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Voice generation failed", e)
                voiceDao.updateVoiceTask(
                    task.copy(
                        status = TaskStatus.FAILED.name,
                        errorMessage = "Voice generation failed: ${e.message}"
                    )
                )
            } finally {
                checkCompletionAndStop()
            }
        }
    }

    private fun onTaskFinished(taskId: String) {
        activeJobs.remove(taskId)
        val remaining = activeCount.decrementAndGet()
        val completed = completedCount.get()
        val total = totalCount.get()

        if (remaining <= 0) {
            showCompletionNotification(
                "Batch Generation Complete",
                "Successfully generated and downloaded $completed of $total items."
            )
            checkCompletionAndStop()
        } else {
            updateForegroundNotification(
                "Batch in progress: $completed/$total videos ready",
                completed,
                total
            )
        }
    }

    private fun cancelAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        scope.launch {
            database.videoTaskDao().cancelAllActiveTasks()
        }
    }

    private fun checkCompletionAndStop() {
        if (activeJobs.isEmpty()) {
            scope.launch {
                delay(1500)
                if (activeJobs.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                "Media Generation Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time parallel media generation progress"
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Media Alerts & Errors",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when video generation completes or fails"
            }

            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun buildProgressNotification(text: String, current: Int, max: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, MediaGenerationService::class.java).apply {
            action = ACTION_CANCEL_ALL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_PROGRESS)
            .setContentTitle("AI Studio Media Generator")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setProgress(max, current, max == 0)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel All", cancelPendingIntent)
            .build()
    }

    private fun updateForegroundNotification(text: String, current: Int, max: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_FOREGROUND, buildProgressNotification(text, current, max))
    }

    private fun showFailureNotification(title: String, message: String) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 2, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    private fun showCompletionNotification(title: String, message: String) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 3, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_COMPLETED, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "MediaGenService"

        const val CHANNEL_ID_PROGRESS = "media_generation_progress"
        const val CHANNEL_ID_ALERTS = "media_generation_alerts"

        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_COMPLETED = 1002

        const val ACTION_GENERATE_VIDEOS = "com.example.action.GENERATE_VIDEOS"
        const val ACTION_GENERATE_VOICE = "com.example.action.GENERATE_VOICE"
        const val ACTION_RETRY_FAILED = "com.example.action.RETRY_FAILED"
        const val ACTION_CANCEL_ALL = "com.example.action.CANCEL_ALL"

        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_BATCH_ID = "extra_batch_id"
        const val EXTRA_VOICE_TASK_ID = "extra_voice_task_id"

        fun startVideoBatch(context: Context, projectId: String, batchId: String) {
            val intent = Intent(context, MediaGenerationService::class.java).apply {
                action = ACTION_GENERATE_VIDEOS
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_BATCH_ID, batchId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startVoiceTask(context: Context, voiceTaskId: String) {
            val intent = Intent(context, MediaGenerationService::class.java).apply {
                action = ACTION_GENERATE_VOICE
                putExtra(EXTRA_VOICE_TASK_ID, voiceTaskId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun retryFailed(context: Context, projectId: String) {
            val intent = Intent(context, MediaGenerationService::class.java).apply {
                action = ACTION_RETRY_FAILED
                putExtra(EXTRA_PROJECT_ID, projectId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelAll(context: Context) {
            val intent = Intent(context, MediaGenerationService::class.java).apply {
                action = ACTION_CANCEL_ALL
            }
            context.startService(intent)
        }
    }
}
