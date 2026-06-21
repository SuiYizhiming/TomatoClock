package com.xuweikai.tomatoclock.data.alert

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xuweikai.tomatoclock.core.domain.alert.AlertDelivery
import com.xuweikai.tomatoclock.core.domain.alert.AlertEnvironment
import com.xuweikai.tomatoclock.core.domain.alert.BasicAlertStrategy
import com.xuweikai.tomatoclock.core.domain.repository.AlertManager
import com.xuweikai.tomatoclock.core.domain.repository.SettingsRepository
import com.xuweikai.tomatoclock.core.domain.settings.SettingsValidator
import com.xuweikai.tomatoclock.core.model.TimerMode
import com.xuweikai.tomatoclock.core.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AndroidAlertManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : AlertManager {

    override suspend fun notifyFinish(mode: TimerMode) {
        val settings = settingsRepository.getSettings()
        val delivery = BasicAlertStrategy.chooseDelivery(
            AlertEnvironment(
                isSilent = isSilent(),
                vibrationEnabled = settings.vibrationEnabled,
            ),
        )

        showSystemNotification(mode)
        deliver(mode = mode, settings = settings, delivery = delivery)
    }

    override suspend fun playPreview(sound: String) {
        require(SettingsValidator.isSupportedAlertSound(sound)) {
            "Unsupported alert sound: $sound"
        }
        // Always play tone for preview, regardless of silent/vibration settings.
        val played = playTone(sound)
        if (!played) {
            showToast("Alert preview")
        }
    }

    private suspend fun deliver(
        mode: TimerMode,
        settings: AppSettings,
        delivery: AlertDelivery,
    ) {
        var delivered = false

        if (delivery == AlertDelivery.SOUND_ONLY || delivery == AlertDelivery.SOUND_AND_VIBRATION) {
            delivered = playTone(settings.alertSound) || delivered
        }

        if (delivery == AlertDelivery.SOUND_AND_VIBRATION || delivery == AlertDelivery.VIBRATION_ONLY) {
            delivered = vibrate() || delivered
        }

        if (!delivered || delivery == AlertDelivery.POPUP_ONLY) {
            showToast(finishMessage(mode))
        }
    }

    private fun isSilent(): Boolean {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return true
        val notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        return audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL || notificationVolume == 0
    }

    private suspend fun playTone(sound: String): Boolean = withContext(Dispatchers.Main) {
        runCatching {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME_PERCENT)
            toneGenerator.startTone(toneFor(sound), durationFor(sound))
            delay(RELEASE_TONE_DELAY_MS)
            toneGenerator.release()
        }.isSuccess
    }

    private fun vibrate(): Boolean {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return false

        if (!vibrator.hasVibrator()) return false

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        VIBRATION_DURATION_MS,
                        VibrationEffect.DEFAULT_AMPLITUDE,
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VIBRATION_DURATION_MS)
            }
        }.isSuccess
    }

    @SuppressLint("MissingPermission")
    private fun showSystemNotification(mode: TimerMode) {
        if (!canPostNotifications()) return
        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(finishMessage(mode))
            .setContentText("Open TomatoClock to continue.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
    }

    private suspend fun showToast(message: String) = withContext(Dispatchers.Main) {
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun finishMessage(mode: TimerMode): String {
        return when (mode) {
            TimerMode.FOCUS -> "Focus session complete"
            TimerMode.SHORT_BREAK -> "Short break complete"
            TimerMode.LONG_BREAK -> "Long break complete"
        }
    }

    private fun toneFor(sound: String): Int {
        return when (sound) {
            "soft" -> ToneGenerator.TONE_PROP_ACK
            "pulse" -> ToneGenerator.TONE_PROP_BEEP2
            else -> ToneGenerator.TONE_PROP_BEEP
        }
    }

    private fun durationFor(sound: String): Int {
        return when (sound) {
            "soft" -> 140
            "pulse" -> 240
            else -> 180
        }
    }

    private companion object {
        const val CHANNEL_ID = "timer_alerts"
        const val NOTIFICATION_ID = 1001
        const val TONE_VOLUME_PERCENT = 80
        const val RELEASE_TONE_DELAY_MS = 280L
        const val VIBRATION_DURATION_MS = 300L
    }
}
