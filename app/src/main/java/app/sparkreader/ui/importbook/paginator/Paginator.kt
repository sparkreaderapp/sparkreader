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

import java.io.File

interface Paginator {
    suspend fun paginate(
        sourceFile: File,
        outputDir: File,
        bookId: String,
        wordsPerPage: Int = 100
    ): PaginationResult
}

data class PaginationResult(
    val success: Boolean,
    val totalPages: Int,
    val pagesDir: File?,
    val error: String? = null
)

data class BookPage(
    val pageNumber: Int,
    val content: String,
    val startOffset: Long,
    val endOffset: Long
)
