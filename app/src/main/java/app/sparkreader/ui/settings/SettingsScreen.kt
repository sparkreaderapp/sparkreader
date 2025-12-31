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

package app.sparkreader.ui.settings

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.sparkreader.proto.Theme
import app.sparkreader.ui.modelmanager.ModelManagerViewModel
import app.sparkreader.ui.theme.ThemeSettings
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.Box
import app.sparkreader.data.ModelDownloadStatusType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import app.sparkreader.data.TASK_LLM_ASK_IMAGE
import app.sparkreader.data.ModelDownloadStatus
import app.sparkreader.ui.common.humanReadableSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalUriHandler
import app.sparkreader.ui.modelmanager.ModelManagerUiState
import androidx.compose.foundation.background
import androidx.compose.foundation.border

private val THEME_OPTIONS = listOf(Theme.THEME_AUTO, Theme.THEME_LIGHT, Theme.THEME_DARK)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onNavigateBack: () -> Unit,
  onThemeSelectionClicked: () -> Unit,
  onImportBookClicked: () -> Unit,
  modifier: Modifier = Modifier,
  modelManagerViewModel: ModelManagerViewModel = hiltViewModel(),
  settingsViewModel: SettingsViewModel = hiltViewModel(),
  fromImportBook: Boolean = false,
) {
  var selectedTheme by remember { mutableStateOf(ThemeSettings.themeOverride.value) }
  val libraryState by settingsViewModel.libraryState.collectAsState()
  val modelsState by settingsViewModel.modelsState.collectAsState()
  var showDeleteConfirmation by remember { mutableStateOf(false) }
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val selectedModelName = modelManagerUiState.selectedModelName
  var showModelDeleteConfirmation by remember { mutableStateOf(false) }
  var modelToDelete by remember { mutableStateOf<app.sparkreader.data.Model?>(null) }
  var expandedModelManager by remember { mutableStateOf(false) }
  
  // Set up notification permission launcher
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    // Permission result is handled automatically by the system
  }
  
  // Set the launcher in the viewModel
  LaunchedEffect(notificationPermissionLauncher) {
    modelManagerViewModel.setNotificationPermissionLauncher(notificationPermissionLauncher)
  }
  
  // Monitor model manager loading state - check if any models are downloading
  LaunchedEffect(modelManagerUiState.modelDownloadStatus) {
    val isLoadingModels = modelManagerUiState.modelDownloadStatus.values.any { 
      it.status == ModelDownloadStatusType.CONNECTING 
    }
    settingsViewModel.setModelsLoading(isLoadingModels)
  }

  // Refresh library state when screen becomes visible
  LaunchedEffect(Unit) {
    settingsViewModel.refreshLibraryState()
  }
  
  // Handle auto-navigation back when download completes
  LaunchedEffect(libraryState.shouldNavigateBack) {
    if (libraryState.shouldNavigateBack) {
      settingsViewModel.clearNavigateBack()
      onNavigateBack()
    }
  }
  
  BackHandler(enabled = true) {
    onNavigateBack()
  }
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        }
      )
    },
    modifier = modifier
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Text(
          text = "General",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
          ),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(bottom = 2.dp)
        )
      }
      
      item {
        ModelManagerSection(
          modelManagerViewModel = modelManagerViewModel,
          modelManagerUiState = modelManagerUiState,
          selectedModelName = selectedModelName,
          expanded = expandedModelManager,
          onExpandedChange = { expandedModelManager = it },
          onModelDeleteRequested = { model ->
            modelToDelete = model
            showModelDeleteConfirmation = true
          },
          isLoadingModels = modelsState.isLoadingModels
        )
      }
      
      item {
        SparkReaderLibraryItem(
          libraryState = libraryState,
          onRefreshClick = { settingsViewModel.checkForUpdates() },
          onDownloadClick = { settingsViewModel.downloadLibrary(fromImportBook) },
          onDeleteClick = { showDeleteConfirmation = true },
          onOpenLibraryClick = onImportBookClicked,
          onCancelDownloadClick = { settingsViewModel.cancelDownload() }
        )
      }
      
      item {
        Column(
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 2.dp)
          )
          
          MultiChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
          ) {
            THEME_OPTIONS.forEachIndexed { index, theme ->
              SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                  index = index, 
                  count = THEME_OPTIONS.size
                ),
                onCheckedChange = {
                  selectedTheme = theme
                  // Update theme settings
                  ThemeSettings.themeOverride.value = theme
                  // Save to data store
                  modelManagerViewModel.saveThemeOverride(theme)
                },
                checked = theme == selectedTheme,
                label = { Text(themeLabel(theme)) },
              )
            }
          }
        }
      }
    }
  }
  
  // Delete confirmation dialog
  if (showDeleteConfirmation) {
    ConfirmDeleteLibraryDialog(
      onConfirm = {
        settingsViewModel.deleteLibrary()
        showDeleteConfirmation = false
      },
      onDismiss = {
        showDeleteConfirmation = false
      }
    )
  }
  
  // Model delete confirmation dialog
  if (showModelDeleteConfirmation && modelToDelete != null) {
    ConfirmDeleteModelDialog(
      modelName = modelToDelete!!.name,
      onConfirm = {
        modelManagerViewModel.deleteModel(TASK_LLM_ASK_IMAGE, modelToDelete!!)
        showModelDeleteConfirmation = false
        modelToDelete = null
      },
      onDismiss = {
        showModelDeleteConfirmation = false
        modelToDelete = null
      }
    )
  }
}

