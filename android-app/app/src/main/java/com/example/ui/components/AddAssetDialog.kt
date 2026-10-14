package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary

@Composable
fun AddAssetDialog(
    onDismiss: () -> Unit,
    onAddAssetFromUri: (tag: String, uri: Uri) -> Unit,
    onAddSyntheticAsset: (tag: String, label: String, color: Int) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var hasError by remember { mutableStateOf(false) }

    // Android Zero-Permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            if (tagInput.isBlank()) {
                tagInput = "img_asset"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upload Project Image Asset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Upload once to this project. Reference in prompts using @tag (e.g. @img_hero, @img_bg) without re-uploading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tag Name
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = {
                        tagInput = it.replace(" ", "_").lowercase()
                        if (it.isNotBlank()) hasError = false
                    },
                    label = { Text("Asset Reference Tag *") },
                    placeholder = { Text("e.g. img_hero, img_bg, img_logo") },
                    leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = StudioPrimary) },
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text("Tag name is required") }
                    } else {
                        { Text("Used to refer to this image inside prompts") }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("asset_tag_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Photo Picker Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Pick Image",
                            tint = StudioPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (selectedUri != null) "Image Selected" else "Select from Device Gallery",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (selectedUri != null) "Ready to save to project" else "Tap to choose a reference photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider()

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Or create studio sample asset:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val tag = if (tagInput.isNotBlank()) tagInput else "img_character"
                            onAddSyntheticAsset(tag, "Hero Character Reference", android.graphics.Color.parseColor("#4338CA"))
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hero Card", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            val tag = if (tagInput.isNotBlank()) tagInput else "img_environment"
                            onAddSyntheticAsset(tag, "Cinematic City Environment", android.graphics.Color.parseColor("#0F172A"))
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Environment", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dialog Buttons
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
                            if (tagInput.isBlank()) {
                                hasError = true
                                return@Button
                            }
                            selectedUri?.let { uri ->
                                onAddAssetFromUri(tagInput.trim(), uri)
                                onDismiss()
                            } ?: run {
                                // Default synthetic
                                onAddSyntheticAsset(tagInput.trim(), tagInput.replace("_", " ").uppercase(), android.graphics.Color.parseColor("#2563EB"))
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("submit_add_asset_button")
                    ) {
                        Text("Save Asset")
                    }
                }
            }
        }
    }
}
