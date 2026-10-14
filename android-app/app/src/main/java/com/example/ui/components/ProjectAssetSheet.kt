package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProjectAssetEntity
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSuccess
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectAssetSheet(
    projectName: String,
    assets: List<ProjectAssetEntity>,
    attachedAssets: List<ProjectAssetEntity>,
    onAttachAsset: (ProjectAssetEntity) -> Unit,
    onDetachAsset: (ProjectAssetEntity) -> Unit,
    onUploadNewAsset: (tag: String, uri: Uri) -> Unit,
    onAddSampleAsset: (tag: String, label: String, color: Int) -> Unit,
    onDeleteAsset: (ProjectAssetEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isUploadingMode by remember { mutableStateOf(false) }
    var assetNameInput by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var inputError by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            if (assetNameInput.isBlank()) {
                assetNameInput = "image_${assets.size + 1}"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Project Image References",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Project: $projectName • ${assets.size} image(s) saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isUploadingMode) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to attach/detach from prompt:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = { isUploadingMode = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("upload_image_sheet_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Image", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (assets.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No images in this project yet", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Upload reference images once and use them across all video prompts.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { isUploadingMode = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Upload First Image")
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        items(assets) { asset ->
                            val isAttached = attachedAssets.any { it.id == asset.id }
                            AssetGridTile(
                                asset = asset,
                                isAttached = isAttached,
                                onToggle = {
                                    if (isAttached) onDetachAsset(asset) else onAttachAsset(asset)
                                },
                                onDelete = { onDeleteAsset(asset) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }

            } else {
                // Upload Form
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Upload Image to Project",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Assign a name (e.g. hero, car, background) so you can reference it anywhere.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = assetNameInput,
                            onValueChange = {
                                assetNameInput = it.replace(" ", "_").lowercase()
                                if (it.isNotBlank()) inputError = false
                            },
                            label = { Text("Image Name / Tag *") },
                            placeholder = { Text("e.g. hero, car, background") },
                            leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = StudioPrimary) },
                            isError = inputError,
                            supportingText = if (inputError) {
                                { Text("Image name is required") }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Choose from Gallery
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (selectedPhotoUri != null) "Photo Selected" else "Pick from Phone Gallery",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (selectedPhotoUri != null) "Ready to save" else "Select any photo from device",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { isUploadingMode = false },
                                colors = ButtonDefaults.outlinedButtonColors(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Back")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (assetNameInput.isBlank()) {
                                        inputError = true
                                        return@Button
                                    }
                                    selectedPhotoUri?.let { uri ->
                                        onUploadNewAsset(assetNameInput.trim(), uri)
                                    } ?: run {
                                        onAddSampleAsset(assetNameInput.trim(), assetNameInput.uppercase(), android.graphics.Color.parseColor("#4338CA"))
                                    }
                                    isUploadingMode = false
                                    assetNameInput = ""
                                    selectedPhotoUri = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save & Add to Project")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AssetGridTile(
    asset: ProjectAssetEntity,
    isAttached: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (isAttached) androidx.compose.foundation.BorderStroke(2.5.dp, StudioSuccess) else null,
        modifier = Modifier
            .size(105.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() }
    ) {
        Box {
            val file = File(asset.filePath)
            if (file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = asset.tag,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StudioPrimary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "@${asset.tag}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Attached check badge
            if (isAttached) {
                Surface(
                    shape = CircleShape,
                    color = StudioSuccess,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.TopStart)
                        .padding(3.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = "Attached", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Delete icon
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                )
            }

            // Name label at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "@${asset.tag}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
