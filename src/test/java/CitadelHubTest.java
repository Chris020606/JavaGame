import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Properties;

public class CitadelHubTest {

    @Test
    public void testCitadelNpcPositionsAndProximity() {
        // Goran the Blacksmith at (-7.5, 0, 1.5)
        CitadelNpc blacksmith = CitadelNpc.BLACKSMITH;
        assertTrue(blacksmith.npcName().contains("Goran"));
        assertTrue(blacksmith.isNear(-7.5f, 1.5f, 1.0f));
        assertFalse(blacksmith.isNear(0f, 0f, 2.0f));

        // Lady Elyndra the Arcanist at (7.5, 0, 1.5)
        CitadelNpc arcanist = CitadelNpc.ARCANIST;
        assertTrue(arcanist.npcName().contains("Elyndra"));
        assertTrue(arcanist.isNear(7.5f, 1.5f, 1.0f));
        assertFalse(arcanist.isNear(-7.5f, 1.5f, 2.0f));

        // Commander Sean at South Gate exit (2.6, 0, 29.5)
        CitadelNpc commander = CitadelNpc.COMMANDER;
        assertTrue(commander.npcName().contains("Sean"));
        assertTrue(commander.isNear(2.6f, 29.5f, 1.0f));
        assertFalse(commander.isNear(0f, 10f, 2.0f));

        // Caravan Trader Torvald in Market Square (-5.5, 0, 15.5)
        CitadelNpc caravan = CitadelNpc.CARAVAN_TRADER;
        assertTrue(caravan.npcName().contains("Torvald"));
        assertTrue(caravan.isNear(-2.5f, 15.5f, 1.0f));
        assertFalse(caravan.isSurvivor());

        // Archer Mentor Diana at (-7.5, 0, -5.5)
        CitadelNpc archer = CitadelNpc.ARCHER;
        assertTrue(archer.npcName().contains("Diana"));
        assertTrue(archer.isNear(-7.5f, -5.5f, 1.0f));
        assertFalse(archer.isSurvivor());
    }

    @Test
    public void testRescuedSurvivorsInCitadel() {
        // Rescued survivors list
        assertEquals(6, CitadelNpc.RESCUED_SURVIVORS.length);
        CitadelNpc bram = CitadelNpc.SURVIVOR_BRAM;
        assertTrue(bram.isSurvivor());
        assertTrue(bram.npcName().contains("Bram"));
        assertTrue(bram.isNear(-8.0f, 7.5f, 1.0f));

        CitadelNpc kaelen = CitadelNpc.SURVIVOR_KAELEN;
        assertTrue(kaelen.isSurvivor());
        assertTrue(kaelen.npcName().contains("Kaelen"));
        assertTrue(kaelen.isNear(-10.0f, 11.5f, 1.0f));

        // When 0 rescued, no survivors are active
        assertEquals(0, CitadelNpc.rescuedNpcs(0).length);

        // When 2 rescued, first 2 survivors are active
        CitadelNpc[] activeTwo = CitadelNpc.rescuedNpcs(2);
        assertEquals(2, activeTwo.length);
        assertEquals(bram, activeTwo[0]);
        assertEquals(kaelen, activeTwo[1]);

        // Hub checks nearby NPCs including rescued survivors
        CitadelHub hub = new CitadelHub();
        try {
            assertNull(hub.checkNearbyNpc(-8.0f, 7.5f, 0));
            assertEquals(bram, hub.checkNearbyNpc(-8.0f, 7.5f, 1));
            assertEquals(kaelen, hub.checkNearbyNpc(-10.0f, 11.5f, 2));
        } finally {
            hub.close();
        }
    }

    @Test
    public void testRescuedSurvivorsPersistence() {
        Properties props = new Properties();
        Progression p = new Progression();
        assertEquals(0, p.rescuedSurvivors());
        p.rescueSurvivor();
        p.rescueSurvivor();
        assertEquals(2, p.rescuedSurvivors());

        p.store(props);
        Progression restored = Progression.restore(props);
        assertEquals(2, restored.rescuedSurvivors());
    }

