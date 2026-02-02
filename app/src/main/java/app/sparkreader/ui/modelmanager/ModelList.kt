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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sparkreader.data.Model
import app.sparkreader.data.Task

private const val TAG = "AGModelList"

/** The list of models in the model manager. */
@Composable
fun ModelList(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  contentPadding: PaddingValues,
  onModelClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
  headerContent: @Composable (() -> Unit)? = null,
) {
  // This is just to update "models" list when task.updateTrigger is updated so that the UI can
  // be properly updated.
  val models by
    remember(task) {
      derivedStateOf {
        val trigger = task.updateTrigger.value
        if (trigger >= 0) {
          task.models.toList().filter { !it.imported }
        } else {
          listOf()
        }
      }
    }
  val importedModels by
    remember(task) {
      derivedStateOf {
        val trigger = task.updateTrigger.value
        if (trigger >= 0) {
          task.models.toList().filter { it.imported }
        } else {
          listOf()
        }
      }
    }

  val listState = rememberLazyListState()

  Box(contentAlignment = Alignment.BottomEnd) {
    LazyColumn(
      modifier = modifier.padding(top = 8.dp),
      contentPadding = contentPadding,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      state = listState,
    ) {
      // Add header content if provided
      headerContent?.let {
        item {
          it()
        }
      }

      // List of models within a task.
      items(items = models) { model ->
        SimpleModelItem(
          model = model,
          task = task,
          modelManagerViewModel = modelManagerViewModel,
          onModelClicked = onModelClicked,
          modifier = Modifier.padding(horizontal = 12.dp),
        )
      }

      // Title for imported models.
      if (importedModels.isNotEmpty()) {
        item(key = "importedModelsTitle") {
          Text(
            "Imported models",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 24.dp),
          )
        }
      }

      // List of imported models within a task.
      items(items = importedModels, key = { it.name }) { model ->
        SimpleModelItem(
          model = model,
          task = task,
          modelManagerViewModel = modelManagerViewModel,
          onModelClicked = onModelClicked,
          modifier = Modifier.padding(horizontal = 12.dp),
        )
      }
    }
  }
}

// @Preview(showBackground = true)
// @Composable
// fun ModelListPreview() {
//   val context = LocalContext.current

//   GalleryTheme {
//     ModelList(
//       task = TASK_TEST1,
//       modelManagerViewModel = PreviewModelManagerViewModel(context = context),
//       onModelClicked = {},
//       contentPadding = PaddingValues(all = 16.dp),
//     )
//   }
// }
