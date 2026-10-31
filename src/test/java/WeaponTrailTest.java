import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponTrailTest {
    @Test void historyIsBoundedAndExpiresWithoutNewSamples() {
        WeaponTrail t=new WeaponTrail();
        for(int i=0;i<100;i++)t.sample(i*.02f,1,0,i*.02f,2,0);
        assertEquals(WeaponTrail.CAPACITY,t.size());assertEquals(1.52f,t.coordinate(0,0),.0001f);
        t.update(.045f);assertEquals(.5f,t.opacity(0),.0001f);
        t.update(.05f);assertEquals(0,t.size());
    }
    @Test void stationaryFramesTeleportsAndNewAttacksCannotMakeLargeSheets() {
        WeaponTrail t=new WeaponTrail();t.sample(0,1,0,0,2,0);t.sample(0,1,0,0,2,0);
        assertEquals(1,t.size());t.sample(20,1,0,20,2,0);assertEquals(1,t.size());
        t.sample(Float.NaN,0,0,0,0,0);assertEquals(0,t.size());
        t.sample(0,1,0,0,2,0);t.begin(true);assertEquals(0,t.size());
        t.sample(0,1,0,0,2,0);t.update(.1f);assertEquals(1,t.size());t.update(.03f);assertEquals(0,t.size());
    }
}
