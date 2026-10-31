import Entities.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatPresentationTest {
    @Test void animationReachesContactAtTheActualDamageTime() {
        for(HeroClass hero:HeroClass.values())for(boolean heavy:new boolean[]{false,true}) {
            Combat.Attack a=hero.attack(1,heavy);
            assertEquals(0,AttackMotion.contact(0,a.impact(),a.duration(),.45f,0),.00001);
            assertEquals(.45f,AttackMotion.contact(a.impact(),a.impact(),a.duration(),.45f,0),.00001);
            assertEquals(1,AttackMotion.contact(a.duration(),a.impact(),a.duration(),.45f,0),.00001);
            float previous=0;
            for(int i=0;i<=100;i++) {
                float pose=AttackMotion.contact(a.duration()*i/100,a.impact(),a.duration(),.45f,0);
                assertTrue(pose>=previous && pose<=1);previous=pose;
            }
            assertEquals(0,AttackMotion.recovery(a.impact(),a.impact(),a.duration()));
            assertEquals(1,AttackMotion.recovery(a.duration(),a.impact(),a.duration()));
        }
    }
    @Test void fractionalAnimationSamplesStayFiniteAndInsideNativeArrays() {
        assertEquals(9,AnimationFrames.sample(.5f,20));
        assertEquals(0,AnimationFrames.sample(Float.NaN,20));
        assertEquals(18,AnimationFrames.sample(Float.POSITIVE_INFINITY,20));
        assertEquals(0,AnimationFrames.sample(Float.NEGATIVE_INFINITY,20));
        assertEquals(18,AnimationFrames.looping(-1,20));assertEquals(0,AnimationFrames.looping(Float.NaN,20));
        assertThrows(IllegalArgumentException.class,()->AnimationFrames.sample(0,0));
    }
    @Test void fallenCharactersNeverDescendAndStayVisibleThroughTheCollapse() {
        for(boolean boss:new boolean[]{false,true}) {
            assertEquals(1,CorpsePresentation.opacity(8,boss));assertEquals(0,CorpsePresentation.height());
            assertEquals(.5f,CorpsePresentation.opacity(CorpsePresentation.lifetime(boss)-1,boss));
            assertEquals(0,CorpsePresentation.opacity(CorpsePresentation.lifetime(boss)+1,boss));
        }
    }
    @Test void waterContinuesPastTheVisibleTerrainAndHasLayeredBanks() {
        assertTrue(EnvironmentLayout.RIVER_EXTENT*4>150);
        for(int x=-EnvironmentLayout.RIVER_EXTENT;x<=EnvironmentLayout.RIVER_EXTENT;x++) {
            if(Math.abs(x)<=8)continue;
            assertEquals("ground_riverStraight",EnvironmentLayout.riverTile(x,-5).model());
        }
        for(int side:new int[]{-1,1}) {
            long trees=EnvironmentLayout.props(0).stream().filter(p->p.model().startsWith("tree_") && p.x()*side>=30 && p.z()>=-26 && p.z()<=-14).count();
            assertTrue(trees>=14,"Riverbank concealment missing on side "+side);
        }
    }
}
