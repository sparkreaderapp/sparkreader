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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sparkreader.data.getModelByName
import app.sparkreader.data.ConfigKey
import app.sparkreader.data.DEFAULT_TEMPERATURE
import app.sparkreader.data.EXPLAIN_CONTEXT_WORDS
import app.sparkreader.data.STOP_BUTTON_WORD_THRESHOLD
import app.sparkreader.data.INITIAL_MESSAGE_LOAD_COUNT
import app.sparkreader.data.LOAD_MORE_MESSAGE_COUNT
import app.sparkreader.llm.GemmaInferenceHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import app.sparkreader.ui.llmchat.LlmChatModelHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import app.sparkreader.data.Model
import app.sparkreader.data.DataStoreRepository
import app.sparkreader.ui.newhome.Book
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import app.sparkreader.proto.Theme
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class ModelNotFoundException(message: String) : Exception(message)

data class BookChatMessage(
    val content: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false,
    val wordCount: Int = 0
)

data class SavedChatMessage(
    val sender: String, // "user" or "llm"
    val message: String,
    val timestamp: String
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemmaInferenceHelper: GemmaInferenceHelper,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {
    
    private val _explanationState = MutableStateFlow<ExplanationState>(ExplanationState.Idle)
    val explanationState: StateFlow<ExplanationState> = _explanationState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<BookChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<BookChatMessage>> = _chatMessages.asStateFlow()

    private var currentBookId: String? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Pagination state
    private var allSavedMessages: List<SavedChatMessage> = emptyList()
    private var currentLoadedCount = 0
    private val _isLoadingMoreMessages = MutableStateFlow(false)
    val isLoadingMoreMessages: StateFlow<Boolean> = _isLoadingMoreMessages.asStateFlow()
    private var lastLoadedCount = 0
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()
    
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
    
    private val _showBackConfirmDialog = MutableStateFlow(false)
    val showBackConfirmDialog: StateFlow<Boolean> = _showBackConfirmDialog.asStateFlow()
    
    private val _selectedModelName = MutableStateFlow<String?>(null)
    val selectedModelName: StateFlow<String?> = _selectedModelName.asStateFlow()
    
    private val _modelValidationError = MutableStateFlow<String?>(null)
    val modelValidationError: StateFlow<String?> = _modelValidationError.asStateFlow()
    
    // Create a flow that observes the DataStore directly
    val modelNameFlow = viewModelScope.launch {
        // This will emit whenever the model changes in DataStore
        dataStoreRepository.readSelectedModel()?.let { modelName ->
            _selectedModelName.value = modelName
        }
    }
    
    private var currentGenerationId = 0L
    private val activeGenerations = mutableSetOf<Long>()
    private val cancelledGenerations = mutableSetOf<Long>()
    private var needsNewSession = false
    
    private val userBooksFile = File(context.filesDir, "books.json")
    private val booksMutex = Mutex()
    private val gson = Gson()
    
    // Default temperature value
    private var temperature = 0.7f
    private var lastAppliedTemperature = 0.7f
    
    private val commandProcessor = ChatCommandProcessor()
    
    fun sendChatMessage(message: String, currentPage: Int, bookTitle: String, bookAuthor: String, totalPages: Int = 0): Boolean {
        var success = false
        viewModelScope.launch {
            // Generate a unique ID for this generation
            val generationId = System.currentTimeMillis()
            currentGenerationId = generationId
            activeGenerations.add(generationId)
            
            try {
                // Clear any previous validation error
                _validationError.value = null
                
                // Validate model first for all commands that need AI
                val needsModel = message.startsWith("/explain") || message.startsWith("/quote") || 
                               message.startsWith("/page") || message.startsWith("/book") || 
                               !message.startsWith("/")
                if (needsModel) {
                    val validationResult = gemmaInferenceHelper.validateSelectedModel()
                    if (!validationResult.isValid) {
                        _snackbarMessage.value = validationResult.errorMessage ?: "Model validation failed"
                        activeGenerations.remove(generationId)
                        success = false
                        return@launch
                    }
                }
                
                // Load page content if this is a /page or /quote command
                val pageContent = if (message.startsWith("/page") || message.startsWith("/quote")) {
                    loadPageContent(currentPage)
                } else {
                    null
                }
                
                // Process the message using the command processor
                val processedCommand = commandProcessor.processMessage(
                    message = message,
                    bookTitle = bookTitle,
                    bookAuthor = bookAuthor,
                    totalPages = totalPages,
                    pageContent = pageContent
                )
                
                val prompt = processedCommand.prompt
                
                // If we get here, validation passed
                success = true
                // Add user message
                _chatMessages.value = _chatMessages.value + BookChatMessage(message, true)
                _isGenerating.value = true
                
                // Save chat history after adding user message
                currentBookId?.let { bookId ->
                    saveChatHistory(bookId)
                }
                
                generateChatResponse(prompt, generationId)
            } catch (e: IllegalArgumentException) {
                // For validation errors, show in UI instead of chat
                _validationError.value = e.message ?: "Invalid command format"
                _isGenerating.value = false
                activeGenerations.remove(generationId)
                success = false
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + BookChatMessage(
                    "Error: ${e.message ?: "Unknown error"}", 
                    false
                )
                _isGenerating.value = false
                activeGenerations.remove(generationId)
                success = false
            }
        }
        return success
    }
    
    fun getExplanation(
        pageText: String,
        selectionStart: Int,
        selectionEnd: Int,
        currentPage: Int,
        bookTitle: String,
        bookAuthor: String,
        totalPages: Int = 0
    ): Boolean {
        // Check if already generating
        if (_isGenerating.value) {
            _snackbarMessage.value = "Please wait for the current generation to finish"
            return false
        }
        
        try {
            // Generate the explain command using the processor
            val message = commandProcessor.generateExplainCommand(
                pageText = pageText,
                selectionStart = selectionStart,
                selectionEnd = selectionEnd,
                currentPage = currentPage
            )
            
            // Send the command directly - validation will be handled in sendChatMessage
            return sendChatMessage(message, currentPage, bookTitle, bookAuthor, totalPages)
        } catch (e: IllegalArgumentException) {
            _snackbarMessage.value = e.message ?: "Invalid selection"
            return false
        }
    }

    fun generateQuoteCommand(
        pageText: String,
        selectionStart: Int,
        selectionEnd: Int,
        currentPage: Int
    ): String {
        return try {
            commandProcessor.generateQuoteCommand(
                pageText = pageText,
                selectionStart = selectionStart,
                selectionEnd = selectionEnd,
                currentPage = currentPage
            )
        } catch (e: IllegalArgumentException) {
            _snackbarMessage.value = e.message ?: "Invalid selection"
            ""
        }
    }
    
    
    private fun loadPageContent(pageNumber: Int): String? {
        return try {
            val libraryDir = File(context.filesDir, "library")
            val bookDir = File(libraryDir, currentBookId ?: return null)
            val pageFile = File(bookDir, "page_$pageNumber.json")
            
            if (!pageFile.exists()) {
                return null
            }
            
            val pageData = gson.fromJson(pageFile.readText(), app.sparkreader.ui.importbook.paginator.BookPage::class.java)
            pageData?.content
        } catch (e: Exception) {
            android.util.Log.e("BookDetailViewModel", "Error loading page content for validation", e)
            null
        }
    }
    
    private suspend fun generateChatResponse(prompt: String, generationId: Long) = withContext(Dispatchers.IO) {
        try {
            // Validate the selected model
            val validationResult = gemmaInferenceHelper.validateSelectedModel()
            
            if (!validationResult.isValid) {
                throw ModelNotFoundException(validationResult.errorMessage ?: "Model validation failed")
            }
            
            val selectedModel = validationResult.model!!
            
            // Initialize model if needed
            if (selectedModel.instance == null) {
                // Only show initializing message if this is the current generation
                if (generationId == currentGenerationId) {
                    withContext(Dispatchers.Main) {
                        _chatMessages.value = _chatMessages.value + BookChatMessage("Initializing AI model...", false)
                    }
                }
                initializeModel(selectedModel)
                // Remove the initializing message only if this is still the current generation
                if (generationId == currentGenerationId) {
                    withContext(Dispatchers.Main) {
                        _chatMessages.value = _chatMessages.value.dropLast(1)
                    }
                }
            }
            
            // Load temperature from settings
            temperature = dataStoreRepository.readTemperature()
            
            // Add "Please wait..." message for streaming only if this is the current generation
            var streamingMessageIndex = -1
            if (generationId == currentGenerationId) {
                streamingMessageIndex = _chatMessages.value.size
                _chatMessages.value = _chatMessages.value + BookChatMessage("Please wait...", false, true)
            }
            
            val accumulatedResponse = StringBuilder()
            var wordCount = 0
            
            // Only update temperature and reset session if temperature has changed or if we need a new session
            if (temperature != lastAppliedTemperature || needsNewSession) {
                val updatedConfigValues = selectedModel.configValues.toMutableMap()
                updatedConfigValues["temperature"] = temperature
                selectedModel.configValues = updatedConfigValues
                LlmChatModelHelper.resetSession(selectedModel)
                lastAppliedTemperature = temperature
                needsNewSession = false
            }
            
            LlmChatModelHelper.runInference(
                model = selectedModel,
                input = prompt,
                resultListener = { partialResult, done ->
                    // Only process if this is the current generation
                    if (generationId == currentGenerationId) {
                        accumulatedResponse.append(partialResult)
                        
                        // Count words in the accumulated response
                        val currentText = accumulatedResponse.toString().trim()
                        wordCount = currentText.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                        
                        viewModelScope.launch(Dispatchers.Main) {
                            // Double-check this is still the current generation
                            if (generationId == currentGenerationId && streamingMessageIndex >= 0) {
                                // Update the streaming message
                                val messages = _chatMessages.value.toMutableList()
                                if (streamingMessageIndex < messages.size) {
                                    // Show accumulated response or keep "Please wait..." if no content yet
                                    val displayText = if (currentText.isEmpty()) {
                                        "Please wait..."
                                    } else {
                                        currentText
                                    }
                                    messages[streamingMessageIndex] = BookChatMessage(
                                        displayText,
                                        false,
                                        !done, // Set isStreaming to true while still generating, false when done
                                        wordCount
                                    )
                                    _chatMessages.value = messages
                                }
                                
                                if (done) {
                                    _isGenerating.value = false
                                    // Save chat history when response is complete
                                    currentBookId?.let { bookId ->
                                        saveChatHistory(bookId)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Clean up when done
                    if (done) {
                        viewModelScope.launch(Dispatchers.IO) {
                            activeGenerations.remove(generationId)
                            
                            // If this was a cancelled generation, close its session
                            if (cancelledGenerations.contains(generationId)) {
                                cancelledGenerations.remove(generationId)
                                try {
                                    // Note: We already created a new session when stop was pressed,
                                    // so we just need to clean up the cancelled generation
                                    android.util.Log.d("BookDetailViewModel", "Cleaned up cancelled generation: $generationId")
                                } catch (e: Exception) {
                                    android.util.Log.e("BookDetailViewModel", "Failed to clean up cancelled generation", e)
                                }
                            }
                        }
                    }
                },
                cleanUpListener = {
                    viewModelScope.launch(Dispatchers.Main) {
                        // Only update UI state if this is the current generation
                        if (generationId == currentGenerationId) {
                            _isGenerating.value = false
                        }
                    }
                    // Always remove from active generations
                    viewModelScope.launch(Dispatchers.IO) {
                        activeGenerations.remove(generationId)
                        cancelledGenerations.remove(generationId)
                    }
                }
            )
            
        } catch (e: Exception) {
            // Only show error if this is the current generation
            if (generationId == currentGenerationId) {
                withContext(Dispatchers.Main) {
                    // Only show error if this is the current generation
                    _snackbarMessage.value = "Failed to get response. Please consider deleting and downloading the model again, or send us a bug report (see FAQ) if this persists. (detail: ${e.message})"
                    _isGenerating.value = false
                }
            }
            // Always remove from active generations
            activeGenerations.remove(generationId)
            cancelledGenerations.remove(generationId)
        }
    }
    
    
    private suspend fun initializeModel(model: Model) = withContext(Dispatchers.IO) {
        val initResult = suspendCancellableCoroutine<String> { continuation ->
            LlmChatModelHelper.initialize(context, model) { result ->
                continuation.resume(result)
            }
        }
        
        if (initResult.isNotEmpty()) {
            throw Exception("Model initialization failed. Please consider deleting and downloading the model again, or send us a bug report (see FAQ) if this persists. (detail: $initResult)")
        }
    }
    
    fun stopGeneration() {
        // Mark the current generation as cancelled
        val oldGenerationId = currentGenerationId
        cancelledGenerations.add(oldGenerationId)
        
        // Create a new generation ID
        currentGenerationId = System.currentTimeMillis()
        _isGenerating.value = false
        
        // Update the last message to remove streaming indicator
        val messages = _chatMessages.value.toMutableList()
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            val lastMessage = messages[lastIndex]
            if (lastMessage.isStreaming) {
                messages[lastIndex] = lastMessage.copy(isStreaming = false)
                _chatMessages.value = messages
                
                // Save chat history after stopping generation
                currentBookId?.let { bookId ->
                    saveChatHistory(bookId)
                }
            }
        }
        
        // Set flag to create new session on next generation
        // This is necessary because MediaPipe doesn't allow sending messages while actively generating
        needsNewSession = true
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
        _validationError.value = null
        allSavedMessages = emptyList()
        currentLoadedCount = 0
        // Save empty chat history
        currentBookId?.let { bookId ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val libraryDir = File(context.filesDir, "library")
                    val bookDir = File(libraryDir, bookId)
                    val chatsFile = File(bookDir, "chats.json")
                    chatsFile.writeText("[]")
                } catch (e: Exception) {
                    android.util.Log.e("BookDetailViewModel", "Failed to clear chat history", e)
                }
            }
        }
    }
    
    fun clearChatMessages() {
        // Clear only the UI state without affecting saved messages
        _chatMessages.value = emptyList()
        _validationError.value = null
        // Reset pagination state
        currentLoadedCount = 0
        allSavedMessages = emptyList()
    }

    fun loadChatHistory(bookId: String) {
        currentBookId = bookId
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val libraryDir = File(context.filesDir, "library")
                val bookDir = File(libraryDir, bookId)
                val chatsFile = File(bookDir, "chats.json")

                if (chatsFile.exists()) {
                    val jsonContent = chatsFile.readText()
                    val type = object : TypeToken<List<SavedChatMessage>>() {}.type
                    allSavedMessages = gson.fromJson(jsonContent, type) ?: emptyList()

                    // Load only the most recent messages initially
                    val initialMessages = allSavedMessages.takeLast(INITIAL_MESSAGE_LOAD_COUNT)
                    currentLoadedCount = initialMessages.size

                    // Convert saved messages to BookChatMessage
                    val chatMessages = initialMessages.map { saved ->
                        BookChatMessage(
                            content = saved.message,
                            isUser = saved.sender == "user",
                            isStreaming = false,
                            wordCount = 0
                        )
                    }

                    withContext(Dispatchers.Main) {
                        _chatMessages.value = chatMessages
                    }
                } else {
                    // No chat history exists
                    allSavedMessages = emptyList()
                    currentLoadedCount = 0
                    withContext(Dispatchers.Main) {
                        _chatMessages.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BookDetailViewModel", "Failed to load chat history", e)
                allSavedMessages = emptyList()
                currentLoadedCount = 0
                withContext(Dispatchers.Main) {
                    _chatMessages.value = emptyList()
                }
            }
        }
    }

    fun loadMoreMessages() {
        if (_isLoadingMoreMessages.value || currentLoadedCount >= allSavedMessages.size) {
            return
        }

        _isLoadingMoreMessages.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Add timeout to prevent infinite loading
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeout(5000) { // 5 second timeout
                        // Add a small delay to ensure UI shows the loading indicator
                        delay(100)
                        
                        // Calculate how many more messages to load
                        val remainingCount = allSavedMessages.size - currentLoadedCount
                        val loadCount = minOf(LOAD_MORE_MESSAGE_COUNT, remainingCount)

                        // Get the next batch of older messages
                        val startIndex = allSavedMessages.size - currentLoadedCount - loadCount
                        val endIndex = allSavedMessages.size - currentLoadedCount
                        val moreMessages = allSavedMessages.subList(startIndex, endIndex)

                        // Convert to BookChatMessage
                        val newChatMessages = moreMessages.map { saved ->
                            BookChatMessage(
                                content = saved.message,
                                isUser = saved.sender == "user",
                                isStreaming = false,
                                wordCount = 0
                            )
                        }

                        withContext(Dispatchers.Main) {
                            // Store how many messages we're adding
                            lastLoadedCount = loadCount
                            
                            // Prepend new messages to existing ones
                            _chatMessages.value = newChatMessages + _chatMessages.value
                            currentLoadedCount += loadCount
                            
                            // Add a small delay before hiding the loading indicator
                            delay(200)
                            _isLoadingMoreMessages.value = false
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.e("BookDetailViewModel", "Timeout while loading more messages", e)
                withContext(Dispatchers.Main) {
                    lastLoadedCount = 0
                    _isLoadingMoreMessages.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("BookDetailViewModel", "Failed to load more messages", e)
                withContext(Dispatchers.Main) {
                    lastLoadedCount = 0
                    _isLoadingMoreMessages.value = false
                }
            }
        }
    }

    fun hasMoreMessages(): Boolean {
        return currentLoadedCount < allSavedMessages.size
    }
    
    fun getLastLoadedCount(): Int {
        return lastLoadedCount
    }

    private fun saveChatHistory(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val libraryDir = File(context.filesDir, "library")
                val bookDir = File(libraryDir, bookId)

                // Ensure directory exists
                if (!bookDir.exists()) {
                    bookDir.mkdirs()
                }

                val chatsFile = File(bookDir, "chats.json")

                // Get only the new messages that aren't already saved
                val currentMessages = _chatMessages.value.filter { !it.isStreaming }
                val newMessageCount = currentMessages.size - currentLoadedCount

                if (newMessageCount > 0) {
                    // Get the new messages (the ones at the end)
                    val newMessages = currentMessages.takeLast(newMessageCount).map { message ->
                        SavedChatMessage(
                            sender = if (message.isUser) "user" else "llm",
                            message = message.content,
                            timestamp = dateFormat.format(Date())
                        )
                    }

                    // Append to existing saved messages
                    allSavedMessages = allSavedMessages + newMessages
                    currentLoadedCount = currentMessages.size

                    // Save all messages
                    val json = gson.toJson(allSavedMessages)
                    chatsFile.writeText(json)

                    android.util.Log.d("BookDetailViewModel", "Saved ${newMessages.size} new chat messages for book $bookId")
                }
            } catch (e: Exception) {
                android.util.Log.e("BookDetailViewModel", "Failed to save chat history", e)
            }
        }
    }
    
    fun clearValidationError() {
        _validationError.value = null
    }
    
    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
    
    fun showSnackbarMessage(message: String) {
        _snackbarMessage.value = message
    }
    
    fun handleBackPressed(): Boolean {
        return if (_isGenerating.value) {
            _showBackConfirmDialog.value = true
            true // Consume the back press
        } else {
            false // Allow normal back navigation
        }
    }
    
    fun dismissBackConfirmDialog() {
        _showBackConfirmDialog.value = false
    }
    
    fun confirmBackWithAbort() {
        stopGeneration()
        _showBackConfirmDialog.value = false
    }
    
    fun updateLastReadPage(book: Book, pageNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            booksMutex.withLock {
                try {
                    // Read current books
                    val jsonContent = userBooksFile.readText()
                    val type = object : TypeToken<List<Book>>() {}.type
                    val books: List<Book> = gson.fromJson(jsonContent, type) ?: emptyList()
                    
                    // Update the book with new last read page
                    val updatedBooks = books.map { existingBook ->
                        if (existingBook.id == book.id) {
                            existingBook.copy(lastReadPage = pageNumber)
                        } else {
                            existingBook
                        }
                    }
                    
                    // Write back to file
                    val updatedJson = gson.toJson(updatedBooks)
                    userBooksFile.writeText(updatedJson)
                    
                    android.util.Log.d("BookDetailViewModel", "Updated last read page for book ${book.id} to $pageNumber")
                } catch (e: Exception) {
                    android.util.Log.e("BookDetailViewModel", "Failed to update last read page", e)
                }
            }
        }
    }
    
    fun getSelectedModelName(): String? {
        return dataStoreRepository.readSelectedModel()
    }
    
    fun refreshModelState() {
        // Force a re-read of the selected model from DataStore
        // This ensures we get the latest state when returning to the screen
        viewModelScope.launch {
            val validationResult = gemmaInferenceHelper.validateSelectedModel()
            
            if (validationResult.isValid) {
                _selectedModelName.value = validationResult.model?.name
                _modelValidationError.value = null
            } else {
                _selectedModelName.value = null
                _modelValidationError.value = validationResult.errorMessage
            }
        }
    }
    
    init {
        // Initialize selected model name
        refreshModelState()
    }
    
    fun getSelectedModelDefaultTemperature(): Float? {
        val modelName = dataStoreRepository.readSelectedModel()
        if (modelName.isNullOrEmpty()) return null
        
        val model = getModelByName(modelName)
        return model?.getFloatConfigValue(ConfigKey.TEMPERATURE, DEFAULT_TEMPERATURE)
    }
    
    fun saveTheme(theme: Theme) {
        dataStoreRepository.saveTheme(theme)
    }
    
    fun getTemperature(): Float {
        temperature = dataStoreRepository.readTemperature()
        return temperature
    }
    
    fun saveTemperature(temp: Float) {
        temperature = temp
        dataStoreRepository.saveTemperature(temp)
    }
    
    fun getFontSize(): Float {
        return dataStoreRepository.readFontSize()
    }
    
    fun saveFontSize(size: Float) {
        dataStoreRepository.saveFontSize(size)
    }
    
    override fun onCleared() {
        super.onCleared()
        gemmaInferenceHelper.close()
        // Clean up any LLM model instances
        val selectedModelName = dataStoreRepository.readSelectedModel()
        if (!selectedModelName.isNullOrEmpty()) {
            val selectedModel = getModelByName(selectedModelName)
            selectedModel?.let { LlmChatModelHelper.cleanUp(it) }
        }
    }
}

sealed class ExplanationState {
    object Idle : ExplanationState()
    object Loading : ExplanationState()
    data class Streaming(val word: String, val explanation: String, val isComplete: Boolean = false) : ExplanationState()
    data class Error(val message: String) : ExplanationState()
}
