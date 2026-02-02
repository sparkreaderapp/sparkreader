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

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sparkreader.data.DataStoreRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import app.sparkreader.ui.newhome.Book
import kotlinx.coroutines.delay
import app.sparkreader.ui.importbook.paginator.TextPaginator
import app.sparkreader.ui.importbook.paginator.EpubPaginator
import kotlinx.coroutines.sync.withLock
import app.sparkreader.data.BookTagUtils

private const val TAG = "ImportBookViewModel"
private const val LIBRARY_DIR_NAME = "sparkreader-library"

data class LibraryBook(
  val id: String,
  val title: String?,
  val author: String?,
  val description: String? = null,
  val file: String? = null,
  val source: String? = null,
  val tags: String? = null,
  val date: String? = null,
  val source_link: String? = null
)

data class TagInfo(
  val dimension: String,
  val value: String,
  val fullTag: String
)

data class LibraryState(
  val books: List<LibraryBook> = emptyList(),
  val error: String = "",
  val userBooks: List<Book> = emptyList(),
  val isLibraryAvailable: Boolean = false,
  val isLoadingLibrary: Boolean = false, // New loading state
  val addingBooks: Map<String, Boolean> = emptyMap(), // bookId -> isAdding
  val addedBooks: Set<String> = emptySet(), // bookIds that have been added
  val removingBooks: Map<String, Boolean> = emptyMap(), // bookId -> isRemoving
  val tagsByDimension: Map<String, List<String>> = emptyMap(), // dimension -> ordered list of tag values
  val selectedTags: Set<String> = emptySet(), // selected tag values (without dimension prefix)
  val bookSnackbarMessage: String? = null // message to show when book is added
)

