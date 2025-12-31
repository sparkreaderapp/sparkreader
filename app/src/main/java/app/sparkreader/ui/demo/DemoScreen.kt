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

package app.sparkreader.ui.demo
import app.sparkreader.R
import android.text.Html
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

data class NextTrigger(
  val action: String? = null,
  val `action-args`: ActionArgs? = null,
  val `animation-direction`: String? = null
)

data class ActionArgs(
  val `context-menu-name`: String? = null,
  val selection: String? = null
)

data class DemoStep(
  val type: String,
  val text: String,
  val `next-trigger`: NextTrigger? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(
  onBackClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var currentStep by remember { mutableStateOf(0) }
  var demoSteps by remember { mutableStateOf<List<DemoStep>>(emptyList()) }
  var isRunning by remember { mutableStateOf(false) }
  var selectedText by remember { mutableStateOf("") }
  var showSelection by remember { mutableStateOf(false) }
  var isTransitioning by remember { mutableStateOf(false) }
  var showContextMenu by remember { mutableStateOf(false) }
  var contextMenuPosition by remember { mutableStateOf(Pair(0f, 0f)) }
  var currentTrigger by remember { mutableStateOf<NextTrigger?>(null) }
  val selectionAlpha = remember { Animatable(0f) }
  val coroutineScope = rememberCoroutineScope()
  var animationDirection by remember { mutableStateOf("forward") }

  // Load demo script
  LaunchedEffect(Unit) {
    demoSteps = loadDemoScript(context)
    if (demoSteps.isNotEmpty()) {
      currentTrigger = demoSteps[currentStep].`next-trigger`
    }
  }

  // Handle context menu selection
  fun handleContextMenuSelect(menuName: String) {
    val trigger = currentTrigger
    if (trigger?.action == "menu-select" && 
        trigger.`action-args`?.`context-menu-name` == menuName) {
      
      // Always move forward to next step, but use animation direction for visual effect
      if (currentStep < demoSteps.size - 1) {
        animationDirection = trigger.`animation-direction` ?: "forward"
        isTransitioning = true
        coroutineScope.launch {
          delay(350) // Wait for exit animation to complete
          currentStep++
          currentTrigger = if (currentStep < demoSteps.size) {
            demoSteps[currentStep].`next-trigger`
          } else null
          delay(50) // Small delay before showing new content
          isTransitioning = false
        }
      }
    }
  }

  // Handle text selection and context menu
  fun handleTextLongPress(selectedText: String) {
    val trigger = currentTrigger
//    if (trigger?.action == "menu-select" && trigger.`action-args`?.selection != null) {
//      val expectedSelection = trigger.`action-args`.selection
//      if (selectedText.equals(expectedSelection, ignoreCase = true)) {
//        // Trigger the next step animation
//        handleContextMenuSelect(trigger.`action-args`?.`context-menu-name` ?: "")
//      }
//    }

    if (trigger?.action == "menu-select") {
        handleContextMenuSelect(trigger.`action-args`?.`context-menu-name` ?: "")
    }
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { 
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            when {
              demoSteps.isNotEmpty() && currentStep < demoSteps.size -> {
                when (demoSteps[currentStep].type) {
                  "ebook-reader" -> {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.MenuBook,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Awesome Book Reader")
                  }
                  "dictionary" -> {
                    Icon(
                      imageVector = Icons.Default.Book,
                      contentDescription = null,
                      tint = Color(0xFF4CAF50),
                      modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Awesome Dictionary")
                  }
                  "browser" -> {
                    Icon(
                      imageVector = Icons.Default.Language,
                      contentDescription = null,
                      tint = Color(0xFF2196F3),
                      modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Web Browser")
                  }
                  else -> Text("Demo")
                }
              }
              else -> Text("Demo")
            }
          }
        }
      )
    },
    modifier = modifier
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp)
        .verticalScroll(rememberScrollState())
    ) {
      if (demoSteps.isNotEmpty() && currentStep < demoSteps.size) {
        val step = demoSteps[currentStep]
        
        // Show spinner during transition
        if (isTransitioning) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            val spinnerColor = when (step.type) {
              "ebook-reader" -> MaterialTheme.colorScheme.primary
              "dictionary" -> Color(0xFF4CAF50)
              "browser" -> Color(0xFF2196F3)
              else -> MaterialTheme.colorScheme.primary
            }
            
            CircularProgressIndicator(
              modifier = Modifier.size(48.dp),
              color = spinnerColor,
              strokeWidth = 4.dp
            )
          }
        }
        
        if (!isTransitioning) {
          Box {
            when (step.type) {
              "ebook-reader" -> {
                EbookReaderView(
                  text = step.text,
                  selectedText = selectedText,
                  showSelection = showSelection,
                  selectionAlpha = selectionAlpha.value,
                  currentTrigger = currentTrigger,
                  onTextLongPress = ::handleTextLongPress
                )
              }
              "dictionary" -> {
                val previousSelection = if (currentStep > 0) {
                  demoSteps[currentStep - 1].`next-trigger`?.`action-args`?.selection ?: ""
                } else ""
                
                DictionaryView(
                  text = step.text,
                  selectedText = selectedText,
                  showSelection = showSelection,
                  selectionAlpha = selectionAlpha.value,
                  currentTrigger = currentTrigger,
                  previousSelection = previousSelection,
                  onTextLongPress = ::handleTextLongPress
                )
              }
              "browser" -> {
                BrowserView(
                  text = step.text,
                  selectedText = selectedText,
                  showSelection = showSelection,
                  selectionAlpha = selectionAlpha.value,
                  currentTrigger = currentTrigger,
                  onTextLongPress = ::handleTextLongPress
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EbookReaderView(
  text: String,
  selectedText: String,
  showSelection: Boolean,
  selectionAlpha: Float,
  currentTrigger: NextTrigger?,
  onTextLongPress: (String) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val textColor = MaterialTheme.colorScheme.onSurface
  
  Column {
    // Book content
    Card(
      modifier = Modifier.fillMaxWidth(),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp)
      ) {
        Text(
          text = "Chapter 12",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(bottom = 16.dp)
        )
        
        AndroidView(
          factory = { context ->
            TextView(context).apply {
              this.text = text
              textSize = 18f
              setTextColor(textColor.toArgb())
              setPadding(0, 0, 0, 0)
              setTextIsSelectable(true)
              setLineSpacing(0f, 1.5f)
              
              // Set custom action mode callback for context menu
              customSelectionActionModeCallback = createContextMenuCallback(currentTrigger, onTextLongPress)
            }
          },
          modifier = Modifier.fillMaxWidth(),
          update = { textView ->
            textView.text = text
            textView.setTextColor(textColor.toArgb())
          }
        )
      }
    }
  }
}

@Composable
private fun DictionaryView(
  text: String,
  selectedText: String,
  showSelection: Boolean,
  selectionAlpha: Float,
  currentTrigger: NextTrigger?,
  previousSelection: String,
  onTextLongPress: (String) -> Unit
) {
  val context = LocalContext.current
  val textColor = MaterialTheme.colorScheme.onSurface
  
  // Use previous selection as search term, or extract from text if no previous selection
  val searchTerm = remember(text, previousSelection) {
    if (previousSelection.isNotEmpty()) {
      previousSelection
    } else {
      val colonIndex = text.indexOf(":")
      if (colonIndex > 0) {
        text.substring(0, colonIndex).trim()
      } else {
        ""
      }
    }
  }
  
  Column {
    // Search box
    OutlinedTextField(
      value = searchTerm,
      onValueChange = { },
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text("Search dictionary...") },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search",
          tint = Color(0xFF4CAF50)
        )
      },
      colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF4CAF50),
        unfocusedBorderColor = Color(0xFF4CAF50).copy(alpha = 0.6f),
        focusedLabelColor = Color(0xFF4CAF50),
        cursorColor = Color(0xFF4CAF50)
      ),
      singleLine = true,
      readOnly = true
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Dictionary content
    Card(
      modifier = Modifier.fillMaxWidth(),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp)
      ) {
        AndroidView(
          factory = { context ->
            TextView(context).apply {
              this.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
              textSize = 18f
              setTextColor(textColor.toArgb())
              setPadding(0, 0, 0, 0)
              setTextIsSelectable(true)
              setLineSpacing(0f, 1.5f)
              
              // Set custom action mode callback for context menu
              customSelectionActionModeCallback = createContextMenuCallback(currentTrigger, onTextLongPress)
            }
          },
          modifier = Modifier.fillMaxWidth(),
          update = { textView ->
            textView.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
            textView.setTextColor(textColor.toArgb())
            // Update the callback when currentTrigger changes
            textView.customSelectionActionModeCallback = createContextMenuCallback(currentTrigger, onTextLongPress)
          }
        )
      }
    }
  }
}

