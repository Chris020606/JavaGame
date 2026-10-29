package Entities;

public class Enemy {
    public float x, z, health, cooldown;
    public float facing, animationTime, attackTime, hitFlash, stun;
    public float attackX, attackZ;
    public BossMove bossMove=BossMove.CLEAVE;
    private int attackCycle;
    public float chargeTime,chargeVx,chargeVz,chargeEndX,chargeEndZ;
    public boolean chargeHit;
    public final boolean king,undead;
    public final Difficulty difficulty;
    public Enemy healingTarget;
    public float locomotion,sideStepTime,healCooldown;
    private float velocityX,velocityZ,decisionTime;
    private boolean heroFocus;
    public String action="APPROACH";
    public boolean chooseHero(float px,float pz,float dt) {
        if(boss()) return true;
        decisionTime-=dt;
        if(decisionTime<=0) {
            float threshold=variant==2?4.5f:variant==1?13:ranged()?12:8;
            heroFocus=distance(px,pz)<threshold+(heroFocus?2:0);
            decisionTime=.55f*difficulty.cadence+personality*.2f;
        }
        return heroFocus;
    }
    public boolean enraged() { return boss() && health<=maxHealth*.5f; }
    public void launchCharge() {
        chargeTime=.5f;chargeHit=false;
        chargeVx=(chargeEndX-x)/.5f;chargeVz=(chargeEndZ-z)/.5f;
    }

