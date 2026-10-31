# Outpost — extended gameplay trailer

The edit uses actual gameplay and the original Outpost soundtrack. It shows the current development build, with equipment and training available through its all-gear playtest. It has no stock gameplay, paid-store claims, or invented release date.

The delivered video shows v2.2 gameplay. New footage has been recorded for the animation rebuild, but refreshing the edited export is deferred while gameplay development continues.

## Suggested upload copy

**Title:** OUTPOST — Guard the Last Light | Extended Gameplay Trailer

One ember still burns. Choose your warden and protect it.

Fight as a Warrior, Archer, or Mage through a four-chapter action RPG. Read enemy formations, manage stamina, parry and dodge, and face the Ash Warden and Hollow King. Outside the sanctuary, recover lost supplies, escort survivors, and defend awakened waystones. Earn ember blessings to shape your build, and change weapons and fitted armor as your journey grows.

Try the latest development build and the all-gear sandbox:
https://github.com/Chris020606/JavaGame/actions/workflows/build.yml

Source, instructions and feedback:
https://github.com/Chris020606/JavaGame

The video shows a development build. Original music and sound by Outpost; characters and animations by KayKit; environment models by Kenney. Full credits: https://github.com/Chris020606/JavaGame/blob/master/THIRD_PARTY_NOTICES.md

## Video chapters

| Time | Scene |
| --- | --- |
| 0:00 | The last ember |
| 0:08 | A valley worth defending |
| 0:33 | Warrior combat |
| 1:03 | Parry and evade |
| 1:21 | Archer and crossbow |
| 1:46 | Mage and piercing spells |
| 2:11 | Lost supplies |
| 2:27 | Choose an ember blessing |
| 2:45 | Bring a survivor home |
| 3:10 | Defend a waystone |
| 3:35 | Equipment and armor |
| 3:53 | The frozen pass |
| 4:08 | The Ash Warden |
| 4:33 | The Hollow King |
| 4:58 | Bring back the dawn |
| 5:10 | Try Outpost |

## Rebuild the video

Requires JDK 24+, FFmpeg on PATH, and a desktop graphics session. Capture writes only the game framebuffer, using an isolated temporary profile. It never records the desktop or your campaign save.

```sh
./gradlew captureTrailer
./gradlew trailerTitles
python3 tools/edit_trailer.py
```

Outputs stay in ignored `build/marketing/`: the H.264/AAC 1080p/24 MP4, separate English captions, a poster, sound-cue logs, raw footage, and a validation report. The planned edit is **5:25**, using 16 distinct shots. The editor checks duration, streams, unique footage and a complete decode. It combines the original score with sound effects logged at the actual gameplay frame.

The finished delivery files are also copied to ignored `marketing/exports/`, so a Gradle clean preserves the video, poster, captions, upload copy and validation report. Optional captions are supplied as a sidecar SRT; the edit already contains feature titles.

Use `captureTrailer --args=--preview` and `trailerTitles --args=build/marketing-preview`, followed by `python3 tools/edit_trailer.py --folder build/marketing-preview`, for a short technical preview. `--from=N` resumes a full recording at a zero-based shot; `--only=N` replaces one shot. `edit_trailer.py --resume` reuses already edited clips.

Rendered media is delivered locally. Uploading or publishing it is a separate action.
