# Bundled clips: source, license and encoding

All three clips are short excerpts of **"Sol Levante"** (2020), produced by Netflix and Production I.G.
Source: Netflix Open Content, https://opencontent.netflix.com/
License: **Creative Commons Attribution 4.0 International** (CC BY 4.0), https://creativecommons.org/licenses/by/4.0/
(license text shipped by Netflix at `SolLevante/creative-commons-attribution-4-intl-public-license.txt`).

**Attribution:** "Sol Levante" © Netflix, Inc., used under CC BY 4.0.
**Changes made:** trimmed to 30.3 s (frames 1202–1929 of the 24 fps masters, from one scene cut to another),
audio removed, and the frames re-encoded to HEVC for playback on a TV. The grading and tone mapping weren't changed.
No endorsement by Netflix or Production I.G. is implied.

These clips are viewing content that makes the TV switch into HDR (or stay SDR). They are **not** calibration
test patterns.

## clips/hdr10.mp4

| | |
|---|---|
| Source master | `SolLevante/hdr10/SolLevante_HDR10_r2020_ST2084_UHD_24fps_1000nit.mov` (ProRes 4444 XQ, 12-bit, BT.2020, PQ) |
| Mastering display | From `SolLevante/vdm/sollevante_lp_16b_hdr_p3d65pq_dolbyvision29.xml`: P3-D65 primaries, 1000 nit peak, 0.0001 nit minimum |
| MaxCLL / MaxFALL | 1429 / 611 nits, measured on the source segment with `scripts/hdr_light_levels.py` |
| Encode | HEVC Main 10, 3840x2160, 24 fps, yuv420p10le, BT.2020 / SMPTE ST 2084 / BT.2020nc, limited range, HDR10 SEI on every keyframe, 2-pass 8 Mb/s |
| Command | `scripts/make-clips.sh` |
| Size / check | see "Validation" below |

## clips/dolby_vision.mp4

| | |
|---|---|
| Frames | Same encoded Sol Levante frames as `clips/hdr10.mp4`, not re-encoded |
| Dolby Vision metadata | Profile 8.1 RPUs generated with dovi_tool 2.3.4 (MIT) from `SolLevante/vdm/sollevante_lp_16b_hdr_p3d65pq_dolbyvision29.xml` (CM v2.9, CC BY 4.0), trimmed to frames 1202–1929 |
| L6 MaxCLL / MaxFALL | 1429 / 611 nits |
| dvvC box | Profile 8, level 6, BL compatibility 1 (HDR10) |
| Command | `scripts/make-clips.sh` |

## clips/sdr.mp4

| | |
|---|---|
| Source master | `SolLevante/sdr/SolLevante_SDR_UHD_24fps.mov` (ProRes 422 HQ, 10-bit, BT.709) |
| Encode | HEVC Main, 3840x2160, 24 fps, yuv420p, BT.709 primaries/transfer/matrix, limited range, 2-pass 4 Mb/s |
| Command | `scripts/make-clips.sh` |

## Validation

`ffprobe` on `clips/hdr10.mp4` (2026-11-03):
`hevc` Main 10 (level 5.0, tag `hvc1`), 3840x2160, `yuv420p10le`, 24/1 fps, 728 frames, 30.333 s,
`color_range=tv`, `color_space=bt2020nc`, `color_transfer=smpte2084`, `color_primaries=bt2020`,
30,418,046 bytes (8.02 Mb/s). First-frame side data: Mastering display metadata (R 0.68,0.32 · G 0.265,0.69 ·
B 0.15,0.06 · WP 0.3127,0.329 · 1000 / 0.0001 nits) and Content light level (MaxCLL 1429, MaxFALL 611).
The same HDR10 values are also in the MP4 container (`mdcv`/`clli` boxes, added by
`scripts/add_hdr_boxes.py`; decoded frames verified identical with `framemd5`).
Proof that the TV is in HDR is separate: it's the HDR icon in the TV's own picture menu.
