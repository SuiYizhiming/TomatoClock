# 🍅 TomatoClock — 番茄钟计时器

一款基于番茄工作法（Pomodoro Technique）的 Android 计时器应用，帮助用户高效管理专注时间、追踪任务完成情况并查看专注统计。

---

## 📱 应用功能

### 计时页（Timer）
- 圆形进度环倒计时显示（MM:SS）
- 支持开始 / 暂停 / 继续 / 重置操作
- 自动切换专注 → 短休息 → 长休息模式
- 基于单调时钟（`elapsedRealtime`）计时，不受系统时间修改影响
- 前台 Service 保证后台计时不中断

### 任务页（Tasks）
- 添加、编辑、删除任务
- 任务标题校验：非空、1–100 字符、不含控制字符
- 关联任务到专注会话，完成后提示标记任务完成
- 未完成 / 已完成分组展示

### 统计页（Statistics）
- 今日摘要：番茄数、今日专注时长、累计专注时长、与昨日对比
- 本周趋势：7 天柱状图（Canvas 绘制）
- 月度打卡日历：按专注强度四色标注（无 / 低 / 中 / 高）

### 设置页（Settings）
- 外观：深色模式（跟随系统 / 浅色 / 深色）
- 计时时长：专注（1–60 分钟）、短休息、长休息、长休息间隔（2–8 轮）
- 提示音：经典 / 柔和 / 脉冲（可试听）
- 振动开关

---

## 🛠 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.1.21 |
| UI 框架 | Jetpack Compose (Material 3) | BOM 2026.02.00 |
| 架构 | MVVM + Clean Architecture | — |
| 导航 | Navigation Compose | 2.9.7 |
| 数据库 | Room | 2.8.4 |
| 偏好存储 | DataStore Preferences | 1.2.0 |
| 生命周期 | Lifecycle ViewModel + Runtime | 2.10.0 |
| 构建工具 | Gradle (Kotlin DSL) | 8.11.1 |
| AGP | Android Gradle Plugin | 8.10.1 |
| KSP | Kotlin Symbol Processing | 2.1.21-2.0.2 |
| JDK | OpenJDK | 21 |

---

## 📂 项目结构

```
app/src/main/java/com/xuweikai/tomatoclock/
├── MainActivity.kt                    # 单 Activity 入口
├── app/navigation/                    # NavHost + 底部导航栏（4 个 Tab）
├── di/                                # 手动依赖注入容器（AppContainer）
├── core/
│   ├── model/                         # 领域模型：TimerSession, Task, FocusRecord, AppSettings
│   ├── database/                      # Room 数据库：Entity, DAO, Database, TypeConverter
│   ├── datastore/                     # DataStore 键定义
│   └── domain/
│       ├── timer/                     # TimerStateMachine, CycleManager, TimerEngine
│       ├── alert/                     # 提示策略：AlertStrategy, AlertDelivery
│       ├── settings/                  # SettingsValidator
│       ├── repository/                # Repository 接口
│       └── event/                     # FocusCompletedEvent, TaskDeletedEvent
├── data/                              # Repository 实现（Room*, DataStore*, Android*）
├── domain/                            # 用例：ArchiveFocusUseCase, ValidateTaskUseCase
├── feature/
│   ├── timer/                         # 计时 ViewModel + UiState + Screen
│   ├── tasks/                         # 任务 ViewModel + UiState + Screen
│   ├── stats/                         # 统计 ViewModel + UiState + Screen
│   └── settings/                      # 设置 ViewModel + UiState + Screen
├── service/                           # ForegroundTimerService（前台绑定服务）
└── ui/theme/                          # Material 3 主题配置
```

---

## 🚀 安装与部署

### 环境要求

| 项目 | 要求 |
|------|------|
| JDK | OpenJDK 21 |
| Android SDK | compileSdk 36 |
| 最低支持版本 | Android 7.0（API 24） |
| Gradle | 8.11.1（通过 Wrapper 自动下载） |

### 1. 克隆项目

```bash
git clone <仓库地址>
cd TomatoClock
```

### 2. 配置 Android SDK

项目会自动生成 `local.properties` 并根据操作系统写入 SDK 路径：

- **Windows**：`C:\Users\<用户名>\AppData\Local\Android\Sdk`
- **macOS**：`~/Library/Android/sdk`
- **Linux**：`~/Android/Sdk`

