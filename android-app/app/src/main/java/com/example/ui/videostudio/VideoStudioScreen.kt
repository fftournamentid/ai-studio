package com.example.ui.videostudio

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProjectEntity
import com.example.data.model.VideoTaskEntity
import com.example.ui.components.CreateProjectDialog
import com.example.ui.components.ProjectAssetSheet
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioTertiary
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoStudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val isStudioOpen by viewModel.isProjectStudioOpen.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val currentProject = activeProject
    val projectAssets by viewModel.projectAssets.collectAsStateWithLifecycle()
    val attachedAssets by viewModel.attachedAssets.collectAsStateWithLifecycle()
    val prompt by viewModel.videoPrompt.collectAsStateWithLifecycle()
    val selectedRatio by viewModel.selectedAspectRatio.collectAsStateWithLifecycle()
    val selectedRes by viewModel.selectedResolution.collectAsStateWithLifecycle()
    val selectedStyle by viewModel.selectedStyle.collectAsStateWithLifecycle()
    val selectedDuration by viewModel.selectedDuration.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedAiModel.collectAsStateWithLifecycle()
    val batchCount by viewModel.batchCount.collectAsStateWithLifecycle()
    val isEnhancing by viewModel.isEnhancingPrompt.collectAsStateWithLifecycle()
    val allVideos by viewModel.allVideoTasks.collectAsStateWithLifecycle()
    val previewVideo by viewModel.previewVideo.collectAsStateWithLifecycle()

    var showAssetSheet by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showPatternSettings by remember { mutableStateOf(false) }
    var showBatchDropdown by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }

    val projectVideos = remember(allVideos, currentProject) {
        if (currentProject != null) allVideos.filter { it.projectId == currentProject.id } else emptyList()
    }

    val totalCredits = batchCount * 15

    val aiModels = listOf(
        "Google Veo-2 (AI Video)",
        "Gemini 1.5 Flash (Fast)",
        "Gemini 1.5 Pro (Quality)",
        "Imagen 3 Video (Animation)"
    )

    if (!isStudioOpen || currentProject == null) {
        // ==========================================
        // 1. PROJECT HUB / SELECTION OVERVIEW
        // ==========================================
        ProjectHubOverview(
            allProjects = allProjects,
            onSelectProject = { proj ->
                viewModel.selectProject(proj)
                viewModel.isProjectStudioOpen.value = true
            },
            onCreateNewProject = { showCreateProjectDialog = true },
            modifier = modifier
        )
    } else {
        // ==========================================
        // 2. IMMERSIVE PROJECT STUDIO WORKSPACE
        // (Bottom navigation hidden, prompt docked at bottom,
        //  top back button, pattern settings, and top image button)
        // ==========================================
        BackHandler {
            viewModel.isProjectStudioOpen.value = false
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ---------------------------------------------
                // TOP BAR: Back Arrow + Pattern HUD + Add Image
                // ---------------------------------------------
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Back Arrow Button to exit project
                            IconButton(
                                onClick = { viewModel.isProjectStudioOpen.value = false },
                                modifier = Modifier.testTag("exit_project_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Exit Project",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Center: Pattern HUD Pill (Inchi / Checkered pattern toggle)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (showPatternSettings) StudioPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (showPatternSettings) StudioPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showPatternSettings = !showPatternSettings }
                                    .testTag("pattern_hud_pill")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    // Ruler / Pattern icon
                                    Icon(
                                        imageVector = Icons.Default.Straighten,
                                        contentDescription = "Project Settings Pattern",
                                        tint = StudioPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Compact parameters overview
                                    Text(
                                        text = "⚡ $totalCredits cr",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioTertiary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "•", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedRatio,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "•", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${selectedDuration}s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "•", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedModel.split(" ").firstOrNull() ?: "Veo",
                                        fontSize = 11.sp,
                                        color = StudioSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Right: Top "+ Image" Button for project assets
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = StudioPrimary.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showAssetSheet = true }
                                    .testTag("top_add_image_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = StudioPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ Image (${projectAssets.size})",
                                        color = StudioPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // ---------------------------------------------
                        // PATTERN POPUP / EXPANDED SETTINGS DRAWER
                        // Revealed when tapping the pattern pill!
                        // ---------------------------------------------
                        AnimatedVisibility(
                            visible = showPatternSettings,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Model & Credit cost row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Model Picker
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioSecondary),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { showModelDropdown = true }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Tune, contentDescription = null, tint = StudioSecondary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = selectedModel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StudioSecondary
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = showModelDropdown,
                                            onDismissRequest = { showModelDropdown = false }
                                        ) {
                                            aiModels.forEach { model ->
                                                DropdownMenuItem(
                                                    text = { Text(model, fontSize = 12.sp) },
                                                    onClick = {
                                                        viewModel.selectedAiModel.value = model
                                                        showModelDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Uses $totalCredits credits (${batchCount}x parallel)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioTertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Aspect Ratio Row: 16:9, 9:16, 1:1, 4:3, 21:9
                                Text("Aspect Ratio (Tap to change in project)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("16:9", "9:16", "1:1", "4:3", "21:9").forEach { ratio ->
                                        FilterChip(
                                            selected = selectedRatio == ratio,
                                            onClick = { viewModel.updateActiveProjectSettings(aspectRatio = ratio) },
                                            label = { Text(ratio, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = StudioPrimary.copy(alpha = 0.2f),
                                                selectedLabelColor = StudioPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Duration & Quality Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Duration", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(5, 10).forEach { dur ->
                                                FilterChip(
                                                    selected = selectedDuration == dur,
                                                    onClick = { viewModel.selectedDuration.value = dur },
                                                    label = { Text("${dur}s", fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Resolution", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf("720p", "1080p", "4K").forEach { res ->
                                                FilterChip(
                                                    selected = selectedRes == res,
                                                    onClick = { viewModel.updateActiveProjectSettings(resolution = res) },
                                                    label = { Text(res, fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // AI Prompt Enhancer Action
                                OutlinedButton(
                                    onClick = { viewModel.enhancePrompt() },
                                    enabled = !isEnhancing,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isEnhancing) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioTertiary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("Enhance Prompt with AI Director", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // ---------------------------------------------
                // CENTER: Video Timeline & Output Gallery
                // ---------------------------------------------
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (projectVideos.isEmpty()) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp, bottom = 20.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = StudioPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = StudioPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Ready to generate videos for \"${currentProject.name}\"",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add character images with the top '+ Image' or bottom '+' button.\nType your prompt below and tap -> to generate sequentially.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (projectAssets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Saved Project Characters (Tap to reference):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        projectAssets.forEach { asset ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.attachAsset(asset) }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.Image, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("@${asset.tag}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "Generated Videos (${projectVideos.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(projectVideos) { video ->
                            ProjectStudioVideoCard(
                                video = video,
                                onPlay = { viewModel.previewVideo.value = video },
                                onRetry = { viewModel.retryFailedVideos(currentProject.id) },
                                onDelete = { viewModel.deleteVideoTask(video) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Spacer at bottom so content doesn't get covered by docked prompt bar
                    item {
                        Spacer(modifier = Modifier.height(130.dp))
                    }
                }
            }

            // -------------------------------------------------------------
            // DOCKED BOTTOM PROMPT BAR (Anchored at the very bottom of screen)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Character Reference Chips above the input
                if (projectAssets.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        items(projectAssets) { asset ->
                            val isAttached = attachedAssets.any { it.id == asset.id }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAttached) StudioPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isAttached) StudioPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isAttached) {
                                            viewModel.detachAsset(asset)
                                        } else {
                                            viewModel.attachAsset(asset)
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = if (isAttached) StudioPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "@${asset.tag}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isAttached) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAttached) StudioPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isAttached) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = StudioPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioPrimary.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAssetSheet = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Add Image", fontSize = 11.sp, color = StudioPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Docked Bottom Input Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left "+" button to attach/upload image
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { showAssetSheet = true }
                                .testTag("docked_add_asset_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Image",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Text Field
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { viewModel.videoPrompt.value = it },
                            placeholder = {
                                Text(
                                    text = "What do you want to make?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("docked_prompt_input")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Variations Pill: "Video x1"
                        Box {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showBatchDropdown = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("x$batchCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
                                }
                            }

                            DropdownMenu(
                                expanded = showBatchDropdown,
                                onDismissRequest = { showBatchDropdown = false }
                            ) {
                                listOf(1, 2, 3, 5, 10, 15).forEach { count ->
                                    DropdownMenuItem(
                                        text = { Text("$count Video${if (count > 1) "s" else ""}") },
                                        onClick = {
                                            viewModel.batchCount.value = count
                                            showBatchDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Arrow Send Button "->"
                        Surface(
                            shape = CircleShape,
                            color = StudioPrimary,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.triggerVideoGeneration() }
                                .testTag("docked_generate_arrow")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Generate",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Asset Sheet
    if (showAssetSheet && currentProject != null) {
        ProjectAssetSheet(
            projectName = currentProject.name,
            assets = projectAssets,
            attachedAssets = attachedAssets,
            onAttachAsset = { viewModel.attachAsset(it) },
            onDetachAsset = { viewModel.detachAsset(it) },
            onUploadNewAsset = { tag, uri -> viewModel.addAssetFromUri(tag, uri) },
            onAddSampleAsset = { tag, label, color -> viewModel.addSyntheticAsset(tag, label, color) },
            onDeleteAsset = { viewModel.deleteAsset(it) },
            onDismiss = { showAssetSheet = false }
        )
    }

    // Modal Create Project Dialog
    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onCreate = { name, ratio, res, style, desc ->
                viewModel.createProject(name, ratio, res, style, desc)
                viewModel.isProjectStudioOpen.value = true
            }
        )
    }

    // Modal Video Preview Player
    previewVideo?.let { video ->
        VideoPlayerDialog(
            video = video,
            onDismiss = { viewModel.previewVideo.value = null }
        )
    }
}

@Composable
private fun ProjectHubOverview(
    allProjects: List<ProjectEntity>,
    onSelectProject: (ProjectEntity) -> Unit,
    onCreateNewProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
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
                        text = "Video Projects Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select a project to enter its dedicated creation workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onCreateNewProject,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Project", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (allProjects.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No projects yet", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Create your first project to start generating sequential AI videos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(allProjects) { project ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProject(project) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
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
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${project.aspectRatio} • ${project.resolution} • ${project.defaultStyle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { onSelectProject(project) },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open Studio", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ProjectStudioVideoCard(
    video: VideoTaskEntity,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            video.isCompleted -> StudioPrimary
                            video.isFailed -> StudioError
                            else -> StudioSecondary
                        }
                    ) {
                        Text(
                            text = "#${video.sequenceLabel}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = video.outputFileName ?: "Video_${video.sequenceLabel}.mp4",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (video.isRunning || video.isQueued) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { video.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = StudioSecondary
                )
            } else if (video.isFailed) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = video.errorMessage ?: "Failed due to quota exhaustion or network error.",
                    color = StudioError,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"${video.prompt}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (video.isCompleted) {
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", fontSize = 11.sp)
                    }
                } else if (video.isFailed) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioError),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", fontSize = 11.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
