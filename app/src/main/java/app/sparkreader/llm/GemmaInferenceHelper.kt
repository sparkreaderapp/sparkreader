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

package app.sparkreader.llm

import android.content.Context
import app.sparkreader.data.Model
import app.sparkreader.data.ConfigKey
import app.sparkreader.data.DataStoreRepository
import app.sparkreader.data.getModelByName
import app.sparkreader.data.TASK_LLM_ASK_IMAGE
import app.sparkreader.data.ONLINE_MODEL_NAME
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ModelValidationResult(
    val isValid: Boolean,
    val isOnline: Boolean = false,
    val model: Model? = null,
    val modelFile: File? = null,
    val errorMessage: String? = null
)

@Singleton
class GemmaInferenceHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreRepository: DataStoreRepository
) {
    private var llmInference: LlmInference? = null
    private var currentModelPath: String? = null
    
    suspend fun generateResponse(
        model: Model,
        modelPath: String,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        try {
            // Initialize or reinitialize if model path changed
            if (llmInference == null || currentModelPath != modelPath) {
                llmInference?.close()
                llmInference = initializeLlmInference(model, modelPath)
                currentModelPath = modelPath
            }
            
            // Generate response
            val response = llmInference?.generateResponse(prompt)
                ?: throw Exception("Failed to generate response")
            
            return@withContext response
        } catch (e: Exception) {
            throw Exception("LLM inference failed: ${e.message}")
        }
    }
    
    private fun initializeLlmInference(model: Model, modelPath: String): LlmInference {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(model.getIntConfigValue(ConfigKey.MAX_TOKENS, 512))
            .build()
            
        return LlmInference.createFromOptions(context, options)
    }
    
    fun close() {
        llmInference?.close()
        llmInference = null
        currentModelPath = null
    }
    
    suspend fun validateSelectedModel(task: String = "text"): ModelValidationResult = withContext(Dispatchers.IO) {
        try {
            // Get the selected model from settings
            val selectedModelName = dataStoreRepository.readSelectedModel()
            
            if (selectedModelName.isNullOrEmpty()) {
                return@withContext ModelValidationResult(
                    isValid = false,
                    errorMessage = if (task == "text") "AI model is needed for contextual explanations and chat." else
                        "AI model is needed for converting image to text."
                )
            }

            if (selectedModelName == ONLINE_MODEL_NAME) {
                if (task == "image") {
                    return@withContext ModelValidationResult(
                        isValid = false,
                        errorMessage = "Online model doesn't support image analysis."
                    )
                }
                return@withContext ModelValidationResult(
                    isValid = true,
                    isOnline = true
                )
            }
            
            // Find the selected model
            val selectedModel = getModelByName(selectedModelName)
            if (selectedModel == null) {
                return@withContext ModelValidationResult(
                    isValid = false,
                    errorMessage = "Default AI model '$selectedModelName' not found."
                )
            }
            
            // Check if model is downloaded
            val externalFilesDir = context.getExternalFilesDir(null)
            val modelFile = if (selectedModel.imported) {
                File(externalFilesDir, selectedModel.downloadFileName)
            } else {
                File(externalFilesDir, "${selectedModel.normalizedName}/${selectedModel.version}/${selectedModel.downloadFileName}")
            }
            
            if (!modelFile.exists()) {
                return@withContext ModelValidationResult(
                    isValid = false,
                    errorMessage = "Default AI model '${selectedModel.name}' is not downloaded."
                )
            }
            
            // Check if model supports the requested task
            if (task == "image") {
                // Find if the model exists in the ask image task
                val supportsImage = TASK_LLM_ASK_IMAGE.models.any { it.name == selectedModel.name }
                if (!supportsImage) {
                    return@withContext ModelValidationResult(
                        isValid = false,
                        errorMessage = "Default AI model '${selectedModel.name}' doesn't support image analysis"
                    )
                }
            }
            
            return@withContext ModelValidationResult(
                isValid = true,
                model = selectedModel,
                modelFile = modelFile
            )
        } catch (e: Exception) {
            return@withContext ModelValidationResult(
                isValid = false,
                errorMessage = "Error validating model: ${e.message}"
            )
        }
    }
}
