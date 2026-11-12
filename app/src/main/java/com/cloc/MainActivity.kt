package com.cloc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.cloc.data.AppEvents
import com.cloc.ring.RingActivity
import com.cloc.ring.RingService
import com.cloc.ui.alarm.AlarmEditor
import com.cloc.ui.alarm.AlarmEditorState
import com.cloc.ui.alarm.AlarmScreen
import com.cloc.ui.clock.ClockScreen
import com.cloc.ui.components.BottomNav
import com.cloc.ui.components.Tab
import com.cloc.ui.stopwatch.StopwatchScreen
import com.cloc.ui.theme.ND
import com.cloc.ui.theme.NothingTheme
import com.cloc.ui.theme.ndTransition
import com.cloc.ui.timer.TimerScreen

class MainActivity : ComponentActivity() {

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        AppEvents.resumed.value++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        // Alarms need notifications to show over the lock screen, so ask once on first launch.
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            NothingTheme {
                ClocApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppEvents.resumed.value++
    }
}

/** The whole app: the current tab's screen on top, the bottom bar underneath, the alarm editor over both. */
@Composable
fun ClocApp() {
    var tab by rememberSaveable { mutableStateOf(Tab.Clock) }
    val editing by AlarmEditorState.editing
    val ringing by RingService.current.collectAsState()
    val context = LocalContext.current

    // If something starts ringing while the app is open, show the ring screen straight away.
    LaunchedEffect(ringing) {
        if (ringing != null) {
            context.startActivity(Intent(context, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    Box(Modifier.fillMaxSize().background(ND.colors.black)) {
        Column(Modifier.fillMaxSize()) {
            Crossfade(
                targetState = tab,
                animationSpec = ndTransition(),
                label = "tabs",
                modifier = Modifier.weight(1f),
            ) { current ->
                Box(Modifier.fillMaxSize()) {
                    when (current) {
                        Tab.Clock -> ClockScreen()
                        Tab.Alarm -> AlarmScreen()
                        Tab.Timer -> TimerScreen()
                        Tab.Stopwatch -> StopwatchScreen()
                    }
                }
            }
            BottomNav(selected = tab, onSelect = { tab = it })
        }

        AnimatedVisibility(editing != null, enter = fadeIn(ndTransition()), exit = fadeOut(ndTransition())) {
            // Keep showing the last request while fading out.
            var shown by androidx.compose.runtime.remember { mutableStateOf(editing) }
            if (editing != null) shown = editing
            shown?.let { req ->
                // key(): a different request gets a fresh editor instead of reusing the old one's state
                androidx.compose.runtime.key(req) { AlarmEditor(req.alarm, onClose = { AlarmEditorState.close() }) }
            }
        }
    }
}
