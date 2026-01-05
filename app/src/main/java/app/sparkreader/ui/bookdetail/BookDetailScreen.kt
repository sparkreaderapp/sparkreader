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

package app.sparkreader.ui.bookdetail
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.sparkreader.ui.newhome.Book
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.TextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.systemBars
import com.google.gson.Gson
import app.sparkreader.ui.importbook.paginator.BookPage
import java.io.File
import androidx.compose.animation.core.animateDpAsState
import app.sparkreader.data.STOP_BUTTON_WORD_THRESHOLD
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import app.sparkreader.proto.Theme
import app.sparkreader.ui.theme.ThemeSettings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import app.sparkreader.data.BookTagUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
  book: Book,
  onNavigateUp: () -> Unit,
  onNavigateToModelManager: () -> Unit = {},
  modifier: Modifier = Modifier,
  viewModel: BookDetailViewModel = hiltViewModel(),
) {
  var currentPage by remember { mutableIntStateOf(book.lastReadPage) }
  var showBookInfoDialog by remember { mutableStateOf(false) }
  var showPageInputDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showIntroDialog by remember { mutableStateOf(false) }
  
  val context = LocalContext.current
  val chatMessages by viewModel.chatMessages.collectAsState()
  val isGenerating by viewModel.isGenerating.collectAsState()
  val validationError by viewModel.validationError.collectAsState()
  val modelValidationError by viewModel.modelValidationError.collectAsState()
  val snackbarMessage by viewModel.snackbarMessage.collectAsState()
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  
  // Always create a new LazyListState per bookId to ensure scroll resets on book/page change
  val bookId = book.libraryId ?: book.id.toString()
  val lazyListState = remember(bookId) { LazyListState() }
  val isLoadingMoreMessages by viewModel.isLoadingMoreMessages.collectAsState()
  val showBackConfirmDialog by viewModel.showBackConfirmDialog.collectAsState()
  
  var previousMessageCount by remember { mutableIntStateOf(0) }
  var isUserScrolling by remember { mutableStateOf(false) }

  // Clear validation error and load chat history when entering the screen
  LaunchedEffect(bookId) {
    viewModel.clearValidationError()
    
    // Clear previous book's messages immediately to prevent UI lag
    viewModel.clearChatMessages()
    
    // Check if we should show the intro dialog
    if (!viewModel.getHasSeenIntroDialog()) {
      showIntroDialog = true
    }
    
    // Validate selected model at page launch
    launch {
      delay(100) // Small delay to ensure UI is ready
      viewModel.refreshModelState()
    }
    
    // Load chat history in background with delay to not affect page animation
    launch {
      // Wait for page animation to complete
      delay(600) // Slightly longer than the 500ms animation duration
      viewModel.loadChatHistory(bookId)
    }
  }
  
  // Recheck model existence when screen becomes visible
  // This ensures UI updates if model was downloaded/changed while away
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, bookId) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      when (event) {
        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
          // Force refresh of model state
          viewModel.refreshModelState()
        }
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      // Clear snackbars when leaving the screen
      snackbarHostState.currentSnackbarData?.dismiss()
    }
  }
  
  // Also refresh when first entering the screen
  LaunchedEffect(bookId) {
    // Delay model state refresh to not interfere with page animation
    launch {
      delay(300) // Quick delay for model state check
      viewModel.refreshModelState()
    }
  }
  
  // Scroll to bottom when entering a new book and messages are loaded
  LaunchedEffect(bookId, chatMessages.size) {
    if (chatMessages.isNotEmpty() && !isLoadingMoreMessages) {
      // Check if this is initial load (messages loaded for first time in this book)
      if (previousMessageCount == 0 && chatMessages.size > 0) {
        // Add a small delay to ensure smooth scrolling after messages load
        delay(100)
        // Scroll to bottom for initial load
        lazyListState.scrollToItem(chatMessages.size) // Points to the spinner item
        previousMessageCount = chatMessages.size
      }
    }
  }
  
  var chatInput by remember { mutableStateOf(TextFieldValue("")) }
  val chatInputFocusRequester = remember { FocusRequester() }
  
  // Clipboard and snackbar
  val clipboardManager = LocalClipboardManager.current
  
  // Show snackbar messages from ViewModel
  LaunchedEffect(snackbarMessage) {
    snackbarMessage?.let { message ->
      try {
        // Clear any existing snackbar before showing new one
        snackbarHostState.currentSnackbarData?.dismiss()
        
        val result = snackbarHostState.showSnackbar(
          message = message,
          actionLabel = if (message.contains("model", ignoreCase = true)) "Go to Models" else "Dismiss",
          duration = androidx.compose.material3.SnackbarDuration.Long
        )
        
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed && 
            message.contains("model", ignoreCase = true)) {
          onNavigateToModelManager()
        }
        
        viewModel.clearSnackbarMessage()
      } catch (e: Exception) {
        // Ignore if coroutine is cancelled (e.g., when leaving the screen)
        viewModel.clearSnackbarMessage()
      }
    }
  }
  
  // Bottom sheet state
  val bottomSheetState = rememberStandardBottomSheetState(
    initialValue = SheetValue.PartiallyExpanded
  )
  val scaffoldState = rememberBottomSheetScaffoldState(
    bottomSheetState = bottomSheetState
  )
  
  // Settings state
  var fontSize by remember { mutableFloatStateOf(viewModel.getFontSize()) }
  var temperature by remember { mutableFloatStateOf(viewModel.getTemperature()) }
  val selectedModelName by viewModel.selectedModelName.collectAsState()
  val defaultTemperature = viewModel.getSelectedModelDefaultTemperature()
  
  // Save the current page when it changes
  LaunchedEffect(currentPage) {
    if (currentPage != book.lastReadPage) {
      viewModel.updateLastReadPage(book, currentPage)
    }
  }
  
  // Load paginated book data using the library ID if available, otherwise fall back to numeric ID
  val totalPages = book.totalPages ?: 0
  
  val currentPageText = remember(currentPage) {
    loadSinglePage(context, bookId, currentPage)?.content ?: "No content available"
  }

  val view = LocalView.current
  
  // Make the screen full screen with smooth transition
  LaunchedEffect(Unit) {
    val window = (context as? androidx.activity.ComponentActivity)?.window
    if (window != null) {
      // First, set the window to not fit system windows
      WindowCompat.setDecorFitsSystemWindows(window, false)
      
      // Wait longer for navigation animation to complete
      kotlinx.coroutines.delay(500)
      
      // Then hide both status bar and navigation bar
      val windowInsetsController = WindowCompat.getInsetsController(window, view)
      windowInsetsController.apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      }
    }
  }
  
  // Clean up when leaving the screen
  DisposableEffect(Unit) {
    onDispose {
      val window = (context as? androidx.activity.ComponentActivity)?.window
      if (window != null) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
      }
      // Clear any remaining snackbars when leaving the screen
      snackbarHostState.currentSnackbarData?.dismiss()
      // Also clear the ViewModel's snackbar message to prevent showing on return
      viewModel.clearSnackbarMessage()
    }
  }

  BackHandler {
    val shouldShowDialog = viewModel.handleBackPressed()
    if (!shouldShowDialog) {
      coroutineScope.launch {
        // Restore system bars before navigating back
        val window = (context as? androidx.activity.ComponentActivity)?.window
        if (window != null) {
          val windowInsetsController = WindowCompat.getInsetsController(window, view)
          windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
        // Small delay to allow the system bars to show before navigation
        delay(100)
        onNavigateUp()
      }
    }
  }

  // Animate padding changes for smooth transition
  var isFullScreen by remember { mutableStateOf(false) }
  
  LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(500)
    isFullScreen = true
  }
  
  val bottomPadding by animateDpAsState(
    targetValue = if (isFullScreen) 0.dp else with(LocalDensity.current) { 
      WindowInsets.systemBars.getBottom(this).toDp() 
    },
    animationSpec = tween(durationMillis = 300),
    label = "bottomPadding"
  )
  
  // Track keyboard visibility and height
  val imeInsets = WindowInsets.ime
  val imeHeight = with(LocalDensity.current) { imeInsets.getBottom(this).toDp() }
  val isKeyboardVisible = imeHeight > 0.dp
  
  // Animate bottom sheet offset based on keyboard visibility
  val sheetOffset by animateDpAsState(
    targetValue = if (isKeyboardVisible) imeHeight else 0.dp,
    animationSpec = tween(durationMillis = 300),
    label = "sheetOffset"
  )
  
  BottomSheetScaffold(
    scaffoldState = scaffoldState,
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    },
    sheetContent = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.6f)
          .padding(start = 16.dp, top = 0.dp, end= 16.dp, bottom= 0.dp)
          .offset(y = -sheetOffset) // Move sheet up when keyboard appears
      ) {
        // Track if we're loading more messages
        var isLoadingMoreMessages by remember { mutableStateOf(false) }
        val isLoadingMore by viewModel.isLoadingMoreMessages.collectAsState()
        
        // Chat messages
        LazyColumn(
          state = lazyListState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          reverseLayout = false
        ) {
          // Load more indicator at the top
          if (viewModel.hasMoreMessages() || isLoadingMore) {
            item(key = "load-more-indicator") {
              // Track scroll position before loading starts
              var scrollPositionCaptured by remember { mutableStateOf(false) }
              var savedFirstVisibleIndex by remember { mutableIntStateOf(0) }
              var savedFirstVisibleOffset by remember { mutableIntStateOf(0) }
              var hasTriggeredLoad by remember { mutableStateOf(false) }
              
              // Trigger load when this item becomes visible
              LaunchedEffect(Unit) {
                if (!hasTriggeredLoad && !isLoadingMore && viewModel.hasMoreMessages()) {
                  // Capture the current scroll position before loading
                  // We want the first actual message item, not the spinner
                  val firstMessageIndex = if (lazyListState.firstVisibleItemIndex == 0) {
                    // If we're looking at the spinner, find the first message
                    1
                  } else {
                    lazyListState.firstVisibleItemIndex
                  }
                  
                  savedFirstVisibleIndex = firstMessageIndex
                  savedFirstVisibleOffset = if (lazyListState.firstVisibleItemIndex == 0) {
                    0 // Reset offset if we're at the spinner
                  } else {
                    lazyListState.firstVisibleItemScrollOffset
                  }
                  scrollPositionCaptured = true
                  hasTriggeredLoad = true
                  
                  // Set flag before loading more messages
                  isLoadingMoreMessages = true
                  // Load more when this item becomes visible
                  viewModel.loadMoreMessages()
                }
              }
              
              // When loading completes, restore scroll position
              LaunchedEffect(isLoadingMore, chatMessages.size) {
                if (!isLoadingMore && hasTriggeredLoad && scrollPositionCaptured) {
                  val itemsAdded = viewModel.getLastLoadedCount()
                  if (itemsAdded > 0) {
                    // Calculate the new position
                    // The saved index now needs to account for the newly added items
                    val newIndex = savedFirstVisibleIndex + itemsAdded
                    
                    // Scroll to maintain view of the same content
                    lazyListState.scrollToItem(
                      index = newIndex,
                      scrollOffset = savedFirstVisibleOffset
                    )
                    
                    // Reset flags
                    scrollPositionCaptured = false
                    isLoadingMoreMessages = false
                    hasTriggeredLoad = false
                  } else if (itemsAdded == 0) {
                    // No items were added, reset the loading state
                    isLoadingMoreMessages = false
                    hasTriggeredLoad = false
                  }
                }
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
              }
            }
          }
          // Show placeholder if no messages and model is valid
          if (chatMessages.isEmpty() && modelValidationError.isNullOrEmpty()) {
            item {
              // Model is selected - show feature guide
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Text(
                    text = "How to use SparkReader AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                  )
                  
                  // Explain feature
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                  ) {
                    Icon(
                      imageVector = Icons.Default.Info,
                      contentDescription = null,
                      modifier = Modifier.size(20.dp),
                      tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = "Explain: select a word or phrase in the book and tap 'Explain'",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                      )
                    }
                  }
                  
                  // Quote feature
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                  ) {
                    Icon(
                      imageVector = Icons.Default.FormatQuote,
                      contentDescription = null,
                      modifier = Modifier.size(20.dp),
                      tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = "Chat about a quote: select as much as you want and tap 'Quote'",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                      )
                    }
                  }
                  
                  // Page chat
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                  ) {
                    Icon(
                      imageVector = Icons.Default.Description,
                      contentDescription = null,
                      modifier = Modifier.size(20.dp),
                      tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = "Free chat: use chips below to start a free chat about the book",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                      )
                    }
                  }
                }
              }
            }
          }
          
          items(
            items = chatMessages,
            key = { message -> 
              // Generate a stable unique key that doesn't depend on index
              // This prevents UI glitches when messages are prepended
              "${message.isUser}_${message.content.hashCode()}_${System.identityHashCode(message)}"
            }
          ) { message ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
            ) {
              Card(
                modifier = Modifier
                  .widthIn(max = 280.dp)
                  .combinedClickable(
                    onClick = { /* Regular click - do nothing */ },
                    onLongClick = {
                      clipboardManager.setText(AnnotatedString(message.content))
                      // Android 13+ shows system notification for clipboard operations
                    }
                  ),
                colors = CardDefaults.cardColors(
                  containerColor = if (message.isUser) 
                    MaterialTheme.colorScheme.primaryContainer 
                  else 
                    MaterialTheme.colorScheme.surfaceVariant
                )
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser)
                      MaterialTheme.colorScheme.onPrimaryContainer
                    else
                      MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
          
          // Always show spinner item (visible or invisible based on streaming state)
          item {
            val lastMessage = chatMessages.lastOrNull()
            val isStreaming = lastMessage?.isStreaming == true
            val showStopButton = isStreaming && (lastMessage?.wordCount ?: 0) >= STOP_BUTTON_WORD_THRESHOLD
            val showProgress = isStreaming || isGenerating
            
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = if (chatMessages.isNotEmpty() && chatMessages.last().isUser) 
                Arrangement.End else Arrangement.Start
            ) {
              Card(
                modifier = Modifier.widthIn(max = 280.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (showProgress) 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                  else
                    Color.Transparent
                )
              ) {
                Box(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  contentAlignment = Alignment.Center
                ) {
                  if (showProgress) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(20.dp),
                      strokeWidth = 2.dp,
                      color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Only show stop button after 10 words
                    if (showStopButton) {
                      IconButton(
                        onClick = { viewModel.stopGeneration() },
                        modifier = Modifier.size(20.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Stop,
                          contentDescription = "Stop generation",
                          modifier = Modifier.size(10.dp),
                          tint = MaterialTheme.colorScheme.primary
                        )
                      }
                    }
                  } else {
                    // Invisible placeholder to prevent jumping
                    Box(modifier = Modifier.size(20.dp))
                  }
                }
              }
            }
          }
          
          // Show download model message at bottom if no model is selected or there's a validation error
          if (!modelValidationError.isNullOrEmpty()) {
            item {
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    imageVector = if (!modelValidationError.isNullOrEmpty()) Icons.Default.Warning else Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (!modelValidationError.isNullOrEmpty())
                      MaterialTheme.colorScheme.error
                    else
                      MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = modelValidationError ?: "XX",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                  )
                  Button(
                    onClick = { onNavigateToModelManager() },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.error,
                      contentColor = MaterialTheme.colorScheme.onError
                    )
                  ) {
                    Text(
                      text = "Go to Settings",
                      //style = MaterialTheme.typography.bodySmall
                    )
                  }
                }
              }
            }
          }
        }
        
        // Track user scrolling
        LaunchedEffect(lazyListState.isScrollInProgress) {
          if (lazyListState.isScrollInProgress) {
            // User is manually scrolling
            isUserScrolling = true
          } else {
            // Scrolling stopped, check if user is near bottom
            val lastVisibleIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = lazyListState.layoutInfo.totalItemsCount
            // Consider "near bottom" if within 2 items of the end
            if (lastVisibleIndex >= totalItems - 3) {
              isUserScrolling = false
            }
          }
        }
        
        // Auto-scroll to bottom during streaming/generation
        LaunchedEffect(chatMessages, isGenerating) {
          if (chatMessages.isNotEmpty() && !isLoadingMoreMessages) {
            // Auto-scroll during streaming or generation
            val lastMessage = chatMessages.lastOrNull()
            val shouldAutoScroll = lastMessage?.isStreaming == true || isGenerating
            
            if (shouldAutoScroll) {
              // Always scroll to bottom during generation, regardless of user scroll position
              // During generation, we want to see the bottom of the content
              // The spinner is at index chatMessages.size, so we scroll there
              // This ensures the bottom of the last message is visible
              val targetIndex = chatMessages.size // Points to the spinner item
              // Scroll without animation for smoother real-time updates during streaming
              lazyListState.scrollToItem(targetIndex)
              // Reset user scrolling flag during generation
              isUserScrolling = false
            }
          }
        }
        
        // Scroll to bottom when new messages are added at the bottom
        LaunchedEffect(chatMessages.size) {
          if (chatMessages.isNotEmpty() && !isLoadingMoreMessages) {
            // Check if new messages were added (not just updated)
            if (chatMessages.size > previousMessageCount) {
              // Check if messages were added at the bottom (new messages) vs top (loaded history)
              val addedAtBottom = chatMessages.size - previousMessageCount < 10 // Assume bulk loads are > 10
              
              if (addedAtBottom) {
                // New message added at bottom, scroll to bottom immediately
                previousMessageCount = chatMessages.size
                // Reset user scrolling flag when new message is added
                isUserScrolling = false
                delay(50) // Small delay to ensure UI has updated
                lazyListState.scrollToItem(chatMessages.size) // Scroll to spinner
              } else {
                // Messages loaded at top (history), just update count without scrolling
                previousMessageCount = chatMessages.size
              }
            }
          }
        }
        
        // No longer need this LaunchedEffect as scroll handling is done in the load more item
        
        // Quick action chips
        val hasModel = !selectedModelName.isNullOrEmpty()
