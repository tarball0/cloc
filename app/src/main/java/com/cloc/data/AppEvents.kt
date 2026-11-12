package com.cloc.data

import kotlinx.coroutines.flow.MutableStateFlow

/** Small app-wide signals. */
object AppEvents {
    /** Goes up every time the main screen comes back to the front (e.g. after visiting Settings). */
    val resumed = MutableStateFlow(0)
}
