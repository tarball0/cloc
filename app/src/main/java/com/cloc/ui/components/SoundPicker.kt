package com.cloc.ui.components

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cloc.R
import com.cloc.data.AppEvents
import com.cloc.ui.theme.ND
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Sound(val title: String, val uri: String?)

/**
 * Alarm-sound dropdown: a field showing the current sound; tap to open a list of the
 * phone's alarm tones, plus "Choose file…" and "Change default" (opens the phone's sound
 * settings). Tapping a tone selects it for this alarm only and plays a preview.
 *
 * [selected] is a content URI, or null to follow the phone's default alarm sound. Null is
 * shown as that sound's real name, and picking the phone's default tone stores null again.
 * The phone's own default setting is never changed here.
 */
@Composable
fun SoundPicker(
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    listMaxHeight: androidx.compose.ui.unit.Dp = 288.dp,
) {
    val context = LocalContext.current
    val c = ND.colors
    var open by remember { mutableStateOf(false) }
    val preview = remember { Preview(context) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }

    // The phone's alarm tones, loaded off the main thread.
    val phoneSounds by produceState(emptyList<Sound>()) {
        value = withContext(Dispatchers.IO) { loadAlarmSounds(context) }
    }
    // The phone's default alarm sound; re-read whenever the app comes back (e.g. from Settings).
    val resumed by AppEvents.resumed.collectAsState()
    val deviceDefault by produceState<Sound?>(null, resumed, phoneSounds) {
        value = withContext(Dispatchers.IO) { loadDeviceDefault(context, phoneSounds) }
    }
    val effective = selected ?: deviceDefault?.uri
    fun titleOf(uri: String?): String = when {
        uri == null -> ""
        else -> phoneSounds.firstOrNull { sameSound(it.uri, uri) }?.title
            ?: deviceDefault?.takeIf { sameSound(it.uri, uri) }?.title
            ?: fileName(context, uri)
    }
    val title = titleOf(effective)

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // Keep access after the app restarts, so the alarm can still play it.
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onSelect(uri.toString())
            preview.play(uri)
        }
    }

    val shape = RoundedCornerShape(8.dp)
    Column(modifier.fillMaxWidth()) {
        // The field
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .border(1.dp, if (open) c.textDisplay else c.borderVisible, shape)
                .ndClickable {
                    open = !open
                    if (!open) preview.stop()
                }
                .padding(horizontal = ND.space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = ND.type.body, color = c.textDisplay, maxLines = 1, modifier = Modifier.weight(1f))
            val turn by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
            val white = c.textDisplay
            Canvas(Modifier.size(12.dp).rotate(turn)) {
                val w = size.width
                val h = size.height
                val stroke = 1.5.dp.toPx()
                drawLine(white, Offset(0f, h * 0.3f), Offset(w / 2, h * 0.75f), stroke, StrokeCap.Round)
                drawLine(white, Offset(w / 2, h * 0.75f), Offset(w, h * 0.3f), stroke, StrokeCap.Round)
            }
        }

        // The list
        AnimatedVisibility(
            visible = open,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            val options = buildList {
                addAll(phoneSounds)
                // A default or chosen file that isn't a phone tone still gets a row, so it shows as selected.
                listOfNotNull(deviceDefault?.uri, selected).distinctBy { it.substringBefore('?') }.forEach { uri ->
                    if (none { sameSound(it.uri, uri) }) add(Sound(titleOf(uri), uri))
                }
            }
            Column(
                Modifier
                    .padding(top = ND.space.sm)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(c.surfaceRaised)
                    .border(1.dp, c.borderVisible, shape),
            ) {
                // Opens scrolled so the selected sound is in view.
                val scroll = rememberScrollState()
                val rowPx = with(LocalDensity.current) { 48.dp.roundToPx() }
                LaunchedEffect(Unit) {
                    val i = options.indexOfFirst { sameSound(it.uri, effective) }
                    if (i > 0) scroll.scrollTo((i - 1) * rowPx)
                }
                Column(Modifier.heightIn(max = listMaxHeight).verticalScroll(scroll)) {
                    options.forEach { sound ->
                        SoundRow(sound.title, isSelected = sameSound(sound.uri, effective)) {
                            // Picking the phone's default keeps this alarm following it (null).
                            val isDefault = sameSound(sound.uri, deviceDefault?.uri)
                            onSelect(if (isDefault) null else sound.uri)
                            sound.uri?.let { preview.play(Uri.parse(it)) }
                        }
                    }
                }
                NDDivider()
                SoundRow("Choose file…", isSelected = false) {
                    preview.stop()
                    pickFile.launch(arrayOf("audio/*"))
                }
                NDDivider()
                SoundRow("Change default", isSelected = false, icon = R.drawable.ic_settings) {
                    preview.stop()
                    // The phone's own sound settings, where the default alarm sound lives.
                    runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }
                }
            }
        }
    }
}

@Composable
private fun SoundRow(title: String, isSelected: Boolean, icon: Int? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).ndClickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Selected: a 2dp red bar on the left.
        Box(Modifier.width(2.dp).fillMaxHeight().background(if (isSelected) ND.colors.accent else ND.colors.surfaceRaised))
        Spacer(Modifier.width(ND.space.md - 2.dp))
        Text(title, style = ND.type.body, color = ND.colors.textDisplay, maxLines = 1, modifier = Modifier.weight(1f))
        if (icon != null) {
            Image(
                painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(ND.colors.textDisplay),
            )
            Spacer(Modifier.width(ND.space.md))
        }
    }
}

/** Plays one short preview at a time, at alarm volume. */
private class Preview(private val context: Context) {
    private var current: Ringtone? = null

    fun play(uri: Uri) {
        stop()
        current = RingtoneManager.getRingtone(context, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            runCatching { play() }
        }
    }

    fun stop() {
        current?.stop()
        current = null
    }
}

private fun loadAlarmSounds(context: Context): List<Sound> = runCatching {
    val rm = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
    val cursor = rm.cursor
    buildList {
        while (cursor.moveToNext()) {
            add(Sound(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX), rm.getRingtoneUri(cursor.position).toString()))
        }
    }
}.getOrDefault(emptyList())

/** Same sound even if one URI carries extra "?title=…&canonical=1" query bits. */
private fun sameSound(a: String?, b: String?): Boolean =
    a != null && b != null && a.substringBefore('?') == b.substringBefore('?')

/** The phone's current default alarm sound, with its real name. */
private fun loadDeviceDefault(context: Context, phoneSounds: List<Sound>): Sound? = runCatching {
    val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM) ?: return null
    val s = uri.toString()
    val title = phoneSounds.firstOrNull { sameSound(it.uri, s) }?.title
        ?: RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        ?: fileName(context, s)
    Sound(title, s)
}.getOrNull()

/** Display name of a chosen audio file, without its extension. */
private fun fileName(context: Context, uri: String): String = runCatching {
    context.contentResolver.query(Uri.parse(uri), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0)?.substringBeforeLast('.') else null
    }
}.getOrNull() ?: "Custom sound"
