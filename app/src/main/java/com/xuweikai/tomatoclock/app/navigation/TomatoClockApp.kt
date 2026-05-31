package com.xuweikai.tomatoclock.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xuweikai.tomatoclock.di.AppContainer
import com.xuweikai.tomatoclock.feature.settings.SettingsViewModel
import com.xuweikai.tomatoclock.feature.settings.ui.SettingsScreen
import com.xuweikai.tomatoclock.feature.stats.StatsViewModel
import com.xuweikai.tomatoclock.feature.stats.ui.StatsScreen
import com.xuweikai.tomatoclock.feature.tasks.TaskUiEvent
import com.xuweikai.tomatoclock.feature.tasks.TaskViewModel
import com.xuweikai.tomatoclock.feature.tasks.ui.TasksScreen
import com.xuweikai.tomatoclock.feature.timer.TimerEffect
import com.xuweikai.tomatoclock.feature.timer.TimerViewModel
import com.xuweikai.tomatoclock.feature.timer.ui.TimerScreen
import java.util.Calendar
import kotlinx.coroutines.launch

private enum class TomatoRoute(
    val route: String,
    val label: String,
) {
    Timer("timer", "计时"),
    Tasks("tasks", "任务"),
    Stats("stats", "统计"),
    Settings("settings", "设置"),
}

@Composable
fun TomatoClockApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val timerViewModel: TimerViewModel = viewModel(
        factory = tomatoViewModelFactory {
            TimerViewModel(
                timerRepository = appContainer.timerRepository,
                settingsRepository = appContainer.settingsRepository,
                alertManager = appContainer.alertManager,
                focusCompletedEventSink = appContainer.focusCompletedEventSink,
            )
        },
    )
    val taskViewModel: TaskViewModel = viewModel(
        factory = tomatoViewModelFactory {
            TaskViewModel(
                taskRepository = appContainer.taskRepository,
                statisticsRepository = appContainer.statisticsRepository,
            )
        },
    )
    val statsViewModel: StatsViewModel = viewModel(
        factory = tomatoViewModelFactory {
            StatsViewModel(statisticsRepository = appContainer.statisticsRepository)
        },
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = tomatoViewModelFactory {
            SettingsViewModel(settingsRepository = appContainer.settingsRepository)
        },
    )

    LaunchedEffect(timerViewModel) {
        timerViewModel.restoreLatestSession()
        timerViewModel.effects.collect { effect ->
            if (effect is TimerEffect.FocusCompleted) {
                taskViewModel.onFocusCompleted(effect.event.taskId)
                statsViewModel.loadTodaySummary()
                statsViewModel.loadWeeklyTrend()
                val calendar = Calendar.getInstance()
                statsViewModel.loadMonthStats(
                    year = calendar.get(Calendar.YEAR),
                    month = calendar.get(Calendar.MONTH) + 1,
                )
            }
        }
    }

    LaunchedEffect(taskViewModel, navController) {
        taskViewModel.events.collect { event ->
            if (event is TaskUiEvent.StartFocus) {
                timerViewModel.startFocus(event.taskId)
                navController.navigate(TomatoRoute.Timer.route) {
                    launchSingleTop = true
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    restoreState = true
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            TomatoBottomBar(
                currentRoute = navController.currentBackStackEntryAsState().value
                    ?.destination
                    ?.route,
                onNavigate = { route ->
                    navController.navigate(route.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TomatoRoute.Timer.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TomatoRoute.Timer.route) {
                val uiState by timerViewModel.uiState.collectAsStateWithLifecycle()
                TimerScreen(
                    uiState = uiState,
                    onPrimaryAction = {
                        when {
                            uiState.isRunning || uiState.isPaused -> timerViewModel.pauseOrResume()
                            else -> timerViewModel.startFocus()
                        }
                    },
                    onConfirmReset = timerViewModel::confirmReset,
                )
            }
            composable(TomatoRoute.Tasks.route) {
                val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()
                TasksScreen(
                    uiState = uiState,
                    onAddTask = taskViewModel::addTask,
                    onUpdateTask = taskViewModel::updateTitle,
                    onDeleteTask = taskViewModel::deleteTask,
                    onCompleteTask = taskViewModel::completeTask,
                    onFocusTask = taskViewModel::focusTask,
                    onCompletedExpandedChange = taskViewModel::setCompletedExpanded,
                    onConfirmFocusCompletedTask = taskViewModel::confirmFocusCompletedTask,
                    onDismissFocusCompletedTask = taskViewModel::dismissFocusCompletedTask,
                    onClearTitleError = taskViewModel::clearTitleError,
                )
            }
            composable(TomatoRoute.Stats.route) {
                val uiState by statsViewModel.uiState.collectAsStateWithLifecycle()
                StatsScreen(
                    uiState = uiState,
                    onLoadTodaySummary = statsViewModel::loadTodaySummary,
                    onLoadWeeklyTrend = statsViewModel::loadWeeklyTrend,
                    onLoadMonthStats = statsViewModel::loadMonthStats,
                    onSelectDate = statsViewModel::selectDate,
                )
            }
            composable(TomatoRoute.Settings.route) {
                val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = uiState,
                    onUpdateDuration = settingsViewModel::updateDuration,
                    onSaveDurations = settingsViewModel::saveDurations,
                    onSelectAlertSound = settingsViewModel::selectAlertSound,
                    onPreviewAlertSound = { sound ->
                        coroutineScope.launch {
                            appContainer.alertManager.playPreview(sound)
                        }
                    },
                    onVibrationEnabledChange = settingsViewModel::setVibrationEnabled,
                )
            }
        }
    }
}

@Composable
private fun TomatoBottomBar(
    currentRoute: String?,
    onNavigate: (TomatoRoute) -> Unit,
) {
    NavigationBar {
        TomatoRoute.entries.forEach { route ->
            NavigationBarItem(
                selected = currentRoute == route.route,
                onClick = { onNavigate(route) },
                icon = { Text(route.label.take(1)) },
                label = { Text(route.label) },
            )
        }
    }
}

private inline fun <reified T : ViewModel> tomatoViewModelFactory(
    crossinline create: () -> T,
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            if (modelClass.isAssignableFrom(T::class.java)) {
                return create() as VM
            }
            error("Unknown ViewModel class ${modelClass.name}")
        }
    }
}