    @Test
    public void testGearProgressionLevelRequirements() {
        // Tier 0 requires Lv 1
        assertEquals(1, Progression.requiredLevel(0));
        // Tier 1 requires Lv 3 (extended from Lv 1)
        assertEquals(3, Progression.requiredLevel(1));
        // Tier 2 requires Lv 6 (extended from Lv 3)
        assertEquals(6, Progression.requiredLevel(2));
        // Tier 3 endgame requires Lv 10 (extended from Lv 6)
        assertEquals(10, Progression.requiredLevel(3));
    }

    @Test
    public void testProgressionLevelLockEnforcement() {
        Properties props = new Properties();
        new Progression().store(props);
        props.setProperty("crystals", "10000");
        Progression p = Progression.restore(props);

        // At start (level 1), cannot buy Tier 1 (needs Lv 3) or higher
        assertEquals(1, p.characterLevel());
        assertFalse(p.canBuyWeapon(1));
        assertFalse(p.canBuyWeapon(2));
        assertFalse(p.canBuyWeapon(3));
        assertFalse(p.canBuyArmor(1));
        assertFalse(p.canBuyArmor(2));
        assertFalse(p.canBuyArmor(3));

        // Gain XP to reach level 3
        p.addXp(300);
        assertTrue(p.characterLevel() >= 3);
        assertTrue(p.canBuyWeapon(1));
        assertTrue(p.canBuyArmor(1));
        assertFalse(p.canBuyWeapon(2));
        assertFalse(p.canBuyWeapon(3));

        // Gain XP to reach level 6
        p.addXp(1200);
        assertTrue(p.characterLevel() >= 6);
        assertTrue(p.canBuyWeapon(2));
        assertTrue(p.canBuyArmor(2));
        assertFalse(p.canBuyWeapon(3));
        assertFalse(p.canBuyArmor(3));

        // Gain XP to reach level 10
        p.addXp(3000);
        assertTrue(p.characterLevel() >= 10);
        assertTrue(p.canBuyWeapon(3));
        assertTrue(p.canBuyArmor(3));
    }

    @Test
    public void testCitadelHubPortalProximityAndTeleport() {
        CitadelHub hub = new CitadelHub();
        try {
            // Portal is at South Wall exit (0, 0, 29.5) with radius 2.2
            assertTrue(hub.isNearPortal(0f, 29.5f));
            assertTrue(hub.isNearPortal(1.5f, 29.5f));
            assertFalse(hub.isNearPortal(0f, 0f));

            // Teleport timer lifecycle
            assertFalse(hub.isTeleporting());
            hub.startTeleport();
            assertTrue(hub.isTeleporting());
            assertEquals(1.2f, hub.teleportTimer(), 0.01f);

            // Update partial time
            boolean completed = hub.updateTeleport(0.5f);
            assertFalse(completed);
            assertTrue(hub.isTeleporting());

            // Complete teleport
            completed = hub.updateTeleport(0.8f);
            assertTrue(completed);
            assertFalse(hub.isTeleporting());

            // NPC interaction
            assertNull(hub.activeNpc());
            hub.openNpc(CitadelNpc.BLACKSMITH);
            assertEquals(CitadelNpc.BLACKSMITH, hub.activeNpc());
            assertTrue(hub.npcDialogue().contains("Beacon"));

            hub.setLoreDialogue("Testing lore");
            assertEquals("Testing lore", hub.npcDialogue());

            hub.closeNpc();
            assertNull(hub.activeNpc());
            assertEquals("", hub.npcDialogue());
        } finally {
            hub.close();
        }
    }

