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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sparkreader.data.DataStoreRepository
import app.sparkreader.data.Model
import app.sparkreader.data.TASK_LLM_ASK_IMAGE
import app.sparkreader.data.getModelByName
import app.sparkreader.ui.llmchat.LlmChatModelHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

private const val TAG = "CreateBookViewModel"

sealed class OcrState {
    object Idle : OcrState()
    data class Processing(val status: String) : OcrState()
    data class Streaming(val text: String, val isComplete: Boolean = false, val wordCount: Int = 0) : OcrState()
    data class Success(val text: String) : OcrState()
    data class Error(val message: String) : OcrState()
}

@HiltViewModel
class CreateBookViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreRepository: DataStoreRepository,
    private val gemmaInferenceHelper: app.sparkreader.llm.GemmaInferenceHelper
) : ViewModel() {
    
    private val _ocrState = MutableStateFlow<OcrState>(OcrState.Idle)
    val ocrState: StateFlow<OcrState> = _ocrState.asStateFlow()
    
    private val _selectedModelName = MutableStateFlow<String?>(null)
    val selectedModelName: StateFlow<String?> = _selectedModelName.asStateFlow()
    
    private val _modelValidationError = MutableStateFlow<String?>(null)
    val modelValidationError: StateFlow<String?> = _modelValidationError.asStateFlow()
    
    private val _showBackConfirmDialog = MutableStateFlow(false)
    val showBackConfirmDialog: StateFlow<Boolean> = _showBackConfirmDialog.asStateFlow()
    
    private var currentModel: Model? = null
    
    // Generation tracking for streaming
    private var currentGenerationId = 0L
    private val activeGenerations = mutableSetOf<Long>()
    private val cancelledGenerations = mutableSetOf<Long>()
    private var needsNewSession = false
    
    init {
        refreshModelState()
    }
    
    fun refreshModelState() {
        viewModelScope.launch {
            val validationResult = gemmaInferenceHelper.validateSelectedModel(task = "image")
            
            if (validationResult.isValid) {
                _selectedModelName.value = validationResult.model?.name
                currentModel = validationResult.model
                _modelValidationError.value = null
            } else {
                _selectedModelName.value = null
                currentModel = null
                _modelValidationError.value = validationResult.errorMessage
            }
        }
    }
    
    fun processImageForOcr(uri: Uri) {
        viewModelScope.launch {
            try {
                // Reset state
                _ocrState.value = OcrState.Processing("Loading image...")
                
                // Load the image
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
                
                if (bitmap == null) {
                    _ocrState.value = OcrState.Error("Failed to load image")
                    return@launch
                }
                
                // Validate the selected model for image task
                val validationResult = gemmaInferenceHelper.validateSelectedModel(task = "image")
                
                if (!validationResult.isValid) {
                    _ocrState.value = OcrState.Error(validationResult.errorMessage ?: "Model validation failed")
                    return@launch
                }
                
                // Find the model in the ask image task
                val model = TASK_LLM_ASK_IMAGE.models.find { it.name == validationResult.model?.name }
                if (model == null) {
                    _ocrState.value = OcrState.Error("Model configuration error")
                    return@launch
                }
                
                // Initialize model if needed
                if (model.instance == null) {
                    _ocrState.value = OcrState.Processing("Initializing AI model...")
                    
                    val initResult = withContext(Dispatchers.IO) {
                        suspendCancellableCoroutine<String> { continuation ->
                            LlmChatModelHelper.initialize(context, model) { result ->
                                continuation.resume(result)
                            }
                        }
                    }
                    
                    if (initResult.isNotEmpty()) {
                        _ocrState.value = OcrState.Error("Failed to initialize model. Please consider deleting and downloading the model again, or send us a bug report (see FAQ) if this persists. (detail: $initResult")
                        return@launch
                    }
                }
                
                _ocrState.value = OcrState.Processing("Extracting text from image...")
                
                // Generate a unique ID for this generation
                val generationId = System.currentTimeMillis()
                currentGenerationId = generationId
                activeGenerations.add(generationId)
                
                // Check if we need a new session (after a stop)
                if (needsNewSession) {
                    LlmChatModelHelper.resetSession(model)
                    needsNewSession = false
                }
                
                // Prepare OCR prompt
                val ocrPrompt = "Please extract all text from this image. Return only the extracted text without any additional commentary or formatting."
                
                // Run OCR inference with streaming
                val accumulatedText = StringBuilder()
                var wordCount = 0
                
                LlmChatModelHelper.runInference(
                    model = model,
                    input = ocrPrompt,
                    images = listOf(bitmap),
                    resultListener = { partialResult, done ->
                        // Check if this generation was cancelled
                        if (cancelledGenerations.contains(generationId)) {
                            Log.d(TAG, "Ignoring result from cancelled generation: $generationId")
                            return@runInference
                        }
                        
                        // Only process if this is the current generation
                        if (generationId == currentGenerationId) {
                            accumulatedText.append(partialResult)
                            
                            // Count words in the accumulated response
                            val currentText = accumulatedText.toString().trim()
                            wordCount = currentText.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                            
                            viewModelScope.launch(Dispatchers.Main) {
                                // Double-check this is still the current generation and not cancelled
                                if (generationId == currentGenerationId && !cancelledGenerations.contains(generationId)) {
                                    _ocrState.value = OcrState.Streaming(
                                        text = currentText,
                                        isComplete = done,
                                        wordCount = wordCount
                                    )
                                    
                                    if (done) {
                                        // Transition to success state
                                        if (currentText.isNotEmpty()) {
                                            _ocrState.value = OcrState.Success(currentText)
                                        } else {
                                            _ocrState.value = OcrState.Error("No text extracted from image")
                                        }
                                    }
                                } else if (generationId != currentGenerationId) {
                                    Log.d(TAG, "Skipping UI update - generation $generationId is not current ($currentGenerationId)")
                                }
                            }
                        } else {
                            Log.d(TAG, "Ignoring result - generation $generationId is not current ($currentGenerationId)")
                        }
                        
                        // Clean up when done
                        if (done) {
                            viewModelScope.launch(Dispatchers.IO) {
                                activeGenerations.remove(generationId)
                                
                                // If this was a cancelled generation, clean up
                                if (cancelledGenerations.contains(generationId)) {
                                    cancelledGenerations.remove(generationId)
                                    Log.d(TAG, "Cleaned up cancelled OCR generation: $generationId")
                                }
                            }
                        }
                    },
                    cleanUpListener = {
                        viewModelScope.launch(Dispatchers.Main) {
                            // Only update UI state if this is the current generation
                            if (generationId == currentGenerationId) {
                                // If we're still in streaming state, transition to error
                                val currentState = _ocrState.value
                                if (currentState is OcrState.Streaming && !currentState.isComplete) {
                                    _ocrState.value = OcrState.Error("OCR processing was interrupted")
                                }
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
                Log.e(TAG, "OCR processing failed", e)
                _ocrState.value = OcrState.Error("Failed to convert image. Please consider deleting and downloading the model again, or send us a bug report (see FAQ) if this persists. (detail: ${e.message})" ?: "Unknown error occurred")
            }
        }
    }
    
    fun resetOcrState() {
        _ocrState.value = OcrState.Idle
    }
    
    fun clearModelValidationError() {
        _modelValidationError.value = null
    }
    
    fun stopOcrGeneration() {
        Log.d(TAG, "stopOcrGeneration called - currentGenerationId: $currentGenerationId")
        
        // Mark the current generation as cancelled
        val oldGenerationId = currentGenerationId
        cancelledGenerations.add(oldGenerationId)
        
        // Create a new generation ID to prevent any further updates
        currentGenerationId = System.currentTimeMillis()
        
        Log.d(TAG, "Generation $oldGenerationId cancelled, new generationId: $currentGenerationId")
        
        // Update the state to show completion immediately
        val currentState = _ocrState.value
        if (currentState is OcrState.Streaming) {
            // Transition to success state with current text
            _ocrState.value = OcrState.Success(currentState.text)
            Log.d(TAG, "Transitioned to Success state with ${currentState.text.length} characters")
        }
        
        // Set flag to create new session on next generation
        needsNewSession = true
        
        // The cancelled generation will continue in the background but its results will be ignored
        // due to the generationId check in the resultListener
    }
    
    fun handleBackPressed(): Boolean {
        // Check if OCR generation is active
        val currentState = _ocrState.value
        val isGenerating = currentState is OcrState.Processing || currentState is OcrState.Streaming
        
        if (isGenerating) {
            _showBackConfirmDialog.value = true
            return true // Show dialog
        }
        
        return false // Don't show dialog, allow normal back navigation
    }
    
    fun confirmBackWithAbort() {
        // Stop the current generation
        stopOcrGeneration()
        _showBackConfirmDialog.value = false
    }
    
    fun dismissBackConfirmDialog() {
        _showBackConfirmDialog.value = false
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up model instance if needed
        currentModel?.let { model ->
            if (model.instance != null) {
                LlmChatModelHelper.cleanUp(model)
            }
        }
    }
}
