# Quiz Feature Documentation

## Purpose
Provides an interactive quiz to test users' knowledge of the Quran.

## Implementation Details
- **Location**: `features/quiz/`
- **Key Components**:
  - Question generation
  - Answer validation
  - Score tracking

## User Interface Components
- Question display
- Answer input field
- Scoreboard

## Usage Example
```kotlin
// Initialize ViewModel
private val viewModel: QuizViewModel by viewModels()

// Start a new quiz
viewModel.startQuiz()

// Observe state changes
lifecycleScope.launch {
    viewModel.quizState.collect { state ->
        when (state) {
            is QuizState.Question -> {
                // Display question
            }
            // Handle other states...
        }
    }
}
``` 