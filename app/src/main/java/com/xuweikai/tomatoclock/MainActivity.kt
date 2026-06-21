package com.xuweikai.tomatoclock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuweikai.tomatoclock.app.navigation.TomatoClockApp
import com.xuweikai.tomatoclock.core.model.AppSettings
import com.xuweikai.tomatoclock.di.AppContainer
import com.xuweikai.tomatoclock.di.DefaultAppContainer
import com.xuweikai.tomatoclock.ui.theme.TomatoClockTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MainActivity : ComponentActivity() {
    private val appContainer: AppContainer by lazy {
        DefaultAppContainer(applicationContext)
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Alerts still fall back to toast/vibration when notifications are denied.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPostNotificationsIfNeeded()
        val settingsFlow: Flow<AppSettings> = appContainer.settingsRepository.observeSettings()
        setContent {
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())
            TomatoClockTheme(darkMode = settings.darkMode) {
                TomatoClockApp(appContainer = appContainer)
            }
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        requestNotificationPermission.launch(permission)
    }
}
