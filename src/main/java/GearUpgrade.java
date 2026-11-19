import Entities.ArmorSlot;

/** Only owned gear with a strict gain over the equipped slot counts as an upgrade. */
public final class GearUpgrade {
    public static boolean isUpgrade(Progression p, int item) {
        if (item < 0 || item >= 16) return false;
        if (item < 4) return p.ownsWeapon(item) && p.weaponDamage(item) > p.damage();
        ArmorSlot slot = ArmorSlot.values()[(item - 4) % 4];
        int tier = 1 + (item - 4) / 4;
        return p.ownsArmor(slot, tier)
                && p.armorBonus(slot, tier) > p.armorBonus(slot, p.armorTier(slot));
    }

    public static int count(Progression p) {
        int count = 0;
        for (int item = 0; item < 16; item++) if (isUpgrade(p, item)) count++;
        return count;
    }

    private GearUpgrade() {}
}