@HiltViewModel
class ImportBookViewModel @Inject constructor(
  private val dataStoreRepository: DataStoreRepository,
  @ApplicationContext private val context: Context
) : ViewModel() {
  
  private val _libraryState = MutableStateFlow(LibraryState())
  val libraryState = _libraryState.asStateFlow()
  
  private val externalFilesDir = context.getExternalFilesDir(null)
  private val userBooksFile = File(context.filesDir, "books.json")
  private val libraryPagesDir = File(context.filesDir, "library")
  private val textPaginator = TextPaginator()
  private val epubPaginator = EpubPaginator()
  
  // Mutex to synchronize book additions and file access
  private val booksMutex = kotlinx.coroutines.sync.Mutex()
  
  init {
    ensureUserBooksFileExists()
    ensureLibraryPagesDirectoryExists()
    loadUserBooks()
    // Don't load library in init - wait for UI to request it
  }
  
  fun refreshLibraryState() {
    // Reset state first to ensure clean refresh
    _libraryState.value = _libraryState.value.copy(
      books = emptyList(),
      isLibraryAvailable = false,
      error = "",
      selectedTags = emptySet(),
      addedBooks = emptySet()
    )
    checkLibraryStatus()
  }
  
  fun loadLibraryIfNeeded() {
    // Only load if not already loaded and not currently loading
    if (_libraryState.value.books.isEmpty() && !_libraryState.value.isLoadingLibrary && !_libraryState.value.isLibraryAvailable) {
      checkLibraryStatus()
    }
  }
  
  private fun ensureLibraryPagesDirectoryExists() {
    if (!libraryPagesDir.exists()) {
      try {
        libraryPagesDir.mkdirs()
        Log.d(TAG, "Created library pages directory at: ${libraryPagesDir.absolutePath}")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to create library pages directory", e)
      }
    }
  }
  
  
  private fun checkLibraryStatus() {
    viewModelScope.launch(Dispatchers.IO) {
      // Set loading state
      _libraryState.value = _libraryState.value.copy(isLoadingLibrary = true)
      
      // The actual path where files are extracted by DownloadRepository
      val modelPath = File(externalFilesDir, "SparkReader_Library/_/$LIBRARY_DIR_NAME")
      val catalogFile = File(modelPath, "library/catalog.json")
      
      Log.d(TAG, "Checking library status:")
      Log.d(TAG, "Model path: ${modelPath.absolutePath}, exists: ${modelPath.exists()}")
      Log.d(TAG, "Catalog file: ${catalogFile.absolutePath}, exists: ${catalogFile.exists()}")
      
      val isLibraryDownloaded = dataStoreRepository.readLibraryDownloaded()
      
      if (isLibraryDownloaded && modelPath.exists() && modelPath.isDirectory && catalogFile.exists()) {
        // Already extracted, load books
        val books = loadBooksFromLibrary()
        _libraryState.value = _libraryState.value.copy(
          isLibraryAvailable = true,
          books = books,
          isLoadingLibrary = false
        )
      } else {
        // Not downloaded or files missing
        _libraryState.value = _libraryState.value.copy(
          isLibraryAvailable = false,
          isLoadingLibrary = false,
          error = if (isLibraryDownloaded) "Library files not found. Please re-download from Settings." else ""
        )
      }
    }
  }
  
  
  private suspend fun loadBooksFromLibrary(): List<LibraryBook> = withContext(Dispatchers.IO) {
    try {
      // Use the actual path where DownloadRepository extracts files
      val catalogFile = File(externalFilesDir, "SparkReader_Library/_/$LIBRARY_DIR_NAME/library/catalog.json")
      Log.d(TAG, "Loading books from: ${catalogFile.absolutePath}")
      
      if (!catalogFile.exists()) {
        Log.w(TAG, "Catalog file not found: ${catalogFile.absolutePath}")
        return@withContext emptyList()
      }
      
      val jsonContent = catalogFile.readText()
      Log.d(TAG, "Catalog JSON content length: ${jsonContent.length}")
      
      val gson = Gson()
      val type = object : TypeToken<List<LibraryBook>>() {}.type
      val books: List<LibraryBook> = gson.fromJson(jsonContent, type) ?: emptyList()
      
      // Filter out books with null or empty titles to avoid display issues
      val validBooks = books.filter { !it.title.isNullOrBlank() }
      
      Log.d(TAG, "Loaded ${books.size} books from library, ${validBooks.size} valid")
      
      // Log first few books for debugging
      validBooks.take(3).forEach { book ->
        Log.d(TAG, "Sample book: id=${book.id}, title=${book.title}, file=${book.file}")
      }
      
      // Load tags from tags.txt and organize by dimension
      val tagsByDimension = BookTagUtils.loadTagsFromFile(context)
      _libraryState.value = _libraryState.value.copy(tagsByDimension = tagsByDimension)
      
      return@withContext validBooks
      
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load books from library", e)
      return@withContext emptyList()
    }
  }
  
  
  fun toggleTagSelection(tagValue: String) {
    val currentSelected = _libraryState.value.selectedTags
    val newSelected = if (tagValue in currentSelected) {
      currentSelected - tagValue
    } else {
      currentSelected + tagValue
    }
    _libraryState.value = _libraryState.value.copy(selectedTags = newSelected)
  }
  
  fun clearAllTagSelections() {
    _libraryState.value = _libraryState.value.copy(selectedTags = emptySet())
  }
  
  fun clearBookAddedMessage() {
    _libraryState.value = _libraryState.value.copy(bookSnackbarMessage = null)
  }
  
  private fun ensureUserBooksFileExists() {
    if (!userBooksFile.exists()) {
      try {
        userBooksFile.writeText("[]")
        Log.d(TAG, "Created empty books.json at: ${userBooksFile.absolutePath}")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to create books.json", e)
      }
    }
  }
  
  private fun loadUserBooks() {
    viewModelScope.launch(Dispatchers.IO) {
      booksMutex.withLock {
        try {
          val jsonContent = userBooksFile.readText()
          val gson = Gson()
          val type = object : TypeToken<List<Book>>() {}.type
          val books: List<Book> = gson.fromJson(jsonContent, type) ?: emptyList()
          
          _libraryState.value = _libraryState.value.copy(userBooks = books)
          Log.d(TAG, "Loaded ${books.size} user books")
        } catch (e: Exception) {
          Log.e(TAG, "Failed to load user books", e)
          _libraryState.value = _libraryState.value.copy(userBooks = emptyList())
        }
      }
    }
  }
  
  fun addBookToUserLibrary(libraryBook: LibraryBook) {
    val bookIdentifier = "${libraryBook.title}_${libraryBook.author}"
    
    Log.d(TAG, "Starting to add book to library: $bookIdentifier")
    Log.d(TAG, "Library book details: id=${libraryBook.id}, file=${libraryBook.file}, title=${libraryBook.title}")
    
    // Start the adding process
    _libraryState.value = _libraryState.value.copy(
      addingBooks = _libraryState.value.addingBooks + (bookIdentifier to true)
    )
    
    viewModelScope.launch {
      // Process the book (paginate if needed) using the library book's ID
      val paginationResult = processBookForLibrary(libraryBook, libraryBook.id)
      
      // Wait for 1 second minimum for UI feedback
      delay(1000)
      
      if (paginationResult == null || !paginationResult.success) {
        // If processing failed, just update UI state
        _libraryState.value = _libraryState.value.copy(
          addingBooks = _libraryState.value.addingBooks - bookIdentifier,
          error = "Failed to process book"
        )
        return@launch
      }
      
      // Use mutex to ensure thread-safe book addition
      booksMutex.withLock {
        // Re-read current books inside the lock to get the latest state
        val currentBooks = _libraryState.value.userBooks
        
        // Check if book already exists (by title and author)
        val bookExists = currentBooks.any { 
          it.title.equals(libraryBook.title, ignoreCase = true) && 
          it.author.equals(libraryBook.author, ignoreCase = true) 
        }
        
        if (!bookExists) {
          // Use the library book ID directly as the new book ID
          val newId = libraryBook.id
          
          // Convert LibraryBook to Book with pagination info
          val newBook = Book(
            id = newId,
            title = libraryBook.title ?: "Unknown Title",
            author = libraryBook.author ?: "Unknown Author",
            description = libraryBook.description ?: "",
            date = libraryBook.date,
            libraryId = libraryBook.id,  // Store the library book ID
            totalPages = paginationResult.totalPages,
            wordsPerPage = 100,  // We're using 100 as default
            lastReadPage = 0,  // Start at page 0
            source = "SparkReader Library (${libraryBook.source})",
            source_link = libraryBook.source_link ?: "",
            tags = libraryBook.tags ?: ""
          )
          
          // Update state with new book
          val updatedBooks = currentBooks + newBook
          _libraryState.value = _libraryState.value.copy(
            userBooks = updatedBooks,
            addingBooks = _libraryState.value.addingBooks - bookIdentifier,
            addedBooks = _libraryState.value.addedBooks + bookIdentifier,
            //bookAddedMessage = "\"${newBook.title}\" added to your library!"
            bookSnackbarMessage = "Book added to your library!"
          )
          
          // Save to file within the lock to ensure consistency
          try {
            withContext(Dispatchers.IO) {
              val gson = Gson()
              val jsonContent = gson.toJson(updatedBooks)
              userBooksFile.writeText(jsonContent)
            }
            Log.d(TAG, "Added book to user library: ${newBook.title}")
          } catch (e: Exception) {
            Log.e(TAG, "Failed to save book to file", e)
            // Revert the state if save fails
            _libraryState.value = _libraryState.value.copy(
              userBooks = currentBooks,
              addedBooks = _libraryState.value.addedBooks - bookIdentifier
            )
          }
        } else {
          // Book already exists, just update the state
          _libraryState.value = _libraryState.value.copy(
            addingBooks = _libraryState.value.addingBooks - bookIdentifier,
            addedBooks = _libraryState.value.addedBooks + bookIdentifier
          )
          Log.d(TAG, "Book already exists in user library: ${libraryBook.title}")
        }
      }
    }
  }
  
  fun isBookInUserLibrary(libraryBook: LibraryBook): Boolean {
    return _libraryState.value.userBooks.any { userBook ->
      userBook.title.equals(libraryBook.title, ignoreCase = true) && 
      userBook.author.equals(libraryBook.author, ignoreCase = true)
    }
  }
  
  fun removeBookFromUserLibrary(libraryBook: LibraryBook) {
    val bookIdentifier = "${libraryBook.title}_${libraryBook.author}"
    
    Log.d(TAG, "Starting to remove book from library: $bookIdentifier")
    
    // Start the removing process
    _libraryState.value = _libraryState.value.copy(
      removingBooks = _libraryState.value.removingBooks + (bookIdentifier to true)
    )
    
    viewModelScope.launch {
      // Add a minimum delay for UI feedback
      delay(100)
      
      booksMutex.withLock {
        val currentBooks = _libraryState.value.userBooks
        
        // Find the book to get its ID
        val bookToRemove = currentBooks.find { userBook ->
          userBook.title.equals(libraryBook.title, ignoreCase = true) && 
          userBook.author.equals(libraryBook.author, ignoreCase = true)
        }
        
        if (bookToRemove != null) {
          try {
            // Remove the book from the list
            val updatedBooks = currentBooks.filter { it.id != bookToRemove.id }
            
            // Save to file
            withContext(Dispatchers.IO) {
              val gson = Gson()
              val jsonContent = gson.toJson(updatedBooks)
              userBooksFile.writeText(jsonContent)
              
              // Remove the paginated book data using the library ID if available
              val bookDirId = bookToRemove.libraryId ?: bookToRemove.id
              val bookDir = File(libraryPagesDir, bookDirId)
              if (bookDir.exists()) {
                bookDir.deleteRecursively()
                Log.d(TAG, "Removed paginated data for book ID: $bookDirId")
              }
            }
            
            // Update state after successful removal
            _libraryState.value = _libraryState.value.copy(
              userBooks = updatedBooks,
              addedBooks = _libraryState.value.addedBooks - bookIdentifier,
              removingBooks = _libraryState.value.removingBooks - bookIdentifier,
              //bookAddedMessage = "\"${bookToRemove.title}\" removed from your library"
              bookSnackbarMessage = "Book deleted from your library"
            )
            
            Log.d(TAG, "Successfully removed book from user library: ${libraryBook.title}")
          } catch (e: Exception) {
            Log.e(TAG, "Failed to remove book", e)
            // Just remove the removing state on error, don't change the books list
            _libraryState.value = _libraryState.value.copy(
              removingBooks = _libraryState.value.removingBooks - bookIdentifier,
              error = "Failed to remove book: ${e.message}"
            )
          }
        } else {
          // Book not found, just remove the removing state
          _libraryState.value = _libraryState.value.copy(
            removingBooks = _libraryState.value.removingBooks - bookIdentifier
          )
          Log.w(TAG, "Book not found in user library: ${libraryBook.title}")
        }
      }
    }
  }
  
  private suspend fun processBookForLibrary(libraryBook: LibraryBook, bookId: String): app.sparkreader.ui.importbook.paginator.PaginationResult? {
    return withContext(Dispatchers.IO) {
      try {
        // Check if book is already paginated by checking if the directory exists with pages
        val bookDir = File(libraryPagesDir, bookId)
        if (bookDir.exists() && bookDir.isDirectory) {
          val pageFiles = bookDir.listFiles { file -> file.name.startsWith("page_") && file.name.endsWith(".json") }
          if (!pageFiles.isNullOrEmpty()) {
            Log.d(TAG, "Book already paginated: $bookId with ${pageFiles.size} pages")
            return@withContext app.sparkreader.ui.importbook.paginator.PaginationResult(
              success = true,
              totalPages = pageFiles.size,
              pagesDir = bookDir
            )
          }
        }
        
        // Get the actual book file from the library
        val libraryBaseDir = File(externalFilesDir, "SparkReader_Library/_/$LIBRARY_DIR_NAME/library")
        val bookFileName = libraryBook.file ?: "books/${bookId}/${bookId}-0.txt"
        val bookFile = File(libraryBaseDir, bookFileName)
        
        Log.d(TAG, "Looking for book file:")
        Log.d(TAG, "  Library base dir: ${libraryBaseDir.absolutePath}")
        Log.d(TAG, "  Library base dir exists: ${libraryBaseDir.exists()}")
        Log.d(TAG, "  Book filename: $bookFileName")
        Log.d(TAG, "  Book file path: ${bookFile.absolutePath}")
        Log.d(TAG, "  Book file exists: ${bookFile.exists()}")
        
        // Also check the books directory for debugging
        val booksDir = File(libraryBaseDir, "books")
        if (booksDir.exists()) {
          val bookIdDir = File(booksDir, bookId)
          if (bookIdDir.exists()) {
            val files = bookIdDir.listFiles()
            Log.d(TAG, "Files in book ID directory (${bookIdDir.absolutePath}): ${files?.size ?: 0}")
            files?.forEach { file ->
              Log.d(TAG, "  - ${file.name} (${file.length()} bytes)")
            }
          }
        }
        
        if (!bookFile.exists()) {
          Log.e(TAG, "Book file not found: ${bookFile.absolutePath}")
          return@withContext app.sparkreader.ui.importbook.paginator.PaginationResult(
            success = false,
            totalPages = 0,
            pagesDir = null,
            error = "Book file not found"
          )
        }
        
        Log.d(TAG, "Processing book file: ${bookFile.absolutePath}")
        
        // Determine the paginator based on file extension
        val paginator = when (bookFile.extension.lowercase()) {
          "epub" -> epubPaginator
          "txt" -> textPaginator
          else -> {
            Log.w(TAG, "Unsupported file format: ${bookFile.extension}")
            textPaginator // Default to text paginator
          }
        }
        
        // Ensure the library pages directory exists
        if (!libraryPagesDir.exists()) {
          libraryPagesDir.mkdirs()
          Log.d(TAG, "Created library pages directory: ${libraryPagesDir.absolutePath}")
        }
        
        // Paginate the book
        Log.d(TAG, "Starting pagination with ${paginator.javaClass.simpleName}")
        val result = try {
          paginator.paginate(
            sourceFile = bookFile,
            outputDir = libraryPagesDir,
            bookId = bookId,
            wordsPerPage = 100
          )
        } catch (e: Exception) {
          Log.e(TAG, "Exception during pagination", e)
          app.sparkreader.ui.importbook.paginator.PaginationResult(
            success = false,
            totalPages = 0,
            pagesDir = null,
            error = "Pagination failed: ${e.message}"
          )
        }
        
        if (result.success) {
          Log.d(TAG, "Successfully paginated book: $bookId with ${result.totalPages} pages")
          
          // Verify the pages were created
          val bookDir = File(libraryPagesDir, bookId)
          if (bookDir.exists()) {
            val pageFiles = bookDir.listFiles { file -> file.name.startsWith("page_") && file.name.endsWith(".json") }
            Log.d(TAG, "Verified ${pageFiles?.size ?: 0} page files created in ${bookDir.absolutePath}")
          }
        } else {
          Log.e(TAG, "Failed to paginate book: $bookId - ${result.error}")
        }
        
        return@withContext result
      } catch (e: Exception) {
        Log.e(TAG, "Error processing book for library", e)
        e.printStackTrace()
        return@withContext app.sparkreader.ui.importbook.paginator.PaginationResult(
          success = false,
          totalPages = 0,
          pagesDir = null,
          error = e.message ?: "Unknown error"
        )
      }
    }
  }
}
