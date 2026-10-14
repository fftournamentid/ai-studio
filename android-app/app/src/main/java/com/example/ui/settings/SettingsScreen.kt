package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ApiKeyItem
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioTertiary
import com.example.ui.viewmodel.StudioViewModel
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apiKeyPool by viewModel.apiKeyPool.collectAsStateWithLifecycle()
    val voiceKey by viewModel.voiceApiKeyInput.collectAsStateWithLifecycle()
    val concurrency by viewModel.concurrencyLimitInput.collectAsStateWithLifecycle()
    val failedLogs by viewModel.failedLogs.collectAsStateWithLifecycle()

    var newKeyInput by remember { mutableStateOf("") }
    var newKeyLabel by remember { mutableStateOf("") }
    var showNewKey by remember { mutableStateOf(false) }
    var showVoiceKey by remember { mutableStateOf(false) }

    // Test status per key (keyed by item id)
    var keyTestResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var testingKeyId by remember { mutableStateOf<String?>(null) }

    // Calculate downloads size
    val downloadsDir = File(context.filesDir, "downloads")
    val totalDownloadBytes = remember(downloadsDir) {
        if (downloadsDir.exists()) {
            downloadsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }
    val downloadSizeMb = String.format("%.2f", totalDownloadBytes / (1024.0 * 1024.0))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "API Key Pool & Studio Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add multiple Google AI Studio keys. If one runs out of quota, generation automatically switches to the next key.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Multi-API Key Failover Explanation Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSecondary.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = StudioSecondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Automatic Multi-Key Failover",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioSecondary
                    )
                    Text(
                        text = "You can add 2, 3, or more API keys. If your active key hits a quota limit (HTTP 429), the app automatically rotates to your backup keys seamlessly without interrupting tasks!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: API Key Pool
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Google AI Studio Key Pool (${apiKeyPool.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (apiKeyPool.any { it.isExhausted }) {
                OutlinedButton(
                    onClick = { viewModel.resetExhaustedKeys() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset All to Ready", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // List of configured keys in pool
        if (apiKeyPool.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "No API keys added yet.",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Paste your Google AI Studio keys below to enable automatic rotation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            apiKeyPool.forEachIndexed { index, keyItem ->
                val isFirstActive = !keyItem.isExhausted && apiKeyPool.take(index).all { it.isExhausted }
                ApiKeyCardItem(
                    keyItem = keyItem,
                    isActiveInUse = isFirstActive,
                    testResult = keyTestResults[keyItem.id],
                    isTesting = testingKeyId == keyItem.id,
                    onTestKey = {
                        testingKeyId = keyItem.id
                        viewModel.testSpecificApiKey(keyItem.key) { success, msg ->
                            testingKeyId = null
                            keyTestResults = keyTestResults.toMutableMap().apply {
                                put(keyItem.id, if (success) "Active & Authorized" else msg)
                            }
                        }
                    },
                    onDeleteKey = { viewModel.removeApiKeyFromPool(keyItem.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card to Add New Key to Pool
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "+ Add Another API Key to Pool",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newKeyLabel,
                    onValueChange = { newKeyLabel = it },
                    label = { Text("Key Label / Nickname") },
                    placeholder = { Text("e.g. Key #${apiKeyPool.size + 1} (Backup)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newKeyInput,
                    onValueChange = { newKeyInput = it },
                    label = { Text("Google AI Studio API Key *") },
                    placeholder = { Text("Paste AIzaSy...") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = StudioPrimary) },
                    trailingIcon = {
                        IconButton(onClick = { showNewKey = !showNewKey }) {
                            Icon(
                                imageVector = if (showNewKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showNewKey) "Hide" else "Show"
                            )
                        }
                    },
                    visualTransformation = if (showNewKey) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_api_key_pool_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (newKeyInput.isNotBlank()) {
                            val label = if (newKeyLabel.isNotBlank()) newKeyLabel.trim() else "API Key #${apiKeyPool.size + 1}"
                            viewModel.addApiKeyToPool(newKeyInput.trim(), label)
                            newKeyInput = ""
                            newKeyLabel = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_key_to_pool_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add to Failover Pool", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // Optional Voice Synthesis Key
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = StudioSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Voice Synthesis API Key (Optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = voiceKey,
            onValueChange = { viewModel.voiceApiKeyInput.value = it },
            placeholder = { Text("TTS / Audio Synthesis API Key") },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = StudioSecondary) },
            trailingIcon = {
                IconButton(onClick = { showVoiceKey = !showVoiceKey }) {
                    Icon(
                        imageVector = if (showVoiceKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showVoiceKey) "Hide" else "Show"
                    )
                }
            },
            visualTransformation = if (showVoiceKey) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Parallel Concurrency Limit
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Max Parallel Concurrency Limit", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioPrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$concurrency Workers",
                            color = StudioPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Controls simultaneous background generation requests (1 to 15 workers).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = concurrency.toFloat(),
                    onValueChange = { viewModel.concurrencyLimitInput.value = it.toInt() },
                    valueRange = 1f..15f,
                    steps = 13,
                    colors = SliderDefaults.colors(thumbColor = StudioPrimary, activeTrackColor = StudioPrimary)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { viewModel.saveSettings() },
            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Save Voice Key & Concurrency", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // Error Tracking & Resume Point Status Log
        Text(
            text = "Error Tracking & Resume Point Log",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tracks exact failed video numbers (e.g. Video #004) so you can resume after adding or resetting keys.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (failedLogs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioSuccess, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "All systems operational. No failed video tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            failedLogs.take(5).forEach { log ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioError.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Video ${log.itemLabel} (${log.projectName})",
                                fontWeight = FontWeight.Bold,
                                color = StudioError,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.retryFailedVideos(log.projectId) },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Resume", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // Storage Management
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storage, contentDescription = null, tint = StudioSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Storage & Downloads", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Total downloaded media storage: $downloadSizeMb MB",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                if (downloadsDir.exists()) {
                    downloadsDir.listFiles()?.forEach { it.deleteRecursively() }
                }
            },
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear Downloads Cache")
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ApiKeyCardItem(
    keyItem: ApiKeyItem,
    isActiveInUse: Boolean,
    testResult: String?,
    isTesting: Boolean,
    onTestKey: () -> Unit,
    onDeleteKey: () -> Unit
) {
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
                Column {
                    Text(
                        text = keyItem.label,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = keyItem.maskedKey,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        keyItem.isExhausted -> StudioError.copy(alpha = 0.15f)
                        isActiveInUse -> StudioSuccess.copy(alpha = 0.15f)
                        else -> StudioPrimary.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = when {
                            keyItem.isExhausted -> "QUOTA EXHAUSTED"
                            isActiveInUse -> "IN USE (ACTIVE)"
                            else -> "READY (BACKUP)"
                        },
                        color = when {
                            keyItem.isExhausted -> StudioError
                            isActiveInUse -> StudioSuccess
                            else -> StudioPrimary
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            testResult?.let { result ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (result.contains("Active", ignoreCase = true)) StudioSuccess.copy(alpha = 0.1f) else StudioError.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result,
                        fontSize = 11.sp,
                        color = if (result.contains("Active", ignoreCase = true)) StudioSuccess else StudioError,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTestKey,
                    enabled = !isTesting,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Test Key", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDeleteKey,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Key",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
