package app.sparkreader.ui.bookdetail

import app.sparkreader.data.EXPLAIN_CONTEXT_WORDS

/**
 * Processes chat commands for the book detail screen.
 * Supports the following commands:
 * - /explain p<page> "word" [#occurrence] - Explain a word with optional occurrence number
 * - /quote p<page> "selected text" [#occurrence] <question> - Ask about a quote with optional occurrence
 * - /page p<page> <question> - Ask about a specific page
 * - /book <question> - Ask about the entire book
 */
class ChatCommandProcessor {
    
    data class ProcessedCommand(
        val type: CommandType,
        val prompt: String,
        val originalMessage: String
    )
    
    enum class CommandType {
        EXPLAIN,
        QUOTE,
        PAGE,
        BOOK,
        REGULAR
    }
    
    /**
     * Parses and validates a page number from a string like "p5" or "p123".
     * @param pageStr The page string (e.g., "p5")
     * @param totalPages Total number of pages for validation (0 means no validation)
     * @return The page number (1-based) if valid
     * @throws IllegalArgumentException if the format is invalid or page is out of range
     */
    private fun parsePageNumber(pageStr: String, totalPages: Int = 0): Int {
        if (!pageStr.startsWith("p")) {
            throw IllegalArgumentException("Invalid page number format. Use: p<number>")
        }
        
        val pageNumStr = pageStr.removePrefix("p")
        
        if (pageNumStr.isEmpty() || !pageNumStr.all { it.isDigit() }) {
            throw IllegalArgumentException("Invalid page number format. Use: p<number>")
        }
        
        val pageNum = pageNumStr.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid page number")
        
        if (pageNum < 1) {
            throw IllegalArgumentException("Page number must be greater than 0")
        }
        
        if (totalPages > 0 && pageNum > totalPages) {
            throw IllegalArgumentException("Page number must be between 1 and $totalPages")
        }
        
        return pageNum
    }
    
    /**
     * Data class to hold occurrence information for a text match.
     */
    data class OccurrenceInfo(
        val totalCount: Int,
        val positions: List<Int> // Character positions where matches start
    )
    
    /**
     * Counts occurrences of a target text within a page text and returns their positions.
     * Performs case-insensitive matching and handles word boundaries properly.
     * 
     * @param targetText The text to search for
     * @param pageText The text to search within
     * @param wholeWordsOnly If true, only matches complete words (default: false)
     * @return OccurrenceInfo containing count and positions of matches
     */
    fun countOccurrences(
        targetText: String, 
        pageText: String, 
        wholeWordsOnly: Boolean = false
    ): OccurrenceInfo {
        if (targetText.isEmpty() || pageText.isEmpty()) {
            return OccurrenceInfo(0, emptyList())
        }
        
        val cleanTargetText = targetText.trim()
        if (cleanTargetText.isEmpty()) {
            return OccurrenceInfo(0, emptyList())
        }
        
        val positions = mutableListOf<Int>()
        var searchIndex = 0
        
        while (searchIndex <= pageText.length - cleanTargetText.length) {
            val foundIndex = pageText.indexOf(cleanTargetText, searchIndex, ignoreCase = true)
            
            if (foundIndex == -1) {
                break
            }
            
            // Check word boundaries if wholeWordsOnly is true
            if (wholeWordsOnly) {
                val isWordStart = foundIndex == 0 || 
                    !pageText[foundIndex - 1].isLetterOrDigit()
                val isWordEnd = foundIndex + cleanTargetText.length >= pageText.length || 
                    !pageText[foundIndex + cleanTargetText.length].isLetterOrDigit()
                
                if (isWordStart && isWordEnd) {
                    positions.add(foundIndex)
                }
            } else {
                positions.add(foundIndex)
            }
            
            searchIndex = foundIndex + 1
        }
        
        return OccurrenceInfo(positions.size, positions)
    }
    