//        Row(
//          modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp),
//          horizontalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//          AssistChip(
//            onClick = {
//              val text = "/page p${currentPage+1} "
//              chatInput = TextFieldValue(
//                text = text,
//                selection = TextRange(text.length)
//              )
//              // Request focus after setting text
//              coroutineScope.launch {
//                chatInputFocusRequester.requestFocus()
//              }
//            },
//            enabled = hasModel && !isGenerating,
//            label = { Text("Chat about page") },
//            colors = AssistChipDefaults.assistChipColors(
//              containerColor = if (hasModel && !isGenerating)
//                MaterialTheme.colorScheme.secondaryContainer
//              else
//                MaterialTheme.colorScheme.surfaceVariant
//            )
//          )
//
//          AssistChip(
//            onClick = {
//              val text = "/book "
//              chatInput = TextFieldValue(
//                text = text,
//                selection = TextRange(text.length)
//              )
//              // Request focus after setting text
//              coroutineScope.launch {
//                chatInputFocusRequester.requestFocus()
//              }
//            },
//            enabled = hasModel && !isGenerating,
//            label = { Text("Chat about book") },
//            colors = AssistChipDefaults.assistChipColors(
//              containerColor = if (hasModel && !isGenerating)
//                MaterialTheme.colorScheme.tertiaryContainer
//              else
//                MaterialTheme.colorScheme.surfaceVariant
//            )
//          )
//        }
        
        // Validation error message
        validationError?.let { error ->
          Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
        
        // Input field
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = chatInput,
            onValueChange = { 
              chatInput = it
              // Clear validation error when user types
              if (validationError != null) {
                viewModel.clearValidationError()
              }
            },
            modifier = Modifier
              .weight(1f)
              .focusRequester(chatInputFocusRequester),
            placeholder = { 
              Text(
                if (hasModel) "Ask a question..." else "Download a model to enable chat"
              ) 
            },
            keyboardOptions = KeyboardOptions(
              imeAction = ImeAction.Default
            ),
            minLines = 3,
            maxLines = 3,
            singleLine = false,
            enabled = !isGenerating && hasModel
          )
          
          IconButton(
            onClick = {
              if (chatInput.text.isNotBlank() && !isGenerating) {
                val success = viewModel.sendChatMessage(chatInput.text, currentPage, book.title, book.author, totalPages)
                if (success) {
                  chatInput = TextFieldValue("")
                  // Scroll will happen automatically via LaunchedEffect when message count changes
                }
              }
            },
            enabled = chatInput.text.isNotBlank() && !isGenerating && hasModel
          ) {
            Icon(
              imageVector = Icons.Default.Send,
              contentDescription = "Send",
              tint = if (chatInput.text.isNotBlank() && !isGenerating && hasModel) 
                MaterialTheme.colorScheme.primary 
              else 
                MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    },
    sheetPeekHeight = 120.dp,
    modifier = modifier
      .fillMaxSize()
      .padding(bottom = bottomPadding)
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      verticalArrangement = Arrangement.Top
    ) {
      // TOC and paging row - no spacing
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 8.dp, top = 16.dp, end=8.dp, bottom=0.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left side: TOC, info and settings icons
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 7.5.dp) // Align with back arrow (40dp - 25dp) / 2
        ) {
          val tocFile = remember(bookId) {
            val libraryDir = File(context.filesDir, "library")
            val bookDir = File(libraryDir, bookId)
            File(bookDir, "toc.json")
          }
          
          if (tocFile.exists()) {
            IconButton(
              onClick = { /* TODO: Show TOC dialog */ },
              modifier = Modifier.size(25.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Table of Contents",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          
          IconButton(
            onClick = { showBookInfoDialog = true },
            modifier = Modifier.size(25.dp)
          ) {
            Icon(
              Icons.Default.Info,
              contentDescription = "Book Information",
              modifier = Modifier.size(20.dp)
            )
          }
          
          IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier.size(25.dp)
          ) {
            Icon(
              Icons.Default.Settings,
              contentDescription = "Settings",
              modifier = Modifier.size(20.dp)
            )
          }
        }
        
        // Center spacer
        Spacer(modifier = Modifier.weight(1f))
        
        // Right side: Paging controls
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { 
              if (currentPage > 0) currentPage-- 
            },
            enabled = currentPage > 0,
            modifier = Modifier.size(25.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ArrowBackIos,
              contentDescription = "Previous page",
              modifier = Modifier.size(20.dp)
            )
          }
          
          Text(
            text = "${currentPage + 1} / $totalPages",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
              .padding(horizontal = 0.dp)
              .clickable { showPageInputDialog = true }
          )
          
          IconButton(
            onClick = { 
              if (currentPage < totalPages - 1) currentPage++ 
            },
            enabled = currentPage < totalPages - 1,
            modifier = Modifier.size(25.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ArrowForwardIos,
              contentDescription = "Next page",
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
      
      // Book title row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = {
            val shouldShowDialog = viewModel.handleBackPressed()
            if (!shouldShowDialog) {
              coroutineScope.launch {
                // Restore system bars before navigating back
                val window = (context as? androidx.activity.ComponentActivity)?.window
                if (window != null) {
                  val windowInsetsController = WindowCompat.getInsetsController(window, view)
                  windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                }
                // Small delay to allow the system bars to show before navigation
                delay(100)
                onNavigateUp()
              }
            }
          },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            Icons.Default.ArrowBack, 
            contentDescription = "Back",
            modifier = Modifier.size(20.dp)
          )
        }
        
        Text(
          text = book.title,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier
            .padding(start = 8.dp)
            .weight(1f)
        )
      }
      
      // Tinted divider line below title
      HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
      )
      
      // Main content area with selectable text
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .padding(top = 8.dp)
          .background(MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier.fillMaxSize()
        ) {
          // Left tap area for previous page
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .width(20.dp)
              .clickable(
                enabled = currentPage > 0,
                onClick = { currentPage-- },
                indication = null, // Remove ripple effect for invisible tap area
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
              )
          )
          
          // Center area with text content
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .weight(1f)
              .padding(top = 8.dp) // Only top padding, bottom handled by content
          ) {
            AnimatedContent(
              targetState = currentPage,
              transitionSpec = {
                if (targetState > initialState) {
                  // Going to next page
                  (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                    (slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                  // Going to previous page
                  (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                    (slideOutHorizontally { width -> width } + fadeOut())
                }.using(
                  SizeTransform(clip = false)
                )
              },
              label = "page_transition"
            ) { page ->
              val pageText = loadSinglePage(context, bookId, page)?.content ?: "No content available"
              val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
              val scrollState = rememberScrollState()
              
              // Calculate bottom padding based on bottom sheet state
              val bottomSheetOffset = with(LocalDensity.current) {
                val sheetPeekHeight = 120.dp.toPx()
                val currentSheetOffset = try {
                  val sheetTop = scaffoldState.bottomSheetState.requireOffset()
                  val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
                  screenHeight - sheetTop
                } catch (e: Exception) {
                  sheetPeekHeight
                }
                currentSheetOffset.toDp()
              }
              
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(scrollState)
                  .padding(bottom = bottomSheetOffset + 8.dp) // Add padding for bottom sheet
              ) {
                AndroidView(
                  factory = { context ->
                    TextView(context).apply {
                      text = pageText
                      textSize = fontSize
                      setTextColor(textColor)
                      setPadding(0, 0, 0, 0)
                      setTextIsSelectable(true)
                      // Set line spacing to 1.5
                      setLineSpacing(0f, 1.5f)
                      // Disable TextView's own scrolling to let Compose handle it
                      movementMethod = android.text.method.ArrowKeyMovementMethod.getInstance()
                      // Removed text justification as it interferes with text selection handles
                      
                      // Speed up long press detection
                      var longPressTimer: java.util.Timer? = null
                      var touchDownTime = 0L
                      var touchDownX = 0f
                      var touchDownY = 0f
                      val longPressThreshold = 300L // 300ms instead of default 500ms
                      val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                      
                      setOnTouchListener { view, event ->
                        when (event.action) {
                          android.view.MotionEvent.ACTION_DOWN -> {
                            touchDownTime = System.currentTimeMillis()
                            touchDownX = event.x
                            touchDownY = event.y
                            
                            // Start timer for long press
                            longPressTimer?.cancel()
                            longPressTimer = java.util.Timer()
                            longPressTimer?.schedule(object : java.util.TimerTask() {
                              override fun run() {
                                view.post {
                                  // Trigger long press
                                  view.performLongClick()
                                }
                              }
                            }, longPressThreshold)
                            
                            // Let the default handler process the event too
                            false
                          }
                          android.view.MotionEvent.ACTION_MOVE -> {
                            // Cancel timer if finger moved too much
                            val deltaX = kotlin.math.abs(event.x - touchDownX)
                            val deltaY = kotlin.math.abs(event.y - touchDownY)
                            if (deltaX > touchSlop || deltaY > touchSlop) {
                              longPressTimer?.cancel()
                              longPressTimer = null
                            }
                            false
                          }
                          android.view.MotionEvent.ACTION_UP,
                          android.view.MotionEvent.ACTION_CANCEL -> {
                            // Cancel timer
                            longPressTimer?.cancel()
                            longPressTimer = null
                            false
                          }
                          else -> false
                        }
                      }
                    
                    // Set custom action mode callback
                    customSelectionActionModeCallback = object : ActionMode.Callback {
                      override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        // Let the system create the default menu
                        return true
                      }

                      override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        // Check if "Explain" item already exists
                        if (menu?.findItem(1001) == null) {
                          // Add custom "Explain" menu item only if it doesn't exist
                          menu?.add(0, 1001, 0, "Explain")?.apply {
                            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                            //setIcon(R.drawable.logo_menu)
                          }
                        }
                        // Check if "Quote" item already exists
                        if (menu?.findItem(1002) == null) {
                          // Add custom "Quote" menu item only if it doesn't exist
                          menu?.add(0, 1002, 0, "Quote")?.apply {
                            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                          }
                        }
                        return true
                      }

                      override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                        return when (item?.itemId) {
                          1001 -> {
                            // Handle "Explain" action
                            val start = selectionStart
                            val end = selectionEnd
                            if (start != end && start >= 0 && end >= 0) {
                              // Check if generation is in progress
                              if (isGenerating) {
                                viewModel.showSnackbarMessage(
                                  message = "Please wait for the current generation to finish",
                                )
                              } else {
                                // Get explanation from LLM
                                val success = viewModel.getExplanation(
                                  pageText = text.toString(),
                                  selectionStart = start,
                                  selectionEnd = end,
                                  currentPage = currentPage,
                                  bookTitle = book.title,
                                  bookAuthor = book.author,
                                  totalPages = totalPages
                                )
                                if (success) {
                                  // Expand bottom sheet - scroll will happen automatically via LaunchedEffect
                                  coroutineScope.launch {
                                    scaffoldState.bottomSheetState.expand()
                                  }
                                }
                              }
                            }
                            mode?.finish()
                            true
                          }
                          1002 -> {
                            // Handle "Quote" action
                            val start = selectionStart
                            val end = selectionEnd
                            if (start != end && start >= 0 && end >= 0) {
                              // Check if generation is in progress
                              if (isGenerating) {
                                viewModel.showSnackbarMessage("Please wait for the current generation to finish")
                              } else {
                                coroutineScope.launch {
                                  // Generate quote command
                                  val quoteCommand = viewModel.generateQuoteCommand(
                                    pageText = text.toString(),
                                    selectionStart = start,
                                    selectionEnd = end,
                                    currentPage = currentPage
                                  )
                                  
                                  if (quoteCommand.isNotEmpty()) {
                                    chatInput = TextFieldValue(
                                      text = quoteCommand,
                                      selection = TextRange(quoteCommand.length)
                                    )
                                    // Request focus and expand bottom sheet
                                    chatInputFocusRequester.requestFocus()
                                    scaffoldState.bottomSheetState.expand()
                                  }
                                }
                              }
                            }
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
                  },
                  modifier = Modifier.fillMaxWidth(),
                  update = { textView ->
                    textView.text = pageText
                    textView.textSize = fontSize
                    textView.setTextColor(textColor)
                  }
                )
              }
            }
          }
          
          // Right tap area for next page
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .width(20.dp)
              .clickable(
                enabled = currentPage < totalPages - 1,
                onClick = { currentPage++ },
                indication = null, // Remove ripple effect for invisible tap area
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
              )
          )
        }
      }
    }
  }
    
    // Book info dialog
    if (showBookInfoDialog) {
      BookInfoDialog(
        book = book,
        onDismiss = { showBookInfoDialog = false }
      )
    }
    
    // Page input dialog
    if (showPageInputDialog) {
      PageInputDialog(
        currentPage = currentPage + 1, // Display 1-based page number
        totalPages = totalPages,
        onPageSelected = { page ->
          currentPage = page - 1 // Convert back to 0-based index
          showPageInputDialog = false
        },
        onDismiss = { showPageInputDialog = false }
      )
    }
    
    // Settings dialog
    if (showSettingsDialog) {
      BookSettingsDialog(
        currentTheme = ThemeSettings.themeOverride.value,
        selectedModelName = selectedModelName,
        defaultTemperature = defaultTemperature,
        fontSize = fontSize,
        temperature = temperature,
        onThemeChanged = { theme ->
          ThemeSettings.themeOverride.value = theme
          viewModel.saveTheme(theme)
        },
        onFontSizeChanged = { newSize ->
          fontSize = newSize
          viewModel.saveFontSize(newSize)
        },
        onTemperatureChanged = { newTemp ->
          temperature = newTemp
          viewModel.saveTemperature(newTemp)
        },
        onNavigateToModelManager = {
          showSettingsDialog = false
          coroutineScope.launch {
            // Save the current book and page state before navigating
            viewModel.updateLastReadPage(book, currentPage)
            onNavigateToModelManager()
          }
        },
        onDismiss = { showSettingsDialog = false }
      )
    }
    
    // Back confirmation dialog
    if (showBackConfirmDialog) {
      BackConfirmationDialog(
        onAbortAndLeave = {
          viewModel.confirmBackWithAbort()
          coroutineScope.launch {
            // Restore system bars before navigating back
            val window = (context as? androidx.activity.ComponentActivity)?.window
            if (window != null) {
              val windowInsetsController = WindowCompat.getInsetsController(window, view)
              windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            // Small delay to allow the system bars to show before navigation
            delay(100)
            onNavigateUp()
          }
        },
        onStay = {
          viewModel.dismissBackConfirmDialog()
        },
        onDismiss = {
          viewModel.dismissBackConfirmDialog()
        }
      )
    }
    
    // Intro dialog for first-time users
    if (showIntroDialog) {
      IntroDialog(
        onDismiss = {
          showIntroDialog = false
          viewModel.saveHasSeenIntroDialog(true)
        }
      )
    }
  }

