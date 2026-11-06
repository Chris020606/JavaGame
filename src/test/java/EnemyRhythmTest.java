import static org.junit.jupiter.api.Assertions.*;

import Entities.Enemy;

import org.junit.jupiter.api.Test;

class EnemyRhythmTest {
    @Test
    void meleeRolesHaveDistinctReadableWindupsAndRecovery() {
        Enemy scout = new Enemy(0, 0, 3, 1, .5f),
                raider = new Enemy(0, 0, 3, 0, .5f),
                brute = new Enemy(0, 0, 3, 2, .5f);
        scout.beginAttack(1, 0, true);
        raider.beginAttack(1, 0, true);
        brute.beginAttack(1, 0, true);
        assertTrue(scout.attackDuration() < raider.attackDuration());
        assertTrue(brute.attackDuration() > raider.attackDuration());
        assertTrue(scout.attackDuration() > .25f);
        assertTrue(brute.cooldown > raider.cooldown);
        float locked = brute.attackDuration();
        brute.health = 1;
        assertEquals(locked, brute.attackDuration());
    }
}
