import static org.junit.jupiter.api.Assertions.*;

import Entities.Enemy;

import org.junit.jupiter.api.Test;

import java.util.List;

class RangedAimTest {
    @Test
    void assistsNearHeadingButRejectsWideDeadDistantAndBlockedTargets() {
        Enemy near = new Enemy(1, 8, 1),
                wide = new Enemy(6, 8, 1),
                far = new Enemy(0, 25, 1),
                dead = new Enemy(0, 3, 1);
        dead.health = 0;
        assertSame(near, RangedAim.target(0, 0, 0, 15, List.of(wide, far, dead, near), e -> true));
        assertNull(RangedAim.target(0, 0, 0, 15, List.of(near), e -> false));
        assertNull(RangedAim.target(0, 0, 180, 15, List.of(near), e -> true));
        Enemy centered = new Enemy(0, 9, 1);
        assertSame(centered, RangedAim.target(0, 0, 0, 15, List.of(near, centered), e -> true));
    }

    @Test
    void widerSweepsCatchNearMissesWithoutHittingFarAwayOrTwice() {
        var attack = new Combat.Attack(.5f, .2f, 14, 10, 1, "SHOT");
        for (boolean magic : new boolean[] {false, true}) {
            HeroShot shot = new HeroShot(0, 0, 0, attack, 20, magic);
            shot.update(.2f);
            Enemy near = new Enemy(magic ? .9f : .8f, 3, 1), far = new Enemy(1.5f, 3, 1);
            assertTrue(shot.hit(near));
            assertFalse(shot.hit(near));
            assertFalse(shot.hit(far));
            assertTrue(shot.z > 3.5f);
        }
    }
}
