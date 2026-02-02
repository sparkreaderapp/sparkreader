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

import android.content.Context
import java.io.File

data class ModelDataFile(
  val name: String,
  val url: String,
  val downloadFileName: String,
  val sizeInBytes: Long,
)

const val IMPORTS_DIR = "__imports"
private val NORMALIZE_NAME_REGEX = Regex("[^a-zA-Z0-9]")

data class PromptTemplate(val title: String, val description: String, val prompt: String)

/** A model for a task */
data class Model(
  /** The name (for display purpose) of the model. */
  val name: String,

  /** The version of the model. */
  val version: String = "_",

  /**
   * The name of the downloaded model file.
   *
   * The final file path of the downloaded model will be:
   * {context.getExternalFilesDir}/{normalizedName}/{version}/{downloadFileName}
   */
  val downloadFileName: String,

  /** The URL to download the model from. */
  val url: String,

  /** The size of the model file in bytes. */
  val sizeInBytes: Long,

  /** A list of additional data files required by the model. */
  val extraDataFiles: List<ModelDataFile> = listOf(),

  /**
   * A description or information about the model.
   *
   * Will be shown at the start of the chat session and in the expanded model item.
   */
  val info: String = "",

  /** The url to jump to when clicking "learn more" in expanded model item. */
  val learnMoreUrl: String = "",

  /** The link to the model's page (e.g., Hugging Face model card). */
  val modelLink: String = "",

  /** A list of configurable parameters for the model. */
  val configs: List<Config> = listOf(),

  /** Whether to show the "run again" button in the UI. */
  val showRunAgainButton: Boolean = true,

  /** Whether to show the "benchmark" button in the UI. */
  val showBenchmarkButton: Boolean = true,

  /** Indicates whether the model is a zip file. */
  val isZip: Boolean = false,

  /** The name of the directory to unzip the model to (if it's a zip file). */
  val unzipDir: String = "",

  /** The prompt templates for the model (only for LLM). */
  val llmPromptTemplates: List<PromptTemplate> = listOf(),

  /** Whether the LLM model supports image input. */
  val llmSupportImage: Boolean = false,

  /** Whether the LLM model supports audio input. */
  val llmSupportAudio: Boolean = false,

  /** Whether the model is imported or not. */
  val imported: Boolean = false,

  // The following fields are managed by the app. Don't need to set manually.
  var normalizedName: String = "",
  var instance: Any? = null,
  var initializing: Boolean = false,
  // TODO(jingjin): use a "queue" system to manage model init and cleanup.
  var cleanUpAfterInit: Boolean = false,
  var configValues: Map<String, Any> = mapOf(),
  var totalBytes: Long = 0L,
  var accessToken: String? = null,

  /** The estimated peak memory in byte to run the model. */
  val estimatedPeakMemoryInBytes: Long? = null,
) {
  init {
    normalizedName = NORMALIZE_NAME_REGEX.replace(name, "_")
  }

  fun preProcess() {
    val configValues: MutableMap<String, Any> = mutableMapOf()
    for (config in this.configs) {
      configValues[config.key.label] = config.defaultValue
    }
    this.configValues = configValues
    this.totalBytes = this.sizeInBytes + this.extraDataFiles.sumOf { it.sizeInBytes }
  }

  fun getPath(context: Context, fileName: String = downloadFileName): String {
    if (imported) {
      return listOf(context.getExternalFilesDir(null)?.absolutePath ?: "", fileName)
        .joinToString(File.separator)
    }

    val baseDir =
      listOf(context.getExternalFilesDir(null)?.absolutePath ?: "", normalizedName, version)
        .joinToString(File.separator)
    return if (this.isZip && this.unzipDir.isNotEmpty()) {
      "$baseDir/${this.unzipDir}"
    } else {
      "$baseDir/$fileName"
    }
  }

  fun getIntConfigValue(key: ConfigKey, defaultValue: Int = 0): Int {
    return getTypedConfigValue(key = key, valueType = ValueType.INT, defaultValue = defaultValue) as Int
  }

  fun getFloatConfigValue(key: ConfigKey, defaultValue: Float = 0.0f): Float {
    return getTypedConfigValue(key = key, valueType = ValueType.FLOAT, defaultValue = defaultValue) as Float
  }

  fun getBooleanConfigValue(key: ConfigKey, defaultValue: Boolean = false): Boolean {
    return getTypedConfigValue(
      key = key,
      valueType = ValueType.BOOLEAN,
      defaultValue = defaultValue,
    ) as Boolean
  }

  fun getStringConfigValue(key: ConfigKey, defaultValue: String = ""): String {
    return getTypedConfigValue(key = key, valueType = ValueType.STRING, defaultValue = defaultValue) as String
  }

  fun getExtraDataFile(name: String): ModelDataFile? {
    return extraDataFiles.find { it.name == name }
  }

  private fun getTypedConfigValue(key: ConfigKey, valueType: ValueType, defaultValue: Any): Any {
    return convertValueToTargetType(
      value = configValues.getOrDefault(key.label, defaultValue),
      valueType = valueType,
    )
  }
}

enum class ModelDownloadStatusType {
  NOT_DOWNLOADED,
  PARTIALLY_DOWNLOADED,
  CONNECTING,
  IN_PROGRESS,
  UNZIPPING,
  SUCCEEDED,
  FAILED,
}

data class ModelDownloadStatus(
  val status: ModelDownloadStatusType,
  val totalBytes: Long = 0,
  val receivedBytes: Long = 0,
  val errorMessage: String = "",
  val bytesPerSecond: Long = 0,
  val remainingMs: Long = 0,
)

////////////////////////////////////////////////////////////////////////////////////////////////////
// Configs.

val EMPTY_MODEL: Model =
  Model(name = "empty", downloadFileName = "empty.tflite", url = "", sizeInBytes = 0L)