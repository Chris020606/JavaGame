package Entities;

import java.util.List;

/** Local perception and squad rules; no renderer or desktop input required. */
public final class SquadTactics {
    private SquadTactics() {}
    public static boolean clearShot(Enemy shooter,float tx,float tz,List<Enemy> allies) {
        float dx=tx-shooter.x,dz=tz-shooter.z,length=dx*dx+dz*dz;
        if(length<.01f) return true;
        for(Enemy ally:allies) if(ally!=shooter && ally.health>0) {
            float t=((ally.x-shooter.x)*dx+(ally.z-shooter.z)*dz)/length;
            if(t>.08f && t<.92f && Math.hypot(ally.x-shooter.x-dx*t,ally.z-shooter.z-dz*t)<(ally.variant==2?.9f:.65f)) return false;
        }
        return true;
    }
    public static boolean canJoinAssault(Enemy enemy,List<Enemy> allies) {
        if(enemy.boss() || enemy.ranged()) return true;
        int active=0;
        for(Enemy ally:allies) if(ally!=enemy && ally.targetsPlayer && ally.attackTime>.25f && !ally.ranged()) active++;
        return active<enemy.difficulty.attackers;
    }
    public static Enemy woundedAlly(Enemy healer,List<Enemy> allies) {
        Enemy best=null;
        for(Enemy ally:allies) if(ally!=healer && !ally.boss() && ally.health>0 && ally.health<ally.maxHealth*.6f && ally.distance(healer.x,healer.z)<7)
            if(best==null || ally.health/ally.maxHealth<best.health/best.maxHealth) best=ally;
        return best;
    }
}
