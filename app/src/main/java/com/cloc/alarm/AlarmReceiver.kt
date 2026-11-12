package com.cloc.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cloc.data.AlarmStore
import com.cloc.data.TimerStore
import com.cloc.ring.RingInfo
import com.cloc.ring.RingKind
import com.cloc.ring.RingService
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Android calls this when one of our scheduled alarms goes off.
 * It starts the ringing service and lines up the next ring.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_ALARM -> onAlarm(context, intent)
            AlarmScheduler.ACTION_SNOOZE -> onSnooze(context, intent)
            AlarmScheduler.ACTION_TIMER -> onTimer(context, intent)
        }
    }

    private fun onAlarm(context: Context, intent: Intent) {
        val alarm = AlarmStore.get(intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)) ?: return
        val index = intent.getIntExtra(AlarmScheduler.EXTRA_INDEX, 0)
        val triggerMs = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER, System.currentTimeMillis())
        val trigger = LocalDateTime.ofInstant(Instant.ofEpochMilli(triggerMs), ZoneId.systemDefault())
        val minutes = alarm.times.getOrNull(index) ?: return
        val startDay = Schedule.startDayOf(trigger, minutes)

        if (alarm.enabled && alarm.skipDate != startDay) {
            RingService.start(
                context,
                RingInfo(
                    kind = RingKind.ALARM,
                    alarmId = alarm.id,
                    label = alarm.label,
                    isBatch = alarm.isBatch,
                    position = index + 1,
                    total = alarm.times.size,
                    startDay = startDay.toString(),
                ),
            )
        }
        AlarmScheduler.scheduleAll(context, afterMs = triggerMs)
    }

    private fun onSnooze(context: Context, intent: Intent) {
        val alarm = AlarmStore.get(intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)) ?: return
        RingService.start(
            context,
            RingInfo(kind = RingKind.ALARM, alarmId = alarm.id, label = alarm.label, isBatch = false),
        )
    }

    private fun onTimer(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_TIMER_ID, -1)
        val timer = TimerStore.get(id) ?: return
        TimerStore.markFinished(id)
        RingService.start(context, RingInfo(kind = RingKind.TIMER, timerId = id, durationMs = timer.durationMs))
    }
}

/** After a reboot or a clock/time-zone change, Android forgets or misplaces alarms, so set them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                AlarmScheduler.scheduleAll(context)
                TimerStore.rescheduleRunning()
            }
        }
    }
}
