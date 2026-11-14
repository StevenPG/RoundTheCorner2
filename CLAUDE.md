# CLAUDE.md: context for working on RoundTheCorner2

This file is the hand-off for a new session. It records what has been built, the
decisions made and why, the conventions used, and what comes next. Read it before you
change anything. **README.md** is the product and architecture overview;
**docs/LOCAL_DEVELOPMENT.md** covers Android Studio and emulator setup.

## About the owner

- **Background:** a senior software engineer who works mostly in Java, likes Python, is
  interested in Go, knows DevOps well, and works in aerospace. When a Kotlin or Android
  idea is new, compare it to the Java or Spring equivalent (for example, Hilt is like
  Spring DI, and a version catalog is like a Maven BOM).
- **Standards:** the code should be modern, well separated into modules, and well
  commented. The UI should look polished and professional.

## What the app does

When a trigger is armed, a background service watches the device's location. When you
come within, or go beyond, a configured distance of a saved place, it sends a message.
- **Channels:** SMS, a Slack webhook or a Discord webhook.
- **Live notification:** while watching, it shows how far you are from the place and how
  far is left until the message sends.

## Milestone status

| # | Milestone | State |
|---|---|---|
| 1 | Skeleton, theme, navigation, Settings, CI | ✅ |
| 2 | Domain model, Room, distance and hysteresis logic | ✅ |
| 3 | Places: coordinates, Nominatim search, MapLibre picker | ✅ |
| 4 | Recipients and senders (SMS per flavor, Slack, Discord), encrypted secrets | ✅ |
| 5 | Trigger list and editor | ✅ |
| 6 | Foreground monitoring service, live notification, retries, restart after reboot | ✅ |
| 7 | **UI review and polish** | 🟡 Local dev guide done; the rest is the backlog below |
| 8 | Device-testing guide | 🟡 Mostly covered by docs/LOCAL_DEVELOPMENT.md |

**Nothing has been run on a real device or emulator yet.** All work so far happened in a
cloud container, verified by JVM unit tests, Robolectric tests and Roborazzi screenshots.
The first local run is the most important next step. Use the checklist under
"Untested on device".

## Milestone 7 backlog (agreed with the owner, in suggested order)

1. **First run on a device or emulator.** Fix anything that breaks: permissions, the
   service starting, the notification, the MapLibre maps, the contact picker.
2. **First-run onboarding.** Two or three pages: what the app does, then the permissions
   (precise location, then notifications, then optionally "all the time"). Store an
   `onboardingComplete` flag in DataStore through `UserPreferences`, and show onboarding
   before `RoundTheCornerApp` while the flag is unset. Reuse `rememberRequirementFixer` from
   `feature/triggers/.../list/RequirementLaunchers.kt`, or move it into a shared module.
3. **"Use my location" in the place editor.** Add a button that fills the coordinates from
   a single fix: `LocationClient.updates(...).first()` with a timeout, after requesting
   permission if needed. This adds a dependency from `feature/places` on `core:location`.
4. **Tablet and wide-screen layout.** Content currently stretches edge to edge on tablets
   (see `app/src/test/screenshots/app_shell_tablet_*.png`). Limit list and editor content
   to about 640–720 dp wide and centre it. List/detail layouts are optional.
5. **Bundled brand font.** The app currently uses the system sans-serif. Candidates are
   **Manrope** or **Plus Jakarta Sans** (OFL), downloadable from
   `raw.githubusercontent.com/google/fonts/main/ofl/...`. Put the font in
   `core/designsystem/src/main/res/font/` and wire it into `theme/Type.kt` only.
6. **Accessibility pass.** Merge semantics on cards, check touch targets of 48 dp or
   more, check contrast, and test with TalkBack and large font sizes.
7. **Visual review of every screen in light and dark,** then re-record the screenshots.
8. **Smaller ideas:** use the latest send attempt to show "Sent" or "Failed" in the
   trigger list; add a "Send test notification" item to Settings.

## Module map and rules

```
app                  MainActivity, Application (Hilt + WorkManager), navigation, flavor SmsModule
feature/triggers     list (watch switch, live distance, permission banner) + editor
feature/places       list + editor (coordinates / Nominatim / centre-pin map)
feature/recipients   list + editor (channel, contact picker, test send)
feature/settings     units, check interval, theme
core/monitoring      MonitoringService, TriggerFirer, SendRetryWorker, notifications, BootReceiver
core/location        LocationClient (Fused, or platform LocationManager without Play services)
core/messaging       MessageSender implementations + MessageDispatcher (Hilt map multibinding)
core/maps            MapLibre wrappers: LocationPickerMap, RadiusPreviewMap
core/network         OkHttp + NominatimClient (1 request/s, identifying User-Agent)
core/data            repositories (Room, DataStore), SecretCipher, UnitSystemProvider
core/database        Room entities, DAOs, exported schemas (core/database/schemas)
core/domain          pure Kotlin: Geodesy, ProximityEvaluator, MonitoringEngine, PollingPolicy, formatting
core/model           pure Kotlin domain types, CoordinateParser
core/designsystem    theme, tokens, shared composables (no domain types)
core/ui              composables that use domain types (place icons, channel badges)
core/testing         fakes, MainDispatcherRule, screenshot helpers (test-only)
build-logic          convention plugins: rtc.android.{application,library,compose,feature}, rtc.hilt, rtc.jvm.library
```

