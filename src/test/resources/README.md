# Protocol compatibility fixtures

These files encode protocol-version-1 snapshots using the writer from published commit `6b64b36`, before the readability refactor. They contain synthetic test identities, not player data.

- `coop-protocol-v1.bin`: a fresh Normal co-op lobby with seed 42, player One/Warrior in slot 0 and Two/Mage in slot 1.
- `coop-protocol-v1-active.bin`: a synthetic PLAYING snapshot with both hero slots plus a mob, bolt and danger. Distinct nonzero field values exercise wire ordering; the variant is 5, boss move 3 and danger kind 1. Floating-point fields use their record-field index plus 1.25; ordinary integers use index plus 1. Booleans are true. This is a serialization fixture, not a balanced gameplay state.

`ProtocolCompatibilityTest` must decode these bytes and re-encode them identically. It also compares a fresh lobby to the old writer's lobby bytes. Do not regenerate fixtures using a changed writer simply to make a failing compatibility test pass. A deliberate protocol migration should add new-version fixtures and retain the old expectations where compatibility is supported.
