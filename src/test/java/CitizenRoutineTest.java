import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CitizenRoutineTest {
    @Test
    void attentionPersistsAtEdgeUntilPlayerLeaves() {
        CitizenRoutine trader = new CitizenRoutine(CitadelNpc.CARAVAN_TRADER);
        assertFalse(trader.attend(3, false));
        assertTrue(trader.attend(2, false));
        assertTrue(trader.attend(3, false));
        assertFalse(trader.attend(4, false));
        assertTrue(trader.attend(4, true));
    }

    @Test
    void citizensMovePauseForConversationAndRespectObstacles() {
        CitizenRoutine citizen = new CitizenRoutine(CitadelNpc.SURVIVOR_BRAM);
        float x = citizen.x, z = citizen.z;
        for (int i = 0; i < 400; i++) citizen.update(.05f, false, (a, b) -> true);
        assertTrue(Math.hypot(citizen.x - x, citizen.z - z) > .1);
        float pausedX = citizen.x, pausedZ = citizen.z;
        for (int i = 0; i < 200; i++) citizen.update(.05f, true, (a, b) -> true);
        assertEquals(pausedX, citizen.x);
        assertEquals(pausedZ, citizen.z);
        for (int i = 0; i < 200; i++) citizen.update(.05f, false, (a, b) -> false);
        assertEquals(pausedX, citizen.x);
        assertEquals(pausedZ, citizen.z);
    }

    @Test
    void allCitizenHomesAndPatrolsRecoverOutsideCityBuildings() {
        try (CitadelHub hub = new CitadelHub()) {
            CitadelNpc[] npcs = CitadelNpc.values();
            hub.updateCitizens(.05f, 30, 0, 6);
            float[] startX = new float[npcs.length], startZ = new float[npcs.length];
            boolean[] moved = new boolean[npcs.length];
            for (CitadelNpc npc : npcs) {
                startX[npc.ordinal()] = hub.npcX(npc);
                startZ[npc.ordinal()] = hub.npcZ(npc);
            }
            for (int frame = 0; frame < 1200; frame++) {
                hub.updateCitizens(.05f, 30, 0, 6);
                for (CitadelNpc npc : npcs) {
                    int i = npc.ordinal();
                    float x = hub.npcX(npc), z = hub.npcZ(npc);
                    assertTrue(hub.citizenWalkable(x, z), npc.npcName());
                    moved[i] |= Math.hypot(x - startX[i], z - startZ[i]) > .5;
                }
            }
            for (CitadelNpc npc : npcs) assertTrue(moved[npc.ordinal()], npc.npcName());
        }
    }
}
