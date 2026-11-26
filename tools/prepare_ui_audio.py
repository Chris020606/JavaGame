"""Generate original, short interface cues without third-party recordings."""

import math
from pathlib import Path
import struct
import wave

ROOT = Path(__file__).resolve().parents[1] / "resources" / "audio"


def cue(name, seconds, frequency, fall):
    rate = 22050
    samples = []
    for i in range(round(rate * seconds)):
        t = i / rate
        envelope = min(1, t / 0.006) * (1 - t / seconds) ** 2
        phase = 2 * math.pi * (frequency * t - fall * t * t / 2)
        tone = math.sin(phase) * 0.75 + math.sin(phase * 2.01) * 0.15
        samples.append(round(11000 * tone * envelope))
    with wave.open(str(ROOT / f"{name}.wav"), "wb") as output:
        output.setparams((1, 2, rate, 0, "NONE", "not compressed"))
        output.writeframes(struct.pack(f"<{len(samples)}h", *samples))


if __name__ == "__main__":
    ROOT.mkdir(parents=True, exist_ok=True)
    cue("menu", 0.085, 690, 300)
    cue("error", 0.14, 220, 650)
