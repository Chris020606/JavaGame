import Entities.ArmorSlot;
import Entities.Difficulty;

/** Rules and economy kept independent of rendering so they can be tested. */
public class Progression {
    public void store(java.util.Properties p) {
        p.setProperty("unlockedChapter", Integer.toString(unlockedChapter));
        expedition.store(p);
        fieldEvent.store(p);
        recoveryWisps.store(p);
        chapterFeats.store(p);
        p.setProperty("difficulty", difficulty.name());
        p.setProperty("vitality", vitality + "");
        p.setProperty("power", power + "");
        p.setProperty("endurance", endurance + "");
        p.setProperty("recovery", recovery + "");
        p.setProperty("agility", agility + "");
        p.setProperty("armorUnlocked", armorUnlocked + "");
        p.setProperty("ownedArmor", ownedArmor + "");
        for (ArmorSlot slot : ArmorSlot.values())
            p.setProperty("ownedArmor." + slot, "" + armorOwnership[slot.ordinal()]);
        p.setProperty("ownedWeapons", ownedWeapons + "");
        for (ArmorSlot slot : ArmorSlot.values())
            p.setProperty("equipped." + slot, armor[slot.ordinal()] + "");
        p.setProperty("weaponUnlocked", weaponUnlocked + "");
        p.setProperty("weaponTier", weaponTier + "");
        p.setProperty("hasWeapon", hasWeapon + "");
        p.setProperty("heroClass", heroClass.name());
        p.setProperty("potions", potions + "");
        p.setProperty("crystals", crystals + "");
        p.setProperty("kills", kills + "");
        p.setProperty("baseLevel", baseLevel + "");
        p.setProperty("armorLevel", armorLevel + "");
        p.setProperty("characterLevel", characterLevel + "");
        p.setProperty("xp", xp + "");
        p.setProperty("levelPoints", levelPoints + "");
        p.setProperty("spireLevel", spireLevel + "");
        p.setProperty("sanctuaryAuraLevel", sanctuaryAuraLevel + "");
        p.setProperty("solarWardLevel", solarWardLevel + "");
        for (int i = 0; i < caravanRanks.length; i++)
            p.setProperty("caravan." + i, "" + caravanRanks[i]);
        p.setProperty("rescuedSurvivors", rescuedSurvivors + "");
        p.setProperty("health", health + "");
        p.setProperty("baseHealth", baseHealth + "");
    }