@Composable
private fun BookSettingsDialog(
  currentTheme: Theme,
  selectedModelName: String?,
  defaultTemperature: Float?,
  fontSize: Float,
  temperature: Float,
  onThemeChanged: (Theme) -> Unit,
  onFontSizeChanged: (Float) -> Unit,
  onTemperatureChanged: (Float) -> Unit,
  onNavigateToModelManager: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { 
      Text(
        text = "Settings",
        style = MaterialTheme.typography.headlineSmall
      ) 
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Theme selection - no heading needed
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier.fillMaxWidth()
        ) {
          val themeOptions = listOf(
            Theme.THEME_AUTO to "Auto",
            Theme.THEME_LIGHT to "Light", 
            Theme.THEME_DARK to "Dark"
          )
          
          themeOptions.forEachIndexed { index, (theme, label) ->
            SegmentedButton(
              shape = SegmentedButtonDefaults.itemShape(
                index = index,
                count = themeOptions.size
              ),
              onClick = { onThemeChanged(theme) },
              selected = theme == currentTheme,
              label = { Text(label) }
            )
          }
        }
        
        // Model selection in a card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToModelManager() },
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "AI Model",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (selectedModelName.isNullOrEmpty()) {
                  "No model selected - tap to download"
                } else {
                  selectedModelName
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Icon(
              imageVector = Icons.Default.ArrowForwardIos,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        
        // Font size with slider
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Font Size",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(80.dp)
          )
          
          Slider(
            value = fontSize,
            onValueChange = onFontSizeChanged,
            valueRange = 12f..32f,
            steps = 9, // Creates steps of 2: 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32
            modifier = Modifier.weight(1f)
          )
          
          Text(
            text = "${fontSize.toInt()}sp",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
          )
        }
        
        // Temperature with slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Temp.",
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier.width(80.dp),
              color = if (selectedModelName.isNullOrEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
              } else {
                MaterialTheme.colorScheme.onSurface
              }
            )
            
            Slider(
              value = temperature * 100f, // Convert 0-1 to 0-100
              onValueChange = { onTemperatureChanged(it / 100f) },
              valueRange = 0f..100f,
              modifier = Modifier.weight(1f),
              enabled = !selectedModelName.isNullOrEmpty()
            )
            
            Text(
              text = "${(temperature * 100).toInt()}",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.width(40.dp),
              textAlign = TextAlign.End,
              color = if (selectedModelName.isNullOrEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
              } else {
                MaterialTheme.colorScheme.onSurface
              }
            )
          }
          
          Text(
            text = if (selectedModelName.isNullOrEmpty()) {
              "Select a model to adjust temperature"
            } else if (defaultTemperature != null) {
              "Temperature controls the model's creativity: lower values yield focused output, higher values yield more creative responses (Default: ${(defaultTemperature * 100).toInt()})."
            } else {
              "Lower = more focused, Higher = more creative"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}

@Composable
private fun BookInfoDialog(
  book: Book,
  onDismiss: () -> Unit
) {
  val uriHandler = LocalUriHandler.current
  val context = LocalContext.current

  // Parse tags from the book
  val parsedTags = remember(book.tags) {
    BookTagUtils.parseBookTags(book.tags)
  }
  
  // State for ordered tags
  var orderedTags by remember { mutableStateOf(parsedTags) }
  
  // Load tags from file and order them
  LaunchedEffect(parsedTags) {
    if (parsedTags.isNotEmpty()) {
      // Load tags from file to get the ordering
      BookTagUtils.loadTagsFromFile(context)
      
      // Log the old and new order
      android.util.Log.d("BookInfoDialog", "Original tag order: $parsedTags")
      val newOrderedTags = BookTagUtils.orderTagsByFileOrder(parsedTags)
      android.util.Log.d("BookInfoDialog", "New ordered tags: $newOrderedTags")
      
      orderedTags = newOrderedTags
    }
  }
  
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { 
      Text(
        text = "Book Information",
        style = MaterialTheme.typography.headlineSmall
      ) 
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        // Title
        Text(
          text = "Title",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = book.title,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Author
        Text(
          text = "Author",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = book.author,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Date if available
        book.date?.let { date ->
          Text(
            text = "Date",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }
        
        // Description
        if (book.description.isNotEmpty()) {
          Text(
            text = "Description",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = book.description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }
        
        // Tags
        if (!book.tags.isNullOrEmpty() && orderedTags.isNotEmpty()) {
          Text(
            text = "Tags",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            orderedTags.forEach { tag ->
              val tagValue = BookTagUtils.getTagDisplayValue(tag, full=true)
              val tagColor = BookTagUtils.getTagColor(tag, on = false, container = true) //bg
              val onTagColor = BookTagUtils.getTagColor(tag, on = true, container = true) //fg
              
              // Tag chip
              Box(
                modifier = Modifier
                  .background(
                    color = tagColor,
                    shape = RoundedCornerShape(12.dp)
                  )
                  .border(
                    width = 1.dp,
                    color = tagColor,
                    shape = RoundedCornerShape(12.dp)
                  )
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = tagValue,
                  style = MaterialTheme.typography.labelSmall,
                  color = onTagColor
                )
              }
            }
          }
          
          Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Source
        book.source?.let { source ->
          Text(
            text = "Source",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = source,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }
        
        // Source link - only show if not null or empty
        if (!book.source_link.isNullOrEmpty()) {
          Text(
            text = "Source Link",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = book.source_link,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
              .padding(bottom = 12.dp)
              .clickable { 
                try {
                  uriHandler.openUri(book.source_link)
                } catch (e: Exception) {
                  // Handle invalid URL gracefully
                  android.util.Log.e("BookInfoDialog", "Failed to open URL: ${book.source_link}", e)
                }
              }
          )
        }
        
        // Book metadata
        Text(
          text = "Book Details",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Total Pages: ${book.totalPages ?: "N/A"}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Words per Page: ${book.wordsPerPage ?: "N/A"}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Last Read: Page ${book.lastReadPage + 1}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

// Placeholder function to convert page number to TOC section
private fun pageToTocSection(pageNumber: Int): String {
  // TODO: Implement actual logic to read toc.json and map page to chapter
  return "Chapter ${(pageNumber / 10) + 1}"
}

private fun loadSinglePage(context: android.content.Context, bookId: String, pageNumber: Int): BookPage? {
  return try {
    val gson = Gson()
    val libraryDir = File(context.filesDir, "library")
    val bookDir = File(libraryDir, bookId)
    
    if (!bookDir.exists() || !bookDir.isDirectory) {
      return null
    }
    
    val pageFile = File(bookDir, "page_$pageNumber.json")
    if (!pageFile.exists()) {
      return null
    }
    
    val pageData = gson.fromJson(pageFile.readText(), BookPage::class.java)
    return pageData
  } catch (e: Exception) {
    return null
  }
}

@Composable
private fun PageInputDialog(
  currentPage: Int,
  totalPages: Int,
  onPageSelected: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { 
    mutableStateOf(
      TextFieldValue(
        text = currentPage.toString(),
        selection = TextRange(0, currentPage.toString().length)
      )
    )
  }
  val focusRequester = remember { FocusRequester() }
  
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }
  
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { 
      Text(
        text = "Go to Page",
        style = MaterialTheme.typography.headlineSmall
      ) 
    },
    text = {
      Column {
        Text(
          text = "Enter page number (1-$totalPages):",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        TextField(
          value = textFieldValue,
          onValueChange = { newValue ->
            // Only allow digits
            if (newValue.text.all { it.isDigit() } || newValue.text.isEmpty()) {
              textFieldValue = newValue
            }
          },
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              val page = textFieldValue.text.toIntOrNull()
              if (page != null && page in 1..totalPages) {
                onPageSelected(page)
              }
            }
          ),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val page = textFieldValue.text.toIntOrNull()
          if (page != null && page in 1..totalPages) {
            onPageSelected(page)
          }
        }
      ) {
        Text("Go")
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
private fun IntroDialog(
  onDismiss: () -> Unit
) {
  val uriHandler = LocalUriHandler.current
  
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { 
      Text(
        text = "Welcome to SparkReader",
        style = MaterialTheme.typography.headlineSmall
      ) 
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "The first few pages contain Project Gutenberg license, table of contents, etc.",
          style = MaterialTheme.typography.bodyMedium
        )
        
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "As per ",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "PG License",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
              uriHandler.openUri("https://www.gutenberg.org/policy/license.html")
            }
          )
          Text(
            text = ", no changes are allowed to the ebook contents, so we kept it as is. Adding a \"Skip Intro\" button is in our roadmap.",
            style = MaterialTheme.typography.bodyMedium
          )
        }
        
        Text(
          text = "Tip: Tap the left and right edges of the screen to navigate between pages.",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.primary
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Got it!")
      }
    }
  )
}

@Composable
private fun BackConfirmationDialog(
  onAbortAndLeave: () -> Unit,
  onStay: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { 
      Text(
        text = "AI is generating response",
        style = MaterialTheme.typography.headlineSmall
      ) 
    },
    text = {
      Text(
        text = "The AI is currently generating a response. Do you want to stop the generation and leave, or stay on this page?",
        style = MaterialTheme.typography.bodyMedium
      )
    },
    confirmButton = {
      Button(
        onClick = onAbortAndLeave,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error
        )
      ) {
        Text("Stop & Leave")
      }
    },
    dismissButton = {
      TextButton(onClick = onStay) {
        Text("Stay")
      }
    }
  )
}
