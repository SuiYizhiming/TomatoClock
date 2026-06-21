# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests (JVM)
./gradlew testDebugUnitTest

# Run a single test class
./gradlew testDebugUnitTest --tests "com.xuweikai.tomatoclock.core.domain.timer.TimerStateMachineTest"

# Run Android instrumentation tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Clean build
./gradlew clean assembleDebug
```

## Architecture

**Package**: `com.xuweikai.tomatoclock`

Single Activity + Navigation Compose + MVVM + Clean Architecture with manual DI (no Hilt despite build config).

### Layer Structure

```
app/src/main/java/com/xuweikai/tomatoclock/
├── core/
│   ├── model/          # Domain models: TimerSession, Task, FocusRecord, AppSettings
│   ├── database/       # Room entities, DAOs, AppDatabase
│   ├── datastore/      # DataStore keys for AppSettings
│   ├── domain/
│   │   ├── repository/ # Repository interfaces (TimerRepository, TaskRepository, etc.)
│   │   ├── timer/      # TimerStateMachine, CycleManager, TimerEngine
│   │   ├── alert/      # AlertStrategy, AlertDelivery
│   │   ├── settings/   # SettingsValidator
│   │   └── event/      # FocusCompletedEvent, TaskDeletedEvent
│   └── common/         # Result wrapper
├── data/               # Repository implementations (Room*, DataStore*, Android*)
├── domain/             # Use cases (ArchiveFocusUseCase, ValidateTaskUseCase)
├── feature/            # Feature modules with ViewModel + UiState + UI
│   ├── timer/          # TimerViewModel, TimerUiState, TimerScreen
│   ├── tasks/          # TaskViewModel, TaskUiState, TasksScreen
│   ├── stats/          # StatsViewModel, StatsUiState, StatsScreen
│   └── settings/       # SettingsViewModel, SettingsUiState, SettingsScreen
├── service/            # ForegroundTimerService (bound service, not started)
├── di/                 # AppContainer interface + DefaultAppContainer
├── app/navigation/     # TomatoClockApp composable (NavHost + bottom bar)
└── ui/theme/           # Material3 theme
```

### Key Design Decisions

1. **Manual DI via AppContainer**: `DefaultAppContainer` in `di/AppContainer.kt` creates all dependencies. ViewModels are created via `ViewModelProvider.Factory` in `TomatoClockApp`, not Hilt.

2. **Timer uses monotonic clock**: `TimerStateMachine` takes a `TimerClock` interface (wraps `elapsedRealtime`) to avoid system clock changes affecting countdown. `MonotonicTimerEngine` drives the actual tick loop.

3. **ForegroundTimerService is bound, not started**: The service runs as a bound service (`onBind`), communicating via `SharedFlow<ForegroundTimerEvent>`. The ViewModel collects ticks and finished events.

4. **Event-based cross-module communication**: Focus completion emits `FocusCompletedEvent` via `FocusCompletedEventSink`. The `TomatoClockApp` composable wires effects: timer completion → task completion prompt + stats refresh.

5. **State machine for timer**: `TimerStateMachine.transition(current, event)` is a pure function returning new `TimerSession?`. States: IDLE → RUNNING → PAUSED → COMPLETED/INVALID. Break states follow after COMPLETED.

6. **CycleManager** decides short vs long break based on `validFocusCount` and `longBreakInterval` setting.

### Data Models (Room)

- `TimerSessionEntity` / `timer_sessions` — active timer state
- `TimerOperationLogEntity` / `timer_operation_logs` — audit log for state changes
- `TaskEntity` / `tasks` — user tasks
- `FocusRecordEntity` / `focus_records` — completed focus sessions (UNIQUE on sessionId)
- `DailyFocusStatsEntity` / `daily_focus_stats` — aggregated daily stats (PK: dateKey YYYY-MM-DD)

### Settings (DataStore)

`AppSettings`: focusDurationMin (1-60), shortBreakDurationMin, longBreakDurationMin, longBreakInterval (2-8), alertSound (classic/soft/pulse), vibrationEnabled. Changes take effect on next session, not current running timer.

## Testing

**Unit tests** (`src/test/`): JUnit 4 + Mockito-Kotlin + kotlinx-coroutines-test. Focus on:
- `TimerStateMachine` state transitions
- `CycleManager` break type decisions
- `ValidateTaskUseCase` title validation
- `ArchiveFocusUseCase` deduplication
- ViewModel state management

**Android tests** (`src/androidTest/`): Room DAO CRUD, database schema verification, `RoomStatisticsRepository`.

Use `FakeTimerClock` in tests to control time progression.

## Version Catalog

All dependency versions are in `gradle/libs.versions.toml`. Key versions: Kotlin 2.1.21, KSP 2.1.21-2.0.2, AGP 8.10.1, Compose BOM 2026.02.00, Room 2.8.4, Gradle 8.11.1, JDK 21.

## Conventions

- LF line endings enforced via `.gitattributes`
- `local.properties` is auto-generated with OS-appropriate SDK path, never committed
- Room schema exports to `$projectDir/schemas` (configured in `app/build.gradle.kts` KSP args)
- Chinese UI labels throughout (计时, 任务, 统计, 设置)
