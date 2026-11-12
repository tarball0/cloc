package com.cloc.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Stopwatch state. Uses [SystemClock.elapsedRealtime], which keeps counting while the
 * phone sleeps and can't be changed by the user, so it's perfect for measuring time.
 */
data class StopwatchState(
    val running: Boolean = false,
    val startElapsed: Long = 0L,   // elapsedRealtime when last started
    val accumulatedMs: Long = 0L,  // time banked from earlier runs
    val laps: List<Long> = emptyList(), // total elapsed at each lap press
) {
    fun elapsedAt(nowElapsed: Long): Long =
        accumulatedMs + if (running) (nowElapsed - startElapsed).coerceAtLeast(0) else 0
}

object StopwatchStore {
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(StopwatchState())
    val state: StateFlow<StopwatchState> = _state

    fun init(context: Context) {
        prefs = context.getSharedPreferences("stopwatch", Context.MODE_PRIVATE)
        var s = StopwatchState(
            running = prefs.getBoolean("running", false),
            startElapsed = prefs.getLong("start", 0L),
            accumulatedMs = prefs.getLong("acc", 0L),
            laps = prefs.getString("laps", "")!!.split(',').mapNotNull { it.toLongOrNull() },
        )
        // After a reboot elapsedRealtime restarts from zero, so a saved start is meaningless.
        if (s.running && s.startElapsed > SystemClock.elapsedRealtime()) s = s.copy(running = false)
        _state.value = s
    }

    private fun now() = SystemClock.elapsedRealtime()

    fun start() = set { if (it.running) it else it.copy(running = true, startElapsed = now()) }

    fun stop() = set { if (!it.running) it else it.copy(running = false, accumulatedMs = it.elapsedAt(now())) }

    fun lap() = set { if (!it.running) it else it.copy(laps = it.laps + it.elapsedAt(now())) }

    fun reset() = set { StopwatchState() }

    private inline fun set(change: (StopwatchState) -> StopwatchState) {
        val s = change(_state.value)
        _state.value = s
        prefs.edit()
            .putBoolean("running", s.running)
            .putLong("start", s.startElapsed)
            .putLong("acc", s.accumulatedMs)
            .putString("laps", s.laps.joinToString(","))
            .apply()
    }
}
