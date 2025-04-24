# Bookmarks Feature Documentation

## Purpose
Allows users to bookmark verses for easy access and reference.

## Implementation Details
- **Location**: `features/bookmark/`
- **Key Components**:
  - Bookmark creation and deletion
  - Bookmark listing
  - Bookmark search

## User Interface Components
- Bookmark list view
- Add bookmark button
- Search bar for bookmarks

## Usage Example
```kotlin
// Initialize ViewModel
private val viewModel: BookmarkViewModel by viewModels()

// Add a bookmark
viewModel.addBookmark(
    verseId = 123
)

// Observe state changes
lifecycleScope.launch {
    viewModel.bookmarkState.collect { state ->
        when (state) {
            is BookmarkState.Added -> {
                // Update UI with new bookmark
            }
            // Handle other states...
        }
    }
}
``` 