    public static Progression restore(java.util.Properties p) {
        Progression result =
                new Progression(
                        HeroClass.valueOf(p.getProperty("heroClass", "WARRIOR")),
                        Difficulty.valueOf(p.getProperty("difficulty", "NORMAL")));
        result.unlockedChapter = optional(p, "unlockedChapter", 0, 3, 0);
        result.expedition = Expedition.restore(p);
        result.fieldEvent = FieldEvent.restore(p);
        result.recoveryWisps = RecoveryWisps.restore(p);
        result.chapterFeats = ChapterFeats.restore(p);
        result.potions = p.containsKey("potions") ? SaveGame.integer(p, "potions", 0, 5) : 2;
        result.weaponUnlocked =
                p.containsKey("weaponUnlocked") ? SaveGame.integer(p, "weaponUnlocked", 0, 3) : 0;
        result.weaponTier =
                p.containsKey("weaponTier")
                        ? SaveGame.integer(p, "weaponTier", 0, result.weaponUnlocked)
                        : 0;
        result.hasWeapon =
                p.containsKey("hasWeapon")
                        ? Boolean.parseBoolean(p.getProperty("hasWeapon"))
                        : (result.characterLevel > 1
                                || result.kills > 0
                                || result.weaponTier > 0
                                || result.weaponUnlocked > 0);
        result.crystals = SaveGame.integer(p, "crystals", 0, 100000000);
        result.kills = SaveGame.integer(p, "kills", 0, 100000000);
        result.baseLevel = SaveGame.integer(p, "baseLevel", 1, 1000);
        result.armorLevel = SaveGame.integer(p, "armorLevel", 1, 1000);
        result.characterLevel = SaveGame.integer(p, "characterLevel", 1, 1000);
        result.xp = optional(p, "xp", 0, 10000000, 0);
        result.levelPoints = optional(p, "levelPoints", 0, 1000, 0);
        result.spireLevel = optional(p, "spireLevel", 1, 10, 1);
        result.sanctuaryAuraLevel = optional(p, "sanctuaryAuraLevel", 1, 10, 1);
        result.solarWardLevel = optional(p, "solarWardLevel", 0, 10, 0);
        result.rescuedSurvivors = optional(p, "rescuedSurvivors", 0, 10000, 0);
        result.vitality = optional(p, "vitality", 1, 10, 1);
        result.power = optional(p, "power", 1, 10, 1);
        result.endurance = optional(p, "endurance", 1, 10, 1);
        result.recovery = optional(p, "recovery", 1, 10, 1);
        result.agility = optional(p, "agility", 1, 10, 1);
        result.armorUnlocked = optional(p, "armorUnlocked", 0, 3, 0);
        for (ArmorSlot slot : ArmorSlot.values())
            result.armor[slot.ordinal()] =
                    optional(p, "equipped." + slot, 0, result.armorUnlocked, 0);
        int legacyArmor = (1 << (result.armorUnlocked + 1)) - 1;
        result.ownedArmor =
                p.containsKey("armorUnlocked")
                        ? optional(p, "ownedArmor", 1, 15, legacyArmor)
                        : legacyArmor;
        result.ownedWeapons =
                p.containsKey("weaponUnlocked")
                        ? optional(p, "ownedWeapons", 0, 15, (1 << (result.weaponUnlocked + 1)) - 1)
                        : (result.hasWeapon ? 1 : 0);
        if ((result.ownedArmor & 1) == 0
                || (result.hasWeapon && (result.ownedWeapons & 1) == 0)
                || (result.hasWeapon && !result.ownsWeapon(result.weaponTier))
                || (result.hasWeapon && result.weaponUnlocked != highest(result.ownedWeapons))
                || result.armorUnlocked != highest(result.ownedArmor))
            throw new IllegalArgumentException("Invalid equipment ownership");
        int union = 1;
        for (ArmorSlot slot : ArmorSlot.values()) {
            int mask = optional(p, "ownedArmor." + slot, 1, 15, result.ownedArmor);
            if ((mask & 1) == 0)
                throw new IllegalArgumentException("Missing starter armor ownership");
            result.armorOwnership[slot.ordinal()] = mask;
            union |= mask;
            if (!result.ownsArmor(slot, result.armor[slot.ordinal()]))
                throw new IllegalArgumentException("Equipped armor is not owned");
        }
        if (union != result.ownedArmor)
            throw new IllegalArgumentException("Inconsistent armor ownership");
        // Accept healthy older saves from the former +25 HP/level curve, then clamp to the new cap.
        for (int i = 0; i < result.caravanRanks.length; i++)
            result.caravanRanks[i] = optional(p, "caravan." + i, 0, 5, 0);
        int legacyMaxHealth =
                result.heroClass.health
                        + (result.characterLevel - 1) * 25
                        + (result.vitality - 1) * 20;
        result.health =
                Math.min(
                        result.maxHealth(),
                        SaveGame.number(
                                p, "health", .001f, legacyMaxHealth + result.caravanRanks[0] * 5));
        result.baseHealth = SaveGame.number(p, "baseHealth", .001f, result.maxBaseHealth());
        return result;
    }

    private static int optional(
            java.util.Properties p, String key, int min, int max, int fallback) {
        return p.containsKey(key) ? SaveGame.integer(p, key, min, max) : fallback;
    }

    public enum Upgrade {
        BASE,
        ARMOR,
        CHARACTER,
        VITALITY,
        POWER,
        ENDURANCE,
        RECOVERY,
        AGILITY,
        SPIRE,
        HEARTH,
        SOLAR_WARD
    }

    public static final Upgrade[] TRAINING = {
        Upgrade.VITALITY,
        Upgrade.POWER,
        Upgrade.ENDURANCE,
        Upgrade.RECOVERY,
        Upgrade.AGILITY,
        Upgrade.ARMOR,
        Upgrade.BASE
    };
    public static final Upgrade[] ATTRIBUTES = {
        Upgrade.VITALITY,
        Upgrade.POWER,
        Upgrade.ENDURANCE,
        Upgrade.RECOVERY,
        Upgrade.AGILITY,
        Upgrade.ARMOR
    };
    public static final Upgrade[] BASE_UPGRADES = {
        Upgrade.BASE, Upgrade.SPIRE, Upgrade.HEARTH, Upgrade.SOLAR_WARD
    };
    private int vitality = 1, power = 1, endurance = 1, recovery = 1, agility = 1, armorUnlocked;
    private final int[] armor = new int[4];
    private final int[] armorOwnership = {1, 1, 1, 1};

