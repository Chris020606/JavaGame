import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class DayNightTest {
    @TempDir Path directory;
    @Test void sunAndMoonLightingFollowTheSolarArc() {
        var noon=DayNightCycle.sample(.5f);var midnight=DayNightCycle.sample(0);
        assertTrue(noon.sunY()>.9f);assertTrue(midnight.sunY()<-.9f);
        assertEquals(1,noon.daylight());assertEquals(0,midnight.daylight());
        assertTrue(noon.light().r()>midnight.light().r()*5);
        for(float p=0;p<1;p+=.01f) {
            var f=DayNightCycle.sample(p);assertTrue(f.lightY()>0);assertTrue(f.daylight()>=0 && f.daylight()<=1);
            assertEquals(1,Math.sqrt(f.sunX()*f.sunX()+f.sunY()*f.sunY()+f.sunZ()*f.sunZ()),.0001);
        }
    }
    @Test void timeWrapsAndRejectsInvalidSavedPhases() {
        DayNightCycle cycle=new DayNightCycle();cycle.phase(.99f);cycle.update(DayNightCycle.PERIOD*.02f);assertEquals(.01f,cycle.phase(),.00001);
        assertThrows(IllegalArgumentException.class,()->cycle.phase(Float.NaN));assertThrows(IllegalArgumentException.class,()->cycle.phase(1));
        assertEquals("12:00",DayNightCycle.sample(.5f).clock());assertEquals("00:00",DayNightCycle.sample(0).clock());
    }
    @Test void dawnAndDuskFadeSmoothlyAcrossHorizon() {
        var before=DayNightCycle.sample(.2499f);var after=DayNightCycle.sample(.2501f);
        assertTrue(Math.abs(before.daylight()-after.daylight())<.01f);
        assertTrue(DayNightCycle.sample(.25f).horizon().r()>DayNightCycle.sample(0).horizon().r());
    }
    @Test void savedCycleRoundTripsAndOlderSavesUseChapterMood() throws Exception {
        Path file=directory.resolve("save.properties");
        SaveGame.write(file,new SaveGame.Snapshot("Ember",0,4,0,new Progression(),new WaveDirector.State(6,0,0,8,0,21,0),List.of(),Arena.HIGHLANDS,100,.81f));
        assertEquals(.81f,SaveGame.read(file).dayPhase());
        var properties=AppData.read(file);properties.remove("dayPhase");AppData.write(file,properties);assertEquals(DayNightCycle.initial(2),SaveGame.read(file).dayPhase());
        properties.setProperty("dayPhase","NaN");AppData.write(file,properties);assertThrows(java.io.IOException.class,()->SaveGame.read(file));
    }
    @Test void bridgeCrossesTheEastWestRiverRatherThanFollowingIt() {
        var bridge=EnvironmentLayout.props(0).stream().filter(p->p.model().equals("bridge_wood")).findFirst().orElseThrow();
        assertEquals(90,bridge.yaw());assertEquals(-28,bridge.z());
        assertEquals(90,EnvironmentLayout.riverTile(-1,-7).yaw());
    }
}
