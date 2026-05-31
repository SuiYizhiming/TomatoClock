# 番茄钟 APP 多 Agent 编码分工

本文档用于把 `CODING_CONTEXT.md` 拆成多个可并行执行的编码任务。原则是：共享契约先定，业务模块按目录隔离，跨模块只通过接口/事件交互，避免多个 agent 同时改同一批文件。

## 0. 总体协作规则

- 所有 agent 必须先阅读 `CODING_CONTEXT.md` 和本文档。
- 不要跨越自己的文件所有权改别人的模块；确需改动共享契约时，先停下并让负责共享契约的 agent 或集成 agent 处理。
- 包名、目录名和类型名以 Agent 0 产出的工程结构为准。
- 业务逻辑优先放在 Domain/UseCase，不要写死在 Compose 页面里。
- 各模块 ViewModel 对 UI 暴露 `StateFlow<UiState>`，UI 只订阅状态并转发事件。
- 各模块之间不要直接互相访问数据库表；通过 Repository 接口、UseCase 或 Domain Event 交互。
- 如果多个 agent 同时开工，禁止同时修改这些文件：Gradle 配置、Manifest、AppDatabase、Navigation 根图、DI 根模块、主题根文件。

## 1. 推荐执行顺序

### 阶段 A：必须先完成

先启动 Agent 0。Agent 0 完成后，其他 agent 才能稳定并行。

```text
Agent 0: 工程骨架 + 共享契约 + 数据库基础
```

### 阶段 B：可并行

Agent 0 完成并提交后，可同时启动：

```text
Agent 1: 计时核心模块
Agent 2: 任务管理模块
Agent 3: 统计复盘模块
Agent 4: 系统配置与提醒模块
```

### 阶段 C：依赖前面模块

下面两个 agent 等阶段 B 的相关模块完成后再启动：

```text
Agent 5: UI 页面与导航集成
Agent 6: 集成测试、自测闭环、缺口修复
```

Agent 5 至少要等 Agent 1、2、3、4 的 ViewModel/UiState/事件接口稳定后再开工。Agent 6 必须最后开工。

## 2. Agent 0：工程骨架 + 共享契约 + 数据库基础

### 目标

创建 Android 工程基础结构，定义所有模块共享的数据模型、枚举、DAO、Repository 接口、事件接口和 DI 骨架。它是所有其他 agent 的前置依赖。

### 文件所有权

- Gradle 根配置、模块配置。
- `AndroidManifest.xml`。
- `MainActivity` 空壳。
- `core/model/**`
- `core/database/**`
- `core/datastore/**` 的基础键定义，不实现复杂设置逻辑。
- `core/domain/event/**`
- `core/common/**`
- `di/**` 根模块和占位 provider。
- 测试基础配置。

### 需要实现

- Android/Kotlin 工程可编译，最低 API 24。
- 推荐依赖：Compose、Navigation、Lifecycle ViewModel、Room、DataStore、Coroutines、Hilt 或等价 DI。
- 共享枚举：
  - `TimerMode`: `FOCUS`, `SHORT_BREAK`, `LONG_BREAK`
  - `TimerStatus`: `IDLE`, `RUNNING`, `PAUSED`, `COMPLETED`, `INVALID`
  - `OperationType`: `START`, `PAUSE`, `RESUME`, `RESET`, `COMPLETE`, `RESTORE`
- Room Entity：
  - `TimerSessionEntity`
  - `TimerOperationLogEntity`
  - `TaskEntity`
  - `FocusRecordEntity`
  - `DailyFocusStatsEntity`
- DAO 最小接口：
  - `TimerDao`: insert/update/query active/query by id/insert log
  - `TaskDao`: insert/update/delete/observe active and completed
  - `StatisticsDao`: insert focus record/upsert daily stats/query date range/null linked task
- Repository 接口：
  - `TimerRepository`
  - `TaskRepository`
  - `StatisticsRepository`
  - `SettingsRepository`
