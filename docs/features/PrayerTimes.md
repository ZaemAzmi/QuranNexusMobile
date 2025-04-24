# Prayer Times Feature Documentation

## Purpose
Provides accurate prayer times based on user's location and date.

## Implementation Details
- **Location**: `features/prayerTimes/`
- **Key Files**:
  - `PrayerTimesFragment.kt`: Main UI implementation
  - `PrayerTimesViewModel.kt`: Business logic and state management
  - `PrayerTimesRepository.kt`: Data operations
  - `models/PrayerTimesResponse.kt`: Data models

## State Management
```kotlin
sealed class PrayerTimesState {
    object Loading : PrayerTimesState()
    data class Success(val data: PrayerTimesResponse) : PrayerTimesState()
    data class Error(val message: String?) : PrayerTimesState()
}
```

## API Integration
- Endpoint: `/api/prayer-times`
- Method: GET
- Parameters:
  - `date`: Current date
  - `location`: User's location

## User Interface Components
- Prayer times display
- Location selection
- Date selection
- Next prayer countdown
- Prayer time notifications

## Usage Example
```kotlin
// Initialize ViewModel
private val viewModel: PrayerTimesViewModel by viewModels()

// Load prayer times
viewModel.loadPrayerTimes(
    date = "2024-03-22",
    location = "Kuala Lumpur"
)

// Observe state changes
lifecycleScope.launch {
    viewModel.prayerTimes.collect { state ->
        when (state) {
            is PrayerTimesState.Success -> {
                // Update UI with prayer times
            }
            // Handle other states...
        }
    }
}
``` 