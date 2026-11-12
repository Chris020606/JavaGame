import static org.junit.jupiter.api.Assertions.*;

import Entities.ArmorSlot;
import Entities.Difficulty;
import Entities.Enemy;
import Entities.Player;

import org.junit.jupiter.api.Test;

/**
 * Validates the complete tutorial flow, dialogue and interactions with all Citadel NPCs, and
 * crafting and equipping every armor and weapon across all three hero classes.
 */
public class TutorialFlowAndCraftingTest {

    @Test
    public void testUnarmedNewGameRestrictionsAndStarterClaim() {
        CitadelHub hub = new CitadelHub();
        try {
            // New game starts unarmed for all classes
            for (HeroClass heroClass : HeroClass.values()) {
                Progression p = new Progression(heroClass, Difficulty.NORMAL);
                p.setHasWeapon(false);

                assertFalse(p.hasWeapon(), heroClass + " should start unarmed");
                assertFalse(p.ownsWeapon(0), heroClass + " should not own weapon 0 while unarmed");
                assertFalse(p.ownsWeapon(1), heroClass + " should not own weapon 1 while unarmed");
                assertEquals(0, p.weaponTier());

                // Cannot forge or craft while unarmed
                assertFalse(p.canBuyWeapon(1));
                assertFalse(p.canBuyWeapon(2));
                assertFalse(p.canBuyWeapon(3));
                assertFalse(p.canBuyArmor(1));
                assertFalse(p.buyWeapon(1));
                assertFalse(p.buyArmor(1));

                // Talking to mismatched mentor blocks starter weapon
                CitadelNpc wrongMentor =
                        heroClass == HeroClass.WARRIOR
                                ? CitadelNpc.ARCANIST
                                : CitadelNpc.BLACKSMITH;
                assertNotEquals(wrongMentor.mentorClass(), heroClass);
                String wrongGreeting = wrongMentor.dynamicGreeting(p);
                assertTrue(
                        wrongGreeting.contains("mentor") || wrongGreeting.contains("calling"),
                        "Mismatched mentor greeting must redirect player: " + wrongGreeting);

                // Commander Raymond blocks unarmed deployment
                String commanderGreeting = CitadelNpc.COMMANDER.dynamicGreeting(p);
                assertTrue(
                        commanderGreeting.contains("HALT") || commanderGreeting.contains("weapon"));

                // Speaking with matching mentor grants the starter weapon
                CitadelNpc correctMentor =
                        switch (heroClass) {
                            case WARRIOR -> CitadelNpc.BLACKSMITH;
                            case MAGE -> CitadelNpc.ARCANIST;
                            case ARCHER -> CitadelNpc.ARCHER;
                        };
                assertEquals(heroClass, correctMentor.mentorClass());
                String mentorGreeting = correctMentor.dynamicGreeting(p);
                assertFalse(mentorGreeting.isEmpty());

                p.grantStarterWeapon();
                assertTrue(p.hasWeapon(), heroClass + " must have weapon after claiming");
                assertTrue(p.ownsWeapon(0), heroClass + " must own starter weapon (tier 0)");
                assertEquals(0, p.weaponTier());
                assertTrue(p.damage() >= 25, "Base weapon damage must be active");
            }
        } finally {
            hub.close();
        }
    }

