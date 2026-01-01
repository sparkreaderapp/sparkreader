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

package app.sparkreader.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sparkreader.BuildConfig
import app.sparkreader.R
import app.sparkreader.ui.settings.SettingsViewModel
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  settingsViewModel: SettingsViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val libraryState by settingsViewModel.libraryState.collectAsState()
  
  // State for license dialog
  var showLicenseDialog by remember { mutableStateOf(false) }
  var selectedLicense by remember { mutableStateOf("") }
  var licenseContent by remember { mutableStateOf("") }
  
  // Refresh library state when screen becomes visible
  LaunchedEffect(Unit) {
    settingsViewModel.refreshLibraryState()
  }
  
  val libraryVersion = if (libraryState.isDownloaded && libraryState.currentVersion != null) {
    libraryState.currentVersion
  } else {
    "Not downloaded"
  }
  
  // Function to load license content
  fun loadLicense(licenseFile: String, licenseName: String) {
    try {
      val inputStream = context.assets.open("licenses/$licenseFile")
      licenseContent = inputStream.bufferedReader().use { it.readText() }
      selectedLicense = licenseName
      showLicenseDialog = true
    } catch (e: IOException) {
      licenseContent = "Error loading license: ${e.message}"
      selectedLicense = licenseName
      showLicenseDialog = true
    }
  }
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("About SparkReader") },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(
              imageVector = Icons.Default.ArrowBack,
              contentDescription = "Navigate back"
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
        .padding(paddingValues),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // App Header
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            // App logo
            Icon(
              painter = painterResource(if (isDarkTheme()) R.drawable.logo_dark else R.drawable.logo_light),
              contentDescription = "SparkReader Logo",
              tint = Color.Unspecified,
              modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
              text = "SparkReader",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold
            )
            
            Text(
              text = "Offline AI. Limitless Learning. In Your Hand.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
      
      // Version & Build Info
      item {
        InfoCard(
          title = "Version Information",
          content = {
            Column {
              InfoRow("App Version", BuildConfig.VERSION_NAME)
              InfoRow("Library Version", libraryVersion ?: "Not downloaded")
            }
          }
        )
      }
      
      // Links & Resources
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(
            modifier = Modifier.padding(16.dp)
          ) {
            Text(
              text = "Ecosystem",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              CommunityButton(
                icon = Icons.Default.Language,
                label = "Website",
                onClick = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sparkreader.app"))
                  context.startActivity(intent)
                }
              )
              
              CommunityButton(
                icon = Icons.Default.Code,
                label = "App",
                onClick = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sparkreaderapp/sparkreader"))
                  context.startActivity(intent)
                }
              )
              
              CommunityButton(
                icon = Icons.Default.LibraryBooks,
                label = "Library",
                onClick = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sparkreader.app/library.html"))
                  context.startActivity(intent)
                }
              )
            }
          }
        }
      }
      
      // Copyright & Acknowledgments
      item {
        InfoCard(
          title = "Copyright & Acknowledgments",
          content = {
            Column(
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text(
                text = "SparkReader App",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "© 2025 The SparkReader Creator",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Text(
                text = "SparkReader Library Compilation",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "© 2025 The SparkReader Creator",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              
              Divider(modifier = Modifier.padding(vertical = 8.dp))
              
              Text(
                text = "Credits",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
              
              AcknowledgmentItem(
                title = "Project Gutenberg",
                description = "Most books in our library are sourced from Project Gutenberg's extensive collection of public domain works.",
                url = "https://www.gutenberg.org/",
                licenseKey = "PROJECT-GUTENBERG-TERMS.txt",
                onViewLicense = { licenseFile, licenseName -> loadLicense(licenseFile, licenseName) }
              )
              
              AcknowledgmentItem(
                title = "Google Edge Gallery",
                description = "This app is built upon the Google Edge Gallery framework.",
                url = "https://github.com/google/edge-gallery",
                licenseKey = "APACHE-2.0.txt",
                onViewLicense = { licenseFile, licenseName -> loadLicense(licenseFile, licenseName) }
              )

              AcknowledgmentItem(
                title = "Gemma 3n",
                description = "Google's multimodal AI model powering contextual explanations and OCR capabilities.",
                url = "https://deepmind.google/models/gemma/gemma-3n/",
                licenseKey = "GEMMA-TERMS.txt",
                onViewLicense = { licenseFile, licenseName -> loadLicense(licenseFile, licenseName) }
              )
              
//              AcknowledgmentItem(
//                title = "Aider",
//                description = "AI-powered coding assistant used in developing this application.",
//                url = "https://aider.chat/"
//              )
            }
          }
        )
      }
      
//      // Open Source Licenses
//      item {
//        Card(
//          modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp)
//            .clickable {
//              // TODO: Navigate to licenses screen
//            },
//          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//        ) {
//          Row(
//            modifier = Modifier
//              .fillMaxWidth()
//              .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//          ) {
//            Icon(
//              imageVector = Icons.Default.Description,
//              contentDescription = null,
//              tint = MaterialTheme.colorScheme.primary
//            )
//            Spacer(modifier = Modifier.width(16.dp))
//            Column(modifier = Modifier.weight(1f)) {
//              Text(
//                text = "Open Source Licenses",
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold
//              )
//              Text(
//                text = "View licenses for open source software",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//              )
//            }
//          }
//        }
//      }
      
//      // Privacy Policy
//      item {
//        InfoCard(
//          icon = Icons.Default.Lock,
//          title = "Privacy Policy",
//          content = {
//            Column(
//              verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//              Text(
//                text = "Your Privacy Matters",
//                style = MaterialTheme.typography.bodyMedium,
//                fontWeight = FontWeight.Bold
//              )
//
//              Text(
//                text = "• We don't collect or store personal data\n" +
//                      "• No user information is sent over the internet\n" +
//                      "• All processing happens locally on your device\n" +
//                      "• Your reading history stays private",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//              )
//            }
//          }
//        )
//      }

      // Terms of Use
      item {
        InfoCard(
          title = "Terms of Use",
          content = {
            Column(
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "By using SparkReader, you agree to:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
              
              Text(
                text = """By using SparkReader, you agree to use the application for personal, non-commercial purposes and to comply with all applicable laws. Understand that Gemma is provided under and subject to the Gemma Terms of Use found at ai.google.dev/gemma/terms. You acknowledge that AI-generated explanations are provided for informational purposes only and may not always be accurate or complete. SparkReader is provided “as is,” without warranties of any kind, and the creator assumes no responsibility for decisions or outcomes resulting from the use of the app.""",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              
              Spacer(modifier = Modifier.height(8.dp))
              
//              Text(
//                text = "Full terms available at: sparkreader.app/terms",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.clickable {
//                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sparkreader.app/terms"))
//                  context.startActivity(intent)
//                }
//              )
            }
          }
        )
      }
      
      item {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
    
    // License Dialog
    if (showLicenseDialog) {
      LicenseDialog(
        licenseName = selectedLicense,
        licenseContent = licenseContent,
        onDismiss = { showLicenseDialog = false }
      )
    }
  }
}

@Composable
private fun InfoCard(
  title: String,
  icon: ImageVector? = null,
  content: @Composable () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
      ) {
        if (icon != null) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
      content()
    }
  }
}

@Composable
private fun InfoRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium
    )
  }
}