@Composable
private fun BrowserView(
  text: String,
  selectedText: String,
  showSelection: Boolean,
  selectionAlpha: Float,
  currentTrigger: NextTrigger?,
  onTextLongPress: (String) -> Unit
) {
  val textColor = MaterialTheme.colorScheme.onSurface
  
  Column {
    // Address bar with tab count
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
          .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "1",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold
        )
      }
      
      Spacer(modifier = Modifier.width(8.dp))
      
      OutlinedTextField(
        value = "dictionary.com/browse/...",
        onValueChange = { },
        modifier = Modifier.weight(1f),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color(0xFF2196F3),
          unfocusedBorderColor = Color(0xFF2196F3).copy(alpha = 0.6f),
          focusedLabelColor = Color(0xFF2196F3),
          cursorColor = Color(0xFF2196F3)
        ),
        singleLine = true,
        readOnly = true
      )
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Browser content
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onTextLongPress("") },
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp)
      ) {
        AndroidView(
          factory = { context ->
            TextView(context).apply {
              this.text = text
              textSize = 18f
              setTextColor(textColor.toArgb())
              setPadding(0, 0, 0, 0)
              setTextIsSelectable(true)
              setLineSpacing(0f, 1.5f)
              
              // Set custom action mode callback for context menu
              customSelectionActionModeCallback = createContextMenuCallback(currentTrigger, onTextLongPress)
            }
          },
          modifier = Modifier.fillMaxWidth(),
          update = { textView ->
            textView.text = text
            textView.setTextColor(textColor.toArgb())
            // Update the callback when currentTrigger changes
            textView.customSelectionActionModeCallback = createContextMenuCallback(currentTrigger, onTextLongPress)
          }
        )
      }
    }
  }
}

