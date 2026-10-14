package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AiStudioApplication
import com.example.audio.AudioPlayerManager
import com.example.data.model.ApiKeyItem
import com.example.data.model.GenerationLogEntity
import com.example.data.model.ProjectAssetEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.VideoTaskEntity
import com.example.data.model.VoiceTaskEntity
import com.example.data.remote.ApiResult
import com.example.data.repository.GenerationRepository
import com.example.data.repository.ProjectRepository
import com.example.data.security.SecureStorageManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowErrorDialog(val title: String, val message: String, val canRetry: Boolean, val projectId: String?) : UiEvent()
}

class StudioViewModel(
    private val projectRepo: ProjectRepository = AiStudioApplication.instance.projectRepository,
    private val generationRepo: GenerationRepository = AiStudioApplication.instance.generationRepository,
    private val secureStorage: SecureStorageManager = AiStudioApplication.instance.secureStorage,
    val audioPlayer: AudioPlayerManager = AiStudioApplication.instance.audioPlayerManager
) : ViewModel() {

    // UI Events (e.g. Failure popups, alerts)
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    // Navigation & Selected Tab
    val activeTab = MutableStateFlow(0) // 0: Video Studio, 1: Voice Studio, 2: Downloads, 3: Settings
    val activeSubProject = MutableStateFlow<ProjectEntity?>(null) // For viewing specific project in Downloads

    // Projects & Active Project
    val allProjects: StateFlow<List<ProjectEntity>> = projectRepo.allProjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProject = MutableStateFlow<ProjectEntity?>(null)

    // Pre-uploaded Assets for currently selected Project
    private val _projectAssets = MutableStateFlow<List<ProjectAssetEntity>>(emptyList())
    val projectAssets: StateFlow<List<ProjectAssetEntity>> = _projectAssets.asStateFlow()

    // Attached images for current generation prompt
    val attachedAssets = MutableStateFlow<List<ProjectAssetEntity>>(emptyList())

    // Project Workspace state (hides bottom tabs when inside a project)
    val isProjectStudioOpen = MutableStateFlow(true)

    // Selected AI Model
    val selectedAiModel = MutableStateFlow("Google Veo-2 (Video)")

    // Mode Toggle (Image / Video) like Google Flow
    val selectedMode = MutableStateFlow("Video") // "Video" or "Image"

    // Video Studio Form States
    val videoPrompt = MutableStateFlow("A cinematic wide shot of @img_hero moving through rainy neon streets @img_bg with reflections on pavement")
    val selectedAspectRatio = MutableStateFlow("16:9")
    val selectedResolution = MutableStateFlow("1080p")
    val selectedStyle = MutableStateFlow("Cinematic")
    val selectedDuration = MutableStateFlow(5) // 5s or 10s
    val batchCount = MutableStateFlow(1) // Default 1x parallel request
    val isEnhancingPrompt = MutableStateFlow(false)

    // Multi-API Key Pool
    val apiKeyPool: StateFlow<List<ApiKeyItem>> = secureStorage.apiKeyPoolFlow

    // Voice Studio Form States
    val voiceTitle = MutableStateFlow("Scene 1 Voiceover")
    val voiceScript = MutableStateFlow("In the neon-drenched sprawl of the future city, shadows hold more truth than light. Prepare for the journey ahead.")
    val selectedVoice = MutableStateFlow("Puck (Studio Male)")
    val voiceSpeed = MutableStateFlow(1.0f)
    val voicePitch = MutableStateFlow(0.0f)

    // Settings States
    val videoApiKeyInput = MutableStateFlow(secureStorage.getVideoApiKey())
    val voiceApiKeyInput = MutableStateFlow(secureStorage.getVoiceApiKey())
    val concurrencyLimitInput = MutableStateFlow(secureStorage.getConcurrencyLimit())
    val apiKeyTestStatus = MutableStateFlow<String?>(null)
    val isTestingKey = MutableStateFlow(false)

    // Reactive Tasks & Logs
    val allVideoTasks: StateFlow<List<VideoTaskEntity>> = generationRepo.allVideoTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeVideoTasks: StateFlow<List<VideoTaskEntity>> = generationRepo.activeVideoTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val failedVideoTasks: StateFlow<List<VideoTaskEntity>> = generationRepo.failedVideoTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVoiceTasks: StateFlow<List<VoiceTaskEntity>> = generationRepo.allVoiceTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<GenerationLogEntity>> = generationRepo.allLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val failedLogs: StateFlow<List<GenerationLogEntity>> = generationRepo.failedLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently playing preview video path for in-app VideoPlayerDialog
    val previewVideo = MutableStateFlow<VideoTaskEntity?>(null)

    init {
        // Observe projects to set default active project
        viewModelScope.launch {
            allProjects.collectLatest { projects ->
                if (projects.isNotEmpty() && activeProject.value == null) {
                    val savedId = secureStorage.getActiveProjectId()
                    val target = projects.find { it.id == savedId } ?: projects.first()
                    selectProject(target)
                }
            }
        }

        // Monitor failed tasks to surface descriptive error dialogs as requested
        viewModelScope.launch {
            failedLogs.collectLatest { failedItems ->
                val recentFail = failedItems.firstOrNull()
                if (recentFail != null && System.currentTimeMillis() - recentFail.timestamp < 3000) {
                    _uiEvents.emit(
                        UiEvent.ShowErrorDialog(
                            title = "Generation Failure (${recentFail.itemLabel})",
                            message = recentFail.message,
                            canRetry = true,
                            projectId = recentFail.projectId
                        )
                    )
                }
            }
        }
    }

    fun selectProject(project: ProjectEntity) {
        activeProject.value = project
        selectedAspectRatio.value = project.aspectRatio
        selectedResolution.value = project.resolution
        selectedStyle.value = project.defaultStyle
        secureStorage.setActiveProjectId(project.id)

        // Load pre-uploaded assets for this project
        viewModelScope.launch {
            projectRepo.getAssetsForProjectFlow(project.id).collectLatest { assets ->
                _projectAssets.value = assets
            }
        }
    }

    fun createProject(name: String, aspectRatio: String, resolution: String, style: String, description: String) {
        viewModelScope.launch {
            val newProject = projectRepo.createProject(name, aspectRatio, resolution, style, description)
            selectProject(newProject)
            _uiEvents.emit(UiEvent.ShowToast("Project '${newProject.name}' created!"))
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectRepo.deleteProject(project.id)
            if (activeProject.value?.id == project.id) {
                activeProject.value = null
            }
            if (activeSubProject.value?.id == project.id) {
                activeSubProject.value = null
            }
            _uiEvents.emit(UiEvent.ShowToast("Project deleted."))
        }
    }

    fun addAssetFromUri(tag: String, uri: Uri) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            try {
                projectRepo.saveProjectAssetFromUri(project.id, tag, uri)
                _uiEvents.emit(UiEvent.ShowToast("Asset @$tag added!"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Failed to add asset: ${e.message}"))
            }
        }
    }

    fun addSyntheticAsset(tag: String, label: String, color: Int) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            projectRepo.createSyntheticAsset(project.id, tag, label, color)
            _uiEvents.emit(UiEvent.ShowToast("Sample asset @$tag created!"))
        }
    }

    fun deleteAsset(asset: ProjectAssetEntity) {
        viewModelScope.launch {
            projectRepo.deleteAsset(asset)
            _uiEvents.emit(UiEvent.ShowToast("Asset deleted."))
        }
    }

    fun attachAsset(asset: ProjectAssetEntity) {
        val current = attachedAssets.value.toMutableList()
        if (current.none { it.id == asset.id }) {
            current.add(asset)
            attachedAssets.value = current
            insertTagIntoPrompt(asset.tag)
        }
    }

    fun detachAsset(asset: ProjectAssetEntity) {
        val current = attachedAssets.value.filter { it.id != asset.id }
        attachedAssets.value = current
        videoPrompt.value = videoPrompt.value.replace("@${asset.tag}", "").trim()
    }

    fun updateActiveProjectSettings(aspectRatio: String? = null, resolution: String? = null, style: String? = null) {
        aspectRatio?.let { selectedAspectRatio.value = it }
        resolution?.let { selectedResolution.value = it }
        style?.let { selectedStyle.value = it }

        val currentProj = activeProject.value ?: return
        val updated = currentProj.copy(
            aspectRatio = aspectRatio ?: currentProj.aspectRatio,
            resolution = resolution ?: currentProj.resolution,
            defaultStyle = style ?: currentProj.defaultStyle,
            updatedAt = System.currentTimeMillis()
        )
        activeProject.value = updated
        viewModelScope.launch {
            projectRepo.updateProject(updated)
        }
    }

    fun insertTagIntoPrompt(tag: String) {
        val current = videoPrompt.value
        val tagRef = "@$tag"
        if (!current.contains(tagRef)) {
            videoPrompt.value = if (current.isBlank()) tagRef else "$current $tagRef"
        }
    }

    fun enhancePrompt() {
        val current = videoPrompt.value
        if (current.isBlank()) return
        viewModelScope.launch {
            isEnhancingPrompt.value = true
            try {
                val enhanced = generationRepo.enhancePrompt(
                    current,
                    selectedStyle.value,
                    selectedAspectRatio.value
                )
                videoPrompt.value = enhanced
                _uiEvents.emit(UiEvent.ShowToast("Prompt enhanced with AI cinematography!"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Prompt enhancement: ${e.message}"))
            } finally {
                isEnhancingPrompt.value = false
            }
        }
    }

    /**
     * Triggers up to 15 concurrent parallel video generation requests
     */
    fun triggerVideoGeneration() {
        val project = activeProject.value ?: run {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("Please select or create a project first.")) }
            return
        }
        val prompt = videoPrompt.value.trim()
        if (prompt.isBlank()) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("Please enter a video prompt.")) }
            return
        }

        val referencedTags = _projectAssets.value
            .filter { prompt.contains("@${it.tag}") }
            .joinToString(",") { it.tag }

        viewModelScope.launch {
            val count = batchCount.value.coerceIn(1, 15)
            generationRepo.enqueueVideoBatch(
                projectId = project.id,
                projectName = project.name,
                prompt = prompt,
                aspectRatio = selectedAspectRatio.value,
                resolution = selectedResolution.value,
                style = selectedStyle.value,
                durationSeconds = selectedDuration.value,
                batchCount = count,
                referencedAssetTags = referencedTags
            )
            _uiEvents.emit(UiEvent.ShowToast("Enqueued $count video(s) for background generation!"))
        }
    }

    fun triggerVoiceGeneration() {
        val script = voiceScript.value.trim()
        if (script.isBlank()) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("Please enter a voice script.")) }
            return
        }

        viewModelScope.launch {
            val project = activeProject.value
            generationRepo.enqueueVoiceTask(
                projectId = project?.id,
                projectName = project?.name,
                title = voiceTitle.value.ifBlank { "Voice Clip" },
                script = script,
                voiceName = selectedVoice.value,
                speed = voiceSpeed.value,
                pitch = voicePitch.value
            )
            _uiEvents.emit(UiEvent.ShowToast("Voice generation started in background!"))
        }
    }

    fun retryFailedVideos(projectId: String?) {
        val pid = projectId ?: activeProject.value?.id ?: return
        viewModelScope.launch {
            generationRepo.retryFailedVideos(pid)
            _uiEvents.emit(UiEvent.ShowToast("Retrying failed video tasks from failure point..."))
        }
    }

    fun cancelAllTasks() {
        viewModelScope.launch {
            generationRepo.cancelAllActiveTasks()
            _uiEvents.emit(UiEvent.ShowToast("All active tasks cancelled."))
        }
    }

    fun deleteVideoTask(task: VideoTaskEntity) {
        viewModelScope.launch {
            generationRepo.deleteVideoTask(task)
            _uiEvents.emit(UiEvent.ShowToast("Video removed."))
        }
    }

    fun deleteVoiceTask(task: VoiceTaskEntity) {
        viewModelScope.launch {
            generationRepo.deleteVoiceTask(task)
            _uiEvents.emit(UiEvent.ShowToast("Voice clip removed."))
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            generationRepo.clearLogs()
            _uiEvents.emit(UiEvent.ShowToast("Status logs cleared."))
        }
    }

    fun saveSettings() {
        val videoKey = videoApiKeyInput.value.trim()
        val voiceKey = voiceApiKeyInput.value.trim()
        val concurrency = concurrencyLimitInput.value.coerceIn(1, 15)

        secureStorage.setVideoApiKey(videoKey)
        secureStorage.setVoiceApiKey(voiceKey)
        secureStorage.setConcurrencyLimit(concurrency)

        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Settings and API keys saved securely!"))
        }
    }

    fun testVideoApiKey() {
        val key = videoApiKeyInput.value.trim()
        if (key.isBlank()) {
            apiKeyTestStatus.value = "Please enter an API key to test."
            return
        }

        viewModelScope.launch {
            isTestingKey.value = true
            apiKeyTestStatus.value = "Validating key with Google AI Studio..."
            when (val res = generationRepo.testApiKey(key)) {
                is ApiResult.Success -> {
                    apiKeyTestStatus.value = "Success: API key is active and authorized!"
                }
                is ApiResult.Error -> {
                    apiKeyTestStatus.value = res.message
                }
            }
            isTestingKey.value = false
        }
    }

    fun addApiKeyToPool(key: String, label: String) {
        if (key.isBlank()) return
        secureStorage.addApiKeyToPool(key, label)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("API key added to rotation pool!"))
        }
    }

    fun removeApiKeyFromPool(id: String) {
        secureStorage.removeApiKeyFromPool(id)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("API key removed from pool."))
        }
    }

    fun resetExhaustedKeys() {
        secureStorage.resetExhaustedKeys()
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("All keys reset to READY state!"))
        }
    }

    fun testSpecificApiKey(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val res = generationRepo.testApiKey(key)) {
                is ApiResult.Success -> onResult(true, "Key is active and authorized!")
                is ApiResult.Error -> onResult(false, res.message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
