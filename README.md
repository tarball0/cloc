# CLOC

A minimal clock app for Android in the Nothing design language: OLED black, dot-matrix numbers, and red only where it matters.

<p>
  <img src="docs/screenshots/clock.png" width="200" alt="Clock">
  <img src="docs/screenshots/alarms.png" width="200" alt="Alarms">
  <img src="docs/screenshots/timers.png" width="200" alt="Timers">
  <img src="docs/screenshots/stopwatch.png" width="200" alt="Stopwatch">
</p>

## Features

### Clock
A clean analog dial (a thin ring, white hands, a red seconds hand) with the digital time and date underneath. The hands glide rather than tick.

### Alarms
- **Single alarms** that repeat on chosen days or ring once. They have a **SNOOZE** button.
- **Batch alarms**: a group of alarms that you switch on and off together. Three kinds:
  - **CUSTOM**: build it one alarm at a time. Pick a time on the dial and tap the red **+**, or use the `+1 +2 +5 +10 +20 +30` bubbles to add one that many minutes after the last.
  - **LINEAR**: a start time, an interval and a count.
  - **LOG**: a start time and a "must be up by" time. The gaps roughly halve each time, so the alarms get closer together as the deadline nears. Times snap to round multiples of 5 where they can (06:30 → 07:00, 07:15, 07:25, 07:28, 07:30).
- Batch alarms have no snooze. You get a big **OK** that stops the current ring, and a small **I'M UP** that skips the rest of that batch for today.
- **Per-alarm sounds**: each alarm can use any of the phone's alarm tones or your own audio file. New alarms follow your phone's default alarm sound, which CLOC never changes. "Change default" in the list takes you to the phone's sound settings.
- Time dials scroll or tap: tap the number above or below, or AM/PM, to jump to it.
- Only one alarm ever sounds at a time. If another goes off, the newest takes over.
- Alarms ring over the lock screen, and are rescheduled after a reboot or a time zone change.

<p>
  <img src="docs/screenshots/editor.png" width="200" alt="Batch alarm editor with sound picker">
  <img src="docs/screenshots/editor-log.png" width="200" alt="LOG batch preview with rounded times">
  <img src="docs/screenshots/sound-picker.png" width="200" alt="Alarm sound picker">
  <img src="docs/screenshots/ring.png" width="200" alt="Ringing batch alarm">
</p>

### Timers
- Run several timers at once. Each one has a segmented progress bar, play/pause and reset, and its own sound.
- Tap a timer to open the hourglass view. The red sand drains at a steady rate, and there's a large play/pause and a reset.

<p>
  <img src="docs/screenshots/sandtimer.png" width="200" alt="Hourglass timer">
</p>

### Stopwatch
An analog stopwatch:
- seconds round the edge;
- a 60-dot minutes sub-dial;
- a digital readout in the centre;
- laps with split and total times.

## Requirements
- Android 12 (API 31) or newer.
- **Notifications** and **Alarms & reminders** permissions. The app will ask for them.

## Install
Open the signed `CLOC-x.y.apk` on your phone, or build it yourself as shown below. You may need to allow installs from unknown sources.

## Build
You need JDK 21 and the Android SDK (API 37).

```sh
./gradlew assembleDebug        # debug APK  → app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests for the batch maths and scheduling
```

For a signed release build, create a `keystore.properties` in the project root. It is gitignored, so never commit it.

```properties
storeFile=/path/to/your-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then run `./gradlew assembleRelease`.

## Tech
- **Stack:** Kotlin and Jetpack Compose. Material 3 only does plumbing; every visible component is custom and drawn to the Nothing design rules.
- **Scheduling:** `AlarmManager.setAlarmClock` handles alarms, and a foreground service with a full-screen notification does the ringing.
- **Storage:** state is saved in SharedPreferences as JSON. There's no database.

## Licence
CLOC is free software. You can redistribute it and/or modify it under the terms of the [GNU General Public License v3.0](LICENSE).

These are third-party assets with their own licences:
- the Ndot, NType and Lettera Mono fonts, which belong to their respective owners;
- the reset icon, which is [Lucide](https://lucide.dev)'s `rotate-ccw` (ISC licence).

CLOC is not affiliated with Nothing Technology Limited.