    public boolean ownsArmor(ArmorSlot slot, int tier) {
        return tier >= 0 && tier <= 3 && (armorOwnership[slot.ordinal()] & (1 << tier)) != 0;
    }

    public int armorPrice(ArmorSlot slot, int tier) {
        return Math.round(
                armorPrice(tier)
                        * (slot == ArmorSlot.CHEST ? .4f : slot == ArmorSlot.LEGS ? .3f : .2f));
    }

    public boolean canBuyArmor(ArmorSlot slot, int tier) {
        return tier >= 1
                && tier <= 3
                && hasWeapon
                && !defeated()
                && !ownsArmor(slot, tier)
                && characterLevel >= requiredLevel(tier)
                && crystals >= armorPrice(slot, tier);
    }

    public boolean buyArmor(ArmorSlot slot, int tier) {
        if (!canBuyArmor(slot, tier)) return false;
        crystals -= armorPrice(slot, tier);
        armorOwnership[slot.ordinal()] |= 1 << tier;
        ownedArmor |= 1 << tier;
        armorUnlocked = Math.max(armorUnlocked, tier);
        return true;
    }

    private int ownedWeapons = 1, ownedArmor = 1, lastBossWeapon = -1;

    private static int highest(int mask) {
        return 31 - Integer.numberOfLeadingZeros(mask);
    }

    public boolean ownsWeapon(int tier) {
        return hasWeapon && tier >= 0 && tier <= 3 && (ownedWeapons & (1 << tier)) != 0;
    }

    public boolean ownsArmor(int tier) {
        return tier >= 0 && tier <= 3 && (ownedArmor & (1 << tier)) != 0;
    }

    public int lastBossWeapon() {
        return lastBossWeapon;
    }

    // This is a chapter index (0..3), not a wave number. Older saves infer it on load.
    private int unlockedChapter;

    public int unlockedChapter() {
        return unlockedChapter;
    }

    public void unlockChapter(int chapter) {
        unlockedChapter = Math.max(unlockedChapter, Math.min(3, Math.max(0, chapter)));
    }

    private Expedition expedition = new Expedition();
    private FieldEvent fieldEvent = new FieldEvent();
    private RecoveryWisps recoveryWisps = new RecoveryWisps();
    private ChapterFeats chapterFeats = new ChapterFeats();
    private int rescuedSurvivors;

    public int rescuedSurvivors() {
        return rescuedSurvivors;
    }

    public void rescueSurvivor() {
        rescuedSurvivors++;
    }

    public ChapterFeats chapterFeats() {
        return chapterFeats;
    }

    public int rewardChapterFeats(int chapter) {
        int reward = chapterFeats.claim(chapter);
        crystals += reward;
        return reward;
    }

    public RecoveryWisps recoveryWisps() {
        return recoveryWisps;
    }

    public Expedition expedition() {
        return expedition;
    }

    public FieldEvent fieldEvent() {
        return fieldEvent;
    }

    public void restoreHealth(float amount) {
        if (!defeated()) health = Math.min(maxHealth(), health + Math.max(0, amount));
    }

    public void rewardObjective() {
        if (!defeated()) {
            expedition.objectiveCompleted();
            crystals += 65;
            if (potions < 5) potions++;
            restoreHealth(15);
        }
    }

    private final Difficulty difficulty;
    private int crystals, kills, baseLevel = 1, armorLevel = 1, characterLevel = 1;
    private int xp, levelPoints, spireLevel = 1, sanctuaryAuraLevel = 1, solarWardLevel = 0;
    private final HeroClass heroClass;
    private int potions = 2, weaponUnlocked, weaponTier;
    private boolean hasWeapon = true;
    private float health, baseHealth = 400;

    public int xp() {
        return xp;
    }

    public int levelPoints() {
        return levelPoints;
    }

    public void grantLevelPoints(int points) {
        if (points > 0 && !defeated()) levelPoints = Math.min(1000, levelPoints + points);
    }

    public int xpToNextLevel() {
        return 100 + (characterLevel - 1) * 75;
    }

