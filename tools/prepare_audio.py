"""Compose original looping scores and layered Foley with Python's standard library.
No downloaded recordings, samples, or additional dependencies are used.
"""

import math, random, struct, wave
from array import array
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "resources/audio"
OUT.mkdir(exist_ok=True)
TAU = math.tau


def pcm(name, left, right, rate):
    data = array("h")
    peak = max(max(map(abs, left)), max(map(abs, right)), 0.001)
    gain = 0.86 / max(1, peak)
    for l, r in zip(left, right):
        data.extend(
            (round(math.tanh(l * gain) * 30000), round(math.tanh(r * gain) * 30000))
        )
    import sys

    if sys.byteorder != "little":
        data.byteswap()
    with wave.open(str(OUT / (name + ".wav")), "wb") as f:
        f.setnchannels(2)
        f.setsampwidth(2)
        f.setframerate(rate)
        f.writeframes(data.tobytes())


def effect(name, duration, kind, seed=1):
    rate = 44100
    rng = random.Random(seed)
    left = array("f")
    right = array("f")
    low = 0
    for i in range(int(rate * duration)):
        t = i / rate
        u = t / duration
        noise = rng.uniform(-1, 1)
        low += 0.16 * (noise - low)
        attack = min(1, t / 0.004)
        tail = (1 - u) ** 2
        value = 0
        if kind == "swing":
            value = (
                0.44 * (noise - low) * math.sin(math.pi * u) ** 2
                + 0.12 * math.sin(TAU * (260 * t - 150 * t * t)) * tail
            )
        elif kind == "hit":
            value = (
                0.46 * low * math.exp(-t * 24)
                + 0.27 * math.sin(TAU * (115 * t - 70 * t * t)) * math.exp(-t * 17)
                + 0.15 * math.sin(TAU * 2310 * t) * math.exp(-t * 50)
            )
        elif kind == "hurt":
            value = (
                0.24 * low * math.exp(-t * 12)
                + 0.30 * math.sin(TAU * (83 * t - 25 * t * t)) * tail
            )
        elif kind == "kill":
            value = (
                0.22 * low * math.exp(-t * 9)
                + 0.14 * math.sin(TAU * 196 * t) * tail
                + 0.1 * math.sin(TAU * 294 * t) * tail
            )
        elif kind == "wave":
            value = sum(
                0.08 * math.sin(TAU * f * t + 0.15 * math.sin(TAU * 4 * t))
                for f in (146.83, 220, 293.66)
            ) * math.sin(math.pi * u)
        elif kind == "upgrade":
            value = sum(
                0.13
                * math.sin(TAU * f * t)
                * math.exp(-max(0, t - start) * 5)
                * (t >= start)
                for f, start in (
                    (523.25, 0),
                    (659.25, 0.09),
                    (783.99, 0.18),
                    (1046.5, 0.30),
                )
            )
        elif kind == "bow":
            value = (
                0.32 * low * math.exp(-t * 90)
                + 0.22 * math.sin(TAU * 310 * t) * math.exp(-t * 45)
                + 0.20 * (noise - low) * math.exp(-t * 70)
                + 0.10 * math.sin(TAU * 92 * t) * math.exp(-t * 30)
                + 0.38 * math.sin(TAU * (170 * t + 180 * t * t)) * math.exp(-t * 24)
                + 0.13 * (noise - low) * math.exp(-t * 15)
            )
        elif kind == "magic":
            value = (
                0.22 * math.sin(TAU * (740 * t - 350 * t * t)) * math.exp(-t * 6)
                + 0.18 * math.sin(TAU * (110 * t + 80 * t * t)) * math.exp(-t * 12)
                + 0.12 * (noise - low) * math.exp(-t * 20)
                + (
                    0.15 * math.sin(TAU * (380 * t + 320 * t * t))
                    + 0.13 * math.sin(TAU * (570 * t + 500 * t * t))
                    + 0.12 * low
                )
                * math.sin(math.pi * u)
            )
        elif kind == "parry":
            value = sum(
                0.16 * math.sin(TAU * f * t) * math.exp(-t * decay)
                for f, decay in ((1318, 9), (2093, 14), (2794, 20))
            ) + 0.12 * noise * math.exp(-t * 80)
        elif kind == "slam":
            value = 0.45 * math.sin(TAU * (55 * t - 12 * t * t)) * math.exp(
                -t * 4
            ) + 0.38 * low * math.exp(-t * 7)
        elif kind == "dash":
            value = 0.35 * (noise - low) * math.sin(math.pi * u) * tail
        elif kind == "ward":
            value = (
                0.17 * math.sin(TAU * 392 * t)
                + 0.12 * math.sin(TAU * 587.33 * t)
                + 0.08 * math.sin(TAU * 783.99 * t)
            ) * math.sin(math.pi * u) ** 2
        elif kind == "victory":
            for f, start in (
                (392, 0),
                (493.88, 0.18),
                (587.33, 0.36),
                (783.99, 0.60),
                (987.77, 0.60),
            ):
                age = t - start
                if age >= 0:
                    value += (
                        (
                            0.17 * math.sin(TAU * f * age)
                            + 0.04 * math.sin(TAU * f * 2 * age)
                        )
                        * (1 - math.exp(-age * 60))
                        * math.exp(-age * 2.7)
                    )
        elif kind == "boss-fall":
            value = (
                0.40 * math.sin(TAU * (64 * t - 17 * t * t)) * math.exp(-t * 3)
                + 0.32 * low * math.exp(-t * 5)
                + 0.16 * noise * math.exp(-t * 14) * (0.5 + 0.5 * math.cos(t * 95))
            )
        value *= attack * min(1, (duration - t) / 0.012)
        left.append(value)
        right.append(value * (0.97 + 0.03 * math.sin(t * 11)))
    # Short stereo reflections soften synthetic edges and give metal/stone a space.
    for delay, amount in ((0.031, 0.16), (0.067, 0.1)):
        offset = int(rate * delay)
        for i in range(len(left) - 1, offset - 1, -1):
            left[i] += right[i - offset] * amount
            right[i] += left[i - offset] * amount * 0.8
    pcm(name, left, right, rate)


