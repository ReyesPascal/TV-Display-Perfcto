# Handoff — Picture Helper (2026-11-03 ~03:50)

Read CLAUDE.md and SPEC.md first. Owner rules: plan before build; wait for "go"; plain-text questions;
push to GitHub after each TV-verified milestone; ask before creating any signing key.

## State
- `main` @ 7c4bb50 — **M1 verified on the Hisense**: HDR10 loop, TV picture menu (square two-line button →
  Picture) opens over playback without pausing it, HDR picture modes listed, right-side diagnostics panel.
- Branch `wip-guide` — **guide work, NOT yet verified on the TV** (builds; 11 unit tests pass; lint 0 errors):
  - `checklist/RtingsChecklist.kt` (26 items, HDR/SDR/BOTH, kind, where, source section), `ProgressStore.kt`
    (DataStore, per-mode marks + position, reset), `Scenes.kt` (5 held scenes + Moving, mapped per item).
  - `ui/`: Home (HDR10 setup, SDR setup, Saved progress, Diagnostics, About), PrePlay, Progress (+ reset
    confirm), About (RTINGS + CC BY attribution), Diagnostics, Components (TvButton yellow focus, ConfirmDialog).
  - `ui/GuideFlow.kt` + `playback/PlaybackScreen.kt`: one setting at a time on the right; auto-holds the item's
    scene for before/after; **compare mode**: when the TV menu opens (window focus lost) the guide shrinks to a
    one-line reminder bottom-right and stays small after the menu closes until OK; Hide; Details; summary at end.
  - Polish per tvOS HIG: 250 ms fade/slide panel, fade between items, progress bar, ✓ on current mark,
    safe margins 48x30 dp.
- Installed on the TV: still the M1+right-panel debug build (0.1.0). The guide build is not installed.

## Next
1. Emulator check of the guide UI. The emulator can't decode our 4K clips, and `adb push` into
   `/sdcard/Android/data/<pkg>/files/` was **denied** on the Android 11 emulator. Use a debug-only test asset or
   `run-as`, or skip the emulator and test on the TV. The SPEC "copy your own clip" fallback is unverified.
2. Install the guide build on the TV (`adb -s 192.168.4.27:5555 install -r app/build/outputs/apk/debug/app-debug.apk`)
   and have the owner test: scene hold keeps HDR on, compare mode, marks saved, focus after closing the TV menu.
3. If it passes: merge `wip-guide` → `main`, push. Then M5 SDR check (HDR→SDR transition) and M6 release
   (signing key only if the owner agrees; the debug APK is 90 MB).

## Housekeeping
- The emulator may still be running (`adb -s emulator-5584 emu kill`).
- Clip rebuild: `scripts/make-clips.sh <work dir on disk>` (downloads ~8 GB of masters; delete them afterwards).