/**
 * Composable function to determine if the current theme is dark.
 *
 * @return true if the current theme is dark, false otherwise
 */
@Composable
fun isDarkTheme(): Boolean {
  return MaterialTheme.colorScheme.background.luminance() < 0.5
}

@Composable
private fun CommunityButton(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(32.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
private fun AcknowledgmentItem(
  title: String,
  description: String,
  url: String? = null,
  licenseKey: String? = null,
  onViewLicense: ((String, String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp, horizontal = 4.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .then(
          if (url != null) {
            Modifier.clickable {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
              context.startActivity(intent)
            }
          } else {
            Modifier
          }
        )
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = if (url != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
      )
      if (url != null) {
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.Default.ArrowForward,
          contentDescription = "Visit $title",
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.primary
        )
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = description,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    // License chip
    if (licenseKey != null && onViewLicense != null) {
      Spacer(modifier = Modifier.height(8.dp))
      FilterChip(
        onClick = { onViewLicense(licenseKey, "$title License") },
        label = { 
          Text(
            text = "View License",
            style = MaterialTheme.typography.bodySmall
          ) 
        },
        selected = false,
        modifier = Modifier.height(32.dp)
      )
    }
  }
}

@Composable
private fun LicenseDialog(
  licenseName: String,
  licenseContent: String,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = licenseName,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(400.dp)
      ) {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = licenseContent,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
    modifier = Modifier.fillMaxWidth(0.95f)
  )
}