- Dependencies only point downward. **Feature modules never depend on each other.** The
  app module wires links between features, for example `onCreatePlace` passed into
  `triggersEntries`.
- `core:model` and `core:domain` must stay free of Android, so they're testable on a
  plain JVM.

## Key decisions (don't silently reverse these)

- **Package and flavors.** The package is `com.stevenpg.roundthecorner2`, with a
  `distribution` flavor dimension:
  - **`full`** (sideload): declares `SEND_SMS` only in `app/src/full/AndroidManifest.xml`
    and sends SMS automatically through `AutomaticSmsSender`.
  - **`play`**: has no SMS permission. `ComposeSmsSender` posts a tap-to-send
    notification, or opens the composer directly when the user is in the app.
  - **How the flavor is picked:** each flavor has its own `di/SmsModule.kt` that binds the
    SMS sender and `SmsMode`. Nothing else in the code checks the flavor.
- **Maps and search** use free services with no API keys: OpenStreetMap data,
  **OpenFreeMap** tiles (`liberty` and `dark` styles) through MapLibre, and **Nominatim**
  for address search.
- **Manual lat/lng is the primary input.** The owner insisted an address lookup must
  never be required. `CoordinateParser` accepts decimal, hemisphere-letter and
  degrees-minutes-seconds formats.
- **Saved Places and Recipients** are reusable, and triggers reference them by ID.
  Deleting one that a trigger still uses is blocked with `DeleteResult.InUse`, never
  cascaded.
- **Units:** the setting is Automatic (from the locale), Metric or Imperial. US and UK
  locales resolve to miles. Everything is stored in meters.
- **Triggers fire once.** After firing, the trigger is FIRED; the user re-arms it with the
  switch.
- **Edge-triggered with hysteresis** (`ProximityEvaluator`):
  - **Where it fires:** exactly at the radius, with a dead band of 10% of the radius
    (15–200 m) on the starting side.
  - **Confirmation:** two consecutive fixes must agree before it fires.
  - **Accuracy gate:** fixes less accurate than half the radius (clamped to 50–500 m) are
    ignored.
  - **Already past the line:** a trigger armed while already past the line reports
    `AlreadySatisfied`; the service then posts a notification with **Send now** and
    **Keep waiting**.
- **At most once.** `TriggerFirer` changes the trigger from ARMED to FIRED with a single
  conditional SQL update (`markFiredIfArmed`) *before* sending. Transient failures are
  retried by `SendRetryWorker` (WorkManager, exponential backoff, 5 attempts), which resends
  the originally rendered text. An SMS whose confirmation times out is never retried, to
  avoid duplicate texts.
- **Secrets.** Recipient addresses (phone numbers and webhook URLs) are stored encrypted
  with AES-256-GCM, using a key in the Android Keystore, with the `enc:v1:` prefix. The
  database is backed up but the key isn't, so a restored recipient shows `needsReentry`.
- **Notification:**
  - Android 16 (API 36): `ProgressStyle`.
  - Android 16 QPR2 (API 36.1): also `setRequestPromotedOngoing` and
    `setShortCriticalText`, a Live Update with a status-bar chip. These calls **must** stay
    behind `Build.VERSION.SDK_INT_FULL >= VERSION_CODES_FULL.BAKLAVA_1`, because on 36.0
    they throw `NoSuchMethodError`.
  - Older versions use the compat progress bar.
- **Polling** adapts to how close the nearest trigger is: from 10 s up to the user's
  setting of 30 s–5 min. The interval is snapped to fixed steps (`IntervalBuckets`) so the
  location request isn't re-registered constantly.
- **No `READ_CONTACTS`.** The contact picker uses `ACTION_PICK` on `Phone.CONTENT_URI`,
  which grants access to only the picked row.
