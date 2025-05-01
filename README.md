# QuranNexus

QuranNexus is an Android application designed to provide a comprehensive Quran study and reading experience. This application is built using modern Android development practices and follows a clean architecture approach.

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/qurannexus/
│   │   │       ├── core/           # Core functionality and utilities
│   │   │       ├── features/       # Feature-specific implementations
│   │   │       └── QuranNexusApplication.kt
│   │   ├── res/                    # Android resources
│   │   ├── assets/                 # Application assets
│   │   └── AndroidManifest.xml     # App manifest
│   ├── test/                       # Unit tests
│   └── androidTest/                # Instrumentation tests
├── build.gradle                    # App-level build configuration
└── proguard-rules.pro             # ProGuard rules

```

## Technology Stack

- **Language**: Kotlin & Java
- **Build System**: Gradle
- **Architecture**: Clean Architecture with MVVM pattern
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34 (Android 14)
- **Backend**: Laravel (PHP)
- **Database**: MongoDB (Accessed through API call)

### Key Dependencies

- Hilt for dependency injection
- Navigation Component for navigation
- Media3 for audio playback
- Retrofit and OkHttp for networking
- Glide for image loading
- MPAndroidChart for graphing

## System Architecture

### MVVM Architecture Implementation
The project implements the MVVM (Model-View-ViewModel) architecture pattern. Here's how the components are structured, using the PrayerTimes feature as an example:

#### Components Structure
```
features/
└── feature_name/               # e.g., prayerTimes/
    ├── models/                 # Data models
    ├── di/                    # Dependency injection modules
    ├── ui/                    # UI components and adapters
    ├── FeatureRepository.kt   # Repository implementation
    ├── FeatureViewModel.kt    # ViewModel
    └── FeatureFragment.kt     # Main UI Fragment
```

#### Architecture Components
1. **Repository**
   - Handles data operations and API calls
   - Example from PrayerTimes feature:
     ```kotlin
     class PrayerTimesRepository @Inject constructor(
         private val api: PrayerTimesApi
     ) {
         suspend fun getPrayerTimes(date: String, location: String): PrayerTimesResponse {
             return api.getPrayerTimes(date, location)
         }
     }
     ```

2. **ViewModel**
   - Handles UI logic and state management
   - Uses coroutines for async operations
   - Example from PrayerTimes feature:
     ```kotlin
     class PrayerTimesViewModel @Inject constructor(
         private val repository: PrayerTimesRepository
     ) : ViewModel() {
         private val _prayerTimes = MutableStateFlow<PrayerTimesState>(PrayerTimesState.Loading)
         val prayerTimes: StateFlow<PrayerTimesState> = _prayerTimes.asStateFlow()

         fun loadPrayerTimes(date: String, location: String) {
             viewModelScope.launch {
                 try {
                     val result = repository.getPrayerTimes(date, location)
                     _prayerTimes.value = PrayerTimesState.Success(result)
                 } catch (e: Exception) {
                     _prayerTimes.value = PrayerTimesState.Error(e.message)
                 }
             }
         }
     }
     ```

3. **Fragment/Activity (View)**
   - UI implementation
   - Observes ViewModel state
   - Example from PrayerTimes feature:
     ```kotlin
     class PrayerTimesFragment : Fragment() {
         private val viewModel: PrayerTimesViewModel by viewModels()
         
         override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
             super.onViewCreated(view, savedInstanceState)
             
             lifecycleScope.launch {
                 viewModel.prayerTimes.collect { state ->
                     when (state) {
                         is PrayerTimesState.Success -> {
                             // Update UI with prayer times
                             updatePrayerTimesUI(state.data)
                         }
                         is PrayerTimesState.Error -> {
                             // Show error message
                             showError(state.message)
                         }
                         is PrayerTimesState.Loading -> {
                             // Show loading state
                             showLoading()
                         }
                     }
                 }
             }
         }
     }
     ```

4. **Dependency Injection**
   - Located in the `di` package
   - Provides dependencies using Hilt
   - Example:
     ```kotlin
     @Module
     @InstallIn(ViewModelComponent::class)
     object PrayerTimesModule {
         @Provides
         fun providePrayerTimesRepository(
             api: PrayerTimesApi
         ): PrayerTimesRepository {
             return PrayerTimesRepository(api)
         }
     }
     ```

5. **Models**
   - Data classes representing the feature's data structures
   - Located in the `models` package
   - Example:
     ```kotlin
     data class PrayerTimesResponse(
         val fajr: String,
         val dhuhr: String,
         val asr: String,
         val maghrib: String,
         val isha: String
     )

     sealed class PrayerTimesState {
         object Loading : PrayerTimesState()
         data class Success(val data: PrayerTimesResponse) : PrayerTimesState()
         data class Error(val message: String?) : PrayerTimesState()
     }
     ```

### Backend Integration
The application integrates with a Laravel-based backend service that provides the necessary APIs. The backend:
- Is built with Laravel (PHP)
- Uses MongoDB as the primary database
- Provides RESTful APIs for the Android client
- Handles data processing and business logic

### Database
- MongoDB is used as the primary database
- Stores Quranic content, user data, and application-related information
- Provides efficient querying and data management capabilities

## Features

The application is organized into distinct features, each contained within its own module under the `features` directory. The core functionality is separated into the `core` directory for better maintainability and reusability.

## Setup and Installation

### Android Application
1. Clone the repository
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Run the application on an emulator or physical device

### Backend Setup
1. Clone the Laravel backend repository (https://github.com/Amirul1411/QuranNexus)
2. Install dependencies:
   ```bash
   composer install
   ```
3. Configure MongoDB connection in `.env` file
4. Start the development server:
   ```bash
   php artisan serve
   ```
   This will start the backend server, typically at `http://localhost:8000`

