# Quran Recitation Feature Documentation

## Purpose
Provides Quran recitation functionality with various reciters and audio controls.

## Implementation Details
- **Location**: `features/recitation/`
- **Key Components**:
  - **Fragments**: Handle different views and interactions for recitation
    - `RecitationPageFragment.java`: Manages recitation by page
    - `ByPageRecitationFragment.java`: Manages recitation by page
    - `ByAyatRecitationFragment.java`: Manages recitation by ayat
    - `SurahListFragment.java`: Displays list of surahs
  - **Models**: Represent data structures
    - `SurahModel.java`, `Word.java`, `AyahRecitationModel.kt`, etc.
  - **Audio Handling**: Manages audio playback
    - `AudioPlayerManager.kt`: Handles audio playback logic
    - `AudioPlayerService.kt`: Provides audio playback services
    - UI Components: `AudioPlayerLayoutBehavior.kt`, `DraggableFloatingActionButton.kt`
  - **Adapters**: Bind data to views
    - `VersesPaginationAdapter.java`, `SurahRecitationByAyatAdapter.java`

## User Interface Components
- Reciter selection dropdown
- Audio playback controls
- Verse highlighting during playback
- Repeat and shuffle options

## Usage Example
```kotlin
// Initialize ViewModel
private val viewModel: RecitationViewModel by viewModels()

// Load recitation
viewModel.loadRecitation(
    surahNumber = 1,
    reciter = "Mishary Rashid Alafasy"
)

// Observe state changes
lifecycleScope.launch {
    viewModel.recitationState.collect { state ->
        when (state) {
            is RecitationState.Playing -> {
                // Update UI for playing state
            }
            // Handle other states...
        }
    }
}
``` 