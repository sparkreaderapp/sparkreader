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

package app.sparkreader.ui.newhome

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.sparkreader.BuildConfig
import app.sparkreader.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlinx.coroutines.launch

private enum class ImportOptionType {
  LIBRARY,
  CREATE_NEW,
  IMPORT_FILE,
  IMPORT_WEB
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewHomeScreen(
  onRowClicked: (Book) -> Unit,
  onOldHomescreenClicked: () -> Unit,
  onImportBookClicked: () -> Unit,
  onCreateBookClicked: () -> Unit,
  onEditBookClicked: (Book) -> Unit,
  onHelpFeedbackClicked: () -> Unit,
  onAboutClicked: () -> Unit,
  onSettingsClicked: () -> Unit,
  onDemoClicked: () -> Unit,
  bookCreatedMessage: String? = null,
  onBookCreatedMessageShown: () -> Unit = {},
  modifier: Modifier = Modifier,
  viewModel: NewHomeViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  var books by remember { mutableStateOf<List<Book>>(emptyList()) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedBook by remember { mutableStateOf<Book?>(null) }
  var showBottomSheet by remember { mutableStateOf(false) }
  val bottomSheetState = rememberModalBottomSheetState()
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  var showOverflowMenu by remember { mutableStateOf(false) }
  var showImportBottomSheet by remember { mutableStateOf(false) }
  var showDeleteConfirmation by remember { mutableStateOf(false) }
  var bookToDelete by remember { mutableStateOf<Book?>(null) }
  
  // Use persistent scroll state
  val lazyListState = rememberPersistentLazyListState(key = "new_home_screen")

  // Clear any existing snackbars when entering the screen
  LaunchedEffect(Unit) {
    snackbarHostState.currentSnackbarData?.dismiss()
  }

  // Load books from user's books.json
  LaunchedEffect(Unit) {
    books = loadBooksFromDataFolder(context)
  }

  // Show book created message if present
  LaunchedEffect(bookCreatedMessage) {
    bookCreatedMessage?.let { message ->
      coroutineScope.launch {
        val result = snackbarHostState.showSnackbar(
          message = message,
          actionLabel = "Dismiss",
          duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
          onBookCreatedMessageShown()
        }
      }
      // Reload books to show the newly created book
      books = loadBooksFromDataFolder(context)
    }
  }

  // Filter books based on search query
  val filteredBooks = remember(books, searchQuery) {
    if (searchQuery.isBlank()) {
      books
    } else {
      books.filter { book ->
        book.title.contains(searchQuery, ignoreCase = true) ||
        book.author.contains(searchQuery, ignoreCase = true) ||
        book.description.contains(searchQuery, ignoreCase = true)
      }
    }
  }

  // Handle delete action
  fun deleteBook(book: Book) {
    coroutineScope.launch {
      // Update UI immediately
      books = books.filter { it.id != book.id }
      
      // Save the updated books list
      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        saveBooksToDataFolder(context, books)
        
        // Delete the actual files
        val libraryPagesDir = File(context.filesDir, "library")
        val bookDirId = book.libraryId ?: book.id.toString()
        val bookDir = File(libraryPagesDir, bookDirId)
        if (bookDir.exists()) {
          bookDir.deleteRecursively()
          android.util.Log.d("NewHomeScreen", "Deleted paginated data for book ID: $bookDirId")
        }
      }
      
      // Show confirmation snackbar with dismiss button
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(
        //message = "\"${book.title}\" removed from your library",
        message = "Book deleted from your library",
        actionLabel = "Dismiss",
        duration = SnackbarDuration.Long
      )
    }
  }

  // Bottom sheet for book actions
  if (showBottomSheet && selectedBook != null) {
    ModalBottomSheet(
      onDismissRequest = { 
        showBottomSheet = false
        selectedBook = null
      },
      sheetState = bottomSheetState
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 24.dp)
      ) {
        Text(
          text = selectedBook!!.title,
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
          text = "by ${selectedBook!!.author}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 24.dp)
        )

        // Edit action (only for user-created books)
        if (selectedBook!!.source == "user") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                val bookToEdit = selectedBook!!
                showBottomSheet = false
                selectedBook = null
                onEditBookClicked(bookToEdit)
              }
              .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit",
              modifier = Modifier.size(24.dp),
              tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
              text = "Edit",
              style = MaterialTheme.typography.bodyLarge
            )
          }
        }

        // Delete action
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              bookToDelete = selectedBook
              showDeleteConfirmation = true
              // Dismiss bottom sheet
              showBottomSheet = false
              selectedBook = null
            }
            .padding(vertical = 16.dp, horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Start
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.error
          )
          Spacer(modifier = Modifier.width(16.dp))
          Text(
            text = "Delete",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
          )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // Import options bottom sheet
  if (showImportBottomSheet) {
    ModalBottomSheet(
      onDismissRequest = { showImportBottomSheet = false },
      sheetState = bottomSheetState
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 24.dp)
      ) {
        // First row of import options
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          ImportOptionButton(
            drawableRes = if (isDarkTheme()) R.drawable.logo_library_dark else R.drawable.logo_library_light,
            label = "Starter\nLibrary",
            optionType = ImportOptionType.LIBRARY,
            recolor = false,
            onClick = { 
              showImportBottomSheet = false
              onImportBookClicked()
            }
          )

          ImportOptionButton(
            icon = Icons.Default.Edit,
            label = "Create\nNew",
            optionType = ImportOptionType.CREATE_NEW,
            enabled = false,
            onClick = {
              showImportBottomSheet = false
              coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                  message = "Coming soon!",
                  actionLabel = "Dismiss",
                  duration = SnackbarDuration.Long
                )
              }
            }
          )

          ImportOptionButton(
            icon = Icons.Default.PictureAsPdf,
            label = "File\nImport",
            optionType = ImportOptionType.IMPORT_FILE,
            enabled = false,
            onClick = {
              showImportBottomSheet = false
              coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                  message = "Coming soon!",
                  actionLabel = "Dismiss",
                  duration = SnackbarDuration.Long
                )
              }
            }
          )

          ImportOptionButton(
            icon = Icons.Default.Web,
            label = "Web\nImport",
            optionType = ImportOptionType.IMPORT_WEB,
            enabled = false,
            onClick = {
              showImportBottomSheet = false
              coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                  message = "Coming soon!",
                  actionLabel = "Dismiss",
                  duration = SnackbarDuration.Long
                )
              }
            }
          )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
      }
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      CenterAlignedTopAppBar(
        title = { 
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            if ("SparkReader" == stringResource(R.string.app_name)) {
              Icon(
                painterResource(if (isDarkTheme()) R.drawable.logo_dark else R.drawable.logo_light),
                contentDescription = "SparkReader",
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
            }
            Text("SparkReader")
          }
        },
        actions = {
          IconButton(
            onClick = { showOverflowMenu = true },
            modifier = Modifier.padding(end = 8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "More options",
              modifier = Modifier.size(24.dp),
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
          
          DropdownMenu(
            expanded = showOverflowMenu,
            onDismissRequest = { showOverflowMenu = false },
            modifier = Modifier.padding(end = 16.dp)
          ) {
            // Only show Demo in debug builds
            if (BuildConfig.DEBUG) {
              DropdownMenuItem(
                text = { 
                  Text(
                    "Demo",
                    style = MaterialTheme.typography.bodyLarge
                  ) 
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                  )
                },
                onClick = {
                  showOverflowMenu = false
                  onDemoClicked()
                }
              )
            }
            
            // DropdownMenuItem(
            //   text = { 
            //     Text(
            //       "Our Vision",
            //       style = MaterialTheme.typography.bodyLarge
            //     ) 
            //   },
            //   leadingIcon = {
            //     Icon(
            //       imageVector = Icons.Default.Visibility,
            //       contentDescription = null,
            //       tint = MaterialTheme.colorScheme.onSurface
            //     )
            //   },
            //   onClick = {
            //     showOverflowMenu = false
            //     coroutineScope.launch {
            //       snackbarHostState.currentSnackbarData?.dismiss()
            //       snackbarHostState.showSnackbar(
            //         message = "Our Vision - Coming soon!",
            //         actionLabel = "Dismiss",
            //         duration = SnackbarDuration.Long
            //       )
            //     }
            //   }
            // )
            
            DropdownMenuItem(
              text = { 
                Text(
                  "Help and Feedback",
                  style = MaterialTheme.typography.bodyLarge
                ) 
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Help,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurface
                )
              },
              onClick = {
                showOverflowMenu = false
                onHelpFeedbackClicked()
              }
            )
            
            DropdownMenuItem(
              text = { 
                Text(
                  "About the App",
                  style = MaterialTheme.typography.bodyLarge
                ) 
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurface
                )
              },
              onClick = {
                showOverflowMenu = false
                onAboutClicked()
              }
            )
            
            DropdownMenuItem(
              text = { 
                Text(
                  "Settings",
                  style = MaterialTheme.typography.bodyLarge
                ) 
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurface
                )
              },
              onClick = {
                showOverflowMenu = false
                onSettingsClicked()
              }
            )
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showImportBottomSheet = true },
        modifier = Modifier.padding(16.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Import Book",
          modifier = Modifier.size(32.dp)
        )
      }
    },
    modifier = modifier
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(start=16.dp, top=2.dp, end=16.dp, bottom=2.dp)
    ) {
      // Search bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search the library...") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search"
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear search"
              )
            }
          }
        },
        singleLine = true
      )
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Book list
      LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(
          items = filteredBooks,
          key = { book -> book.id }
        ) { book ->
          BookRowItem(
            book = book,
            onClicked = { 
              onRowClicked(book) 
            },
            onLongClicked = {
              selectedBook = book
              showBottomSheet = true
            }
          )
        }
      }
    }
  }
  
  // Delete confirmation dialog
  if (showDeleteConfirmation && bookToDelete != null) {
    AlertDialog(
      onDismissRequest = { 
        showDeleteConfirmation = false
        bookToDelete = null
      },
      title = { Text("Delete Book?") },
      text = { 
        Text("Are you sure you want to delete \"${bookToDelete!!.title}\"? This will also delete all associated chats.")
      },
      confirmButton = {
        Button(
          onClick = {
            val book = bookToDelete!!
            showDeleteConfirmation = false
            bookToDelete = null
            deleteBook(book)
          },
          colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
          )
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { 
            showDeleteConfirmation = false
            bookToDelete = null
          }
        ) {
          Text("Cancel")
        }
      }
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookRowItem(
  book: Book,
  onClicked: () -> Unit,
  onLongClicked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val authorText = book.author?.takeIf { it.isNotBlank() }?.let { "by $it" }
  val dateText = book.date?.let { "($it)" }
  val subtitle = listOfNotNull(authorText, dateText).joinToString(" ")

  Card(
    modifier = modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = onClicked,
        onLongClick = onLongClicked
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Book,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )
      
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = book.title,
          style = MaterialTheme.typography.titleMedium
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      
      Icon(
        imageVector = Icons.Default.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

private fun loadBooksFromDataFolder(context: Context): List<Book> {
  return try {
    val booksFile = File(context.filesDir, "books.json")
    
    // Create empty file if it doesn't exist
    if (!booksFile.exists()) {
      booksFile.writeText("[]")
    }
    
    val json = booksFile.readText()
    val gson = Gson()
    val bookListType = object : TypeToken<List<Book>>() {}.type
    gson.fromJson(json, bookListType) ?: emptyList()
  } catch (e: Exception) {
    e.printStackTrace()
    emptyList()
  }
}

private fun saveBooksToDataFolder(context: Context, books: List<Book>) {
  try {
    val booksFile = File(context.filesDir, "books.json")
    val gson = Gson()
    val json = gson.toJson(books)
    booksFile.writeText(json)
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

@Composable
private fun ImportOptionButton(
  icon: ImageVector? = null,
  drawableRes: Int? = null,
  label: String,
  optionType: ImportOptionType,
  onClick: () -> Unit,
  enabled: Boolean = true,
  recolor: Boolean = true,
  modifier: Modifier = Modifier
) {
  // Define colors for different import types
  val buttonColor = when (optionType) {
    ImportOptionType.LIBRARY -> Color(0xFF00BCD4) // Cyan
    ImportOptionType.CREATE_NEW -> Color(0xFF9C27B0) // Purple
    ImportOptionType.IMPORT_FILE -> Color(0xFFFF9800) // Orange
    ImportOptionType.IMPORT_WEB -> Color(0xFF2196F3) // Blue
  }
  
  // Format label text to max 3 words on 2 lines
//  val formattedLabel = remember(label) {
//    val words = label.split(" ")
//    when {
//      words.size == 2 -> label
//      words.size == 3 -> "${words[0]} ${words[1]}\n${words[2]}"
//      else -> "${words[0]} ${words[1]}\n${words[2]}"
//    }
//  }

  val formattedLabel = label
  
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .clickable { onClick() }
      .alpha(if (enabled) 1f else 0.5f)
      .padding(4.dp)
  ) {
    if (recolor) {
      Card(
        modifier = Modifier.size(52.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (enabled) buttonColor else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxSize()
        ) {
          when {
            drawableRes != null -> {
              Icon(
                painter = painterResource(drawableRes),
                contentDescription = label,
                tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
              )
            }
            icon != null -> {
              Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }
      }
    } else {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(52.dp)
      ) {
        when {
          drawableRes != null -> {
            Icon(
              painter = painterResource(drawableRes),
              contentDescription = label,
              tint = Color.Unspecified,
              modifier = Modifier.size(48.dp)
            )
          }
          icon != null -> {
            Icon(
              imageVector = icon,
              contentDescription = label,
              tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(32.dp)
            )
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = formattedLabel,
      style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
      ),
      color = if (enabled) MaterialTheme.colorScheme.onSurface 
              else MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      maxLines = 2
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

