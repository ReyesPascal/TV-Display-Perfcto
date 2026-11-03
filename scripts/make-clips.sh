#!/usr/bin/env bash
# Rebuild the bundled setup clips from Netflix Open Content "Sol Levante" (CC BY 4.0).
# Only the 30.3 s segment (frames 1202-1929, shot cut to shot cut) is downloaded (~7.4 GB total, ProRes).
# Usage: scripts/make-clips.sh <work dir on a real disk>   (needs ffmpeg with libx265)
set -euo pipefail
REPO=$(cd "$(dirname "$0")/.." && pwd)
OUT=$REPO/app/src/main/assets/clips; mkdir -p "$OUT"
WORK=${1:?work dir}; mkdir -p "$WORK"; cd "$WORK"
B=https://s3.amazonaws.com/download.opencontent.netflix.com/SolLevante
SS=50.083333; N=728   # 1202 / 24 fps; 728 frames

[ -s hdr_src.mov ] || ffmpeg -v error -y -ss $SS -i "$B/hdr10/SolLevante_HDR10_r2020_ST2084_UHD_24fps_1000nit.mov" -map 0:v:0 -frames:v $N -c copy hdr_src.mov
[ -s sdr_src.mov ] || ffmpeg -v error -y -ss $SS -i "$B/sdr/SolLevante_SDR_UHD_24fps.mov" -map 0:v:0 -frames:v $N -c copy sdr_src.mov

# Mastering display from SolLevante/vdm/*.xml: P3-D65 primaries, 1000 nit peak, 0.0001 nit min.
# MaxCLL/MaxFALL measured on hdr_src.mov with scripts/hdr_light_levels.py.
MD="G(13250,34500)B(7500,3000)R(34000,16000)WP(15635,16450)L(10000000,1)"
CLL="1429,611"
HDRX="hdr10=1:hdr10-opt=1:repeat-headers=1:colorprim=bt2020:transfer=smpte2084:colormatrix=bt2020nc:range=limited:master-display=$MD:max-cll=$CLL:keyint=48:min-keyint=24:vbv-maxrate=16000:vbv-bufsize=32000"
[ "${SKIP_HDR:-0}" = 1 ] || for pass in 1 2; do
  ffmpeg -v error -y -i hdr_src.mov -an -map 0:v:0 -vf "scale=flags=lanczos,format=yuv420p10le" \
    -c:v libx265 -preset medium -profile:v main10 -b:v 8M -x265-params "$HDRX:pass=$pass:stats=hdr.log" \
    -color_primaries bt2020 -color_trc smpte2084 -colorspace bt2020nc -color_range tv \
    -tag:v hvc1 -movflags +faststart $( [ $pass = 1 ] && echo "-f null /dev/null" || echo "$OUT/hdr10.mp4" )
done
# Also write the HDR10 values into the MP4 container (mdcv/clli boxes); frames are untouched.
if [ "${SKIP_HDR:-0}" != 1 ]; then
  mv "$OUT/hdr10.mp4" hdr10_nobox.mp4
  python3 -I "$REPO/scripts/add_hdr_boxes.py" hdr10_nobox.mp4 "$OUT/hdr10.mp4" "$MD" ${CLL/,/ }
fi

# Dolby Vision profile 8.1: the same HDR10 frames plus Dolby Vision RPUs (no re-encode), built from Netflix's
# Dolby Vision XML for Sol Levante (CM v2.9, 6314 frames = the HDR10 master, CC BY 4.0). Needs dovi_tool
# (github.com/quietvoid/dovi_tool, MIT). The TV decodes DV profiles 4, 5 and 8, not 7.
if [ "${SKIP_DV:-0}" != 1 ]; then
  DOVI=${DOVI_TOOL:-dovi_tool}
  [ -s dv.xml ] || curl -sfo dv.xml "$B/vdm/sollevante_lp_16b_hdr_p3d65pq_dolbyvision29.xml"
  "$DOVI" generate --xml dv.xml -o dv_full.bin
  # Keep frames 1202-1929 (the clip) and put the measured MaxCLL/MaxFALL in L6 (the XML has 0/0).
  cat > dv_edit.json <<JSON
{"remove": ["0-$((1202 - 1))", "$((1202 + N))-6313"],
 "level6": {"max_display_mastering_luminance": 1000, "min_display_mastering_luminance": 1,
            "max_content_light_level": ${CLL%,*}, "max_frame_average_light_level": ${CLL#*,}}}
JSON
  "$DOVI" editor -i dv_full.bin -j dv_edit.json -o dv_clip.bin
  ffmpeg -v error -y -i "$OUT/hdr10.mp4" -map 0:v:0 -c copy -bsf:v hevc_mp4toannexb -f hevc dv_bl.hevc
  "$DOVI" inject-rpu -i dv_bl.hevc --rpu-in dv_clip.bin -o dv.hevc
  ffmpeg -v error -y -r 24 -i dv.hevc -c copy -tag:v hvc1 -movflags +faststart dv_nobox.mp4
  # dvvC (profile 8, HDR10-compatible, level 6 = UHD 24 fps) plus the same mdcv/clli as the HDR10 clip.
  python3 -I "$REPO/scripts/add_hdr_boxes.py" dv_nobox.mp4 "$OUT/dolby_vision.mp4" "$MD" ${CLL/,/ } 8.1@6
fi

# SDR: one pass, quality target with a bitrate cap (no need for an exact size).
SDRX="colorprim=bt709:transfer=bt709:colormatrix=bt709:range=limited:keyint=48:min-keyint=24:crf=22:vbv-maxrate=5000:vbv-bufsize=10000"
ffmpeg -v error -y -i sdr_src.mov -an -map 0:v:0 -vf "scale=flags=lanczos,format=yuv420p" \
  -c:v libx265 -preset medium -profile:v main -x265-params "$SDRX" \
  -color_primaries bt709 -color_trc bt709 -colorspace bt709 -color_range tv \
  -tag:v hvc1 -movflags +faststart "$OUT/sdr.mp4"
ls -l "$OUT"
