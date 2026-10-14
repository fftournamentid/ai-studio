package com.example.ui.downloads

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GenerationLogEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.VideoTaskEntity
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSuccess
import com.example.ui.viewmodel.StudioViewModel
import java.io.File

@Composable
fun DownloadsScreen(
    viewModel: StudioViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSubProject by viewModel.activeSubProject.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allVideos by viewModel.allVideoTasks.collectAsStateWithLifecycle()
    val failedLogs by viewModel.failedLogs.collectAsStateWithLifecycle()
    val previewVideo by viewModel.previewVideo.collectAsStateWithLifecycle()

    var selectedSectionTab by remember { mutableStateOf(0) } // 0: Video Projects, 1: Failure Status Log

    // Handle back button when drilled into a specific project
    if (activeSubProject != null) {
        BackHandler {
            viewModel.activeSubProject.value = null
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (activeSubProject == null) {
            // Level 1: Projects Overview
            TabRow(
                selectedTabIndex = selectedSectionTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedSectionTab == 0,
                    onClick = { selectedSectionTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Projects & Downloads")
                        }
                    }
                )
                Tab(
                    selected = selectedSectionTab == 1,
                    onClick = { selectedSectionTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Status & Error Log")
                            if (failedLogs.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = StudioError,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${failedLogs.size}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }

            if (selectedSectionTab == 0) {
                ProjectsListView(
                    projects = allProjects,
                    allVideos = allVideos,
                    failedLogs = failedLogs,
                    onSelectProject = { project ->
                        viewModel.activeSubProject.value = project
                    },
                    onRetryFailed = { projectId ->
                        viewModel.retryFailedVideos(projectId)
                    }
                )
            } else {
                FailureStatusLogView(
                    logs = failedLogs,
                    onRetryProject = { projectId -> viewModel.retryFailedVideos(projectId) },
                    onOpenSettings = onOpenSettings,
                    onClearLogs = { viewModel.clearLogs() }
                )
            }
        } else {
            // Level 2: Project Detail & Sequential Video List
            ProjectVideosDetailView(
                project = activeSubProject!!,
                videos = allVideos.filter { it.projectId == activeSubProject!!.id },
                onBack = { viewModel.activeSubProject.value = null },
                onPlayVideo = { video -> viewModel.previewVideo.value = video },
                onRetryFailed = { viewModel.retryFailedVideos(activeSubProject!!.id) },
                onDeleteVideo = { video -> viewModel.deleteVideoTask(video) }
            )
        }
    }

    previewVideo?.let { video ->
        VideoPlayerDialog(
            video = video,
            onDismiss = { viewModel.previewVideo.value = null }
        )
    }
}

@Composable
private fun ProjectsListView(
    projects: List<ProjectEntity>,
    allVideos: List<VideoTaskEntity>,
    failedLogs: List<GenerationLogEntity>,
    onSelectProject: (ProjectEntity) -> Unit,
    onRetryFailed: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // High-visibility Error / Failure Banner if any video failed
        if (failedLogs.isNotEmpty()) {
            val mostRecent = failedLogs.first()
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioError.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StudioError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Action Required: Generation Error",
                                fontWeight = FontWeight.Bold,
                                color = StudioError,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = mostRecent.message,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onRetryFailed(mostRecent.projectId) },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioError),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Resume", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Projects Directory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap a project to view its sequentially named videos (e.g. Project_001.mp4).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (projects.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No projects yet", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Create a project in the Video Studio tab to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(projects) { project ->
                val projectVideos = allVideos.filter { it.projectId == project.id }
                val completedCount = projectVideos.count { it.isCompleted }
                val runningCount = projectVideos.count { it.isRunning || it.isQueued }
                val failedCount = projectVideos.count { it.isFailed }

                ProjectFolderCard(
                    project = project,
                    totalVideos = projectVideos.size,
                    completedCount = completedCount,
                    runningCount = runningCount,
                    failedCount = failedCount,
                    onClick = { onSelectProject(project) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProjectFolderCard(
    project: ProjectEntity,
    totalVideos: Int,
    completedCount: Int,
    runningCount: Int,
    failedCount: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("project_folder_card_${project.name}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = StudioPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = project.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${project.aspectRatio} • ${project.resolution} • ${project.defaultStyle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "$totalVideos Videos",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Breakdown Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (completedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioSuccess.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioSuccess, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$completedCount Done", color = StudioSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (runningCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioSecondary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = StudioSecondary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$runningCount Active", color = StudioSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (failedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioError.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = StudioError, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$failedCount Failed", color = StudioError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectVideosDetailView(
    project: ProjectEntity,
    videos: List<VideoTaskEntity>,
    onBack: () -> Unit,
    onPlayVideo: (VideoTaskEntity) -> Unit,
    onRetryFailed: () -> Unit,
    onDeleteVideo: (VideoTaskEntity) -> Unit
) {
    val failedVideos = videos.filter { it.isFailed }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Button & Project Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Projects")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${videos.size} sequentially numbered videos • ${project.aspectRatio}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (failedVideos.isNotEmpty()) {
                    Button(
                        onClick = onRetryFailed,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioError),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry Failed (${failedVideos.size})", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (videos.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No videos generated yet", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Generate videos in the Video Studio tab. They will appear here named sequentially as ${project.name}_001.mp4",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(videos) { video ->
                SequentialVideoCard(
                    video = video,
                    onPlay = { onPlayVideo(video) },
                    onDelete = { onDeleteVideo(video) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SequentialVideoCard(
    video: VideoTaskEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sequential Number Badge & Filename
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            video.isCompleted -> StudioPrimary
                            video.isFailed -> StudioError
                            else -> StudioSecondary
                        },
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Text(
                            text = "#${video.sequenceLabel}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Column {
                        Text(
                            text = video.outputFileName ?: "${video.projectName}_${video.sequenceLabel}.mp4",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${video.aspectRatio} • ${video.resolution} • ${video.durationSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        video.isCompleted -> StudioSuccess.copy(alpha = 0.15f)
                        video.isFailed -> StudioError.copy(alpha = 0.15f)
                        else -> StudioSecondary.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = video.status,
                        color = when {
                            video.isCompleted -> StudioSuccess
                            video.isFailed -> StudioError
                            else -> StudioSecondary
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Running progress or error reason
            if (video.isRunning || video.isQueued) {
                LinearProgressIndicator(
                    progress = { video.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = StudioSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Generating: ${video.progressPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioSecondary
                )
            } else if (video.isFailed) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioError.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = video.errorMessage ?: "Failed due to quota exhaustion or network error.",
                        color = StudioError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Prompt
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"${video.prompt}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Actions: Play, Share, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (video.isCompleted) {
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("play_video_button_${video.sequenceLabel}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview Video", fontSize = 12.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (video.isCompleted && video.outputPath != null) {
                        IconButton(
                            onClick = {
                                val file = File(video.outputPath)
                                if (file.exists()) {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                    } catch (_: Exception) {}
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FailureStatusLogView(
    logs: List<GenerationLogEntity>,
    onRetryProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClearLogs: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Failure Tracking & Resume Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Identifies exact video numbers that failed so you can resume immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (logs.isNotEmpty()) {
                    OutlinedButton(onClick = onClearLogs, shape = RoundedCornerShape(8.dp)) {
                        Text("Clear", fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (logs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioSuccess, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All Systems Operational", fontWeight = FontWeight.Bold)
                        Text(
                            "No failed generation tasks. All parallel videos downloaded successfully.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(logs) { log ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StudioError
                            ) {
                                Text(
                                    text = "Failed: Video ${log.itemLabel}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = log.projectName,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = log.message,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = onOpenSettings, shape = RoundedCornerShape(8.dp)) {
                                Text("Update API Key", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onRetryProject(log.projectId) },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Resume from ${log.itemLabel}", fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