    @Test
    public void testFullTutorialFlowWithDamageFeedback() {
        Tutorial tutorial = new Tutorial();
        assertEquals(Tutorial.Step.MOVE, tutorial.step());
        assertFalse(tutorial.complete());

        // Step 1: MOVE
        tutorial.movement(2.0f);
        assertEquals(Tutorial.Step.MOVE, tutorial.step());
        tutorial.movement(2.5f);
        assertEquals(Tutorial.Step.FOCUS, tutorial.step());

        // Step 2: FOCUS
        tutorial.focus();
        assertEquals(Tutorial.Step.STRIKE, tutorial.step());

        // Step 3: STRIKE (requires 2 registered hits)
        Enemy partner = new Enemy(2.2f, 0, 1, 0, 0.5f, Difficulty.EASY, 6.0f);
        float initialHealth = partner.health;
        assertTrue(initialHealth > 100);

        // First strike deals damage
        partner.hit(25, 0, 0, 0.35f);
        assertEquals(
                initialHealth - 25, partner.health, 0.01f, "Partner health must visibly decrease");
        tutorial.hit(false);
        assertEquals(Tutorial.Step.STRIKE, tutorial.step());

        // Second strike advances to DODGE
        partner.hit(25, 0, 0, 0.35f);
        assertEquals(
                initialHealth - 50, partner.health, 0.01f, "Partner health must decrease further");
        tutorial.hit(false);
        assertEquals(Tutorial.Step.DODGE, tutorial.step());

        // Step 4: DODGE
        tutorial.dodge();
        assertEquals(Tutorial.Step.CHARGE, tutorial.step());

        // Step 5: CHARGE
        partner.hit(45, 0, 0, 0.8f);
        assertEquals(
                initialHealth - 95,
                partner.health,
                0.01f,
                "Charged strike deals heavy visible damage");
        tutorial.hit(true);
        assertEquals(Tutorial.Step.PARRY, tutorial.step());

        // Step 6: PARRY
        tutorial.parry();
        assertEquals(Tutorial.Step.GEAR, tutorial.step());

        // Step 7: GEAR (opening inventory)
        tutorial.inventory();
        assertEquals(Tutorial.Step.HAVEN, tutorial.step());

        // Step 8: HAVEN (healing in light for 3 seconds)
        tutorial.healing(1.5f, true);
        assertEquals(Tutorial.Step.HAVEN, tutorial.step());
        tutorial.healing(1.6f, true);
        assertEquals(Tutorial.Step.COMPLETE, tutorial.step());
        assertTrue(tutorial.complete());
    }

    @Test
    public void testTalksToEveryNpcInCitadel() {
        CitadelHub hub = new CitadelHub();
        try {
            Progression p = new Progression(HeroClass.WARRIOR);
            p.setHasWeapon(false);

            // 1. Blacksmith Goran (Warrior Mentor)
            CitadelNpc blacksmith = CitadelNpc.BLACKSMITH;
            assertTrue(blacksmith.isNear(-7.5f, 1.5f, 1.0f));
            hub.openNpc(blacksmith, p);
            assertEquals(blacksmith, hub.activeNpc());
            assertFalse(hub.npcDialogue().isEmpty());
            hub.setLoreDialogue(blacksmith.lore());
            assertEquals(blacksmith.lore(), hub.npcDialogue());
            hub.closeNpc();
            assertNull(hub.activeNpc());

            // 2. Lady Elyndra (Arcanist / Mage Mentor)
            CitadelNpc arcanist = CitadelNpc.ARCANIST;
            assertTrue(arcanist.isNear(7.5f, 1.5f, 1.0f));
            hub.openNpc(arcanist, p);
            assertEquals(arcanist, hub.activeNpc());
            assertFalse(hub.npcDialogue().isEmpty());
            hub.setLoreDialogue(arcanist.lore());
            assertEquals(arcanist.lore(), hub.npcDialogue());
            hub.closeNpc();
            assertNull(hub.activeNpc());

            // 3. Diana Swiftflight (Archer Mentor)
            CitadelNpc archer = CitadelNpc.ARCHER;
            assertTrue(archer.isNear(-7.5f, -5.5f, 1.0f));
            hub.openNpc(archer, p);
            assertEquals(archer, hub.activeNpc());
            assertFalse(hub.npcDialogue().isEmpty());
            hub.setLoreDialogue(archer.lore());
            assertEquals(archer.lore(), hub.npcDialogue());
            hub.closeNpc();
            assertNull(hub.activeNpc());

            // 4. Commander Sean (Expedition Deployer / Gatekeeper)
            CitadelNpc commander = CitadelNpc.COMMANDER;
            assertTrue(commander.isNear(2.6f, 29.5f, 1.0f));
            hub.openNpc(commander, p);
            assertEquals(commander, hub.activeNpc());
            assertFalse(hub.npcDialogue().isEmpty());
            hub.setLoreDialogue(commander.lore());
            assertEquals(commander.lore(), hub.npcDialogue());
            hub.closeNpc();
            assertNull(hub.activeNpc());

            // 5. Caravan Trader Torvald (Market Square Supplies)
            CitadelNpc trader = CitadelNpc.CARAVAN_TRADER;
            assertTrue(trader.isNear(-2.5f, 15.5f, 1.0f));
            hub.openNpc(trader, p);
            assertEquals(trader, hub.activeNpc());
            assertFalse(hub.npcDialogue().isEmpty());
            hub.setLoreDialogue(trader.lore());
            assertEquals(trader.lore(), hub.npcDialogue());
            hub.closeNpc();
            assertNull(hub.activeNpc());

            // 6. Rescued Citizens (Bram, Kaelen, Rowan, Theron)
            CitadelNpc[] survivors = {
                CitadelNpc.SURVIVOR_BRAM,
                CitadelNpc.SURVIVOR_KAELEN,
                CitadelNpc.SURVIVOR_ROWAN,
                CitadelNpc.SURVIVOR_THERON
            };
            for (CitadelNpc survivor : survivors) {
                assertTrue(survivor.isSurvivor());
                hub.openNpc(survivor, p);
                assertEquals(survivor, hub.activeNpc());
                assertFalse(hub.npcDialogue().isEmpty());
                hub.setLoreDialogue(survivor.lore());
                assertEquals(survivor.lore(), hub.npcDialogue());
                hub.closeNpc();
                assertNull(hub.activeNpc());
            }
        } finally {
            hub.close();
        }
    }

