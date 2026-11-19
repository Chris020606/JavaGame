import static org.junit.jupiter.api.Assertions.*;

import Entities.Difficulty;
import Entities.Enemy;
import Entities.Player;
import Entities.PlayerAnimation;

import org.junit.jupiter.api.Test;

public class MageAndBossCombatTest {

    @Test
    void mageArmedIdleUsesStaffStanceInsteadOfUnarmed() {
        Player mage = new Player();
        mage.setWeaponStyle(2); // Mage
        mage.setHasWeapon(true);

        mage.update(0.016f);
        // Armed Mage with staff should hold it in MELEE_IDLE (two-handed staff stance)
        assertEquals(PlayerAnimation.MELEE_IDLE, mage.currentClip());

        // When disarmed, falls back to unarmed fists
        mage.setHasWeapon(false);
        mage.update(0.016f);
        assertEquals(PlayerAnimation.UNARMED_IDLE, mage.currentClip());
    }

    @Test
    void mageChargingUsesMagicSpellcastingClip() {
        Player mage = new Player();
        mage.setWeaponStyle(2);
        mage.setHasWeapon(true);
        mage.setCharging(true);

        mage.update(0.016f);
        assertEquals(PlayerAnimation.MAGIC_CAST, mage.currentClip());
    }

    @Test
    void mageAttackCombosVaryBetweenSpellcastingAndRaise() {
        Player mage = new Player();
        mage.setWeaponStyle(2);
        mage.setHasWeapon(true);

        // Combo 1 starts with MAGIC_RAISE
        mage.attack(1, false, 0.5f, 0.2f, 0f);
        assertEquals(PlayerAnimation.MAGIC_RAISE, mage.currentClip());

        // Combo 2 starts with MAGIC_CAST
        mage.attack(2, false, 0.5f, 0.2f, 0f);
        assertEquals(PlayerAnimation.MAGIC_CAST, mage.currentClip());

        // Combo 3 finisher starts with MAGIC_CAST
        mage.attack(3, false, 0.6f, 0.25f, 0f);
        assertEquals(PlayerAnimation.MAGIC_CAST, mage.currentClip());

        // Heavy attack starts with MAGIC_CAST
        mage.attack(1, true, 0.7f, 0.3f, 0.5f);
        assertEquals(PlayerAnimation.MAGIC_CAST, mage.currentClip());
    }

    @Test
    void activeParryWindowRendersDistinctDefensivePoses() {
        Player warrior = new Player();
        warrior.setWeaponStyle(0);
        warrior.discipline().configure(100, 25);
        warrior.discipline().stamina(100);

        warrior.parry();
        assertTrue(warrior.discipline().parrying());
        warrior.update(0.016f);
        assertEquals(PlayerAnimation.BLOCK_ATTACK, warrior.currentClip());

        Player mage = new Player();
        mage.setWeaponStyle(2);
        mage.discipline().configure(100, 25);
        mage.discipline().stamina(100);

        mage.parry();
        assertTrue(mage.discipline().parrying());
        mage.update(0.016f);
        assertEquals(PlayerAnimation.MAGIC_RAISE, mage.currentClip());
    }

    @Test
    void successfulDeflectTriggersBlockHitReaction() {
        Player warrior = new Player();
        warrior.setWeaponStyle(0);
        assertFalse(warrior.isDeflecting());

        warrior.triggerDeflectReaction();
        assertTrue(warrior.isDeflecting());

        warrior.update(0.016f);
        assertEquals(PlayerAnimation.BLOCK_HIT, warrior.currentClip());

        // After deflection duration (0.28s), reaction state finishes
        warrior.update(0.30f);
        assertFalse(warrior.isDeflecting());
    }

    @Test
    void enragedBossGainsSpeedOnNormalAndTighterCooldown() {
        Enemy normalBoss = new Enemy(0, 0, 5, 5, 0.5f, Difficulty.NORMAL);
        Enemy easyBoss = new Enemy(0, 0, 5, 5, 0.5f, Difficulty.EASY);

        // Before enrage (> 50% HP)
        assertFalse(normalBoss.enraged());
        normalBoss.launchCharge();
        assertEquals(0.5f, normalBoss.chargeTime, 0.001f);

        // Cross enrage threshold (< 50% HP)
        normalBoss.hit(normalBoss.maxHealth * 0.55f, 0, 0, 0);
        assertTrue(normalBoss.enraged());

        // On Normal, enraged charge is faster (0.40s)
        normalBoss.launchCharge();
        assertEquals(0.40f, normalBoss.chargeTime, 0.001f);

        // On Easy, enraged charge remains forgiving (0.50s)
        easyBoss.hit(easyBoss.maxHealth * 0.55f, 0, 0, 0);
        assertTrue(easyBoss.enraged());
        easyBoss.launchCharge();
        assertEquals(0.50f, easyBoss.chargeTime, 0.001f);

        // Enraged attack cooldown on Normal is tighter than Easy
        normalBoss.beginAttack(0, 2, true);
        easyBoss.beginAttack(0, 2, true);
        assertTrue(normalBoss.cooldown < easyBoss.cooldown);
    }
}
