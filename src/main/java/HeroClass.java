/** Starting equipment and combat style. Class is fixed for a defense. */
public enum HeroClass {
    WARRIOR("Warrior", "Sword / 100 HP / wide melee combos",100,5),
    ARCHER("Archer", "Bow / 90 HP / fast, precise arrows",90,6),
    MAGE("Mage", "Staff / 80 HP / piercing magic bolts",80,5.2f);
    public final String label, description;
    public final int health;
    public final float speed;
    HeroClass(String label,String description,int health,float speed) {
        this.label=label;this.description=description;this.health=health;this.speed=speed;
    }
    public Combat.Attack attack(int combo,boolean heavy) {
        if (this==WARRIOR) return Combat.attack(combo,heavy);
        if (this==ARCHER) return new Combat.Attack(heavy?.95f:.68f,heavy?.4f:.32f,heavy?16:12,4,heavy?2: .85f,heavy?"POWER SHOT":"ARROW");
        return new Combat.Attack(heavy?1.1f:.74f,heavy?.4f:.28f,heavy?12:10,6,heavy?2.2f:1.1f,heavy?"ARCANE SURGE":"MAGIC BOLT");
    }
}
