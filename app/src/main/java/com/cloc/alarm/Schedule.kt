package com.cloc.alarm

import com.cloc.data.Alarm
import java.time.LocalDate
import java.time.LocalDateTime

/** Pure date maths for "when does this alarm ring next?". No Android code, so it can be unit-tested. */
object Schedule {

    /** Next time alarm [index] of [alarm] should ring after [now], or null if never. */
    fun nextTrigger(alarm: Alarm, index: Int, now: LocalDateTime): LocalDateTime? {
        val minutes = alarm.times.getOrNull(index) ?: return null

        if (alarm.days == 0) {
            val day = alarm.oneOffDate ?: return null
            if (alarm.skipDate == day) return null
            val t = day.atStartOfDay().plusMinutes(minutes.toLong())
            return if (t.isAfter(now)) t else null
        }

        val today = now.toLocalDate()
        // Start two days back: a batch that began yesterday may still have alarms after midnight.
        for (offset in -2L..8L) {
            val day = today.plusDays(offset)
            if (!alarm.hasDay(day.dayOfWeek)) continue
            if (alarm.skipDate == day) continue
            val t = day.atStartOfDay().plusMinutes(minutes.toLong())
            if (t.isAfter(now)) return t
        }
        return null
    }

    /** The day a batch run started on, given when one of its alarms rang. */
    fun startDayOf(trigger: LocalDateTime, minutes: Int): LocalDate =
        trigger.toLocalDate().minusDays((minutes / 1440).toLong())
}
