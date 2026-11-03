#!/usr/bin/env python3
"""Add HDR10 'mdcv' and 'clli' boxes to the HEVC sample entry of an MP4, without re-encoding.

ffmpeg/libx265 puts HDR10 metadata only in the HEVC bitstream (SEI). Players such as ExoPlayer also read
the container boxes and pass them to the decoder, so we write the same values there.

Usage: add_hdr_boxes.py in.mp4 out.mp4 "G(x,y)B(x,y)R(x,y)WP(x,y)L(max,min)" MaxCLL MaxFALL
(the mastering string uses x265 units: 0.00002 for chromaticity, 0.0001 cd/m2 for luminance).
The moov box must come before mdat (ffmpeg -movflags +faststart); chunk offsets are shifted to match.
"""
import re
import struct
import sys

CONTAINERS = {b"moov", b"trak", b"mdia", b"minf", b"stbl"}


def boxes(data, start, end):
    """Yield (offset, size, type) for boxes in data[start:end]."""
    pos = start
    while pos + 8 <= end:
        size, kind = struct.unpack(">I4s", data[pos:pos + 8])
        if size == 1:
            size = struct.unpack(">Q", data[pos + 8:pos + 16])[0]
        elif size == 0:
            size = end - pos
        yield pos, size, kind
        pos += size


def main():
    src, dst, md, cll, fall = sys.argv[1], sys.argv[2], sys.argv[3], int(sys.argv[4]), int(sys.argv[5])
    nums = [int(n) for n in re.findall(r"\d+", md)]
    gx, gy, bx, by, rx, ry, wx, wy, lmax, lmin = nums
    mdcv = struct.pack(">I4s8HII", 32, b"mdcv", gx, gy, bx, by, rx, ry, wx, wy, lmax, lmin)
    clli = struct.pack(">I4sHH", 12, b"clli", cll, fall)
    extra = mdcv + clli

    data = bytearray(open(src, "rb").read())
    top = list(boxes(data, 0, len(data)))
    kinds = [k for _, _, k in top]
    if kinds.index(b"moov") > kinds.index(b"mdat"):
        sys.exit("moov must come before mdat (use -movflags +faststart)")
    moov = next(b for b in top if b[2] == b"moov")

    # Find the hvc1/hev1 sample entry and remember the chain of parents.
    target = None

    def walk(start, end, chain):
        nonlocal target
        for off, size, kind in boxes(data, start, end):
            if kind in CONTAINERS:
                walk(off + 8, off + size, chain + [off])
            elif kind == b"stsd":
                # FullBox header (4) + entry_count (4), then sample entries.
                for eoff, esize, ekind in boxes(data, off + 16, off + size):
                    if ekind in (b"hvc1", b"hev1"):
                        inner = list(boxes(data, eoff + 8 + 78, eoff + esize))
                        if any(k in (b"mdcv", b"clli") for _, _, k in inner):
                            sys.exit("file already has mdcv/clli")
                        target = (eoff, esize, chain + [off])

    walk(moov[0], moov[0] + moov[1], [])
    if target is None:
        sys.exit("no hvc1/hev1 sample entry found")
    eoff, esize, parents = target

    insert_at = eoff + esize
    data[insert_at:insert_at] = extra
    for off in parents + [eoff]:  # moov … stsd and the sample entry itself grow by len(extra)
        size = struct.unpack(">I", data[off:off + 4])[0]
        if size == 1:
            sys.exit("64-bit box sizes not supported")
        struct.pack_into(">I", data, off, size + len(extra))

    # mdat moved later by len(extra): shift every chunk offset in every track.
    moov_size = struct.unpack(">I", data[moov[0]:moov[0] + 4])[0]

    def shift(start, end):
        for off, size, kind in boxes(data, start, end):
            if kind in CONTAINERS:
                shift(off + 8, off + size)
            elif kind == b"stco":
                n = struct.unpack(">I", data[off + 12:off + 16])[0]
                for i in range(n):
                    p = off + 16 + 4 * i
                    struct.pack_into(">I", data, p, struct.unpack(">I", data[p:p + 4])[0] + len(extra))
            elif kind == b"co64":
                n = struct.unpack(">I", data[off + 12:off + 16])[0]
                for i in range(n):
                    p = off + 16 + 8 * i
                    struct.pack_into(">Q", data, p, struct.unpack(">Q", data[p:p + 8])[0] + len(extra))

    shift(moov[0] + 8, moov[0] + moov_size)
    open(dst, "wb").write(data)
    print(f"added mdcv+clli ({len(extra)} bytes) -> {dst}")


if __name__ == "__main__":
    main()
