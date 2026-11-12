import static org.junit.jupiter.api.Assertions.*;

import Entities.Enemy;

import org.junit.jupiter.api.Test;

class ProgressionTest {
    @Test
    void unaffordableUpgradeLeavesStateUnchanged() {
        Progression p = new Progression();
        assertFalse(p.purchase(Progression.Upgrade.BASE));
        assertEquals(0, p.crystals());
        assertEquals(1, p.baseLevel());
    }

    @Test
    void threeKillsFundOneUpgradeAndCostScales() {
        Progression p = new Progression();
        for (int i = 0; i < 3; i++) p.rewardKill();
        assertEquals(45, p.crystals());
        assertEquals(3, p.kills());
        p.damageBase(90);
        assertTrue(p.purchase(Progression.Upgrade.BASE));
        assertEquals(0, p.crystals());
        assertEquals(500, p.baseHealth());
        assertEquals(90, p.cost(Progression.Upgrade.BASE));
        assertFalse(p.purchase(Progression.Upgrade.BASE));
    }

    @Test
    void characterUpgradeHealsAndImprovesAttack() {
        Progression p = new Progression();
        p.damagePlayer(50);
        for (int i = 0; i < 3; i++) p.rewardKill();
        assertTrue(p.purchase(Progression.Upgrade.CHARACTER));
        assertEquals(125, p.health());
        assertEquals(35, p.damage());
    }

    @Test
    void armorReducesDamageWithoutTurningItIntoHealing() {
        Progression p = new Progression();
        for (int i = 0; i < 3; i++) p.rewardKill();
        p.purchase(Progression.Upgrade.ARMOR);
        p.damagePlayer(10);
        assertEquals(92, p.health());
        p.damagePlayer(1);
        assertEquals(91, p.health());
        p.damagePlayer(-5);
        assertEquals(91, p.health());
    }

    @Test
    void healthClampsAndEitherDeathEndsRun() {
        Progression p = new Progression();
        p.heal(1000);
        assertEquals(100, p.health());
        p.damagePlayer(1000);
        assertEquals(0, p.health());
        assertTrue(p.defeated());
        Progression base = new Progression();
        base.damageBase(1000);
        assertEquals(0, base.baseHealth());
        assertTrue(base.defeated());
    }

    @Test
    void sanctuaryRepairsWithCrystalsAndReceivesChapterResupply() {
        Progression p = new Progression();
        assertEquals(400, p.maxBaseHealth());
        for (int i = 0; i < 6; i++) p.rewardKill();
        p.damageBase(150);
        assertTrue(p.repairBase());
        assertEquals(350, p.baseHealth());
        assertEquals(60, p.crystals());
        assertTrue(p.restoreBase(200));
        assertEquals(400, p.baseHealth());
        assertFalse(p.restoreBase(50));
        p.damageBase(200);
        p.chapterRespite();
        assertEquals(300, p.baseHealth());
        assertEquals(12, p.healRate());
    }

    @Test
    void enemyApproachDoesNotOvershootOrDivideByZero() {
        Enemy e = new Enemy(10, 0, 1);
        e.approach(0, 0, 100);
        assertEquals(1.3f, e.x, .001f);
        e.x = 0;
        e.approach(0, 0, 1);
        assertTrue(Float.isFinite(e.x));
    }

    @Test
    void xpAccumulationTriggersLevelUpAndAwardsLevelPoints() {
        Progression p = new Progression();
        assertEquals(1, p.characterLevel());
        assertEquals(0, p.levelPoints());
        assertEquals(100, p.xpToNextLevel());
        assertTrue(p.addXp(100));
        assertEquals(2, p.characterLevel());
        assertEquals(1, p.levelPoints());
        assertEquals(0, p.xp());
        // Spending level point on vitality upgrades max health and consumes point
        int hpBefore = p.maxHealth();
        assertTrue(p.canPurchase(Progression.Upgrade.VITALITY));
        assertTrue(p.purchase(Progression.Upgrade.VITALITY));
        assertEquals(hpBefore + 20, p.maxHealth());
        assertEquals(0, p.levelPoints());
        assertFalse(p.canPurchase(Progression.Upgrade.VITALITY));
        assertFalse(p.purchase(Progression.Upgrade.VITALITY));
        assertFalse(p.canPurchase(Progression.Upgrade.POWER));
        assertFalse(p.purchase(Progression.Upgrade.POWER));
    }

    @Test
    void beaconFortificationsScaleDefensesAndAura() {
        Progression p = new Progression();
        for (int i = 0; i < 20; i++) p.rewardKill();
        float baseHearth = p.healRate();
        assertTrue(p.purchase(Progression.Upgrade.HEARTH));
        assertTrue(p.healRate() > baseHearth);
        assertTrue(p.purchase(Progression.Upgrade.SPIRE));
        assertEquals(2, p.spireLevel());
        assertTrue(p.purchase(Progression.Upgrade.SOLAR_WARD));
        assertEquals(1, p.solarWardLevel());
    }
}
