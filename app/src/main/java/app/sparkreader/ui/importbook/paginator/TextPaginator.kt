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

package app.sparkreader.ui.importbook.paginator

//import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets

class TextPaginator : Paginator {

    private val gson = Gson()
    private val TAG = "TextPaginator"

    override suspend fun paginate(
        sourceFile: File,
        outputDir: File,
        bookId: String,
        wordsPerPage: Int
    ): PaginationResult = withContext(Dispatchers.IO) {
        try {
            //Log.d(TAG, "Starting pagination for book $bookId, file size: ${sourceFile.length()} bytes")

            val bookDir = File(outputDir, bookId).apply { mkdirs() }

            val pageCount = paginateFileInChunks(sourceFile, bookDir, wordsPerPage)

            //Log.d(TAG, "Successfully paginated $bookId: $pageCount pages")

            PaginationResult(
                success = true,
                totalPages = pageCount,
                pagesDir = bookDir
            )
        } catch (e: Exception) {
            //Log.e(TAG, "Failed to paginate $bookId", e)
            PaginationResult(
                success = false,
                totalPages = 0,
                pagesDir = null,
                error = e.message
            )
        }
    }

    private fun paginateFileInChunks(
        sourceFile: File,
        bookDir: File,
        wordsPerPage: Int
    ): Int {
        var pageNumber = 0
        var currentPageWords = 0
        var currentPageContent = StringBuilder()
        var currentPageStartOffset = 0L
        var currentOffset = 0L

        BufferedReader(FileReader(sourceFile, StandardCharsets.UTF_8), 8192).use { reader ->
            val paragraphLines = mutableListOf<String>()
            var line: String?

            fun processParagraph(paragraph: List<String>): String {
                if (paragraph.size == 1 && paragraph[0].length < 60) {
                    return paragraph[0].trimEnd()
                }
                return paragraph.joinToString(" ") { it.trim() }.trimEnd()
            }

            while (true) {
                line = reader.readLine()

                if (line == null || line.isBlank()) {
                    if (paragraphLines.isNotEmpty()) {
                        val paragraphText = processParagraph(paragraphLines)
                        val words = paragraphText.split(Regex("\\s+")).filter { it.isNotEmpty() }

                        var wordIndex = 0
                        while (wordIndex < words.size) {
                            if (currentPageWords == 0) {
                                currentPageStartOffset = currentOffset
                            }

                            if (currentPageWords >= wordsPerPage) {
                                val lastLine = currentPageContent.toString().substringAfterLast("\n\n", currentPageContent.toString())
                                if (lastLine.length >= 40 || currentPageContent.isEmpty()) {
                                    savePage(bookDir, pageNumber, currentPageContent.toString().trim(), currentPageStartOffset, currentOffset)
                                    pageNumber++
                                    currentPageContent = StringBuilder()
                                    currentPageWords = 0
                                    currentPageStartOffset = currentOffset
                                }
                            }

                            if (currentPageWords > 0 && !currentPageContent.endsWith("\n\n")) {
                                currentPageContent.append(" ")
                                currentOffset++
                            }

                            currentPageContent.append(words[wordIndex])
                            currentOffset += words[wordIndex].length
                            currentPageWords++
                            wordIndex++
                        }

                        currentPageContent.append("\n\n")
                        currentOffset += 2

                        paragraphLines.clear()
                    }

                    if (line == null) break
                } else {
                    paragraphLines.add(line)
                }
            }

            if (currentPageWords > 0) {
                savePage(bookDir, pageNumber, currentPageContent.toString().trim(), currentPageStartOffset, currentOffset)
                pageNumber++
            }
        }

        return pageNumber
    }

    private fun savePage(bookDir: File, pageNumber: Int, content: String, startOffset: Long, endOffset: Long) {
        val pageFile = File(bookDir, "page_${pageNumber}.json")
        val pageData = BookPage(pageNumber, content, startOffset, endOffset)
        pageFile.writeText(gson.toJson(pageData))

        //Log.d(TAG, "Saved page $pageNumber (${content.length} chars, ~${content.split(Regex("\\s+")).size} words)")
    }
}