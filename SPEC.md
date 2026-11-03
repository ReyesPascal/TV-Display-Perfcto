# Picture Helper — Specification

Status: planning complete 2026-11-03. Items marked **[validate]** still need checking on the TV or at build time.

## 1. Purpose and target device

The Hisense U6H only shows its HDR picture settings while HDR content is playing. Picture Helper is a personal,
sideloaded Google TV app that runs on the TV itself. It:

1. Plays genuine HDR10 setup content in a continuous loop.
2. Lets the owner open the TV's own picture menu while that content plays.
3. Walks through the RTINGS U6H recommendations one setting at a time.
4. Saves which settings the owner confirmed (Applied / Skipped / Not found).
5. Offers a separate SDR workflow.

This is guided picture setup, not instrument calibration. Version 1 does not read or change any TV setting.

**Target device** (read with adb on 2026-11-03, read-only):

| Property | Value |
|---|---|
| Model | Hisense 65U6H, `ro.product.model` "SmartTV 4K FFM", device `songshan` |
| OS | Android 11 (SDK 30), Google TV, build RTT2.220118.001, patch 2026-03-01 |
| CPU ABIs | `armeabi-v7a,armeabi` (32-bit userspace only) |
| UI display modes | 1920x1080 @ 60 Hz and 50 Hz only (no 24p mode reported) |
| Video plane | `vendor.display-size=3840x2160` |
| Reported HDR types | Dolby Vision, HDR10, HLG, HDR10+ (`[1,2,3,4]`), max luminance 500 |
| HEVC decoder | `OMX.MS.HEVC.Decoder`, up to 4096x2176, 3840x2160 @ 60 performance point, tunneled playback |
| Network adb | `persist.adb.tcp.port=5555` |

## 2. Confirmed requirements and assumptions

Confirmed by the owner:
- Runs directly on the TV's built-in Google TV; no HDMI streaming device.
- One installable APK for sideloading, covering 32- and 64-bit ARM.
- No backend, accounts, analytics, ads or subscriptions.
- The repo is public. The owner doesn't mind, so keys, IPs and RTINGS images must never be committed.
- Push to GitHub after each verified milestone.
- An APK of about 50–120 MB (bundled 4K HDR clip) is acceptable.

Assumptions **[validate]**:
- The Hisense picture menu opens as an overlay (RTINGS screenshots show a left-side panel over live video).
  Whether it keeps this app started and HDR active is unknown until M1.
- The remote button that opens the picture menu is unknown; find it during M1.
- HDR picture controls are offered for an app's video, not only for HDMI inputs.

## 3. Scope and exclusions

In scope (v1): HDR10 setup, SDR setup, RTINGS checklist with saved progress, diagnostics, About/attribution.

Excluded from v1:
- Dolby Vision, HDR10+, HLG (deferred until HDR10 is reliable).
- Gaming and PC settings (deferred).
- HDMI setup (HDMI Format "Enhanced" etc.): the app runs on the TV, not over HDMI.
- eARC, accessibility, Ambient Mode, remote/voice notes from the article.
- RTINGS' white-balance and colorspace calibration values: they're specific to RTINGS' own unit. This is not the
  same as the general "Color Space: Auto" recommendation, which is included.
- Reading or changing TV settings automatically; drawing over or controlling the native menu.

## 4. Screen flow and remote controls

```
Home ─┬─ SDR setup ──► Before playback ──► Playback + guide ──► Summary
      ├─ HDR10 setup ─► Before playback ──► Playback + guide ──► Summary
      ├─ Saved progress (per mode, reset with confirmation)
      ├─ Diagnostics
      └─ About and source attribution
```

M1 prototype only: Home with one "Play HDR setup video" button → full-screen loop.

Remote:
- D-pad moves focus; OK selects; Back goes up one level (Back on playback stops it and returns).
- During playback: OK shows/hides the guide panel; a "Hide all" action leaves a clean picture; any D-pad or OK
  press brings the guide back.
- The app never consumes Settings, Menu, Info, Home or other TV keys, so the native menus keep working.
- No touchscreen, mouse or keyboard needed. No special remote key needed for app navigation.

UI rules: large text (body ≥ 24 sp, titles ≥ 36 sp), high-contrast dark theme, clear focus ring + scale,
about 5% safe margin from every edge, landscape only, focus restored to the last item when returning to a screen.

Before playback: says which mode will play, that settings are changed by hand in the TV's own menu, and how to
open that menu (filled in once the remote button is confirmed). It also says the steps set the picture for
this app's playback, and other apps or inputs may need checking separately.

During playback: one checklist item at a time (setting name, recommended value, one-line reason), with
Applied / Skipped / Not found / Previous / Next. If the guide can't stay visible while the native menu is open,
the flow is: read one item, open the TV menu, change it, close the menu, mark it.

