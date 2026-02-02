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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun OcrSourceIcon(
    icon: ImageVector,
    contentDescription: String,
    isEnabled: Boolean,
    isProcessing: Boolean,
    hasError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (isProcessing) {
            // Show spinner when processing
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            // Show icon
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(enabled = isEnabled) { onClick() },
                tint = if (isEnabled) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            // Error indicator
            if (hasError) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Model Error",
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
