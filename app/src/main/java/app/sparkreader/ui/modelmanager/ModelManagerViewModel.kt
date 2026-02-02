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
*/


package app.sparkreader.ui.modelmanager

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.activity.result.ActivityResultLauncher
import app.sparkreader.AppLifecycleProvider
import app.sparkreader.common.getJsonResponse
import app.sparkreader.data.AGWorkInfo
import app.sparkreader.data.Accelerator
import app.sparkreader.data.Config
import app.sparkreader.data.DataStoreRepository
import app.sparkreader.data.DownloadRepository
import app.sparkreader.data.EMPTY_MODEL
import app.sparkreader.data.IMPORTS_DIR
import app.sparkreader.data.Model
import app.sparkreader.data.ModelAllowlist
import app.sparkreader.data.ModelDownloadStatus
import app.sparkreader.data.ModelDownloadStatusType
import app.sparkreader.data.ONLINE_MODEL_NAME
import app.sparkreader.data.TASKS
import app.sparkreader.data.TASK_LLM_ASK_IMAGE
import app.sparkreader.data.Task
import app.sparkreader.data.TaskType
import app.sparkreader.data.createLlmChatConfigs
import app.sparkreader.data.getModelByName
import app.sparkreader.data.processTasks
import app.sparkreader.proto.AccessTokenData
import app.sparkreader.proto.ImportedModel
import app.sparkreader.proto.Theme
import app.sparkreader.ui.llmchat.LlmChatModelHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import app.sparkreader.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
//import net.openid.appauth.AuthorizationException
//import net.openid.appauth.AuthorizationRequest
//import net.openid.appauth.AuthorizationResponse
//import net.openid.appauth.AuthorizationService
//import net.openid.appauth.ResponseTypeValues

private const val TAG = "AGModelManagerViewModel"
private const val TEXT_INPUT_HISTORY_MAX_SIZE = 50
private const val MODEL_ALLOWLIST_URL =
  "https://raw.githubusercontent.com/sparkreaderapp/sparkreader/refs/heads/main/models.json"
private const val MODEL_ALLOWLIST_FILENAME = "model_allowlist.json"

data class ModelInitializationStatus(
  val status: ModelInitializationStatusType,
  var error: String = "",
)

enum class ModelInitializationStatusType {
  NOT_INITIALIZED,
  INITIALIZING,
  INITIALIZED,
  ERROR,
}

enum class TokenStatus {
  NOT_STORED,
  EXPIRED,
  NOT_EXPIRED,
}

enum class TokenRequestResultType {
  FAILED,
  SUCCEEDED,
  USER_CANCELLED,
}

data class TokenStatusAndData(val status: TokenStatus, val data: AccessTokenData?)

data class TokenRequestResult(val status: TokenRequestResultType, val errorMessage: String? = null)

data class ModelManagerUiState(
  /** A list of tasks available in the application. */
  val tasks: List<Task>,

  /** A map that tracks the download status of each model, indexed by model name. */
  val modelDownloadStatus: Map<String, ModelDownloadStatus>,

  /** A map that tracks the initialization status of each model, indexed by model name. */
  val modelInitializationStatus: Map<String, ModelInitializationStatus>,

  /** Whether the app is loading and processing the model allowlist. */
  val loadingModelAllowlist: Boolean = true,

  /** The error message when loading the model allowlist. */
  val loadingModelAllowlistError: String = "",

  /** The currently selected model. */
  val selectedModel: Model = EMPTY_MODEL,

  /** The history of text inputs entered by the user. */
  val textInputHistory: List<String> = listOf(),

  /** The name of the currently selected model. */
  val selectedModelName: String? = null,
)

data class PagerScrollState(val page: Int = 0, val offset: Float = 0f)

/**
 * ViewModel responsible for managing models, their download status, and initialization.
 *
 * This ViewModel handles model-related operations such as downloading, deleting, initializing, and
 * cleaning up models. It also manages the UI state for model management, including the list of
 * tasks, models, download statuses, and initialization statuses.
 */
