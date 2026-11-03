# Handoff — Picture Helper (2026-11-03 ~04:55)

Read CLAUDE.md and SPEC.md first. Owner rules: plan before build; plain-text questions; push to GitHub after each
TV-verified milestone; ask before creating any signing key. Hisense adb: `adb -s <tv-ip>:5555` (IP in Claude memory, not in the repo) (check
`adb devices -l` shows `songshan`).

## State
- `main` @ 7c4bb50 (M1). Branch `wip-guide` @ d8fe7d9, **committed locally, NOT pushed** (origin/wip-guide is 2354a32).
- Fixed and verified on the TV in this session:
  - **HDR-loss bug**: ExoPlayer prepare() before the SurfaceView existed → decoder started on the placeholder
    surface → moving it switched the Hisense buffer format 0x13→0x15 → GPU composition → TV dropped HDR.
    Fix: prepare in `onSurfaceSizeChanged` (PlaybackScreen.kt). Owner confirmed HDR modes listed again.
    Probes: logcat `SurfaceUtils ... color 0x13` good / `0x15` bad; `dumpsys SurfaceFlinger` HWC comp DEVICE vs
    CLIENT; logcat `getSystemCB ... Picture_Mode=` (17 = SDR Theater Night, 4 = HDR Theater).
  - Held (paused) scenes keep HDR (logs: DEVICE, Picture_Mode=4).
  - While the TV menu is open the guide shows a **card on the right listing every unmarked item** (changes first,
    check-only dimmed); closing the menu brings the full guide back. Owner saw it fit on screen.
  - Removed the "Check HDR is on" step; clearer compare instructions.
- Current TV state: guide build d8fe7d9 installed; owner finished HDR list (8/8 old items).
- Remote control from adb works: `KEYCODE_SETTINGS` opens the quick bar, 4×Right = Picture, OK. `uiautomator dump`
  reads the tv.settings menu text. Never press OK on toggles (Smart Scene, light sensor) or list items.

## Approved next step (owner said go; in progress)
Rewrite `checklist/RtingsChecklist.kt` to cover EVERY setting in this TV's Picture menu (map below), labelled
RTINGS / other reviewers / our suggestion; settings usually already right become quick "check" items listed after
the changes. **Owner decisions: no white-balance/calibration section at all; Color Temperature = Warm1.**

| Setting | HDR | SDR | Label |
|---|---|---|---|
| Picture Mode | HDR Theater | Theater Night | RTINGS |
| Color Temperature | Warm1 | Warm1 | RTINGS (U8H: Warm1 closest to 6500K) + Digital Trends U8K |
| Noise Reduction / Digital NR | Off / Off (Low only for bad streams) | same | RTINGS, Reviewed |
| Sharpness | 0 (7 = factory if soft) | 0 | RTINGS U6H screenshot |
| Motion Enhancement | Off (Film if pans stutter) | same | RTINGS, Reviewed |
| Motion Clearness | Off | Off | Reviewed |
| FILMMAKER MODE Auto Detection | Off | Off | our suggestion |
| Backlight Level | Max (TV sets it; check) | ~55 dark room / 80–100 bright | RTINGS |
| Gamma | ST2084 (locked) | 2.2 lit room / BT1886 dark room | RTINGS, Sound & Vision |
| Contrast | 50 | 39 | RTINGS |
| quick checks | Local Dimming High, Light Sensor Off, Smart Scene Off, Brightness 50, Color 50, Tint 0, Active Contrast Off, Color Space Auto, Low Blue Light Off, Instant Game Response On, Apply Picture Settings (your choice), Calibration Settings menus stay 0 | same | mixed |

Remove items not in this firmware's menu: Picture Size, Overscan. Update tests (H4/S11 values, S16 id), Scenes map
(every id mapped), MenuCard partition (quick items dimmed), About screen source list (RTINGS U6H/U8H/U6K, Reviewed
U6H, Digital Trends U8K, PCMag, T3, Sound & Vision 65H8G). Then build/lint/test, install, owner checks, then merge
`wip-guide` → `main` and push.

## TV Picture menu (read 2026-11-03)
Picture: Backlight › (Local Dimming, Backlight Level, Automatic Light Sensor, Light Sensor Shift) · Picture Mode ·
Contrast · Brightness · Color · Tint · Sharpness · Apply Picture Settings · Smart Scene (toggle) · Advanced Settings ›
(Color Temperature Warm1/Warm2/Standard/Cool, Motion Enhancement, Motion Clearness, Noise Reduction, Digital Noise
Reduction, Active Contrast, FILMMAKER MODE Auto Detection, Color Space, Instant Game Response, PC Sync, Low Blue
Light) · Calibration Settings › (Color Tuner, White Balance 2/20 Point, Gamma, Gamma Calibration, RGB Only) · Reset.

## Later
Dolby Vision setup (owner asked; deferred, needs a DV clip). M5 SDR↔HDR transition check, M6 release (signing key
only if the owner agrees).