for name, duration in [
    ("swing", 0.30),
    ("hit", 0.28),
    ("hurt", 0.46),
    ("kill", 0.5),
    ("wave", 1.5),
    ("upgrade", 1.1),
    ("bow", 0.28),
    ("magic", 0.55),
    ("parry", 0.65),
    ("slam", 1.0),
    ("dash", 0.35),
    ("ward", 0.95),
    ("victory", 1.8),
    ("boss-fall", 1.2),
]:
    effect(name, duration, name, 19)
for kind in ("swing", "hit"):
    for variant in range(3):
        effect(
            kind + str(variant),
            (0.27 if kind == "swing" else 0.25) + variant * 0.025,
            kind,
            31 + variant * 17,
        )

RATE = 24000
BEAT = 0.75
LENGTH = 24
FRAMES = RATE * LENGTH
CHORDS = (
    (50, 53, 57),
    (46, 50, 53),
    (53, 57, 60),
    (48, 52, 55),
    (50, 53, 57),
    (46, 50, 53),
    (43, 46, 50),
    (45, 49, 52),
)
MELODIES = (
    (69, 76, 74, 81, 79, 76, 72, 74),
    (62, 69, 65, 72, 70, 65, 61, 64),
    (76, 83, 81, 79, 86, 83, 79, 81),
    (50, 57, 62, 61, 65, 64, 57, 61),
    (69, 76, 79, 81, 76, 74, 72, 69),
)


