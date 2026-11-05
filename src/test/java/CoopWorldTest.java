import Entities.Difficulty;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoopWorldTest {
    static CoopWorld started() {
        CoopWorld w=new CoopWorld(Difficulty.NORMAL,42);w.attach(0,"One",HeroClass.WARRIOR);w.attach(1,"Two",HeroClass.MAGE);
        w.input(0,new CoopInput(1,0,0,0,CoopInput.READY));w.input(1,new CoopInput(1,0,0,0,CoopInput.READY));w.tick(1f/60);return w;
    }
    @Test void bothPlayersMustReadyAndPartyHasExplicitBalance() {
        CoopWorld w=new CoopWorld(Difficulty.NORMAL,1);w.attach(0,"One",HeroClass.ARCHER);w.input(0,new CoopInput(1,0,0,0,CoopInput.READY));w.tick(1f/60);
        assertEquals(CoopState.Phase.LOBBY,w.snapshot().phase());
        w.attach(1,"Two",HeroClass.MAGE);w.input(1,new CoopInput(1,0,0,0,CoopInput.READY));w.tick(1f/60);
        assertEquals(CoopState.Phase.PLAYING,w.snapshot().phase());assertEquals(10,w.snapshot().queued());
        assertEquals(1,CoopBalance.count(5,Difficulty.NORMAL));assertTrue(CoopBalance.healthScale(true)>CoopBalance.healthScale(false));
    }
    @Test void pauseDisconnectAndStaleInputsFreezeWorldAndReplayCannotTeleport() {
        CoopWorld w=started();float x=w.snapshot().heroes().get(0).x();
        w.input(0,new CoopInput(2,1,0,90,0));w.input(1,new CoopInput(2,0,0,0,CoopInput.PAUSE));
        for(int i=0;i<30;i++)w.tick(1f/60);
        assertEquals(x,w.snapshot().heroes().get(0).x(),.0001);assertTrue(w.snapshot().paused());
        w.input(1,new CoopInput(3,0,0,0,0));w.input(1,new CoopInput(4,0,0,0,CoopInput.PAUSE));w.input(0,new CoopInput(3,1,0,90,0));w.tick(1f/60);
        assertTrue(w.snapshot().heroes().get(0).x()>x);w.detach(1);float stopped=w.snapshot().heroes().get(0).x();
        for(int i=0;i<60;i++)w.tick(1f/60);assertEquals(stopped,w.snapshot().heroes().get(0).x(),.0001);
        assertThrows(IllegalArgumentException.class,()->new CoopInput(4,Float.NaN,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new CoopInput(4,2,0,0,0));
    }
    @Test void downedPartnerNeedsThreeUninterruptedSecondsAndBothDownMeansDefeat() {
        CoopWorld w=started();w.enemyDamage(0,1000,0,8,false);assertEquals(0,w.snapshot().heroes().get(0).health());
        long seq=2;for(int i=0;i<25;i++) { w.input(1,new CoopInput(seq++,-1,0,270,0));w.tick(1f/60); }
        for(int i=0;i<185;i++) { w.input(1,new CoopInput(seq++,0,0,0,CoopInput.INTERACT));w.input(0,new CoopInput(seq++,0,0,0,0));w.tick(1f/60); }
        assertTrue(w.snapshot().heroes().get(0).health()>0,"Partner did not revive");
        for(int i=0;i<65;i++) { w.input(0,new CoopInput(seq++,0,0,0,0));w.input(1,new CoopInput(seq++,0,0,0,0));w.tick(1f/60); }
        w.enemyDamage(0,1000,0,8,false);w.enemyDamage(1,1000,0,8,false);w.tick(1f/60);
        assertEquals(CoopState.Phase.DEFEAT,w.snapshot().phase());
    }
    @Test void parriesProtectHealthAndCoopCheckpointIsIndependentAndRestorable() {
        CoopWorld w=started();w.input(0,new CoopInput(2,0,0,0,CoopInput.PARRY));w.tick(1f/60);float hp=w.snapshot().heroes().get(0).health();
        w.enemyDamage(0,30,-2,10,true);assertEquals(hp,w.snapshot().heroes().get(0).health());
        CoopWorld restored=CoopWorld.restore(w.checkpoint());restored.attach(0,"New name",HeroClass.ARCHER);assertEquals(0,restored.snapshot().chapter());
        assertEquals(CoopState.Phase.LOBBY,restored.snapshot().phase());
    }
}
