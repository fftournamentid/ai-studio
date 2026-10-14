package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CreateProjectDialog
import com.example.ui.components.StatusLogDialog
import com.example.ui.components.TopStudioBar
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.StudioPrimary
import com.example.ui.videostudio.VideoStudioScreen
import com.example.ui.viewmodel.StudioViewModel
import com.example.ui.viewmodel.UiEvent
import com.example.ui.voicestudio.VoiceStudioScreen

@Composable
fun MainStudioApp(
    viewModel: StudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val activeTasks by viewModel.activeVideoTasks.collectAsStateWithLifecycle()
    val activeSubProject by viewModel.activeSubProject.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateProjectDialog by remember { mutableStateOf(false) }

    // Error Dialog state
    var errorDialogData by remember {
        mutableStateOf<UiEvent.ShowErrorDialog?>(null)
    }

    val isStudioOpen by viewModel.isProjectStudioOpen.collectAsStateWithLifecycle()

    // Handle back stack navigation
    BackHandler(enabled = (activeTab == 0 && isStudioOpen) || activeTab != 0 || activeSubProject != null) {
        if (activeTab == 0 && isStudioOpen) {
            viewModel.isProjectStudioOpen.value = false
        } else if (activeSubProject != null) {
            viewModel.activeSubProject.value = null
        } else if (activeTab != 0) {
            viewModel.activeTab.value = 0
        }
    }

    // Listen to UI Events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ShowErrorDialog -> {
                    errorDialogData = event
                }
            }
        }
    }

    val showBars = !(activeTab == 0 && isStudioOpen)

    Scaffold(
        topBar = {
            if (showBars) {
                TopStudioBar(
                    activeProject = activeProject,
                    allProjects = allProjects,
                    activeTasksCount = activeTasks.size,
                    onSelectProject = { viewModel.selectProject(it) },
                    onCreateNewProjectClick = { showCreateProjectDialog = true },
                    onSettingsClick = { viewModel.activeTab.value = 3 }
                )
            }
        },
        bottomBar = {
            if (showBars) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { viewModel.activeTab.value = 0 },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Video Studio") },
                    label = { Text("Video Studio", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioPrimary,
                        selectedTextColor = StudioPrimary
                    ),
                    modifier = Modifier.testTag("tab_video_studio")
                )

                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { viewModel.activeTab.value = 1 },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = "Voice Studio") },
                    label = { Text("Voice Studio", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioPrimary,
                        selectedTextColor = StudioPrimary
                    ),
                    modifier = Modifier.testTag("tab_voice_studio")
                )

                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { viewModel.activeTab.value = 2 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Downloads") },
                    label = { Text("Downloads", fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioPrimary,
                        selectedTextColor = StudioPrimary
                    ),
                    modifier = Modifier.testTag("tab_downloads")
                )

                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { viewModel.activeTab.value = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioPrimary,
                        selectedTextColor = StudioPrimary
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "MainTabsTransition"
            ) { tab ->
                when (tab) {
                    0 -> VideoStudioScreen(viewModel = viewModel)
                    1 -> VoiceStudioScreen(viewModel = viewModel)
                    2 -> DownloadsScreen(
                        viewModel = viewModel,
                        onOpenSettings = { viewModel.activeTab.value = 3 }
                    )
                    3 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onCreate = { name, ratio, res, style, desc ->
                viewModel.createProject(name, ratio, res, style, desc)
            }
        )
    }

    errorDialogData?.let { dialogData ->
        StatusLogDialog(
            title = dialogData.title,
            message = dialogData.message,
            canRetry = dialogData.canRetry,
            onRetry = {
                dialogData.projectId?.let { pid -> viewModel.retryFailedVideos(pid) }
                errorDialogData = null
            },
            onOpenSettings = {
                viewModel.activeTab.value = 3
                errorDialogData = null
            },
            onDismiss = { errorDialogData = null }
        )
    }
}