Summary: counts and lists of applied, skipped and not-found items. Reset asks for confirmation and states that it
clears the app's checklist only, not the TV's settings.

## 5. Minimal architecture

- Single `app` module, Kotlin, Compose + `androidx.tv:tv-material`, one Activity.
- `ui/`: Home, PrePlay, Playback (+ guide overlay), Progress, Diagnostics, About. Navigation is a small
  sealed-class back stack kept in a ViewModel (no navigation library).
- `playback/`: creates/releases ExoPlayer, loops the clip, exposes state and errors.
- `checklist/`: `RtingsChecklist.kt` (static data, section 6) and `ProgressStore` (DataStore Preferences;
  separate keys per mode).
- `diagnostics/`: display, decoder and stream info (section 7).
- Assets: clips in `app/src/main/assets/` or `res/raw/` with a LICENSE note beside them.

## 6. RTINGS checklist (summarized, with source mapping)

Source: [Hisense U6H Calibration Settings — RTINGS.com](https://www.rtings.com/tv/reviews/hisense/u6-u6h/settings).
Page shows last update 2023-10-24; checked 2026-11-03. RTINGS used a 65U6H and expects the settings to mostly
apply to the other sizes. They call these a starting point, not values to copy exactly. Text below is our own
summary; no RTINGS images are included. The app shows the source link and check date on About.

Kinds: **REC** recommended value in the text · **SHOWN** visible in an RTINGS settings screenshot but not
discussed in the text · **DEFAULT** left as it is · **AUTO** the TV changes it itself · **PREF** depends on room
or taste.

| ID | Mode | Setting | Recommended value / action | Kind | Article section | Note (our wording) |
|---|---|---|---|---|---|---|
| G1 | both | Energy Saver | Off is what RTINGS used | PREF | Other Settings › Power and Energy Saving | They turned it off only so the TV stayed on during tests; choose freely |
| G2 | both | Apply Picture Settings | Screenshot shows "All Sources" | SHOWN | Basic SDR (screenshot) | Shows where the TV applies changes; don't assume other apps/inputs match |
| S1 | SDR | Picture Mode | Theater Night | REC | Basic SDR | Most accurate preset with most adjustments; Theater Day is slightly brighter, less accurate |
| S2 | SDR | Backlight Level | 55, then set for your room | PREF | Basic SDR | 55 matched their dim test target; changing it doesn't hurt accuracy |
| S3 | SDR | Local Dimming | High | SHOWN | Basic SDR (screenshot) | Value seen on the Backlight page |
| S4 | SDR | Automatic Light Sensor | Off (Light Sensor Shift 0) | SHOWN | Basic SDR (screenshot) | Keeps brightness steady |
| S5 | SDR | Contrast | 39 | REC | Basic SDR | |
| S6 | SDR | Brightness | 50 | REC | Basic SDR | Not the same as Backlight; RTINGS suggests leaving it |
| S7 | SDR | Color | 50 | REC | Basic SDR | |
| S8 | SDR | Tint | 0 | REC | Basic SDR | |
| S9 | SDR | Sharpness | 0 | SHOWN | Basic SDR (screenshot) | |
| S10 | SDR | Picture Size | Wide | SHOWN | Basic SDR (screenshot) | |
| S11 | SDR | Color Temperature | Low | REC | Advanced SDR | Closest to the 6500K white target |
| S12 | SDR | Overscan | Off | SHOWN | Advanced SDR (screenshot) | |
| S13 | SDR | Noise Reduction | Off | REC | Advanced SDR | Can soften fine detail in good sources |
| S14 | SDR | Digital Noise Reduction | Off | REC | Advanced SDR | Same reason |
| S15 | SDR | Active Contrast | Off | REC | Advanced SDR | Same reason |
| S16 | SDR | Color Space | Auto | REC | Advanced SDR | Stops SDR looking oversaturated |
| H1 | HDR | Start HDR first | Open the picture menu only while HDR plays; look for the small HDR icon | REC | HDR | HDR settings appear only during HDR |
| H2 | HDR | Backlight Level | Check it went to Max | AUTO | HDR | The TV raises it by itself in HDR |
| H3 | HDR | Picture Mode | HDR Theater | REC | HDR | |
| H4 | HDR | Color Temperature | Low | REC | HDR | |
| H5 | HDR | Other settings | Leave as they are | DEFAULT | HDR | Text gives no other HDR values. Their HDR screenshot shows Contrast 50, Brightness 50, Color 50, Tint 0, Sharpness 0, so SDR values are **not** copied |
| M1 | both | Motion Enhancement | Off as tested; for smoothing use Custom and set Judder/Blur Reduction to taste | PREF | Motion Settings › Motion Interpolation | May need changing per content |
| M2 | both | Motion Clearness | Off as tested | SHOWN | Advanced SDR (screenshot) | Preference |
| M3 | info | Judder from 24p | Nothing to set | AUTO | Motion Settings › Judder | The TV removes judder from native 24p signals. This TV reports only 60/50 Hz modes for apps **[validate]**; info only, not checklisted |

Excluded article content: HDMI Format "Enhanced format" (HDR over HDMI), Gaming/GameZone/PC, eARC/Digital Audio Out,
accessibility, Ambient Mode, remote/voice, white-balance/colorspace calibration values.

Compared with the initial brief: every example value matched the page. Added from the page: SDR Picture Mode
"Theater Night" and Backlight 55 (preference). Values found only in screenshots are marked SHOWN.

Each item stores: `id`, `name`, `value`, `mode` (SDR/HDR/BOTH/INFO), `note`, `section`, `kind`.
User status per item: Applied, Skipped, Not found on my TV (unset = not done). These are the owner's own
confirmations, not settings checked by the app.

## 7. HDR and SDR playback approach

- Media3 ExoPlayer with platform hardware decoders, rendered to a **SurfaceView** (Media3 `PlayerSurface`
  with `SURFACE_TYPE_SURFACE_VIEW`; fallback `AndroidView(SurfaceView)`).
- Loop: `REPEAT_MODE_ONE` on one local file. Keep the screen on while playing.
- Leave ExoPlayer's frame-rate strategy at the default. No forced display mode switch.
- Diagnostics keep these apart and label each one:
  1. Display: HDR types the display reports (`Display.getHdrCapabilities`).
  2. Decoder: whether an HEVC decoder lists Main10 HDR10 (`MediaCodecList`).
  3. Source: track `Format` color info (primaries, transfer, range) and HDR static metadata if present.
  4. Playback: decoder actually selected and output size (Media3 analytics).
  5. TV: **"Active HDR on TV: needs confirmation"** until the owner confirms the HDR icon in the picture menu.
     Stored as the owner's confirmation.
- Nothing in 1–4 is presented as proof that the TV is in HDR mode.
- SDR: an SDR BT.709 clip. Switching from HDR to SDR shows "Check the picture menu: HDR icon should be gone"
  rather than claiming the TV changed mode.

## 8. Test-asset source, license and validation

Primary HDR10 source: **Netflix Open Content** (opencontent.netflix.com). The site states its content is under
Creative Commons Attribution 4.0 International. Candidates with HDR10 (PQ, BT.2020, 1000-nit) masters: *Sparks*
and *Sol Levante*. **[validate at M0]** per-title license file and download size.

Clip target: 20–60 s, 3840x2160, HEVC Main10, `yuv420p10le`, BT.2020 primaries, SMPTE ST 2084 (PQ) transfer,
mastering-display metadata and MaxCLL/MaxFALL from the source master, 59.94/60 or 50 fps (the TV's UI modes are
60/50 Hz), about 50–120 MB. Encoded with ffmpeg/libx265 from the HDR10 master, without changing grading or tone mapping.

Recorded in `assets/LICENSES.md`: title, author (Netflix), license and link, the changes made (trimmed and re-encoded),
file size, the exact ffmpeg command, and the ffprobe summary.

SDR clip: the same title's SDR deliverable if available, otherwise a Blender open movie (CC BY); BT.709, 8-bit, tags checked.

Not allowed: streaming-service footage, SDR relabelled as HDR, images passed off as HDR, claiming the clip is a
calibration pattern, generated test patterns without validating them.

Validation: `ffprobe -v error -show_streams -show_frames -read_intervals %+#1 <clip>` must show `hevc`/Main 10,
`yuv420p10le`, `bt2020`, `smpte2084`, `bt2020nc` and mastering-display side data. Proof of HDR on the TV is the HDR
icon in the TV's picture menu.

Fallback if no usable redistributable HDR10 source: the app also plays `hdr10.mp4` / `sdr.mp4` from its own
external files folder, pushed with `adb push <clip> /sdcard/Android/data/com.reyespascal.picturehelper/files/`.
It works offline once copied. The app says clearly when no clip is present.

## 9. Lifecycle behavior and device uncertainties

- Player created in `onStart`, released in `onStop`; it keeps playing through `onPause` (an overlay may only pause
  the activity or take window focus). No lifecycle overrides, no background service, no picture-in-picture.
- Logged under logcat tag `PictureHelper`: lifecycle callbacks, window-focus changes, player state and errors, loop
  transitions.
- During M1 on the TV (read-only):
  `adb shell dumpsys activity activities | grep -i resumed`,
  `adb shell dumpsys window | grep mCurrentFocus`,
  `adb shell dumpsys SurfaceFlinger` (video layer dataspace, as supporting evidence only).

Open device questions **[validate]**: picture-menu button and whether it's an overlay or a full activity; HDR held
while the menu is open; HDR controls available for app video; blips at loop; whether 24p content gets a 24 Hz mode
(not reported by the display).

## 10. Phased implementation tasks

| Milestone | Scope |
|---|---|
| M0 | Gradle wrapper, pinned versions, empty TV activity builds; HDR10 + SDR clips sourced, encoded, validated, licensed |
| M1 | Launcher screen, one "Play HDR setup video" button, full-screen HDR10 loop, Back exits, errors + basic diagnostics → **stop for TV check** |
| M2 | `RtingsChecklist.kt` + `ProgressStore` (per mode, reset) + unit tests |
| M3 | Home, Before playback, Saved progress, About screens; focus and focus restore |
| M4 | Guide overlay during playback: one item, mark status, hide/restore, refocus after native menu |
| M5 | SDR workflow + HDR↔SDR transition check on TV |
| M6 | Signed release APK, APK inspection, install notes |

## 11. Pass/fail criteria

- **M0:** `./gradlew assembleDebug lint` succeeds; clip ffprobe output meets section 8; license file present.
- **M1 (emulator):** app launches from the TV launcher; button is focused and selectable with OK; Back exits playback.
- **M1 (owner on TV):** installs; visible in launcher; clip plays smoothly; picture menu shows HDR icon; menu opens
  during playback; HDR settings available; closing the menu returns to playing video; no HDR drop or blank at the loop
  point; leaving playback stops it (log shows release). Any failure → report observations, propose the smallest
  next experiment, and don't build M2+ on it.
- **M2:** unit tests pass (unique ids, every item has mode/kind/section; no calibration numbers; save/load/reset per mode).
- **M3:** emulator D-pad test reaches every Home item; Back returns; focus restored after returning.
- **M4 (TV):** guide readable from the couch; hide/restore works; after closing the native menu, focus is back on the guide.
- **M5 (TV):** SDR clip plays with no HDR icon; going HDR→SDR the guide doesn't claim a mode change.
- **M6:** release APK installs over the previous build (same key); inspection in section 13 done.

## 12. Build, test and installation commands

```bash
./gradlew assembleDebug lint testDebugUnitTest        # build + checks
./gradlew connectedDebugAndroidTest                   # on API 30 TV emulator
./gradlew assembleRelease                              # signed release (needs signing props, section 14)
find app/build/outputs/apk -name '*.apk'               # actual APK path; don't guess the name
```

Install on the TV:
1. On the TV: Settings › System › About › tap "Android TV OS build" 7 times; then Developer options → USB debugging
   (and network debugging if shown).
2. Find the TV's current IP in Settings › Network (it is not fixed).
3. `adb connect <tv-ip>:5555`, accept the prompt on the TV, `adb devices -l` (check `songshan`).
4. `adb -s <tv-ip>:5555 install -r <apk path from find>`.
Android 11 also offers Wireless debugging with a pairing code (`adb pair`) if 5555 isn't open.
Never change router settings or expose adb to the internet. After testing, turn USB/network debugging off in
Developer options.

## 13. APK compatibility inspection

- No `abiFilters`, no ABI splits → one universal APK.
- `unzip -l <apk> | grep '\.so$'`: if native libraries exist (Compose may add `libandroidx.graphics.path.so`),
  each must be present for both `armeabi-v7a` and `arm64-v8a`.
- `aapt2 dump badging <apk>`: check `leanback-launchable-activity`, `uses-feature` lines, minSdk/targetSdk, native-code.
- A clean result shows only that no ABI restriction exists. It doesn't prove the app works on every Android TV.

## 14. Signing and update procedure

- One release keystore, created once with `keytool`, stored outside the repo in `~/.local/state/picture-helper/keys/`.
- Gradle reads the keystore path, alias and passwords from `~/.gradle/gradle.properties` (`PH_STORE_FILE`,
  `PH_STORE_PASSWORD`, `PH_KEY_ALIAS`, `PH_KEY_PASSWORD`) or matching environment variables. Never printed in logs.
- `.gitignore` excludes `*.jks`, `*.keystore`, `keystore.properties`, `local.properties`, `build/`, `.gradle/`, `.idea/`.
- Backup: copy the keystore plus a note of the passwords to the owner's password manager or an encrypted USB drive.
  Losing it means uninstalling (and losing saved progress) before a new-key build can be installed.
- Updates: bump `versionCode`, build release with the same key, `adb install -r`.
