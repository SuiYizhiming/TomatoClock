# TomatoClock 工程规范升级补丁 (BUILD_CONVENTIONS.md)

本文档作为 `CODING_CONTEXT.md` 和 `AGENT_WORK_SPLIT.md` 的补充，旨在引入现代化 Android 工程标准，提升多 Agent/多环境协作的稳定性。请在项目初始化（Agent 0 阶段）及后续开发中严格遵守。

## 1. 环境与构建锁（版本目录制）

为了避免依赖版本冲突，全面弃用传统的 `build.gradle` 依赖硬编码，改用 **Version Catalog**。

### 1.1 依赖集中管理 (`gradle/libs.versions.toml`)
必须创建并使用该文件管理所有依赖，核心版本锁定如下：
*   **JDK**: OpenJDK 21 (通过 `jvmToolchain(21)` 强制绑定)
*   **Kotlin**: 2.1.21
*   **AGP (Android Gradle Plugin)**: 8.10.1
*   **Compose BOM**: 2026.02.00 (通过 BOM 管理所有 Compose UI/Material3 版本)
*   **KSP**: 2.1.21-2.0.2

### 1.2 Gradle Wrapper 锁定
*   必须使用 `gradle-wrapper.properties` 将 Gradle 版本固定为 **8.11.1**。

## 2. 跨平台/多 Agent 协作约束

由于项目涉及多 Agent 以及可能的双系统开发（Windows/WSL），必须从底层消灭环境差异造成的编译失败和 Git 冲突。

### 2.1 强制 LF 换行符
在项目根目录创建 `.gitattributes`，强制所有文本文件使用 Unix 换行符：
```gitattributes
* text=auto eol=lf
*.kt text eol=lf
*.kts text eol=lf
*.xml text eol=lf
*.toml text eol=lf
*.properties text eol=lf
```

### 2.2 动态 SDK 路径路由
严禁将 `local.properties` 提交入库（必须在 `.gitignore` 中）。
在 `settings.gradle.kts` 顶部添加动态 OS 检测逻辑，确保在 Windows 和 WSL 之间切换时，自动生成包含正确 `sdk.dir` 的 `local.properties`，避免互相覆盖报错。

## 3. 架构基础设施升级

在原有 MVVM + Clean Architecture 的基础上，明确底层技术选型。

### 3.1 全面启用 Hilt (Dagger) 进行依赖注入
*   应用入口必须声明 `@HiltAndroidApp`。
*   ViewModel 必须使用 `@HiltViewModel`。
*   通过模块（如 `DatabaseModule`, `RepositoryModule`, `UseCaseModule`）隔离不同层的依赖。

### 3.2 废弃 KAPT，强制使用 KSP
对于 Room 数据库和 Hilt，**严禁使用老旧的 KAPT 插件**，必须使用 KSP（Kotlin Symbol Processing）以加快编译速度并减少跨平台错误。

### 3.3 前台服务与 UI 的通信契约
针对 `CODING_CONTEXT.md` 中提到的“后台计时用前台 Service”，补充以下技术约束：
*   `ForegroundTimerService` 必须使用 `@AndroidEntryPoint` 接收注入。
*   Service 与 UI 之间的状态同步，禁止使用古老的 `BroadcastReceiver`。必须通过单例的 Repository（如 `TimerRepository`）暴露 `StateFlow<TimerState>`，让 ViewModel 去 `collect`。

## 4. 自动化测试基线规范

为了满足 `CODING_CONTEXT.md` 中要求的“核心逻辑要可单测”，规范测试目录与框架：

### 4.1 测试框架选型
*   单元测试：`JUnit 4`, `Mockito-Kotlin`, `kotlinx-coroutines-test`
*   UI 与集成测试：`Compose UI Test`, `androidx.room.testing`

### 4.2 测试职责划分
*   **`src/test/` (运行于 JVM)**: 必须覆盖 `TimerStateMachine` (状态流转)、`CycleManager` (周期计数)、各类 `UseCase` 和 `ViewModel` 的状态管理。
*   **`src/androidTest/` (运行于真机/模拟器)**: 必须覆盖 Room DAO 层 (`TimerSessionDao`, `FocusRecordDao` 等) 的 CRUD 验证、数据库 Migration 测试，以及部分 Compose UI 渲染测试。