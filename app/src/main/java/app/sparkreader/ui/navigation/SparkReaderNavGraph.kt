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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

private const val TAG = "AGSparkReaderNavGraph"
private const val ROUTE_NEW_HOME = "new_home"
private const val ROUTE_BOOK_DETAIL = "book_detail"
private const val ROUTE_IMPORT_BOOK = "import_book"
private const val ROUTE_HELP_FEEDBACK = "help_feedback"
private const val ROUTE_ABOUT = "about"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_CREATE_BOOK = "create_book"
private const val ROUTE_DEMO = "demo"

private const val ENTER_ANIMATION_DURATION_MS = 800
private val ENTER_ANIMATION_EASING = EaseOutExpo

private fun AnimatedContentTransitionScope<*>.slideEnter(): EnterTransition {
  return slideIntoContainer(
    animationSpec = tween(ENTER_ANIMATION_DURATION_MS, easing = ENTER_ANIMATION_EASING),
    towards = AnimatedContentTransitionScope.SlideDirection.Left,
  )
}

private fun AnimatedContentTransitionScope<*>.slideExit(): ExitTransition {
  return slideOutOfContainer(
    animationSpec = tween(ENTER_ANIMATION_DURATION_MS, easing = ENTER_ANIMATION_EASING),
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

  NavHost(
    navController = navController,
    startDestination = ROUTE_NEW_HOME,
    enterTransition = { slideEnter() },
    exitTransition = { slideExit() },
    modifier = modifier,
  ) {
    composable(route = ROUTE_NEW_HOME) {
      val newHomeViewModel: NewHomeViewModel = hiltViewModel()
      NewHomeScreen(
        onRowClicked = { book ->
          navController.navigate("$ROUTE_BOOK_DETAIL/${book.id}")
        },
        onImportBookClicked = {
          navController.navigate(ROUTE_IMPORT_BOOK)
        },
        onCreateBookClicked = {
          navController.navigate(ROUTE_CREATE_BOOK)
        },
        onEditBookClicked = { book ->
          navController.navigate("$ROUTE_CREATE_BOOK/${book.id}")
        },
        onHelpFeedbackClicked = {
          navController.navigate(ROUTE_HELP_FEEDBACK)
        },
        onAboutClicked = {
          navController.navigate(ROUTE_ABOUT)
        },
        onSettingsClicked = {
          navController.navigate(ROUTE_SETTINGS)
        },
        onDemoClicked = {
          navController.navigate(ROUTE_DEMO)
        },
        onOldHomescreenClicked = {
          // Handle navigation to old homescreen if needed
        },
        bookCreatedMessage = null,
        onBookCreatedMessageShown = { },
        viewModel = newHomeViewModel
      )
    }

    composable(
      route = "$ROUTE_BOOK_DETAIL/{bookId}",
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() }
    ) { backStackEntry ->
      val bookId = backStackEntry.arguments?.getString("bookId")
      val context = LocalContext.current
      
      // Load book data from file system
      val book = remember(bookId) {
        loadBookFromFileSystem(context, bookId ?: "")
      }
      
      BookDetailScreen(
        book = book,
        onNavigateUp = {
          navController.popBackStack()
        },
        onNavigateToModelManager = {
          navController.navigate(ROUTE_SETTINGS)
        }
      )
    }

    composable(
      route = ROUTE_IMPORT_BOOK,
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() }
    ) {
      ImportBookScreen(
        onNavigateUp = {
          navController.popBackStack()
        },
        onNavigateToSettings = {
          navController.navigate(ROUTE_SETTINGS)
        },
        refreshKey = 0
      )
    }

    composable(
      route = ROUTE_HELP_FEEDBACK,
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() }
    ) {
      HelpFeedbackScreen(
        onNavigateUp = {
          navController.popBackStack()
        },
        onNavigateToSettings = {
          navController.navigate(ROUTE_SETTINGS)
        }
      )
    }

    composable(
      route = ROUTE_ABOUT,
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() }
    ) {
      AboutScreen(
        onNavigateUp = {
          navController.popBackStack()
        }
      )
    }

    composable(
      route = ROUTE_SETTINGS,
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() }
    ) {
      SettingsScreen(
        onNavigateBack = {
          navController.popBackStack()
        },
        onThemeSelectionClicked = {
          navController.popBackStack()
        },
        onImportBookClicked = {
          navController.navigate(ROUTE_IMPORT_BOOK)
        },
        fromImportBook = false
      )
    }

    composable(
      route = ROUTE_CREATE_BOOK,
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() }
    ) {
      CreateBookScreen(
        onBackPressed = {
          navController.popBackStack()
        },
        onBookCreated = { success ->
          if (success) {
            navController.popBackStack()
          }
        },
        bookToEdit = null,
        onNavigateToModelManager = {
          navController.navigate(ROUTE_SETTINGS)
        }
      )
    }

    composable(
      route = "$ROUTE_CREATE_BOOK/{bookId}",
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() }
    ) { backStackEntry ->
      val bookId = backStackEntry.arguments?.getString("bookId")?.toIntOrNull()
      // TODO: Get book by ID from repository for editing
      val bookToEdit = null // Placeholder
      
      CreateBookScreen(
        onBackPressed = {
          navController.popBackStack()
        },
        onBookCreated = { success ->
          if (success) {
            navController.popBackStack()
          }
        },
        bookToEdit = bookToEdit,
        onNavigateToModelManager = {
          navController.navigate(ROUTE_SETTINGS)
        }
      )
    }

    composable(
      route = ROUTE_DEMO,
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() }
    ) {
      DemoScreen(
        onBackClicked = {
          navController.popBackStack()
        }
      )
    }
  }

  // Handle incoming intents for deep links and navigation extras
  val activity = androidx.activity.compose.LocalActivity.current
  val intent = activity?.intent
  
  // Check for navigation extra first
  val navigateTo = intent?.getStringExtra("navigate_to")
  if (navigateTo == "model_manager") {
    // Clear the extra to prevent re-navigation
    intent.removeExtra("navigate_to")
    
    // Navigate to settings (where model manager is now integrated)
    Log.d(TAG, "Navigating to settings from notification")
    navController.navigate(ROUTE_SETTINGS)
  } else {
    // Handle deep links
    val data = intent?.data
    if (data != null) {
      intent.data = null
      Log.d(TAG, "navigation link clicked: $data")
      
      if (data.toString() == "app.sparkreader://modelmanager") {
        // Navigate to settings (where model manager is now integrated)
        Log.d(TAG, "Navigating to settings from deep link")
        navController.navigate(ROUTE_SETTINGS)
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

private fun loadBookFromFileSystem(context: android.content.Context, bookId: String): Book {
  return try {
    val gson = Gson()
    
    // Load from the same books.json file that ImportBookViewModel uses
    val userBooksFile = File(context.filesDir, "books.json")
    if (userBooksFile.exists()) {
      val jsonContent = userBooksFile.readText()
      val type = object : com.google.gson.reflect.TypeToken<List<Book>>() {}.type
      val books: List<Book> = gson.fromJson(jsonContent, type) ?: emptyList()
      
      // Find the book by ID
      val book = books.find { it.id == bookId }
      if (book != null) {
        Log.d(TAG, "Loaded book from books.json: ${book.title}")
        return book
      }
    }
    
    // Fallback: create placeholder book
    Log.d(TAG, "Book not found in books.json for ID: $bookId")
    return createPlaceholderBook(bookId)
  } catch (e: Exception) {
    Log.e(TAG, "Error loading book from books.json: $bookId", e)
    return createPlaceholderBook(bookId)
  }
}

private fun createPlaceholderBook(bookId: String): Book {
  return Book(
    id = bookId,
    title = "Book Not Found",
    author = "Unknown Author",
    description = "Could not load book data",
    libraryId = bookId,
    totalPages = 1,
    lastReadPage = 0
  )
}

private fun countPagesInDirectory(bookDir: File): Int {
  return try {
    val pageFiles = bookDir.listFiles { file ->
      file.name.startsWith("page_") && file.name.endsWith(".json")
    }
    pageFiles?.size ?: 0
  } catch (e: Exception) {
    0
  }
}
