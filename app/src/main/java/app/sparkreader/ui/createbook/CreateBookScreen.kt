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

package app.sparkreader.ui.createbook
import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import app.sparkreader.data.STOP_BUTTON_WORD_THRESHOLD
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import androidx.core.content.FileProvider
import java.io.IOException
import app.sparkreader.ui.createbook.OcrSourceIcon
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material3.SnackbarDuration
import androidx.core.content.ContextCompat

data class BookPage(
    var pageNumber: Int,
    var content: String = ""
)

data class PageData(
    val content: String,
    val startOffset: Long,
    val endOffset: Long,
    val pageNumber: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateBookScreen(
    onBackPressed: () -> Unit,
    onBookCreated: (Boolean) -> Unit,
    bookToEdit: app.sparkreader.ui.newhome.Book? = null,
    onNavigateToModelManager: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CreateBookViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // OCR state from ViewModel
    val ocrState by viewModel.ocrState.collectAsState()
    val selectedModelName by viewModel.selectedModelName.collectAsState()
    val showBackConfirmDialog by viewModel.showBackConfirmDialog.collectAsState()
    val modelValidationError by viewModel.modelValidationError.collectAsState()

    // Book metadata
    var title by remember { mutableStateOf(bookToEdit?.title ?: "") }
    var author by remember { mutableStateOf(bookToEdit?.author ?: "") }
    
    // Track if user has interacted with the form when editing
    var hasUserInteracted by remember { mutableStateOf(false) }

    // Pages management
    val pages = remember {
        mutableStateListOf<BookPage>().apply {
            if (bookToEdit != null) {
                // Load existing pages
                val libraryDir = File(context.filesDir, "library")
                val bookDir = File(libraryDir, bookToEdit.libraryId ?: bookToEdit.id.toString())
                if (bookDir.exists()) {
                    val gson = Gson()
                    bookDir.listFiles()
                        ?.filter { it.name.startsWith("page_") && it.name.endsWith(".json") }
                        ?.sortedBy { file ->
                            file.name.removePrefix("page_").removeSuffix(".json").toIntOrNull() ?: 0
                        }
                        ?.forEach { pageFile ->
                            try {
                                val json = pageFile.readText()
                                val pageData = gson.fromJson(json, PageData::class.java)
                                add(BookPage(pageData.pageNumber + 1, pageData.content))
                            } catch (e: Exception) {
                                // Skip corrupted page files
                            }
                        }
                }
                // If no pages were loaded, add an empty one
                if (isEmpty()) {
                    add(BookPage(1))
                }
            } else {
                // New book - start with one empty page
                add(BookPage(1))
            }
        }
    }
    var currentPageIndex by remember { mutableStateOf(0) }
    var isEditMode by remember { mutableStateOf(false) }
    var editingContent by remember { mutableStateOf("") }
    
    // Track if user has explicitly saved any page (even empty)
    var hasExplicitlySavedPage by remember { mutableStateOf(false) }
    
    // Track if gallery or camera was tapped (to show spinner immediately)
    var galleryTapped by remember { mutableStateOf(false) }
    var cameraTapped by remember { mutableStateOf(false) }
    
    // Unsaved changes dialog
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    
    // Unsaved edit dialog
    var showUnsavedEditDialog by remember { mutableStateOf(false) }
    var pendingNavigation: (() -> Unit)? by remember { mutableStateOf(null) }
    
    // Delete confirmation dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Page input dialog
    var showPageInputDialog by remember { mutableStateOf(false) }
    
    // Saving state
    var isSaving by remember { mutableStateOf(false) }
    
    val currentPage = pages[currentPageIndex]
    
    // Track keyboard visibility
    val isKeyboardVisible = WindowInsets.isImeVisible
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.processImageForOcr(uri)
        } else {
            // User cancelled image picker, reset gallery tapped state
            galleryTapped = false
        }
    }
  
    // Function to create a temporary file for camera capture
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    // Camera launcher
    var cameraImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.processImageForOcr(cameraImageUri!!)
        } else {
            // User cancelled camera or capture failed, reset camera tapped state
            cameraTapped = false
        }
        // Clean up the URI
        cameraImageUri = null
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with camera
            try {
                val photoFile = createImageFile()
                cameraImageUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    photoFile
                )
                cameraLauncher.launch(cameraImageUri!!)
            } catch (e: IOException) {
                cameraTapped = false
                coroutineScope.launch {
                    try {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = "Failed to open camera",
                            actionLabel = "Dismiss",
                            duration = SnackbarDuration.Long
                        )
                    } catch (e: Exception) {
                        // Ignore if coroutine is cancelled
                    }
                }
            }
        } else {
            // Permission denied
            cameraTapped = false
            coroutineScope.launch {
                try {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = "Camera permission is required to take photos",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long
                    )
                } catch (e: Exception) {
                    // Ignore if coroutine is cancelled
                }
            }
        }
    }
    
    // Handle OCR state changes
    LaunchedEffect(ocrState) {
        when (val state = ocrState) {
            is OcrState.Streaming -> {
                // Update content during streaming
                if (isEditMode) {
                    editingContent = state.text
                } else {
                    currentPage.content = state.text
                }
                if (state.isComplete) {
                    hasExplicitlySavedPage = true
                    if (bookToEdit != null) hasUserInteracted = true
                }
            }
            is OcrState.Success -> {
                if (isEditMode) {
                    editingContent = state.text
                } else {
                    currentPage.content = state.text
                }
                hasExplicitlySavedPage = true
                if (bookToEdit != null) hasUserInteracted = true
                galleryTapped = false // Reset gallery tapped state
                cameraTapped = false // Reset camera tapped state
                viewModel.resetOcrState()
            }
            is OcrState.Error -> {
                // Clear any existing snackbar before showing error
                snackbarHostState.currentSnackbarData?.dismiss()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = state.message,
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long
                    )
                }
                galleryTapped = false // Reset gallery tapped state
                cameraTapped = false // Reset camera tapped state
                viewModel.resetOcrState()
            }
            else -> {}
        }
    }
    
    // Refresh model state when screen becomes visible
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
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
    
    // Check if there are unsaved changes
    fun hasUnsavedChanges(): Boolean {
        if (bookToEdit != null) {
            // For editing, only check if user has interacted
            return hasUserInteracted
        } else {
            // For new book, check if any content was added
            return title.isNotBlank() || 
                   author.isNotBlank() || 
                   pages.any { it.content.isNotBlank() } ||
                   pages.size > 1
        }
    }
    
    // Check if we're in the initial empty state (no content at all)
    fun isInitialEmptyState(): Boolean {
        return title.isBlank() && 
               author.isBlank() && 
               pages.size == 1 && 
               pages[0].content.isBlank()
    }
    
    // Check if there's any page content
    fun hasPageContent(): Boolean {
        return pages.any { it.content.isNotBlank() } || 
               pages.size > 1 || 
               isEditMode ||  // Enable add pages when in edit mode
               hasExplicitlySavedPage  // Keep enabled if user has saved any page
    }
    
    // Check if OCR is actively running
    fun isOcrActive(): Boolean {
        return ocrState is OcrState.Processing || ocrState is OcrState.Streaming
    }
    
    // Handle back navigation
    fun handleBackNavigation() {
        // Check if OCR generation is active first
        val shouldShowOcrDialog = viewModel.handleBackPressed()
        if (shouldShowOcrDialog) {
            // OCR dialog will be shown by the viewModel
            return
        }
        
        // No OCR active, check for unsaved changes
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onBackPressed()
        }
    }
    
    // Back handler for system back button
    BackHandler(enabled = !isSaving) {
        handleBackNavigation()
    }
    
    fun saveBook() {
        if (title.isBlank()) {
            coroutineScope.launch {
                try {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = "Please enter a title",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long
                    )
                } catch (e: Exception) {
                    // Ignore if coroutine is cancelled
                }
            }
            return
        }

        isSaving = true
        coroutineScope.launch {
            try {
                // Use existing book ID if editing, otherwise create new
                val bookId = bookToEdit?.libraryId ?: UUID.randomUUID().toString()
                
                // Create library directory if it doesn't exist
                val libraryDir = File(context.filesDir, "library")
                if (!libraryDir.exists()) {
                    libraryDir.mkdirs()
                }
                
                // Create book directory
                val bookDir = File(libraryDir, bookId)
                bookDir.mkdirs()
                
                // Save pages as JSON files
                val gson = Gson()
                var currentOffset = 0L
                pages.forEachIndexed { index, page ->
                    val pageFile = File(bookDir, "page_${index}.json")
                    val startOffset = currentOffset
                    val endOffset = currentOffset + page.content.length
                    
                    val pageData = PageData(
                        content = page.content,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        pageNumber = index
                    )
                    
                    pageFile.writeText(gson.toJson(pageData))
                    currentOffset = endOffset
                }

                // Create book metadata
                val bookMetadata = app.sparkreader.ui.newhome.Book(
                    id = bookToEdit?.id ?: bookId.hashCode(),
                    title = title,
                    author = author,
                    date = bookToEdit?.date ?: "Created on ${SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())}",
                    description = bookToEdit?.description ?: "",
                    libraryId = bookId,
                    totalPages = pages.size,
                    wordsPerPage = bookToEdit?.wordsPerPage,
                    source = "user",
                    source_link = "",
                    tags = "" // x`"user"
                )
                
                // Load existing books
                val booksFile = File(context.filesDir, "books.json")
                val existingBooks = if (booksFile.exists()) {
                    val json = booksFile.readText()
                    try {
                        gson.fromJson(json, Array<app.sparkreader.ui.newhome.Book>::class.java).toMutableList()
                    } catch (e: Exception) {
                        mutableListOf()
                    }
                } else {
                    mutableListOf()
                }

                // Add new book or update existing
                if (bookToEdit != null) {
                    // Remove old entry and add updated one
                    existingBooks.removeAll { it.id == bookToEdit.id }
                }
                existingBooks.add(bookMetadata)
                
                // Save updated books list
                booksFile.writeText(gson.toJson(existingBooks))

                // Navigate back to home screen with success
                onBookCreated(true)
            } catch (e: Exception) {
                isSaving = false
                coroutineScope.launch {
                    try {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = "Error creating book: ${e.message}",
                            actionLabel = "Dismiss",
                            duration = SnackbarDuration.Long
                        )
                    } catch (e: Exception) {
                        // Ignore if coroutine is cancelled
                    }
                }
            }
        }
    }
    
    fun addPageBeforeCurrent() {
        if (isEditMode) {
            showUnsavedEditDialog = true
            pendingNavigation = {
                // Insert at current position
                pages.add(currentPageIndex, BookPage(0))
                // Renumber all pages
                pages.forEachIndexed { index, page ->
                    page.pageNumber = index + 1
                }
                // Stay on the newly added page (which is now at currentPageIndex)
                if (bookToEdit != null) hasUserInteracted = true
            }
        } else {
            // Insert at current position
            pages.add(currentPageIndex, BookPage(0))
            // Renumber all pages
            pages.forEachIndexed { index, page ->
                page.pageNumber = index + 1
            }
            // Stay on the newly added page (which is now at currentPageIndex)
            if (bookToEdit != null) hasUserInteracted = true
        }
    }
    
    fun addPageAfterCurrent() {
        if (isEditMode) {
            showUnsavedEditDialog = true
            pendingNavigation = {
                // Insert after current position
                val insertIndex = currentPageIndex + 1
                pages.add(insertIndex, BookPage(0))
                // Renumber all pages
                pages.forEachIndexed { index, page ->
                    page.pageNumber = index + 1
                }
                // Navigate to the newly added page
                currentPageIndex = insertIndex
                if (bookToEdit != null) hasUserInteracted = true
            }
        } else {
            // Insert after current position
            val insertIndex = currentPageIndex + 1
            pages.add(insertIndex, BookPage(0))
            // Renumber all pages
            pages.forEachIndexed { index, page ->
                page.pageNumber = index + 1
            }
            // Navigate to the newly added page
            currentPageIndex = insertIndex
            if (bookToEdit != null) hasUserInteracted = true
        }
    }
    
    fun performDeleteCurrentPage() {
        if (pages.size <= 1) {
            // If it's the only page, replace it with a new empty page
            pages[0] = BookPage(1, "")
            // Exit edit mode if active
            if (isEditMode) {
                isEditMode = false
                editingContent = ""
            }
            // Reset the explicit save flag since we're back to initial state
            hasExplicitlySavedPage = false
            // Force recomposition by resetting the index
            currentPageIndex = 0
        } else {
            // Exit edit mode if active
            if (isEditMode) {
                isEditMode = false
                editingContent = ""
            }
            
            // Remove the current page
            pages.removeAt(currentPageIndex)
            
            // Renumber all pages
            pages.forEachIndexed { index, page ->
                page.pageNumber = index + 1
            }
            
            // Adjust current page index
            if (currentPageIndex >= pages.size) {
                // If we deleted the last page, go to the new last page
                currentPageIndex = pages.size - 1
            }
            // Otherwise, stay at the same index (which now shows the next page)
        }
        if (bookToEdit != null) hasUserInteracted = true
    }
    
    fun deleteCurrentPage() {
        showDeleteConfirmation = true
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (bookToEdit != null) "Edit Book" else "Create New Book") },
                navigationIcon = {
                    val backIconColor by animateColorAsState(
                        targetValue = if (!isSaving) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        animationSpec = tween(durationMillis = 200),
                        label = "backIconColor"
                    )
                    
                    IconButton(
                        onClick = { 
                            handleBackNavigation()
                        },
                        enabled = !isSaving
                    ) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = backIconColor
                        )
                    }
                },
                actions = {
                    val saveEnabled = hasUnsavedChanges() && !isSaving
                    
                    // Animate colors for smooth transitions
                    val iconColor by animateColorAsState(
                        targetValue = if (saveEnabled) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        animationSpec = tween(durationMillis = 200),
                        label = "saveIconColor"
                    )
                    
                    val textColor by animateColorAsState(
                        targetValue = if (saveEnabled) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        animationSpec = tween(durationMillis = 200),
                        label = "saveTextColor"
                    )
                    
                    TextButton(
                        onClick = { saveBook() },
                        enabled = saveEnabled
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(
                                Icons.Default.Save, 
                                contentDescription = "Save",
                                tint = iconColor
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Save",
                            color = textColor
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
                .imePadding() // Add IME padding to adjust for keyboard
        ) {
            // Book metadata fields - hide when keyboard is visible AND in edit mode
            AnimatedVisibility(
                visible = !(isEditMode && isKeyboardVisible),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { 
                            title = it
                            if (bookToEdit != null) hasUserInteracted = true
                        },
                        placeholder = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = author,
                        onValueChange = { 
                            author = it
                            if (bookToEdit != null) hasUserInteracted = true
                        },
                        placeholder = { Text("Author (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Page content area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Edit mode - show text field with animation
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isEditMode,
                        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(300)
                        ),
                        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                            targetScale = 0.8f,
                            animationSpec = tween(200)
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TextField(
                            value = editingContent,
                            onValueChange = { 
                                editingContent = it
                                if (bookToEdit != null) hasUserInteracted = true
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding(), // Add IME padding to the TextField
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            placeholder = { Text("Start typing...") }
                        )
                    }
                    
                    // Empty page icons with animation - hide when streaming has text or when processing starts
                    val streamingState = ocrState as? OcrState.Streaming
                    val hasStreamingText = streamingState != null && streamingState.text.isNotBlank()
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isEditMode && currentPage.content.isBlank() && ocrState is OcrState.Idle && !hasStreamingText,
                        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(300)
                        ),
                        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                            targetScale = 0.8f,
                            animationSpec = tween(200)
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(32.dp)
                            ) {
                                val isProcessing = galleryTapped || cameraTapped || ocrState is OcrState.Processing || ocrState is OcrState.Streaming
                                
                                if (isProcessing) {
                                    // Show only a single spinner in the center when any source was tapped or OCR is active
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    // Show normal icons
                                    // Camera icon
                                    OcrSourceIcon(
                                        icon = Icons.Default.CameraAlt,
                                        contentDescription = "Camera",
                                        isEnabled = !selectedModelName.isNullOrEmpty(),
                                        isProcessing = cameraTapped,
                                        hasError = !modelValidationError.isNullOrEmpty(),
                                        onClick = {
                                            if (!selectedModelName.isNullOrEmpty()) {
                                                cameraTapped = true
                                                // Check camera permission
                                                when {
                                                    ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.CAMERA
                                                    ) == PackageManager.PERMISSION_GRANTED -> {
                                                        // Permission already granted
                                                        try {
                                                            val photoFile = createImageFile()
                                                            cameraImageUri = FileProvider.getUriForFile(
                                                                context,
                                                                "${context.packageName}.provider",
                                                                photoFile
                                                            )
                                                            cameraLauncher.launch(cameraImageUri!!)
                                                        } catch (e: IOException) {
                                                            cameraTapped = false
                                                            coroutineScope.launch {
                                                                try {
                                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                                    snackbarHostState.showSnackbar(
                                                                        message = "Failed to open camera",
                                                                        actionLabel = "Dismiss",
                                                                        duration = SnackbarDuration.Long
                                                                    )
                                                                } catch (e: Exception) {
                                                                    // Ignore if coroutine is cancelled
                                                                }
                                                            }
                                                        }
                                                    }
                                                    else -> {
                                                        // Request camera permission
                                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    
                                    // Gallery icon
                                    OcrSourceIcon(
                                        icon = Icons.Default.Image,
                                        contentDescription = "Gallery",
                                        isEnabled = !selectedModelName.isNullOrEmpty(),
                                        isProcessing = galleryTapped,
                                        hasError = !modelValidationError.isNullOrEmpty(),
                                        onClick = {
                                            if (!selectedModelName.isNullOrEmpty()) {
                                                galleryTapped = true
                                                imagePickerLauncher.launch("image/*")
                                            }
                                        }
                                    )
                                    
                                    // Pencil icon (enabled)
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clickable {
                                                isEditMode = true
                                                editingContent = currentPage.content
                                                if (bookToEdit != null) hasUserInteracted = true
                                            },
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // Show message if no model is selected or there's an error
                            val errorMessage = modelValidationError
                            if (!errorMessage.isNullOrEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = errorMessage,
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
                                        Text("Go to Model Manager")
                                    }
                                }
                            } else if (selectedModelName.isNullOrEmpty()) {
                                Text(
                                    text = "Download a model to enable OCR",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    
                    // Page content with animation - show during streaming too
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isEditMode && (currentPage.content.isNotBlank() || ocrState is OcrState.Streaming),
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Use streaming text if available, otherwise use saved content
                        val displayText = when (val state = ocrState) {
                            is OcrState.Streaming -> state.text
                            else -> currentPage.content
                        }
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            // Don't allow editing while streaming
                                            if (ocrState !is OcrState.Streaming) {
                                                isEditMode = true
                                                editingContent = currentPage.content
                                                if (bookToEdit != null) hasUserInteracted = true
                                            }
                                        }
                                    )
                                }
                        ) {
                            item {
                                Text(
                                    text = displayText,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                    
                    // OCR processing indicator - show on top of content
                    androidx.compose.animation.AnimatedVisibility(
                        visible = ocrState is OcrState.Processing || ocrState is OcrState.Streaming,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(48.dp)
                            ) {
                                // Show stop button if streaming and word count exceeds threshold
                                val streamingState = ocrState as? OcrState.Streaming
                                val showStopButton = streamingState != null && 
                                    streamingState.wordCount >= STOP_BUTTON_WORD_THRESHOLD
                                
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                if (showStopButton) {
                                    IconButton(
                                        onClick = { 
                                            Log.d("CreateBookScreen", "Stop button tapped - calling stopOcrGeneration()")
                                            viewModel.stopOcrGeneration() 
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Stop OCR",
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            // Only show status message for processing state
                            val statusMessage = when (val state = ocrState) {
                                is OcrState.Processing -> state.status
                                is OcrState.Streaming -> "" // Don't show status during streaming - text is shown in page content
                                else -> ""
                            }
                            
                            if (statusMessage.isNotEmpty()) {
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    
                    // Save/Cancel buttons when in edit mode with animation
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isEditMode,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    isEditMode = false
                                    editingContent = ""
                                    // No need to check here - hasPageContent() will handle it
                                }
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = {
                                    currentPage.content = editingContent
                                    isEditMode = false
                                    editingContent = ""
                                    hasExplicitlySavedPage = true  // User has explicitly saved a page
                                    if (bookToEdit != null) hasUserInteracted = true
                                }
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Navigation and page management row - hide when keyboard is visible AND in edit mode
            AnimatedVisibility(
                visible = !(isEditMode && isKeyboardVisible),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add page before current
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        Text(
                            "Add page before current",
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.inverseSurface,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(
                        onClick = { addPageBeforeCurrent() },
                        enabled = hasPageContent() && !isOcrActive()
                    ) {
                        Icon(
                            Icons.Default.FirstPage,
                            contentDescription = "Add page before current",
                            tint = if (hasPageContent() && !isOcrActive()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
                
                // Plus icon (decorative only)
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(0.5f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Add page after current
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        Text(
                            "Add page after current",
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.inverseSurface,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(
                        onClick = { addPageAfterCurrent() },
                        enabled = hasPageContent() && !isOcrActive()
                    ) {
                        Icon(
                            Icons.Default.LastPage,
                            contentDescription = "Add page after current",
                            tint = if (hasPageContent() && !isOcrActive()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Delete current page
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        Text(
                            "Delete current page",
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.inverseSurface,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(
                        onClick = { deleteCurrentPage() },
                        enabled = hasPageContent() && !isOcrActive()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete current page",
                            tint = if (hasPageContent() && !isOcrActive()) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Previous page
                IconButton(
                    onClick = {
                        if (isEditMode) {
                            showUnsavedEditDialog = true
                            pendingNavigation = {
                                if (currentPageIndex > 0) {
                                    currentPageIndex--
                                }
                            }
                        } else if (currentPageIndex > 0) {
                            currentPageIndex--
                        }
                    },
                    enabled = currentPageIndex > 0 && !isOcrActive()
                ) {
                    Icon(Icons.Default.ArrowBackIos, contentDescription = "Previous")
                }
                
                // Page indicator
                Text(
                    text = "${currentPage.pageNumber} / ${pages.size}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable(enabled = !isOcrActive()) { 
                            if (!isOcrActive()) {
                                showPageInputDialog = true 
                            }
                        }
                )
                
                // Next page
                IconButton(
                    onClick = {
                        if (isEditMode) {
                            showUnsavedEditDialog = true
                            pendingNavigation = {
                                if (currentPageIndex < pages.size - 1) {
                                    currentPageIndex++
                                }
                            }
                        } else if (currentPageIndex < pages.size - 1) {
                            currentPageIndex++
                        }
                    },
                    enabled = currentPageIndex < pages.size - 1 && !isOcrActive()
                ) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next")
                }
            }
            }
        }
    }
    
    // Unsaved changes confirmation dialog
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("Are you sure you want to leave? Any unsaved changes will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedChangesDialog = false
                        // Stop any ongoing OCR generation before leaving
                        if (isOcrActive()) {
                            viewModel.stopOcrGeneration()
                        }
                        // Exit edit mode if active (discarding edits)
                        if (isEditMode) {
                            isEditMode = false
                            editingContent = ""
                        }
                        onBackPressed()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard & Leave")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showUnsavedChangesDialog = false
                        // User chose to stay - keep edit mode and content as is
                    }
                ) {
                    Text("Stay")
                }
            }
        )
    }
    
    // Unsaved edit confirmation dialog
    if (showUnsavedEditDialog) {
        AlertDialog(
            onDismissRequest = { 
                showUnsavedEditDialog = false
                pendingNavigation = null
            },
            title = { Text("Save Changes?") },
            text = { Text("You have unsaved changes to this page. What would you like to do?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Discard the current edit
                        isEditMode = false
                        editingContent = ""
                        showUnsavedEditDialog = false
                        // Execute pending navigation
                        pendingNavigation?.invoke()
                        pendingNavigation = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard & Leave")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showUnsavedEditDialog = false
                        pendingNavigation = null
                    }
                ) {
                    Text("Stay")
                }
            }
        )
    }
    
    // Delete page confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Page?") },
            text = { 
                Text(
                    if (pages.size <= 1) {
                        "This will clear all content from this page. Are you sure?"
                    } else {
                        "Are you sure you want to delete page ${currentPage.pageNumber}?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        performDeleteCurrentPage()
                        showDeleteConfirmation = false
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
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Page input dialog
    if (showPageInputDialog) {
        PageInputDialog(
            currentPage = currentPage.pageNumber,
            totalPages = pages.size,
            onPageSelected = { page ->
                if (isEditMode) {
                    showUnsavedEditDialog = true
                    pendingNavigation = {
                        currentPageIndex = page - 1
                        showPageInputDialog = false
                    }
                } else {
                    currentPageIndex = page - 1
                    showPageInputDialog = false
                }
            },
            onDismiss = { showPageInputDialog = false }
        )
    }
    
    // Back confirmation dialog
    if (showBackConfirmDialog) {
        BackConfirmationDialog(
            onAbortAndLeave = {
                viewModel.confirmBackWithAbort()
                onBackPressed()
            },
            onStay = {
                viewModel.dismissBackConfirmDialog()
            },
            onDismiss = {
                viewModel.dismissBackConfirmDialog()
            }
        )
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

