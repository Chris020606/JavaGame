/** Rules and economy kept independent of rendering so they can be tested. */
public class Progression {
    public void store(java.util.Properties p) {
        p.setProperty("heroClass",heroClass.name()); p.setProperty("potions",potions+"");
        p.setProperty("crystals",crystals+""); p.setProperty("kills",kills+"");
        p.setProperty("baseLevel",baseLevel+""); p.setProperty("armorLevel",armorLevel+""); p.setProperty("characterLevel",characterLevel+"");
        p.setProperty("health",health+""); p.setProperty("baseHealth",baseHealth+"");
    }
    public static Progression restore(java.util.Properties p) {
        Progression result=new Progression(HeroClass.valueOf(p.getProperty("heroClass","WARRIOR")));
        result.potions=p.containsKey("potions")?SaveGame.integer(p,"potions",0,5):2;
        result.crystals=SaveGame.integer(p,"crystals",0,100000000); result.kills=SaveGame.integer(p,"kills",0,100000000);
        result.baseLevel=SaveGame.integer(p,"baseLevel",1,1000); result.armorLevel=SaveGame.integer(p,"armorLevel",1,1000);
        result.characterLevel=SaveGame.integer(p,"characterLevel",1,1000);
        result.health=SaveGame.number(p,"health",.001f,result.maxHealth()); result.baseHealth=SaveGame.number(p,"baseHealth",.001f,result.maxBaseHealth());
        return result;
    }
    public enum Upgrade { BASE, ARMOR, CHARACTER }
    private int crystals, kills, baseLevel = 1, armorLevel = 1, characterLevel = 1;
    private final HeroClass heroClass;
    private int potions=2;
    private float health, baseHealth = 250;
    public Progression() { this(HeroClass.WARRIOR); }
    public Progression(HeroClass heroClass) { this.heroClass=heroClass; health=maxHealth(); }
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
    public int crystals() { return crystals; }
    public int kills() { return kills; }
    public int baseLevel() { return baseLevel; }
    public int armorLevel() { return armorLevel; }
    public int characterLevel() { return characterLevel; }
    public int maxHealth() { return heroClass.health + (characterLevel - 1) * 25; }
    public int maxBaseHealth() { return 250 + (baseLevel - 1) * 100; }
    public int damage() { return 25 + (characterLevel - 1) * 10; }
    public int defense() { return (armorLevel - 1) * 2; }
    public float health() { return health; }
    public float baseHealth() { return baseHealth; }
    public boolean defeated() { return health <= 0 || baseHealth <= 0; }
    public void rewardKill() { kills++; crystals += 15; }
    public int level(Upgrade type) { return switch (type) { case BASE -> baseLevel; case ARMOR -> armorLevel; case CHARACTER -> characterLevel; }; }
    public int cost(Upgrade type) { return 45 * level(type); }
    public boolean purchase(Upgrade type) {
        int cost = cost(type);
        if (crystals < cost || defeated()) return false;
        crystals -= cost;
        switch (type) {
            case BASE -> { baseLevel++; baseHealth = maxBaseHealth(); }
            case ARMOR -> armorLevel++;
            case CHARACTER -> { characterLevel++; health = maxHealth(); }
        }
        return true;
    }
    public void damagePlayer(float amount) { if (amount > 0) health = Math.max(0, health - Math.max(1, amount - defense())); }
    public void damageBase(float amount) { baseHealth = Math.max(0, baseHealth - Math.max(0, amount)); }
    public void heal(float dt) { health = Math.min(maxHealth(), health + Math.max(0, dt) * (3 + baseLevel)); }
}