    public boolean addXp(int amount) {
        if (amount <= 0 || defeated() || characterLevel >= 1000) return false;
        // Use a long accumulator so a large award cannot wrap XP negative.
        long earned = (long) xp + amount;
        boolean leveledUp = false;
        while (characterLevel < 1000 && earned >= xpToNextLevel()) {
            earned -= xpToNextLevel();
            characterLevel++;
            levelPoints = Math.min(1000, levelPoints + 1);
            health = maxHealth();
            leveledUp = true;
        }
        xp = characterLevel == 1000 ? 0 : (int) earned;
        return leveledUp;
    }

    public int spireLevel() {
        return spireLevel;
    }

    public int sanctuaryAuraLevel() {
        return sanctuaryAuraLevel;
    }

    public int solarWardLevel() {
        return solarWardLevel;
    }

    public Progression() {
        this(HeroClass.WARRIOR);
    }

    public Progression(HeroClass heroClass) {
        this(heroClass, Difficulty.NORMAL);
    }

    public Progression(HeroClass heroClass, Difficulty difficulty) {
        this.heroClass = heroClass;
        this.difficulty = difficulty;
        health = maxHealth();
    }

    /** Separate sandbox loadout; grants equipment without fabricated kills or boss rewards. */
    public static Progression playtest(HeroClass hero, Difficulty difficulty, boolean maxTraining) {
        Progression p = new Progression(hero, difficulty);
        p.crystals = 100000;
        p.levelPoints = 50;
        p.potions = 5;
        p.weaponUnlocked = p.weaponTier = p.armorUnlocked = 3;
        p.hasWeapon = true;
        p.ownedWeapons = p.ownedArmor = 15;
        java.util.Arrays.fill(p.armorOwnership, 15);
        java.util.Arrays.fill(p.armor, 3);
        if (maxTraining) {
            p.vitality =
                    p.power =
                            p.endurance =
                                    p.recovery =
                                            p.agility =
                                                    p.baseLevel =
                                                            p.armorLevel =
                                                                    p.spireLevel =
                                                                            p.sanctuaryAuraLevel =
                                                                                    p.solarWardLevel =
                                                                                            10;
        }
        p.health = p.maxHealth();
        p.baseHealth = p.maxBaseHealth();
        return p;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public int armorUnlocked() {
        return armorUnlocked;
    }

    public int armorTier(ArmorSlot slot) {
        return armor[slot.ordinal()];
    }

    public int armorBonus(ArmorSlot slot, int tier) {
        return slot.protection * tier;
    }

    public String armorName(ArmorSlot slot, int tier) {
        return (tier == 0 ? "Traveler" : tier == 1 ? "Iron" : tier == 2 ? "Runic" : "Dawnsteel")
                + " "
                + slot.label;
    }

    public boolean equipArmor(ArmorSlot slot, int tier) {
        if (!ownsArmor(slot, tier) || defeated()) return false;
        armor[slot.ordinal()] = tier;
        return true;
    }

    public int weaponPrice(int tier) {
        return switch (tier) {
            case 1 -> 140;
            case 2 -> 400;
            case 3 -> 850;
            default -> 0;
        };
    }

    public int armorPrice(int tier) {
        return switch (tier) {
            case 1 -> 130;
            case 2 -> 380;
            case 3 -> 800;
            default -> 0;
        };
    }

    public static int requiredLevel(int tier) {
        return switch (tier) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 10;
            default -> 1;
        };
    }

    public boolean canBuyWeapon(int tier) {
        return tier >= 1
                && tier <= 3
                && !ownsWeapon(tier)
                && crystals >= weaponPrice(tier)
                && characterLevel >= requiredLevel(tier)
                && hasWeapon
                && !defeated();
    }

    public boolean canBuyArmor(int tier) {
        return tier >= 1
                && tier <= 3
                && !ownsArmor(tier)
                && crystals >= armorPrice(tier)
                && characterLevel >= requiredLevel(tier)
                && hasWeapon
                && !defeated();
    }

    public boolean buyWeapon(int tier) {
        if (!hasWeapon
                || tier < 1
                || tier > 3
                || ownsWeapon(tier)
                || crystals < weaponPrice(tier)
                || defeated()) return false;
        crystals -= weaponPrice(tier);
        ownedWeapons |= 1 << tier;
        weaponUnlocked = Math.max(weaponUnlocked, tier);
        return true;
    }

