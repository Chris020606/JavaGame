import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {
    @Test void slotsAndHitTargetsFitCompactAndWideWindows() {
        for(int[] size:new int[][]{{640,480},{1000,680},{1280,800},{1920,1080}}) {
            HudLayout h=new HudLayout(size[0],size[1]);
            assertTrue(h.x()>=8);assertTrue(h.x()+h.span()+8<=size[0]);
            for(int slot=0;slot<6;slot++)assertEquals(slot,h.hit(h.slotX(slot)+h.cell()/2f,h.y()+h.cell()/2f));
            assertEquals(-1,h.hit(h.slotX(0)+h.cell()+2,h.y()+10));
            assertTrue(h.contains(h.slotX(0)+h.cell()+2,h.y()+10));
            assertEquals(-1,h.hit(h.x(),h.y()-1));assertEquals(-1,h.hit(h.x(),h.y()+h.cell()));
            assertFalse(h.contains(h.x(),h.y()-20));
        }
    }
}
