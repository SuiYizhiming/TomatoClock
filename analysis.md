# TomatoClock 仓库分析报告

> 生成日期：2026-06-02

---

## 1. 项目概述

**TomatoClock** 是一款本地化的 Android 番茄钟计时器应用，帮助用户通过番茄工作法管理专注时间。项目名称取自 "Tomato"（番茄，Pomodoro 在意大利语中意为番茄）+ "Clock"（时钟）。包名空间为 `com.xuweikai.tomatoclock`。

**目标用户**：学生及自习用户  
**版本**：V1.0  
**语言**：Kotlin 100%  
**最低 SDK**：24（Android 7.0）  
**目标 SDK**：36（Android 14）

---

## 2. 技术栈

| 类别 | 技术选型 |
|------|----------|
| 语言 | Kotlin 2.0.21 |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构模式 | 单 Activity + MVVM + 单向数据流 |
| 导航 | Navigation Compose 2.8.5（底部导航栏 4 标签） |
| 数据库 | Room 2.6.1（5 张表） |
| 偏好存储 | Jetpack DataStore Preferences 1.1.1 |
| 依赖注入 | 手动 DI（AppContainer 接口） |
| 异步 | Kotlin Coroutines + StateFlow / SharedFlow |
| 后台计时 | Foreground Service + Monotonic Clock |
| 构建 | Gradle 8.10.2 + Android Gradle Plugin 8.8.0 |
| 持续集成 | GitHub Actions（assembleDebug + testDebugUnitTest） |

---

## 3. 目录结构

```
TomatoClock/
├── build.gradle.kts                          # 根项目 Gradle 配置
├── settings.gradle.kts                       # 项目设置
├── gradle.properties                         # Gradle 属性
├── gradlew / gradlew.bat                     # Gradle Wrapper
├── local.properties                          # 本地 SDK 路径 (macOS)
├── CODING_CONTEXT.md                         # 工程规范文档
├── AGENT_WORK_SPLIT.md                       # 多 Agent 开发分工计划
└── app/
    ├── build.gradle.kts                      # App 模块 Gradle 配置
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/xuweikai/tomatoclock/
        │   │   ├── MainActivity.kt           # 唯一 Activity
        │   │   ├── app/
        │   │   │   ├── navigation/TomatoClockApp.kt  # 导航宿主
        │   │   │   └── di/AppContainer.kt            # 手动 DI 容器
        │   │   ├── service/ForegroundTimerService.kt # 前台服务
        │   │   ├── ui/theme/                        # Material 3 主题
        │   │   ├── core/
        │   │   │   ├── common/Result.kt              # AppResult<T>
        │   │   │   ├── database/                     # Room 数据库
        │   │   │   │   ├── AppDatabase.kt
        │   │   │   │   ├── dao/ (TimerDao, TaskDao, StatisticsDao)
        │   │   │   │   ├── entity/ (5 张表实体)
        │   │   │   │   └── converter/TimerConverters.kt
        │   │   │   ├── datastore/AppSettingsKeys.kt  # DataStore 键
        │   │   │   ├── domain/
        │   │   │   │   ├── alert/                    # 提醒策略
        │   │   │   │   ├── event/                    # 领域事件
        │   │   │   │   ├── repository/               # 仓库接口
        │   │   │   │   ├── settings/                 # 设置校验
        │   │   │   │   └── timer/                    # 计时器核心
        │   │   │   └── model/                        # 领域模型
        │   │   ├── data/
        │   │   │   ├── alert/AndroidAlertManager.kt
        │   │   │   ├── settings/DataStoreSettingsRepository.kt
        │   │   │   ├── stats/RoomStatisticsRepository.kt
        │   │   │   ├── task/RoomTaskRepository.kt
        │   │   │   └── timer/RoomTimerRepository.kt
        │   │   ├── domain/
        │   │   │   ├── stats/ArchiveFocusUseCase.kt
        │   │   │   ├── stats/DateKeys.kt
        │   │   │   └── task/ (ValidateTaskUseCase, TaskTitleValidation)
        │   │   └── feature/
        │   │       ├── timer/   (ViewModel + 界面)
        │   │       ├── tasks/   (ViewModel + 界面)
        │   │       ├── stats/   (ViewModel + 界面)
        │   │       └── settings/(ViewModel + 界面)
        │   └── res/values/ (strings.xml, styles.xml)
        ├── test/                                    # 单元测试
        │   └── java/com/xuweikai/tomatoclock/
        │       ├── core/domain/timer/
        │       │   ├── TimerStateMachineTest.kt
        │       │   ├── CycleManagerTest.kt
        │       │   └── FakeTimerClock.kt
        │       ├── core/domain/settings/SettingsValidatorTest.kt
        │       ├── core/domain/alert/BasicAlertStrategyTest.kt
        │       ├── core/model/AppSettingsDefaultsTest.kt
        │       ├── data/settings/DataStoreSettingsRepositoryTest.kt
        │       ├── domain/stats/ (DateKeysTest, ArchiveFocusUseCaseTest)
        │       ├── domain/task/ValidateTaskUseCaseTest.kt
        │       └── feature/tasks/TaskViewModelTest.kt
        └── androidTest/                             # 插桩测试
            └── (room-testing 依赖)
```

