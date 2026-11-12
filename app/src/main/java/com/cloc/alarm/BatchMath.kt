package com.cloc.alarm

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Works out the ring times for batch alarms.
 * All times are whole minutes after midnight of the day the batch starts.
 * Values of 1440 or more mean "the next day".
 */
object BatchMath {

    /** A batch may not run past the end of the following day. */
    const val MAX_MINUTE = 2 * 1440 - 1
    const val MAX_ALARMS = 60

    /** Custom: the first time, then a chain of offsets (each one minutes after the first). */
    fun custom(first: Int, offsets: List<Int>): List<Int> =
        (listOf(first) + offsets.map { first + it })
            .filter { it <= MAX_MINUTE }
            .distinct()
            .sorted()

    /** Linear: start, start + interval, start + 2 × interval … */
    fun linear(start: Int, interval: Int, count: Int): List<Int> =
        List(count) { i -> start + i * interval }.filter { it <= MAX_MINUTE }

    /** Largest count that still fits before MAX_MINUTE. */
    fun maxLinearCount(start: Int, interval: Int): Int =
        ((MAX_MINUTE - start) / interval.coerceAtLeast(1) + 1).coerceIn(1, MAX_ALARMS)

    /** Minutes from start to end; an end before the start means tomorrow. */
    fun span(start: Int, end: Int): Int {
        val d = end - start
        return if (d > 0) d else d + 1440
    }

    /**
     * Logarithmic: [count] alarms from [start] to [end], each gap half the one before.
     * Gaps are rounded to whole minutes (at least 1), and the first gap absorbs any
     * rounding difference so the last alarm lands exactly on [end].
     * Example: 06:30 → 07:30, 5 alarms → gaps 32, 16, 8, 4.
     */
    fun logarithmic(start: Int, end: Int, count: Int): List<Int> {
        val total = span(start, end)
        val n = (count - 1).coerceIn(1, maxLogCount(total) - 1)
        val denominator = 1 - 2.0.pow(-n)
        val gaps = IntArray(n) { k ->
            (total * 2.0.pow(-(k + 1)) / denominator).roundToInt().coerceAtLeast(1)
        }
        gaps[0] += total - gaps.sum()
        val times = ArrayList<Int>(n + 1)
        var t = start
        times += t
        for (g in gaps) {
            t += g
            times += t
        }
        return times
    }

    /** Largest count for which the smallest gap (before rounding) is still at least 1 minute. */
    fun maxLogCount(totalMinutes: Int): Int {
        if (totalMinutes < 1) return 2
        // smallest gap = total / (2^n − 1) ≥ 1  ⇔  2^n ≤ total + 1
        val n = floor(ln(totalMinutes + 1.0) / ln(2.0)).toInt()
        return (n + 1).coerceIn(2, MAX_ALARMS)
    }
}
