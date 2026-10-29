import Entities.Difficulty;
import Entities.ArmorSlot;
/** Rules and economy kept independent of rendering so they can be tested. */
public class Progression {
    public void store(java.util.Properties p) {
        p.setProperty("difficulty",difficulty.name());
        p.setProperty("vitality",vitality+"");p.setProperty("power",power+"");p.setProperty("endurance",endurance+"");p.setProperty("recovery",recovery+"");p.setProperty("agility",agility+"");
        p.setProperty("armorUnlocked",armorUnlocked+"");
        for(ArmorSlot slot:ArmorSlot.values()) p.setProperty("equipped."+slot,armor[slot.ordinal()]+"");
        p.setProperty("weaponUnlocked",weaponUnlocked+""); p.setProperty("weaponTier",weaponTier+"");
        p.setProperty("heroClass",heroClass.name()); p.setProperty("potions",potions+"");
        p.setProperty("crystals",crystals+""); p.setProperty("kills",kills+"");
        p.setProperty("baseLevel",baseLevel+""); p.setProperty("armorLevel",armorLevel+""); p.setProperty("characterLevel",characterLevel+"");
        p.setProperty("health",health+""); p.setProperty("baseHealth",baseHealth+"");
    }
    public static Progression restore(java.util.Properties p) {
        Progression result=new Progression(HeroClass.valueOf(p.getProperty("heroClass","WARRIOR")),Difficulty.valueOf(p.getProperty("difficulty","NORMAL")));
        result.potions=p.containsKey("potions")?SaveGame.integer(p,"potions",0,5):2;
        result.weaponUnlocked=p.containsKey("weaponUnlocked")?SaveGame.integer(p,"weaponUnlocked",0,3):0;
        result.weaponTier=p.containsKey("weaponTier")?SaveGame.integer(p,"weaponTier",0,result.weaponUnlocked):0;
        result.crystals=SaveGame.integer(p,"crystals",0,100000000); result.kills=SaveGame.integer(p,"kills",0,100000000);
        result.baseLevel=SaveGame.integer(p,"baseLevel",1,1000); result.armorLevel=SaveGame.integer(p,"armorLevel",1,1000);
        result.characterLevel=SaveGame.integer(p,"characterLevel",1,1000);
        result.vitality=optional(p,"vitality",1,10,1);result.power=optional(p,"power",1,10,1);
        result.endurance=optional(p,"endurance",1,10,1);result.recovery=optional(p,"recovery",1,10,1);result.agility=optional(p,"agility",1,10,1);
        result.armorUnlocked=optional(p,"armorUnlocked",0,3,0);
        for(ArmorSlot slot:ArmorSlot.values()) result.armor[slot.ordinal()]=optional(p,"equipped."+slot,0,result.armorUnlocked,0);
        result.health=SaveGame.number(p,"health",.001f,result.maxHealth()); result.baseHealth=SaveGame.number(p,"baseHealth",.001f,result.maxBaseHealth());
        return result;
    }
    private static int optional(java.util.Properties p,String key,int min,int max,int fallback) { return p.containsKey(key)?SaveGame.integer(p,key,min,max):fallback; }
    public enum Upgrade { BASE, ARMOR, CHARACTER, VITALITY, POWER, ENDURANCE, RECOVERY, AGILITY }
    public static final Upgrade[] TRAINING={Upgrade.VITALITY,Upgrade.POWER,Upgrade.ENDURANCE,Upgrade.RECOVERY,Upgrade.AGILITY,Upgrade.ARMOR,Upgrade.BASE};
    private int vitality=1,power=1,endurance=1,recovery=1,agility=1,armorUnlocked;
    private final int[] armor=new int[4];
    private final Difficulty difficulty;
    private int crystals, kills, baseLevel = 1, armorLevel = 1, characterLevel = 1;
    private final HeroClass heroClass;
    private int potions=2, weaponUnlocked, weaponTier;
    private float health, baseHealth = 250;
    public Progression() { this(HeroClass.WARRIOR); }
    public Progression(HeroClass heroClass) { this(heroClass,Difficulty.NORMAL); }
    public Progression(HeroClass heroClass,Difficulty difficulty) { this.heroClass=heroClass;this.difficulty=difficulty;health=maxHealth(); }
    /** Separate sandbox loadout; grants equipment without fabricated kills or boss rewards. */
    public static Progression playtest(HeroClass hero,Difficulty difficulty,boolean maxTraining) {
        Progression p=new Progression(hero,difficulty);p.crystals=100000;p.potions=5;
        p.weaponUnlocked=p.weaponTier=p.armorUnlocked=3;
        java.util.Arrays.fill(p.armor,3);
        if(maxTraining) { p.vitality=p.power=p.endurance=p.recovery=p.agility=p.baseLevel=p.armorLevel=10; }
        p.health=p.maxHealth();p.baseHealth=p.maxBaseHealth();return p;
    }
    public Difficulty difficulty() { return difficulty; }
    public int armorUnlocked() { return armorUnlocked; }
    public int armorTier(ArmorSlot slot) { return armor[slot.ordinal()]; }
    public int armorBonus(ArmorSlot slot,int tier) { return slot.protection*tier; }
    public String armorName(ArmorSlot slot,int tier) { return (tier==0?"Traveler":tier==1?"Iron":tier==2?"Runic":"Dawnsteel")+" "+slot.label; }
    public boolean equipArmor(ArmorSlot slot,int tier) { if(tier<0 || tier>armorUnlocked || defeated()) return false;armor[slot.ordinal()]=tier;return true; }
    public int weaponPrice(int tier) { return switch(tier) { case 1 -> 100;case 2 -> 240;case 3 -> 450;default -> 0; }; }
    public int armorPrice(int tier) { return switch(tier) { case 1 -> 100;case 2 -> 220;case 3 -> 380;default -> 0; }; }
    public boolean buyWeapon(int tier) {
        if(tier<1 || tier>3 || tier<=weaponUnlocked || crystals<weaponPrice(tier) || defeated()) return false;
        crystals-=weaponPrice(tier);weaponUnlocked=tier;return true;
    }
    public boolean buyArmor(int tier) {
        if(tier<1 || tier>3 || tier<=armorUnlocked || crystals<armorPrice(tier) || defeated()) return false;
        crystals-=armorPrice(tier);armorUnlocked=tier;return true;
    }
    public int maxStamina() { return 100+(endurance-1)*15; }
    public int staminaRecovery() { return 22+(recovery-1)*3; }
    public float movementSpeed() { return heroClass.speed*(1+(agility-1)*.04f)+armorTier(ArmorSlot.BOOTS)*.12f; }
    public HeroClass heroClass() { return heroClass; }
    public int potions() { return potions; }
    public boolean usePotion() {
        if (potions==0 || defeated() || health>=maxHealth()) return false;
        potions--; health=Math.min(maxHealth(),health+60); return true;
    }
    public boolean buyPotion() {
        if (potions>=5 || crystals<30 || defeated()) return false;
        crystals-=30; potions++; return true;
    }
    public int weaponTier() { return weaponTier; }
    public int weaponUnlocked() { return weaponUnlocked; }
    public int weaponBonus(int tier) { return tier*8; }
    public String weaponName(int tier) {
        String quality=switch(tier) { case 1 -> "Forged"; case 2 -> "Runic"; case 3 -> "Legendary"; default -> "Starter"; };
        return quality+" "+switch(heroClass) { case WARRIOR -> tier==1?"Longsword":tier==2?"Waraxe":tier==3?"Dawnblade":"Sword"; case ARCHER -> tier>=2?"Crossbow":"Bow"; case MAGE -> tier==1?"Wand":"Staff"; };
    }
    public boolean equipWeapon(int tier) {
        if (tier<0 || tier>weaponUnlocked || defeated()) return false;
        weaponTier=tier; return true;
    }
    public void rewardEnemy(boolean boss) {
        kills++; crystals+=boss?120:15;
        if (boss) weaponUnlocked=Math.min(3,weaponUnlocked+1);
    }
    public int crystals() { return crystals; }
    public int kills() { return kills; }
    public int baseLevel() { return baseLevel; }
    public int armorLevel() { return armorLevel; }
    public int characterLevel() { return characterLevel+vitality+power+endurance+recovery+agility-5; }
    public int maxHealth() { return heroClass.health + (characterLevel - 1) * 25 + (vitality-1)*20; }
    public int maxBaseHealth() { return 250 + (baseLevel - 1) * 100; }
    public int damage() { return 25 + (characterLevel - 1) * 10 + weaponBonus(weaponTier)+(power-1)*5; }
    public int defense() { int total=(armorLevel-1)*2;for(ArmorSlot slot:ArmorSlot.values()) total+=armorBonus(slot,armorTier(slot));return total; }
    public float health() { return health; }
    public float baseHealth() { return baseHealth; }
    public boolean defeated() { return health <= 0 || baseHealth <= 0; }
    public void rewardKill() { rewardEnemy(false); }
    public int level(Upgrade type) { return switch (type) { case BASE -> baseLevel; case ARMOR -> armorLevel; case CHARACTER -> characterLevel;case VITALITY -> vitality;case POWER -> power;case ENDURANCE -> endurance;case RECOVERY -> recovery;case AGILITY -> agility; }; }
    public int cost(Upgrade type) { int lv=level(type);return switch(type) { case BASE,ARMOR,CHARACTER -> 45*lv;default -> 45*lv+10*(lv-1)*(lv-1); }; }
    public int maxLevel(Upgrade type) { return type==Upgrade.CHARACTER?1000:10; }
    public String upgradeLabel(Upgrade type) { return switch(type) { case BASE -> "SANCTUARY";case ARMOR -> "RESILIENCE";case CHARACTER -> "WARDEN";case VITALITY -> "VITALITY";case POWER -> "POWER";case ENDURANCE -> "ENDURANCE";case RECOVERY -> "RECOVERY";case AGILITY -> "AGILITY"; }; }
    public String upgradeEffect(Upgrade type) { return switch(type) { case BASE -> maxBaseHealth()+" > "+(maxBaseHealth()+100)+" integrity";case ARMOR -> defense()+" > "+(defense()+2)+" defense";case CHARACTER -> "+25 HP / +10 damage";case VITALITY -> maxHealth()+" > "+(maxHealth()+20)+" max HP";case POWER -> damage()+" > "+(damage()+5)+" damage";case ENDURANCE -> maxStamina()+" > "+(maxStamina()+15)+" stamina";case RECOVERY -> staminaRecovery()+" > "+(staminaRecovery()+3)+" stamina / s";case AGILITY -> String.format("%.1f > %.1f movement",movementSpeed(),movementSpeed()+heroClass.speed*.04f); }; }
    public boolean purchase(Upgrade type) {
        int cost = cost(type);
        if (crystals < cost || defeated() || level(type)>=maxLevel(type)) return false;
        crystals -= cost;
        switch (type) {
            case BASE -> { baseLevel++; baseHealth = maxBaseHealth(); }
            case ARMOR -> armorLevel++;
            case VITALITY -> { vitality++;health=Math.min(maxHealth(),health+20); }
            case POWER -> power++;
            case ENDURANCE -> endurance++;
            case RECOVERY -> recovery++;
            case AGILITY -> agility++;
            case CHARACTER -> { characterLevel++; health = maxHealth(); }
        }
        return true;
    }
    public void damagePlayer(float amount) { if (amount > 0) health = Math.max(0, health - Math.max(1, amount - defense())); }
    public void damageBase(float amount) { baseHealth = Math.max(0, baseHealth - Math.max(0, amount)); }
    public void heal(float dt) { health = Math.min(maxHealth(), health + Math.max(0, dt) * (3 + baseLevel)); }
}
