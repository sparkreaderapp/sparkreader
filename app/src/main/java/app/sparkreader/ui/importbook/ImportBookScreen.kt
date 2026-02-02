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

package app.sparkreader.ui.importbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.rememberCoroutineScope
import app.sparkreader.data.BookTagUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBookScreen(
  onNavigateUp: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
  refreshKey: Int = 0,
  viewModel: ImportBookViewModel = hiltViewModel()
) {
  val libraryState by viewModel.libraryState.collectAsState()
  var showRemoveConfirmation by remember { mutableStateOf<LibraryBook?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  
  // Load library when screen becomes visible or refreshKey changes
  LaunchedEffect(refreshKey) {
    viewModel.refreshLibraryState()
  }
  
  // Show book added message if present
  LaunchedEffect(libraryState.bookSnackbarMessage) {
    libraryState.bookSnackbarMessage?.let { message ->
      // Clear any existing snackbar first
      snackbarHostState.currentSnackbarData?.dismiss()
      
      coroutineScope.launch {
        val result = snackbarHostState.showSnackbar(
          message = message,
          actionLabel = "Dismiss",
          duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
          viewModel.clearBookAddedMessage()
        }
      }
    }
  }
  
  // Clear snackbars when leaving the page
  androidx.compose.runtime.DisposableEffect(Unit) {
    onDispose {
      snackbarHostState.currentSnackbarData?.dismiss()
      viewModel.clearBookAddedMessage()
    }
  }
  
  // Check if any books are currently being added or removed
  val hasActiveOperations = libraryState.addingBooks.values.any { it } || 
                           libraryState.removingBooks.values.any { it }
  
  // Back handler for system back button - disable when operations are active
  BackHandler(enabled = hasActiveOperations) {
    // Do nothing when operations are active - prevents back navigation
  }
  
  // Normal back handler when no operations are active
  BackHandler(enabled = !hasActiveOperations) {
    onNavigateUp()
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("SparkReader Library") },
        navigationIcon = {
          IconButton(
            onClick = onNavigateUp,
            enabled = !hasActiveOperations
          ) {
            Icon(
              imageVector = Icons.Default.ArrowBack,
              contentDescription = "Navigate back",
              tint = if (!hasActiveOperations) 
                MaterialTheme.colorScheme.onSurface 
              else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
          }
        }
      )
    },
    modifier = modifier
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 16.dp)
    ) {
      // Show loading spinner while checking library
      if (libraryState.isLoadingLibrary) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(48.dp),
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Loading library...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else if (!libraryState.isLibraryAvailable) {
        // Show message to download from settings
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          )
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = null,
              modifier = Modifier.size(48.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "SparkReader Library not downloaded",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Please download the library from Settings to browse available books.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { onNavigateToSettings() }
              ) {
                Text("Go to Settings")
              }
            }
          }
        }
      } else {
        LibraryBooksList(
          libraryState = libraryState,
          onBookClick = { book -> viewModel.addBookToUserLibrary(book) },
          onRemoveBookClick = { book -> showRemoveConfirmation = book },
          isBookInLibrary = { book -> viewModel.isBookInUserLibrary(book) },
          viewModel = viewModel
        )
      }
    }
  }
  
  // Remove book confirmation dialog
  showRemoveConfirmation?.let { book ->
    AlertDialog(
      onDismissRequest = { showRemoveConfirmation = null },
      title = { Text("Delete Book?") },
      text = { 
        Text("Are you sure you want to delete \"${book.title}\" by from your library? This will also delete all associated chats.")
      },
      confirmButton = {
        Button(
          onClick = { 
            viewModel.removeBookFromUserLibrary(book)
            showRemoveConfirmation = null
          },
          colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
          )
        ) {
          Text("Remove")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRemoveConfirmation = null }) {
          Text("Cancel")
        }
      }
    )
  }
}


