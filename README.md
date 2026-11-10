# RoundTheCorner2

This is the second iteration of RoundTheCorner, an Android app that runs a background
service and sends a message when you get within (or move beyond) a chosen distance of a
place. The message can go out as an SMS, a Slack message or a Discord message.

## Status

| # | Milestone | State |
|---|-----------|-------|
| 1 | Gradle skeleton, modules, Hilt, theme, navigation shell, Settings (units + theme), CI | ✅ |
| 2 | Domain model, Room, distance + hysteresis logic | ✅ |
| 3 | Places: manual lat/lng entry, address search (Nominatim), map picker (MapLibre/OSM), saved places | ✅ |
| 4 | Recipients + senders: SMS (per flavor), Slack webhook, Discord webhook | ✅ |
| 5 | Trigger list and editor | ✅ |
| 6 | Foreground monitoring service + live progress notification | ✅ |
| 7 | UI review loop | 🟡 in progress (see CLAUDE.md backlog) |
| 8 | Device-testing guide | ⏳ |

**Running it locally:** see [docs/LOCAL_DEVELOPMENT.md](docs/LOCAL_DEVELOPMENT.md) for Android Studio setup, build variants,
simulating a drive in the emulator, and `adb` commands for testing.

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
core/network          OkHttp client and the Nominatim geocoding client
core/location         Device location: Fused provider, with a platform LocationManager fallback
core/monitoring       Foreground service, live notification, firing, retries, boot restart
core/messaging        Message senders (Slack/Discord webhooks, SMS) + MessageDispatcher
core/maps             MapLibre map wrapped for Compose (OpenFreeMap tiles)
core/designsystem     Theme, colors, typography, shared composables (no domain types)
core/ui               Shared composables that use domain types (place icons, ...)
core/testing          Fakes + screenshot helpers (test-only)
feature/triggers      Triggers: list (watch/pause switch) + editor (radius map, message preview)
feature/places        Saved places: list + editor (coordinates, search, map)
feature/recipients    Saved recipients: list + editor with contact picker and test send
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

`AppShellScreenshotTest` is a Hilt integration test. It starts the real `MainActivity`
with the real dependency graph (Room, DataStore and repositories) on a fresh install, so a
mistake in dependency wiring fails CI.

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

A place needs only a name and coordinates; an address is optional. The editor can get the
location in three ways, and each one keeps the others in sync:

1. **Coordinates field.** Works offline, and it's the only method that needs no network.
   `CoordinateParser` accepts whatever you paste from a map app (examples below). The text
   under the field shows how the input was read, e.g. `39.638611° N, 105.239167° W`.
2. **Address search** with OpenStreetMap Nominatim. Searches wait for a 600 ms pause in
   typing and are limited to one request per second, as Nominatim's usage policy requires.
   Choosing a result fills in the coordinates and the address, and suggests a name.
3. **Map.** The pin stays fixed at the center and you drag the map underneath it. A small
   adjustment (under 150 m) keeps the address from the search; moving the pin further
   clears it.

Coordinate formats `CoordinateParser` accepts:

```
40.7128, -74.0060            40.7128° N, 74.0060° W
40°42'46.1"N 74°00'21.6"W    40°42.768'N 74°0.36'W
```

A place or recipient that a trigger still uses can't be deleted. The repository returns
`DeleteResult.InUse(n)` instead of quietly deleting the triggers too.

## Triggers

- **List:** each trigger has a switch. On means **watching**, which also re-arms a
  trigger that has already sent; off means **paused**. Triggers that are watching sort
  first and are visually emphasised. A trigger that has sent shows when it sent. A
  recipient that needs re-entering is flagged on every trigger that uses it.
- **Editor:**
  - **When:** Arrive or Leave, with a short explanation of each.
  - **Place:** a dropdown of saved places, plus *New place…*.
  - **Distance:** entered in mi or km depending on your unit setting, with quick-pick
    chips and limits of 25 m to 1,000 km.
  - **Map:** a live MapLibre view of the radius around the place.
  - **Recipient:** a dropdown, plus *New recipient…*.
  - **Message:** placeholder chips insert `{place}`, `{distance}`, `{time}` and
    `{radius}` at the cursor, and a chat-bubble preview shows the exact text that will
    be sent.
  - **History:** previously edited triggers show their send history.
- A place or recipient created from inside the editor is selected automatically when you
  come back. The app module wires those links, so feature modules never depend on each
  other.
- A distance you entered is always shown exactly as typed ("0.25 mi"). Measured distances
  are rounded ("1.8 mi").

## Monitoring (the background part)

```
Room: ARMED triggers ─┐
                      ├─▶ MonitoringEngine ──▶ fire ─▶ TriggerFirer ─▶ MessageDispatcher
Location updates ─────┘     (pure Kotlin)    ├─▶ "already here?" notification
  ▲ adaptive interval                        └─▶ live status notification + list UI
  └──────────── next interval ◀──────────────┘
```

- **`MonitoringService`** is a foreground service of type `location`. It runs while at
  least one trigger is armed and stops itself when none are. `MonitoringController` starts
  it when triggers become armed, and `BootReceiver` restarts it after a reboot or an app
  update.
- **Adaptive checks.** The engine picks the next check interval from the nearest trigger:
  10 s when close, up to the *Check location at least every* setting (30 s to 5 min) when
  far away. The interval is snapped to a few fixed steps, so the location request is only
  re-registered when the step actually changes.
