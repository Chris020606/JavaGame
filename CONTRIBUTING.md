# Working on Outpost

The [maintenance handbook](docs/development/README.md) explains setup, runtime ownership, save/network compatibility, assets, testing and the preserved unfinished feature branch.

- Use JDK 24 or newer; CI uses 25.
- Run `./gradlew test` before committing rule changes.
- Format Java with `python3 tools/format_java.py` and Python tools with the pinned Black dependency in `requirements-dev.txt`.
- Keep behavioral changes separate from formatting or code movement.
- Preserve save keys and network field order unless you implement a deliberate versioned migration.
- Keep native Raylib presentation on the window thread. Do not mix incompatible animation rigs.
- Run relevant native scenarios after UI, animation, map or co-op changes. Review their captures.
- Keep source/license provenance for imported assets. Do not commit player saves or private notes.

The game entry point is `Main`. Retained files under `src/main/java/examples/` are experiments, not alternative gameplay implementations.
