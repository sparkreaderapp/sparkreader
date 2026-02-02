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

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlinx.coroutines.runBlocking
import java.io.File

class TextPaginatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val paginator = TextPaginator()

    @Test
    fun testBasicPagination() = runBlocking {
        val sourceFile = tempFolder.newFile("basic.txt")
        sourceFile.writeText("This is a simple test file to paginate into pages of a few words each.")

        val result = paginator.paginate(sourceFile, tempFolder.root, "basic", 5)

        assertTrue(result.success)
        assertEquals(3, result.totalPages)
    }

    @Test
    fun testParagraphPreservation() = runBlocking {
        val sourceFile = tempFolder.newFile("paragraphs.txt")
        sourceFile.writeText("First paragraph line one.\nFirst paragraph line two.\n\nShort line.\n\nSecond paragraph line one. Second paragraph line two.")

        val result = paginator.paginate(sourceFile, tempFolder.root, "paragraphs", 10)

        assertTrue(result.success)
        assertEquals(1, result.totalPages)

        val pageContent = File(result.pagesDir, "page_0.json").readText()
        assertTrue(pageContent.contains("First paragraph line one. First paragraph line two."))
        assertTrue(pageContent.contains("Short line."))
        assertTrue(pageContent.contains("Second paragraph line one. Second paragraph line two."))
    }

    @Test
    fun testGutenbergHeaderPreservation() = runBlocking {
        val sourceFile = tempFolder.newFile("gutenberg.txt")
        sourceFile.writeText("The Project Gutenberg eBook of The Adventures of Sherlock Holmes,\nby Arthur Conan Doyle\n\nThis eBook is for the use of anyone anywhere in the United States and\nmost other parts of the world at no cost and with almost no restrictions whatsoever.")

        val result = paginator.paginate(sourceFile, tempFolder.root, "gutenberg", 20)

        assertTrue(result.success)
        assertEquals(3, result.totalPages)

        val pageContent = File(result.pagesDir, "page_0.json").readText()
        assertTrue(pageContent.contains("The Project Gutenberg eBook of The Adventures of Sherlock Holmes, by Arthur Conan Doyle"))
        assertTrue(pageContent.contains("\\n\\n")) // Ensuring paragraphs are separated properly
    }

    @Test
    fun testLargeFilePagination() = runBlocking {
        val sourceFile = tempFolder.newFile("large.txt")
        val largeContent = "word ".repeat(1000)
        sourceFile.writeText(largeContent)

        val result = paginator.paginate(sourceFile, tempFolder.root, "large", 100)

        assertTrue(result.success)
        assertEquals(10, result.totalPages)
    }

    @Test
    fun testEmptyFilePagination() = runBlocking {
        val sourceFile = tempFolder.newFile("empty.txt")
        sourceFile.writeText("")

        val result = paginator.paginate(sourceFile, tempFolder.root, "empty", 50)

        assertTrue(result.success)
        assertEquals(0, result.totalPages)
    }

    @Test
    fun testSingleShortLineParagraph() = runBlocking {
        val sourceFile = tempFolder.newFile("shortline.txt")
        sourceFile.writeText("Short line.\n\nAnother short line.\n\nYet another short line.")

        val result = paginator.paginate(sourceFile, tempFolder.root, "shortline", 50)

        assertTrue(result.success)
        assertEquals(1, result.totalPages)

        val pageContent = File(result.pagesDir, "page_0.json").readText()
        assertTrue(pageContent.contains("Short line."))
        assertTrue(pageContent.contains("Another short line."))
        assertTrue(pageContent.contains("Yet another short line."))
    }
}