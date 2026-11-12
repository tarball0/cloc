package com.cloc.alarm

import com.cloc.data.Alarm
import com.cloc.data.AlarmKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleTest {
    // 2026-11-10 is a Tuesday.
    private val tue = LocalDate.of(2026, 11, 10)
    private val tueToThu = 0b0001110

    private fun batch(days: Int = tueToThu, skip: LocalDate? = null) = Alarm(
        id = 1, kind = AlarmKind.CUSTOM, days = days,
        times = listOf(390, 420, 435, 445, 447, 450), // 6:30 … 7:30
        skipDate = skip,
    )

    @Test fun laterTodayOnAnEnabledDay() {
        val now = tue.atTime(7, 10)
        assertEquals(tue.atTime(7, 15), Schedule.nextTrigger(batch(), 2, now))
    }

    @Test fun alreadyPassedMovesToNextEnabledDay() {
        val now = tue.atTime(7, 20)
        assertEquals(tue.plusDays(1).atTime(7, 15), Schedule.nextTrigger(batch(), 2, now))
    }

    @Test fun thursdayToNextTuesday() {
        val thuEvening = tue.plusDays(2).atTime(20, 0)
        assertEquals(tue.plusDays(7).atTime(6, 30), Schedule.nextTrigger(batch(), 0, thuEvening))
    }

    @Test fun skippedRunJumpsToNextDay() {
        val now = tue.atTime(6, 35)
        assertEquals(tue.plusDays(1).atTime(7, 0), Schedule.nextTrigger(batch(skip = tue), 1, now))
    }

    @Test fun batchPastMidnightBelongsToStartDay() {
        // Tuesday-only batch from 23:50 to 00:10 (next day)
        val a = Alarm(id = 2, kind = AlarmKind.LINEAR, days = 0b0000010, times = listOf(1430, 1440, 1450))
        val wedEarly = tue.plusDays(1).atTime(0, 5)
        assertEquals(tue.plusDays(1).atTime(0, 10), Schedule.nextTrigger(a, 2, wedEarly))
        assertEquals(tue.plusDays(1).atTime(0, 10).toLocalDate().minusDays(1), Schedule.startDayOf(tue.plusDays(1).atTime(0, 10), 1450))
    }

    @Test fun onceAlarm() {
        val a = Alarm(id = 3, days = 0, times = listOf(420), oneOffDate = tue)
        assertEquals(tue.atTime(7, 0), Schedule.nextTrigger(a, 0, tue.atTime(6, 0)))
        assertNull(Schedule.nextTrigger(a, 0, tue.atTime(7, 1)))
    }

    @Test fun oneOffDateRollsToTomorrow() {
        assertEquals(tue, Alarm.oneOffDateFor(420, LocalDateTime.of(tue, java.time.LocalTime.of(6, 0))))
        assertEquals(tue.plusDays(1), Alarm.oneOffDateFor(420, LocalDateTime.of(tue, java.time.LocalTime.of(8, 0))))
    }
}
