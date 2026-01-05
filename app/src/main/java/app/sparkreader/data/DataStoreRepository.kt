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

package app.sparkreader.data

import androidx.datastore.core.DataStore
import app.sparkreader.proto.AccessTokenData
import app.sparkreader.proto.ImportedModel
import app.sparkreader.proto.Settings
import app.sparkreader.proto.Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// TODO(b/423700720): Change to async (suspend) functions
interface DataStoreRepository {
  fun saveTextInputHistory(history: List<String>)

  fun readTextInputHistory(): List<String>

  fun saveTheme(theme: Theme)

  fun readTheme(): Theme

  fun saveAccessTokenData(accessToken: String, refreshToken: String, expiresAt: Long)

  fun clearAccessTokenData()

  fun readAccessTokenData(): AccessTokenData?

  fun saveImportedModels(importedModels: List<ImportedModel>)

  fun readImportedModels(): List<ImportedModel>
  
  fun saveSelectedModel(modelName: String)
  
  fun readSelectedModel(): String?
  
  fun saveLibraryVersion(version: String)
  
  fun readLibraryVersion(): String?
  
  fun saveLibraryDownloaded(isDownloaded: Boolean)
  
  fun readLibraryDownloaded(): Boolean
  
  fun saveTemperature(temperature: Float)
  
  fun readTemperature(): Float
  
  fun saveFontSize(fontSize: Float)
  
  fun readFontSize(): Float
  
}

/** Repository for managing data using Proto DataStore. */
class DefaultDataStoreRepository(private val dataStore: DataStore<Settings>) : DataStoreRepository {
  override fun saveTextInputHistory(history: List<String>) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().clearTextInputHistory().addAllTextInputHistory(history).build()
      }
    }
  }

  override fun readTextInputHistory(): List<String> {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.textInputHistoryList
    }
  }

  override fun saveTheme(theme: Theme) {
    runBlocking {
      dataStore.updateData { settings -> settings.toBuilder().setTheme(theme).build() }
    }
  }

  override fun readTheme(): Theme {
    return runBlocking {
      val settings = dataStore.data.first()
      val curTheme = settings.theme
      // Use "auto" as the default theme.
      if (curTheme == Theme.THEME_UNSPECIFIED) Theme.THEME_AUTO else curTheme
    }
  }

  override fun saveAccessTokenData(accessToken: String, refreshToken: String, expiresAt: Long) {
    runBlocking {
      dataStore.updateData { settings ->
        settings
          .toBuilder()
          .setAccessTokenData(
            AccessTokenData.newBuilder()
              .setAccessToken(accessToken)
              .setRefreshToken(refreshToken)
              .setExpiresAtMs(expiresAt)
              .build()
          )
          .build()
      }
    }
  }

  override fun clearAccessTokenData() {
    runBlocking {
      dataStore.updateData { settings -> settings.toBuilder().clearAccessTokenData().build() }
    }
  }

  override fun readAccessTokenData(): AccessTokenData? {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.accessTokenData
    }
  }

  override fun saveImportedModels(importedModels: List<ImportedModel>) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().clearImportedModel().addAllImportedModel(importedModels).build()
      }
    }
  }

  override fun readImportedModels(): List<ImportedModel> {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.importedModelList
    }
  }
  
  override fun saveSelectedModel(modelName: String) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().setSelectedModel(modelName).build()
      }
    }
  }
  
  override fun readSelectedModel(): String? {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.selectedModel.ifEmpty { null }
    }
  }
  
  override fun saveLibraryVersion(version: String) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().setLibraryVersion(version).build()
      }
    }
  }
  
  override fun readLibraryVersion(): String? {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.libraryVersion.ifEmpty { null }
    }
  }
  
  override fun saveLibraryDownloaded(isDownloaded: Boolean) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().setLibraryDownloaded(isDownloaded).build()
      }
    }
  }
  
  override fun readLibraryDownloaded(): Boolean {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.libraryDownloaded
    }
  }
  
  override fun saveTemperature(temperature: Float) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().setTemperature(temperature).build()
      }
    }
  }
  
  override fun readTemperature(): Float {
    return runBlocking {
      val settings = dataStore.data.first()
      // Default to 0.7 if not set or if it's 0
      val temp = settings.temperature
      if (temp == 0f) 0.7f else temp
    }
  }
  
  override fun saveFontSize(fontSize: Float) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().setFontSize(fontSize).build()
      }
    }
  }
  
  override fun readFontSize(): Float {
    return runBlocking {
      val settings = dataStore.data.first()
      // Default to 18f if not set or if it's 0
      val size = settings.fontSize
      if (size == 0f) 18f else size
    }
  }
  
}
