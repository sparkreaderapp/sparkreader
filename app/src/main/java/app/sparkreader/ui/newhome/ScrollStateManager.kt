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

package app.sparkreader.ui.newhome

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Static field, contains all scroll values
 */
private val SaveMap = mutableMapOf<String, ScrollParams>()

private data class ScrollParams(
    val index: Int,
    val scrollOffset: Int
)

/**
 * Save scroll state persistently across navigation.
 * @param key unique identifier for the screen
 * @param initialFirstVisibleItemIndex initial index
 * @param initialFirstVisibleItemScrollOffset initial offset
 */
@Composable
fun rememberPersistentLazyListState(
    key: String,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0
): LazyListState {
    val savedValue = SaveMap[key]
    val savedIndex = savedValue?.index ?: initialFirstVisibleItemIndex
    val savedOffset = savedValue?.scrollOffset ?: initialFirstVisibleItemScrollOffset
    
    val scrollState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState(
            savedIndex,
            savedOffset
        )
    }
    
    DisposableEffect(key) {
        onDispose {
            val lastIndex = scrollState.firstVisibleItemIndex
            val lastOffset = scrollState.firstVisibleItemScrollOffset
            SaveMap[key] = ScrollParams(lastIndex, lastOffset)
        }
    }
    
    return scrollState
}