    @Test
    public void testCitadelStructureCollision() {
        CitadelHub hub = new CitadelHub();
        try {
            Entities.Player player = new Entities.Player();

            // 1. Moving deep into Castle Keep at (0, -36) is blocked and pushed out
            player.getPosition().x(0f).z(-36.0f);
            hub.resolvePlayerCollision(player);
            assertTrue(player.getPosition().z() >= -35.5f, "Should be pushed south of castle keep");

            // 2. Cathedral obstacle at (21.5, -15) blocks entry
            player.getPosition().x(21.5f).z(-15.0f);
            hub.resolvePlayerCollision(player);
            assertTrue(
                    player.getPosition().x() <= 17.0f || player.getPosition().z() >= -10.0f,
                    "Should be pushed outside cathedral collision box");

            // 3. Central avenue at (0, 16) and (0, 0) is completely unobstructed
            player.getPosition().x(0f).z(16.0f);
            hub.resolvePlayerCollision(player);
            assertEquals(0f, player.getPosition().x(), 0.01f);
            assertEquals(16.0f, player.getPosition().z(), 0.01f);

            player.getPosition().x(0f).z(0f);
            hub.resolvePlayerCollision(player);
            assertEquals(0f, player.getPosition().x(), 0.01f);
            assertEquals(0f, player.getPosition().z(), 0.01f);

            // 4. Moving into West Wall from inside at (-34.2, 0) is pushed east
            player.getPosition().x(-34.2f).z(0f);
            hub.resolvePlayerCollision(player);
            assertTrue(
                    player.getPosition().x() >= -33.6f,
                    "Should be pushed east inside the west wall");

            // 5. Moving into Front Wall at South Gate (0, 34.0) is blocked and pushed back north
            player.getPosition().x(0f).z(34.0f);
            hub.resolvePlayerCollision(player);
            assertTrue(
                    player.getPosition().z() <= 31.35f,
                    "Should be blocked by front wall gate and cannot walk through it");

            // 6. Perimeter boundary clamp prevents clipping past fortress limits
            player.getPosition().x(50.0f).z(50.0f);
            hub.resolvePlayerCollision(player);
            assertTrue(player.getPosition().x() <= 33.5f);
            assertTrue(player.getPosition().z() <= 31.35f);
        } finally {
            hub.close();
        }
    }

    @Test
    public void testStarterWeaponAndMentorMatching() {
        // Mentor class matching
        assertEquals(HeroClass.WARRIOR, CitadelNpc.BLACKSMITH.mentorClass());
        assertEquals(HeroClass.MAGE, CitadelNpc.ARCANIST.mentorClass());
        assertEquals(HeroClass.ARCHER, CitadelNpc.ARCHER.mentorClass());
        assertNull(CitadelNpc.COMMANDER.mentorClass());
        assertNull(CitadelNpc.SURVIVOR_BRAM.mentorClass());

        // Unarmed player mechanics at start of new game
        Progression p = new Progression(HeroClass.WARRIOR);
        p.setHasWeapon(false);
        assertFalse(p.hasWeapon());
        assertEquals(0, p.weaponTier());
        assertEquals(5, p.damage()); // Unarmed fist damage
        assertFalse(p.canBuyWeapon(1)); // Cannot forge without starter weapon

        // Mentor grants starter weapon
        p.grantStarterWeapon();
        assertTrue(p.hasWeapon());
        assertEquals(0, p.weaponTier());
        assertTrue(p.damage() > 5); // Full weapon damage applied
        assertTrue(p.ownsWeapon(0));

        // Dynamic greeting test
        Progression unarmedMage = new Progression(HeroClass.MAGE);
        unarmedMage.setHasWeapon(false);
        assertTrue(CitadelNpc.ARCANIST.dynamicGreeting(unarmedMage).contains("Sun-Wand"));
        assertTrue(CitadelNpc.BLACKSMITH.dynamicGreeting(unarmedMage).contains("Mage"));
        assertTrue(CitadelNpc.COMMANDER.dynamicGreeting(unarmedMage).contains("HALT"));

        // WorkshopScreen mentor matching
        WorkshopScreen workshop = new WorkshopScreen();
        assertNull(workshop.mentor());
        workshop.setMentor(HeroClass.WARRIOR);
        assertEquals(HeroClass.WARRIOR, workshop.mentor());
    }

