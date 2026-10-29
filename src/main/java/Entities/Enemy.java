package Entities;

public class Enemy {
    public float x, z, health, cooldown;
    public float facing, animationTime, attackTime, hitFlash, stun;
    public float attackX, attackZ;
    public BossMove bossMove=BossMove.CLEAVE;
    private int attackCycle;
    public float chargeTime,chargeVx,chargeVz,chargeEndX,chargeEndZ;
    public boolean chargeHit;
    public final boolean king;
    public boolean enraged() { return boss() && health<=maxHealth*.5f; }
    public void launchCharge() {
        chargeTime=.5f;chargeHit=false;
        chargeVx=(chargeEndX-x)/.5f;chargeVz=(chargeEndZ-z)/.5f;
    }

    public boolean targetsPlayer, impactPending;
    public final float maxHealth, speed, personality;
    public final int variant;
    public Enemy(float x, float z, int wave) { this(x,z,wave,0,.5f); }
    public Enemy(float x,float z,int wave,int variant,float personality) {
        this.x=x; this.z=z; king=wave>=10; this.variant=variant; this.personality=personality;
        maxHealth=(40+wave*10)*(variant==5?5:variant==2?1.4f:variant==1||variant==3?.8f:variant==4?1.1f:1);
        health=maxHealth;
        speed=Math.min(3.5f,1.4f+wave*.12f)*(variant==5?.7f:variant==1?1.35f:variant==2?.8f:1)*(.9f+personality*.2f);
        cooldown=personality*.6f; animationTime=personality*3;
    }
    public float distance(float px,float pz) { return (float)Math.hypot(x-px,z-pz); }
    public void approach(float px,float pz,float dt) {
        float distance=distance(px,pz);
        animationTime+=dt; hitFlash=Math.max(0,hitFlash-dt); stun=Math.max(0,stun-dt);
        attackTime=Math.max(0,attackTime-dt); cooldown=Math.max(0,cooldown-dt);
        if(chargeTime>0) {
            float step=Math.min(dt,chargeTime);chargeTime=Math.max(0,chargeTime-dt);
            x=Math.max(-22,Math.min(22,x+chargeVx*step));z=Math.max(-22,Math.min(22,z+chargeVz*step));return;
        }
        if (attackTime>0 || stun>0) return;
        facing=(float)Math.toDegrees(Math.atan2(px-x,pz-z));
        if (ranged() && distance<4 && distance>.001f) { x=Math.max(-22,Math.min(22,x-(px-x)/distance*speed*.65f*dt)); z=Math.max(-22,Math.min(22,z-(pz-z)/distance*speed*.65f*dt)); return; }
        float stop=ranged()?6:1.3f;
        if (distance>stop) {
            float step=Math.min(speed*dt,distance-stop);
            x+=(px-x)/distance*step; z+=(pz-z)/distance*step;
        }
    }
    public void beginAttack(float px,float pz,boolean player) {
        attackX=px; attackZ=pz; targetsPlayer=player;
        if(boss()) {
            bossMove=BossMove.choose(attackCycle++,distance(px,pz),king);
            float dx=px-x,dz=pz-z,d=(float)Math.hypot(dx,dz),length=Math.min(12,d+1.5f);
            chargeEndX=Math.max(-22,Math.min(22,x+(d>.01f?dx/d*length:0)));
            chargeEndZ=Math.max(-22,Math.min(22,z+(d>.01f?dz/d*length:0)));
        }
        facing=(float)Math.toDegrees(Math.atan2(px-x,pz-z));
        attackTime=attackDuration(); cooldown=(boss()?attackDuration()+(enraged()?.65f:1.1f):1.3f)+personality*.35f; impactPending=true;
    }
    public void hit(float damage,float px,float pz,float push) {
        health=Math.max(0,health-damage); hitFlash=.16f;
        if (!boss()) { stun=.25f; attackTime=0; impactPending=false; }
        else push*=.15f;
        float d=distance(px,pz);
        if (d>.001) { x=Math.max(-22,Math.min(22,x+(x-px)/d*push)); z=Math.max(-22,Math.min(22,z+(z-pz)/d*push)); }
    }
    public String moveLabel() {
        if(!king) return bossMove==BossMove.SIGILS?"EMBER SIGILS":bossMove.label;
        return switch(bossMove) { case CLEAVE -> "ROYAL REAP";case SLAM -> "GRAVE FAULT";case CHARGE -> "PHANTOM RUSH";case SIGILS -> "SOUL SIGILS"; };
    }
    public boolean boss() { return variant==5; }
    public float attackDuration() { return boss()?bossMove.duration*(enraged()?.82f:1):.7f; }
    public float meleeRange() { return boss()?3.8f:2.2f; }
    public float meleeDamage(int wave) { return boss()?22+wave*3:8+wave*2+(variant==2?5:0); }
    public float siegeDamage(int wave) { return boss()?30+wave*3:10+wave*2; }
    public int reward() { return boss()?120:15; }
    public boolean ranged() { return variant==3 || variant==4; }
    public float attackRange() { return ranged()?8:boss()?12:1.9f; }
    public String label() { return switch(variant) { case 1 -> "SCOUT"; case 2 -> "BRUTE"; case 3 -> "ARCHER"; case 4 -> "SHAMAN"; case 5 -> "WARLORD"; default -> "RAIDER"; }; }
}
