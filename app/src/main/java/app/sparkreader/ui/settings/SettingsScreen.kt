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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cancel
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

private val THEME_OPTIONS = listOf(Theme.THEME_AUTO, Theme.THEME_LIGHT, Theme.THEME_DARK)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onNavigateBack: () -> Unit,
  onModelManagerClicked: () -> Unit,
  onThemeSelectionClicked: () -> Unit,
  onImportBookClicked: () -> Unit,
  modifier: Modifier = Modifier,
  modelManagerViewModel: ModelManagerViewModel = hiltViewModel(),
  settingsViewModel: SettingsViewModel = hiltViewModel(),
  fromImportBook: Boolean = false,
) {
  var selectedTheme by remember { mutableStateOf(ThemeSettings.themeOverride.value) }
  val libraryState by settingsViewModel.libraryState.collectAsState()
  var showDeleteConfirmation by remember { mutableStateOf(false) }
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val selectedModelName = modelManagerUiState.selectedModelName
  
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
        SettingsItem(
          icon = Icons.Default.Storage,
          title = "Model Manager",
          subtitle = if (selectedModelName.isNullOrEmpty()) {
            "No default model"
          } else {
            "Default: $selectedModelName"
          },
          onClick = onModelManagerClicked
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

private fun themeLabel(theme: Theme): String {
  return when (theme) {
    Theme.THEME_AUTO -> "Auto"
    Theme.THEME_LIGHT -> "Light"
    Theme.THEME_DARK -> "Dark"
    else -> "Unknown"
  }
}
