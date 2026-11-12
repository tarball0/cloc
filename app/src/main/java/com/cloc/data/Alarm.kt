package com.cloc.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

enum class AlarmKind { SINGLE, CUSTOM, LINEAR, LOG }

/**
 * One alarm card. A SINGLE alarm has one time; the batch kinds have many.
 *
 * @param days bitmask of weekdays (Monday = bit 0 … Sunday = bit 6). 0 means "once".
 * @param times ring times in minutes after midnight of the start day (≥ 1440 = next day).
 * @param skipDate the start day whose remaining alarms were skipped with I'M UP.
 * @param oneOffDate for a "once" alarm, the day it is set for.
 * @param start / interval / count / end: the inputs used in the editor, kept so it can be reopened.
 */
data class Alarm(
    val id: Long,
    val label: String = "",
    val kind: AlarmKind = AlarmKind.SINGLE,
    val days: Int = 0,
    val times: List<Int>,
    val enabled: Boolean = true,
    val skipDate: LocalDate? = null,
    val oneOffDate: LocalDate? = null,
    val start: Int = times.firstOrNull() ?: 0,
    val interval: Int = 30,
    val count: Int = times.size,
    val end: Int = times.lastOrNull() ?: 0,
) {
    val isBatch: Boolean get() = kind != AlarmKind.SINGLE

    fun hasDay(day: DayOfWeek): Boolean = days and (1 shl (day.value - 1)) != 0

    companion object {
        const val WEEKDAYS = 0b0011111
        const val WEEKEND = 0b1100000
        const val EVERY_DAY = 0b1111111

        /** The day a "once" alarm starting at [firstMinute] should use: today if still ahead, else tomorrow. */
        fun oneOffDateFor(firstMinute: Int, now: LocalDateTime): LocalDate {
            val today = now.toLocalDate()
            val t = today.atStartOfDay().plusMinutes(firstMinute.toLong())
            return if (t.isAfter(now)) today else today.plusDays(1)
        }
    }
}
