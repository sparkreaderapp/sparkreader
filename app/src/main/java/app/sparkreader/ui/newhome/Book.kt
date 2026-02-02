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

data class Book(
  val id: String,
  val title: String,
  val author: String,
  val description: String,
  val date: String? = null,        // Date as string from catalog
  val libraryId: String? = null,   // Store the original library book ID
  val totalPages: Int? = null,     // Total number of pages after pagination
  val wordsPerPage: Int? = null,   // Words per page used for pagination
  val lastReadPage: Int = 0,       // Last page the user was reading
  val source: String? = null,       // Source of the book (e.g., "user", "library")
  val source_link: String? = null,
  val tags: String? = null,
)
