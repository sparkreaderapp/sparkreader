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
* (This file was originally named GalleryLifecycleProvider.kt)
*/

package app.sparkreader

interface AppLifecycleProvider {
  var isAppInForeground: Boolean
}

class SparkReaderLifecycleProvider : AppLifecycleProvider {
  private var _isAppInForeground = false

  override var isAppInForeground: Boolean
    get() = _isAppInForeground
    set(value) {
      _isAppInForeground = value
    }
}
