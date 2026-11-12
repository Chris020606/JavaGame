import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CitadelGuideTest {
    @Test
    void unarmedClassesAreDirectedToTheirOwnMentor() {
        for (HeroClass hero : HeroClass.values()) {
            Progression progress = new Progression(hero);
            progress.setHasWeapon(false);
            var destination = CitadelGuide.destination(progress);
            assertTrue(
                    java.util.Arrays.stream(CitadelNpc.values())
                            .anyMatch(npc ->
                                    npc.mentorClass() == hero
                                            && npc.npcName().equals(destination.title())
                                            && npc.x() == destination.x()
                                            && npc.z() == destination.z()));
        }
    }

    @Test
    void compassUsesWorldAxesAndHandlesArrival() {
        var destination = new CitadelGuide.Destination("Test", "Test", 0, 0);
        assertEquals("N", destination.direction(0, 10));
        assertEquals("S", destination.direction(0, -10));
        assertEquals("E", destination.direction(-10, 0));
        assertEquals("NW", destination.direction(10, 10));
        assertEquals("HERE", destination.direction(1, 1));
        assertEquals(5, destination.distance(3, 4));
    }
}
