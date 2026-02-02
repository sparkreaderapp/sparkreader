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
* (This file was originally named GalleryApplication.kt)
*/

package app.sparkreader

import android.app.Application
import app.sparkreader.common.writeLaunchInfo
import app.sparkreader.data.DataStoreRepository
import app.sparkreader.ui.theme.ThemeSettings
//import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SparkReaderApplication : Application() {

  @Inject lateinit var dataStoreRepository: DataStoreRepository

  override fun onCreate() {
    super.onCreate()

    writeLaunchInfo(context = this)

    // Load saved theme.
    ThemeSettings.themeOverride.value = dataStoreRepository.readTheme()

    //FirebaseApp.initializeApp(this)
  }
}