@Composable
private fun SettingsItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() },
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
      )
      
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium
          )
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
private fun SparkReaderLibraryItem(
  libraryState: LibraryState,
  onRefreshClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onOpenLibraryClick: () -> Unit,
  onCancelDownloadClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDownloaded = libraryState.isDownloaded
  val hasUpdate = libraryState.latestVersion != null && 
                  libraryState.currentVersion != libraryState.latestVersion?.version
  
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(enabled = isDownloaded) { onOpenLibraryClick() },
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // First column: Icon (vertically centered)
      Icon(
        imageVector = Icons.Default.Book,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .size(24.dp)
          .align(Alignment.CenterVertically)
      )
      
      // Second column: All existing content
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        // First row: Title and Refresh button
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "SparkReader Library",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Medium
            )
          )
          
          if (isDownloaded) {
            IconButton(
              onClick = onRefreshClick,
              modifier = Modifier.size(32.dp),
              enabled = !libraryState.isCheckingUpdate && !libraryState.isDownloading
            ) {
              if (libraryState.isCheckingUpdate) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
              } else {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Refresh",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
        
        // Second row: Version/Size with inline icon
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          if (isDownloaded) {
            Text(
              text = "Version ${libraryState.currentVersion ?: "Unknown"}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
              onClick = onDeleteClick,
              modifier = Modifier.size(20.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
              )
            }
          } else {
            if (libraryState.latestVersion != null) {
              // Show version and size with download icon
              Text(
                text = "${libraryState.latestVersion.version} (${formatBytes(libraryState.latestVersion.sizeInBytes)})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(20.dp),
                enabled = !libraryState.isDownloading
              ) {
                if (libraryState.isDownloading || libraryState.downloadStatus.status == ModelDownloadStatusType.CONNECTING) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            } else if (libraryState.isCheckingUpdate) {
              // Show loading state
              Text(
                text = "Checking for library...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            } else {
              // Show placeholder text
              Text(
                text = "Library not available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
        
        // Third row: Update status
        if (isDownloaded && libraryState.downloadStatus.status != ModelDownloadStatusType.IN_PROGRESS) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            if (hasUpdate) {
              val latestVersion = libraryState.latestVersion!!
              Text(
                text = "New version available (${latestVersion.version}, ${formatBytes(latestVersion.sizeInBytes)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
              )
              IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(20.dp),
                enabled = !libraryState.isDownloading
              ) {
                if (libraryState.isDownloading || libraryState.downloadStatus.status == ModelDownloadStatusType.CONNECTING) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download update",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            } else if (libraryState.latestVersion != null) {
              Text(
                text = "No update available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
        
        // Error message
        if (libraryState.error.isNotEmpty()) {
          Text(
            text = libraryState.error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
        
        // Progress bar when downloading
        if (libraryState.downloadStatus.status == ModelDownloadStatusType.IN_PROGRESS || 
            libraryState.downloadStatus.status == ModelDownloadStatusType.CONNECTING) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(
              modifier = Modifier.weight(1f)
            ) {
              val progress = if (libraryState.downloadStatus.totalBytes > 0) {
                libraryState.downloadStatus.receivedBytes.toFloat() / 
                libraryState.downloadStatus.totalBytes.toFloat()
              } else {
                0f
              }
              
              LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
              )
              
              if (libraryState.downloadStatus.totalBytes > 0) {
                Text(
                  text = "${formatBytes(libraryState.downloadStatus.receivedBytes)} / ${formatBytes(libraryState.downloadStatus.totalBytes)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }
            
            IconButton(
              onClick = onCancelDownloadClick,
              modifier = Modifier.size(32.dp)
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
}

private fun formatBytes(bytes: Long): String {
  val kb = bytes / 1024.0
  val mb = kb / 1024.0
  val gb = mb / 1024.0
  
  return when {
    gb >= 1 -> "%.1f GB".format(gb)
    mb >= 1 -> "%.1f MB".format(mb)
    kb >= 1 -> "%.1f KB".format(kb)
    else -> "$bytes B"
  }
}

@Composable
private fun ConfirmDeleteLibraryDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Delete Library?") },
    text = {
      Text("This will delete the downloaded library and all its books. You can download it again later from this settings page.")
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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

@Composable
private fun ModelManagerSection(
  modelManagerViewModel: ModelManagerViewModel,
  modelManagerUiState: ModelManagerUiState,
  selectedModelName: String?,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  onModelDeleteRequested: (app.sparkreader.data.Model) -> Unit,
  isLoadingModels: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val task = TASK_LLM_ASK_IMAGE
  val models = task.models.filter { !it.imported }
  val importedModels = task.models.filter { it.imported }
  
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onExpandedChange(!expanded) },
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Header row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Icon(
          imageVector = Icons.Default.SmartToy,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = "AI Models",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Medium
            )
          )
          Text(
            text = if (selectedModelName.isNullOrEmpty()) {
              "No default model selected"
            } else {
              "Default: $selectedModelName"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        
        IconButton(
          onClick = { onExpandedChange(!expanded) },
          modifier = Modifier.size(32.dp),
          enabled = !isLoadingModels
        ) {
          if (isLoadingModels) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Icon(
              imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = if (expanded) "Collapse" else "Expand",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
      
      // Expanded content
      if (expanded) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Models list
        models.forEach { model ->
          ModelItem(
            model = model,
            modelManagerViewModel = modelManagerViewModel,
            modelManagerUiState = modelManagerUiState,
            selectedModelName = selectedModelName,
            onDeleteRequested = onModelDeleteRequested,
            modifier = Modifier.padding(vertical = 4.dp)
          )
        }
        
        // Imported models section
        if (importedModels.isNotEmpty()) {
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Imported models",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 8.dp)
          )
          
          importedModels.forEach { model ->
            ModelItem(
              model = model,
              modelManagerViewModel = modelManagerViewModel,
              modelManagerUiState = modelManagerUiState,
              selectedModelName = selectedModelName,
              onDeleteRequested = onModelDeleteRequested,
              modifier = Modifier.padding(vertical = 4.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ModelItem(
  model: app.sparkreader.data.Model,
  modelManagerViewModel: ModelManagerViewModel,
  modelManagerUiState: ModelManagerUiState,
  selectedModelName: String?,
  onDeleteRequested: (app.sparkreader.data.Model) -> Unit,
  modifier: Modifier = Modifier,
) {
  val downloadStatus = modelManagerUiState.modelDownloadStatus[model.name]
  val isDownloaded = downloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
  val isDownloading = downloadStatus?.status == ModelDownloadStatusType.IN_PROGRESS
  val isConnecting = downloadStatus?.status == ModelDownloadStatusType.CONNECTING
  val isFailed = downloadStatus?.status == ModelDownloadStatusType.FAILED
  val isSelected = selectedModelName == model.name
  val uriHandler = LocalUriHandler.current

  Box(
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
      )
      .clickable(enabled = isDownloaded && !isSelected) { 
        if (isDownloaded && !isSelected) {
          modelManagerViewModel.setSelectedModel(model.name)
        }
      }
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
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        
        // Model link icon if available
        if (model.modelLink.isNotEmpty()) {
          IconButton(
            onClick = { 
              uriHandler.openUri(model.modelLink)
            },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Language,
              contentDescription = "Open model link",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
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
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          // Action button (download or delete)
          when {
            isDownloaded -> {
              IconButton(
                onClick = { onDeleteRequested(model) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete model",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
            isConnecting -> {
              Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
            !isDownloading && !isFailed -> {
              IconButton(
                onClick = {
                  modelManagerViewModel.startModelDownload(TASK_LLM_ASK_IMAGE, model)
                },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = "Download model",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
            isFailed -> {
              IconButton(
                onClick = {
                  modelManagerViewModel.startModelDownload(TASK_LLM_ASK_IMAGE, model)
                },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = "Retry download",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp)
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
                  shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = "Default",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Medium
              )
            }
          } else {
            // Set as default button (outlined)
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.primary,
                  shape = RoundedCornerShape(8.dp)
                )
                .clickable { 
                  modelManagerViewModel.setSelectedModel(model.name)
                }
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = "Set default",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
      
      // Error message when download fails
      if (isFailed && downloadStatus?.errorMessage?.isNotEmpty() == true) {
        Text(
          text = "Error: ${downloadStatus.errorMessage}",
          style = MaterialTheme.typography.labelSmall,
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
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val progress = if ((downloadStatus?.totalBytes ?: 0) > 0) {
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
              
              if ((downloadStatus?.totalBytes ?: 0) > 0) {
                Text(
                  text = "${(downloadStatus?.receivedBytes ?: 0).humanReadableSize()} / ${downloadStatus!!.totalBytes.humanReadableSize()}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }
            
            IconButton(
              onClick = { 
                modelManagerViewModel.cancelDownloadModel(TASK_LLM_ASK_IMAGE, model) 
              },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Cancel download",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }
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
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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

private fun themeLabel(theme: Theme): String {
  return when (theme) {
    Theme.THEME_AUTO -> "Auto"
    Theme.THEME_LIGHT -> "Light"
    Theme.THEME_DARK -> "Dark"
    else -> "Unknown"
  }
}
