package com.cloc.alarm

import kotlin.math.ceil
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

    /**
     * Custom, built one alarm at a time: adds [clock] (0–1439) to [times].
     * The time is placed on whichever day (today or the next) keeps the whole batch shortest,
     * so 23:50 then 00:10 becomes a 20-minute batch, and 06:00 then 05:50 puts 05:50 first.
     * Returns [times] unchanged if the alarm is already there, the batch is full, or it would be too long.
     */
    fun addCustom(times: List<Int>, clock: Int): List<Int> {
        if (times.isEmpty()) return listOf(clock % 1440)
        if (times.size >= MAX_ALARMS) return times
        val best = listOf(clock - 1440, clock, clock + 1440)
            .map { (times + it).sorted() }
            .minBy { it.last() - it.first() }
        val shift = if (best.first() < 0) 1440 else if (best.first() >= 1440) -1440 else 0
        val result = best.map { it + shift }.distinct()
        return if (result.size == times.size || result.last() > MAX_MINUTE) times else result
    }

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
     * Logarithmic: [count] alarms from [start] to [end], each gap roughly half the one before.
     * Alarms snap to multiples of 5 where that keeps the halving shape; small gaps near the end
     * stay exact. Gaps are whole minutes, at least 1, never growing, and the last alarm is on [end].
     * Example: 06:30 → 07:30, 6 alarms → 06:30, 07:00, 07:15, 07:25, 07:28, 07:30.
     */
    fun logarithmic(start: Int, end: Int, count: Int): List<Int> {
        val total = span(start, end)
        val n = (count - 1).coerceIn(1, maxLogCount(total) - 1)
        val denominator = 1 - 2.0.pow(-n)
        val ideal = DoubleArray(n) { k -> total * 2.0.pow(-(k + 1)) / denominator }

        // Options for each alarm after the first: whole minutes near the ideal time, plus the
        // multiples of 5 either side. The last alarm is always exactly on the end.
        var idealTime = start.toDouble()
        val options = List(n) { k ->
            idealTime += ideal[k]
            if (k == n - 1) listOf(start + total)
            else {
                val x = idealTime
                listOf(
                    x.roundToInt(), floor(x).toInt(), ceil(x).toInt(),
                    5 * floor(x / 5).toInt(), 5 * ceil(x / 5).toInt(),
                ).distinct()
            }
        }

        // Best path to each (time, last gap): cost = how far each gap strays from halving,
        // minus a bonus for every alarm on a round time. Gaps must be ≥ 1 and never grow.
        data class Path(val cost: Double, val times: List<Int>)
        var paths = mapOf((start to Int.MAX_VALUE) to Path(0.0, listOf(start)))
        for (k in 0 until n) {
            val next = HashMap<Pair<Int, Int>, Path>()
            for ((key, path) in paths) {
                val (prev, prevGap) = key
                for (t in options[k]) {
                    val gap = t - prev
                    if (gap < 1 || gap > prevGap) continue
                    val miss = (gap - ideal[k]) / ideal[k]
                    val round = if (k < n - 1 && t % 5 == 0) ROUND_BONUS else 0.0
                    val cost = path.cost + miss * miss - round
                    val old = next[t to gap]
                    if (old == null || cost < old.cost) next[t to gap] = Path(cost, path.times + t)
                }
            }
            paths = next
        }
        return paths.values.minByOrNull { it.cost }?.times ?: exactLog(start, end, count)
    }

    /** How much a LOG batch prefers times on multiples of 5 over the exact halving. */
    private const val ROUND_BONUS = 0.15

    /** LOG batch with the exact halving gaps, rounded to whole minutes (fallback). */
    private fun exactLog(start: Int, end: Int, count: Int): List<Int> {
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
