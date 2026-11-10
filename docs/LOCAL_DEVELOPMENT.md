# Running RoundTheCorner2 locally

## 1. Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Latest stable. The project uses **AGP 9.5** and **Gradle 9.9**. If sync says the Android Gradle Plugin is newer than your Studio supports, update Studio (*Help › Check for Updates*). |
| JDK | Nothing to install: Studio's bundled JetBrains Runtime (21) is used. On the command line, any JDK 17+ works. |
| Android SDK | **Platform 37** and **Build-Tools 37**. Studio offers to install them on first sync, or use *Settings › Languages & Frameworks › Android SDK*. |

## 2. Get the code

```bash
git clone https://github.com/StevenPG/RoundTheCorner2.git
cd RoundTheCorner2
git checkout claude/upbeat-wozniak-40hyg5
```

Then use *File › Open* and pick the `RoundTheCorner2` folder (the root, not `app/`), and let
Gradle sync. Studio creates `local.properties` with your SDK path; it's git-ignored.

## 3. Pick a build variant

Open *View › Tool Windows › Build Variants* and set the **app** module to one of these:

| Variant | Use it for | Installs as |
|---|---|---|
| `fullDebug` | Your own phone: texts send automatically | `com.stevenpg.roundthecorner2.full.debug` |
| `playDebug` | Checking the Play behaviour: texts open your messaging app pre-filled | `com.stevenpg.roundthecorner2.debug` |

The two variants have different application IDs, so you can install both side by side.

## 4. Run it

**On a phone:**
1. Turn on *Developer options* (tap *Build number* seven times), then turn on
   *USB debugging* or *Wireless debugging*.
2. Pick the phone in Studio's device menu and press **Run ▶**.

**On an emulator:** use *Device Manager › Create device* and pick a Pixel image with
**Google Play**:
- **API 36.1** (Android 16 QPR2) shows the Live Update status-bar chip.
- **API 36** shows the new progress-style notification.
- **API 34–35** shows the standard progress notification.

From the command line:

```bash
./gradlew installFullDebug
```

```bash
adb shell am start -n com.stevenpg.roundthecorner2.full.debug/com.stevenpg.roundthecorner2.MainActivity
```

## 5. Try a full trigger

1. **Recipients › Add recipient.**
   - **Slack:** in Slack, add the *Incoming Webhooks* app to a channel, then paste its URL.
   - **Discord:** use *Channel settings › Integrations › Webhooks › New webhook › Copy URL*.
   - **SMS:** pick a contact. The `full` variant asks for SMS permission.

   Tap **Send test message** to check the recipient before using it.
2. **Places › Add place.** Search an address, paste coordinates, or drag the map.
3. **Triggers › New trigger.** Choose Arrive, a distance of 0.5 mi, the recipient and a
   message. Save it.
4. Allow the permissions the banner asks for: location, notifications, and ideally
   "Allow all the time".
5. **Move.** Drive or walk, or simulate a route in the emulator (next section). Watch the
   notification count down, and the message sends when you cross the line.

## 6. Simulating movement in the emulator

Generate a GPX "drive" towards your place. `tools/make_route_gpx.py` uses only the
standard library:

```bash
python3 tools/make_route_gpx.py 39.7392 -104.9903 --from-km 3 --bearing 180 --kmh 50 > arrive.gpx
```

The route starts 3 km south of the target and drives north at 50 km/h. Add `--leave` to
drive away from the target instead.

In the emulator, open *Extended controls (⋯) › Location › Routes › Load GPX/KML*, then
press **Play route**. Raise the playback speed to compress a long drive.

To jump to a single point instead (emulator only; longitude comes first):

```bash
adb emu geo fix -104.9903 39.7392
```

## 7. Useful commands while testing

Follow the app's logs:

```bash
adb logcat --pid=$(adb shell pidof -s com.stevenpg.roundthecorner2.full.debug)
```

Check whether the service is running:

```bash
adb shell dumpsys activity services com.stevenpg.roundthecorner2.full.debug
```

Check the posted notifications:

```bash
adb shell dumpsys notification --noredact | grep -A3 roundthecorner2
```

Check the scheduled retries:

```bash
adb shell dumpsys jobscheduler | grep -A5 roundthecorner2
```

Test the restart after a reboot (requires "Allow all the time"):

```bash
adb reboot
```

Test a process kill. The service is START_STICKY and Android restarts it:

```bash
adb shell am kill com.stevenpg.roundthecorner2.full.debug
```

Run all tests, including screenshot verification, as CI does:

```bash
./gradlew :core:model:test :core:domain:test testDebugUnitTest :app:testPlayDebugUnitTest :app:testFullDebugUnitTest -Proborazzi.test.verify=true
```

## 8. Troubleshooting

| Symptom | Fix |
|---|---|
| Sync fails: "AGP version … not supported" | Update Android Studio. |
| `SDK location not found` | Open the project in Studio once, or create `local.properties` with `sdk.dir=/path/to/Android/sdk`. |
| Monitoring stops when the screen is off (Samsung, Xiaomi, OnePlus…) | Set the app's battery usage to *Unrestricted*. See [dontkillmyapp.com](https://dontkillmyapp.com) for your phone. |
| No status-bar chip on Android 16 | The chip needs 16 QPR2 (API 36.1) or later. 36.0 shows the progress bar without it. |
| Screenshot tests fail after an intentional UI change | Re-record with `-Proborazzi.test.record=true` and commit the PNGs. |