- **Battery:** the app opens `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`. It doesn't use
  the Play-restricted `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- **Nominatim:** results show the attribution. Keep the 1 request/s limit and the debounce.

## Conventions

- **Screens.**
  - Each screen has a stateful `XxxRoute` (Hilt ViewModel, permissions, snackbars) that
    wraps a stateless `XxxScreen(uiState, onAction, …)`. Previews and screenshots use the
    stateless screen.
  - Editors take a single `onAction(XxxAction)`, using a sealed interface for actions. One-off
    results come through a `Channel` exposed with `receiveAsFlow()`.
  - Editors that need an ID use Hilt assisted injection, via
    `hiltViewModel<VM, VM.Factory>(creationCallback = { it.create(id) })`.
- **MapLibre can't render on the JVM.** Screens that contain a map take a slot
  (`MapContent` or `RadiusMapContent`), and screenshot tests pass a stand-in.
- **Strings** go in each module's `res/values/strings.xml`. Webhook URLs must never appear
  in logs or error messages.
- **Comments** explain *why*, and where it helps, the Java or Spring equivalent.
- **ViewModel tests.**
  - Use `MainDispatcherRule`, and create the ViewModel **lazily** (`by lazy`) so it's built
    *after* the rule has installed the test Main dispatcher. Building it eagerly caused
    flaky races.
  - Use `runTest(main.testDispatcher)`.
- **Screenshot tests.**
  - `composeRule.captureLightAndDark("name") { … }` from `core:testing` writes to
    `<module>/src/test/screenshots/`, and those PNGs are committed baselines.
  - Name test functions so they don't shadow stdlib calls (`emptyList()` once did), and
    give them an explicit `: Unit` return type.
  - The README's Screenshots section links to six of these PNGs by path. If you rename
    a capture, update the README too.
- **`AppShellScreenshotTest`** is a Hilt integration test that runs the real `MainActivity`
  and the real dependency graph. It waits for "No triggers yet" before capturing.
- **Commits.** End each commit message with the co-author and session lines used in
  previous commits. Keep model names out of commit messages and code.

## Commands

Build both flavors:

```bash
./gradlew assemblePlayDebug assembleFullDebug
```

Run every test as CI does (warnings are errors when the CI variable is set):

```bash
CI=true ./gradlew :core:model:test :core:domain:test testDebugUnitTest :app:testPlayDebugUnitTest :app:testFullDebugUnitTest -Proborazzi.test.verify=true
```

Re-record screenshots after an intentional UI change:

```bash
./gradlew testDebugUnitTest :app:testPlayDebugUnitTest -Proborazzi.test.record=true
```

Release builds (one APK per ABI for sideloading, an AAB for Play):

```bash
./gradlew assembleFullRelease bundlePlayRelease
```

Publishing: pushing a `vX.Y.Z` tag runs `.github/workflows/release.yml`, which builds signed
`full` APKs and attaches them to a GitHub Release (see README "Releasing"). The version
comes from the tag (`-Prtc.versionName`); signing comes from the `RTC_*` secrets created by
`tools/setup_release_signing.sh`. Never fall back to debug signing for published APKs.

## Gotchas learned the hard way

- **AGP 9 compiles Kotlin itself.** Don't apply `org.jetbrains.kotlin.android`.
- **Robolectric** needs `--add-exports java.base/jdk.internal.access` and
  `--add-opens java.base/java.io`. They're already set in `build-logic/.../KotlinAndroid.kt`.
- **Robolectric can't run `SmsManager.divideMessage`,** which is why SMS goes through
  `SmsTransport`. On real devices without telephony (Wi-Fi tablets), `SmsManager` is null
  or throws; the sender reports that as `FailureReason.UNSUPPORTED`.
- **ABI splits are turned off for `bundle*` tasks,** because AGP refuses to build both.
- **`git rm` of the last file in a directory deletes the directory.** Run `mkdir -p` before
  writing new files there.
- **The cloud container used a Gradle init script** to point Maven Central at a mirror,
  because Central was rate-limiting it. Your machine doesn't need it; it isn't in the repo.

## Untested on device: check these on the first local run

- [ ] App launches; splash appears, then the Triggers tab. Theme and dark mode follow
      Settings.
- [ ] Place editor: the OpenFreeMap tiles load, dragging the map updates the coordinates,
      typed coordinates move the map, and Nominatim search returns results.
- [ ] Trigger editor: the radius circle draws and fits the view.
- [ ] Recipients: the contact picker fills the number. **Send test message** works for
      Slack, Discord, SMS in the `full` flavor, and the composer in the `play` flavor.
- [ ] Permission banner flow: precise location, notifications, "all the time" (opens system
      Settings), battery.
- [ ] Arming a trigger starts the service. The notification updates with distance and
      progress; on API 36.1 there's a Live Update chip. **Send now**, **Pause** and
      **Stop all** work.
- [ ] An emulator GPX route (`tools/make_route_gpx.py`) crosses the radius: the message
      sends once, the trigger shows "Sent", and the history records it.
- [ ] Arming while already inside shows the "Already near" notification; **Send now** and
      **Keep waiting** behave correctly.
- [ ] With airplane mode on during a webhook send: a retry is scheduled and later succeeds.
- [ ] After a reboot, with "all the time" granted, monitoring resumes.
- [ ] `play` APK: no `SEND_SMS` permission (check with `aapt2 dump permissions`).
