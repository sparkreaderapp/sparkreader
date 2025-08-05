/*
 * Copyright 2025 The SparkReader Creator
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.sparkreader.ui.modelmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sparkreader.data.Model
import app.sparkreader.data.ModelDownloadStatusType
import app.sparkreader.data.Task
import app.sparkreader.ui.common.humanReadableSize

@Composable
fun SimpleModelItem(
  model: Model,
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  onModelClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by modelManagerViewModel.uiState.collectAsState()
  val downloadStatus = uiState.modelDownloadStatus[model.name]
  val isDownloaded = downloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
  val isDownloading = downloadStatus?.status == ModelDownloadStatusType.IN_PROGRESS
  val isConnecting = downloadStatus?.status == ModelDownloadStatusType.CONNECTING
  val isFailed = downloadStatus?.status == ModelDownloadStatusType.FAILED
  val isSelected = uiState.selectedModelName == model.name
  val uriHandler = LocalUriHandler.current
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clickable(enabled = isDownloaded && !isSelected) { 
        if (isDownloaded && !isSelected) {
          onModelClicked(model)
        }
      },
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Model name and link
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = model.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        
        // Model link icon if available
        if (model.modelLink.isNotEmpty()) {
          IconButton(
            onClick = { 
              uriHandler.openUri(model.modelLink)
            },
            modifier = Modifier
              .size(32.dp)
              .padding(start = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Language,
              contentDescription = "Open model link",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
      
      // Model size and action buttons row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = model.totalBytes.humanReadableSize(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          // Action button (download or delete)
          when {
            isDownloaded -> {
              IconButton(
                onClick = { 
                  showDeleteConfirmation = true
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete model",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            isConnecting -> {
              Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
            !isDownloading && !isFailed -> {
              IconButton(
                onClick = {
                  modelManagerViewModel.startModelDownload(task, model)
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = "Download model",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            isFailed -> {
              IconButton(
                onClick = {
                  modelManagerViewModel.startModelDownload(task, model)
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = "Retry download",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
        
        // Default tag or Set as default button
        if (isDownloaded) {
          if (isSelected) {
            // Default tag (filled)
            Box(
              modifier = Modifier
                .background(
                  color = MaterialTheme.colorScheme.primary,
                  shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
              Text(
                text = "Default",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Medium
              )
            }
          } else {
            // Set as default button (outlined)
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.primary,
                  shape = RoundedCornerShape(12.dp)
                )
                .clickable { 
                  modelManagerViewModel.setSelectedModel(model.name)
                }
                .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
              Text(
                text = "Set as default",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
      // Error message when download fails
      if (isFailed && !downloadStatus?.errorMessage.isNullOrEmpty()) {
        Text(
          text = "Error: ${downloadStatus?.errorMessage}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(top = 4.dp)
        )
      }

      // Progress bar when downloading or connecting
      if (isDownloading || isConnecting) {
        Column(
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            val progress = if (downloadStatus?.totalBytes ?: 0 > 0) {
              (downloadStatus?.receivedBytes ?: 0).toFloat() / downloadStatus!!.totalBytes.toFloat()
            } else {
              0f
            }
            
            Column(
              modifier = Modifier.weight(1f)
            ) {
              LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
              )
              
              if (downloadStatus?.totalBytes ?: 0 > 0) {
                Text(
                  text = "${(downloadStatus?.receivedBytes ?: 0).humanReadableSize()} / ${downloadStatus!!.totalBytes.humanReadableSize()}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
              
              // Helpful message for users during download
              Text(
                text = "Feel free to leave - we'll download in the background and send you a notification when complete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
              )
            }
            
            IconButton(
              onClick = { 
                modelManagerViewModel.cancelDownloadModel(task, model) 
              },
              modifier = Modifier.size(32.dp).padding(start = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Cancel download",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
  
  // Delete confirmation dialog
  if (showDeleteConfirmation) {
    ConfirmDeleteModelDialog(
      modelName = model.name,
      onConfirm = {
        modelManagerViewModel.deleteModel(task, model)
        showDeleteConfirmation = false
      },
      onDismiss = {
        showDeleteConfirmation = false
      }
    )
  }
}

@Composable
private fun ConfirmDeleteModelDialog(
  modelName: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Delete Model?") },
    text = {
      Text("Are you sure you want to delete \"$modelName\"? You can download it again later from this settings page.")
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error
        )
      ) {
        Text("Delete")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
