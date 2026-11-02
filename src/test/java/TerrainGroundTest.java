import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerrainGroundTest {
    @Test void landCoversEveryDryCellAndLeavesRecessedWaterOpen() {
        for(int chapter=0;chapter<4;chapter++) {
            var strips=EnvironmentLayout.groundStrips(chapter);
            int extent=chapter==0?EnvironmentLayout.RIVER_EXTENT:8;
            for(int z=-8;z<=8;z++)for(int x=-extent;x<=extent;x++) {
                float px=x*4,pz=z*4;
                long covering=strips.stream().filter(s->Math.abs(px-s.x())<s.width()/2 && Math.abs(pz-s.z())<s.depth()/2).count();
                boolean water=chapter==0 && EnvironmentLayout.riverTile(x,z)!=null;
                assertEquals(water?0L:1L,covering,"chapter="+chapter+" cell="+x+","+z);
            }
            assertTrue(strips.size()<40,"Land should remain batched rather than drawing individual tiles");
        }
    }
}
