"""Create the Outpost app icon with no third-party image package."""
from pathlib import Path
import struct, zlib

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "packaging" / "icons"
OUT.mkdir(parents=True, exist_ok=True)

def rgba_png(size):
    rows = []
    cx = cy = (size - 1) / 2
    for y in range(size):
        row = bytearray([0])
        for x in range(size):
            dx, dy = x - cx, y - cy
            edge = 0.10 * size
            inside = abs(dx) <= cx-edge or abs(dy) <= cy-edge or (dx*dx+dy*dy) <= (cx-edge)**2
            r, g, b, a = (16, 30, 39, 255) if inside else (0, 0, 0, 0)
            sx, sy = abs(dx) / (size*.37), (dy + size*.02) / (size*.42)
            shield = sy > -.85 and sy < .88 and sx < (1.0 - max(0, sy)*.28) and (sy < .45 or sx < .82)
            if shield: r, g, b = 47, 157, 145
            line = abs(dy + dx*.92) < size*.065 and -size*.34 < dx < size*.34
            if line: r, g, b = 241, 190, 91
            guard = abs(dx) < size*.20 and abs(dy - size*.05) < size*.045
            if guard: r, g, b = 241, 190, 91
            pommel = (dx-size*.18)**2 + (dy-size*.23)**2 < (size*.075)**2
            if pommel: r, g, b = 241, 190, 91
            row.extend((r,g,b,a))
        rows.append(bytes(row))
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind+data)&0xffffffff)
    raw = zlib.compress(b"".join(rows), 9)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB",size,size,8,6,0,0,0)) + chunk(b"IDAT",raw) + chunk(b"IEND",b"")

png = rgba_png(512)
(OUT / "outpost.png").write_bytes(png)
ico_header = struct.pack("<HHH", 0, 1, 1)
ico_entry = struct.pack("<BBBBHHII", 0, 0, 0, 0, 1, 32, len(png), 22)
(OUT / "outpost.ico").write_bytes(ico_header + ico_entry + png)
# A minimal ICNS with a 512px PNG payload works on current macOS versions.
icns_entry = b"ic09" + struct.pack(">I", len(png) + 8) + png
(OUT / "outpost.icns").write_bytes(b"icns" + struct.pack(">I", len(icns_entry) + 8) + icns_entry)