- Domain event 或回调契约：
  - `FocusCompletedEvent(sessionId, taskId?, durationSeconds, completedAt)`
  - `TaskDeletedEvent(taskId)`
- DataStore keys：
  - `focusDurationMin`
  - `shortBreakDurationMin`
  - `longBreakDurationMin`
  - `longBreakInterval`
  - `alertSound`
  - `vibrationEnabled`
- 基础测试：数据库建表、关键唯一约束 `focus_records.sessionId`。

### 不要实现

- 不写具体计时状态机。
- 不写具体 UI 页面。
- 不写任务业务规则。
- 不写统计归档逻辑。

### 完成标准

- 工程能编译。
- 空 App 能启动。
- 所有共享模型和接口命名稳定。
- 其他 agent 可以只依赖这些接口开始写业务。

## 3. Agent 1：计时核心模块

### 目标

实现番茄钟计时主逻辑：启动、暂停、继续、重置、状态恢复、周期切换、完成事件发出。该模块是业务主链路核心。

### 文件所有权

- `feature/timer/**`
- `domain/timer/**`
- `data/timer/**` 中 `TimerRepository` 实现。
- `service/ForegroundTimerService` 或同等计时服务。
- 计时模块单元测试。

### 依赖

- 必须等待 Agent 0 完成。
- 依赖 Agent 4 的 `SettingsRepository` 接口，但不能等待其实现；可先使用接口或 fake。
- 归档统计只发出 `FocusCompletedEvent` 或调用 `ArchiveFocusUseCase` 接口，不直接写统计表。
- 任务完成提示只保留 taskId，不直接修改任务。

### 需要实现

- `TimerStateMachine.transition(current, event)`。
- `CycleManager.nextAfter(session, validFocusCount, settings)`。
- `TimerViewModel`：
  - `startFocus(taskId: String?)`
  - `pauseOrResume()`
  - `reset()`
  - `confirmReset()`
  - `onTick()`
  - `onFinish()`
  - `restoreLatestSession()`
- 前台计时服务或可替代计时引擎：
  - running 用单调时间差重算 remainingSec。
  - paused 保持 remainingSec。
  - 避免系统时间修改影响倒计时。
- 数据写入：
  - 创建/更新 `TimerSession`。
  - 每个关键状态写 `TimerOperationLog`。
  - reset 标记 `INVALID`，不发完成事件。
- 周期规则：
  - focus 完成后进入 short/long break。
  - break 完成后准备下一次 focus，但不自动开始。
- 提醒触发只通过 Agent 4 定义的 `AlertManager`/接口调用。

### 不要实现

- 不实现任务列表 UI。
- 不直接完成任务。
- 不直接维护 `daily_focus_stats`。
- 不修改 Settings 页面。

### 单元测试重点

- 重复 start 不创建并行会话。
- pause/resume 后 remainingSec 正确。
- reset 后状态为 invalid 且不计入完成。
- 连续 4 个有效 focus 后进入 long break。
- break 完成后只进入 ready/idle，不自动 focus running。
- running 恢复时根据 elapsedRealtime 修正剩余时间。

## 4. Agent 2：任务管理模块

### 目标

实现任务新增、编辑、删除、批量删除、手动完成、选择任务开始专注，以及专注完成后的任务完成确认逻辑。

### 文件所有权

- `feature/tasks/**`
- `domain/task/**`
- `data/task/**` 中 `TaskRepository` 实现。
- 任务模块单元测试。

### 依赖

- 必须等待 Agent 0 完成。
- 可以与 Agent 1 并行，但“选择任务开始专注”只能调用 Agent 0/1 暴露的计时入口接口，不直接操作 TimerRepository 内部实现。
- 删除任务需要调用 `StatisticsRepository.clearLinkedTask(taskId)` 或等价接口，因此依赖 Agent 0 的接口；Agent 3 可后续实现实际行为。

### 需要实现

