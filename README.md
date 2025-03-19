# Quran Nexus

## 📖 Overview
Quran Nexus is a Quranic application that provides users with access to the Quran along with in-depth analysis and interactive quizzes. The app offers insights into Quranic words, statistics, and visual representations of data, enhancing the learning experience.

## 🛠 Tech Stack
- **Language**: Kotlin & Java
- **Architecture**: MVVM
- **UI**: XML
- **Backend**: Laravel PHP (via API calls)
- **Database**: MongoDB (accessed through API)
- **Dependency Injection**: Hilt
- **Navigation**: Jetpack Navigation Component
- **Multimedia**: Media3 ExoPlayer

## 🚀 Features
- Quranic text with analysis tools
- Word frequency and statistical insights
- Graphical representation of data using MPAndroidChart
- Interactive quizzes
- Audio playback with Media3 ExoPlayer
- AWS S3 integration for storage
- Modern UI with smooth navigation

## 📦 Installation
1. Clone the repository:
   ```sh
   git clone https://github.com/yourusername/quran-nexus.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and build the project.
4. Run the app on an emulator or a physical device.

## 🔧 Configuration
- Add API keys in `local.properties`:
  ```properties
  AWS_ACCESS_KEY=your_aws_access_key
  AWS_SECRET_KEY=your_aws_secret_key
  ```
- Ensure the backend Laravel project is running for full functionality.

## 🏗 Project Structure
The project follows a **feature-based structure**, with core functionalities separated into different packages for better maintainability.
```
com.example.qurannexus/
├── core/
│   ├── activities/
│   ├── customViews/
│   ├── enums/
│   ├── exceptions/
│   ├── extensions/
│   ├── interfaces/
│   ├── models/
│   ├── network/
│   ├── utils/
├── features/
│   ├── analysis/
│   ├── auth/
│   ├── bookmark/
│   ├── faq/
│   ├── graphs/
│   ├── home/
│   ├── irab/
│   ├── onboard/
│   ├── prayerTimes/
│   ├── quiz/
│   ├── recitation/
│   ├── settings/
│   ├── statistics/
│   ├── tajweed/
│   ├── words/
├── QuranNexusApplication.kt
```

## 📜 License

