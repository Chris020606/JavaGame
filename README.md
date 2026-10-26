# Outpost

![Outpost gameplay](docs/images/ranged-enemies.png)

**Outpost** is a small 3D wave-defense game about a warden protecting a frontier settlement. Fight through increasingly dangerous raids, collect crystals, and improve your hero and outpost before the next attack arrives.

See [GAME_GUIDE.md](GAME_GUIDE.md) for the complete player and developer guide, and [DEVELOPMENT_NOTES.md](DEVELOPMENT_NOTES.md) for future work.

## Features

- Choose Warrior, Archer, or Mage with a sword, bow, or staff.
- An inventory with equippable weapon tiers, healing potions, and workshop restocking.
- Warlord bosses every fifth wave with crystal rewards and weapon loot.
- Directional sword attacks with a three-hit combo and a heavy cleave.
- Six enemy types with different strengths and attack styles.
- Archers and shamans that attack from a distance.
- Enemy attack warnings, floating damage numbers, health bars, and a minimap.
- A decorated low-poly map with paths, trees, lanterns, fences, rocks, and flowers.
- Sound effects, a name screen, save/load, and customizable controls.

![Combat feedback](docs/images/combat-feedback.png)

## Download and install

Open the [Releases page](https://github.com/Chris020606/JavaGame/releases) and download the file for your computer:

| Computer | File |
| --- | --- |
| Windows 64-bit | `Outpost-windows-x64.zip` |
| Mac with Apple Silicon | `Outpost-macos-arm64.tar.gz` |
| Mac with Intel processor | `Outpost-macos-x64.tar.gz` |
| Linux 64-bit | `Outpost-linux-x64.tar.gz` |

Extract the downloaded file somewhere on your computer. Keep the folders inside the archive together, then launch the Outpost application inside the extracted folder. Java is already included, so you do not need to install it separately.

On Windows or macOS, the first launch may show a security confirmation because the game is not digitally signed. Choose the option to open it if you downloaded it from the project’s GitHub Releases page. Linux requires a normal desktop session with working graphics and audio support.

## Controls

| Action | Key |
| --- | --- |
| Move | `W A S D` |
| Orbit the camera | Hold the right mouse button |
| Sword combo | `Space` or left click |
| Heavy class attack | `Q` |
| Inventory / use potion | `Tab` / `H` |
| Open the workshop | `E` near the outpost |
| Save / load | `F5` / `F9` |
| Mute sound | `M` |
| Pause or go back | `Escape` |
| Confirm a choice | `Enter` |

Choose your class on the name screen before beginning. Face an enemy before attacking. Arrows stop at the first enemy; magic bolts pierce enemies. Each class has a stronger attack on Q.

 Sword swings hit in the direction your warden is facing. Press again near the end of a swing to continue the combo. The third attack is stronger, while the heavy cleave does more damage but takes longer to recover.

## Enemies

Each wave sends enemies toward the outpost at different times, so you can choose which threat to handle first.

- **Raiders** are the standard melee enemy.
- **Scouts** move quickly and chase you from farther away.
- **Brutes** are slow, tough, and hit hard.
- **Archers** keep their distance and fire arrows.
- **Shamans** cast powerful magic from a safe range.
- **Warlords** lead every fifth wave and resist interruption. Defeat them for 120 crystals and the next weapon tier, then equip it with `Tab`.

Red and gold warnings show where an enemy is about to attack. Step away from the warning or interrupt the enemy with your sword. Gold numbers show damage you deal, red numbers show damage you take, and teal text shows rewards.

## Upgrades

Defeated enemies drop crystals. Spend them at the workshop near the outpost:

- **Outpost:** increases its maximum integrity and repairs it completely.
- **Armor:** reduces damage taken from enemy attacks.
- **Character:** increases sword damage and maximum health.

Stay near the outpost when you need to recover. The safe area heals your warden over time.

## Playing from the source code

If you want to run the project from source, install JDK 25 or newer, clone the repository, and run the Gradle wrapper from the project folder:

```sh
git clone https://github.com/Chris020606/JavaGame.git
cd JavaGame
./gradlew run
```

On Windows, use `gradlew.bat run` instead. The first run downloads the required dependencies. To run the checks used by the project, use `./gradlew test`.

## Credits

The character, weapon, and animation assets come from KayKit and are included under their CC0 license. The map models and sound effects were made for this project.

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for the licenses of the game engine and included assets.

Have fun defending the outpost.
