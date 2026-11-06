# Maintaining Outpost

Start here if you are taking over the game. The current `master` maintenance work is based on published **v4.2.1**. It changes code organization, names, formatting and documentation, rather than combat balance or the save/network formats.

The unfinished feature experiment is preserved at **`archive/roadmap-before-readability`**, commit `2120842`. It includes chapter travel, enchantment rolls, champion encounters and ranged balance experiments. Its double-Archer Normal campaign check was failing. It has **not** been merged into this maintainable baseline. Do not mistake the old development wishlist for a description of the shipped game.

## Read in this order

1. [Architecture and frame lifecycle](ARCHITECTURE.md)
2. [Where to change things](CHANGE_GUIDE.md)
3. [Saves and multiplayer protocol](DATA_FORMATS.md)
4. [Assets, animation and native resources](ASSETS.md)
5. [Checks, packaging and release](TESTING.md)
6. [Source index](SOURCE_INDEX.md)

## Run the project

You need a JDK **24 or newer**. CI uses JDK 25. The Gradle wrapper downloads the configured Gradle version; a separate Gradle installation is unnecessary. Run from the repository root:

```sh
./gradlew run
./gradlew test
./gradlew packageGame
```

On Windows use `gradlew.bat` instead of `./gradlew`. `packageGame` builds for the OS and CPU on which it runs; it does not cross-compile all desktop platforms. It embeds a Java runtime. The native app is written under `build/native/`.

The menu and window title report the game version. The startup log reports the actual Archer asset path, which helps distinguish a source run from a downloaded app. Packaged resources live beside the packaged JAR; source runs normally use the repository's `resources/` directory.

## Formatting

All Java source, including tests and retained examples, uses four-space AOSP formatting from a pinned google-java-format binary. The formatter script verifies its download checksum and caches it under `build/tools/`.

```sh
python3 tools/format_java.py
python3 tools/format_java.py --check
```

The Gradle equivalents are `./gradlew formatJava` and `./gradlew checkJavaFormat`.

Python authoring tools use Black. Install development-only dependencies in a virtual environment:

```sh
python3 -m venv build/dev-env
build/dev-env/bin/python -m pip install -r requirements-dev.txt
build/dev-env/bin/python -m black tools
build/dev-env/bin/python -m black --check tools
```

On Windows the interpreter path is `build\dev-env\Scripts\python.exe`. Python and the formatters are not shipped as game runtime dependencies.

## Generated Java reference

Run `./gradlew javadoc` to create `build/docs/javadoc/index.html`. The task succeeds, but reports missing-comment warnings for legacy public APIs; use this handbook for the architecture and change workflow.

## Current boundaries

- `Game` still coordinates much of solo gameplay. It is now expanded into normal statements, but it remains a large class. `MainMenuScreen` is an extracted view; continue extracting one responsibility at a time with regression checks.
- Most game classes remain in Java's default package. `Entities`, `Config`, `Scene` and `assets` are existing named packages. Moving everything into new packages would require coordinated import and entry-point changes; this pass does not hide such a migration inside formatting.
- Solo and co-op have different progression systems. Co-op does not reuse the complete solo inventory/economy.
- Maps are four chapter presentations with chapter-specific scenery and collision footprints, not an open world. Co-op supports LAN/private VPN, not a public matchmaking service.
- The `examples` directory contains retained Raylib experiments. `Main` is the game entry point; examples are not part of the main menu.
- Private `DEVELOPMENT_NOTES.md` and `GAME_GUIDE.md` remain ignored. The files in this directory are the public maintenance documentation.
