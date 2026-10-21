import org.junit.jupiter.api.Test;
import Entities.Enemy;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionTest {
    @Test void unaffordableUpgradeLeavesStateUnchanged() {
        Progression p = new Progression();
        assertFalse(p.purchase(Progression.Upgrade.BASE));
        assertEquals(0, p.crystals()); assertEquals(1, p.baseLevel());
    }
    @Test void threeKillsFundOneUpgradeAndCostScales() {
        Progression p = new Progression();
        for (int i=0;i<3;i++) p.rewardKill();
        assertEquals(45, p.crystals()); assertEquals(3, p.kills());
        p.damageBase(90);
        assertTrue(p.purchase(Progression.Upgrade.BASE));
        assertEquals(0, p.crystals()); assertEquals(350, p.baseHealth());
        assertEquals(90, p.cost(Progression.Upgrade.BASE));
        assertFalse(p.purchase(Progression.Upgrade.BASE));
    }
    @Test void characterUpgradeHealsAndImprovesAttack() {
        Progression p = new Progression();
        p.damagePlayer(50);
        for (int i=0;i<3;i++) p.rewardKill();
        assertTrue(p.purchase(Progression.Upgrade.CHARACTER));
        assertEquals(125, p.health()); assertEquals(35, p.damage());
    }
    @Test void armorReducesDamageWithoutTurningItIntoHealing() {
        Progression p = new Progression();
        for (int i=0;i<3;i++) p.rewardKill();
        p.purchase(Progression.Upgrade.ARMOR);
        p.damagePlayer(10); assertEquals(92, p.health());
        p.damagePlayer(1); assertEquals(91, p.health());
        p.damagePlayer(-5); assertEquals(91, p.health());
    }
    @Test void healthClampsAndEitherDeathEndsRun() {
        Progression p = new Progression();
        p.heal(1000); assertEquals(100, p.health());
        p.damagePlayer(1000); assertEquals(0, p.health()); assertTrue(p.defeated());
        Progression base = new Progression();
        base.damageBase(1000); assertEquals(0, base.baseHealth()); assertTrue(base.defeated());
    }
    @Test void enemyApproachDoesNotOvershootOrDivideByZero() {
        Enemy e = new Enemy(10, 0, 1);
        e.approach(0, 0, 100); assertEquals(1.3f, e.x, .001f);
        e.x = 0; e.approach(0, 0, 1); assertTrue(Float.isFinite(e.x));
    }
}
