import static org.junit.jupiter.api.Assertions.*;

import Entities.*;

import org.junit.jupiter.api.Test;

/**
 * End-to-end playtest simulation of the solo campaign on Easy and Normal.
 *
 * <p>Validates early vs late enemy durability, ranged pressure, potion availability, crystal
 * economy, and individual armor piece affordability.
 */
class CampaignBalancePlaytestTest {

    @Test
    void playtestFullSoloCampaignOnEasyAndNormal() {
        for (Difficulty difficulty : new Difficulty[] {Difficulty.EASY, Difficulty.NORMAL}) {
            for (HeroClass hero : HeroClass.values()) {
                simulateCampaign(hero, difficulty);
            }
        }
    }

    private void simulateCampaign(HeroClass hero, Difficulty difficulty) {
        Progression progress = new Progression(hero, difficulty);
        int totalCrystalsEarned = 0;
        int totalEnemiesKilled = 0;
        int totalWispsSpawned = 0;

        for (int wave = 1; wave <= 10; wave++) {
            boolean isBossWave = Campaign.chapter(wave).bossOnly();
            int baseCount = Campaign.enemies(wave);
            int count = difficulty.count(baseCount, isBossWave);

            assertTrue(count >= 1, "Wave " + wave + " must have at least 1 enemy");
            if (isBossWave) {
                assertEquals(1, count, "Boss waves must spawn exactly 1 boss");
            }

            // Inspect enemy squad composition and ranged pressure for this wave
            int rangedCount = 0;
            int meleeCount = 0;
            for (int i = 0; i < count; i++) {
                int variant = WaveFormation.variant(wave, i);
                Enemy enemy = new Enemy(0, 10, wave, variant, 0.5f, difficulty);

                if (enemy.ranged()) {
                    rangedCount++;
                } else {
                    meleeCount++;
                }

                // Verify durability scaling: player with tier-appropriate weapon kills normal
                // enemies in 1-4 hits
                float playerDamage = progress.damage();
                assertTrue(playerDamage >= 25, "Base damage must be at least 25");

                if (!enemy.boss()) {
                    float hitsToKill = enemy.maxHealth / playerDamage;
                    assertTrue(
                            hitsToKill <= 4.5f,
                            "Normal enemy in wave "
                                    + wave
                                    + " should not be excessively spongy (hits="
                                    + hitsToKill
                                    + ")");
                } else {
                    float bossHits = enemy.maxHealth / playerDamage;
                    assertTrue(
                            bossHits >= 4.0f && bossHits <= 16.0f,
                            "Boss durability should feel substantial but fair (hits="
                                    + bossHits
                                    + ")");
                }

                // Simulate kill rewards matching Game.java
                progress.rewardEnemy(enemy.boss());
                if (enemy.elite) {
                    progress.rewardElite();
                }
                totalEnemiesKilled++;
                totalCrystalsEarned += enemy.reward();

                // Recovery wisps drop every 5 kills or on elites
                if (totalEnemiesKilled % 5 == 0 || enemy.elite) {
                    totalWispsSpawned++;
                }
            }

            // Verify ranged pressure does not overwhelm melee identity
            if (!isBossWave && wave > 1) {
                float rangedRatio = (float) rangedCount / count;
                assertTrue(
                        rangedRatio <= 0.60f,
                        "Ranged enemies should never exceed 60% of a wave (ratio="
                                + rangedRatio
                                + " in wave "
                                + wave
                                + ")");
            }

            // Simulate field objective completion matching Game.java
            if (!isBossWave) {
                progress.rewardObjective();
                progress.addXp(120);
                totalCrystalsEarned += 65;
            }

            // Economy milestone checks:
            // 1. Chapter 1 (waves 1-4): Player reaches Level >= 3 and can afford Tier 1 armor and
            // weapon
            if (wave == 4) {
                assertTrue(
                        progress.characterLevel() >= 3,
                        "Player should reach at least level 3 by wave 4");
                int t1ArmorCost = progress.armorPrice(1);
                int t1WeaponCost = progress.weaponPrice(1);
                assertTrue(
                        progress.crystals() >= t1ArmorCost,
                        "Player must be able to afford Tier 1 armor set by wave 4 (has="
                                + progress.crystals()
                                + ", needed="
                                + t1ArmorCost
                                + ")");
                assertTrue(
                        progress.crystals() >= t1WeaponCost,
                        "Player must be able to afford Tier 1 weapon by wave 4");

                // Purchase Tier 1 equipment piece by piece
                for (ArmorSlot slot : ArmorSlot.values()) {
                    assertTrue(
                            progress.canBuyArmor(slot, 1), "Should be able to buy Tier 1 " + slot);
                    assertTrue(progress.buyArmor(slot, 1));
                    assertTrue(progress.equipArmor(slot, 1));
                }
                assertTrue(progress.buyWeapon(1));
                assertTrue(progress.equipWeapon(1));
                assertEquals(1, progress.weaponTier());
                assertEquals(1, progress.armorUnlocked());
            }

            // 2. Wave 5 Boss: Grants +120 crystals and +300 XP, unlocking Tier 2 (level >= 6)
            if (wave == 5) {
                assertTrue(
                        progress.characterLevel() >= 6,
                        "Boss XP (+300) should advance player to level 6 or higher (lvl="
                                + progress.characterLevel()
                                + ")");
                int t2WeaponCost = progress.weaponPrice(2);

                // By end of wave 5, player has earned enough crystals across waves 1-5 to purchase
                // Tier 2 weapon
                assertTrue(
                        progress.crystals() >= t2WeaponCost,
                        "Player should be able to purchase Tier 2 weapon after boss (crystals="
                                + progress.crystals()
                                + ")");

                assertTrue(progress.buyWeapon(2));
                assertTrue(progress.equipWeapon(2));
                assertEquals(2, progress.weaponTier());

                // Start crafting Tier 2 armor pieces with remaining crystals (chest & head)
                assertTrue(
                        progress.canBuyArmor(ArmorSlot.CHEST, 2),
                        "Should afford Tier 2 chest piece");
                assertTrue(progress.buyArmor(ArmorSlot.CHEST, 2));
                assertTrue(progress.equipArmor(ArmorSlot.CHEST, 2));

                assertTrue(
                        progress.canBuyArmor(ArmorSlot.HEAD, 2), "Should afford Tier 2 head piece");
                assertTrue(progress.buyArmor(ArmorSlot.HEAD, 2));
                assertTrue(progress.equipArmor(ArmorSlot.HEAD, 2));
                assertEquals(2, progress.armorUnlocked());
            }

            // By Wave 7: Bounty from waves 6-7 funds remaining Tier 2 armor pieces (legs & boots)
            if (wave == 7) {
                assertTrue(
                        progress.canBuyArmor(ArmorSlot.LEGS, 2),
                        "Should afford Tier 2 legs by wave 7");
                assertTrue(progress.buyArmor(ArmorSlot.LEGS, 2));
                assertTrue(progress.equipArmor(ArmorSlot.LEGS, 2));

                assertTrue(
                        progress.canBuyArmor(ArmorSlot.BOOTS, 2),
                        "Should afford Tier 2 boots by wave 7");
                assertTrue(progress.buyArmor(ArmorSlot.BOOTS, 2));
                assertTrue(progress.equipArmor(ArmorSlot.BOOTS, 2));

                for (ArmorSlot slot : ArmorSlot.values()) {
                    assertEquals(
                            2,
                            progress.armorTier(slot),
                            "All slots should be equipped with Tier 2 armor by wave 7");
                }
            }

            // 3. Chapter 2 (waves 6-9): Player reaches Level >= 9, unlocking Tier 3
            if (wave == 9) {
                assertTrue(
                        progress.characterLevel() >= 9,
                        "Player should reach Level >= 9 by wave 9 for Tier 3 equipment");
                int t3WeaponCost = progress.weaponPrice(3);

                assertTrue(
                        progress.crystals() >= t3WeaponCost,
                        "Player must afford Tier 3 weapon before final boss");

                assertTrue(progress.buyWeapon(3));
                assertTrue(progress.equipWeapon(3));
                assertEquals(3, progress.weaponTier());

                // Purchase Tier 3 armor pieces (Chest and Head priority)
                assertTrue(
                        progress.canBuyArmor(ArmorSlot.CHEST, 3),
                        "Should afford Tier 3 chest piece");
                assertTrue(progress.buyArmor(ArmorSlot.CHEST, 3));
                assertTrue(progress.equipArmor(ArmorSlot.CHEST, 3));

                assertTrue(
                        progress.canBuyArmor(ArmorSlot.HEAD, 3), "Should afford Tier 3 head piece");
                assertTrue(progress.buyArmor(ArmorSlot.HEAD, 3));
                assertTrue(progress.equipArmor(ArmorSlot.HEAD, 3));
                assertEquals(3, progress.armorUnlocked());

                // On Normal, larger squads provide surplus crystals to finish all 4 pieces before
                // the final boss
                if (progress.canBuyArmor(ArmorSlot.LEGS, 3)) {
                    assertTrue(progress.buyArmor(ArmorSlot.LEGS, 3));
                    assertTrue(progress.equipArmor(ArmorSlot.LEGS, 3));
                }
                if (progress.canBuyArmor(ArmorSlot.BOOTS, 3)) {
                    assertTrue(progress.buyArmor(ArmorSlot.BOOTS, 3));
                    assertTrue(progress.equipArmor(ArmorSlot.BOOTS, 3));
                }
            }
        }

        // Final verification after wave 10 (Hollow King defeated)
        assertTrue(
                totalWispsSpawned >= 15,
                "Wisps should drop reliably for combat recovery (total=" + totalWispsSpawned + ")");
        assertTrue(progress.potions() >= 2, "Potions should remain stocked throughout campaign");
        assertTrue(
                totalCrystalsEarned >= 1500,
                "Full campaign should provide ample crystals (total=" + totalCrystalsEarned + ")");
        assertEquals(3, progress.weaponTier());
        assertEquals(3, progress.armorUnlocked());
    }

