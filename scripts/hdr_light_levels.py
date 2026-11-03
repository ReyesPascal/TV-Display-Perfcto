#!/usr/bin/env python3
"""Measure MaxCLL and MaxFALL (nits) of a PQ / BT.2020 video.

Decodes every frame to 16-bit RGB with ffmpeg (limited-range input, BT.2020 non-constant matrix),
applies the SMPTE ST 2084 EOTF, and reports:
  MaxCLL  = brightest single pixel component (max of R,G,B) over the whole clip
  MaxFALL = highest per-frame average of max(R,G,B)
Usage: hdr_light_levels.py <video> [scale_width]   (needs numpy)
"""
import subprocess, sys
import numpy as np

src = sys.argv[1]
w, h = 3840, 2160
m1, m2 = 2610 / 16384, 2523 / 4096 * 128
c1, c2, c3 = 3424 / 4096, 2413 / 4096 * 32, 2392 / 4096 * 32

# 65536-entry lookup table: 16-bit full-range RGB code -> nits
e = np.arange(65536, dtype=np.float64) / 65535.0
p = np.power(e, 1 / m2)
lut = 10000.0 * np.power(np.maximum(p - c1, 0) / (c2 - c3 * p), 1 / m1)

cmd = ["ffmpeg", "-v", "error", "-i", src,
       "-vf", "scale=in_range=tv:out_range=pc:in_color_matrix=bt2020:flags=accurate_rnd+full_chroma_int,format=rgb48le",
       "-f", "rawvideo", "-"]
proc = subprocess.Popen(cmd, stdout=subprocess.PIPE)
frame_bytes = w * h * 3 * 2
max_cll, max_fall, n = 0.0, 0.0, 0
while True:
    buf = proc.stdout.read(frame_bytes)
    if len(buf) < frame_bytes:
        break
    rgb = np.frombuffer(buf, dtype="<u2").reshape(h, w, 3)
    nits = lut[rgb.max(axis=2)]
    max_cll = max(max_cll, float(nits.max()))
    max_fall = max(max_fall, float(nits.mean()))
    n += 1
proc.wait()
print(f"frames={n} MaxCLL={max_cll:.0f} MaxFALL={max_fall:.0f}")