    public boolean buyArmor(int tier) {
        if (!hasWeapon
                || tier < 1
                || tier > 3
                || ownsArmor(tier)
                || crystals < armorPrice(tier)
                || defeated()) return false;
        crystals -= armorPrice(tier);
        ownedArmor |= 1 << tier;
        for (ArmorSlot slot : ArmorSlot.values()) armorOwnership[slot.ordinal()] |= 1 << tier;
        armorUnlocked = Math.max(armorUnlocked, tier);
        return true;
    }

    public String resourceLabel() {
        return heroClass == HeroClass.MAGE ? "MANA" : "STAMINA";
    }

    private final int[] caravanRanks = new int[3];

    public int caravanRank(int item) {
        return caravanRanks[item];
    }

    public int caravanPrice(int item) {
        return 180 + caravanRanks[item] * 140;
    }

    public boolean canBuyCaravan(int item) {
        return item >= 0
                && item < 3
                && !defeated()
                && caravanRanks[item] < 5
                && crystals >= caravanPrice(item);
    }

    public boolean buyCaravan(int item) {
        if (!canBuyCaravan(item)) return false;
        crystals -= caravanPrice(item);
        caravanRanks[item]++;
        return true;
    }

    public int maxStamina() {
        return 100 + (endurance - 1) * 15 + caravanRanks[1] * 5;
    }

    public int staminaRecovery() {
        return 22 + (recovery - 1) * 3 + caravanRanks[2];
    }

    public float movementSpeed() {
        return heroClass.speed * (1 + (agility - 1) * .04f) + armorTier(ArmorSlot.BOOTS) * .12f;
    }

    public HeroClass heroClass() {
        return heroClass;
    }

    public int potions() {
        return potions;
    }

    public boolean usePotion() {
        if (potions == 0 || defeated() || health >= maxHealth()) return false;
        potions--;
        health = Math.min(maxHealth(), health + 60);
        return true;
    }

    public boolean buyPotion() {
        if (potions >= 5 || crystals < 30 || defeated()) return false;
        crystals -= 30;
        potions++;
        return true;
    }

    public boolean hasWeapon() {
        return hasWeapon;
    }

    public void setHasWeapon(boolean hasWeapon) {
        this.hasWeapon = hasWeapon;
        if (!hasWeapon) {
            this.weaponTier = 0;
            this.ownedWeapons &= ~1;
        }
    }

    public void grantStarterWeapon() {
        this.hasWeapon = true;
        this.weaponTier = 0;
        this.weaponUnlocked = Math.max(this.weaponUnlocked, 0);
        this.ownedWeapons |= 1;
    }

    public int weaponTier() {
        return weaponTier;
    }

    public int weaponUnlocked() {
        return weaponUnlocked;
    }

    public int weaponBonus(int tier) {
        return tier * 8;
    }

    public String weaponName(int tier) {
        String quality =
                switch (tier) {
                    case 1 -> "Forged";
                    case 2 -> "Runic";
                    case 3 -> "Legendary";
                    default -> "Starter";
                };
        return quality
                + " "
                + switch (heroClass) {
                    case WARRIOR ->
                            tier == 1
                                    ? "Longsword"
                                    : tier == 2 ? "Waraxe" : tier == 3 ? "Dawnblade" : "Sword";
                    case ARCHER ->
                            tier == 2 ? "Hand Crossbow" : tier == 3 ? "Siege Crossbow" : "Bow";
                    case MAGE -> tier == 3 ? "Dawnweaver" : tier == 1 ? "Wand" : "Staff";
                };
    }

    public boolean equipWeapon(int tier) {
        if (!ownsWeapon(tier) || defeated()) return false;
        weaponTier = tier;
        hasWeapon = true;
        return true;
    }

    public static int enemyXp(boolean boss, boolean elite) {
        return boss ? 300 : elite ? 85 : 25;
    }

    public void rewardEnemy(boolean boss) {
        // Award through the shared reward path, not just through floating combat text.
        addXp(enemyXp(boss, false));
        kills++;
        hasWeapon = true;
        crystals += boss ? 120 : 15;
        restoreHealth(expedition.rank(Expedition.Blessing.BLOODROOT) * 3);
        if (boss) lastBossWeapon = -1;
    }

