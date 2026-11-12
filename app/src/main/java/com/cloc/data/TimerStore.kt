package com.cloc.data

import android.content.Context
import android.content.SharedPreferences
import com.cloc.alarm.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * One countdown timer.
 * While running, [endAtWallMs] is the wall-clock time it ends, so it keeps counting
 * even if the app is closed. While paused or idle it is null and [remainingMs] holds the time left.
 */
data class TimerItem(
    val id: Long,
    val durationMs: Long,
    val remainingMs: Long = durationMs,
    val endAtWallMs: Long? = null,
    val finished: Boolean = false,
) {
    val running: Boolean get() = endAtWallMs != null

    fun remainingAt(nowMs: Long): Long =
        if (endAtWallMs != null) (endAtWallMs - nowMs).coerceAtLeast(0) else remainingMs

    /** Started at least once and not back at full time. */
    val started: Boolean get() = running || remainingMs < durationMs || finished
}

object TimerStore {
    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences
    private val _timers = MutableStateFlow<List<TimerItem>>(emptyList())
    val timers: StateFlow<List<TimerItem>> = _timers

    fun init(context: Context) {
        app = context.applicationContext
        prefs = app.getSharedPreferences("timers", Context.MODE_PRIVATE)
        _timers.value = load()
    }

    fun get(id: Long): TimerItem? = _timers.value.firstOrNull { it.id == id }

    fun add(durationMs: Long, start: Boolean): Long {
        val id = (_timers.value.maxOfOrNull { it.id } ?: 0L) + 1
        commit(_timers.value + TimerItem(id, durationMs))
        if (start) start(id)
        return id
    }

    fun start(id: Long) = update(id) { t ->
        if (t.running) return@update t
        val remaining = if (t.finished || t.remainingMs <= 0) t.durationMs else t.remainingMs
        val end = System.currentTimeMillis() + remaining
        AlarmScheduler.scheduleTimer(app, id, end)
        t.copy(remainingMs = remaining, endAtWallMs = end, finished = false)
    }

    fun pause(id: Long) = update(id) { t ->
        if (!t.running) return@update t
        AlarmScheduler.cancelTimer(app, id)
        t.copy(remainingMs = t.remainingAt(System.currentTimeMillis()), endAtWallMs = null)
    }

    fun toggle(id: Long) {
        val t = get(id) ?: return
        if (t.running) pause(id) else start(id)
    }

    fun reset(id: Long) = update(id) { t ->
        AlarmScheduler.cancelTimer(app, id)
        t.copy(remainingMs = t.durationMs, endAtWallMs = null, finished = false)
    }

    fun delete(id: Long) {
        AlarmScheduler.cancelTimer(app, id)
        commit(_timers.value.filterNot { it.id == id })
    }

    /** Called when the timer's alarm fires. */
    fun markFinished(id: Long) = update(id) { it.copy(remainingMs = 0, endAtWallMs = null, finished = true) }

    /** After a reboot the system forgets alarms, so set them again. */
    fun rescheduleRunning() {
        val now = System.currentTimeMillis()
        _timers.value.filter { it.running }.forEach {
            AlarmScheduler.scheduleTimer(app, it.id, maxOf(it.endAtWallMs!!, now + 1000))
        }
    }

    private inline fun update(id: Long, change: (TimerItem) -> TimerItem) {
        commit(_timers.value.map { if (it.id == id) change(it) else it })
    }

    private fun commit(list: List<TimerItem>) {
        _timers.value = list
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("duration", it.durationMs)
                put("remaining", it.remainingMs)
                put("endAt", it.endAtWallMs ?: -1L)
                put("finished", it.finished)
            })
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    private fun load(): List<TimerItem> {
        val raw = prefs.getString("list", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val end = o.getLong("endAt")
                TimerItem(
                    id = o.getLong("id"),
                    durationMs = o.getLong("duration"),
                    remainingMs = o.getLong("remaining"),
                    endAtWallMs = if (end < 0) null else end,
                    finished = o.getBoolean("finished"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
