# Checks and release workflow

## Fast checks

```sh
python3 tools/format_java.py --check
build/dev-env/bin/python -m black --check tools
./gradlew test
```

Java tests exercise rules without an interactive game window, including combat geometry, saves, migrations, terrain, progression and the headless co-op world. Tests that touch native data are distinct from the desktop gameplay scenarios below. The protocol fixtures independently preserve published version-1 bytes.

## Native gameplay checks

These open a real Raylib window and may take minutes. Run native scenarios **sequentially**; concurrent scenarios can compete for input/window focus, and concurrent `packageGame` tasks can delete each other's destination directory.

```sh
./gradlew inputSmokeTest
./gradlew coopNativeTest
```

The full input smoke uses temporary player data and covers menus, tutorials, combat, inventory, saves and class presentation. Co-op native checks launch two game processes and exercise multiple class pairings. They are automated scenarios, not a replacement for a two-human balance session.

Focused scenarios are available when iterating:

```sh
./gradlew inputSmokeTest --args=--adventure-review
./gradlew inputSmokeTest --args=--combat-motion
./gradlew inputSmokeTest --args=--class-review
./gradlew inputSmokeTest --args=--hud-review
./gradlew inputSmokeTest --args=--art-review
./gradlew inputSmokeTest --args=--ownership-review
./gradlew inputSmokeTest --args=--boss-regression
./gradlew coopNativeTest --args=--quick
```

See the flag dispatch in `InputSmokeTest` for all supported scenarios. Captures are written under `build/`; inspect them rather than assuming a successful render means a good layout. The two-client tests also save their capture/log paths there.

To launch a short packaged smoke on macOS:

```sh
./gradlew packageGame
build/native/Outpost.app/Contents/MacOS/Outpost --smoke-test
```

The smoke flag uses temporary data and exits after 120 rendered frames. Windows and Linux launchers live under their native package directories. The normal `run` task supplies platform-specific JVM arguments.

## Check selection

| Change | Minimum relevant checks |
| --- | --- |
| Pure rule/economy change | affected unit tests and save round trips |
| UI/input/state routing | full input smoke and relevant layout captures |
| Animation/model/material | native combat-motion, class review and package launch |
| Map/collision | terrain/navigation tests, art review, ranged cover review |
| Co-op simulation/protocol | unit/campaign tests, protocol fixtures and co-op native check |
| Packaging or runtime resources | `packageGame`, packaged smoke and four-platform CI |

## Build artifacts and publishing

`packageGame` runs `stagePackage`, creates the platform-native app and embeds Java. `tools/archive_release.py` creates the portable archive and SHA-256 file. GitHub Actions builds Windows x64, Linux x64, Apple Silicon and Intel macOS. A `v*` tag additionally publishes a release after all four jobs succeed.

A source push is not proof that downloads have updated. Verify the tag's workflow and release assets before announcing a published version. Keep `build.gradle` and `BuildInfo.VERSION` in agreement when deliberately changing the product version.

## Handoff status

The clean baseline is published v4.2.1 with the maintenance pass and Warden's Journey additions on `master`. See `WARDENS_JOURNEY.md` for the current feature scope. The isolated archived roadmap branch contains new gameplay behavior and an unresolved double-Archer Normal balance failure. Its tests and screenshots are not evidence that those features shipped on `master`. Rebase/cherry-pick small portions deliberately if you decide to resume it.