- `ValidateTaskUseCase.validateTitle(title)`：非空，1..100 字符，支持中文、英文、数字、常用符号。
- `TaskViewModel`：
  - `addTask(title)`
  - `updateTitle(id, title)`
  - `deleteTask(id)`
  - `deleteTasks(ids)`
  - `completeTask(id)`
  - `focusTask(id)`
  - `onFocusCompleted(taskId)`
- 任务排序：
  - 新任务进入未完成顶部。
  - 完成后进入已完成分区。
  - 保留 sortOrder。
- 删除规则：
  - 先解除 `focus_records.linkedTaskId`。
  - 再删除 Task。
- 任务状态：
  - 提前终止专注不完成任务。
  - 已完成任务可再次启动专注，但完成后不重复标记。
  - 专注中删除关联任务，完成后不弹任务完成提示。

### 不要实现

- 不写计时状态机。
- 不写统计归档。
- 不写设置存储。
- UI 可以只提供 ViewModel 状态和事件，不抢 Agent 5 的完整 Compose 页面。

### 单元测试重点

- 标题空/超长拦截。
- 新增任务排序和持久化。
- 编辑只改 title，不改 createdAt/isCompleted/sortOrder。
- 删除任务时调用解除历史关联。
- 手动完成状态正确。
- 专注完成确认后才完成任务。

## 5. Agent 3：统计复盘模块

### 目标

实现完成专注后的统计归档、今日概览、最近 7 天趋势、月度打卡数据查询。

### 文件所有权

- `feature/stats/**`
- `domain/stats/**`
- `data/stats/**` 中 `StatisticsRepository` 实现。
- 统计模块单元测试。

### 依赖

- 必须等待 Agent 0 完成。
- 可与 Agent 1、2、4 并行。
- 需要消费 Agent 1 发出的 focus completed 事件；如果 Agent 1 未完成，先用 fake event 测试。

### 需要实现

- `ArchiveFocusUseCase.execute(sessionOrEvent)`：
  - 只归档 `COMPLETED` 且 `FOCUS` 的有效会话。
  - 以 `sessionId` 唯一约束防重复。
  - 写入 `focus_records`。
  - 同步 upsert `daily_focus_stats`。
  - `dateKey` 按 `completedAt` 计算。
- `StatsViewModel`：
  - `loadTodaySummary()`
  - `loadWeeklyTrend()`
  - `loadMonthStats(year, month)`
- 查询输出模型：
  - 今日番茄数、今日专注时长、累计专注时长、昨日对比标签。
  - 最近 7 天连续日期，缺失补 0。
  - 当前月日历格子，打卡色阶，日期详情。
- `clearLinkedTask(taskId)`：删除任务时将历史记录 `linkedTaskId` 置 null。

### 不要实现

- 不写计时服务。
- 不写任务删除流程，只提供解除关联能力。
- 不写完整统计 UI，只提供 UI state。

### 单元测试重点

- 同一 session 重复归档只记录一次。
- 跨零点按 completedAt 归属。
- 最近 7 天不足数据补 0。
- 无数据时返回 0 值，不崩溃。
- 删除任务后历史记录保留且 linkedTaskId 置 null。

## 6. Agent 4：系统配置与提醒模块

### 目标

实现 AppSettings 的 DataStore 持久化、时长校验、音效/震动配置、提醒策略接口和基础提醒行为。

### 文件所有权

- `feature/settings/**`
- `domain/settings/**`
- `data/settings/**` 中 `SettingsRepository` 实现。
- `domain/alert/**`
- `data/alert/**`
- 本地音频资源。
- 设置与提醒模块测试。

### 依赖

- 必须等待 Agent 0 完成。
- 可与 Agent 1、2、3 并行。
- Agent 1 会调用 `SettingsRepository` 和 `AlertManager`，因此接口要尽早稳定。

### 需要实现

