# SparkReader Development Documentation

## Current Book ID System

### Library Books (Project Gutenberg)
- **Integer ID**: Sequential starting from 1 (same range as user books - potential collision)
- **Library ID**: Original Project Gutenberg string ID stored in `libraryId` field
- **File storage**: Uses `libraryId` if available, otherwise falls back to `id.toString()`
- **Source**: "SparkReader Library (Project Gutenberg)" or similar

### User-Created Books
- **Integer ID**: Sequential starting from 1 (same range as library books - potential collision)
- **Library ID**: `null` (no external library reference)
- **File storage**: Uses `id.toString()`
- **Source**: "User Created" or similar

### Current Issues
- Both book types use the same ID range (1+) which can cause collisions
- File storage can conflict when `libraryId` matches a user book's integer ID
- No collision detection when assigning new IDs

### Files Involved
- `app/src/main/java/app/sparkreader/ui/newhome/Book.kt` - Book data class
- `app/src/main/java/app/sparkreader/ui/importbook/ImportBookViewModel.kt` - Library book import logic
- `app/src/main/java/app/sparkreader/ui/createbook/CreateBookViewModel.kt` - User book creation logic (OCR only currently)
