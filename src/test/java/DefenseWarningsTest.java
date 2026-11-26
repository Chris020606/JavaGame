import static org.junit.jupiter.api.Assertions.*;

import Entities.Enemy;

import org.junit.jupiter.api.Test;

import java.util.List;

class DefenseWarningsTest {
    private final DefenseWarnings warnings = new DefenseWarnings();
    private final FieldEvent event = new FieldEvent();

    @Test
    void warningFollowsReleaseAndOnlyInvitesAReachableFacingParry() {
        Enemy archer = new Enemy(0, 6, 1, 3, .5f);
        archer.beginAttack(0, 0, false);
        warnings.update(0, List.of(archer), List.of(), event, 0, 3, 0);
        assertFalse(warnings.beacon().flying());
        assertFalse(warnings.parryNow());
        Projectile shot = new Projectile(archer, 1);
        warnings.update(0, List.of(), List.of(shot), event, 0, 3, 0);
        assertTrue(warnings.beacon().flying());
        assertEquals(.42f, warnings.beacon().impactIn(), .001f);
        assertTrue(warnings.parryNow());
        warnings.update(0, List.of(), List.of(shot), event, 0, 3, 180);
        assertFalse(warnings.parryNow());
        warnings.update(0, List.of(), List.of(shot), event, 0, -2, 0);
        assertFalse(warnings.parryNow());
        shot.life = .25f;
        warnings.update(0, List.of(), List.of(shot), event, 0, 3, 0);
        assertNull(warnings.beacon());
    }

    @Test
    void blockedArrowsMissesAndInterruptedShotsProduceNoAlarm() {
        Enemy archer = new Enemy(0, 6, 1, 3, .5f);
        archer.beginAttack(0, 0, false);
        archer.hit(1, 0, 7, 0);
        warnings.update(0, List.of(archer), List.of(), event, 0, 2, 0);
        assertNull(warnings.beacon());
        var cover = ArenaNavigation.obstacles(0).getFirst();
        archer.x = cover.x();
        archer.z = cover.z();
        archer.beginAttack(0, 0, false);
        warnings.update(0, List.of(archer), List.of(), event, 0, 2, 0);
        assertNull(warnings.beacon());
        archer.z = 6;
        archer.x = 0;
        archer.beginAttack(5, 6, false);
        warnings.update(0, List.of(archer), List.of(), event, 0, 2, 0);
        assertNull(warnings.beacon());
    }

    @Test
    void onlyAnActiveEscortIsThreatenedAndEarliestShotWins() {
        event.begin(2);
        Enemy hexer = new Enemy(event.x(), event.z() + 2, 2, 4, .5f);
        hexer.beginAttack(event.x(), event.z(), false);
        hexer.targetsTraveler = true;
        Enemy bow = new Enemy(event.x(), event.z() + 1, 2, 3, .5f);
        bow.beginAttack(event.x(), event.z(), false);
        bow.targetsTraveler = true;
        List<Projectile> shots = List.of(new Projectile(bow, 2), new Projectile(hexer, 2));
        warnings.update(0, List.of(), shots, event, 0, 0, 0);
        assertNull(warnings.objective());
        event.update(0, event.x(), event.z(), true);
        warnings.update(0, List.of(), shots, event, 0, 0, 0);
        assertFalse(warnings.objective().magic());
        assertEquals(.02f, warnings.objective().impactIn(), .001f);
        assertNull(warnings.beacon());
    }
}