- `SettingsRepository`：
  - observe/load/save `AppSettings`。
  - 默认值：25/5/15/4/classic/true。
  - focus 时长范围 1..60。
  - longBreakInterval 范围 2..8。
- `SettingsViewModel`：
  - `updateDuration(type, value)`
  - `saveDurations()`
  - `selectAlertSound(sound)`
  - `setVibrationEnabled(enabled)`
  - `loadSettings()`
- 提醒：
  - `AlertManager.notifyFinish(mode)`
  - `AlertManager.playPreview(sound)`
  - 静音/音量 0 降级为弹窗或震动。
  - 后台通知能力预留或实现。
- 配置修改规则：
  - 保存立即持久化。
  - 当前运行 TimerSession 不重算，下一会话生效。

### 不要实现

- 不直接改 TimerSession。
- 不写计时状态机。
- 不写统计页面。

### 单元测试重点

- 默认配置正确。
- 非法时长拒绝保存。
- 保存后重新读取不丢失。
- 音效选择立即持久化。
- 提醒策略在静音时走降级路径。

## 7. Agent 5：UI 页面与导航集成

### 目标

实现 Compose 页面、主题、导航和各 ViewModel 的 UI 绑定。它负责用户可见体验，但不写核心业务规则。

### 文件所有权

- `app/MainActivity`
- `app/navigation/**`
- `ui/theme/**`
- `feature/timer/ui/**`
- `feature/tasks/ui/**`
- `feature/stats/ui/**`
- `feature/settings/ui/**`
- UI 预览和必要的 UI 测试。

### 依赖

- 必须等待 Agent 1、2、3、4 至少完成 ViewModel、UiState、事件接口。
- 如果业务模块没完全完成，可用 fake ViewModel 开始静态 UI，但最终绑定要等真实接口稳定。

### 需要实现

- 全局主题：
  - 主色 `#FF6B6B`
  - 专注背景 `#1A1A2E`
  - 休息背景 `#E8F4FD`
  - 页面背景 `#FAFAFA`
  - 文字三级灰、成功/危险色。
- 导航：
  - 计时页、任务页、统计页、设置页。
  - 任务页点击专注进入计时页。
- 计时页：
  - 环形进度条。
  - 大号倒计时居中。
  - 单主按钮：开始/暂停/继续。
  - 重置小图标 + BottomSheet 确认。
  - focus/break 背景切换。
- 任务页：
  - 未完成/已完成分区。
  - FAB 展开顶部内联输入框。
  - 任务行 checkbox、专注按钮、编辑 BottomSheet、删除确认 BottomSheet。
  - 已完成分区默认折叠。
- 统计页：
  - 今日概览。
  - 最近 7 天柱状图。
  - 月度打卡日历和日期详情浮层。
- 设置页：
  - 时长步进器。
  - 长休息间隔步进器。
  - 音效列表选择/试听。
  - 震动 Toggle。

### 不要实现

- 不在 UI 中直接写数据库。
- 不在 UI 中计算核心周期规则。
- 不直接处理统计归档。

### UI 验收重点

- 添加任务不是弹窗，而是内联输入。
- 编辑/删除/重置使用 BottomSheet。
- 统计页是单页滚动卡片流。
- 无数据/空任务状态显示正常。
- 小屏幕不遮挡、不溢出。

## 8. Agent 6：集成、自测与缺口修复

### 目标

最后统一接线，修复跨模块断点，补齐测试和验收缺口。它可以修改多个模块，但只做集成相关修复，不做大规模重构。

### 文件所有权

- 可修改所有模块的集成点。
- 集成测试、端到端测试、测试 fake。
- DI 绑定、Navigation 接线、Manifest 权限。

### 依赖

- 必须最后启动。
- 至少等待 Agent 1、2、3、4、5 的主要功能完成。

### 需要实现

- DI 真实绑定：
  - Repository 实现绑定。
  - UseCase 绑定。
  - ViewModel 注入。
