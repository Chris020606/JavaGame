import static org.junit.jupiter.api.Assertions.*;

import Entities.Enemy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

class BossEquipmentTest {
    @TempDir Path directory;

    @Test
    void exactlyOneBossArrivesEveryFifthWaveAndResumeDoesNotDuplicateIt() {
        WaveDirector director = new WaveDirector(new Random(19));
        Map<Integer, Integer> bosses = new HashMap<>(), counts = new HashMap<>();
        boolean resumed = false;
        for (int i = 0; i < 20000; i++) {
            if (director.levelCleared(0)) {
                if (director.wave() >= 10) break;
                director.advanceLevel();
            }
            WaveDirector.Spawn spawn = director.update(.1f, 0);
            if (spawn == null) continue;
            counts.merge(spawn.wave(), 1, Integer::sum);
            if (spawn.variant() == 5) {
                bosses.merge(spawn.wave(), 1, Integer::sum);
                WaveDirector copy = new WaveDirector(new Random(20));
                copy.restore(director.snapshot());
                director = copy;
                resumed = true;
            }
        }
        assertTrue(resumed);
        assertEquals(Map.of(5, 1, 10, 1), bosses);
        for (int wave = 1; wave <= 10; wave++)
            assertEquals(Campaign.enemies(wave), counts.get(wave));
    }

    @Test
    void bossIsMeleeAndCannotBeStunLockedDuringItsLongTelegraph() {
        Enemy boss = new Enemy(0, 2, 5, 5, .5f), raider = new Enemy(0, 2, 5);
        assertTrue(boss.boss());
        assertFalse(boss.ranged());
        assertEquals(450, boss.maxHealth);
        assertTrue(boss.speed < raider.speed);
        assertTrue(boss.meleeDamage(5) > raider.meleeDamage(5));
        boss.beginAttack(0, 0, true);
        boss.hit(25, 0, 0, 1);
        assertEquals(425, boss.health);
        assertTrue(boss.impactPending);
        assertEquals(1.2f, boss.attackTime);
        assertEquals(0, boss.stun);
        assertEquals(2.15f, boss.z, .001f);
        boss.approach(8, 8, .2f);
        assertEquals(0, boss.x);
        assertEquals(180, boss.facing);
        assertEquals(1, boss.attackTime, .001f);
    }

    @Test
    void bossRewardsCurrencyAndExperienceWithoutGrantingEquipment() {
        Progression p = new Progression(HeroClass.ARCHER);
        p.rewardEnemy(true);
        assertEquals(120, p.crystals());
        assertEquals(3, p.characterLevel());
        assertEquals(0, p.weaponUnlocked());
        assertEquals(-1, p.lastBossWeapon());
        for (int tier = 1; tier <= 3; tier++) {
            assertFalse(p.ownsWeapon(tier));
            assertFalse(p.ownsArmor(tier));
        }
    }

    @Test
    void bossAndEquipmentRoundTripAndVersionOneMigrates() throws Exception {
        Progression p = new Progression(HeroClass.MAGE);
        p.rewardEnemy(true);
        p.rewardEnemy(false);
        p.rewardEnemy(false);
        assertTrue(p.buyWeapon(1));
        p.equipWeapon(1);
        Enemy boss = new Enemy(0, 12, 5, 5, .5f);
        boss.health = 320;
        Path file = directory.resolve("save.properties");
        SaveGame.write(
                file,
                new SaveGame.Snapshot(
                        "Mage",
                        0,
                        8,
                        0,
                        p,
                        new WaveDirector.State(5, 0, 1, 8, 0, 21, 0),
                        List.of(boss)));
        SaveGame.Snapshot loaded = SaveGame.read(file);
        assertTrue(loaded.enemies().get(0).boss());
        assertEquals(320, loaded.enemies().get(0).health);
        assertEquals(1, loaded.progression().weaponTier());
        assertEquals(53, loaded.progression().damage());
        Properties props = AppData.read(file);
        props.setProperty("weaponTier", "2");
        AppData.write(file, props);
        assertThrows(java.io.IOException.class, () -> SaveGame.read(file));
        props.setProperty("version", "1");
        props.remove("weaponTier");
        props.remove("weaponUnlocked");
        props.setProperty("enemy.0.variant", "0");
        props.setProperty("enemy.0.health", "50");
        AppData.write(file, props);
        loaded = SaveGame.read(file);
        assertEquals(HeroClass.MAGE, loaded.progression().heroClass());
        assertEquals(0, loaded.progression().weaponTier());
        assertEquals(0, loaded.progression().weaponUnlocked());
        props.setProperty("version", "2");
        AppData.write(file, props);
        assertThrows(java.io.IOException.class, () -> SaveGame.read(file));
    }
}