def score(name, theme):
    left = array("f", [0]) * FRAMES
    right = array("f", [0]) * FRAMES

    def note(start, duration, midi, amp, voice, pan=0):
        freq = 440 * 2 ** ((midi - 69) / 12)
        offset = round(start * RATE)
        samples = int(duration * RATE)
        lg = math.sqrt((1 - pan) * 0.5)
        rg = math.sqrt((1 + pan) * 0.5)
        for i in range(samples):
            t = i / RATE
            u = t / duration
            if voice == "pad":
                env = math.sin(math.pi * u) ** 1.5
                sound = (
                    0.65 * math.sin(TAU * freq * t)
                    + 0.22 * math.sin(TAU * freq * 1.003 * t)
                    + 0.13 * math.sin(TAU * freq * 2 * t)
                )
            elif voice == "strings":
                env = (
                    min(1, t / 0.025)
                    * min(1, (duration - t) / 0.12)
                    * math.exp(-t * 0.8)
                )
                phase = TAU * freq * t + 0.012 * math.sin(TAU * 5 * t)
                sound = sum(math.sin(phase * h) / h for h in range(1, 7)) * 0.55
            elif voice == "pluck":
                env = (
                    (1 - math.exp(-t * 180))
                    * math.exp(-t * 3.4)
                    * min(1, (duration - t) / 0.08)
                )
                sound = (
                    math.sin(TAU * freq * t)
                    + 0.28 * math.sin(TAU * freq * 2 * t) * math.exp(-t * 5)
                    + 0.12 * math.sin(TAU * freq * 3 * t) * math.exp(-t * 8)
                )
            else:
                env = min(1, t / 0.11) * min(1, (duration - t) / 0.18)
                phase = TAU * freq * t + 0.035 * math.sin(TAU * 4.8 * t)
                sound = (
                    0.8 * math.sin(phase)
                    + 0.13 * math.sin(phase * 2)
                    + 0.07 * math.sin(phase * 3)
                )
            value = amp * sound * max(0, env)
            index = (offset + i) % FRAMES
            left[index] += value * lg
            right[index] += value * rg

    for bar, chord in enumerate(CHORDS):
        start = bar * 3
        if theme < 5:
            for j, midi in enumerate(chord):
                note(
                    start,
                    4.5,
                    midi,
                    0.095 if theme != 3 else 0.075,
                    "pad",
                    (j - 1) * 0.55,
                )
            note(start, 3, chord[0] - 12, 0.09, "pad", 0)
            for beat in range(4):
                pitch = chord[beat % 3] + 12
                note(
                    start + beat * BEAT,
                    1.8,
                    pitch,
                    0.07 if theme in (0, 2, 4) else 0.035,
                    "pluck",
                    (-1 if beat % 2 else 1) * 0.4,
                )
            melody = MELODIES[theme]
            note(
                start + 0.375,
                1.3,
                melody[bar],
                0.13 if theme != 3 else 0.09,
                "strings",
                0.18,
            )
            if bar % 2 == 1:
                note(start + 1.875, 1.6, melody[(bar + 2) % 8], 0.11, "pluck", -0.25)
            for step in range(8):
                note(
                    start + step * 0.375,
                    0.30,
                    chord[step % 3] + 12,
                    0.075,
                    "pluck",
                    (-1 if step % 2 else 1) * 0.6,
                )
        else:
            # Harmony-free percussion stem follows the same 8-bar loop as every theme.
            rng = random.Random(700 + bar)
            for beat in range(8):
                offset = int((start + beat * 0.375) * RATE)
                for i in range(int(0.36 * RATE)):
                    t = i / RATE
                    noise = rng.uniform(-1, 1)
                    drum = (
                        0.24 * math.sin(TAU * (72 * t - 54 * t * t)) * math.exp(-t * 16)
                        if beat % 2 == 0
                        else 0.07 * noise * math.exp(-t * 45)
                    )
                    if beat in (2, 6):
                        drum += 0.12 * noise * math.exp(-t * 22)
                    if bar % 4 == 3 and beat >= 6:
                        drum += (
                            0.16
                            * math.sin(TAU * (105 * t - 90 * t * t))
                            * math.exp(-t * 18)
                        )
                    drum += 0.025 * (noise) * math.exp(-t * 95)
                    index = (offset + i) % FRAMES
                    left[index] += drum
                    right[index] += drum * 0.94
    # Circular delay preserves the tails over the loop boundary, with no hard reset.
    dryL = array("f", left)
    dryR = array("f", right)
    for delay, amount in ((0.173, 0.18), (0.287, 0.13), (0.431, 0.09)):
        shift = int(delay * RATE)
        for i in range(FRAMES):
            left[i] += dryR[(i - shift) % FRAMES] * amount
            right[i] += dryL[(i - shift - 97) % FRAMES] * amount
    pcm(name, left, right, RATE)