---

## 4. 分层架构

采用**清洁架构（Clean Architecture）** + **MVVM** 模式，分为四层：

```
┌─────────────────────────────────────────────────┐
│          Presentation (Compose UI)               │
│  TimerScreen / TasksScreen / StatsScreen /       │
│  SettingsScreen                                  │
├─────────────────────────────────────────────────┤
│             ViewModel Layer                      │
│  管理 UiState (StateFlow) + 一次性事件 (SharedFlow)│
├─────────────────────────────────────────────────┤
│          Domain Use Cases (业务逻辑)              │
│  ValidateTaskUseCase / ArchiveFocusUseCase       │
├─────────────────────────────────────────────────┤
│         Repository 接口 (契约)                    │
│  TimerRepository / TaskRepository /              │
│  StatisticsRepository / SettingsRepository       │
├──────────────┬──────────────────┬────────────────┤
│ Room DAOs    │ DataStore Prefs  │ Android系统API │
│ (SQLite)     │ (键值对)         │ (通知/震动/音调)│
└──────────────┴──────────────────┴────────────────┘
```

### 4.1 Core 层 — 领域模型与核心逻辑

- **领域模型**：`TimerSession`、`Task`、`FocusRecord`、`DailyFocusStats`、`AppSettings`
- **计时器状态机** (`TimerStateMachine`)：纯函数实现，定义 Start / Pause / Resume / Complete / Reset / Tick / Restore 七种事件及状态转换规则
- **周期管理器** (`CycleManager`)：根据 `validFocusCount` 和设置决定下一个周期步骤（短休息/长休息/准备下一个专注/回到空闲）
- **计时引擎** (`TimerEngine`)：`MonotonicTimerEngine` 基于 `SystemClock.elapsedRealtime()`（单调时钟，不受系统时间修改影响），使用协程驱动每秒 Tick
- **计时会话恢复器** (`TimerSessionRestorer`)：从已过单调时间计算剩余秒数，支持跨进程/跨重启恢复运行中的会话
- **设置校验器** (`SettingsValidator`)：校验时长范围（1-60 分钟）、长休息间隔（2-8）、提示音类型
- **提醒策略** (`BasicAlertStrategy`)：根据静音模式和震动设置选择提醒方式（声音 / 震动 / 弹窗）

### 4.2 Data 层 — 数据持久化

- **Room 数据库**（5 张表）：
  | 表名 | 用途 |
  |------|------|
  | `timer_sessions` | 计时会话记录 |
  | `timer_operation_logs` | 计时操作审计日志 |
  | `tasks` | 任务管理 |
  | `focus_records` | 已完成的专注记录归档 |
  | `daily_focus_stats` | 每日汇总统计 |

- **DataStore Preferences**：存储用户设置（专注时长、休息时长、提示音、震动开关等）
- **AndroidAlertManager**：实现系统通知、音调生成、震动、Toast 回退

### 4.3 Feature 层 — 四个功能模块

| 模块 | ViewModel | UiState 关键字段 | 功能 |
|------|-----------|-------------------|------|
| **Timer** | `TimerViewModel` | session, mode, status, remainingSec, plannedDurationSec | 启动/暂停/重置专注，循环切换专注/休息，完成后触发归档和提示 |
| **Tasks** | `TaskViewModel` | activeTasks, completedTasks, expandedTaskId | 任务增删改查，标记完成，选择关联专注，专注完成后提示任务完成 |
| **Stats** | `StatsViewModel` | todaySummary, weeklyTrend, monthDays (CheckInLevel) | 今日概览、7 日趋势柱状图、月度打卡日历 |
| **Settings** | `SettingsViewModel` | settings, draftSettings, validationErrors | 专注/休息时长调节、提示音选择、震动开关（草稿/提交模式） |

### 4.4 App 层 — 组装与导航

- `MainActivity.kt`：唯一 Activity，创建 DI 容器，请求通知权限（Android 13+）
- `TomatoClockApp.kt`：`NavHost` 管理底部四个标签页 + 跨模块事件处理
- `AppContainer`：手动 DI，组装 Database → Repository → ViewModel 依赖链，通过 `Activity.RetainFragment` 保持跨配置变更

---

## 5. 核心功能及业务流程

### 5.1 番茄钟计时流程

1. 用户点击"开始专注" → `TimerViewModel.startFocus()`
2. 状态机接收 `Start` 事件 → 创建 `TimerSession` → 写入 Room
3. `MonotonicTimerEngine` 启动协程循环，每秒发送 `Tick` 到状态机
4. 到达 `remainingSec == 0` → 状态机 `Complete` → 触发 `FocusCompletedEvent`
5. `ArchiveFocusUseCase` 过滤仅 FOCUS 模式 → 写入 `focus_records` + 更新 `daily_focus_stats`
6. `CycleManager` 决策下一步（短休息 / 长休息 / 空闲）
7. `AndroidAlertManager` 发送通知 + 播放提示音 + 震动
8. 若有关联任务，弹出任务完成确认对话框

