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

package app.sparkreader.ui.helpfeedback

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.graphics.Color

data class FaqItem(
  val question: String,
  val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFeedbackScreen(
  onNavigateUp: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  
  val faqItems = listOf(
    FaqItem(
      question = "Getting Started",
      answer = """To get started with SparkReader:

1. Configure a local LLM (go to Settings)
2. Configure SparkReader library (go to Settings)
3. Add books into your library:
   • Import from the SparkReader starter library
   • Take a picture of book pages (OCR) (coming soon)
   • Import EPUB ebooks and webpages (coming soon)
   
4. Once a book is added to your library, tap on it to start reading, getting explanations, and chatting about the book!"""
    ),
  
    FaqItem(
      question = "What makes SparkReader different?",
      answer = """SparkReader combines:

• Contextual AI explanations (not just definitions) for dense, rare, ambiguous, or archaic vocabulary, jargon, and cultural references  
• Ability to ask follow-up questions  
• All this without leaving the app or interrupting your reading  
• Advanced OCR for easy import of books  
• A curated public domain book library  
• All these features work fully offline, with no Internet connection required and no data ever leaving your device"""
    ),
    FaqItem(
      question = "How do I configure or change the AI model?",
      answer = """go to Settings > Model manager. There you can download, delete, and set the default model."""
    ),
    FaqItem(
      question = "How does the contextual explanation work?",
      answer = """Unlike a traditional dictionary, SparkReader provides contextual explanations:

• By selecting any piece of text while reading, two context menu buttons appear automatically
• Explain: explains the selection while keeping the explanation relevant to the context of the page in question
• Quote: copies the selection into the chat, so that you can ask questions about the selection.
You can also ask follow-up questions in a conventional chat interface."""
    ),
    FaqItem(
      question = "What books are included in SparkReader Library?",
      answer = """We have endeavored to include seminal public domain books spanning the entirety of human history—from ancient times to the present—and across diverse world regions.
Our selection covers a wide range of disciplines, including philosophy, religion, science, and law, as well as literary fiction genres such as crime, thriller, and mystery, and non-fiction categories such as travel writing and biographies.
      """
    ),
    FaqItem(
      question = "Is SparkReader open source?",
      answer = """Yes! Both the app and the library are open source. Contributions are welcome—whether as a one-time pull request or by joining as a regular collaborator."""
    ),
    FaqItem(
      question = "Where can I discuss books or report issues?",
      answer = """We have different channels for different types of discussions:

• Book discussions (additions, removals, etc.) - Discord only
• Bug reports - GitHub or Discord
• Feature requests - GitHub or Discord
"""
    )
  )

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Help & Feedback") },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(
              imageVector = Icons.Default.ArrowBack,
              contentDescription = "Navigate back"
            )
          }
        }
      )
    },
    modifier = modifier
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(start = 16.dp, top = 2.dp, end = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
    ) {
      
      // FAQ Section
      item {
        Text(
          text = "Frequently Asked Questions",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(vertical = 8.dp)
        )
      }
      
      items(faqItems) { faq ->
        ExpandableFaqCard(
          faq = faq,
          onNavigateToSettings = onNavigateToSettings
        )
      }
      
      item {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
          text = "Connect",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(vertical = 8.dp)
        )
      }
      
      // Report a Bug - GitHub
      item {
        ActionCard(
          icon = Icons.Default.BugReport,
          title = "Report a Bug or Request a Feature on GitHub",
          subtitle = "Submit issues and contribute to the project",
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sparkreaderapp/sparkreader/issues"))
            context.startActivity(intent)
          }
        )
      }
      
      // Report a Bug - Discord
      item {
        ActionCard(
          icon = Icons.Default.Chat,
          title = "Join our Discord",
          subtitle = "Get help and discuss with the community",
          onClick = {
            // TODO: Replace with actual Discord invite link
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/sparkreader"))
            context.startActivity(intent)
          }
        )
      }
      
      // // Rate the App
      // item {
      //   ActionCard(
      //     icon = Icons.Default.Star,
      //     title = "Rate the App",
      //     subtitle = "Love SparkReader? Let us know!",
      //     onClick = {
      //       // TODO: Replace with actual Play Store link
      //       val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=app.sparkreader"))
      //       try {
      //         context.startActivity(intent)
      //       } catch (e: Exception) {
      //         // If Play Store app is not installed, open in browser
      //         val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=app.sparkreader"))
      //         context.startActivity(webIntent)
      //       }
      //     }
      //   )
      // }

      // Share the App
      item {
        ActionCard(
          icon = Icons.Default.Share,
          title = "Share SparkReader",
          subtitle = "Tell your friends about SparkReader",
          onClick = {
            val shareIntent = Intent().apply {
              action = Intent.ACTION_SEND
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, "Check out SparkReader - An open source app for reading with AI-powered contextual explanations! https://play.google.com/store/apps/details?id=app.sparkreader")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share SparkReader via"))
          }
        )
      }
      
      item {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun ExpandableFaqCard(
  faq: FaqItem,
  onNavigateToSettings: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  val context = LocalContext.current
  
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { expanded = !expanded },
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = faq.question,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
        Icon(
          imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = if (expanded) "Collapse" else "Expand",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      
      if (expanded) {
        Spacer(modifier = Modifier.height(12.dp))
        
        when {
          faq.question == "Getting Started" -> {
            val annotatedText = buildAnnotatedString {
              append("To get started with SparkReader:\n\n")
              append("1. Configure a local LLM (go to ")
              
              pushStringAnnotation(tag = "SETTINGS", annotation = "settings")
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append("Settings")
              }
              pop()
              
              append(")\n")
              append("2. Configure SparkReader library (optional, go to ")
              
              pushStringAnnotation(tag = "SETTINGS", annotation = "settings")
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append("Settings")
              }
              pop()
              
              append(")\n")
              append("3. Add books into your library:\n")
              append("   • Import from the SparkReader starter library\n")
              append("   • Take a picture of book pages (OCR)\n")
              append("   • EPUB and webpage import ")
              
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)) {
                append("(coming soon)")
              }
              
              append("\n\n4. Once a book is added to your library, tap on it to start reading!")
            }
            
            ClickableText(
              text = annotatedText,
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              onClick = { offset ->
                val annotations = annotatedText.getStringAnnotations(start = offset, end = offset)
                if (annotations.isNotEmpty()) {
                  val annotation = annotations.first()
                  when (annotation.tag) {
                    "SETTINGS" -> {
                      onNavigateToSettings?.invoke()
                    }
                  }
                }
              }
            )
          }
          
          faq.question == "Is SparkReader open source?" -> {
            val annotatedText = buildAnnotatedString {
              append("Yes! Both the app and the library are open source. You can:\n\n")
              append("• Submit pull requests (contributions welcome!)\n")
              append("• Access the ")
              
              pushStringAnnotation(tag = "URL", annotation = "https://github.com/sparkreader/sparkreader")
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append("source code")
              }
              pop()
              
              append(" and contribute to the project\n")
              append("• Join our community of contributors")
            }
            
            ClickableText(
              text = annotatedText,
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              onClick = { offset ->
                val annotations = annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                if (annotations.isNotEmpty()) {
                  val annotation = annotations.first()
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                  context.startActivity(intent)
                }
              }
            )
          }
          
          faq.question == "Where can I discuss books or report issues?" -> {
            val annotatedText = buildAnnotatedString {
              append("We have different channels for different types of discussions:\n\n")
              append("• Book discussions (additions, removals, etc.) - ")
              
              pushStringAnnotation(tag = "URL", annotation = "https://discord.gg/sparkreader")
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append("Discord")
              }
              pop()
              
              append(" only\n")
              append("• Bug reports - ")
              
              pushStringAnnotation(tag = "URL", annotation = "https://github.com/sparkreader/sparkreader/issues")
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append("GitHub")
              }
              pop()
              
              append(" or ")
              
              pushStringAnnotation(tag = "URL", annotation = "https://discord.gg/sparkreader")
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append("Discord")
              }
              pop()
              
              append("\n• Feature requests - ")
              
              pushStringAnnotation(tag = "URL", annotation = "https://github.com/sparkreader/sparkreader/issues/new")
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append("GitHub")
              }
              pop()
              
              append(" or ")
              
              pushStringAnnotation(tag = "URL", annotation = "https://discord.gg/sparkreader")
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append("Discord")
              }
              pop()
              
              append("\n\nBook-related discussions are solely performed on Discord to keep our community engaged and allow for real-time conversations about library content.")
            }
            
            ClickableText(
              text = annotatedText,
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              onClick = { offset ->
                val annotations = annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                if (annotations.isNotEmpty()) {
                  val annotation = annotations.first()
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                  context.startActivity(intent)
                }
              }
            )
          }
          
          faq.question.contains("model", ignoreCase = true) && onNavigateToSettings != null -> {
            val annotatedText = buildAnnotatedString {
              append("The ")
              
              pushStringAnnotation(tag = "URL", annotation = "https://deepmind.google/models/gemma/gemma-3n/")
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append("Gemma 3n model")
              }
              pop()
              
              append(" should have been automatically configured when you first started the app.\n\n")
              append("You can view and change the model at any time through ")
              
              pushStringAnnotation(tag = "SETTINGS", annotation = "settings")
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append("Settings")
              }
              pop()
              
              append(". The model enables:\n")
              append("• OCR for importing images\n")
              append("• Contextual explanations for text\n")
              append("• Advanced language understanding\n\n")
              append("To change models, go to Settings from the menu.")
            }
            
            ClickableText(
              text = annotatedText,
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              onClick = { offset ->
                val annotations = annotatedText.getStringAnnotations(start = offset, end = offset)
                if (annotations.isNotEmpty()) {
                  val annotation = annotations.first()
                  when (annotation.tag) {
                    "URL" -> {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                      context.startActivity(intent)
                    }
                    "SETTINGS" -> {
                      onNavigateToSettings?.invoke()
                    }
                  }
                }
              }
            )
          }
          
          else -> {
            Text(
              text = faq.answer,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ActionCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() },
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(40.dp)
      )
      
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      
      Icon(
        imageVector = Icons.Default.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
