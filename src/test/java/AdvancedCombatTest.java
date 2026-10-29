import Entities.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedCombatTest {
    @TempDir Path directory;
    @Test void staminaLimitsSpamAndRegeneratesOnlyAfterRecoveryDelay() {
        CombatDiscipline d=new CombatDiscipline();
        for(int i=0;i<7;i++) assertTrue(d.spend(CombatDiscipline.LIGHT));
        assertEquals(2,d.stamina());assertFalse(d.spend(CombatDiscipline.LIGHT));
        d.tick(.3f,false);assertEquals(2,d.stamina());d.tick(.4f,false);assertTrue(d.stamina()>2);
        assertFalse(d.spend(Float.NaN));assertFalse(d.spend(-10));
        assertThrows(IllegalArgumentException.class,()->d.stamina(Float.NaN));
    }
    @Test void failedParryHasRecoveryAndSuccessfulParryRewardsOneRiposte() {
        CombatDiscipline d=new CombatDiscipline();assertTrue(d.beginParry());
        d.tick(.23f,false);assertFalse(d.parrying());assertFalse(d.deflect());assertFalse(d.beginParry());
        d.tick(.6f,false);assertTrue(d.beginParry());d.tick(.1f,false);
        assertTrue(d.deflect());assertTrue(d.riposteReady());assertEquals(1.75f,d.consumeRiposte());
        assertEquals(1,d.consumeRiposte());assertFalse(d.riposteReady());
        d.tick(1,false);assertTrue(d.beginParry());assertTrue(d.deflect());d.tick(1.5f,false);assertFalse(d.riposteReady());
    }
    @Test void exhaustedSprintCannotChatterBackOnBeforeStaminaRecovers() {
        CombatDiscipline d=new CombatDiscipline();d.stamina(1);d.tick(.1f,true);
        assertFalse(d.canSprint());assertEquals(0,d.stamina());
        d.tick(.5f,false);assertTrue(d.stamina()>0);assertFalse(d.canSprint());
        d.tick(1,false);assertTrue(d.canSprint());
    }
    @Test void sigilsWarnBeforeDamageAndEachHazardStrikesOnlyOnce() {
        BossHazard sigil=new BossHazard(BossHazard.Kind.SIGIL,2,3,1,25);
        assertFalse(sigil.hits(2,3));sigil.update(.9f);assertFalse(sigil.hits(2,3));
        sigil.update(.11f);assertFalse(sigil.hits(7,3));assertTrue(sigil.hits(2,3));assertFalse(sigil.hits(2,3));
        sigil.update(.5f);assertTrue(sigil.expired());
    }
    @Test void shockwavesUseSweptRadiusRatherThanSkippingFastCrossings() {
        BossHazard ring=new BossHazard(BossHazard.Kind.SHOCKWAVE,0,0,0,30);
        ring.update(.5f);assertTrue(ring.hits(2,0));assertFalse(ring.hits(2,0));
        assertFalse(new BossHazard(BossHazard.Kind.SHOCKWAVE,0,0,1,30).hits(0,0));
        assertEquals(0,Combat.segmentDistance(5,0,0,0,10,0));
        assertEquals(2,Combat.segmentDistance(5,2,0,0,10,0));
    }
    @Test void bossesChooseVariedLockedAttacksAndEnrageBelowHalfHealth() {
        Enemy boss=new Enemy(0,0,5,5,.5f);Set<BossMove> moves=new HashSet<>();
        for(int i=0;i<4;i++) { boss.beginAttack(0,2,true);moves.add(boss.bossMove); }
        assertEquals(4,moves.size());assertFalse(boss.enraged());
        boss.health=boss.maxHealth*.5f;assertTrue(boss.enraged());
        boss.bossMove=BossMove.SLAM;assertTrue(boss.attackDuration()<BossMove.SLAM.duration);
        assertFalse(BossMove.SLAM.parryable);assertTrue(BossMove.CHARGE.parryable);
    }
    @Test void chargeTravelsToLockedPointAndStopsAtWorldBounds() {
        Enemy boss=new Enemy(20,0,5,5,.5f);boss.beginAttack(30,0,true);assertEquals(BossMove.CHARGE,boss.bossMove);
        assertEquals(22,boss.chargeEndX);assertEquals(0,boss.chargeEndZ);
        boss.launchCharge();boss.approach(-20,20,.5f);
        assertEquals(22,boss.x);assertEquals(0,boss.z);assertEquals(0,boss.chargeTime);
    }
    @Test void staminaPersistsAndOlderSavesDefaultToFull() throws Exception {
        Path file=directory.resolve("save.properties");
        SaveGame.write(file,new SaveGame.Snapshot("Warden",0,4,0,new Progression(),new WaveDirector.State(0,0,0,5,0,0,0),List.of(),Arena.HIGHLANDS,31));
        assertEquals(31,SaveGame.read(file).stamina());
        Properties p=AppData.read(file);p.remove("stamina");AppData.write(file,p);assertEquals(100,SaveGame.read(file).stamina());
        p.setProperty("stamina","NaN");AppData.write(file,p);assertThrows(java.io.IOException.class,()->SaveGame.read(file));
    }
    @Test void newBindingsDoNotEraseCustomBindingsThatUsedTheirDefaultKeys() {
        Config.Controls c=new Config.Controls();Properties p=new Properties();c.store(p);
        p.remove("key.PARRY");p.remove("key.LUNGE");p.setProperty("key.FORWARD","70");
        c.restore(p);assertEquals(70,c.key(Config.Controls.Action.FORWARD));
        assertNotEquals(70,c.key(Config.Controls.Action.PARRY));
        Set<Integer> keys=new HashSet<>();for(Config.Controls.Action action:Config.Controls.Action.values()) assertTrue(keys.add(c.key(action)));
    }
}
