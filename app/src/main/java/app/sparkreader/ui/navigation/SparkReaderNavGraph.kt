/*
 * Copyright 2025 Google LLC
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

/*
* Modifications Copyright 2025 The SparkReader Creator
* Licensed under the Apache License, Version 2.0 (the "License");
* Changes relative to the original are documented in docs/upstream/google-ai-edge-gallery/CHANGES.md
* (original source: https://github.com/google-ai-edge/gallery)
* (This file was originally named GalleryNavGraph.kt)
*/

package app.sparkreader.ui.navigation

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.sparkreader.data.Model
import app.sparkreader.data.TASK_LLM_ASK_IMAGE
import app.sparkreader.data.Task
import app.sparkreader.data.TaskType
import app.sparkreader.ui.newhome.NewHomeScreen
import app.sparkreader.ui.newhome.NewHomeViewModel
import app.sparkreader.ui.newhome.Book
import app.sparkreader.ui.bookdetail.BookDetailScreen
import app.sparkreader.ui.importbook.ImportBookScreen
import app.sparkreader.ui.helpfeedback.HelpFeedbackScreen
import app.sparkreader.ui.about.AboutScreen
import app.sparkreader.ui.settings.SettingsScreen
import app.sparkreader.ui.createbook.CreateBookScreen
import app.sparkreader.ui.demo.DemoScreen
import app.sparkreader.ui.modelmanager.ModelManager
import app.sparkreader.ui.modelmanager.ModelManagerViewModel

private const val TAG = "AGSparkReaderNavGraph"
private const val ROUTE_NEW_HOME = "new_home"
private const val ENTER_ANIMATION_DURATION_MS = 500
private val ENTER_ANIMATION_EASING = EaseOutExpo
private const val ENTER_ANIMATION_DELAY_MS = 100

private const val EXIT_ANIMATION_DURATION_MS = 500
private val EXIT_ANIMATION_EASING = EaseOutExpo

private fun enterTween(): FiniteAnimationSpec<IntOffset> {
  return tween(
    ENTER_ANIMATION_DURATION_MS,
    easing = ENTER_ANIMATION_EASING,
    delayMillis = ENTER_ANIMATION_DELAY_MS,
  )
}

private fun exitTween(): FiniteAnimationSpec<IntOffset> {
  return tween(EXIT_ANIMATION_DURATION_MS, easing = EXIT_ANIMATION_EASING)
}

private fun AnimatedContentTransitionScope<*>.slideEnter(): EnterTransition {
  return slideIntoContainer(
    animationSpec = enterTween(),
    towards = AnimatedContentTransitionScope.SlideDirection.Left,
  )
}

private fun AnimatedContentTransitionScope<*>.slideExit(): ExitTransition {
  return slideOutOfContainer(
    animationSpec = exitTween(),
    towards = AnimatedContentTransitionScope.SlideDirection.Right,
  )
}