    /**
     * Finds which occurrence of a text selection was chosen based on character boundaries.
     * This is useful when a user selects text and we need to determine which occurrence
     * they selected if the same text appears multiple times on the page.
     * 
     * @param pageText The full text of the page
     * @param selectedText The text that was selected
     * @param selectionStart The character position where the selection starts (inclusive)
     * @param selectionEnd The character position where the selection ends (exclusive)
     * @param wholeWordsOnly If true, only matches complete words (default: false for quotes, true for explains)
     * @return The 1-based occurrence index, or -1 if the selection doesn't match any occurrence
     */
    fun findOccurrenceIndex(
        pageText: String,
        selectedText: String,
        selectionStart: Int,
        selectionEnd: Int,
        wholeWordsOnly: Boolean = false
    ): Int {
        // Validate inputs
        if (pageText.isEmpty() || selectedText.isEmpty()) {
            return -1
        }
        
        if (selectionStart < 0 || selectionEnd <= selectionStart || selectionEnd > pageText.length) {
            return -1
        }
        
        // Extract the actual selected text from the page
        val actualSelectedText = pageText.substring(selectionStart, selectionEnd)
        
        // Get all occurrences of the target text
        val occurrenceInfo = countOccurrences(selectedText, pageText, wholeWordsOnly)
        
        if (occurrenceInfo.totalCount == 0) {
            return -1
        }
        
        // First, try exact match between actual selection and target text
        if (actualSelectedText.equals(selectedText, ignoreCase = true)) {
            // Find which occurrence matches our selection boundaries exactly
            for ((index, position) in occurrenceInfo.positions.withIndex()) {
                val occurrenceEnd = position + selectedText.length
                
                // Check if this occurrence matches our selection boundaries
                if (position == selectionStart && occurrenceEnd == selectionEnd) {
                    return index + 1 // Return 1-based index
                }
            }
        }
        
        // If exact match not found, find the occurrence that overlaps most with the selection
        var bestMatch = -1
        var bestOverlapRatio = 0.0
        
        for ((index, position) in occurrenceInfo.positions.withIndex()) {
            val occurrenceEnd = position + selectedText.length
            
            // Check if there's any overlap between the selection and this occurrence
            val overlapStart = maxOf(selectionStart, position)
            val overlapEnd = minOf(selectionEnd, occurrenceEnd)
            
            if (overlapStart < overlapEnd) {
                // There's an overlap, calculate how much
                val overlapLength = overlapEnd - overlapStart
                val selectionLength = selectionEnd - selectionStart
                val occurrenceLength = selectedText.length
                
                // Calculate overlap ratio based on the smaller of the two texts
                val overlapRatio = overlapLength.toDouble() / minOf(selectionLength, occurrenceLength)
                
                // If this is the best overlap so far and meets minimum threshold
                if (overlapRatio >= 0.8 && overlapRatio > bestOverlapRatio) {
                    bestOverlapRatio = overlapRatio
                    bestMatch = index + 1 // 1-based index
                }
            }
        }
        
        return bestMatch // Returns -1 if no good match found
    }
    
    /**
     * Process a chat message and return the appropriate prompt.
     * @param message The raw message from the user
     * @param bookTitle The title of the book
     * @param bookAuthor The author of the book
     * @param totalPages Total number of pages in the book
     * @param pageContent Optional page content for /page commands
     * @return ProcessedCommand containing the type and generated prompt
     * @throws IllegalArgumentException if the command format is invalid
     */
    fun processMessage(
        message: String,
        bookTitle: String,
        bookAuthor: String,
        totalPages: Int = 0,
        pageContent: String? = null
    ): ProcessedCommand {
        return when {
            message.startsWith("/explain") -> processExplainCommand(message, totalPages)
            message.startsWith("/quote") -> processQuoteCommand(message, bookTitle, bookAuthor, totalPages, pageContent)
            message.startsWith("/page") -> processPageCommand(message, bookTitle, bookAuthor, totalPages, pageContent)
            message.startsWith("/book") -> processBookCommand(message, bookTitle, bookAuthor)
            else -> ProcessedCommand(CommandType.REGULAR, message, message)
        }
    }
    
    private fun processExplainCommand(
        message: String,
        totalPages: Int
    ): ProcessedCommand {
        // Pattern: /explain p<number> "word" [#occurrence] [context:"..."]
        val pattern = Regex("""/explain (p\d+) "(.+?)"(?:\s+#(\d+))?(?:\s+context:"(.+?)")?""")
        val match = pattern.find(message)
            ?: throw IllegalArgumentException("Invalid format. Use: /explain p<number> \"text\" [#occurrence] [context: <context>]")
        
        val pageNum = parsePageNumber(match.groupValues[1], totalPages)
        val word = match.groupValues[2]
        val occurrenceIndex = match.groupValues[3].toIntOrNull() // Optional
        val context = match.groupValues[4] // Optional context
        
        if (word.isBlank()) {
            throw IllegalArgumentException("Word cannot be empty. Use: /explain p<number> \"word\"")
        }
        
        val prompt = if (!context.isNullOrBlank()) {
            if (occurrenceIndex != null) {
                """Given this context from the book: "$context"
                
Please define "$word" (occurrence #$occurrenceIndex in the page) in a few words, considering how it's used in this specific context."""
            } else {
                """Given this context from the book: "$context"
                
Please define "$word" in a few words, considering how it's used in this specific context."""
            }
        } else {
            if (occurrenceIndex != null) {
                """Define "$word" (occurrence #$occurrenceIndex) in a few words."""
            } else {
                """Define "$word" in a few words."""
            }
        }
        
        return ProcessedCommand(CommandType.EXPLAIN, prompt, message)
    }
    
