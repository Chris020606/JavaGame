import Entities.Enemy;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class CombatTest {
    @Test void swordHitsAheadButNotBehindOrBeyondReach() {
        assertTrue(Combat.inArc(0,0,0,0,2,3,65));
        assertFalse(Combat.inArc(0,0,0,0,-2,3,65));
        assertFalse(Combat.inArc(0,0,0,0,3.1f,3,65));
        assertFalse(Combat.inArc(0,0,0,2,0,3,65));
        assertTrue(Combat.inArc(0,0,90,2,0,3,65));
        assertTrue(Combat.inArc(0,0,0,0,0,3,65));
    }
    @Test void heavyAndFinisherTradeRecoveryForDamage() {
        Combat.Attack light=Combat.attack(1,false), finisher=Combat.attack(3,false), heavy=Combat.attack(0,true);
        assertTrue(heavy.multiplier()>finisher.multiplier());
        assertTrue(finisher.multiplier()>light.multiplier());
        assertTrue(heavy.duration()>light.duration());
        for (Combat.Attack attack:new Combat.Attack[]{light,finisher,heavy}) {
            assertTrue(attack.impact()>0); assertTrue(attack.impact()<attack.duration());
        }
    }
    @Test void enemyTelegraphLocksMovementAndFacing() {
        Enemy e=new Enemy(0,0,1);
        e.beginAttack(0,1.5f,true);
        e.approach(2,0,.2f);
        assertEquals(0,e.x); assertEquals(0,e.z); assertEquals(0,e.facing);
        assertTrue(e.impactPending); assertEquals(.5f,e.attackTime,.001f);
        assertFalse(Combat.inArc(e.x,e.z,e.facing,3,0,2.2f,65));
    }
    @Test void hitInterruptsEnemyWindupAndKnocksAway() {
        Enemy e=new Enemy(0,2,1);
        e.beginAttack(0,0,true); e.hit(25,0,0,.8f);
        assertEquals(25,e.health); assertFalse(e.impactPending);
        assertEquals(0,e.attackTime); assertTrue(e.stun>0); assertEquals(2.8f,e.z,.001f);
    }
    @Test void variantsHaveDifferentHealthAndTravelTimes() {
        Enemy normal=new Enemy(0,20,1,0,.5f), scout=new Enemy(0,20,1,1,.5f), brute=new Enemy(0,20,1,2,.5f);
        assertTrue(scout.speed>normal.speed); assertTrue(normal.speed>brute.speed);
        assertTrue(brute.health>normal.health); assertTrue(normal.health>scout.health);
    }
    @Test void arrivalsAreStaggeredAndQueuedEnemiesPreventPrematureWaveClear() {
        WaveDirector d=new WaveDirector(new Random(7));
        for (int i=0;i<49;i++) assertNull(d.update(.1f,0));
        float last=-100; int arrivals=0; boolean warning=false;
        for (int i=0;i<250;i++) {
            warning|=d.warning();
            WaveDirector.Spawn spawn=d.update(.1f,arrivals);
            if (spawn!=null) {
                assertTrue(i*.1f-last>=.85f); last=i*.1f; arrivals++;
                assertEquals(1,spawn.wave()); assertTrue(Math.hypot(spawn.x(),spawn.z())>=19.99);
            }
        }
        assertEquals(7,arrivals); assertTrue(warning); assertEquals(0,d.remaining());
        assertEquals(1,d.wave());
        for (int i=0;i<70;i++) assertNull(d.update(.1f,0));
        assertEquals(1,d.wave());
        for (int i=0;i<20;i++) d.update(.1f,0);
        assertEquals(2,d.wave());
        d.reset(); assertEquals(0,d.wave()); assertEquals(0,d.remaining()); assertEquals(5,d.rest());
    }
}
