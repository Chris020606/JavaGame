import java.util.Properties;

/** A retry rewinds the encounter, never the player's owned and selected equipment. */
final class RetryLoadout {
    static Progression preserve(Progression checkpoint, Progression current) {
        if (checkpoint.heroClass() != current.heroClass())
            throw new IllegalArgumentException("Checkpoint belongs to another class");
        Properties restored = new Properties(), equipment = new Properties();
        checkpoint.store(restored);
        current.store(equipment);
        for (String key : equipment.stringPropertyNames()) {
            if (key.equals("armorUnlocked")
                    || key.equals("ownedArmor")
                    || key.startsWith("ownedArmor.")
                    || key.startsWith("equipped.")
                    || key.equals("ownedWeapons")
                    || key.equals("weaponUnlocked")
                    || key.equals("weaponTier")
                    || key.equals("hasWeapon"))
                restored.setProperty(key, equipment.getProperty(key));
        }
        // Keeping purchased gear must not also refund the crystals spent on it.
        restored.setProperty("crystals", "" + Math.min(checkpoint.crystals(), current.crystals()));
        return Progression.restore(restored);
    }

    private RetryLoadout() {}
}
