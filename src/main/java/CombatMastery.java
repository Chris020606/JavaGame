/** Short-lived class rewards encourage different decisions without adding key bindings. */
public final class CombatMastery {
    private int stacks;
    private float remaining;
    public int stacks() { return stacks; }
    public void tick(float dt) { remaining=Math.max(0,remaining-Math.max(0,dt));if(remaining==0)stacks=0; }
    public void reset() { stacks=0;remaining=0; }
    public void lightHit(HeroClass hero) { if(hero==HeroClass.MAGE) { stacks=Math.min(3,stacks+1);remaining=6; } }
    public void evade(HeroClass hero) { if(hero==HeroClass.ARCHER) { stacks=1;remaining=3; } }
    public void parry(HeroClass hero) { if(hero==HeroClass.WARRIOR) { stacks=1;remaining=4; } }
    public float attackBonus(HeroClass hero,boolean heavy) {
        if(stacks==0)return 1;
        if(hero==HeroClass.MAGE && !heavy)return 1;
        float bonus=hero==HeroClass.MAGE?1+stacks*.2f:hero==HeroClass.ARCHER?1.3f:1.2f;
        reset();return bonus;
    }
    public String label(HeroClass hero) { return switch(hero) {
        case WARRIOR -> stacks>0?"VALOR / NEXT STRIKE +20%":"VALOR / PARRY TO EMPOWER";
        case ARCHER -> stacks>0?"FOCUS / NEXT SHOT +30%":"FOCUS / EVADE A HIT TO EMPOWER";
        case MAGE -> "RESONANCE / "+stacks+" OF 3 / HEAVY +"+(stacks*20)+"%";
    }; }
}
