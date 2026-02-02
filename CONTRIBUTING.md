## Project Goals for Contributions

We currently have **four high-priority contribution areas (in order)**:

1. **UI Testing**
   - Add automated UI tests using [Jetpack Compose Testing](https://developer.android.com/develop/ui/compose/testing).
   - Contributions are welcome for any major workflow: home screen navigation, reading a book, using the contextual explanation feature, creating a book from text or image, importing a book from the library, and adjusting settings.
   - Tests should be runnable via Gradle.

2. **Text Pagination Support**
   - Currently, only text files from Gutenberg are supported. Pagination is applied when adding a book to the library, using simple heuristics implemented in [TextPaginator](https://github.com/sparkreaderapp/sparkreader/blob/main/app/src/main/java/app/sparkreader/ui/importbook/paginator/TextPaginator.kt).
   - Improvements could include:
     - A smarter pagination algorithm to ensure pages break at more natural points.
     - Automatic chapter detection, so that a chapter does not beging in the middle of a page.
   - We originally had test cases for the `TextPaginator`, but they were later removed. Reintroducing and expanding these tests would be valuable.

3. **Table of Contents (TOC) Support**
   - Automatically extract TOCs from **Project Gutenberg EPUB files** (e.g., [Pride and Prejudice](https://www.gutenberg.org/ebooks/1342)).
   - Ideally, provide a script (in Python or similar) that:
     1. Receives the path to an EPUB file and an approximate words-per-page value (to handle different screen sizes, at minimum, tablet vs. phone).
     2. Produces two outputs: a `toc.json` file and a folder of paginated files. We plan to paginate books for different screen-sizes ourselves, and the right version of the library is downloaded for each user. 
   - The code should include a test suite to verify accuracy and robustness.

4. **Image Support**
   - The book reader screen ([BookDetailScreen](https://github.com/sparkreaderapp/sparkreader/blob/main/app/src/main/java/app/sparkreader/ui/bookdetail/BookDetailScreen.kt), a relic from the app's early days) currently uses [AndroidView](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose) to display book texts.
   - Adding image rendering support would greatly improve the reading experience and will make it possible for users to read books with illustrations.
   
---

## 🛠 How to Contribute

1. **Fork** the repository.  
2. **Clone** your fork locally.  
3. Create a **feature branch** for your work.  
4. Make your changes following Android best practices.  
5. Ensure all relevant tests pass (`./gradlew test` and, if applicable, `./gradlew connectedAndroidTest`).  
6. Submit a **pull request** with a clear description of your changes.
