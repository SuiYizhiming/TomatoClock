package com.xuweikai.tomatoclock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.xuweikai.tomatoclock.core.model.DarkMode

val TomatoPrimary = Color(0xFFFF6B6B)
val TomatoPrimaryPressed = Color(0xFFE55A5A)
val TomatoPrimaryLight = Color(0xFFFF8E8E)
val FocusBackground = Color(0xFF1A1A2E)
val BreakBackground = Color(0xFFE8F4FD)
val PageBackground = Color(0xFFFAFAFA)
val CardBackground = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF333333)
val TextSecondary = Color(0xFF666666)
val TextTertiary = Color(0xFF999999)
val Success = Color(0xFF4CAF50)
val Danger = Color(0xFFF44336)
val DividerColor = Color(0xFFE0E0E0)

private val LightColorScheme = lightColorScheme(
    primary = TomatoPrimary,
    onPrimary = Color.White,
    primaryContainer = TomatoPrimaryLight,
    onPrimaryContainer = Color.White,
    secondary = Success,
    onSecondary = Color.White,
    error = Danger,
    background = PageBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
)

private val DarkColorScheme = darkColorScheme(
    primary = TomatoPrimary,
    onPrimary = Color.White,
    primaryContainer = FocusBackground,
    onPrimaryContainer = Color.White,
    secondary = Success,
    onSecondary = Color.White,
    error = Danger,
    background = FocusBackground,
    onBackground = Color.White,
    surface = Color(0xFF24243A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF30304A),
    onSurfaceVariant = Color(0xFFD8D8E5),
    outline = Color(0xFF55556A),
)

@Composable
fun TomatoClockTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TomatoTypography,
        content = content,
    )
}
