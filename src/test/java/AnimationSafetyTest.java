import Entities.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimationSafetyTest {
    @Test void enrageKeepsCurrentWindupAndSpeedsUpTheNextAttack() {
        for(int wave:new int[]{5,10}) for(Difficulty difficulty:Difficulty.values()) for(BossMove move:BossMove.values()) {
            Enemy boss=new Enemy(0,0,wave,5,.5f,difficulty);
            do { boss.beginAttack(0,2,true); } while(boss.bossMove!=move);
            float original=boss.attackDuration();
            boss.health=boss.maxHealth*.5f+1;
            boss.hit(2,0,0,0);
            assertTrue(boss.enraged());
            assertEquals(original,boss.attackDuration(),0,"Enrage changed an active windup");
            assertEquals(0,1-boss.attackTime/boss.attackDuration(),.00001f);
            boss.approach(0,2,.01f);
            assertTrue(1-boss.attackTime/boss.attackDuration()>0);
            do { boss.beginAttack(0,2,true); } while(boss.bossMove!=move);
            assertTrue(boss.attackDuration()<original,"Next attack did not use enrage speed");
        }
    }

    @Test void frameBoundariesRejectEmptyClipsAndClampInvalidProgress() {
        for(int count:new int[]{1,2,20,81}) {
            assertEquals(0,AnimationFrames.progress(-.22f,count)); // Original crash at the enrage threshold.
            assertEquals(0,AnimationFrames.progress(Float.NaN,count));
            assertEquals(0,AnimationFrames.progress(Float.NEGATIVE_INFINITY,count));
            assertEquals(count-1,AnimationFrames.progress(1,count));
            assertEquals(count-1,AnimationFrames.progress(Float.POSITIVE_INFINITY,count));
            assertEquals(0,AnimationFrames.clamp(-8,count));
            assertEquals(count-1,AnimationFrames.clamp(Integer.MAX_VALUE,count));
            assertEquals(count-1,AnimationFrames.loop(-1,count));
            int longSession=AnimationFrames.loop(Float.MAX_VALUE,count);
            assertTrue(longSession>=0 && longSession<count);
        }
        assertThrows(IllegalArgumentException.class,()->AnimationFrames.progress(0,0));
        assertThrows(IllegalArgumentException.class,()->AnimationFrames.loop(0,0));
    }
}