@Composable
private fun LibraryBooksList(
  libraryState: LibraryState,
  onBookClick: (LibraryBook) -> Unit,
  onRemoveBookClick: (LibraryBook) -> Unit,
  isBookInLibrary: (LibraryBook) -> Boolean,
  modifier: Modifier = Modifier,
  viewModel: ImportBookViewModel = hiltViewModel()
) {
  val books = libraryState.books
  var searchQuery by remember { mutableStateOf("") }
  var areTagsExpanded by remember { mutableStateOf(true) }
  
  // Filter books based on search query and selected tags
  val filteredBooks = remember(books, searchQuery, libraryState.selectedTags) {
    var filtered = books
    
    // Apply text search filter
    if (searchQuery.isNotBlank()) {
      filtered = filtered.filter { book ->
        // Search in title, author, description
        val matchesBasicFields = (book.title?.contains(searchQuery, ignoreCase = true) == true) ||
                                (book.author?.contains(searchQuery, ignoreCase = true) == true) ||
                                (book.description?.contains(searchQuery, ignoreCase = true) == true)
        
        // Search in tags (excluding top-level dimensions)
        val matchesTags = if (!book.tags.isNullOrBlank()) {
          val bookTags = BookTagUtils.parseBookTags(book.tags)
          
          bookTags.any { tag ->
            BookTagUtils.getTagDisplayValue(tag, full = true).contains(searchQuery, ignoreCase = true)
          }
        } else {
          false
        }
        
        matchesBasicFields || matchesTags
      }
    }
    
    // Apply tag filters (intersection - book must have ALL selected tags)
    if (libraryState.selectedTags.isNotEmpty()) {
      filtered = filtered.filter { book ->
        // Check if book has ALL of the selected tags
        if (book.tags.isNullOrBlank()) {
          false
        } else {
          // Parse book tags
          val bookTags = BookTagUtils.parseBookTags(book.tags)
          
          // Check if ALL selected tags are present in the book's tags
          libraryState.selectedTags.all { selectedTag ->
            bookTags.any { bookTag ->
              BookTagUtils.getTagDisplayValue(bookTag).equals(selectedTag, ignoreCase = true)
            }
          }
        }
      }
    }
    
    filtered
  }
  
  
  Column(modifier = modifier) {
    if (books.isNotEmpty()) {
      // Search bar with tag expander
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier.weight(1f),
          placeholder = { Text("Search books...") },
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
        
        // Tag filter expander icon
        IconButton(
          onClick = { areTagsExpanded = !areTagsExpanded },
          modifier = Modifier.padding(start = 8.dp)
        ) {
          Icon(
            imageVector = if (areTagsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (areTagsExpanded) "Hide filters" else "Show filters",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Tag filters by dimension (only show if expanded)
      if (areTagsExpanded) {
        val dimensions = listOf("temporal", "regional", "discipline", "genre")
        val dimensionTitles = mapOf(
          "temporal" to "Time",
          "regional" to "Place",
          "discipline" to "Field",
          "genre" to "Genre"
        )
        
        dimensions.forEach { dimension ->
        if (dimension == "genre") {
          // Special handling for genre - get fiction and non-fiction tags separately
          val fictionTags = libraryState.tagsByDimension["genre_fiction"] ?: emptyList()
          val nonFictionTags = libraryState.tagsByDimension["genre_nonfiction"] ?: emptyList()
          
          if (fictionTags.isNotEmpty() || nonFictionTags.isNotEmpty()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Genre",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 4.dp)
              )
              
              // Fiction genres
              fictionTags.forEach { tagValue ->
                val isSelected = tagValue in libraryState.selectedTags
                val fictionColor = if (isSelected) {
                  BookTagUtils.getTagColor("genre/fiction/$tagValue", on=false, container = true)
                } else {
                  Color.Transparent
                }
                val onFictionColor = if (isSelected) {
                  BookTagUtils.getTagColor("genre/fiction/$tagValue", on=true, container = true)
                } else {
                  BookTagUtils.getTagColor("genre/fiction/$tagValue", on=true, container = false)
                }

                Box(
                  modifier = Modifier
                    .clickable(
                      indication = null,
                      interactionSource = remember { MutableInteractionSource() }
                    ) { viewModel.toggleTagSelection(tagValue) }
                    .background(
                      color = fictionColor,
                      shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                      width = 1.dp,
                      color = if (isSelected) fictionColor else onFictionColor,
                      shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = tagValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = onFictionColor
                  )
                }
              }
              
              // Separator between fiction and non-fiction
              if (fictionTags.isNotEmpty() && nonFictionTags.isNotEmpty()) {
                Text(
                  text = "|",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 4.dp)
                )
              }
              
              // Non-fiction genres
              nonFictionTags.forEach { tagValue ->
                val isSelected = tagValue in libraryState.selectedTags
                val nonFictionColor = if (isSelected) {
                  BookTagUtils.getTagColor("genre/nonfiction/$tagValue", on=false, container = true)
                } else {
                  Color.Transparent
                }
                val onNonFictionColor = if (isSelected) {
                  BookTagUtils.getTagColor("genre/nonfiction/$tagValue", on=true, container = true)
                } else {
                  BookTagUtils.getTagColor("genre/nonfiction/$tagValue", on=true, container = false)
                }

                Box(
                  modifier = Modifier
                    .clickable(
                      indication = null,
                      interactionSource = remember { MutableInteractionSource() }
                    ) { viewModel.toggleTagSelection(tagValue) }
                    .background(
                      color = nonFictionColor,
                      shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                      width = 1.dp,
                      color = if (isSelected) nonFictionColor else onNonFictionColor,
                      shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = tagValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = onNonFictionColor
                  )
                }
              }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
          }
        } else {
          // Regular handling for other dimensions
          val tags = libraryState.tagsByDimension[dimension] ?: emptyList()
          if (tags.isNotEmpty()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = dimensionTitles[dimension] ?: dimension.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 4.dp)
              )
              
              // Use the order from tags.txt file
              tags.forEach { tagValue ->
                val isSelected = tagValue in libraryState.selectedTags
                val color = if (isSelected) { // bgcolor
                  BookTagUtils.getTagColor("$dimension/$tagValue", on=false, container = true)
                } else {
                  Color.Transparent
                }
                val onColor = if (isSelected) { //textcolor
                  BookTagUtils.getTagColor("$dimension/$tagValue", on=true, container = true)
                } else {
                  BookTagUtils.getTagColor("$dimension/$tagValue", on=true, container = false)
                }
                
                Box(
                  modifier = Modifier
                    .clickable(
                      indication = null,
                      interactionSource = remember { MutableInteractionSource() }
                    ) { viewModel.toggleTagSelection(tagValue) }
                    .background(
                      color = color,
                      shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                      width = 1.dp,
                      color = if (isSelected) color else onColor,
                      shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = tagValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = onColor
                  )
                }
              }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
          }
        }
      }
        
        Spacer(modifier = Modifier.height(8.dp))
      }
      
      // Books count and active filters indicator
      Column(
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${filteredBooks.size} ${if (filteredBooks.size == 1) "book" else "books"} available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          // Show active filters count
          if (libraryState.selectedTags.isNotEmpty()) {
            Text(
              text = "${libraryState.selectedTags.size} ${if (libraryState.selectedTags.size == 1) "filter" else "filters"} active",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
        
        // Show clear filters button if any filters are active
        if (libraryState.selectedTags.isNotEmpty()) {
          TextButton(
            onClick = { viewModel.clearAllTagSelections() },
            modifier = Modifier.padding(top = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Clear,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear all filters")
          }
        }
      }
      
      Spacer(modifier = Modifier.height(8.dp))
      
      // Books list
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(
          items = filteredBooks,
          key = { book -> "${book.title}_${book.author}" }
        ) { book ->
          val isInLibrary = libraryState.userBooks.any { userBook ->
            userBook.title.equals(book.title, ignoreCase = true) && 
            userBook.author.equals(book.author, ignoreCase = true)
          }
          
          val bookId = "${book.title}_${book.author}"
          val isAdding = libraryState.addingBooks[bookId] ?: false
          val isRemoving = libraryState.removingBooks[bookId] ?: false
          val wasAdded = libraryState.addedBooks.contains(bookId)
          
          LibraryBookRowItem(
            book = book,
            isInLibrary = isInLibrary,
            isAdding = isAdding,
            isRemoving = isRemoving,
            wasAdded = wasAdded,
            onClicked = { 
              if (!isAdding && !isRemoving) {
                if (isInLibrary) {
                  onRemoveBookClick(book)
                } else if (!wasAdded) {
                  onBookClick(book)
                }
              }
            }
          )
        }
      }
    } else {
      // Empty state
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "No books found in library",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun LibraryBookRowItem(
  book: LibraryBook,
  isInLibrary: Boolean,
  isAdding: Boolean,
  isRemoving: Boolean,
  wasAdded: Boolean,
  onClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }
  
  // Parse tags from the book and order them according to tags.txt
  val bookTags = remember(book.tags) {
    val parsedTags = BookTagUtils.parseBookTags(book.tags)
    BookTagUtils.orderTagsByFileOrder(parsedTags)
  }
  
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClicked() },
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
      ) {
        Icon(
          imageVector = Icons.Default.Book,
          contentDescription = null,
          modifier = Modifier.size(40.dp),
          tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = book.title ?: "Unknown Title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "by ${book.author ?: "Unknown Author"}${book.date?.let { " ($it)" } ?: ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxHeight()
        ) {
          when {
            isAdding || isRemoving -> {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
            }
            wasAdded || (isInLibrary && !isRemoving) -> {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Already in library",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
            }
            else -> {
              Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Add to library",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          
          // Description expander icon (only show if description exists)
          if (!book.description.isNullOrEmpty()) {
            IconButton(
              onClick = { isExpanded = !isExpanded },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }
          } else {
            // Add empty spacer to maintain consistent height when no description
            Spacer(modifier = Modifier.size(24.dp))
          }
        }
      }
      
      // Description and tags
      if (!book.description.isNullOrEmpty() && isExpanded) {
        Spacer(modifier = Modifier.height(8.dp))
        Column {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = book.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          // Display tags if available
          if (bookTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Wrap tags in a scrollable row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              bookTags.forEach { tag ->
                val tagValue = BookTagUtils.getTagDisplayValue(tag, full = true)
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
          }
        }
      }
    }
  }
}