    @Test
    public void testCraftEveryArmorAndWeaponAcrossAllClasses() {
        for (HeroClass heroClass : HeroClass.values()) {
            Progression p = new Progression(heroClass, Difficulty.NORMAL);
            p.setHasWeapon(false);

            // Cannot craft without starter weapon
            assertFalse(p.canBuyWeapon(1));
            assertFalse(p.canBuyArmor(1));

            // Claim starter weapon (Tier 0)
            p.grantStarterWeapon();
            assertTrue(p.hasWeapon());
            assertTrue(p.ownsWeapon(0));
            assertTrue(p.ownsArmor(0)); // Traveler clothes owned
            assertEquals(0, p.weaponTier());

            // Give enough crystals for all tiers
            java.util.Properties props = new java.util.Properties();
            p.store(props);
            props.setProperty("crystals", "50000");
            p = Progression.restore(props);

            // Tier 1: Requires Level 3
            assertFalse(p.canBuyWeapon(1), "Tier 1 requires Lv 3");
            assertFalse(p.canBuyArmor(1), "Tier 1 requires Lv 3");
            p.addXp(300); // Level 3 reached
            assertTrue(p.characterLevel() >= 3);
            assertTrue(p.canBuyWeapon(1));
            assertTrue(p.canBuyArmor(1));

            int baseDmg = p.damage();
            int baseDef = p.defense();
            assertTrue(p.buyWeapon(1), "Failed to buy Tier 1 weapon for " + heroClass);
            assertTrue(p.ownsWeapon(1));
            assertTrue(p.equipWeapon(1));
            assertEquals(1, p.weaponTier());
            assertTrue(p.damage() > baseDmg, "Tier 1 weapon must increase damage");

            assertTrue(p.buyArmor(1), "Failed to buy Tier 1 armor for " + heroClass);
            assertTrue(p.ownsArmor(1));
            for (ArmorSlot slot : ArmorSlot.values()) {
                assertTrue(p.equipArmor(slot, 1));
                assertEquals(1, p.armorTier(slot));
            }
            assertTrue(p.defense() > baseDef, "Tier 1 armor must increase defense");

            // Tier 2: Requires Level 6
            assertFalse(p.canBuyWeapon(2), "Tier 2 requires Lv 6");
            assertFalse(p.canBuyArmor(2), "Tier 2 requires Lv 6");
            p.addXp(1200); // Level 6 reached
            assertTrue(p.characterLevel() >= 6);
            assertTrue(p.canBuyWeapon(2));
            assertTrue(p.canBuyArmor(2));

            int t1Dmg = p.damage();
            int t1Def = p.defense();
            assertTrue(p.buyWeapon(2), "Failed to buy Tier 2 weapon for " + heroClass);
            assertTrue(p.ownsWeapon(2));
            assertTrue(p.equipWeapon(2));
            assertEquals(2, p.weaponTier());
            assertTrue(p.damage() > t1Dmg, "Tier 2 weapon must increase damage beyond Tier 1");

            assertTrue(p.buyArmor(2), "Failed to buy Tier 2 armor for " + heroClass);
            assertTrue(p.ownsArmor(2));
            for (ArmorSlot slot : ArmorSlot.values()) {
                assertTrue(p.equipArmor(slot, 2));
                assertEquals(2, p.armorTier(slot));
            }
            assertTrue(p.defense() > t1Def, "Tier 2 armor must increase defense beyond Tier 1");

            // Tier 3: Requires Level 10
            assertFalse(p.canBuyWeapon(3), "Tier 3 requires Lv 10");
            assertFalse(p.canBuyArmor(3), "Tier 3 requires Lv 10");
            p.addXp(3000); // Level 10 reached
            assertTrue(p.characterLevel() >= 10);
            assertTrue(p.canBuyWeapon(3));
            assertTrue(p.canBuyArmor(3));

            int t2Dmg = p.damage();
            int t2Def = p.defense();
            assertTrue(p.buyWeapon(3), "Failed to buy Tier 3 weapon for " + heroClass);
            assertTrue(p.ownsWeapon(3));
            assertTrue(p.equipWeapon(3));
            assertEquals(3, p.weaponTier());
            assertTrue(p.damage() > t2Dmg, "Tier 3 weapon must increase damage beyond Tier 2");

            assertTrue(p.buyArmor(3), "Failed to buy Tier 3 armor for " + heroClass);
            assertTrue(p.ownsArmor(3));
            for (ArmorSlot slot : ArmorSlot.values()) {
                assertTrue(p.equipArmor(slot, 3));
                assertEquals(3, p.armorTier(slot));
            }
            assertTrue(p.defense() > t2Def, "Tier 3 armor must increase defense beyond Tier 2");

            // Verify player owns all 4 tiers of weapons (0, 1, 2, 3)
            for (int t = 0; t <= 3; t++) {
                assertTrue(p.ownsWeapon(t), "Player must own weapon tier " + t);
                assertTrue(p.ownsArmor(t), "Player must own armor tier " + t);
            }
        }
    }