如 SDK 路径非默认安装位置，手动编辑 `local.properties`：

```properties
sdk.dir=/your/android/sdk/path
```

### 3. 配置 JDK

确保系统已安装 JDK 21，或在 `gradle.properties` 中指定路径：

```properties
org.gradle.java.home=C:/Appciation/OpenJDK21
```

### 4. 构建项目

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需配置签名）
./gradlew assembleRelease
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

### 5. 安装到设备

```bash
# 通过 ADB 安装
adb install app/build/outputs/apk/debug/app-debug.apk
```

或直接在 Android Studio 中点击 **Run** 按钮部署到模拟器 / 真机。

---

## 🧪 测试

### 单元测试（JVM）

```bash
# 运行全部单元测试
./gradlew testDebugUnitTest

# 运行指定测试类
./gradlew testDebugUnitTest --tests "com.xuweikai.tomatoclock.core.domain.timer.TimerStateMachineTest"
```

**测试覆盖：**
- `TimerStateMachineTest` — 计时器状态机转换
- `CycleManagerTest` — 休息类型决策逻辑
- `SettingsValidatorTest` — 设置参数校验
- `ValidateTaskUseCaseTest` — 任务标题校验
- `ArchiveFocusUseCaseTest` — 专注记录归档去重
- `TimerViewModelTest` / `TaskViewModelTest` — ViewModel 状态管理
- `DataStoreSettingsRepositoryTest` — DataStore 读写

### Android 仪器测试（需设备 / 模拟器）

```bash
./gradlew connectedDebugAndroidTest
```

测试 Room DAO 的 CRUD 操作和数据库 Schema 验证。

---

## 🔄 CI/CD

项目配置了 GitHub Actions 自动化流水线（`.github/workflows/`）：

- **触发条件**：Push 或 PR 到 `main` 分支
- **执行内容**：
  1. 配置 JDK 21 环境
  2. 运行 `assembleDebug` 构建
  3. 运行 `testDebugUnitTest` 单元测试
  4. 上传 Debug APK 为构建产物

---

## 🗄 数据库设计

应用使用 Room 持久化框架，包含 5 张表：

| 表名 | 用途 |
|------|------|
| `timer_sessions` | 活跃计时器状态 |
| `timer_operation_logs` | 计时器操作审计日志 |
| `tasks` | 用户任务列表 |
| `focus_records` | 已完成的专注记录（按 sessionId 去重） |
| `daily_focus_stats` | 每日专注统计汇总（主键：YYYY-MM-DD） |

Room Schema 导出路径：`app/schemas/`

---

## 🏗 架构设计

```
┌─────────────────────────────────────────────┐
│                   UI Layer                  │
│  Compose Screens ← ViewModel ← UiState     │
├─────────────────────────────────────────────┤
│               Domain Layer                 │
│  Use Cases ← Repository Interfaces         │
│  TimerStateMachine, CycleManager           │
├─────────────────────────────────────────────┤
│                Data Layer                   │
│  Room (Database/DAO) ← Repository Impl     │
│  DataStore ← SettingsRepository            │
│  AndroidAlertManager ← AlertManager        │
└─────────────────────────────────────────────┘
```

- **单 Activity** + Navigation Compose 管理 4 个 Tab 页面
- **MVVM**：每个 Feature 模块包含 ViewModel + UiState + Screen
- **Clean Architecture**：core/domain/data 三层分离
- **手动依赖注入**：通过 `AppContainer` 接口管理所有依赖实例
- **事件驱动**：`FocusCompletedEventSink` 实现跨模块通信

---

## 📦 权限声明

| 权限 | 用途 |
|------|------|
| `FOREGROUND_SERVICE` | 后台计时不中断 |
| `POST_NOTIFICATIONS` | 计时完成通知（Android 13+） |
| `VIBRATE` | 提示振动 |

> 应用无需网络权限，所有数据本地存储。

---

## 📋 版本信息

| 项目 | 值 |
|------|-----|
| 应用名 | TomatoClock |
| 包名 | `com.xuweikai.tomatoclock` |
| 版本号 | 0.1.0 (versionCode: 1) |
| 最低 SDK | API 24 (Android 7.0) |
| 目标 SDK | API 36 |

---

## 📜 License

本项目为课程小组作业用途。
