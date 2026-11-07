import static org.junit.jupiter.api.Assertions.*;

import Entities.Enemy;

import org.junit.jupiter.api.Test;

import java.util.*;

class AdventureSystemsTest {
    @Test
    void subareasStayLocalToTheirZoneAndRiverPadsStayOnWater() {
        assertEquals("WAYFARER'S REST", WorldLandmarks.nearby(0, 13, -3).name());
        assertEquals("REFUGEE CAMP", WorldLandmarks.nearby(2, -12, 6).name());
        assertNull(WorldLandmarks.nearby(0, 0, 0));
        assertNull(WorldLandmarks.nearby(9, 13, -3));
        var pads =
                EnvironmentLayout.props(0).stream()
                        .filter(p -> p.model().equals("lily_small"))
                        .toList();
        assertFalse(pads.isEmpty());
        for (var pad : pads) {
            assertNotNull(
                    EnvironmentLayout.riverTile(Math.round(pad.x() / 4), Math.round(pad.z() / 4)));
            assertTrue(pad.y() >= -.2f && pad.y() < 0);
        }
    }

    @Test
    void focusCyclesVisibleLivingEnemiesAndClearsStaleActors() {
        Enemy close = new Enemy(0, 4, 1),
                far = new Enemy(0, 10, 1),
                blocked = new Enemy(2, 3, 1),
                dead = new Enemy(0, 2, 1);
        dead.health = 0;
        var enemies = List.of(far, blocked, dead, close, new Enemy(0, 30, 1));
        EnemyFocus focus = new EnemyFocus();
        focus.cycle(0, 0, enemies, enemy -> enemy != blocked);
        assertSame(close, focus.selected());
        focus.cycle(0, 0, enemies, enemy -> enemy != blocked);
        assertSame(far, focus.selected());
        focus.cycle(0, 0, enemies, enemy -> enemy != blocked);
        assertSame(close, focus.selected());
        focus.update(List.of(far));
        assertNull(focus.selected());
    }

    @Test
    void chapterUnlocksRoundTripAndRejectInvalidSaveValues() {
        Progression p = new Progression();
        assertEquals(0, p.unlockedChapter());
        p.unlockChapter(2);
        p.unlockChapter(1);
        assertEquals(2, p.unlockedChapter());
        Properties values = new Properties();
        p.store(values);
        assertEquals(2, Progression.restore(values).unlockedChapter());
        values.remove("unlockedChapter");
        assertEquals(0, Progression.restore(values).unlockedChapter());
        values.setProperty("unlockedChapter", "9");
        assertThrows(IllegalArgumentException.class, () -> Progression.restore(values));
    }

    @Test
    void oldCustomBindingsKeepTheirKeysWhenFocusIsAdded() {
        Config.Controls controls = new Config.Controls();
        Properties properties = new Properties();
        controls.store(properties);
        properties.remove("key.FOCUS");
        properties.setProperty("key.FORWARD", "84");
        controls.restore(properties);
        assertEquals(84, controls.key(Config.Controls.Action.FORWARD));
        assertNotEquals(84, controls.key(Config.Controls.Action.FOCUS));
    }

    @Test
    void treeFadeOnlyAffectsTreesBetweenCameraAndHero() {
        assertEquals(.22f, SceneryVisibility.treeOpacity(0, 5, 4, 0, 0, 0, 10));
        assertEquals(1, SceneryVisibility.treeOpacity(0, -5, 4, 0, 0, 0, 10));
        assertEquals(1, SceneryVisibility.treeOpacity(0, 15, 4, 0, 0, 0, 10));
        assertEquals(1, SceneryVisibility.treeOpacity(10, 5, 4, 0, 0, 0, 10));
    }
}