@HiltViewModel
open class ModelManagerViewModel @Inject constructor(
  private val downloadRepository: DownloadRepository,
  private val dataStoreRepository: DataStoreRepository,
  private val lifecycleProvider: AppLifecycleProvider,
  @ApplicationContext private val context: Context,
) : ViewModel() {
  private val externalFilesDir = context.getExternalFilesDir(null)
  private val inProgressWorkInfos: List<AGWorkInfo> =
    downloadRepository.getEnqueuedOrRunningWorkInfos()
  protected val _uiState = MutableStateFlow(createEmptyUiState())
  val uiState = _uiState.asStateFlow()

  //val authService = AuthorizationService(context)
  var curAccessToken: String = ""

  var pagerScrollState: MutableStateFlow<PagerScrollState> = MutableStateFlow(PagerScrollState())

  init {
    loadModelAllowlist()
    // Load the initially selected model name first
    viewModelScope.launch {
      val selectedName = dataStoreRepository.readSelectedModel()
      _uiState.update { currentState ->
        currentState.copy(selectedModelName = selectedName)
      }
      // Then check and set default Gemma model (which will respect the loaded selection)
      checkAndSetDefaultGemmaModel()
    }
  }

  override fun onCleared() {
    super.onCleared()
    //authService.dispose()
  }

  fun selectModel(model: Model) {
    _uiState.update { _uiState.value.copy(selectedModel = model) }
  }

  fun downloadModel(task: Task, model: Model) {
    // Update status immediately to show download is connecting
    setDownloadStatus(
      curModel = model,
      status = ModelDownloadStatus(status = ModelDownloadStatusType.CONNECTING),
    )

    // Delete the model files first.
    deleteModel(task = task, model = model)

    // Check if model already has a direct download link (from modelDownloadLink)
    val hasDirectDownloadLink = model.url.contains("sparkreaderapp") || 
                                !model.url.contains("huggingface.co/") ||
                                model.url.contains("/resolve/")
    
    val finalModel = if (hasDirectDownloadLink) {
      // Model already has a direct download link, use it as-is
      Log.d(TAG, "Using direct download link for model '${model.name}': ${model.url}")
      model.copy(accessToken = null)
    } else {
      // For AllowedModel without modelDownloadLink, rewrite to sparkreaderapp
      val modelNamePart = extractModelIdFromUrl(model.url) ?: model.normalizedName
      val newUrl = "https://huggingface.co/sparkreaderapp/$modelNamePart/resolve/main/${model.downloadFileName}"
      
      // Also update extra data files URLs if they exist
      val updatedExtraDataFiles = model.extraDataFiles.map { dataFile ->
        dataFile.copy(
          url = "https://huggingface.co/sparkreaderapp/$modelNamePart/resolve/main/${dataFile.downloadFileName}"
        )
      }
      
      Log.d(TAG, "Rewriting URL for model '${model.name}' to sparkreaderapp: $newUrl")
      model.copy(
        url = newUrl,
        accessToken = null,
        extraDataFiles = updatedExtraDataFiles
      )
    }

    // Start download immediately without any OAuth checks
    Log.d(TAG, "Starting direct download for model '${model.name}'")
    downloadRepository.downloadModel(finalModel, onStatusUpdated = { curModel, status ->
      setDownloadStatus(curModel, status)
      
      // If download completed successfully, check if we need to auto-select
      if (status.status == ModelDownloadStatusType.SUCCEEDED) {
        viewModelScope.launch {
          checkAndAutoSelectSingleDownloadedModel()
        }
      }
    })
  }

  private fun extractModelIdFromUrl(url: String): String? {
    // Extract model ID from URLs like https://huggingface.co/org/modelId/...
    val regex = Regex("https://huggingface\\.co/[^/]+/([^/]+)")
    return regex.find(url)?.groupValues?.getOrNull(1)
  }
  
  private fun extractModelNameFromModelId(modelId: String): String {
    // Extract the model name part after the slash from modelId like "google/gemma-3n-E2B-it-litert-preview"
    val parts = modelId.split("/")
    return if (parts.size >= 2) parts[1] else modelId
  }
  
  fun startModelDownload(task: Task, model: Model) {
    // Direct download method that UI should call
    Log.d(TAG, "startModelDownload called for model: ${model.name}")
    
    // Request notification permission before starting download
    requestNotificationPermissionIfNeeded()
    
    downloadModel(task, model)
  }
  
  private fun requestNotificationPermissionIfNeeded() {
    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
      context, 
      android.Manifest.permission.POST_NOTIFICATIONS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    
    if (!hasPermission) {
      downloadRepository.requestNotificationPermission()
    }
  }

  fun cancelDownloadModel(task: Task, model: Model) {
    downloadRepository.cancelDownloadModel(model)
    deleteModel(task = task, model = model)
  }

  fun deleteModel(task: Task, model: Model) {
    if (model.imported) {
      deleteFileFromExternalFilesDir(model.downloadFileName)
    } else {
      deleteDirFromExternalFilesDir(model.normalizedName)
    }

    // Update model download status to NotDownloaded.
    val curModelDownloadStatus = uiState.value.modelDownloadStatus.toMutableMap()
    curModelDownloadStatus[model.name] =
      ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)

    // Delete model from the list if model is imported as a local model.
    if (model.imported) {
      for (curTask in TASKS) {
        val index = curTask.models.indexOf(model)
        if (index >= 0) {
          curTask.models.removeAt(index)
        }
        curTask.updateTrigger.value = System.currentTimeMillis()
      }
      curModelDownloadStatus.remove(model.name)

      // Update data store.
      val importedModels = dataStoreRepository.readImportedModels().toMutableList()
      val importedModelIndex = importedModels.indexOfFirst { it.fileName == model.name }
      if (importedModelIndex >= 0) {
        importedModels.removeAt(importedModelIndex)
      }
      dataStoreRepository.saveImportedModels(importedModels = importedModels)
    }
    
    // If the deleted model was the selected one, clear the selection
    if (uiState.value.selectedModelName == model.name) {
      dataStoreRepository.saveSelectedModel("")
      _uiState.update { currentState ->
        currentState.copy(selectedModelName = null)
      }
    }
    
    val newUiState =
      uiState.value.copy(
        modelDownloadStatus = curModelDownloadStatus,
        tasks = uiState.value.tasks.toList(),
      )
    _uiState.update { newUiState }
    
    // Check if we need to auto-select the only remaining downloaded model
    checkAndAutoSelectSingleDownloadedModel()
  }

  fun initializeModel(context: Context, task: Task, model: Model, force: Boolean = false) {
    viewModelScope.launch(Dispatchers.Default) {
      // Skip if initialized already.
      if (
        !force &&
          uiState.value.modelInitializationStatus[model.name]?.status ==
            ModelInitializationStatusType.INITIALIZED
      ) {
        Log.d(TAG, "Model '${model.name}' has been initialized. Skipping.")
        return@launch
      }

      // Skip if initialization is in progress.
      if (model.initializing) {
        model.cleanUpAfterInit = false
        Log.d(TAG, "Model '${model.name}' is being initialized. Skipping.")
        return@launch
      }

      // Clean up.
      cleanupModel(task = task, model = model)

      // Start initialization.
      Log.d(TAG, "Initializing model '${model.name}'...")
      model.initializing = true

      // Show initializing status after a delay. When the delay expires, check if the model has
      // been initialized or not. If so, skip.
      launch {
        delay(500)
        if (model.instance == null && model.initializing) {
          updateModelInitializationStatus(
            model = model,
            status = ModelInitializationStatusType.INITIALIZING,
          )
        }
      }

      val onDone: (error: String) -> Unit = { error ->
        model.initializing = false
        if (model.instance != null) {
          Log.d(TAG, "Model '${model.name}' initialized successfully")
          updateModelInitializationStatus(
            model = model,
            status = ModelInitializationStatusType.INITIALIZED,
          )
          if (model.cleanUpAfterInit) {
            Log.d(TAG, "Model '${model.name}' needs cleaning up after init.")
            cleanupModel(task = task, model = model)
          }
        } else if (error.isNotEmpty()) {
          Log.d(TAG, "Model '${model.name}' failed to initialize")
          updateModelInitializationStatus(
            model = model,
            status = ModelInitializationStatusType.ERROR,
            error = error,
          )
        }
      }
      when (task.type) {
        TaskType.LLM_CHAT,
        TaskType.LLM_ASK_IMAGE,
        TaskType.LLM_ASK_AUDIO,
        TaskType.LLM_PROMPT_LAB ->
          LlmChatModelHelper.initialize(context = context, model = model, onDone = onDone)

        TaskType.TEST_TASK_1 -> {}
        TaskType.TEST_TASK_2 -> {}
      }
    }
  }

  fun cleanupModel(task: Task, model: Model) {
    if (model.instance != null) {
      model.cleanUpAfterInit = false
      Log.d(TAG, "Cleaning up model '${model.name}'...")
      when (task.type) {
        TaskType.LLM_CHAT,
        TaskType.LLM_PROMPT_LAB,
        TaskType.LLM_ASK_IMAGE,
        TaskType.LLM_ASK_AUDIO -> LlmChatModelHelper.cleanUp(model = model)

        TaskType.TEST_TASK_1 -> {}
        TaskType.TEST_TASK_2 -> {}
      }
      model.instance = null
      model.initializing = false
      updateModelInitializationStatus(
        model = model,
        status = ModelInitializationStatusType.NOT_INITIALIZED,
      )
    } else {
      // When model is being initialized and we are trying to clean it up at same time, we mark it
      // to clean up and it will be cleaned up after initialization is done.
      if (model.initializing) {
        model.cleanUpAfterInit = true
      }
    }
  }

  fun setDownloadStatus(curModel: Model, status: ModelDownloadStatus) {
    // Update model download progress.
    val curModelDownloadStatus = uiState.value.modelDownloadStatus.toMutableMap()
    curModelDownloadStatus[curModel.name] = status
    val newUiState = uiState.value.copy(modelDownloadStatus = curModelDownloadStatus)

    // Delete downloaded file if status is failed or not_downloaded.
    if (
      status.status == ModelDownloadStatusType.FAILED ||
        status.status == ModelDownloadStatusType.NOT_DOWNLOADED
    ) {
      deleteFileFromExternalFilesDir(curModel.downloadFileName)
    }

    _uiState.update { newUiState }
  }

  fun addTextInputHistory(text: String) {
    if (uiState.value.textInputHistory.indexOf(text) < 0) {
      val newHistory = uiState.value.textInputHistory.toMutableList()
      newHistory.add(0, text)
      if (newHistory.size > TEXT_INPUT_HISTORY_MAX_SIZE) {
        newHistory.removeAt(newHistory.size - 1)
      }
      _uiState.update { _uiState.value.copy(textInputHistory = newHistory) }
      dataStoreRepository.saveTextInputHistory(_uiState.value.textInputHistory)
    } else {
      promoteTextInputHistoryItem(text)
    }
  }

  fun promoteTextInputHistoryItem(text: String) {
    val index = uiState.value.textInputHistory.indexOf(text)
    if (index >= 0) {
      val newHistory = uiState.value.textInputHistory.toMutableList()
      newHistory.removeAt(index)
      newHistory.add(0, text)
      _uiState.update { _uiState.value.copy(textInputHistory = newHistory) }
      dataStoreRepository.saveTextInputHistory(_uiState.value.textInputHistory)
    }
  }

  fun deleteTextInputHistory(text: String) {
    val index = uiState.value.textInputHistory.indexOf(text)
    if (index >= 0) {
      val newHistory = uiState.value.textInputHistory.toMutableList()
      newHistory.removeAt(index)
      _uiState.update { _uiState.value.copy(textInputHistory = newHistory) }
      dataStoreRepository.saveTextInputHistory(_uiState.value.textInputHistory)
    }
  }

  fun clearTextInputHistory() {
    _uiState.update { _uiState.value.copy(textInputHistory = mutableListOf()) }
    dataStoreRepository.saveTextInputHistory(_uiState.value.textInputHistory)
  }

  fun readThemeOverride(): Theme {
    return dataStoreRepository.readTheme()
  }

  fun saveThemeOverride(theme: Theme) {
    dataStoreRepository.saveTheme(theme = theme)
  }
  
  fun getSelectedModel(): String? {
    return dataStoreRepository.readSelectedModel()
  }
  
  fun setSelectedModel(modelName: String) {
    dataStoreRepository.saveSelectedModel(modelName)
    // Update UI state immediately
    _uiState.update { currentState ->
      currentState.copy(selectedModelName = modelName)
    }
    Log.d(TAG, "Model selection updated: $modelName")
  }
  
  private fun checkAndAutoSelectSingleDownloadedModel() {
    viewModelScope.launch {
      val allModels = getAllModelsFromTasks()
      val downloadedModels = allModels.filter { model ->
        isModelDownloaded(model)
      }
      
      // If there's exactly one downloaded model and no model is currently selected, select it
      if (downloadedModels.size == 1) {
        val singleModel = downloadedModels.first()
        val currentSelected = dataStoreRepository.readSelectedModel()
        
        if (currentSelected == ONLINE_MODEL_NAME) {
          return@launch
        }
        
        if (currentSelected.isNullOrEmpty() || !isModelDownloaded(getModelByName(currentSelected) ?: EMPTY_MODEL)) {
          dataStoreRepository.saveSelectedModel(singleModel.name)
          _uiState.update { currentState ->
            currentState.copy(selectedModelName = singleModel.name)
          }
          Log.d(TAG, "Automatically selected '${singleModel.name}' as it's the only downloaded model")
        }
      }
    }
  }
  
  private fun checkAndSetDefaultGemmaModel() {
    viewModelScope.launch {
      // Get the current selected model from UI state (which should already be loaded from datastore)
      val selectedModelName = _uiState.value.selectedModelName
      
      // Wait for models to be loaded
      delay(1000)
      
      val allModels = getAllModelsFromTasks()
      
      if (!selectedModelName.isNullOrEmpty()) {
        if (selectedModelName == ONLINE_MODEL_NAME) {
          return@launch
        }
        // Check if the selected model still exists AND is downloaded
        val selectedModel = allModels.firstOrNull { model ->
          model.name == selectedModelName
        }
        
        if (selectedModel == null) {
          // Selected model doesn't exist in the model list anymore, clear the selection
          dataStoreRepository.saveSelectedModel("")
          _uiState.update { currentState ->
            currentState.copy(selectedModelName = null)
          }
          Log.d(TAG, "Selected model '$selectedModelName' no longer exists in model list. Cleared selection.")
        } else if (!isModelDownloaded(selectedModel)) {
          // Selected model exists but is not downloaded, clear the selection
          dataStoreRepository.saveSelectedModel("")
          _uiState.update { currentState ->
            currentState.copy(selectedModelName = null)
          }
          Log.d(TAG, "Selected model '$selectedModelName' is not downloaded. Cleared selection.")
        } else {
          // Model exists and is downloaded, ensure it's properly set in UI state
          Log.d(TAG, "Selected model '$selectedModelName' is valid and downloaded.")
        }
      } else {
        // No model selected, try to find and set a downloaded Gemma model
        val downloadedGemmaModel = allModels.firstOrNull { model ->
          model.name.contains("Gemma", ignoreCase = true) && isModelDownloaded(model)
        }
        
        if (downloadedGemmaModel != null) {
          dataStoreRepository.saveSelectedModel(downloadedGemmaModel.name)
          _uiState.update { currentState ->
            currentState.copy(selectedModelName = downloadedGemmaModel.name)
          }
          Log.d(TAG, "Automatically set '${downloadedGemmaModel.name}' as the default model")
        }
      }
    }
  }
  
  private fun getAllModelsFromTasks(): List<Model> {
    val models = mutableListOf<Model>()
    for (task in TASKS) {
      models.addAll(task.models)
    }
    return models
  }

  fun getModelUrlResponse(model: Model, accessToken: String? = null): Int {
    // Always return 200 OK to bypass access check
    Log.d(TAG, "Bypassing model URL access check - returning 200 OK")
    return HttpURLConnection.HTTP_OK
  }


  private fun processPendingDownloads() {
    Log.d(TAG, "In-progress worker infos: $inProgressWorkInfos")

    // Iterate through the inProgressWorkInfos and retrieve the corresponding modes.
    // Those models are the ones that have not finished downloading.
    val models: MutableList<Model> = mutableListOf()
    for (info in inProgressWorkInfos) {
      getModelByName(info.modelName)?.let { model -> models.add(model) }
    }

    // Cancel all pending downloads for the retrieved models.
    downloadRepository.cancelAll(models) {
      Log.d(TAG, "All pending work is cancelled")

      viewModelScope.launch(Dispatchers.IO) {
        // Kick off downloads for these models .
        withContext(Dispatchers.Main) {
          for (info in inProgressWorkInfos) {
            val model: Model? = getModelByName(info.modelName)
            if (model != null) {
              // Check if model already has a direct download link
              val hasDirectDownloadLink = model.url.contains("sparkreaderapp") || 
                                          !model.url.contains("huggingface.co/") ||
                                          model.url.contains("/resolve/")
              
              val updatedModel = if (hasDirectDownloadLink) {
                // Model already has a direct download link, use it as-is
                model.copy(accessToken = null)
              } else {
                // Extract model name part for sparkreaderapp URL
                val modelNamePart = extractModelIdFromUrl(model.url) ?: model.normalizedName
                val newUrl = "https://huggingface.co/sparkreaderapp/$modelNamePart/resolve/main/${model.downloadFileName}"
                
                // Also update extra data files URLs if they exist
                val updatedExtraDataFiles = model.extraDataFiles.map { dataFile ->
                  dataFile.copy(
                    url = "https://huggingface.co/sparkreaderapp/$modelNamePart/resolve/main/${dataFile.downloadFileName}"
                  )
                }
                
                model.copy(
                  url = newUrl,
                  accessToken = null,
                  extraDataFiles = updatedExtraDataFiles
                )
              }
              
              Log.d(TAG, "Sending a new download request for '${model.name}'")
              downloadRepository.downloadModel(
                updatedModel,
                onStatusUpdated = { curModel, status ->
                  setDownloadStatus(curModel, status)
                  
                  // If download completed successfully, check if we need to auto-select
                  if (status.status == ModelDownloadStatusType.SUCCEEDED) {
                    viewModelScope.launch {
                      checkAndAutoSelectSingleDownloadedModel()
                    }
                  }
                },
              )
            }
          }
        }
      }
    }
  }

  fun loadModelAllowlist() {
    _uiState.update {
      uiState.value.copy(loadingModelAllowlist = true, loadingModelAllowlistError = "")
    }

    viewModelScope.launch(Dispatchers.IO) {
      try {
        // Load model allowlist json.
        Log.d(TAG, "Loading model allowlist from internet...")
        val data = getJsonResponse<ModelAllowlist>(url = MODEL_ALLOWLIST_URL)
        var modelAllowlist: ModelAllowlist? = data?.jsonObj

        if (modelAllowlist == null) {
          Log.d(TAG, "Failed to load model allowlist from internet. Trying to load it from disk")
          modelAllowlist = readModelAllowlistFromDisk()
        } else {
          Log.d(TAG, "Done: loading model allowlist from internet")
          saveModelAllowlistToDisk(modelAllowlistContent = data?.textContent ?: "{}")
        }

        if (modelAllowlist == null) {
          _uiState.update {
            uiState.value.copy(loadingModelAllowlistError = "Failed to load model list")
          }
          return@launch
        }

        Log.d(TAG, "Allowlist: $modelAllowlist")

        // Convert models in the allowlist.
        TASK_LLM_ASK_IMAGE.models.clear()
        for (allowedModel in modelAllowlist.models) {
          if (allowedModel.disabled == true) {
            continue
          }
          
          // Skip "test" models in release builds
          if (!BuildConfig.DEBUG && allowedModel.name.equals("test", ignoreCase = true)) {
            continue
          }

          var model = allowedModel.toModel()
          
          // Only rewrite URL if modelDownloadLink was not provided
          if (allowedModel.modelDownloadLink == null) {
            // Update model URL to use sparkreaderapp organization
            // Extract the model name part from modelId (e.g., "google/gemma-3n-E2B-it-litert-preview" -> "gemma-3n-E2B-it-litert-preview")
            val modelNamePart = extractModelNameFromModelId(allowedModel.modelId)
            val newUrl = "https://huggingface.co/sparkreaderapp/$modelNamePart/resolve/main/${model.downloadFileName}"
            
            // Also update extra data files URLs if they exist
            val updatedExtraDataFiles = model.extraDataFiles.map { dataFile ->
              dataFile.copy(
                url = "https://huggingface.co/sparkreaderapp/$modelNamePart/resolve/main/${dataFile.downloadFileName}"
              )
            }
            
            // Create a new model instance with the updated URL
            model = model.copy(
              url = newUrl,
              extraDataFiles = updatedExtraDataFiles
            )
          }
          
          if (allowedModel.taskTypes.contains(TASK_LLM_ASK_IMAGE.type.id)) {
            TASK_LLM_ASK_IMAGE.models.add(model)
          }
        }

        // Pre-process all tasks.
        processTasks()

        // Update UI state while preserving the selected model name
        val currentSelectedModelName = _uiState.value.selectedModelName
        val newUiState = createUiState()
        _uiState.update { 
          newUiState.copy(
            loadingModelAllowlist = false,
            selectedModelName = currentSelectedModelName
          ) 
        }

        // Process pending downloads.
        processPendingDownloads()
        
        // Check if we need to set a default Gemma model after models are loaded
        checkAndSetDefaultGemmaModel()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun setAppInForeground(foreground: Boolean) {
    lifecycleProvider.isAppInForeground = foreground
  }
  
  fun setNotificationPermissionLauncher(launcher: ActivityResultLauncher<String>?) {
    downloadRepository.setNotificationPermissionLauncher(launcher)
  }

  fun reloadModelExistence() {
    Log.d(TAG, "Reloading model existence status...")
    viewModelScope.launch(Dispatchers.IO) {
      val modelDownloadStatus: MutableMap<String, ModelDownloadStatus> = mutableMapOf()
      
      // Check all models in all tasks
      for (task in TASKS) {
        for (model in task.models) {
          modelDownloadStatus[model.name] = getModelDownloadStatus(model = model)
        }
      }
      
      // Update UI state with new download statuses
      withContext(Dispatchers.Main) {
        _uiState.update { 
          uiState.value.copy(modelDownloadStatus = modelDownloadStatus)
        }
        
        // Check if we need to auto-select the only downloaded model
        checkAndAutoSelectSingleDownloadedModel()
      }
      
      Log.d(TAG, "Model existence status reloaded")
    }
  }

  private fun saveModelAllowlistToDisk(modelAllowlistContent: String) {
    try {
      Log.d(TAG, "Saving model allowlist to disk...")
      val file = File(externalFilesDir, MODEL_ALLOWLIST_FILENAME)
      file.writeText(modelAllowlistContent)
      Log.d(TAG, "Done: saving model allowlist to disk.")
    } catch (e: Exception) {
      Log.e(TAG, "failed to write model allowlist to disk", e)
    }
  }

  private fun readModelAllowlistFromDisk(): ModelAllowlist? {
    try {
      Log.d(TAG, "Reading model allowlist from disk...")
      val file = File(externalFilesDir, MODEL_ALLOWLIST_FILENAME)
      if (file.exists()) {
        val content = file.readText()
        Log.d(TAG, "Model allowlist content from local file: $content")

        val gson = Gson()
        val type = object : TypeToken<ModelAllowlist>() {}.type
        return gson.fromJson<ModelAllowlist>(content, type)
      }
    } catch (e: Exception) {
      Log.e(TAG, "failed to read model allowlist from disk", e)
      return null
    }

    return null
  }

  private fun isModelPartiallyDownloaded(model: Model): Boolean {
    return inProgressWorkInfos.find { it.modelName == model.name } != null
  }

  private fun createEmptyUiState(): ModelManagerUiState {
    return ModelManagerUiState(
      tasks = listOf(),
      modelDownloadStatus = mapOf(),
      modelInitializationStatus = mapOf(),
    )
  }

  private fun createUiState(): ModelManagerUiState {
    val modelDownloadStatus: MutableMap<String, ModelDownloadStatus> = mutableMapOf()
    val modelInstances: MutableMap<String, ModelInitializationStatus> = mutableMapOf()
    for (task in TASKS) {
      for (model in task.models) {
        modelDownloadStatus[model.name] = getModelDownloadStatus(model = model)
        modelInstances[model.name] =
          ModelInitializationStatus(status = ModelInitializationStatusType.NOT_INITIALIZED)
      }
    }

    // Load imported models.
    for (importedModel in dataStoreRepository.readImportedModels()) {
      Log.d(TAG, "stored imported model: $importedModel")

      // Create model.
      val model = createModelFromImportedModelInfo(info = importedModel)

      // Add to task.
      if (model.llmSupportImage) {
        TASK_LLM_ASK_IMAGE.models.add(model)
      }

      // Update status.
      modelDownloadStatus[model.name] =
        ModelDownloadStatus(
          status = ModelDownloadStatusType.SUCCEEDED,
          receivedBytes = importedModel.fileSize,
          totalBytes = importedModel.fileSize,
        )
    }

    val textInputHistory = dataStoreRepository.readTextInputHistory()
    Log.d(TAG, "text input history: $textInputHistory")

    Log.d(TAG, "model download status: $modelDownloadStatus")
    return ModelManagerUiState(
      tasks = TASKS.toList(),
      modelDownloadStatus = modelDownloadStatus,
      modelInitializationStatus = modelInstances,
      textInputHistory = textInputHistory,
    )
  }

  private fun createModelFromImportedModelInfo(info: ImportedModel): Model {
    val accelerators: List<Accelerator> =
      info.llmConfig.compatibleAcceleratorsList.mapNotNull { acceleratorLabel ->
        when (acceleratorLabel.trim()) {
          Accelerator.GPU.label -> Accelerator.GPU
          Accelerator.CPU.label -> Accelerator.CPU
          else -> null // Ignore unknown accelerator labels
        }
      }
    val configs: List<Config> =
      createLlmChatConfigs(
        defaultMaxToken = info.llmConfig.defaultMaxTokens,
        defaultTopK = info.llmConfig.defaultTopk,
        defaultTopP = info.llmConfig.defaultTopp,
        defaultTemperature = info.llmConfig.defaultTemperature,
        accelerators = accelerators,
      )
    val llmSupportImage = info.llmConfig.supportImage
    val llmSupportAudio = info.llmConfig.supportAudio
    val model =
      Model(
        name = info.fileName,
        url = "",
        configs = configs,
        sizeInBytes = info.fileSize,
        downloadFileName = "$IMPORTS_DIR/${info.fileName}",
        showBenchmarkButton = false,
        showRunAgainButton = false,
        imported = true,
        llmSupportImage = llmSupportImage,
        llmSupportAudio = llmSupportAudio,
      )
    model.preProcess()

    return model
  }

  /**
   * Retrieves the download status of a model.
   *
   * This function determines the download status of a given model by checking if it's fully
   * downloaded, partially downloaded, or not downloaded at all. It also retrieves the received and
   * total bytes for partially downloaded models.
   */
  private fun getModelDownloadStatus(model: Model): ModelDownloadStatus {
    var status = ModelDownloadStatusType.NOT_DOWNLOADED
    var receivedBytes = 0L
    var totalBytes = 0L
    if (isModelDownloaded(model = model)) {
      if (isModelPartiallyDownloaded(model = model)) {
        status = ModelDownloadStatusType.PARTIALLY_DOWNLOADED
        val file = File(externalFilesDir, model.downloadFileName)
        receivedBytes = file.length()
        totalBytes = model.totalBytes
      } else {
        status = ModelDownloadStatusType.SUCCEEDED
      }
    }
    return ModelDownloadStatus(
      status = status,
      receivedBytes = receivedBytes,
      totalBytes = totalBytes,
    )
  }

  private fun isFileInExternalFilesDir(fileName: String): Boolean {
    if (externalFilesDir != null) {
      val file = File(externalFilesDir, fileName)
      return file.exists()
    } else {
      return false
    }
  }

  private fun deleteFileFromExternalFilesDir(fileName: String) {
    if (isFileInExternalFilesDir(fileName)) {
      val file = File(externalFilesDir, fileName)
      file.delete()
    }
  }

  private fun deleteDirFromExternalFilesDir(dir: String) {
    if (isFileInExternalFilesDir(dir)) {
      val file = File(externalFilesDir, dir)
      file.deleteRecursively()
    }
  }

  private fun updateModelInitializationStatus(
    model: Model,
    status: ModelInitializationStatusType,
    error: String = "",
  ) {
    val curModelInstance = uiState.value.modelInitializationStatus.toMutableMap()
    curModelInstance[model.name] = ModelInitializationStatus(status = status, error = error)
    val newUiState = uiState.value.copy(modelInitializationStatus = curModelInstance)
    _uiState.update { newUiState }
  }

  private fun isModelDownloaded(model: Model): Boolean {
    val downloadedFileExists =
      model.downloadFileName.isNotEmpty() &&
        isFileInExternalFilesDir(
          listOf(model.normalizedName, model.version, model.downloadFileName)
            .joinToString(File.separator)
        )

    val unzippedDirectoryExists =
      model.isZip &&
        model.unzipDir.isNotEmpty() &&
        isFileInExternalFilesDir(
          listOf(model.normalizedName, model.version, model.unzipDir).joinToString(File.separator)
        )

    // Will also return true if model is partially downloaded.
    return downloadedFileExists || unzippedDirectoryExists
  }
}
