import static org.junit.jupiter.api.Assertions.*;

import Entities.Difficulty;
import Entities.Enemy;
import Entities.EnemyCombatBehavior;

import org.junit.jupiter.api.Test;

class EnemyCombatBehaviorTest {

    @Test
    void rangedArcherMaintainsUsefulDistanceAndRetreatsWhenRushed() {
        Enemy archer = new Enemy(0, 0, 4, 3, 0.75f, Difficulty.NORMAL);

        // 1. When player rushes inside retreat range (e.g. distance = 2.0m)
        archer.cooldown = 0;
        archer.navigate(0, 2.0f, 0, 0, 0.05f);
        assertEquals("RETREAT", archer.action);
        assertTrue(archer.z < 0, "Archer should backpedal away from close target");

        // 2. In tactical sweet spot (distance = 6.0m) while on attack cooldown
        archer.x = 0;
        archer.z = 0;
        archer.cooldown = 0.8f;
        archer.navigate(0, 6.0f, 0, 0, 0.05f);
        assertEquals("STRAFE", archer.action);
        assertTrue(Math.abs(archer.x) > 0, "Archer should strafe laterally while on cooldown");

        // 3. In tactical sweet spot (distance = 6.0m) with arrow ready and clear shot
        archer.x = 0;
        archer.z = 0;
        archer.cooldown = 0;
        archer.sideStepTime = 0;
        archer.navigate(0, 6.0f, 0, 0, 0.05f);
        assertEquals("APPROACH", archer.action);

        // 4. Far distance (distance = 12.0m) beyond attack range (8.0m)
        archer.x = 0;
        archer.z = 0;
        archer.navigate(0, 12.0f, 0, 0, 0.05f);
        assertEquals("APPROACH", archer.action);
        assertTrue(archer.z > 0, "Archer should advance toward far target to enter range");
    }

    @Test
    void easyDifficultyAvoidsFrustratingKiting() {
        // At distance = 4.0m:
        // On Easy, retreat threshold is 3.8m, so archer does NOT run away, letting player close in.
        Enemy easyArcher = new Enemy(0, 0, 2, 3, 0.5f, Difficulty.EASY);
        easyArcher.cooldown = 0;
        easyArcher.sideStepTime = 0;
        var easyMotion = EnemyCombatBehavior.planMotion(easyArcher, 0, 4.0f, 0, 0);
        assertEquals("APPROACH", easyMotion.action(), "Easy archer should not retreat at 4.0m");

        // On Normal, retreat threshold is 4.5m, so archer actively retreats.
        Enemy normalArcher = new Enemy(0, 0, 2, 3, 0.5f, Difficulty.NORMAL);
        normalArcher.cooldown = 0;
        var normalMotion = EnemyCombatBehavior.planMotion(normalArcher, 0, 4.0f, 0, 0);
        assertEquals(
                "RETREAT",
                normalMotion.action(),
                "Normal archer should tactically retreat at 4.0m");

        // When retreating at close range (distance = 2.0m), Easy kiting speed is gentler than
        // Normal
        var easyRetreat = EnemyCombatBehavior.planMotion(easyArcher, 0, 2.0f, 0, 0);
        var normalRetreat = EnemyCombatBehavior.planMotion(normalArcher, 0, 2.0f, 0, 0);
        float easyKiteMag = (float) Math.hypot(easyRetreat.vx(), easyRetreat.vz());
        float normalKiteMag = (float) Math.hypot(normalRetreat.vx(), normalRetreat.vz());
        assertTrue(
                easyKiteMag < normalKiteMag,
                "Easy kiting should be slower and less frustrating to chase");
    }

