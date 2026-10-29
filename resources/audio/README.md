# Original Outpost audio

Rebuild with `python3 tools/prepare_audio.py` using only Python standard-library modules. No external recordings or samples are used.

`music-title`, `music-meadow`, `music-cemetery`, `music-frost`, and `music-necropolis` are 24-second stereo PCM scores at 24 kHz. `music-battle` is a synchronized percussion stem on the same 8-bar / 80-BPM grid. Circular reverb preserves tails at loop boundaries. GameAudio streams all stems from a common start, crossfades scene themes, and smoothly adds combat percussion. All music and effects obey master volume and mute.

The short 44.1-kHz stereo files layer filtered noise, pitch envelopes, resonant tones and stereo reflections. Swings/metal hits include three variants; bow, magic, parry, dash, slam, ward and upgrade have distinct cues. PCM tests check duration, stereo content, silence, peaks and loop seams.
