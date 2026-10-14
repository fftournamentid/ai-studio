package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.StudioPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, aspectRatio: String, resolution: String, style: String, description: String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedStyle by remember { mutableStateOf("Cinematic") }
    var description by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    val aspectRatios = listOf("16:9", "9:16", "1:1", "4:3", "21:9")
    val resolutions = listOf("720p", "1080p", "4K")
    val styles = listOf("Cinematic", "Anime", "Cyberpunk", "Photorealistic", "3D Render", "Fantasy", "Documentary")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New Project",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Project Name Input
                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        if (it.isNotBlank()) hasError = false
                    },
                    label = { Text("Project Name *") },
                    placeholder = { Text("e.g. Sci-Fi Cyberpunk Trailer") },
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text("Project name is required") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Aspect Ratio Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = null,
                        tint = StudioPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aspect Ratio",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    aspectRatios.forEach { ratio ->
                        FilterChip(
                            selected = selectedAspectRatio == ratio,
                            onClick = { selectedAspectRatio = ratio },
                            label = { Text(ratio) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = StudioPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quality / Resolution Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = StudioPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quality / Resolution",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    resolutions.forEach { res ->
                        FilterChip(
                            selected = selectedResolution == res,
                            onClick = { selectedResolution = res },
                            label = { Text(res) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = StudioPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Default Style
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = StudioPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Visual Aesthetic",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    styles.forEach { style ->
                        FilterChip(
                            selected = selectedStyle == style,
                            onClick = { selectedStyle = style },
                            label = { Text(style) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Scene notes, visual references, characters...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (projectName.isBlank()) {
                                hasError = true
                            } else {
                                onCreate(
                                    projectName.trim(),
                                    selectedAspectRatio,
                                    selectedResolution,
                                    selectedStyle,
                                    description.trim()
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("submit_create_project_button")
                    ) {
                        Text("Create Project")
                    }
                }
            }
        }
    }
}
