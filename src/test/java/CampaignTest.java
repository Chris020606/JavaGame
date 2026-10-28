import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class CampaignTest {
    @TempDir Path directory;
    @Test void routeHasFourChaptersAndTwoSingleBossDuels() {
        assertEquals(4,Campaign.CHAPTERS.length);
        assertEquals(4,Campaign.chapter(1).waves());
        assertEquals(0,Campaign.index(4));assertEquals(1,Campaign.index(5));
        assertEquals(2,Campaign.index(6));assertEquals(3,Campaign.index(10));
        assertEquals(1,Campaign.enemies(5));assertEquals(1,Campaign.enemies(10));
        assertFalse(Campaign.chapter(6).bossOnly());
        assertEquals(0,Campaign.chapter(1).cleared(0,0,0,0));
        assertEquals(3,Campaign.chapter(1).cleared(4,0,1,16));
        assertEquals(4,Campaign.chapter(1).cleared(4,0,0,16));
    }
    @Test void levelEndWaitsForAllEnemiesAndCannotSkipIntoNextChapter() {
        WaveDirector d=new WaveDirector(new Random(3));
        d.restore(new WaveDirector.State(4,0,16,8,0,21,0));
        assertFalse(d.levelCleared(1));assertTrue(d.levelCleared(0));
        for(int i=0;i<100;i++) assertNull(d.update(1,0));
        assertEquals(4,d.wave());
        d.advanceLevel();assertEquals(1,d.chapterIndex());assertTrue(d.chapter().bossOnly());
        WaveDirector.Spawn boss=null;
        for(int i=0;i<100 && boss==null;i++) boss=d.update(.2f,0);
        assertNotNull(boss);assertEquals(5,boss.variant());assertEquals(5,boss.wave());
        assertEquals(0,d.remaining());assertEquals(1,d.total());assertFalse(d.levelCleared(1));
        assertTrue(d.levelCleared(0));
    }
    @Test void finalDuelEndsCampaignWithoutAnEleventhWave() {
        WaveDirector d=new WaveDirector(new Random(4));
        d.restore(new WaveDirector.State(10,0,1,8,0,21,0));
        assertTrue(d.levelCleared(0));d.advanceLevel();
        for(int i=0;i<100;i++) assertNull(d.update(1,0));
        assertEquals(10,d.wave());
    }
    @Test void savedChapterBoundaryAndAcknowledgedBoundaryRemainDistinct() throws Exception {
        Path file=directory.resolve("save.properties");
        WaveDirector d=new WaveDirector(new Random(2));
        d.restore(new WaveDirector.State(4,0,16,8,0,21,0));
        SaveGame.write(file,new SaveGame.Snapshot("Lore",0,4,0,new Progression(),d.snapshot(),List.of(),Arena.HIGHLANDS));
        d.restore(SaveGame.read(file).wave());assertTrue(d.levelCleared(0));
        d.advanceLevel();
        SaveGame.write(file,new SaveGame.Snapshot("Lore",0,4,0,new Progression(),d.snapshot(),List.of(),Arena.HIGHLANDS));
        d.restore(SaveGame.read(file).wave());assertFalse(d.levelCleared(0));assertEquals(1,d.chapterIndex());
    }
}
