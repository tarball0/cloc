package com.cloc.ring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import com.cloc.R
import com.cloc.alarm.AlarmScheduler
import com.cloc.data.AlarmStore
import com.cloc.data.TimerStore
import com.cloc.util.TimeFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

enum class RingKind { ALARM, TIMER }

/** What is ringing right now. */
data class RingInfo(
    val kind: RingKind,
    val alarmId: Long = -1,
    val label: String = "",
    val isBatch: Boolean = false,
    val position: Int = 1,
    val total: Int = 1,
    val startDay: String? = null,
    val timerId: Long = -1,
    val durationMs: Long = 0,
)

/**
 * A foreground service: a piece of the app that keeps running with a visible notification.
 * It plays the alarm sound on loop and vibrates until the user taps OK / I'M UP / SNOOZE.
 * The notification's "full-screen intent" opens [RingActivity] over the lock screen.
 */
class RingService : Service() {

    companion object {
        const val CHANNEL_ID = "ringing"
        private const val NOTIFICATION_ID = 42
        private const val AUTO_STOP_MS = 10 * 60_000L

        private const val ACTION_START = "com.cloc.ring.START"
        const val ACTION_OK = "com.cloc.ring.OK"
        const val ACTION_IM_UP = "com.cloc.ring.IM_UP"
        const val ACTION_SNOOZE = "com.cloc.ring.SNOOZE"

        private val _current = MutableStateFlow<RingInfo?>(null)
        /** The screens watch this; null means nothing is ringing. */
        val current: StateFlow<RingInfo?> = _current

        fun start(ctx: Context, info: RingInfo) {
            _current.value = info
            ctx.startForegroundService(Intent(ctx, RingService::class.java).setAction(ACTION_START))
        }

        /** OK / I'M UP / SNOOZE from the ring screen or the notification. */
        fun act(ctx: Context, action: String) {
            ctx.startService(Intent(ctx, RingService::class.java).setAction(action))
        }

        fun createChannel(ctx: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Ringing alarms and timers", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null) // the service plays the sound itself
                enableVibration(false)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { finish() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val info = _current.value ?: run { finish(); return START_NOT_STICKY }
                startForeground(NOTIFICATION_ID, buildNotification(info), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                startSound()
                handler.removeCallbacks(autoStop)
                handler.postDelayed(autoStop, AUTO_STOP_MS)
            }
            ACTION_OK -> {
                _current.value?.let { if (it.kind == RingKind.TIMER) TimerStore.reset(it.timerId) }
                finish()
            }
            ACTION_IM_UP -> {
                _current.value?.let { info ->
                    info.startDay?.let { AlarmStore.skipRun(info.alarmId, LocalDate.parse(it)) }
                }
                finish()
            }
            ACTION_SNOOZE -> {
                _current.value?.let { AlarmScheduler.scheduleSnooze(this, it.alarmId) }
                finish()
            }
            else -> finish()
        }
        return START_NOT_STICKY
    }

    private fun startSound() {
        if (player != null) return // a new batch alarm while ringing just updates the screen
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@RingService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            null
        }
        val vibrator = getSystemService(VibratorManager::class.java).defaultVibrator
        @Suppress("DEPRECATION") // the newer overload needs Android 13; this one works from 12
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 600, 900), 0),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
        )
    }

    private fun finish() {
        handler.removeCallbacks(autoStop)
        player?.run { try { stop() } catch (_: Exception) {}; release() }
        player = null
        getSystemService(VibratorManager::class.java).defaultVibrator.cancel()
        _current.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun buildNotification(info: RingInfo): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fun action(title: String, act: String, code: Int) = Notification.Action.Builder(
            null, title,
            PendingIntent.getService(
                this, code, Intent(this, RingService::class.java).setAction(act),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()

        val title = when (info.kind) {
            RingKind.TIMER -> "Timer done"
            RingKind.ALARM -> info.label.ifBlank { "Alarm" }
        }
        val text = when {
            info.kind == RingKind.TIMER -> TimeFormat.durationLabel(info.durationMs)
            info.isBatch -> "${info.position} / ${info.total}"
            else -> ""
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_cloc)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
            .addAction(action("OK", ACTION_OK, 1))
            .apply {
                if (info.kind == RingKind.ALARM) {
                    if (info.isBatch) addAction(action("I'm up", ACTION_IM_UP, 2))
                    else addAction(action("Snooze", ACTION_SNOOZE, 3))
                }
            }
            .build()
    }
}