    public void rewardElite() {
        if (!defeated()) {
            addXp(enemyXp(false, true) - enemyXp(false, false));
            crystals += 25;
        }
    }

    public int crystals() {
        return crystals;
    }

    public int kills() {
        return kills;
    }

    public int baseLevel() {
        return baseLevel;
    }

    public int armorLevel() {
        return armorLevel;
    }

    public int characterLevel() {
        return characterLevel;
    }

    // Early levels establish the class; later growth is steadier so gear and tactics still matter.
    private static int powerGrowth(int level) {
        return Math.min(level - 1, 2) * 10 + Math.max(0, level - 3) * 3;
    }

    private static int healthGrowth(int level) {
        return Math.min(level - 1, 2) * 25 + Math.max(0, level - 3) * 10;
    }

    public int nextLevelHealthGain() {
        return characterLevel >= 1000
                ? 0
                : healthGrowth(characterLevel + 1) - healthGrowth(characterLevel);
    }

    public int nextLevelDamageGain() {
        return characterLevel >= 1000
                ? 0
                : hasWeapon ? powerGrowth(characterLevel + 1) - powerGrowth(characterLevel) : 2;
    }

    public int nextLevelHealth() {
        return maxHealth() + nextLevelHealthGain();
    }

    public int nextLevelDamage() {
        return damage() + nextLevelDamageGain();
    }

    public int maxHealth() {
        return heroClass.health
                + healthGrowth(characterLevel)
                + (vitality - 1) * 20
                + caravanRanks[0] * 5;
    }

    public int maxBaseHealth() {
        return 400 + (baseLevel - 1) * 100;
    }

    public int damage() {
        if (!hasWeapon) return 5 + (characterLevel - 1) * 2;
        return weaponDamage(weaponTier);
    }

    /** Hypothetical equipped power for inventory comparison; does not grant or equip an item. */
    public int weaponDamage(int tier) {
        if (tier < 0 || tier > 3) throw new IllegalArgumentException("Invalid weapon tier");
        return 25 + powerGrowth(characterLevel) + weaponBonus(tier) + (power - 1) * 5;
    }

    public int defense() {
        int total = (armorLevel - 1) * 2;
        for (ArmorSlot slot : ArmorSlot.values()) total += armorBonus(slot, armorTier(slot));
        return total;
    }

    public float health() {
        return health;
    }

    public float baseHealth() {
        return baseHealth;
    }

    public boolean defeated() {
        return health <= 0 || baseHealth <= 0;
    }

    public void rewardKill() {
        rewardEnemy(false);
    }

    public int level(Upgrade type) {
        return switch (type) {
            case BASE -> baseLevel;
            case ARMOR -> armorLevel;
            case CHARACTER -> characterLevel;
            case VITALITY -> vitality;
            case POWER -> power;
            case ENDURANCE -> endurance;
            case RECOVERY -> recovery;
            case AGILITY -> agility;
            case SPIRE -> spireLevel;
            case HEARTH -> sanctuaryAuraLevel;
            case SOLAR_WARD -> solarWardLevel;
        };
    }

    public int cost(Upgrade type) {
        int lv = level(type);
        return switch (type) {
            case BASE, ARMOR, CHARACTER -> 45 * lv;
            case SPIRE -> 65 * lv;
            case HEARTH -> 45 * lv;
            case SOLAR_WARD -> 85 * Math.max(1, lv);
            default -> 45 * lv + 10 * (lv - 1) * (lv - 1);
        };
    }

    public int maxLevel(Upgrade type) {
        return switch (type) {
            case CHARACTER -> 1000;
            case SOLAR_WARD -> 5;
            default -> 10;
        };
    }

    public String upgradeLabel(Upgrade type) {
        return switch (type) {
            case BASE -> "BEACON INTEGRITY";
            case ARMOR -> "RESILIENCE";
            case CHARACTER -> "WARDEN";
            case VITALITY -> "VITALITY";
            case POWER -> "POWER";
            case ENDURANCE -> "ENDURANCE";
            case RECOVERY -> "RECOVERY";
            case AGILITY -> "AGILITY";
            case SPIRE -> "LIGHT SPIRE TURRET";
            case HEARTH -> "WARMTH HEARTH";
            case SOLAR_WARD -> "SOLAR NOVA WARD";
        };
    }