/** Navigation routes. */
@Composable
fun SparkReaderNavHost(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  modelManagerViewModel: ModelManagerViewModel = hiltViewModel(),
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  var showModelManager by remember { mutableStateOf(false) }
  var showBookDetail by remember { mutableStateOf(false) }
  var showImportBook by remember { mutableStateOf(false) }
  var importBookRefreshKey by remember { mutableStateOf(0) }
  var showHelpFeedback by remember { mutableStateOf(false) }
  var showAbout by remember { mutableStateOf(false) }
  var showSettings by remember { mutableStateOf(false) }
  var showCreateBook by remember { mutableStateOf(false) }
  var showDemo by remember { mutableStateOf(false) }
  var bookToEdit by remember { mutableStateOf<Book?>(null) }
  var selectedBook by remember { mutableStateOf<Book?>(null) }
  var pickedTask by remember { mutableStateOf<Task?>(null) }
  var modelManagerSource by remember { mutableStateOf<String?>(null) }
  var isNavigatingForward by remember { mutableStateOf(true) }
  var bookCreatedMessage by remember { mutableStateOf<String?>(null) }

  // Track whether app is in foreground.
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START,
        Lifecycle.Event.ON_RESUME -> {
          modelManagerViewModel.setAppInForeground(foreground = true)
        }
        Lifecycle.Event.ON_STOP,
        Lifecycle.Event.ON_PAUSE -> {
          modelManagerViewModel.setAppInForeground(foreground = false)
        }
        else -> {
          /* Do nothing for other events */
        }
      }
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // Create the ViewModel outside AnimatedVisibility to ensure it persists
  val newHomeViewModel: NewHomeViewModel = hiltViewModel()
  
  // Show new homescreen by default
  AnimatedVisibility(
    visible = !showModelManager && !showBookDetail && !showImportBook && !showHelpFeedback && !showAbout && !showSettings && !showCreateBook && !showDemo,
    enter = slideInHorizontally(initialOffsetX = { -it }),
    exit = slideOutHorizontally(targetOffsetX = { -it }),
  ) {
    NewHomeScreen(
      onRowClicked = { book ->
        isNavigatingForward = true
        bookCreatedMessage = null  // Clear message when navigating away
        selectedBook = book
        showBookDetail = true
      },
      onImportBookClicked = {
        bookCreatedMessage = null  // Clear message when navigating away
        importBookRefreshKey++  // Increment key to force refresh
        showImportBook = true
      },
      onCreateBookClicked = {
        isNavigatingForward = true
        bookToEdit = null
        bookCreatedMessage = null  // Clear any existing message
        showCreateBook = true
      },
      onEditBookClicked = { book ->
        isNavigatingForward = true
        bookToEdit = book
        bookCreatedMessage = null  // Clear any existing message
        showCreateBook = true
      },
      onHelpFeedbackClicked = {
        bookCreatedMessage = null  // Clear message when navigating away
        showHelpFeedback = true
      },
      onAboutClicked = {
        bookCreatedMessage = null  // Clear message when navigating away
        showAbout = true
      },
      onSettingsClicked = {
        isNavigatingForward = true
        bookCreatedMessage = null  // Clear message when navigating away
        showSettings = true
      },
      onDemoClicked = {
        bookCreatedMessage = null  // Clear message when navigating away
        showDemo = true
      },
      onOldHomescreenClicked = {
        // Handle navigation to old homescreen if needed
        // For now, we can leave this empty or navigate somewhere else
      },
      bookCreatedMessage = bookCreatedMessage,
      onBookCreatedMessageShown = {
        bookCreatedMessage = null
      },
      viewModel = newHomeViewModel
    )
  }

  // Show book detail screen
  AnimatedVisibility(
    visible = showBookDetail,
    enter = if (isNavigatingForward) slideInHorizontally(initialOffsetX = { it }) else slideInHorizontally(initialOffsetX = { -it }),
    exit = slideOutHorizontally(targetOffsetX = { -it }),
  ) {
    selectedBook?.let { book ->
      BookDetailScreen(
        book = book,
        onNavigateUp = {
          isNavigatingForward = false
          showBookDetail = false
          selectedBook = null
        },
        onNavigateToModelManager = {
          isNavigatingForward = true
          showBookDetail = false
          pickedTask = TASK_LLM_ASK_IMAGE
          modelManagerSource = "book_detail"
          showModelManager = true
        }
      )
    }
  }

  // Show import book screen
  AnimatedVisibility(
    visible = showImportBook,
    enter = slideInHorizontally(initialOffsetX = { it }),
    exit = slideOutHorizontally(targetOffsetX = { it }),
  ) {
    BackHandler(enabled = true) {
      showImportBook = false
    }
    ImportBookScreen(
      onNavigateUp = {
        showImportBook = false
      },
      onNavigateToSettings = {
        isNavigatingForward = true
        showSettings = true
        // Don't hide import book screen so we can return to it
      },
      refreshKey = importBookRefreshKey
    )
  }

  // Show help and feedback screen
  AnimatedVisibility(
    visible = showHelpFeedback,
    enter = slideInHorizontally(initialOffsetX = { it }),
    exit = slideOutHorizontally(targetOffsetX = { it }),
  ) {
    BackHandler(enabled = true) {
      showHelpFeedback = false
    }
    HelpFeedbackScreen(
      onNavigateUp = {
        showHelpFeedback = false
      },
      onNavigateToSettings = {
        isNavigatingForward = true
        showHelpFeedback = false
        showSettings = true
      }
    )
  }

  // Show about screen
  AnimatedVisibility(
    visible = showAbout,
    enter = slideInHorizontally(initialOffsetX = { it }),
    exit = slideOutHorizontally(targetOffsetX = { it }),
  ) {
    BackHandler(enabled = true) {
      showAbout = false
    }
    AboutScreen(
      onNavigateUp = {
        showAbout = false
      }
    )
  }

  // Show settings screen
  AnimatedVisibility(
    visible = showSettings,
    enter = if (isNavigatingForward) slideInHorizontally(initialOffsetX = { it }) else slideInHorizontally(initialOffsetX = { -it }),
    exit = if (isNavigatingForward) slideOutHorizontally(targetOffsetX = { -it }) else slideOutHorizontally(targetOffsetX = { it }),
  ) {
    SettingsScreen(
      onNavigateBack = {
        isNavigatingForward = false
        showSettings = false
        // If import book screen is still visible, we came from there
        if (showImportBook) {
          // Increment refresh key when returning from settings to force library check
          importBookRefreshKey++
        }
      },
      onModelManagerClicked = {
        isNavigatingForward = true
        pickedTask = TASK_LLM_ASK_IMAGE
        modelManagerSource = "settings"
        showModelManager = true
        showSettings = false
      },
      onThemeSelectionClicked = {
        showSettings = false
      },
      onImportBookClicked = {
        isNavigatingForward = true
        showSettings = false
        importBookRefreshKey++  // Increment key to force refresh
        showImportBook = true
      },
      fromImportBook = showImportBook
    )
  }

  // Show create book screen
  AnimatedVisibility(
    visible = showCreateBook,
    enter = if (isNavigatingForward) slideInHorizontally(initialOffsetX = { it }) else slideInHorizontally(initialOffsetX = { -it }),
    exit = if (isNavigatingForward) slideOutHorizontally(targetOffsetX = { -it }) else slideOutHorizontally(targetOffsetX = { it }),
  ) {
    CreateBookScreen(
      onBackPressed = {
        isNavigatingForward = false
        showCreateBook = false
        bookToEdit = null
      },
      onBookCreated = { success ->
        isNavigatingForward = false
        showCreateBook = false
        if (success) {
          bookCreatedMessage = if (bookToEdit != null) "Book updated successfully!" else "Book created successfully!"
        }
        bookToEdit = null
      },
      bookToEdit = bookToEdit,
      onNavigateToModelManager = {
        isNavigatingForward = true
        showCreateBook = false
        pickedTask = TASK_LLM_ASK_IMAGE
        modelManagerSource = "create_book"
        showModelManager = true
      }
    )
  }

  // Show demo screen
  AnimatedVisibility(
    visible = showDemo,
    enter = slideInHorizontally(initialOffsetX = { it }),
    exit = slideOutHorizontally(targetOffsetX = { it }),
  ) {
    BackHandler(enabled = true) {
      showDemo = false
    }
    DemoScreen(
      onBackClicked = {
        showDemo = false
      }
    )
  }

  // Model manager.
  AnimatedVisibility(
    visible = showModelManager,
    enter = if (isNavigatingForward) slideInHorizontally(initialOffsetX = { it }) else slideInHorizontally(initialOffsetX = { -it }),
    exit = if (isNavigatingForward) slideOutHorizontally(targetOffsetX = { -it }) else slideOutHorizontally(targetOffsetX = { it }),
  ) {
    val curPickedTask = pickedTask
    if (curPickedTask != null) {
      ModelManager(
        viewModel = modelManagerViewModel,
        task = curPickedTask,
        onModelClicked = { model ->
          // Model click handling removed - no task screens available
        },
        navigateUp = { 
          isNavigatingForward = false
          showModelManager = false
          modelManagerSource?.let { source ->
            when (source) {
              "settings" -> {
                showSettings = true
              }
              "book_detail" -> {
                showBookDetail = true
              }
              "create_book" -> {
                showCreateBook = true
              }
              "notification", "deeplink" -> {
                // When coming from notification or deep link, go to new homescreen
              }
              else -> {
                // Default to new homescreen
              }
            }
            modelManagerSource = null
          }
        },
      )
    }
  }

  NavHost(
    navController = navController,
    // Default to open new home screen.
    startDestination = ROUTE_NEW_HOME,
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None },
    modifier = modifier.zIndex(1f),
  ) {
    // New home screen route
    composable(route = ROUTE_NEW_HOME) { Text("") }
  }

  // Handle incoming intents for deep links and navigation extras
  val activity = androidx.activity.compose.LocalActivity.current
  val intent = activity?.intent
  
  // Check for navigation extra first
  val navigateTo = intent?.getStringExtra("navigate_to")
  if (navigateTo == "model_manager") {
    // Clear the extra to prevent re-navigation
    intent.removeExtra("navigate_to")
    
    // Navigate to model manager
    if (!showModelManager) {
      Log.d(TAG, "Navigating to model manager from notification")
      isNavigatingForward = true
      pickedTask = TASK_LLM_ASK_IMAGE
      modelManagerSource = "notification"
      showModelManager = true
    }
  } else {
    // Handle deep links
    val data = intent?.data
    if (data != null) {
      intent.data = null
      Log.d(TAG, "navigation link clicked: $data")
      
      if (data.toString() == "app.sparkreader://modelmanager") {
        // Navigate to model manager
        if (!showModelManager) {
          Log.d(TAG, "Navigating to model manager from deep link")
          isNavigatingForward = true
          pickedTask = TASK_LLM_ASK_IMAGE
          modelManagerSource = "deeplink"
          showModelManager = true
        }
      } else if (data.toString().startsWith("app.sparkreader://model/")) {
        val modelName = data.pathSegments.last()
        // Deep link model handling removed - no task screens available
      }
    }
  }
}

fun navigateToTaskScreen(
  navController: NavHostController,
  taskType: TaskType,
  model: Model? = null,
) {
  // Currently no task screens are implemented
  // This function is kept for future extensibility
}
