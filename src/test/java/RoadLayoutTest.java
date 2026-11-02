import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RoadLayoutTest {
    @Test void chapterRibbonsHaveContinuousFiniteGeometryAndReachBothEnds() {
        for(int c=0;c<4;c++) {
            var road=RoadLayout.road(c);var source=EnvironmentLayout.path(c);
            assertTrue(road.getFirst().z()<source.getFirst().z()-20,"Road must enter from beyond the visual horizon");
            assertTrue(road.getLast().z()>source.getLast().z()+20,"Road must fade into scenery beyond the playable boundary");
            for(int i=0;i<road.size();i++) {
                var p=road.get(i);assertTrue(Float.isFinite(p.x()) && Float.isFinite(p.z()));
                assertEquals(1,Math.hypot(p.nx(),p.nz()),.001f);assertTrue(p.width()>1 && p.width()<1.6f);
                if(i>0)assertTrue(Math.hypot(p.x()-road.get(i-1).x(),p.z()-road.get(i-1).z())<1);
            }
        }
    }
}
