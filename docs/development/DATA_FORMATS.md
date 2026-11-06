# Saves and network compatibility

## User files

`AppData.directory()` chooses the user-data directory:

| OS | Default |
| --- | --- |
| macOS | `~/Library/Application Support/Outpost` |
| Windows | `%APPDATA%/Outpost`, falling back to `~/Outpost` |
| Linux | `$XDG_DATA_HOME/outpost`, falling back to `~/.local/share/outpost` |

The JVM property `outpost.dataDir` overrides that location. Use it for isolated debugging. Do not test save corruption against a real player's only save.

`AppData` reads Java Properties with a size limit and replaces files through a temporary file, with a `.bak` backup and atomic move when supported. It is data serialization, not Java object deserialization.

The active solo save is `save.properties`; chapter retry uses `chapter-checkpoint.properties`. Playtest saves/checkpoints are separate. `PlayerSaves` archives independent player profiles and their checkpoints. `Settings` has its own versioned file.

## Solo save version 2

`SaveGame.Snapshot` contains identity, ground position, facing, progression, wave director state, enemies, arena, resource amount and day phase. `Progression.store/restore` handles its own nested systems: expedition blessings, field encounter, recovery wisps and chapter feats, as well as gear, training, currency, health and sanctuary integrity.

Optional keys provide migration defaults. For example, old saves without ownership masks retain their previously available tiers; new masks can represent ownership gaps. Equipped gear must actually be owned. Floats must be finite and numeric bounds must be checked before native presentation receives the data.

Version 1 migration is retained. Do not remove it or reinterpret numeric enemy variants without a deliberate migration. A chapter checkpoint represents entry state, not an exact mid-fight rewind. Save/load tests cover valid round trips and corrupt input.

## Co-op checkpoint version 1

This is a separate format with `coop.version`, chapter, difficulty and per-slot build values (`build.<slot>.<index>`). It restarts a chapter; it is not a full serialization of in-flight projectiles or every enemy. `CoopWorld.checkpoint/restore` define the mapping. Names/classes are supplied when the players attach.

## Co-op wire protocol version 1

TCP defaults to port **27878**. The client handshake writes protocol version, requested slot, invite code, player name and class ordinal. `CoopServer` validates the request and returns a status plus assigned slot.

After connection, framed packets use a four-byte length followed by the payload, with a maximum of 65,536 bytes. Input packets contain sequence, movement X/Z, aim and button bits. Host snapshots contain world header, heroes, mobs, bolts and hazards in the precise order written by `CoopProtocol`.

The decoder now names each field in `readHero`, `readMob`, `readBolt` and `readDanger`. Keep that order synchronized with the writer. The current decoder caps heroes at 2, enemies at 128, bolts at 256 and hazards at 64. Floating-point values must be finite; enum/variant ordinals and counts are bounded; trailing packet bytes are rejected.

Changing a Java field name need not change bytes. Adding/removing/reordering fields does. Bump `CoopProtocol.VERSION` and deliberately handle incompatible clients if the layout changes. Stable version-1 binary fixtures are in `src/test/resources/`; one captures a lobby and another exercises populated hero/mob/bolt/hazard fields using the published writer. `ProtocolCompatibilityTest` checks them independently of a newly changed writer/reader pair.

## Properties that are not save data

- `outpost.assets`: override the external resource root; `AssetPaths` resolves runtime files there.
- `outpost.dataDir`: override writable user data.

Do not point both at the same directory. The resource directory contains game assets; the data directory contains player state.
