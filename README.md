# RoundTheCorner2

This is the second iteration of RoundTheCorner, an Android app that runs a background
service and sends a message when you get within (or move beyond) a chosen distance of a
place. The message can go out as an SMS, a Slack message or a Discord message.

## Status

| # | Milestone | State |
|---|-----------|-------|
| 1 | Gradle skeleton, modules, Hilt, theme, navigation shell, Settings (units + theme), CI | ✅ |
| 2 | Domain model, Room, distance + hysteresis logic | ✅ |
| 3 | Places: manual lat/lng entry, address search (Nominatim), map picker (MapLibre/OSM), saved places | ⏳ |
| 4 | Recipients + senders: SMS (per flavor), Slack webhook, Discord webhook | ⏳ |
| 5 | Trigger list and editor | ⏳ |
| 6 | Foreground monitoring service + live progress notification | ⏳ |
| 7 | UI review loop | ⏳ |
| 8 | Device-testing guide | ⏳ |

## Tech stack

- **Kotlin 2.4** and **Coroutines/Flow**
- **Jetpack Compose** with **Material 3**. A brand palette is the default; wallpaper
  ("dynamic") colors can be turned on in Settings.
- **Navigation 3**. Each tab keeps its own back stack, which the app holds as a plain list.
  The navigation chrome adapts to screen size: a bottom bar on phones, a rail on tablets.
- **Hilt** for dependency injection (compile-time, KSP)
- **Room** for places, recipients, triggers and send history. **DataStore** for settings.
- **AGP 9.5** (built-in Kotlin), **Gradle 9.9**, version catalog, convention plugins
- **Robolectric + Roborazzi** for JVM screenshot tests (no emulator needed)
- `minSdk` 29 (Android 10), `targetSdk`/`compileSdk` 37

## Module layout

```
app/                  Entry point: Activity, DI root, navigation wiring
build-logic/          Gradle convention plugins (rtc.android.feature, rtc.hilt, ...)
core/model            Pure-Kotlin domain types and coordinate parsing (no Android imports)
core/domain           Pure-Kotlin logic: WGS84 distance, trigger state machine, polling, templates
core/database         Room entities, DAOs, exported schemas (core/database/schemas)
core/data             Repositories, the only data API that features use
core/designsystem     Theme, colors, typography, shared composables
core/testing          Fakes + screenshot helpers (test-only)
feature/triggers      Triggers tab
feature/places        Saved places tab
feature/settings      Settings tab (distance units, theme)
```

Dependencies only point downward: `app` → `feature:*` → `core:*`. Feature modules never
depend on each other.

## Build flavors

| Flavor | Application ID | SMS behaviour |
|--------|----------------|---------------|
| `play` | `com.stevenpg.roundthecorner2` | Google Play doesn't allow `SEND_SMS` for apps that aren't the default SMS app, so the message composer opens pre-filled and the user taps send. |
| `full` | `com.stevenpg.roundthecorner2.full` | Sideloaded build. Sends SMS automatically in the background. |

Slack and Discord webhooks send automatically in both flavors. Debug builds add `.debug`
to the application ID, so debug and release can be installed side by side.

## Common commands

```bash
./gradlew assemblePlayDebug assembleFullDebug        # build APKs
./gradlew installFullDebug                           # install on a connected device
./gradlew :core:model:test :core:domain:test testDebugUnitTest :app:testPlayDebugUnitTest   # unit tests
```

Screenshot tests write PNGs to `*/src/test/screenshots/`. To update the baselines, run:

```bash
./gradlew testDebugUnitTest :app:testPlayDebugUnitTest -Proborazzi.test.record=true
```

To check the current UI against the baselines (CI runs this), run:

```bash
./gradlew testDebugUnitTest :app:testPlayDebugUnitTest -Proborazzi.test.verify=true
```

## How a trigger decides to send

All of this lives in `core:domain` (`ProximityEvaluator`) and runs as plain JVM unit tests.

- **Distance:** Vincenty's formula on the WGS84 ellipsoid (the datum GPS uses), accurate
  to under a millimetre. Nearly antipodal points fall back to haversine.
- **Edge-triggered:** a trigger fires only when you cross the boundary in its direction.
  *Arrive* means outside → inside; *Leave* means inside → outside. If you're already on
  the "fire" side when you arm it, it reports `AlreadySatisfied` and waits for a real
  crossing.
- **Hysteresis:** it fires exactly at your radius. Before it counts you as having started
  on the other side, you have to be clearly past the boundary by a dead band of 10% of
  the radius, clamped to 15–200 m. GPS wobble near the line can't make it fire twice.
- **Confirmation:** a change of side needs 2 fixes in a row that agree, so a single wild
  fix (for example, signal bouncing off buildings) can't send a message.
- **Accuracy gate:** fixes less accurate than half the radius are ignored. The limit is
  never stricter than 50 m and never looser than 500 m.
- **At most once:** `markFired` is a compare-and-set in SQL
  (`UPDATE … WHERE status = 'ARMED'`), so two location updates racing can't both send.
- **Adaptive polling:** the next check comes after half the time it would take to reach
  the boundary at 35 m/s (about 78 mph), clamped to 10–60 s. Checks are rare when you're
  far away and frequent when you're close.

Message placeholders: `{place}`, `{distance}`, `{radius}`, `{time}`.

## Places and coordinates

A place needs only coordinates; an address is optional. `CoordinateParser` accepts
whatever you paste from a map app:

```
40.7128, -74.0060            40.7128° N, 74.0060° W
40°42'46.1"N 74°00'21.6"W    40°42.768'N 74°0.36'W
```

A place or recipient that a trigger still uses can't be deleted. The repository returns
`DeleteResult.InUse(n)` instead of quietly deleting the triggers too.

## Settings

- **Distance units:** *Automatic* (the default), *Metric* or *Imperial*. Automatic reads
  the device locale's CLDR measurement system. The US and UK both resolve to miles, since
  UK road distances are in miles. Distances are always stored in meters; the setting only
  changes how they are displayed and entered.
- **Theme:** System, Light or Dark. On Android 12+ there's also an optional wallpaper-color
  toggle.
