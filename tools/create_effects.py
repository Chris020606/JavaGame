"""Original soft particle and rune-decal textures, generated without image libraries."""

import math, struct, zlib
from pathlib import Path

out = Path(__file__).resolve().parents[1] / "resources/outpost/effects"
out.mkdir(exist_ok=True)


def png(name, sample, size=256):
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            u = (x + 0.5) / size * 2 - 1
            v = (y + 0.5) / size * 2 - 1
            a = max(0, min(1, sample(u, v)))
            raw.extend((255, 255, 255, round(a * 255)))

    def chunk(kind, data):
        return (
            struct.pack(">I", len(data))
            + kind
            + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        )

    data = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )
    (out / (name + ".png")).write_bytes(data)


def rune(x, y):
    r = math.hypot(x, y)
    a = math.atan2(y, x)
    ring = math.exp(-(((r - 0.88) / 0.018) ** 2)) + 0.5 * math.exp(
        -(((r - 0.68) / 0.012) ** 2)
    )
    tick = abs(math.sin(a * 12)) < 0.075 and 0.72 < r < 0.84
    diamond = abs(abs(x) + abs(y) - 0.86) < 0.014 and r > 0.62
    glyph = abs(math.sin(a * 6)) < 0.05 and 0.48 < r < 0.61
    return min(1, ring + tick * 0.8 + diamond * 0.7 + glyph * 0.7) * max(
        0, min(1, (1 - r) * 20)
    )


png("rune", rune)
png("soft", lambda x, y: math.exp(-(x * x + y * y) * 7) * max(0, 1 - x * x - y * y))
png(
    "spark",
    lambda x, y: math.exp(-(x * x * 24 + y * y * 5)) * max(0, 1 - x * x - y * y),
)
png("ring", lambda x, y: math.exp(-(((math.hypot(x, y) - 0.88) / 0.04) ** 2)))
