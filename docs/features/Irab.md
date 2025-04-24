# I'rab Feature Documentation

## Purpose
Provides grammatical analysis of Quranic verses.

## Implementation Details
- **Location**: `features/irab/`
- **Key Components**:
  - Grammatical parsing
  - Syntax highlighting
  - Morphological analysis

## User Interface Components
- Verse display with syntax highlighting
- Grammatical analysis panel
- Morphological breakdown

## Usage Example
```kotlin
// Initialize ViewModel
private val viewModel: IrabViewModel by viewModels()

// Load grammatical analysis
viewModel.loadIrabData(
    verseId = 45
)

// Observe state changes
lifecycleScope.launch {
    viewModel.irabState.collect { state ->
        when (state) {
            is IrabState.Analyzed -> {
                // Display grammatical analysis
            }
            // Handle other states...
        }
    }
}
``` 