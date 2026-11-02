import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
class ChapterFeatsTest {
    @Test void goalsAndRewardsAreIndependentAndPaidOnlyOnce() {
        Progression p=new Progression();ChapterFeats f=p.chapterFeats();
        f.record(0,ChapterFeats.Action.CHARGED_HIT);assertFalse(f.complete(0,0));
        f.record(0,ChapterFeats.Action.CHARGED_HIT);f.record(0,ChapterFeats.Action.OBJECTIVE);
        assertEquals(2,f.completed(0));assertEquals(0,f.completed(1));assertEquals(0,p.crystals());
        assertEquals(30,p.rewardChapterFeats(0));assertEquals(30,p.crystals());assertEquals(0,p.rewardChapterFeats(0));
        f.record(0,ChapterFeats.Action.PARRY);f.record(0,ChapterFeats.Action.PARRY);
        assertEquals(15,p.rewardChapterFeats(0));assertEquals(45,f.awarded(0));assertEquals(45,p.crystals());
    }
    @Test void bossGoalRequiresActualEvadesInsteadOfFieldObjectives() {
        ChapterFeats f=new ChapterFeats();f.record(1,ChapterFeats.Action.OBJECTIVE);assertEquals(0,f.progress(1,2));
        for(int i=0;i<3;i++)f.record(1,ChapterFeats.Action.EVADE);
        assertTrue(f.complete(1,2));assertEquals(15,f.claim(1));assertEquals(0,f.progress(3,2));
    }
    @Test void savePreservesPartialGoalsAndAwardFlagsAndRejectsUnearnedAwards() {
        Progression p=new Progression();p.chapterFeats().record(2,ChapterFeats.Action.OBJECTIVE);p.rewardChapterFeats(2);
        p.chapterFeats().record(2,ChapterFeats.Action.PARRY);Properties props=new Properties();p.store(props);
        Progression restored=Progression.restore(props);assertEquals(1,restored.chapterFeats().progress(2,1));
        assertTrue(restored.chapterFeats().paid(2,2));assertEquals(0,restored.rewardChapterFeats(2));assertEquals(15,restored.crystals());
        props.setProperty("feats.0.paid","1");assertThrows(IllegalArgumentException.class,()->Progression.restore(props));
        assertEquals(0,ChapterFeats.restore(new Properties()).completed(0));
    }
}
