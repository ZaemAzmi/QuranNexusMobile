# Words Study Feature Documentation

## Purpose
Facilitates the study of Quranic words and their meanings.

## Implementation Details
- **Location**: `features/words/`
- **Key Components**:
  - Word lookup
  - Meaning display
  - Word frequency analysis

## User Interface Components
- Word search bar
- Meaning display panel
- Frequency analysis chart

## Usage Example
```kotlin
// Initialize ViewModel
private val viewModel: WordsViewModel by viewModels()

// Search for a word
viewModel.searchWord(
    word = "Rahman"
)

// Observe state changes
lifecycleScope.launch {
    viewModel.wordsState.collect { state ->
        when (state) {
            is WordsState.Found -> {
                // Display word meaning
            }
            // Handle other states...
        }
    }
}
``` 