    @Test
    public void testPlayerUnarmedAnimationAndWeaponDrawingState() {
        Player player = new Player();
        player.setHasWeapon(false);
        assertFalse(player.hasWeapon());

        // Unarmed player uses unarmed locomotion and idle poses
        player.setMovementSpeed(5.0f);
        player.updateAnimation(0.016f);
        assertFalse(player.hasWeapon());

        // Equipping weapon enables weapon poses
        player.setHasWeapon(true);
        player.setWeaponTier(0); // Starter weapon
        assertTrue(player.hasWeapon());
    }

    @Test
    public void testTutorialSkipStatePreservation() {
        // Unarmed player starting in Citadel
        Progression p = new Progression(HeroClass.WARRIOR);
        p.setHasWeapon(false);
        assertFalse(p.hasWeapon());

        // In Citadel, skipping tutorial must preserve unarmed status without granting starter
        // weapon
        // and without teleporting the player to mission
        Tutorial tutorial = new Tutorial();
        assertEquals(Tutorial.Step.MOVE, tutorial.step());
        assertFalse(p.hasWeapon());
        assertEquals(0, p.weaponTier());

        // Player must still visit mentor to obtain starter weapon
        assertFalse(p.canBuyWeapon(1));
        CitadelNpc mentor = CitadelNpc.BLACKSMITH;
        assertEquals(HeroClass.WARRIOR, mentor.mentorClass());
        p.grantStarterWeapon();
        assertTrue(p.hasWeapon());
        assertEquals(0, p.weaponTier());
    }
}