@Composable
private fun buildHighlightedText(
  text: String,
  selectedText: String,
  showSelection: Boolean,
  selectionAlpha: Float
) = buildAnnotatedString {
  if (showSelection && selectedText.isNotEmpty()) {
    val startIndex = text.indexOf(selectedText, ignoreCase = true)
    if (startIndex >= 0) {
      val endIndex = startIndex + selectedText.length
      
      // Text before selection
      append(text.substring(0, startIndex))
      
      // Selected text with highlight
      withStyle(
        style = SpanStyle(
          background = Color.Blue.copy(alpha = selectionAlpha * 0.3f),
          fontWeight = FontWeight.Bold
        )
      ) {
        append(text.substring(startIndex, endIndex))
      }
      
      // Text after selection
      append(text.substring(endIndex))
    } else {
      append(text)
    }
  } else {
    append(text)
  }
}

@Composable
private fun ContextMenu(
  menuName: String,
  onMenuSelect: (String) -> Unit,
  onDismiss: () -> Unit
) {
  Card(
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
  ) {
    Column(
      modifier = Modifier.padding(8.dp)
    ) {
      Text(
        text = "Context Menu",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(8.dp)
      )
      
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onMenuSelect(menuName) }
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = menuName,
          style = MaterialTheme.typography.bodyMedium
        )
      }
      
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onDismiss() }
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Clear,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Cancel",
          style = MaterialTheme.typography.bodyMedium
        )
      }
    }
  }
}

/**
 * Creates a custom ActionMode.Callback for context menus with standardized menu items
 */
private fun createContextMenuCallback(
  currentTrigger: NextTrigger?,
  onTextLongPress: (String) -> Unit
): ActionMode.Callback {
  return object : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
      return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
      // Clear existing menu items
      menu?.clear()
      
      // Get menu name from current trigger, default to "Dictionary"
      val menuName = currentTrigger?.`action-args`?.`context-menu-name` ?: "Dictionary"
      
      // Add custom menu item (with handler)
      menu?.add(0, 1001, 0, menuName)?.apply {
        setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        //setIcon(R.drawable.circ)
      }
      
      // Add Copy (no handler, demo only)
      menu?.add(0, android.R.id.copy, 1, "Copy")?.apply {
        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
      }
      
      // Add Select All (no handler, demo only)
      menu?.add(0, android.R.id.selectAll, 2, "Select All")?.apply {
        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
      }
      
      return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
      return when (item?.itemId) {
        1001 -> {
          onTextLongPress("")
          mode?.finish()
          true
        }
        // Copy and Select All have no handlers (demo only)
        android.R.id.copy, android.R.id.selectAll -> {
          mode?.finish()
          true
        }
        else -> false
      }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
      // Clean up if needed
    }
  }
}

private fun loadDemoScript(context: Context): List<DemoStep> {
  return try {
    val inputStream = context.assets.open("demo_endless.json")
    val json = inputStream.bufferedReader().use { it.readText() }
    val gson = Gson()
    val listType = object : TypeToken<List<DemoStep>>() {}.type
    gson.fromJson(json, listType) ?: emptyList()
  } catch (e: IOException) {
    e.printStackTrace()
    emptyList()
  }
}
