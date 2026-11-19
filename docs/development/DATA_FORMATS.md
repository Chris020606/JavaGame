# Save compatibility

## User files

`AppData.directory()` chooses the user-data directory:

| OS | Default |
| --- | --- |
| macOS | `~/Library/Application Support/Outpost` |
| Windows | `%APPDATA%/Outpost`, falling back to `~/Outpost` |
| Linux | `$XDG_DATA_HOME/outpost`, falling back to `~/.local/share/outpost` |

The JVM property `outpost.dataDir` overrides that location. Use it for isolated debugging. Do not test save corruption against a real player's only save.

`AppData` reads Java Properties with a size limit and replaces files through a temporary file, with a `.bak` backup and atomic move when supported. It is data serialization, not Java object deserialization.

The active solo save is `save.properties`; chapter retry uses `chapter-checkpoint.properties`. Playtest saves/checkpoints are separate. `PlayerSaves` archives independent player profiles and their checkpoints. `Settings` has its own versioned file. Its optional `musicVolume` and `effectsVolume` keys default to 1, preserving the existing master-volume mix for older profiles.

## Solo save version 2

`SaveGame.Snapshot` contains identity, ground position, facing, progression, wave director state, enemies, arena, resource amount and day phase. `Progression.store/restore` handles its own nested systems: expedition blessings, field encounter, recovery wisps and chapter feats, as well as gear, training, currency, health and sanctuary integrity.

`unlockedChapter` is an optional chapter index (0–3) used by solo travel. Old saves default it to zero; loading an adventure also infers access from its current wave and completed chapter. Replaying an earlier chapter preserves the highest unlock.

Optional keys provide migration defaults. For example, old saves without ownership masks retain their previously available tiers; new masks can represent ownership gaps. Equipped gear must actually be owned. Floats must be finite and numeric bounds must be checked before native presentation receives the data.

Version 1 migration is retained. Do not remove it or reinterpret numeric enemy variants without a deliberate migration. A chapter checkpoint represents entry state, not an exact mid-fight rewind. Save/load tests cover valid round trips and corrupt input.

## Properties that are not save data

- `outpost.assets`: override the external resource root; `AssetPaths` resolves runtime files there.
- `outpost.dataDir`: override writable user data.

Do not point both at the same directory. The resource directory contains game assets; the data directory contains player state.