- **Sending at most once.** `TriggerFirer` changes the trigger from ARMED to FIRED in a
  single atomic step before sending, records the attempt in the history, and posts
  "Sent to Alex". Transient failures go to a WorkManager job (`SendRetryWorker`) that
  survives reboots and process death. It backs off exponentially, waits for network for
  webhooks, gives up after 5 attempts, and retries the text exactly as it was originally
  rendered.
- **"Already here."** If a trigger is armed while you're already past its line, it doesn't
  fire. You get a notification with **Send now** and **Keep waiting**.
- **Live notification.** It shows the trigger nearest to firing, as "1.8 mi to go · 3.8 mi
  away" with a progress bar, plus **Send now**, **Pause** and **Stop all** buttons.
  - On Android 16 it uses `ProgressStyle`.
  - From Android 16 QPR2 (API 36.1) it is also a promoted *Live Update* with a status-bar
    chip showing the distance left. These APIs are guarded at runtime with
    `SDK_INT_FULL`, because calling them on 36.0 crashes.
  - Older versions get a standard progress notification.
- **Trigger list.** Watching triggers show their live distance. A banner asks for the most
  important missing item, in this order:
  1. Precise location (required)
  2. Notifications (required)
  3. "Allow all the time" (recommended)
  4. Unrestricted battery (recommended)

  The two recommended items can be dismissed with "Not now".

### Permissions

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` | Radii can be as small as 25 m |
| `ACCESS_BACKGROUND_LOCATION` | Restarting after a reboot, or starting from the background. Optional |
| `FOREGROUND_SERVICE(_LOCATION)` | The monitoring service |
| `POST_NOTIFICATIONS`, `POST_PROMOTED_NOTIFICATIONS` | Live status, alerts and Android 16 Live Updates |
| `RECEIVE_BOOT_COMPLETED` | Resuming after a restart |
| `SEND_SMS` (`full` only) | Sending texts automatically |

**Play submission notes:**
- **Background location:** this needs Google Play's background-location declaration,
  including a short video of the feature.
- **Battery settings:** the app opens the battery-optimisation *list* screen instead of
  calling `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, which Play restricts.
- **Aggressive OEMs:** some manufacturers (Samsung, Xiaomi, OnePlus and others) still kill
  background services. See [dontkillmyapp.com](https://dontkillmyapp.com).

## Messaging

`MessageDispatcher` looks up the `MessageSender` for each channel. The senders are
registered through Hilt map multibinding, which works much like collecting every bean of a
type in Spring. The webhook senders are the same in every build. The SMS sender comes from
`app/src/<flavor>/kotlin/.../di/SmsModule.kt`, so no other code checks which flavor it's in.

| Channel | How it's sent | Safety |
|---|---|---|
| Slack | `POST {"text": ...}` to an Incoming Webhook | `<`, `>` and `&` are escaped, so a message can't trigger `<!channel>`-style pings |
| Discord | `POST {"content": ...}` to a channel webhook | `allowed_mentions` is empty (no `@everyone`); text is cut at 2,000 characters |
| SMS (`full`) | `SmsManager`, then waits for the carrier to confirm every part | Fails cleanly when there's no permission, no service or no telephony; a timeout is never retried, to avoid duplicate texts |
| SMS (`play`) | A notification opens the messaging app with the text filled in | Needs no `SEND_SMS` permission. In-app tests open the messaging app directly |

Every failure has a `FailureReason` that says whether retrying could help (network, 429 or
5xx) or the user has to fix something (revoked webhook, permission denied). Messages and
logs never include webhook URLs.

**Security.** Recipient phone numbers and webhook URLs are encrypted with AES-256-GCM using
a non-exportable Android Keystore key (`enc:v1:` in the database). Places, triggers and
settings are included in backups. Keystore keys never leave the device, so a recipient
restored onto a new phone shows **"Details need re-entering"** rather than crashing or
leaking the secret.

**Permissions.** `SEND_SMS` is only in `app/src/full/AndroidManifest.xml`. The contact
picker uses `ACTION_PICK` on phone numbers, which grants access to the one picked row, so
the app never asks for `READ_CONTACTS`. MapLibre's library manifest also adds location
permissions, which the monitoring milestone needs anyway.

## Third-party services

| Service | Used for | Key needed | Notes |
|---|---|---|---|
| [OpenFreeMap](https://openfreemap.org) | Map tiles (`liberty` and `dark` styles) | No | OpenStreetMap data; MapLibre shows the attribution |
| [Nominatim](https://nominatim.org) | Address search | No | One request per second, and the app sends an identifying User-Agent |

## APK size

MapLibre includes native code for each CPU architecture, about 12 MB per architecture.
Sideloaded builds therefore produce one APK per architecture: `arm64-v8a` for modern
phones, `armeabi-v7a`, and `x86_64` for emulators. For example,
`app/build/outputs/apk/full/release/app-full-arm64-v8a-release.apk`. Google Play builds use
`./gradlew bundlePlayRelease`, and Play delivers each device only its own architecture.

## Settings

- **Distance units:** *Automatic* (the default), *Metric* or *Imperial*. Automatic reads
  the device locale's CLDR measurement system. The US and UK both resolve to miles, since
  UK road distances are in miles. Distances are always stored in meters; the setting only
  changes how they are displayed and entered.
- **Theme:** System, Light or Dark. On Android 12+ there's also an optional wallpaper-color
  toggle.
