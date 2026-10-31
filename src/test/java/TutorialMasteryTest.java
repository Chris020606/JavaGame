import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TutorialMasteryTest {
    @Test void practiceRequiresEachDemonstrationInOrder() {
        Tutorial t=new Tutorial();t.hit(true);t.parry();t.inventory();
        assertEquals(Tutorial.Step.MOVE,t.step());
        t.movement(Float.NaN);t.movement(-5);t.movement(3);assertEquals(Tutorial.Step.MOVE,t.step());
        t.movement(1);t.hit(false);assertEquals(Tutorial.Step.STRIKE,t.step());t.hit(false);
        t.dodge();t.hit(false);assertEquals(Tutorial.Step.CHARGE,t.step());t.hit(true);
        t.parry();t.inventory();t.healing(2,true);t.healing(1,false);t.healing(2,true);
        assertFalse(t.complete());t.healing(1,true);assertTrue(t.complete());
    }
    @Test void classRewardsAreSpecificConsumedAndExpire() {
        CombatMastery m=new CombatMastery();
        m.lightHit(HeroClass.WARRIOR);assertEquals(1,m.attackBonus(HeroClass.WARRIOR,false));
        m.parry(HeroClass.WARRIOR);assertEquals(1.2f,m.attackBonus(HeroClass.WARRIOR,false));
        assertEquals(1,m.attackBonus(HeroClass.WARRIOR,true));
        m.evade(HeroClass.ARCHER);assertEquals(1.3f,m.attackBonus(HeroClass.ARCHER,false));
        for(int i=0;i<8;i++)m.lightHit(HeroClass.MAGE);
        assertEquals(3,m.stacks());assertEquals(1,m.attackBonus(HeroClass.MAGE,false));assertEquals(3,m.stacks());
        assertEquals(1.6f,m.attackBonus(HeroClass.MAGE,true));assertEquals(0,m.stacks());
        m.evade(HeroClass.ARCHER);m.tick(3.1f);assertEquals(1,m.attackBonus(HeroClass.ARCHER,false));
    }
}
