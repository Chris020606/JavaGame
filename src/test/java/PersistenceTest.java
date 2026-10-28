import Entities.Enemy;
import Config.Controls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {
    @TempDir Path directory;
    private SaveGame.Snapshot snapshot() {
        Progression p=new Progression();for(int i=0;i<6;i++)p.rewardKill();p.purchase(Progression.Upgrade.CHARACTER);p.damagePlayer(17);
        Enemy enemy=new Enemy(4,7,3,3,.4f);enemy.health=25;
        return new SaveGame.Snapshot("Ada",3,4,90,p,new WaveDirector.State(3,8,13,0,1.2f,21,0),List.of(enemy));
    }
    @Test void savesRestorePlayerUpgradesAndQueuedWave() throws Exception {
        Path file=directory.resolve("save.properties");SaveGame.write(file,snapshot());SaveGame.Snapshot restored=SaveGame.read(file);
        assertEquals("Ada",restored.name());assertEquals(3,restored.x());assertEquals(90,restored.facing());
        assertEquals(2,restored.progression().characterLevel());assertEquals(108,restored.progression().health());assertEquals(45,restored.progression().crystals());
        assertEquals(8,restored.wave().remaining());assertEquals(1.2f,restored.wave().arrival());assertEquals(3,restored.enemies().get(0).variant);
        assertEquals(25,restored.enemies().get(0).health);assertTrue(restored.enemies().get(0).cooldown>=.6f);
    }
    @Test void arenaSelectionPersistsAndOlderSavesInferStage() throws Exception {
        Path file=directory.resolve("arena.properties");
        SaveGame.Snapshot s=snapshot();
        SaveGame.write(file,new SaveGame.Snapshot(s.name(),s.x(),s.z(),s.facing(),s.progression(),s.wave(),s.enemies(),Arena.HIGHLANDS));
        assertEquals(Arena.HIGHLANDS,SaveGame.read(file).arena());
        Properties p=AppData.read(file);p.remove("arena");AppData.write(file,p);
        assertEquals(Arena.WOODLAND,SaveGame.read(file).arena());
        p.setProperty("arena","missing-level");AppData.write(file,p);
        assertThrows(java.io.IOException.class,()->SaveGame.read(file));
    }
    @Test void corruptOrFutureSavesAreRejected() throws Exception {
        Path file=directory.resolve("save.properties");SaveGame.write(file,snapshot());Properties p=AppData.read(file);
        p.setProperty("player.x","NaN");AppData.write(file,p);assertThrows(java.io.IOException.class,()->SaveGame.read(file));
        p.setProperty("player.x","2");p.setProperty("version","999");AppData.write(file,p);assertThrows(java.io.IOException.class,()->SaveGame.read(file));
    }
    @Test void failedValidationPreservesExistingSaveAndSuccessfulWriteKeepsBackup() throws Exception {
        Path file=directory.resolve("save.properties");SaveGame.write(file,snapshot());byte[] before=Files.readAllBytes(file);
        SaveGame.Snapshot s=snapshot();
        assertThrows(java.io.IOException.class,()->SaveGame.write(file,new SaveGame.Snapshot("bad",Float.NaN,0,0,s.progression(),s.wave(),s.enemies())));
        assertArrayEquals(before,Files.readAllBytes(file));
        SaveGame.write(file,s);assertTrue(Files.exists(directory.resolve("save.properties.bak")));
    }
    @Test void settingsRoundTripAndBindingsRejectDuplicatesAndReservedKeys() throws Exception {
        Settings s=new Settings();s.volume=.3f;s.muted=true;s.sensitivity=.25f;s.playerName="Mira";
        s.controls.bind(Controls.Action.FORWARD,73);
        Path file=directory.resolve("settings.properties");s.save(file);
        Settings loaded=new Settings();loaded.load(file);assertEquals(.3f,loaded.volume);assertTrue(loaded.muted);assertEquals("Mira",loaded.playerName);
        assertEquals(73,loaded.controls.key(Controls.Action.FORWARD));
        assertThrows(IllegalArgumentException.class,()->loaded.controls.bind(Controls.Action.LEFT,73));
        assertThrows(IllegalArgumentException.class,()->loaded.controls.bind(Controls.Action.LEFT,256));
    }
    @Test void corruptedSettingsFallBackWithoutCrashing() throws Exception {
        Path file=directory.resolve("settings.properties");Files.writeString(file,"version=1\nvolume=NaN\n");
        Settings s=new Settings();s.load(file);assertEquals(.6f,s.volume);assertFalse(s.warning.isEmpty());
        assertEquals("Warden",Settings.cleanName("<>"));assertEquals(16,Settings.cleanName("abcdefghijklmnopqr").length());
    }
    @Test void projectileUsesSweptCollisionSoFastArrowsCannotTunnel() {
        Enemy e=new Enemy(0,0,3,3,.5f);e.beginAttack(0,10,true);Projectile arrow=new Projectile(e,3);
        arrow.update(.5f);assertTrue(arrow.hits(0,2,.5f));assertFalse(arrow.hits(2,2,.5f));
        assertTrue(e.ranged());assertEquals(8,e.attackRange());
    }
}
