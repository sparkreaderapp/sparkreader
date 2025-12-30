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

package app.sparkreader.ui.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sparkreader.data.DataStoreRepository
import app.sparkreader.data.DownloadRepository
import app.sparkreader.data.LibraryVersion
import app.sparkreader.data.Model
import app.sparkreader.data.ModelDownloadStatus
import app.sparkreader.data.ModelDownloadStatusType
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

private const val TAG = "SettingsViewModel"
private const val VERSION_URL = "https://raw.githubusercontent.com/sparkreaderapp/sparkreader-library/refs/heads/main/VERSION.json"
private const val LIBRARY_DIR_NAME = "sparkreader-library"

data class LibraryState(
  val isDownloaded: Boolean = false,
  val currentVersion: String? = null,
  val latestVersion: LibraryVersion? = null,
  val downloadStatus: ModelDownloadStatus = ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED),
  val isCheckingUpdate: Boolean = false,
  val isDownloading: Boolean = false,
  val error: String = "",
  val shouldNavigateBack: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
  private val downloadRepository: DownloadRepository,
  private val dataStoreRepository: DataStoreRepository,
  @ApplicationContext private val context: Context
) : ViewModel() {
  
  private val _libraryState = MutableStateFlow(LibraryState())
  val libraryState = _libraryState.asStateFlow()
  
  private val externalFilesDir = context.getExternalFilesDir(null)
  private var currentLibraryModel: Model? = null
  private var navigateBackOnComplete = false
  
  init {
    loadLibraryState()
  }
  
  fun refreshLibraryState() {
    loadLibraryState()
  }
  
  private fun loadLibraryState() {
    viewModelScope.launch {
      val isDownloaded = dataStoreRepository.readLibraryDownloaded()
      val currentVersion = dataStoreRepository.readLibraryVersion()
      
      _libraryState.value = _libraryState.value.copy(
        isDownloaded = isDownloaded,
        currentVersion = currentVersion
      )
      
      // Check if library files actually exist
      if (isDownloaded) {
        val modelPath = File(externalFilesDir, "SparkReader_Library/_/$LIBRARY_DIR_NAME")
        val catalogFile = File(modelPath, "library/catalog.json")
        
        if (!catalogFile.exists()) {
          // Files don't exist, update state
          dataStoreRepository.saveLibraryDownloaded(false)
          _libraryState.value = _libraryState.value.copy(
            isDownloaded = false,
            currentVersion = null
          )
          // Check for updates since library is not actually downloaded
          checkForUpdates()
        }
      } else {
        // Check for updates if not downloaded
        checkForUpdates()
      }
    }
  }
  
  fun checkForUpdates() {
    if (_libraryState.value.isCheckingUpdate) return
    
    _libraryState.value = _libraryState.value.copy(
      isCheckingUpdate = true,
      error = ""
    )
    
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val latestVersion = fetchLatestVersion()
        
        withContext(Dispatchers.Main) {
          _libraryState.value = _libraryState.value.copy(
            isCheckingUpdate = false,
            latestVersion = latestVersion
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to check for updates", e)
        withContext(Dispatchers.Main) {
          _libraryState.value = _libraryState.value.copy(
            isCheckingUpdate = false,
            error = "Failed to check for updates"
          )
        }
      }
    }
  }
  
  private suspend fun fetchLatestVersion(): LibraryVersion = withContext(Dispatchers.IO) {
    val url = URL(VERSION_URL)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 10000
    connection.readTimeout = 10000
    
    try {
      val responseCode = connection.responseCode
      if (responseCode == HttpURLConnection.HTTP_OK) {
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val gson = Gson()
        return@withContext gson.fromJson(response, LibraryVersion::class.java)
      } else {
        throw Exception("HTTP error code: $responseCode")
      }
    } finally {
      connection.disconnect()
    }
  }
  
  fun downloadLibrary(fromImportBook: Boolean = false) {
    val latestVersion = _libraryState.value.latestVersion ?: return
    
    if (_libraryState.value.isDownloading) return
    
    navigateBackOnComplete = fromImportBook
    
    _libraryState.value = _libraryState.value.copy(
      isDownloading = true,
      error = "",
      downloadStatus = ModelDownloadStatus(status = ModelDownloadStatusType.CONNECTING)
    )
    
    viewModelScope.launch {
      val libraryZipName = "library-${latestVersion.version}.zip"
      val libraryUrl = "https://github.com/sparkreaderapp/sparkreader-library/releases/download/${latestVersion.version}/$libraryZipName"
      
      val libraryModel = Model(
        name = "SparkReader_Library",
        downloadFileName = libraryZipName,
        url = libraryUrl,
        sizeInBytes = latestVersion.sizeInBytes,
        isZip = true,
        unzipDir = LIBRARY_DIR_NAME
      ).apply {
        preProcess()
      }
      
      currentLibraryModel = libraryModel
      
      downloadRepository.downloadModel(
        model = libraryModel,
        onStatusUpdated = { _, status ->
          _libraryState.value = _libraryState.value.copy(
            downloadStatus = status
          )
          
          if (status.status == ModelDownloadStatusType.SUCCEEDED) {
            // Save library info
            dataStoreRepository.saveLibraryVersion(latestVersion.version)
            dataStoreRepository.saveLibraryDownloaded(true)
            
            _libraryState.value = _libraryState.value.copy(
              isDownloaded = true,
              currentVersion = latestVersion.version,
              isDownloading = false,
              latestVersion = null, // Clear latest version so "No update available" doesn't show
              shouldNavigateBack = navigateBackOnComplete
            )
            currentLibraryModel = null
            navigateBackOnComplete = false
          } else if (status.status == ModelDownloadStatusType.FAILED) {
            _libraryState.value = _libraryState.value.copy(
              error = status.errorMessage.ifEmpty { "Download failed" },
              isDownloading = false
            )
            currentLibraryModel = null
          }
        }
      )
    }
  }
  
  fun deleteLibrary() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        // Delete library files
        val modelPath = File(externalFilesDir, "SparkReader_Library")
        if (modelPath.exists()) {
          modelPath.deleteRecursively()
        }
        
        // Update data store
        dataStoreRepository.saveLibraryDownloaded(false)
        dataStoreRepository.saveLibraryVersion("")
        
        withContext(Dispatchers.Main) {
          _libraryState.value = _libraryState.value.copy(
            isDownloaded = false,
            currentVersion = null,
            downloadStatus = ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED),
            latestVersion = null // Clear latest version when deleting
          )
          // Check for updates after deletion
          checkForUpdates()
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to delete library", e)
        withContext(Dispatchers.Main) {
          _libraryState.value = _libraryState.value.copy(
            error = "Failed to delete library"
          )
        }
      }
    }
  }
  
  fun cancelDownload() {
    currentLibraryModel?.let { model ->
      downloadRepository.cancelDownloadModel(model)
      _libraryState.value = _libraryState.value.copy(
        isDownloading = false,
        downloadStatus = ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)
      )
      currentLibraryModel = null
      navigateBackOnComplete = false
    }
  }
  
  fun clearNavigateBack() {
    _libraryState.value = _libraryState.value.copy(shouldNavigateBack = false)
  }
}
