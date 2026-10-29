package Entities;

/** Stamina and defensive windows, independent of graphics and desktop input. */
public final class CombatDiscipline {
    public static final float LIGHT=14,HEAVY=32,LUNGE=24,DODGE=22,PARRY=12;
    private float stamina=100,regenDelay,parry,recovery,riposte;
    private boolean sprintExhausted;
    public float stamina() { return stamina; }
    public void stamina(float value) { if(!Float.isFinite(value)) throw new IllegalArgumentException("Invalid stamina");stamina=Math.max(0,Math.min(100,value)); }
    public boolean spend(float amount) {
        if(!Float.isFinite(amount) || amount<0 || stamina<amount) return false;
        stamina-=amount;regenDelay=.65f;return true;
    }
    public void tick(float dt,boolean sprinting) {
        parry=Math.max(0,parry-dt);recovery=Math.max(0,recovery-dt);riposte=Math.max(0,riposte-dt);
        float regenerate=Math.max(0,dt-regenDelay);regenDelay=Math.max(0,regenDelay-dt);
        if(sprinting) {
            stamina=Math.max(0,stamina-16*dt);regenDelay=.35f;
            if(stamina<=.01f) sprintExhausted=true;
        } else if(regenDelay<=0) stamina=Math.min(100,stamina+22*regenerate);
        if(stamina>=25) sprintExhausted=false;
    }
    public boolean canSprint() { return stamina>0 && !sprintExhausted; }
    public boolean beginParry() {
        if(recovery>0 || !spend(PARRY)) return false;
        parry=.22f;recovery=.70f;return true;
    }
    public boolean parrying() { return parry>0; }
    public float recovery() { return recovery; }
    public boolean deflect() {
        if(!parrying()) return false;
        parry=recovery=0;riposte=1.4f;stamina=Math.min(100,stamina+18);return true;
    }
    public boolean riposteReady() { return riposte>0; }
    public float consumeRiposte() { if(riposte<=0) return 1;riposte=0;return 1.75f; }
    public void reset() { stamina=100;regenDelay=parry=recovery=riposte=0;sprintExhausted=false; }
}
