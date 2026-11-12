package com.cloc

import android.app.Application
import com.cloc.data.AlarmStore
import com.cloc.data.StopwatchStore
import com.cloc.data.TimerStore
import com.cloc.ring.RingService

/**
 * Runs once when the app process starts, before any screen or receiver.
 * Loads the saved alarms, timers and stopwatch so everything else can use them.
 */
class ClocApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmStore.init(this)
        TimerStore.init(this)
        StopwatchStore.init(this)
        RingService.createChannel(this)
    }
}
