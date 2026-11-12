package com.cloc.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchMathTest {

    private fun hm(h: Int, m: Int) = h * 60 + m

    @Test fun linearEvery30() {
        assertEquals(listOf(hm(6, 30), hm(7, 0), hm(7, 30), hm(8, 0)), BatchMath.linear(hm(6, 30), 30, 4))
    }

    @Test fun customChain() {
        // 6:30 then +30, +15, +10, +2, +3 → 7:00, 7:15, 7:25, 7:27, 7:30
        val offsets = listOf(30, 45, 55, 57, 60)
        assertEquals(
            listOf(hm(6, 30), hm(7, 0), hm(7, 15), hm(7, 25), hm(7, 27), hm(7, 30)),
            BatchMath.custom(hm(6, 30), offsets),
        )
    }

    @Test fun logExampleFromPlan() {
        // 06:30 → 07:30, 5 alarms: gaps 32, 16, 8, 4
        assertEquals(
            listOf(hm(6, 30), hm(7, 2), hm(7, 18), hm(7, 26), hm(7, 30)),
            BatchMath.logarithmic(hm(6, 30), hm(7, 30), 5),
        )
    }

    @Test fun logAlwaysEndsExactlyOnEnd() {
        for (span in 2..600) for (count in 2..BatchMath.maxLogCount(span)) {
            val t = BatchMath.logarithmic(0, span, count)
            assertEquals("span=$span count=$count", span, t.last())
            assertEquals(count, t.size)
        }
    }

    @Test fun logGapsAtLeastOneMinuteAndShrinking() {
        for (span in 2..600) for (count in 2..BatchMath.maxLogCount(span)) {
            val gaps = BatchMath.logarithmic(0, span, count).zipWithNext { a, b -> b - a }
            assertTrue("span=$span count=$count gaps=$gaps", gaps.all { it >= 1 })
            assertTrue("span=$span count=$count gaps=$gaps", gaps.zipWithNext().all { (a, b) -> a >= b })
        }
    }

    @Test fun maxLogCount() {
        assertEquals(6, BatchMath.maxLogCount(60))   // gaps 31,15,8,4,2
        assertEquals(2, BatchMath.maxLogCount(1))
        assertEquals(3, BatchMath.maxLogCount(3))    // gaps 2,1
    }

    @Test fun logPastMidnight() {
        val t = BatchMath.logarithmic(hm(23, 30), hm(0, 30), 4)
        assertEquals(hm(23, 30), t.first())
        assertEquals(hm(24, 30), t.last())
    }
}
