# Habit RPG Task Manager - Android App

A gamified habit tracking Android application built following the provided PRD. Users can create tasks, earn XP, level up, and progress through an RPG-style system.

## Features Implemented (MVP Phase 1)

### ✅ Core Architecture
- **3-layer architecture**: Presentation, Business Logic, Data layers
- **Room Database**: Local SQLite storage for offline functionality
- **Firebase Integration**: Cloud storage and authentication
- **SharedPreferences**: Settings and session management

### ✅ User Management
- User registration with email validation
- Firebase authentication with activation system
- Login/logout functionality
- User profile with avatar, level, XP tracking
- Password change capability

### ✅ Task System
- Task creation with difficulty and importance levels
- XP calculation based on difficulty + importance
- Task quota limits (as per PRD specifications)
- Task completion with XP rewards
- Active task listing

### ✅ Category System
- Default categories: Health, Study, Work, Personal
- Category color coding
- CRUD operations for categories

### ✅ Level & XP System
- Level progression formula: Level 1 = 200 XP, then `previous_xp * 2 + previous_xp / 2`
- XP calculation: Difficulty XP + Importance XP
- Level-up notifications
- Progress tracking with visual indicators

### ✅ UI Components
- Material Design components
- Bottom navigation (Tasks, Profile, Categories)
- Floating Action Button for task creation
- Responsive layouts with loading states
- Color-coded task difficulty/importance

## Project Structure

```
app/src/main/java/com/habitrpg/taskmanager/
├── data/
│   ├── database/
│   │   ├── entities/          # Room entities (User, Task, Category, TaskCompletion)
│   │   ├── dao/              # Data Access Objects
│   │   └── AppDatabase.java  # Room database setup
│   ├── firebase/             # Firebase integration
│   └── preferences/          # SharedPreferences management
├── business/
│   ├── auth/                 # Authentication logic
│   ├── tasks/                # Task management logic
│   └── xp/                   # XP calculation system
└── presentation/
    ├── activities/           # Login, Register, MainActivity
    └── fragments/            # Tasks, Profile, Categories fragments
```

## Setup Instructions

### 1. Firebase Configuration
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Enable Authentication with Email/Password
3. Create a Firestore database
4. Download your `google-services.json` file
5. Replace the placeholder `app/google-services.json` with your actual file

### 2. Database Schema
The app automatically creates the following tables:
- `users` - User profiles and game stats
- `categories` - Task categories with colors
- `tasks` - User tasks with difficulty/importance
- `task_completions` - Completion history for XP tracking

### 3. Default Categories
On first registration, users get these default categories:
- Health (Green: #4CAF50)
- Study (Blue: #2196F3)
- Work (Orange: #FF9800)
- Personal (Purple: #9C27B0)

## XP Values & Game Balance

### Difficulty Levels
- **Very Easy**: 1 XP (max 5/day with Normal importance)
- **Easy**: 3 XP (max 5/day with Important)
- **Hard**: 7 XP (max 2/day with Very Important)
- **Extreme**: 20 XP (max 1/week)

### Importance Levels
- **Normal**: +1 XP
- **Important**: +3 XP
- **Very Important**: +10 XP
- **Special**: +100 XP (max 1/month)

### Level System
- Level 1: 200 XP required
- Level 2: 500 XP total (300 additional)
- Level 3: 1,250 XP total (750 additional)
- Formula: `previous_level_xp * 2.5` for each subsequent level

## Development Status

### ✅ Completed (MVP Phase 1)
- Core architecture and database setup
- User authentication and registration
- Basic task management (create, complete, cancel)
- XP and leveling system
- Category management foundation
- UI frameworks and navigation

### 🚧 Pending Implementation
- Task creation UI dialog/screen
- RecyclerView adapters for tasks and categories
- Category creation/editing dialogs
- Task recurring functionality (Phase 2)
- Advanced UI polish and animations

### 🔮 Future Phases
- **Phase 2**: Recurring tasks, calendar view, statistics
- **Phase 3**: Boss battles, equipment system, shop
- **Phase 4**: Social features (friends, alliances)
- **Phase 5**: Special missions, badges, achievements

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Set up Firebase configuration (see Setup Instructions)
4. Sync Gradle dependencies
5. Run on device or emulator (Min SDK: Android 9.0 / API 28)

## Technical Notes

- **Min SDK**: Android 9.0 (API 28)
- **Target SDK**: Android 14 (API 36)
- **Language**: Java
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite) + Firebase Firestore
- **Authentication**: Firebase Auth
- **UI**: Material Design 3

## Contributing

This is the foundational implementation following the PRD. Key areas for contribution:
1. UI/UX improvements and animations
2. RecyclerView adapters for data display
3. Task creation and editing interfaces
4. Enhanced error handling and validation
5. Unit and integration tests

## License

This project is a demonstration implementation of the provided PRD specifications.