    @Test
    void scoutsActivelyFlankInCloseRangeWhileBrutesMaintainFrontline() {
        // Scout (variant 1) with cooldown = 0 at distance = 2.5m (close engagement) continues
        // flanking
        Enemy scout = new Enemy(0, 0, 3, 1, 0.8f, Difficulty.NORMAL);
        scout.cooldown = 0;
        var scoutMotion = EnemyCombatBehavior.planMotion(scout, 0, 2.5f, 0, 0);
        assertEquals(
                "FLANK", scoutMotion.action(), "Scout should continue flanking in close quarters");
        assertTrue(scoutMotion.vx() > 0, "Scout should have lateral flanking velocity");

        // Brute (variant 2) at distance = 2.5m marches directly forward without flanking
        Enemy brute = new Enemy(0, 0, 3, 2, 0.8f, Difficulty.NORMAL);
        brute.cooldown = 0;
        var bruteMotion = EnemyCombatBehavior.planMotion(brute, 0, 2.5f, 0, 0);
        assertEquals(
                "APPROACH", bruteMotion.action(), "Brute should march directly on the frontline");

        // Brutes have higher separation resistance than lighter raiders
        Enemy raider = new Enemy(0, 0, 3, 0, 0.5f, Difficulty.NORMAL);
        raider.cooldown = 0;
        var raiderSeparated = EnemyCombatBehavior.planMotion(raider, 0, 5.0f, 1.0f, 0);
        var bruteSeparated = EnemyCombatBehavior.planMotion(brute, 0, 5.0f, 1.0f, 0);
        assertTrue(
                bruteSeparated.vx() < raiderSeparated.vx(),
                "Brutes should resist squad separation more than raiders");
    }

    @Test
    void raidersDistributeApproachByPersonalityToPreventCrowding() {
        // Personality > 0.6 biases flank right, personality < 0.4 biases flank left
        Enemy rightRaider = new Enemy(0, 0, 3, 0, 0.85f, Difficulty.NORMAL);
        Enemy leftRaider = new Enemy(0, 0, 3, 0, 0.15f, Difficulty.NORMAL);
        Enemy centerRaider = new Enemy(0, 0, 3, 0, 0.50f, Difficulty.NORMAL);

        var rightMotion = EnemyCombatBehavior.planMotion(rightRaider, 0, 6.0f, 0, 0);
        var leftMotion = EnemyCombatBehavior.planMotion(leftRaider, 0, 6.0f, 0, 0);
        var centerMotion = EnemyCombatBehavior.planMotion(centerRaider, 0, 6.0f, 0, 0);

        assertEquals("FLANK", rightMotion.action());
        assertTrue(rightMotion.vx() > 0, "Right-biased raider should fan right");

        assertEquals("FLANK", leftMotion.action());
        assertTrue(leftMotion.vx() < 0, "Left-biased raider should fan left");

        assertEquals("APPROACH", centerMotion.action());
        assertEquals(0, centerMotion.vx(), 0.01f, "Center raider should take the direct corridor");
    }

    @Test
    void meleeRecoveryYieldsSpaceAfterAttack() {
        // Raider on cooldown (> 0.35s) within 3m steps back in RECOVER
        Enemy raider = new Enemy(0, 0, 3, 0, 0.7f, Difficulty.NORMAL);
        raider.cooldown = 0.8f;
        var motion = EnemyCombatBehavior.planMotion(raider, 0, 2.0f, 0, 0);
        assertEquals("RECOVER", motion.action());
        assertTrue(motion.vz() < 0, "Recovering melee enemy should step back from target");

        // Brute (variant 2) holds ground and does not step back
        Enemy brute = new Enemy(0, 0, 3, 2, 0.7f, Difficulty.NORMAL);
        brute.cooldown = 0.8f;
        var bruteMotion = EnemyCombatBehavior.planMotion(brute, 0, 2.0f, 0, 0);
        assertNotEquals(
                "RECOVER", bruteMotion.action(), "Brute should not retreat during recovery");
    }

