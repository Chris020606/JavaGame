import Entities.Enemy;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SanctuaryWardTest {
    @Test void wardSelectsNearestLivingThreatInsideRange() {
        Enemy distant=new Enemy(10,0,1),near=new Enemy(2,0,1),far=new Enemy(7,0,1);
        assertSame(near,SanctuaryWard.target(List.of(distant,far,near)));
        near.health=1;assertSame(far,SanctuaryWard.target(List.of(near,far)));
        assertNull(SanctuaryWard.target(List.of(distant,near)));
    }
    @Test void wardCannotKillAndDamageStaysBoundedAtHighLevels() {
        assertEquals(0,SanctuaryWard.damage(1,100));
        assertEquals(10,SanctuaryWard.damage(2,100));
        assertEquals(14,SanctuaryWard.damage(1000,100));
        for(float hp:new float[]{0,.5f,1,2,10,100})assertTrue(hp-SanctuaryWard.damage(10,hp)>=Math.min(1,hp));
    }
}
