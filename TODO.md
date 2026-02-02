# TODO

## Book ID System - Potential Clashes Between Library and User Books

**Issue**: Current system has potential ID collisions between Project Gutenberg library books and user-created books.

**Problems identified**:
1. Integer ID collision - both book types use sequential integer IDs starting from 1
2. File storage directory collision - user books use `id.toString()`, library books use `libraryId`, could overlap
3. Current duplicate detection only checks title/author, not ID conflicts

**Solutions needed**:
- Use separate ID ranges (e.g., library books start at 1,000,000 or use negative numbers)
- Use prefixed directory names (`user_123` vs `library_123`)
- Add ID collision detection when assigning new IDs

**Files affected**:
- `app/src/main/java/app/sparkreader/ui/importbook/ImportBookViewModel.kt`
- `app/src/main/java/app/sparkreader/ui/newhome/Book.kt`
- Book creation logic (when implemented)