    @Test
    void boundaryAwarenessDeflectsRetreatingAndStrafingEnemies() {
        // Archer trapped near positive Z boundary (z = 21.0m) with target at z = 19.0m
        // Normal retreat would push toward +Z into the boundary.
        // Boundary deflection should steer velocity inward toward the arena center.
        Enemy archer = new Enemy(0, 21.0f, 4, 3, 0.5f, Difficulty.NORMAL);
        var motion = EnemyCombatBehavior.planMotion(archer, 0, 19.0f, 0, 0);
        assertEquals("RETREAT", motion.action());
        assertTrue(
                motion.vz() < 0,
                "Near boundary, retreat should deflect inward away from the perimeter wall");

        // Strafing near arena wall should flip side to stay in open ground
        Enemy wallArcher = new Enemy(21.0f, 0, 4, 3, 0.8f, Difficulty.NORMAL);
        wallArcher.cooldown = 0.8f;
        var strafeMotion = EnemyCombatBehavior.planMotion(wallArcher, 21.0f, 6.0f, 0, 0);
        assertEquals("STRAFE", strafeMotion.action());
        assertTrue(
                strafeMotion.vx() < 0,
                "Strafe heading into wall should deflect/reverse away from wall limit");

        // Firm boundary clamping in navigate
        Enemy boundEnemy = new Enemy(21.9f, 21.9f, 3, 0, 0.5f, Difficulty.NORMAL);
        for (int i = 0; i < 30; i++) {
            boundEnemy.navigate(40.0f, 40.0f, 2.0f, 2.0f, 0.1f);
        }
        assertTrue(
                boundEnemy.x <= 22.0f && boundEnemy.z <= 22.0f,
                "Enemy should never cross arena boundary 22.0f");
    }

    @Test
    void zeroDistanceTargetResolvesGracefullyWithoutNanOrLockup() {
        Enemy enemy = new Enemy(5.0f, 5.0f, 3, 0, 0.5f, Difficulty.NORMAL);
        enemy.facing = 90.0f;

        // Target at exact same position (5.0, 5.0)
        var motion = EnemyCombatBehavior.planMotion(enemy, 5.0f, 5.0f, 0, 0);
        assertTrue(
                Float.isFinite(motion.vx()) && Float.isFinite(motion.vz()),
                "Motion must be finite at distance 0");
        assertTrue(
                Math.hypot(motion.vx(), motion.vz()) > 0,
                "Motion should provide non-zero escape direction");

        // When perfectly overlapping, enemy is considered facing target
        assertTrue(
                enemy.facingTarget(5.0f, 5.0f), "Zero distance target should be considered facing");

        // Navigate with zero-distance target
        enemy.navigate(5.0f, 5.0f, 0, 0, 0.1f);
        assertTrue(
                Float.isFinite(enemy.x) && Float.isFinite(enemy.z), "Coordinates must stay finite");
    }

    @Test
    void largeFrameStepDoesNotCauseWildTeleportationOrTunneling() {
        Enemy enemy = new Enemy(0, 0, 3, 1, 0.5f, Difficulty.NORMAL);
        float startX = enemy.x;
        float startZ = enemy.z;

        // Huge frame step (e.g. 1.0 second hitch)
        enemy.navigate(20.0f, 20.0f, 0, 0, 1.0f);

        float distanceTraveled = (float) Math.hypot(enemy.x - startX, enemy.z - startZ);
        // At speed ~3.5, 0.15s safe clamp moves at most ~0.55m
        assertTrue(
                distanceTraveled < 1.0f,
                "Single frame step should be clamped to prevent tunneling or huge jumps");
        assertTrue(Float.isFinite(enemy.x) && Float.isFinite(enemy.z));
    }

    @Test
    void bossBehaviorAndEnrageArePreserved() {
        Enemy warlord = new Enemy(0, 0, 10, 5, 0.5f, Difficulty.NORMAL);
        assertTrue(warlord.boss());
        assertEquals(3.8f, warlord.meleeRange(), 0.01f);

        // Boss approaches directly without lateral flanking
        var motion = EnemyCombatBehavior.planMotion(warlord, 0, 10.0f, 0, 0);
        assertEquals("APPROACH", motion.action());
        assertEquals(0, motion.vx(), 0.01f);
        assertTrue(motion.vz() > 0);

        // Enrage threshold at <= 50% maxHealth
        assertFalse(warlord.enraged());
        warlord.health = warlord.maxHealth * 0.49f;
        assertTrue(warlord.enraged(), "Boss should be enraged below 50% health");
    }
}