    public boolean targetsPlayer, impactPending;
    public final float maxHealth, speed, personality;
    public final int variant;
    public Enemy(float x, float z, int wave) { this(x,z,wave,0,.5f); }
    public Enemy(float x,float z,int wave,int variant,float personality) { this(x,z,wave,variant,personality,Difficulty.NORMAL); }
    public Enemy(float x,float z,int wave,int variant,float personality,Difficulty difficulty) {
        this.x=x; this.z=z; king=wave>=10;undead=wave>=5;this.difficulty=difficulty; this.variant=variant; this.personality=personality;
        maxHealth=(40+wave*10)*(variant==5?5:variant==2?1.4f:variant==1||variant==3?.8f:variant==4?1.1f:1)*difficulty.health;
        health=maxHealth;
        speed=Math.min(3.5f,1.4f+wave*.12f)*(variant==5?.7f:variant==1?1.35f:variant==2?.8f:1)*(.9f+personality*.2f)*difficulty.speed;
        healCooldown=3+personality*3;cooldown=personality*.6f; animationTime=personality*3;
    }
    public float distance(float px,float pz) { return (float)Math.hypot(x-px,z-pz); }
    private boolean clock(float dt) {
        animationTime+=dt*Math.max(.25f,locomotion);hitFlash=Math.max(0,hitFlash-dt);stun=Math.max(0,stun-dt);
        attackTime=Math.max(0,attackTime-dt);cooldown=Math.max(0,cooldown-dt);healCooldown=Math.max(0,healCooldown-dt);sideStepTime=Math.max(0,sideStepTime-dt);
        if(chargeTime>0) {
            float step=Math.min(dt,chargeTime);chargeTime=Math.max(0,chargeTime-dt);
            x=Math.max(-22,Math.min(22,x+chargeVx*step));z=Math.max(-22,Math.min(22,z+chargeVz*step));locomotion=1;return true;
        }
        if(attackTime>0 || stun>0) { velocityX=velocityZ=locomotion=0;action=stun>0?"STAGGERED":"ATTACK";return true; }
        return false;
    }
    public void approach(float px,float pz,float dt) {
        float distance=distance(px,pz);
        if(clock(dt)) return;
        facing=(float)Math.toDegrees(Math.atan2(px-x,pz-z));
        if (ranged() && distance<4 && distance>.001f) { x=Math.max(-22,Math.min(22,x-(px-x)/distance*speed*.65f*dt)); z=Math.max(-22,Math.min(22,z-(pz-z)/distance*speed*.65f*dt)); return; }
        float stop=ranged()?6:1.3f;
        if (distance>stop) {
            float step=Math.min(speed*dt,distance-stop);
            x+=(px-x)/distance*step; z+=(pz-z)/distance*step;
        }
    }
    public void navigate(float tx,float tz,float separationX,float separationZ,float dt) {
        if(clock(dt)) return;
        float dx=tx-x,dz=tz-z,d=(float)Math.hypot(dx,dz),nx=d>.001f?dx/d:0,nz=d>.001f?dz/d:0;
        float vx=0,vz=0,side=personality>.5f?1:-1;
        action="APPROACH";
        if(ranged()) {
            if(d<4.5f) { vx=-nx+nz*side*.45f;vz=-nz-nx*side*.45f;action="RETREAT"; }
            else if(d<7.4f) { if(cooldown>.15f || sideStepTime>0) { vx=nz*side*.55f;vz=-nx*side*.55f;action="STRAFE"; } }
            else { vx=nx;vz=nz; }
        } else if(!boss() && cooldown>.35f && d<3 && variant!=2) {
            vx=-nx*.65f+nz*side*.25f;vz=-nz*.65f-nx*side*.25f;action="RECOVER";
        } else if(d>(boss()?2.2f:1.45f)) {
            float flank=!boss() && d>3 && d<10?(variant==1?.75f:.35f)*side:0;
            vx=nx+nz*flank;vz=nz-nx*flank;action=flank!=0?"FLANK":"APPROACH";
        }
        vx+=separationX*.9f;vz+=separationZ*.9f;
        float magnitude=(float)Math.hypot(vx,vz);if(magnitude>1) { vx/=magnitude;vz/=magnitude; }
        float blend=Math.min(1,dt*9);velocityX+=(vx*speed-velocityX)*blend;velocityZ+=(vz*speed-velocityZ)*blend;
        x=Math.max(-22,Math.min(22,x+velocityX*dt));z=Math.max(-22,Math.min(22,z+velocityZ*dt));
        locomotion=(float)Math.hypot(velocityX,velocityZ)/Math.max(.1f,speed);
        float target=(float)Math.toDegrees(Math.atan2(dx,dz)),delta=(float)Math.IEEEremainder(target-facing,360);
        facing=(float)Math.IEEEremainder(facing+Math.max(-540*dt,Math.min(540*dt,delta)),360);
    }
    public boolean facingTarget(float tx,float tz) {
        float target=(float)Math.toDegrees(Math.atan2(tx-x,tz-z));return Math.abs(Math.IEEEremainder(target-facing,360))<35;
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
        attackTime=attackDuration(); cooldown=(boss()?attackDuration()+(enraged()?.65f:1.1f):1.3f)*difficulty.cadence+personality*.35f; impactPending=true;
    }
    public void hit(float damage,float px,float pz,float push) {
        health=Math.max(0,health-damage); hitFlash=.16f;
        if (!boss()) { healingTarget=null;stun=.25f; attackTime=0; impactPending=false; }
        else push*=.15f;
        float d=distance(px,pz);
        if (d>.001) { x=Math.max(-22,Math.min(22,x+(x-px)/d*push)); z=Math.max(-22,Math.min(22,z+(z-pz)/d*push)); }
    }
    public String moveLabel() {
        if(!king) return bossMove==BossMove.SIGILS?"EMBER SIGILS":bossMove.label;
        return switch(bossMove) { case CLEAVE -> "ROYAL REAP";case SLAM -> "GRAVE FAULT";case CHARGE -> "PHANTOM RUSH";case SIGILS -> "SOUL SIGILS"; };
    }
    public boolean boss() { return variant==5; }
    public float attackDuration() { return Math.max(.42f,(boss()?bossMove.duration*(enraged()?.82f:1):.7f)*difficulty.cadence); }
    public float meleeRange() { return boss()?3.8f:2.2f; }
    public float meleeDamage(int wave) { return (boss()?22+wave*3:8+wave*2+(variant==2?5:0))*difficulty.damage; }
    public float siegeDamage(int wave) { return (boss()?30+wave*3:10+wave*2)*difficulty.damage; }
    public int reward() { return boss()?120:15; }
    public boolean ranged() { return variant==3 || variant==4; }
    public float attackRange() { return ranged()?8:boss()?12:1.9f; }
    public String label() { if(undead && !boss()) return switch(variant) { case 1 -> "RATTLESCOUT";case 2 -> "FROST REAVER";case 3 -> "BONE ARCHER";case 4 -> "FROST HEXER";default -> "BONEGUARD"; };return switch(variant) { case 1 -> "SCOUT"; case 2 -> "BRUTE"; case 3 -> "ARCHER"; case 4 -> "SHAMAN"; case 5 -> "WARLORD"; default -> "RAIDER"; }; }
}