    @Test
    void easyDifficultyIsForgivingAndReducesCombatFatigue() {
        // Verify Easy difficulty parameters
        assertTrue(
                Difficulty.EASY.health < Difficulty.NORMAL.health,
                "Easy enemies should have less health");
        assertTrue(
                Difficulty.EASY.damage < Difficulty.NORMAL.damage,
                "Easy enemies should deal less damage");
        assertTrue(
                Difficulty.EASY.speed < Difficulty.NORMAL.speed, "Easy enemies should move slower");
        assertTrue(
                Difficulty.EASY.numbers < Difficulty.NORMAL.numbers,
                "Easy should spawn fewer enemies");
        assertTrue(
                Difficulty.EASY.cadence > Difficulty.NORMAL.cadence,
                "Easy enemies should attack with longer telegraphs");
        assertEquals(1, Difficulty.EASY.attackers, "Easy should limit to 1 simultaneous attacker");

        // Verify damage comparison
        Enemy normalRaider = new Enemy(0, 0, 4, 0, 0.5f, Difficulty.NORMAL);
        Enemy easyRaider = new Enemy(0, 0, 4, 0, 0.5f, Difficulty.EASY);

        assertEquals(normalRaider.maxHealth * 0.75f, easyRaider.maxHealth, 0.01f);
        assertEquals(normalRaider.meleeDamage(4) * 0.65f, easyRaider.meleeDamage(4), 0.01f);
        assertEquals(normalRaider.speed * 0.90f, easyRaider.speed, 0.01f);
    }
}
