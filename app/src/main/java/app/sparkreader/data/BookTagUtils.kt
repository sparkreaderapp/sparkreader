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

package app.sparkreader.data

import android.content.Context
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.PinnableContainer
import app.sparkreader.ui.theme.customColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Utility functions for handling book tags and their visual representation.
 */
object BookTagUtils {
    
    private const val TAG = "BookTagUtils"
    private const val LIBRARY_DIR_NAME = "sparkreader-library"
    
    // Cache for tags loaded from file
    private var cachedTagsByDimension: Map<String, List<String>>? = null
    private var cachedTagOrder: Map<String, Int>? = null
    
    /**
     * Parses a comma-separated tag string into a list of individual tags.
     * 
     * @param tags The comma-separated tag string
     * @return List of individual tags, trimmed and filtered for non-empty values
     */
    fun parseBookTags(tags: String?): List<String> {
        return tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
    
    /**
     * Gets theme-aware colors for different tag dimensions using theme constants.
     * 
     * @return Map of dimension names to their corresponding colors
     */
    @Composable
    private fun getDimensionColors(on: Boolean, container: Boolean): Map<String, Color> {
        val customColors = MaterialTheme.customColors
        return when {
            on && container -> mapOf(
                "temporal" to customColors.onTemporalTagContainer,
                "regional" to customColors.onRegionalTagContainer,
                "discipline" to customColors.onDisciplineTagContainer,
                "genre" to customColors.onGenreFictionTagContainer
            )
            on && !container -> mapOf(
                "temporal" to customColors.temporalTag,
                "regional" to customColors.regionalTag,
                "discipline" to customColors.disciplineTag,
                "genre" to customColors.genreFictionTag
            )
            !on && container -> mapOf(
                "temporal" to customColors.temporalTagContainer,
                "regional" to customColors.regionalTagContainer,
                "discipline" to customColors.disciplineTagContainer,
                "genre" to customColors.genreFictionTagContainer
            )
            else -> mapOf(
                "temporal" to customColors.onTemporalTag,
                "regional" to customColors.onRegionalTag,
                "discipline" to customColors.onDisciplineTag,
                "genre" to customColors.onGenreFictionTag
            )
        }
    }
    
    /**
     * Gets theme-aware colors specifically for fiction and non-fiction genres using theme constants.
     * 
     * @return Map of genre types to their corresponding colors
     */
    @Composable
    private fun getGenreColors(on: Boolean, container: Boolean): Map<String, Color> {
        val customColors = MaterialTheme.customColors
        return if (on && container) {
            mapOf(
                "fiction" to customColors.onGenreFictionTagContainer,
                "nonfiction" to customColors.onGenreNonFictionTagContainer,
            )
        } else if (on && !container) {
            mapOf(
                "fiction" to customColors.genreFictionTag,
                "nonfiction" to customColors.genreNonFictionTag,
            )
        } else if (!on && container) {
            mapOf(
                "fiction" to customColors.genreFictionTagContainer,
                "nonfiction" to customColors.genreNonFictionTagContainer,
            )
        } else {
            mapOf(
                "fiction" to customColors.onGenreFictionTag,
                "nonfiction" to customColors.onGenreNonFictionTag,
            )
        }
    }
    
    /**
     * Determines the appropriate color for a tag based on its dimension and content.
     * 
     * @param tag The full tag string (e.g., "genre/fiction/mystery")
     * @param auto_or_white_or_dark (-1 for auto, 0 for white, 1 for dark) - unused now that we use theme colors
     * @return The appropriate color for the tag
     */
    @Composable
    fun getTagColorsInternal(tag: String, dimensionColors: Map<String, Color>, genreColors: Map<String, Color>): Color {
        val fallbackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

        val tagParts = tag.split("/")
        val dimension = tagParts.getOrNull(0)?.lowercase()

        return when {
            dimension == "genre" && tagParts.size >= 3 -> {
                val genreType = tagParts[1].lowercase()
                when (genreType) {
                    "fiction" -> genreColors["fiction"]
                    "nonfiction", "non-fiction" -> genreColors["nonfiction"]
                    else -> dimensionColors["genre"]
                }
            }
            dimension != null -> dimensionColors[dimension]
            else -> null
        } ?: fallbackColor
    }

    @Composable
    fun getTagColor(tag: String, on: Boolean, container: Boolean): Color {
        val dimensionColors = getDimensionColors(on, container)
        val genreColors = getGenreColors(on, container)

        return getTagColorsInternal(tag, dimensionColors, genreColors)
    }

    /**
     * Extracts the display value from a tag string.
     * If full is true, removes the first part and joins the rest with a slash.
     * For genre tags, returns the third level (specific genre).
     * For other tags, returns the second level.
     * 
     * @param tag The full tag string (e.g., "genre/fiction/mystery")
     * @param full Whether to return the full tag display value
     * @return The display value for the tag (e.g., "mystery")
     */
    fun getTagDisplayValue(tag: String, full: Boolean = false): String {
        val tagParts = tag.split("/")
        return if (full) {
            tagParts.drop(1).joinToString("/")
        } else {
            val dimension = tagParts.getOrNull(0)?.lowercase()
            when {
                tagParts.size >= 3 && dimension == "genre" -> {
                    // For genre tags, show the third level
                    tagParts[2]
                }
                tagParts.size >= 2 -> {
                    // For other tags, show the second level
                    tagParts[1]
                }
                else -> tag // Fallback to full tag if parsing fails
            }
        }
    }
    
    /**
     * Loads tags from the tags.txt file and organizes them by dimension.
     * Results are cached to avoid repeated file I/O.
     * 
     * @param context The application context
     * @return Map of dimension names to ordered lists of tag values
     */
    suspend fun loadTagsFromFile(context: Context): Map<String, List<String>> = withContext(Dispatchers.IO) {
        // Return cached result if available
        cachedTagsByDimension?.let { return@withContext it }
        
        val tagsByDimension = mutableMapOf<String, MutableList<String>>()
        val genreFictionTags = mutableListOf<String>()
        val genreNonFictionTags = mutableListOf<String>()
        val validDimensions = setOf("temporal", "regional", "discipline")
        val tagOrderMap = mutableMapOf<String, Int>()
        var orderIndex = 0
        
        try {
            val externalFilesDir = context.getExternalFilesDir(null)
            val tagsFile = File(externalFilesDir, "SparkReader_Library/_/$LIBRARY_DIR_NAME/library/tags.txt")
            Log.d(TAG, "Loading tags from: ${tagsFile.absolutePath}")
            
            if (!tagsFile.exists()) {
                Log.w(TAG, "Tags file not found: ${tagsFile.absolutePath}")
                return@withContext emptyMap()
            }
            
            val lines = tagsFile.readLines()
            Log.d(TAG, "Loaded ${lines.size} lines from tags.txt")
            
            lines.forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty()) {
                    val parts = trimmedLine.split("/")
                    
                    // Store order for all tag values
                    if (parts.size >= 2) {
                        val tagValue = if (parts.size >= 3 && parts[0].lowercase() == "genre") {
                            parts[2] // For genre tags, use the third level
                        } else {
                            parts[1] // For other tags, use the second level
                        }
                        tagOrderMap[tagValue] = orderIndex++
                    }
                    
                    // Handle genre tags specially - look for third level
                    if (parts.size >= 3 && parts[0].lowercase() == "genre") {
                        val secondLevel = parts[1].lowercase()
                        val thirdLevel = parts[2]
                        
                        when (secondLevel) {
                            "fiction" -> {
                                if (!genreFictionTags.contains(thirdLevel)) {
                                    genreFictionTags.add(thirdLevel)
                                }
                            }
                            "nonfiction", "non-fiction" -> {
                                if (!genreNonFictionTags.contains(thirdLevel)) {
                                    genreNonFictionTags.add(thirdLevel)
                                }
                            }
                        }
                    } else if (parts.size >= 2) {
                        val dimension = parts[0].lowercase()
                        val value = parts[1]
                        
                        if (dimension in validDimensions) {
                            // Preserve order by using list instead of set
                            val tagList = tagsByDimension.getOrPut(dimension) { mutableListOf() }
                            if (!tagList.contains(value)) {
                                tagList.add(value)
                            }
                        }
                    }
                }
            }
            
            // Combine fiction and non-fiction tags for genre dimension
            val genreTags = mutableListOf<String>()
            genreTags.addAll(genreFictionTags)
            genreTags.addAll(genreNonFictionTags)
            
            if (genreTags.isNotEmpty()) {
                tagsByDimension["genre"] = genreTags
            }
            
            // Store fiction/non-fiction tags separately for UI rendering
            tagsByDimension["genre_fiction"] = genreFictionTags
            tagsByDimension["genre_nonfiction"] = genreNonFictionTags
            
            Log.d(TAG, "Loaded tags by dimension: ${tagsByDimension.mapValues { it.value.size }}")
            Log.d(TAG, "Fiction genres: ${genreFictionTags.size}, Non-fiction genres: ${genreNonFictionTags.size}")
            
            val result = tagsByDimension.mapValues { it.value.toList() }
            
            // Cache the results
            cachedTagsByDimension = result
            cachedTagOrder = tagOrderMap
            
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tags from file", e)
            return@withContext emptyMap()
        }
    }
    
    /**
     * Orders a list of tag strings according to the order they appear in tags.txt.
     * Tags not found in the file will be placed at the end in their original order.
     * 
     * @param tags List of tag strings to order
     * @return List of tags ordered according to tags.txt
     */
    fun orderTagsByFileOrder(tags: List<String>): List<String> {
        val tagOrder = cachedTagOrder ?: return tags
        
        return tags.sortedWith { tag1, tag2 ->
            val value1 = getTagDisplayValue(tag1)
            val value2 = getTagDisplayValue(tag2)
            
            val order1 = tagOrder[value1] ?: Int.MAX_VALUE
            val order2 = tagOrder[value2] ?: Int.MAX_VALUE
            
            order1.compareTo(order2)
        }
    }
    
    /**
     * Clears the cached tags data. Useful for testing or when the library is updated.
     */
    fun clearCache() {
        cachedTagsByDimension = null
        cachedTagOrder = null
    }
}