### Testing on physical device
To test the Android app on a real device and connect it to the local Laravel backend:
1. Connect the Android device and development machine to the same Wi-Fi network.
2. Start the Laravel server using:
    ```bash
   php artisan serve --host=0.0.0.0
   ```
3. Find the development machine's local IP address:
   ```bash
    ipconfig
   ```
Look for the IPv4 Address (e.g., 192.168.1.10).
4. Update your Android app's API base URL to use this IP address(in ApiService.kt and AuthService.kt), e.g.:
   ```bash
    LOCAL_API_URL = "http://10.0.2.2:8000/api/v1/mobile/";
   ```
5. Ensure network_security_config.xml allows cleartext traffic to your backend IP:

### API Configuration
- The Android application is configured to communicate with the Laravel backend
- Default API base URL can be configured in the application's build configuration
- Ensure the backend server is running before using the Android application

## Build Configuration

The project uses Gradle as its build system with the following key configurations:

- App-level build configuration in `app/build.gradle`
- Project-level build configuration in `build.gradle`
- Settings and properties in `settings.gradle` and `gradle.properties`

## Development

### Architecture

The project follows Clean Architecture principles with the following layers:
- Presentation Layer (UI)
- Domain Layer (Business Logic)
- Data Layer (Data Sources and API Integration)

### Code Organization

- `core/`: Contains core functionality, utilities, and base classes
- `features/`: Contains feature-specific implementations
- `QuranNexusApplication.kt`: Application class for initialization

### Key Features
- Audio playback support (VPS-hosted audio files)
- Graph visualization
- Modern Material Design UI
- Multi-module architecture
- Dependency injection with Hilt
- RESTful API integration with Laravel backend
- MongoDB data persistence

### API Integration
- Uses Retrofit for API communication
- Implements repository pattern for data management
- Handles API responses with coroutines for asynchronous operations
- Includes error handling and response mapping

## Testing

The project includes both unit tests and instrumentation tests:
- Unit tests are located in the `test` directory
- Instrumentation tests are located in the `androidTest` directory

## Extra note
- In mongodb database, the collection "word_statistics" stores the unique words information, along with their locations and positions. The difference with "words" collection is that "words" collection stores every single words and contains many redundant words, while "word_statistics" only contains unique words.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

## Contact 