    public String upgradeEffect(Upgrade type) {
        return switch (type) {
            case BASE -> maxBaseHealth() + " > " + (maxBaseHealth() + 100) + " integrity";
            case ARMOR -> defense() + " > " + (defense() + 2) + " defense";
            case CHARACTER -> "+25 HP / +10 damage";
            case VITALITY -> maxHealth() + " > " + (maxHealth() + 20) + " max HP";
            case POWER -> damage() + " > " + (damage() + 5) + " damage";
            case ENDURANCE ->
                    maxStamina()
                            + " > "
                            + (maxStamina() + 15)
                            + " "
                            + resourceLabel().toLowerCase(java.util.Locale.ROOT);
            case RECOVERY ->
                    staminaRecovery()
                            + " > "
                            + (staminaRecovery() + 3)
                            + " "
                            + resourceLabel().toLowerCase(java.util.Locale.ROOT)
                            + " / s";
            case AGILITY ->
                    String.format(
                            "%.1f > %.1f movement",
                            movementSpeed(), movementSpeed() + heroClass.speed * .04f);
            case SPIRE ->
                    "+"
                            + (4 + spireLevel * 3)
                            + " beam dmg / "
                            + String.format("%.1fs", SanctuaryWard.interval(spireLevel + 1))
                            + " rate";
            case HEARTH ->
                    Math.round(healRate())
                            + " > "
                            + Math.round(healRate() + 6)
                            + " HP / s near beacon";
            case SOLAR_WARD ->
                    solarWardLevel == 0
                            ? "UNLOCKS 35% HP shockwave blast"
                            : "Shockwave +15 dmg / +1m knockback";
        };
    }

    public boolean usesLevelPoint(Upgrade type) {
        return switch (type) {
            case VITALITY, POWER, ENDURANCE, RECOVERY, AGILITY -> true;
            default -> false;
        };
    }

    public boolean canPurchase(Upgrade type) {
        if (defeated() || level(type) >= maxLevel(type)) return false;
        if (usesLevelPoint(type)) return levelPoints >= 1;
        return crystals >= cost(type);
    }

    public boolean purchase(Upgrade type) {
        if (!canPurchase(type)) return false;
        if (usesLevelPoint(type)) levelPoints--;
        else crystals -= cost(type);
        switch (type) {
            case BASE -> {
                baseLevel++;
                baseHealth = maxBaseHealth();
            }
            case ARMOR -> armorLevel++;
            case VITALITY -> {
                vitality++;
                health = Math.min(maxHealth(), health + 20);
            }
            case POWER -> power++;
            case ENDURANCE -> endurance++;
            case RECOVERY -> recovery++;
            case AGILITY -> agility++;
            case CHARACTER -> {
                characterLevel++;
                health = maxHealth();
            }
            case SPIRE -> spireLevel++;
            case HEARTH -> sanctuaryAuraLevel++;
            case SOLAR_WARD -> solarWardLevel++;
        }
        return true;
    }

    public void damagePlayer(float amount) {
        if (amount > 0) health = Math.max(0, health - Math.max(1, amount - defense()));
    }

    public void damageBase(float amount) {
        baseHealth =
                Math.max(
                        0,
                        baseHealth
                                - Math.max(0, amount)
                                        * (1
                                                - .12f
                                                        * expedition.rank(
                                                                Expedition.Blessing.BULWARK)));
    }

    public boolean repairBase() {
        int cost = 30, amount = 100;
        if (defeated() || baseHealth >= maxBaseHealth() || crystals < cost) return false;
        crystals -= cost;
        baseHealth = Math.min(maxBaseHealth(), baseHealth + amount);
        return true;
    }

    public float healRate() {
        return 10 + baseLevel * 2 + (sanctuaryAuraLevel - 1) * 6;
    }

    public void heal(float dt) {
        health = Math.min(maxHealth(), health + Math.max(0, dt) * healRate());
    }

    public void chapterRespite() {
        if (!defeated())
            baseHealth =
                    Math.min(maxBaseHealth(), baseHealth + Math.max(100, maxBaseHealth() * .25f));
    }

    public boolean restoreBase(float amount) {
        if (defeated() || !Float.isFinite(amount) || amount <= 0 || baseHealth >= maxBaseHealth())
            return false;
        baseHealth = Math.min(maxBaseHealth(), baseHealth + amount);
        return true;
    }
}