for theme, name in enumerate(
    ("meadow", "cemetery", "frost", "necropolis", "title", "battle")
):
    score("music-" + name, theme)
    print("Composed", name, flush=True)


def boss_score(enrage=False):
    """160-BPM duel score: low strings, brass calls, driving toms and cymbals."""
    left = array("f", [0]) * FRAMES
    right = array("f", [0]) * FRAMES
    rng = random.Random(194)

    def voice(start, duration, midi, amp, pan, brass=False):
        freq = 440 * 2 ** ((midi - 69) / 12)
        offset = round(start * RATE)
        for i in range(round(duration * RATE)):
            t = i / RATE
            env = (
                min(1, t / 0.018)
                * min(1, (duration - t) / 0.06)
                * math.exp(-t * (1 if brass else 6))
            )
            phase = TAU * freq * t
            tone = (
                math.sin(phase)
                + 0.30 * math.sin(phase * 2)
                + 0.17 * math.sin(phase * 3)
                + 0.08 * math.sin(phase * 5)
            )
            index = (offset + i) % FRAMES
            value = amp * env * tone
            left[index] += value * math.sqrt((1 - pan) / 2)
            right[index] += value * math.sqrt((1 + pan) / 2)

    for bar in range(16):
        start = bar * 1.5
        root = (38, 38, 34, 34, 41, 41, 36, 37)[bar % 8]
        for step in range(8 if not enrage else 16):
            interval = 0.1875 if not enrage else 0.09375
            voice(
                start + step * interval,
                0.24,
                root + (12 if step % 4 in (1, 3) else 0),
                0.13 if not enrage else 0.075,
                (-1 if step % 2 else 1) * 0.45,
            )
        if not enrage:
            for midi in (root + 12, root + 19, root + 24):
                voice(start, 0.7, midi, 0.085, 0, True)
            voice(
                start + 0.75, 0.60, root + ([27, 24, 22, 19][bar % 4]), 0.11, 0.22, True
            )
        for beat in range(8):
            offset = round((start + beat * 0.1875) * RATE)
            for i in range(round(0.30 * RATE)):
                t = i / RATE
                noise = rng.uniform(-1, 1)
                if enrage:
                    value = 0.065 * noise * math.exp(-t * 55) + (
                        0.14 * noise * math.exp(-t * 16) if beat in (2, 6) else 0
                    )
                else:
                    value = (
                        0.34 * math.sin(TAU * (78 * t - 70 * t * t)) * math.exp(-t * 14)
                        if beat % 2 == 0
                        else 0.07 * noise * math.exp(-t * 60)
                    ) + (0.16 * noise * math.exp(-t * 24) if beat in (2, 6) else 0)
                index = (offset + i) % FRAMES
                left[index] += value
                right[index] += value * 0.94
    dryL = array("f", left)
    dryR = array("f", right)
    for i in range(FRAMES):
        left[i] += dryR[(i - round(0.1875 * RATE)) % FRAMES] * 0.14
        right[i] += dryL[(i - round(0.28125 * RATE)) % FRAMES] * 0.14
    pcm("music-enrage" if enrage else "music-boss", left, right, RATE)


boss_score()
boss_score(True)
print("Composed boss duel and enrage layer", flush=True)
