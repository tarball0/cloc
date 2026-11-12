package com.cloc.data

import android.content.Context
import android.content.SharedPreferences
import com.cloc.alarm.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Holds every alarm, saves them to the phone, and reschedules after each change.
 * The UI reads [alarms]; Compose redraws automatically when it changes.
 */
object AlarmStore {
    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences
    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms

    fun init(context: Context) {
        app = context.applicationContext
        prefs = app.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        _alarms.value = load()
    }

    fun get(id: Long): Alarm? = _alarms.value.firstOrNull { it.id == id }

    fun nextId(): Long = (_alarms.value.maxOfOrNull { it.id } ?: 0L) + 1

    /** Add a new alarm or replace the one with the same id. */
    fun save(alarm: Alarm) {
        val fixed = withOneOffDate(alarm)
        val list = _alarms.value.filterNot { it.id == fixed.id } + fixed
        commit(list.sortedBy { it.times.firstOrNull() ?: 0 })
    }

    fun delete(id: Long) {
        AlarmScheduler.cancelAlarm(app, id)
        commit(_alarms.value.filterNot { it.id == id })
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        val alarm = get(id) ?: return
        save(alarm.copy(enabled = enabled, skipDate = if (enabled) null else alarm.skipDate))
    }

    /** I'M UP: skip the rest of the batch run that started on [startDay]. */
    fun skipRun(id: Long, startDay: LocalDate) {
        val alarm = get(id) ?: return
        commit(_alarms.value.map { if (it.id == id) alarm.copy(skipDate = startDay) else it })
    }

    /** Used by the scheduler when a "once" alarm has nothing left to ring. */
    fun disableQuietly(id: Long) {
        val list = _alarms.value.map { if (it.id == id) it.copy(enabled = false) else it }
        _alarms.value = list
        persist(list)
    }

    private fun withOneOffDate(alarm: Alarm): Alarm =
        if (alarm.days == 0 && alarm.enabled) {
            val first = alarm.times.firstOrNull() ?: 0
            val keep = alarm.oneOffDate != null && alarm.skipDate == null &&
                alarm.oneOffDate.atStartOfDay().plusMinutes(first.toLong()).isAfter(LocalDateTime.now())
            if (keep) alarm else alarm.copy(oneOffDate = Alarm.oneOffDateFor(first, LocalDateTime.now()), skipDate = null)
        } else alarm

    private fun commit(list: List<Alarm>) {
        _alarms.value = list
        persist(list)
        AlarmScheduler.scheduleAll(app)
    }

    // ---- saving as JSON ----

    private fun persist(list: List<Alarm>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    private fun load(): List<Alarm> {
        val raw = prefs.getString("list", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i -> arr.getJSONObject(i).toAlarm() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun Alarm.toJson() = JSONObject().apply {
        put("id", id)
        put("label", label)
        put("kind", kind.name)
        put("days", days)
        put("times", JSONArray(times))
        put("enabled", enabled)
        put("skipDate", skipDate?.toString())
        put("oneOffDate", oneOffDate?.toString())
        put("start", start)
        put("interval", interval)
        put("count", count)
        put("end", end)
    }

    private fun JSONObject.toAlarm(): Alarm {
        val t = getJSONArray("times")
        return Alarm(
            id = getLong("id"),
            label = optString("label", ""),
            kind = AlarmKind.valueOf(getString("kind")),
            days = getInt("days"),
            times = List(t.length()) { t.getInt(it) },
            enabled = getBoolean("enabled"),
            skipDate = optStringOrNull("skipDate")?.let(LocalDate::parse),
            oneOffDate = optStringOrNull("oneOffDate")?.let(LocalDate::parse),
            start = getInt("start"),
            interval = getInt("interval"),
            count = getInt("count"),
            end = getInt("end"),
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else getString(key)
}
