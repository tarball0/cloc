package com.cloc.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cloc.MainActivity
import com.cloc.data.AlarmStore
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Talks to Android's AlarmManager, the system service that wakes the app at a set time,
 * even if the app is closed or the phone is asleep.
 *
 * Each ring time gets its own PendingIntent, told apart by a request code:
 *  - alarm times:  id × 100 + index
 *  - snoozes:      SNOOZE_BASE + id
 *  - timers:       TIMER_BASE + id
 */
object AlarmScheduler {
    const val ACTION_ALARM = "com.cloc.action.ALARM"
    const val ACTION_SNOOZE = "com.cloc.action.SNOOZE"
    const val ACTION_TIMER = "com.cloc.action.TIMER"

    const val EXTRA_ALARM_ID = "alarmId"
    const val EXTRA_INDEX = "index"
    const val EXTRA_TRIGGER = "trigger"
    const val EXTRA_TIMER_ID = "timerId"

    private const val SNOOZE_BASE = 1_500_000_000
    private const val TIMER_BASE = 1_600_000_000
    const val SNOOZE_MINUTES = 10L

    private fun manager(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    /** Cancels and re-creates every alarm's next ring. Called after any change. */
    fun scheduleAll(ctx: Context, afterMs: Long = 0L) {
        // afterMs guards against an alarm being delivered a few ms early and then rescheduled for the same moment.
        val nowMs = maxOf(System.currentTimeMillis(), afterMs + 1000)
        val now = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMs), ZoneId.systemDefault())
        for (alarm in AlarmStore.alarms.value) {
            cancelAlarm(ctx, alarm.id)
            if (!alarm.enabled) continue
            var anyLeft = false
            alarm.times.indices.forEach { i ->
                val t = Schedule.nextTrigger(alarm, i, now) ?: return@forEach
                anyLeft = true
                val ms = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                setAlarmClock(ctx, ms, alarmIntent(ctx, alarm.id, i, ms))
            }
            // A "once" alarm that has nothing left to ring switches itself off.
            if (!anyLeft && alarm.days == 0) AlarmStore.disableQuietly(alarm.id)
        }
    }

    fun cancelAlarm(ctx: Context, id: Long) {
        val am = manager(ctx)
        for (i in 0 until BatchMath.MAX_ALARMS) {
            existing(ctx, ACTION_ALARM, (id * 100 + i).toInt())?.let { am.cancel(it); it.cancel() }
        }
    }

    fun scheduleSnooze(ctx: Context, alarmId: Long) {
        val at = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .setAction(ACTION_SNOOZE)
            .putExtra(EXTRA_ALARM_ID, alarmId)
        setAlarmClock(ctx, at, broadcast(ctx, SNOOZE_BASE + alarmId.toInt(), intent))
    }

    fun scheduleTimer(ctx: Context, timerId: Long, atWallMs: Long) {
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .setAction(ACTION_TIMER)
            .putExtra(EXTRA_TIMER_ID, timerId)
        val pi = broadcast(ctx, TIMER_BASE + timerId.toInt(), intent)
        val am = manager(ctx)
        try {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atWallMs, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atWallMs, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atWallMs, pi)
        }
    }

    fun cancelTimer(ctx: Context, timerId: Long) {
        existing(ctx, ACTION_TIMER, TIMER_BASE + timerId.toInt())?.let { manager(ctx).cancel(it); it.cancel() }
    }

    // ---- helpers ----

    private fun alarmIntent(ctx: Context, id: Long, index: Int, triggerMs: Long): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .setAction(ACTION_ALARM)
            .putExtra(EXTRA_ALARM_ID, id)
            .putExtra(EXTRA_INDEX, index)
            .putExtra(EXTRA_TRIGGER, triggerMs)
        return broadcast(ctx, (id * 100 + index).toInt(), intent)
    }

    /**
     * setAlarmClock is the most reliable kind of alarm: it fires exactly on time even in
     * battery-saving mode, and shows the alarm icon in the status bar.
     */
    private fun setAlarmClock(ctx: Context, atMs: Long, pi: PendingIntent) {
        val am = manager(ctx)
        val show = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(atMs, show), pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }

    private fun broadcast(ctx: Context, code: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun existing(ctx: Context, action: String, code: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            ctx, code,
            Intent(ctx, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
}
