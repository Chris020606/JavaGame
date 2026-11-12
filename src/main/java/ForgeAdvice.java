import Entities.ArmorSlot;

/** Purchase explanations use the same class, level and currency rules as the forge. */
public final class ForgeAdvice {
    public static String armor(Progression p, HeroClass mentor, ArmorSlot slot, int tier) {
        if (p.ownsArmor(slot, tier)) return "Owned / equip this piece from your inventory.";
        if (mentor != p.heroClass()) return "Visit your own class mentor to forge this piece.";
        if (!p.hasWeapon()) return "Receive your starter weapon from your mentor first.";
        if (p.characterLevel() < Progression.requiredLevel(tier))
            return "Requires level "
                    + Progression.requiredLevel(tier)
                    + " / your level: "
                    + p.characterLevel();
        int missing = p.armorPrice(slot, tier) - p.crystals();
        if (missing > 0) return "Need " + missing + " more crystals to forge this piece.";
        int gain = p.armorBonus(slot, tier) - p.armorBonus(slot, p.armorTier(slot));
        return "Forge this piece / "
                + (gain >= 0 ? "+" : "")
                + gain
                + " defense vs equipped / equip in inventory.";
    }

    private ForgeAdvice() {}
}
