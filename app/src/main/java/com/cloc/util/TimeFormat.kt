package com.cloc.util

import kotlin.math.ceil

/** Helpers that turn numbers into the strings shown on screen. */
object TimeFormat {

    /**
     * A time of day in minutes (0..1439, larger values wrap to the next day).
     * Returns the number part and, for 12-hour phones, "AM"/"PM" separately,
     * so the number can be drawn in Ndot and the suffix in small Lettera.
     */
    fun clock(minutesOfDay: Int, is24h: Boolean): Pair<String, String?> {
        val m = ((minutesOfDay % 1440) + 1440) % 1440
        val h = m / 60
        val min = m % 60
        return if (is24h) {
            "%02d:%02d".format(h, min) to null
        } else {
            val h12 = if (h % 12 == 0) 12 else h % 12
            "%d:%02d".format(h12, min) to (if (h < 12) "AM" else "PM")
        }
    }

    /** Clock string including the AM/PM suffix, for plain text. */
    fun clockText(minutesOfDay: Int, is24h: Boolean): String {
        val (num, suffix) = clock(minutesOfDay, is24h)
        return if (suffix == null) num else "$num $suffix"
    }

    /** Countdown: rounds up, so "00:01" shows until the very end. H:MM:SS when over an hour. */
    fun countdown(ms: Long): String {
        val totalSec = ceil(ms.coerceAtLeast(0) / 1000.0).toLong()
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    /** Stopwatch: MM:SS.cc, or H:MM:SS.cc past an hour. */
    fun stopwatch(ms: Long): String {
        val cs = (ms / 10) % 100
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs) else "%02d:%02d.%02d".format(m, s, cs)
    }

    /** A gap in minutes, e.g. "+30" or "+1:30". */
    fun gap(minutes: Int): String =
        if (minutes < 60) "+$minutes" else "+%d:%02d".format(minutes / 60, minutes % 60)

    /** A whole duration for labels, e.g. "10 MIN", "1 H 30 MIN", "45 S". */
    fun durationLabel(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val parts = buildList {
            if (h > 0) add("$h H")
            if (m > 0) add("$m MIN")
            if (s > 0) add("$s S")
        }
        return if (parts.isEmpty()) "0 S" else parts.joinToString(" ")
    }
}
