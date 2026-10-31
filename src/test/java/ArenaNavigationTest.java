import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ArenaNavigationTest {
    @Test void landmarksLeaveSanctuaryClearAndUseBoundedFootprints() {
        for(int c=0;c<4;c++)for(var o:ArenaNavigation.obstacles(c)) {
            assertTrue(Math.hypot(o.x(),o.z())>8);assertTrue(o.radius()>=.35f && o.radius()<=1.2f);
        }
    }
    @Test void fastMovementCannotTunnelThroughAnIsolatedTrunk() {
        var o=ArenaNavigation.obstacles(0).stream().filter(p->p.x()<-18 && p.z()>0).findFirst().orElseThrow();
        var p=ArenaNavigation.move(0,o.x()-3,o.z(),o.x()+3,o.z(),.4f);
        assertTrue(p.x()<o.x());assertTrue(Math.hypot(p.x()-o.x(),p.z()-o.z())>=o.radius()+.4f);
    }
    @Test void enemyCanSteerAroundTrunkAndReachTarget() {
        var o=ArenaNavigation.obstacles(0).stream().filter(p->p.x()<-18 && p.z()>0).findFirst().orElseThrow();
        Entities.Enemy enemy=new Entities.Enemy(o.x()-3,o.z(),1);
        float tx=o.x()+3,tz=o.z();
        for(int i=0;i<1200;i++) {
            float x=enemy.x,z=enemy.z;var steer=ArenaNavigation.steering(0,x,z,tx,tz);
            enemy.navigate(tx,tz,steer.x(),steer.z(),1f/60);
            var moved=ArenaNavigation.move(0,x,z,enemy.x,enemy.z,.4f);enemy.x=moved.x();enemy.z=moved.z();
        }
        assertTrue(enemy.distance(tx,tz)<2,"Enemy became stuck behind scenery");
    }
    @Test void overlapRecoveryAndSteeringStayFinite() {
        for(int c=0;c<4;c++)for(var o:ArenaNavigation.obstacles(c)) {
            var p=ArenaNavigation.move(c,o.x(),o.z(),o.x(),o.z(),.4f);
            assertTrue(Float.isFinite(p.x()) && Float.isFinite(p.z()));
            var steer=ArenaNavigation.steering(c,o.x()-1,o.z(),o.x()+3,o.z());
            assertTrue(Float.isFinite(steer.x()) && Float.isFinite(steer.z()));
        }
    }
}