    private fun processQuoteCommand(
        message: String,
        bookTitle: String,
        bookAuthor: String,
        totalPages: Int,
        pageContent: String? = null
    ): ProcessedCommand {
        // Pattern: /quote p<page> "quoted text" [#occurrence] <question>
        val pattern = Regex("""/quote (p\d+) "(.+?)"(?:\s+#(\d+))?\s+(.+)""")
        val match = pattern.find(message)
            ?: throw IllegalArgumentException("Invalid format. Use: /quote p<page> \"text\" [#occurrence] <question>")
        
        val pageNum = parsePageNumber(match.groupValues[1], totalPages)
        val quotedText = match.groupValues[2]
        val occurrenceIndex = match.groupValues[3].toIntOrNull() // Optional
        val question = match.groupValues[4].trim()
        
        if (quotedText.isBlank()) {
            throw IllegalArgumentException("Quote cannot be empty")
        }
        
        if (question.isEmpty()) {
            throw IllegalArgumentException("Please provide a question after the quote")
        }
        
        val prompt = if (!pageContent.isNullOrBlank() && occurrenceIndex != null) {
            // Find the specific occurrence and extract context around it
            val occurrenceInfo = countOccurrences(quotedText, pageContent, wholeWordsOnly = false)
            
            if (occurrenceIndex > 0 && occurrenceIndex <= occurrenceInfo.totalCount) {
                val occurrencePosition = occurrenceInfo.positions[occurrenceIndex - 1] // Convert to 0-based
                val contextStart = occurrencePosition
                val contextEnd = occurrencePosition + quotedText.length
                
                // Extract context around the occurrence
                val context = extractContextInternal(
                    pageText = pageContent,
                    selectionStart = contextStart,
                    selectionEnd = contextEnd,
                    contextWords = EXPLAIN_CONTEXT_WORDS
                )
                
                if (context.isNotEmpty()) {
                    """Answer this question about the following quote from page $pageNum of "$bookTitle" by $bookAuthor.

Here is the context around the quote:
"$context"

The specific quote is:
"$quotedText"

Question: $question"""
                } else {
                    """Answer this question about the following quote from page $pageNum of "$bookTitle" by $bookAuthor:

"$quotedText"

Question: $question"""
                }
            } else {
                """Answer this question about the following quote from page $pageNum of "$bookTitle" by $bookAuthor:

"$quotedText"

Question: $question"""
            }
        } else {
            """Answer this question about the following quote from page $pageNum of "$bookTitle" by $bookAuthor:

"$quotedText"

Question: $question"""
        }
        
        return ProcessedCommand(CommandType.QUOTE, prompt, message)
    }
    
    private fun processPageCommand(
        message: String,
        bookTitle: String,
        bookAuthor: String,
        totalPages: Int,
        pageContent: String? = null
    ): ProcessedCommand {
        // Extract page number and user question
        val parts = message.split(" ", limit = 3)
        val pageNumPart = parts.getOrNull(1)
        
        // Validate that we have a page number part
        if (pageNumPart.isNullOrEmpty()) {
            throw IllegalArgumentException("Invalid page number format. Use: /page p<number> <question>")
        }
        
        val pageNum = parsePageNumber(pageNumPart, totalPages)
        
        val question = if (parts.size > 2) parts[2] else ""
        if (question.isEmpty()) {
            throw IllegalArgumentException("Please provide a question. Use: /page p<number> <question>")
        }
        
        val prompt = if (!pageContent.isNullOrBlank()) {
            """Based on the following content from page $pageNum of "$bookTitle" by $bookAuthor:

"$pageContent"

Answer this question: $question"""
        } else {
            """Answer this question about page $pageNum of "$bookTitle" by $bookAuthor: $question"""
        }
        
        return ProcessedCommand(CommandType.PAGE, prompt, message)
    }
    
    private fun processBookCommand(
        message: String,
        bookTitle: String,
        bookAuthor: String
    ): ProcessedCommand {
        // Check if message is exactly "/book" (with possible trailing spaces)
        if (message.trim() == "/book") {
            throw IllegalArgumentException("Please provide a question. Use: /book <question>")
        }
        
        // Extract user question after /book
        val question = message.removePrefix("/book").trim()
        
        val prompt = """Answer this question about the book "$bookTitle" by $bookAuthor: $question"""
        
        return ProcessedCommand(CommandType.BOOK, prompt, message)
    }
    
