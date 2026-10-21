package Entities;

public class Enemy {
    public float x, z, health, cooldown;
    public float facing, animationTime, attackTime, hitFlash, stun;
    public float attackX, attackZ;
    public boolean targetsPlayer, impactPending;
    public final float maxHealth, speed, personality;
    public final int variant;
    public Enemy(float x, float z, int wave) { this(x,z,wave,0,.5f); }
    public Enemy(float x,float z,int wave,int variant,float personality) {
        this.x=x; this.z=z; this.variant=variant; this.personality=personality;
        maxHealth=(40+wave*10)*(variant==2?1.4f:variant==1||variant==3?.8f:variant==4?1.1f:1);
        health=maxHealth;
        speed=Math.min(3.5f,1.4f+wave*.12f)*(variant==1?1.35f:variant==2?.8f:1)*(.9f+personality*.2f);
        cooldown=personality*.6f; animationTime=personality*3;
    }
    public float distance(float px,float pz) { return (float)Math.hypot(x-px,z-pz); }
    public void approach(float px,float pz,float dt) {
        float distance=distance(px,pz);
        animationTime+=dt; hitFlash=Math.max(0,hitFlash-dt); stun=Math.max(0,stun-dt);
        attackTime=Math.max(0,attackTime-dt); cooldown=Math.max(0,cooldown-dt);
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
        facing=(float)Math.toDegrees(Math.atan2(px-x,pz-z));
        attackTime=.7f; cooldown=1.3f+personality*.65f; impactPending=true;
    }
    public void hit(float damage,float px,float pz,float push) {
        health=Math.max(0,health-damage); hitFlash=.16f; stun=.25f;
        attackTime=0; impactPending=false;
        float d=distance(px,pz);
        if (d>.001) { x=Math.max(-22,Math.min(22,x+(x-px)/d*push)); z=Math.max(-22,Math.min(22,z+(z-pz)/d*push)); }
    }
    public boolean ranged() { return variant>=3; }
    public float attackRange() { return ranged()?8:1.9f; }
    public String label() { return switch(variant) { case 1 -> "SCOUT"; case 2 -> "BRUTE"; case 3 -> "ARCHER"; case 4 -> "SHAMAN"; default -> "RAIDER"; }; }
}
