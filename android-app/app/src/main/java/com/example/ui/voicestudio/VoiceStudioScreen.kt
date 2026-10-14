package com.example.ui.voicestudio

import android.content.Intent
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VoiceTaskEntity
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioTertiary
import com.example.ui.viewmodel.StudioViewModel
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceStudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val title by viewModel.voiceTitle.collectAsStateWithLifecycle()
    val script by viewModel.voiceScript.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val speed by viewModel.voiceSpeed.collectAsStateWithLifecycle()
    val pitch by viewModel.voicePitch.collectAsStateWithLifecycle()
    val voiceTasks by viewModel.allVoiceTasks.collectAsStateWithLifecycle()

    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsStateWithLifecycle()
    val currentPath by viewModel.audioPlayer.currentPlayingPath.collectAsStateWithLifecycle()
    val currentPosMs by viewModel.audioPlayer.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.audioPlayer.durationMs.collectAsStateWithLifecycle()

    val voices = listOf(
        "Puck (Studio Male)",
        "Charon (Deep Narrator)",
        "Kore (Calm Female)",
        "Aoede (Melodic Storyteller)",
        "Fenrir (Intense Action)"
    )

    val wordCount = script.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    val estDurationSec = (wordCount * 0.45f + 1.5f) / speed.coerceAtLeast(0.5f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Voice Synthesis Studio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Convert scripts into expressive speech clips with pitch, cadence, and voice character control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.voiceTitle.value = it },
                label = { Text("Voice Clip Title") },
                placeholder = { Text("e.g. Chapter 1 Narration") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Script Input
            OutlinedTextField(
                value = script,
                onValueChange = { viewModel.voiceScript.value = it },
                label = { Text("Dialogue / Voiceover Script") },
                placeholder = { Text("Type the narration or speech dialogue...") },
                minLines = 4,
                maxLines = 6,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_script_input")
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$wordCount words • Est. duration: ${String.format("%.1f", estDurationSec)}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Voice Character Picker
            Text(
                text = "Voice Character Profile",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                voices.forEach { voice ->
                    FilterChip(
                        selected = selectedVoice == voice,
                        onClick = { viewModel.selectedVoice.value = voice },
                        label = { Text(voice, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sliders for Speed and Pitch
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Playback Speed", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = "${String.format("%.1f", speed)}x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StudioPrimary
                        )
                    }
                    Slider(
                        value = speed,
                        onValueChange = { viewModel.voiceSpeed.value = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioPrimary,
                            activeTrackColor = StudioPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pitch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = StudioSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pitch Cadence", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = if (pitch > 0) "+${String.format("%.1f", pitch)}" else String.format("%.1f", pitch),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StudioSecondary
                        )
                    }
                    Slider(
                        value = pitch,
                        onValueChange = { viewModel.voicePitch.value = it },
                        valueRange = -5f..5f,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioSecondary,
                            activeTrackColor = StudioSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Generate Button
            Button(
                onClick = { viewModel.triggerVoiceGeneration() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(StudioSecondary, StudioPrimary)),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("generate_voice_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Generate Voice Clip",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Library Section
            Text(
                text = "Generated Voice Clips (${voiceTasks.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (voiceTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No voice clips yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Generated clips will be saved locally and appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(voiceTasks) { voiceTask ->
                val isThisPlaying = isPlaying && currentPath == voiceTask.outputPath
                VoiceClipItem(
                    task = voiceTask,
                    isPlaying = isThisPlaying,
                    currentPosMs = if (isThisPlaying) currentPosMs else 0,
                    durationMs = if (isThisPlaying) durationMs else (voiceTask.durationSeconds * 1000).toInt(),
                    onPlayPause = {
                        voiceTask.outputPath?.let { path ->
                            viewModel.audioPlayer.togglePlayPause(path)
                        }
                    },
                    onDelete = { viewModel.deleteVoiceTask(voiceTask) }
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
private fun VoiceClipItem(
    task: VoiceTaskEntity,
    isPlaying: Boolean,
    currentPosMs: Int,
    durationMs: Int,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val progress = if (durationMs > 0) (currentPosMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Circle Button
                Surface(
                    shape = CircleShape,
                    color = if (isPlaying) StudioSecondary else StudioPrimary,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { onPlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${task.voiceName} • ${String.format("%.1f", task.durationSeconds)}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Share Button
                IconButton(
                    onClick = {
                        task.outputPath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/wav"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Voice Clip"))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                }

                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Playback progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = StudioSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "\"${task.script}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
