# Picture Helper — notes for Claude

Personal Google TV app for the owner's Hisense 65U6H: loops genuine HDR10/SDR clips so the native picture menu
can be used, and walks through the RTINGS U6H settings. Full spec: [SPEC.md](SPEC.md). Read it before changing scope.

## Commands
- JDK 17: `export JAVA_HOME=$HOME/jdk17`
- Build + checks: `./gradlew assembleDebug lint testDebugUnitTest`
- APK path: `find app/build/outputs/apk -name '*.apk'` (never guess the file name)
- Install: `adb -s <tv-ip>:5555 install -r <apk>` (always check `adb devices -l` shows `songshan` first)

## TV and HDR constraints
- Android 11 (SDK 30), **32-bit ARM userspace only**, UI 1080p @ 60/50 Hz, video plane 4K.
- Only proof that HDR is active: the HDR icon in the TV's own picture menu, seen by the owner.
  Display capabilities, clip metadata, builds and emulator runs are not proof.
- Never consume Settings/Menu/Info/Home keys. App navigation uses only D-pad, OK, Back.
- Keep playing through `onPause`; release in `onStop`. No background service or PiP tricks.
- No private intents or undocumented Hisense APIs.

## Repo conventions
- The repo is **public**: never commit keys, passwords, IPs, `local.properties`, build outputs or RTINGS images.
- RTINGS content is summarized in our own words with attribution and a link.
- Clip licenses and encode commands live in `app/src/main/assets/LICENSES.md`.

## Verification and commits
- One small feature per step. Report the exact commands and real results; label anything not run.
- Stop and ask the owner whenever a result needs the physical TV.
- After two failed attempts at the same issue, summarize the evidence and rethink.
- Commit each verified milestone and push to `origin main`.

## Confirmed device gotchas
- Hisense adb: LAN `:5555` works when Tailscale is offline. The TV's IP can change.
- The display reports no 24 Hz mode.
- Picture menu = remote's square two-line button → Picture. It's a `com.android.tv.settings` overlay: the app only
  loses window focus (no onPause) and HDR playback continues (verified M1).
- `adb exec-out screencap -p` output is corrupted by vendor messages; use `screencap -p /data/local/tmp/x.png` + `adb pull`.
- **HDR needs the decoder to start on the real SurfaceView.** If ExoPlayer prepares before the surface exists, the
  decoder starts on a placeholder surface; moving it later switches the buffer format (logcat `SurfaceUtils … color
  0x13` → `0x15`), SurfaceFlinger falls back to CLIENT composition and the TV leaves HDR. Prepare in
  `onSurfaceSizeChanged`. Quick check: `dumpsys SurfaceFlinger` video layer must be DEVICE.
