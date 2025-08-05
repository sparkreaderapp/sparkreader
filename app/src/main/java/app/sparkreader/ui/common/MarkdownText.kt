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

package app.sparkreader.ui.common

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.viewinterop.AndroidView
import app.sparkreader.R
import app.sparkreader.ui.theme.customColors
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle

/** Composable function to display Markdown-formatted text. */
@Composable
fun MarkdownText(
  text: String, 
  modifier: Modifier = Modifier, 
  smallFontSize: Boolean = false,
  onTextLongPress: ((String) -> Unit)? = null
) {
  val fontSize =
    if (smallFontSize) MaterialTheme.typography.bodyMedium.fontSize
    else MaterialTheme.typography.bodyLarge.fontSize
  
  // If onTextLongPress is provided, use AndroidView with TextView for context menu support
  if (onTextLongPress != null) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface
    
    AndroidView(
      factory = { context ->
        TextView(context).apply {
          // Set plain text (markdown formatting will be lost but context menu will work)
          this.text = text
          
          textSize = fontSize.value
          setTextColor(textColor.toArgb())
          setPadding(0, 0, 0, 0)
          setTextIsSelectable(true)
          setLineSpacing(0f, 1.5f)
          
          // Set custom action mode callback for context menu
          customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
              return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
              // Add custom menu item
              menu?.add(0, 1001, 0, "Dictionary")?.apply {
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                //setIcon(R.drawable.circ)
              }
              return true
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
              return when (item?.itemId) {
                1001 -> {
                  onTextLongPress("")
                  mode?.finish()
                  true
                }
                else -> false
              }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
              // Clean up if needed
            }
          }
        }
      },
      modifier = modifier,
      update = { textView ->
        textView.text = text
        textView.textSize = fontSize.value
        textView.setTextColor(textColor.toArgb())
      }
    )
  } else {
    // Use the original RichText implementation when no context menu is needed
    CompositionLocalProvider {
      ProvideTextStyle(value = TextStyle(fontSize = fontSize, lineHeight = fontSize * 1.3)) {
        SelectionContainer {
          RichText(
            modifier = modifier,
            style =
              RichTextStyle(
                codeBlockStyle =
                  CodeBlockStyle(
                    textStyle =
                      TextStyle(
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        fontFamily = FontFamily.Monospace,
                      )
                  ),
                stringStyle =
                  RichTextStringStyle(
                    linkStyle =
                      TextLinkStyles(style = SpanStyle(color = MaterialTheme.customColors.linkColor))
                  ),
              ),
          ) {
            Markdown(content = text)
          }
        }
      }
    }
  }
}

// @Preview(showBackground = true)
// @Composable
// fun MarkdownTextPreview() {
//   GalleryTheme {
//     MarkdownText(text = "*Hello World*\n**Good morning!!**")
//   }
// }