    /**
     * Generate a quote command from selection in page text.
     * @param pageText The full text of the page
     * @param selectionStart The character position where the selection starts
     * @param selectionEnd The character position where the selection ends
     * @param currentPage The current page number (0-based)
     * @return The formatted quote command
     */
    fun generateQuoteCommand(
        pageText: String,
        selectionStart: Int,
        selectionEnd: Int,
        currentPage: Int
    ): String {
        if (selectionStart < 0 || selectionEnd <= selectionStart || selectionEnd > pageText.length) {
            throw IllegalArgumentException("Invalid selection boundaries")
        }
        
        val selectedText = pageText.substring(selectionStart, selectionEnd).trim()
        if (selectedText.isEmpty()) {
            throw IllegalArgumentException("Selected text is empty")
        }
        
        // Count occurrences
        val occurrenceInfo = countOccurrences(selectedText, pageText, wholeWordsOnly = false)
        
        return if (occurrenceInfo.totalCount > 1) {
            val occurrenceIndex = findOccurrenceIndex(
                pageText = pageText,
                selectedText = selectedText,
                selectionStart = selectionStart,
                selectionEnd = selectionEnd,
                wholeWordsOnly = false
            )
            
            if (occurrenceIndex > 0) {
                "/quote p${currentPage + 1} \"$selectedText\" #$occurrenceIndex "
            } else {
                "/quote p${currentPage + 1} \"$selectedText\" "
            }
        } else {
            "/quote p${currentPage + 1} \"$selectedText\" "
        }
    }
    
    /**
     * Generate an explain command from selection in page text.
     * @param pageText The full text of the page
     * @param selectionStart The character position where the selection starts
     * @param selectionEnd The character position where the selection ends
     * @param currentPage The current page number (0-based)
     * @return The formatted explain command with context
     */
    fun generateExplainCommand(
        pageText: String,
        selectionStart: Int,
        selectionEnd: Int,
        currentPage: Int
    ): String {
        if (selectionStart < 0 || selectionEnd <= selectionStart || selectionEnd > pageText.length) {
            throw IllegalArgumentException("Invalid selection boundaries")
        }
        
        val selectedText = pageText.substring(selectionStart, selectionEnd).trim()
        if (selectedText.isEmpty()) {
            throw IllegalArgumentException("Selected text is empty")
        }
        
        // Extract context around the selection
        val context = extractContextInternal(pageText, selectionStart, selectionEnd)
        
        // Count occurrences (whole words only for explain)
        val occurrenceInfo = countOccurrences(selectedText, pageText, wholeWordsOnly = true)
        
        val command = if (occurrenceInfo.totalCount > 1) {
            val occurrenceIndex = findOccurrenceIndex(
                pageText = pageText,
                selectedText = selectedText,
                selectionStart = selectionStart,
                selectionEnd = selectionEnd,
                wholeWordsOnly = true
            )
            
            if (occurrenceIndex > 0) {
                "/explain p${currentPage + 1} \"$selectedText\" #$occurrenceIndex"
            } else {
                "/explain p${currentPage + 1} \"$selectedText\""
            }
        } else {
            "/explain p${currentPage + 1} \"$selectedText\""
        }
        
        // Append context if available
        return if (context.isNotEmpty()) {
            "$command context:\"$context\""
        } else {
            command
        }
    }
    
    /**
     * Internal method to extract context around a selection.
     * @param pageText The full text of the page
     * @param selectionStart The character position where the selection starts
     * @param selectionEnd The character position where the selection ends
     * @param contextWords Number of words to include before and after selection
     * @return The context text including the selection
     */
    private fun extractContextInternal(
        pageText: String,
        selectionStart: Int,
        selectionEnd: Int,
        contextWords: Int = EXPLAIN_CONTEXT_WORDS
    ): String {
        if (selectionStart < 0 || selectionEnd <= selectionStart || selectionEnd > pageText.length) {
            return ""
        }
        
        var contextStart = selectionStart
        var contextEnd = selectionEnd
        
        // Find word boundaries for context
        var wordsBeforeCount = 0
        var tempStart = selectionStart
        while (tempStart > 0 && wordsBeforeCount < contextWords) {
            tempStart--
            if (tempStart == 0 || pageText[tempStart].isWhitespace()) {
                wordsBeforeCount++
            }
            if (wordsBeforeCount <= contextWords) {
                contextStart = tempStart
            }
        }
        
        var wordsAfterCount = 0
        var tempEnd = selectionEnd
        while (tempEnd < pageText.length && wordsAfterCount < contextWords) {
            if (pageText[tempEnd].isWhitespace()) {
                wordsAfterCount++
            }
            tempEnd++
            if (wordsAfterCount <= contextWords && tempEnd <= pageText.length) {
                contextEnd = tempEnd
            }
        }
        
        // Clean up context boundaries
        while (contextStart > 0 && !pageText[contextStart].isWhitespace()) {
            contextStart--
        }
        if (contextStart > 0) contextStart++ // Skip the whitespace
        
        while (contextEnd < pageText.length && !pageText[contextEnd - 1].isWhitespace()) {
            contextEnd++
        }
        
        return if (contextEnd <= pageText.length) {
            pageText.substring(contextStart, contextEnd).trim()
        } else {
            pageText.substring(contextStart).trim()
        }
    }
    
}
