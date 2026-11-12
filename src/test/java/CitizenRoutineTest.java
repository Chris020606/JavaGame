import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CitizenRoutineTest {
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
            for (CitadelNpc npc : CitadelNpc.RESCUED_SURVIVORS) {
                CitizenRoutine citizen = new CitizenRoutine(npc);
                for (int frame = 0; frame < 1200; frame++) {
                    citizen.update(.05f, false, hub::citizenWalkable);
                    assertTrue(hub.citizenWalkable(citizen.x, citizen.z), npc.npcName());
                }
            }
        }
    }
}