    @Test
    public void testStrictClassMentorForgingAndGatekeeperModel() {
        // Commander Sean uses the distinct heavy raider/commander model and stands at south gate
        assertEquals("resources/outpost/raider.glb", CitadelNpc.COMMANDER.modelPath());
        assertTrue(CitadelNpc.COMMANDER.isNear(2.6f, 29.5f, 1.0f));

        // Diana the Huntress is the dedicated Archer Mentor at the central sanctuary range (-7.5,
        // -5.5)
        assertEquals(HeroClass.ARCHER, CitadelNpc.ARCHER.mentorClass());
        assertTrue(CitadelNpc.ARCHER.isNear(-7.5f, -5.5f, 1.0f));

        // Inventory default key is now KEY_E
        assertEquals(com.raylib.Raylib.KEY_E, Config.Controls.Action.INVENTORY.defaultKey);

        // Strict mentor matching: Warrior progression with Mage active mentor
        Progression warrior = new Progression(HeroClass.WARRIOR);
        warrior.grantStarterWeapon();
        assertTrue(warrior.hasWeapon());

        // Dynamic greeting checks for mismatched mentors
        String mageToWarrior = CitadelNpc.ARCANIST.dynamicGreeting(warrior);
        assertTrue(mageToWarrior.contains("mentor of Mages"));
        assertTrue(mageToWarrior.contains("Warrior"));

        String blacksmithToMage =
                CitadelNpc.BLACKSMITH.dynamicGreeting(new Progression(HeroClass.MAGE));
        assertTrue(blacksmithToMage.contains("mentor of Warriors"));

        String archerToWarrior = CitadelNpc.ARCHER.dynamicGreeting(warrior);
        assertTrue(archerToWarrior.contains("mentor of Archers"));
    }

    @Test
    public void testCitadelCityBoundaryDeletedAndDoorsReachable() {
        CitadelHub hub = new CitadelHub();
        try {
            Entities.Player player = new Entities.Player();

            // Default bounds are enabled for arena combat [-22, 22]
            assertTrue(player.isBoundsEnabled());

            // In Citadel, bounds are cleared (no artificial boundaries anywhere in the city)
            player.clearBounds();
            assertFalse(player.isBoundsEnabled());

            // Player can freely reach South Gate approach and portal at Z = 29.5f (far past old
            // 22.0f bound)
            player.getPosition().x(0f).z(29.5f);
            hub.resolvePlayerCollision(player);
            assertEquals(29.5f, player.getPosition().z(), 0.01f);
            assertEquals(0f, player.getPosition().x(), 0.01f);

            // Front wall gate firmly collides and stops player from walking through the door at Z =
            // 34.0f
            player.getPosition().x(0f).z(34.0f);
            hub.resolvePlayerCollision(player);
            assertTrue(
                    player.getPosition().z() <= 31.35f,
                    "Front wall gate must block player from walking through");

            // Player can reach Castle Keep Door at Z = -35.0f without being clamped to -22.0f
            player.getPosition().x(0f).z(-35.0f);
            hub.resolvePlayerCollision(player);
            assertEquals(-35.0f, player.getPosition().z(), 0.01f);
            assertEquals(0f, player.getPosition().x(), 0.01f);

            // Player can explore outer east fields (X = 32.0f) without artificial boundary
            player.getPosition().x(32.0f).z(0f);
            hub.resolvePlayerCollision(player);
            assertEquals(32.0f, player.getPosition().x(), 0.01f);

            // Player can explore outer west fields (X = -32.0f) without artificial boundary
            player.getPosition().x(-32.0f).z(0f);
            hub.resolvePlayerCollision(player);
            assertEquals(-32.0f, player.getPosition().x(), 0.01f);

            // Re-enabling bounds (deploying to expedition arena) restores the 22.0f arena bounds
            player.resetBounds();
            assertTrue(player.isBoundsEnabled());
        } finally {
            hub.close();
        }
    }
}