### 5.2 任务管理

- 支持添加、编辑、删除、完成任务
- 任务标题校验：非空、1-100 字符、不含控制字符
- 每个任务可选关联到专注会话，专注完成后可标记任务完成

### 5.3 统计系统

- 仅统计 FOCUS 模式的完整专注（`ArchiveFocusUseCase` 做模式过滤）
- 今日概览：番茄数 + 总专注秒数
- 7 日趋势：每日番茄数柱状图
- 月度日历：按打卡水平颜色编码（NONE < LOW < MEDIUM < HIGH）

### 5.4 设置

- 专注时长（默认 25 分钟，范围 1-60）
- 短休息时长（默认 5 分钟）
- 长休息时长（默认 15 分钟）
- 长休息间隔（默认 4 个专注后）
- 提示音类型（classic / soft / pulse）
- 震动开关

---

## 6. 数据流与状态管理

```
用户操作 → ViewModel.onEvent() → Domain UseCase / State Machine
    → Repository → Room / DataStore
    → ViewModel 更新 StateFlow<UiState>
    → Compose 重组 UI
```

- **UiState**：每个 Feature 暴露不可变的 `StateFlow<UiState>`，UI 层唯一数据来源
- **一次性事件**：通过 `SharedFlow` 发出（如导航、Toast 提示）
- **跨模块通信**：`FocusCompletedEventSink` 函数接口 + `TaskDeletedEvent` 事件对象

---

## 7. 计时器可靠性设计

| 机制 | 说明 |
|------|------|
| 单调时钟 | 使用 `SystemClock.elapsedRealtime()`，不受系统时间修改影响 |
| 前台服务 | `ForegroundTimerService` 确保进程不被回收 |
| 会话恢复 | `TimerSessionRestorer` 从单调时间差值计算剩余秒数 |
| 持久化 | 每次操作写入 Room，重启后可恢复 |
| 状态机 | 纯函数实现，每个事件产生新状态，无副作用 |

---

## 8. 测试覆盖

共 **11 个测试文件**（均在 `app/src/test/` 下）：

| 测试文件 | 测试内容 |
|----------|----------|
| `TimerStateMachineTest.kt` | 状态机所有事件转换及边界情况 |
| `CycleManagerTest.kt` | 周期计算逻辑 |
| `SettingsValidatorTest.kt` | 设置校验规则 |
| `BasicAlertStrategyTest.kt` | 提醒策略选择 |
| `AppSettingsDefaultsTest.kt` | 设置默认值 |
| `DataStoreSettingsRepositoryTest.kt` | DataStore 仓库实现 |
| `DateKeysTest.kt` | 日期键值工具 |
| `ArchiveFocusUseCaseTest.kt` | 归档用例（过滤非 FOCUS 模式） |
| `ValidateTaskUseCaseTest.kt` | 任务标题校验 |
| `TaskViewModelTest.kt` | Task ViewModel 逻辑 |
| `FakeTimerClock.kt` | 计时器测试辅助 Fake |

---

## 9. 构建与 CI

### 本地构建命令
```bash
./gradlew assembleDebug          # 构建 Debug APK
./gradlew testDebugUnitTest      # 运行单元测试
./gradlew assembleRelease        # 构建 Release APK（未配置签名）
```

### CI（GitHub Actions）
- 触发条件：`main` 分支的 push / PR
- 步骤：JDK 17 → Gradle Setup → `assembleDebug` → `testDebugUnitTest` → 上传 APK Artifact

---

## 10. 关键依赖清单

```
androidx.compose:compose-bom:2024.12.01
androidx.activity:activity-compose:1.9.3
androidx.compose.material3:material3
androidx.core:core-ktx:1.15.0
androidx.datastore:datastore-preferences:1.1.1
androidx.lifecycle:lifecycle-runtime-compose:2.8.7
androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7
androidx.navigation:navigation-compose:2.8.5
androidx.room:room-ktx:2.6.1
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0
junit:junit:4.13.2 (test)
```

---

## 11. 工程文档

| 文档 | 内容 |
|------|------|
| `CODING_CONTEXT.md` | 详细工程规范：架构约束、数据模型定义、计时器状态机、任务管理规则、统计逻辑、UI/UX 规范、P0 测试清单 |
| `AGENT_WORK_SPLIT.md` | 多 Agent 并行开发分工计划：7 个 Agent（脚手架、计时器核心、任务、统计、设置、UI、集成），含接口契约和依赖顺序 |

---

## 12. 架构特点与设计决策

1. **无 Hilt/Dagger**：使用手动 DI（AppContainer），保持项目轻量且易于理解
2. **纯函数状态机**：TimerStateMachine 不依赖 Android 框架，易于测试
3. **Monotonic Clock**：基于 elapsedRealtime 而非系统时间，防止用户修改系统时间破坏计时
4. **草稿/提交模式**：Settings 页面先编辑 draft 再统一保存，避免频繁写入 DataStore
5. **事务性归档**：ArchiveFocusUseCase 在 Room 事务中同时插入记录和更新统计，保证数据一致性
6. **周期管理**：CycleManager 独立于状态机，职责分离清晰