- 事件流接线：
  - Timer focus completed -> Stats archive。
  - Timer focus completed with taskId -> Task complete confirm。
  - Task delete -> Stats clear linked task。
  - Settings -> Timer 下一会话读取新配置。
  - Timer finish -> AlertManager。
- Android Manifest：
  - 前台服务声明。
  - 通知权限按 Android 版本处理。
  - 不申请网络权限。
- P0 自测闭环：
  1. 新增任务，重启后仍存在。
  2. 关联任务启动专注，重复点击开始不会创建并行会话。
  3. 暂停、继续、重置确认，状态和日志正确。
  4. 完成一个 focus 后写入 `focus_records` 和 `daily_focus_stats`。
  5. 连续完成 4 个 focus 后进入长休息，否则短休息。
  6. 休息完成后准备下一个 focus，但不自动开始。
  7. 专注完成后确认完成任务，任务进入已完成分区。
  8. 删除已关联任务后，历史记录保留且 `linkedTaskId` 置 null。
  9. 修改时长配置，当前会话不变，下一会话生效。
  10. 无数据统计页、空任务页、异常输入均正常展示。
- 性能检查：
  - 启动/暂停/继续/重置响应。
  - 统计页加载。
  - 50 条任务、1000 条记录基础压力测试。

### 不要实现

- 不重新设计架构。
- 不擅自改共享模型字段含义。
- 不把业务逻辑搬到 UI 层。

## 9. 共享契约建议

如果 Agent 0 需要更明确的接口，可以按下面方向定义。具体签名可根据项目结构调整，但语义应保持一致。

```kotlin
interface TimerRepository {
    fun observeActiveSession(): Flow<TimerSession?>
    suspend fun getActiveSession(): TimerSession?
    suspend fun createSession(session: TimerSession)
    suspend fun updateSession(session: TimerSession)
    suspend fun appendLog(log: TimerOperationLog)
}

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun addTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: String)
    suspend fun findTask(id: String): Task?
}

interface StatisticsRepository {
    suspend fun archiveFocus(session: TimerSession)
    suspend fun clearLinkedTask(taskId: String)
    fun observeTodaySummary(): Flow<TodaySummary>
    suspend fun getWeeklyTrend(endDate: LocalDate): List<DailyTrend>
    suspend fun getMonthStats(year: Int, month: Int): List<DayFocusStats>
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun saveSettings(settings: AppSettings)
}

interface AlertManager {
    suspend fun notifyFinish(mode: TimerMode)
    suspend fun playPreview(sound: String)
}
```

## 10. 最小冲突文件边界

| 区域 | 负责人 | 其他 agent 是否可改 |
| --- | --- | --- |
| Gradle/Manifest/DI 根模块 | Agent 0，最终 Agent 6 | 否 |
| Room Entity/DAO/AppDatabase | Agent 0，最终 Agent 6 | 原则上否 |
| Timer 业务 | Agent 1 | 否 |
| Task 业务 | Agent 2 | 否 |
| Stats 业务 | Agent 3 | 否 |
| Settings/Alert 业务 | Agent 4 | 否 |
| Compose 页面/Navigation/Theme | Agent 5 | 业务 agent 不改 |
| 集成接线/端到端测试 | Agent 6 | 最后统一改 |

## 11. 可并行与不可并行总结

可以同时启动：

- Agent 1、2、3、4，在 Agent 0 完成之后。
- Agent 5 的静态 UI，可在 ViewModel 接口稳定后启动。

不要同时启动：

- Agent 0 和任何业务 agent。
- Agent 5 和业务 agent 同时修改同一个 feature 目录。
- Agent 6 和其他 agent 同时修集成点。

最推荐流程：

```text
1. Agent 0 完成工程骨架和共享契约
2. 并行启动 Agent 1/2/3/4
3. Agent 1/2/3/4 完成后启动 Agent 5
4. Agent 5 完成后启动 Agent 6 做最终接线和验收
```
