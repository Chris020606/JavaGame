import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LivingExpeditionTest {
    @Test void eachSiegeWaveEarnsExactlyOnePersistentDraftAndBossesDoNot() {
        Expedition e=new Expedition();assertTrue(e.waveCleared(1));assertFalse(e.waveCleared(1));assertFalse(e.waveCleared(0));
        assertEquals(1,e.embers());var initial=e.offers();assertEquals(3,initial.size());
        assertFalse(e.choose(Expedition.Blessing.WAYFARER));assertTrue(e.choose(initial.get(0)));assertEquals(0,e.embers());
        assertFalse(e.choose(initial.get(0)));e.waveCleared(4);assertFalse(e.waveCleared(5));assertEquals(1,e.embers());
        assertNotEquals(initial,e.offers());Properties p=new Properties();e.store(p);Expedition copy=Expedition.restore(p);
        assertEquals(e.offers(),copy.offers());assertEquals(e.rank(initial.get(0)),copy.rank(initial.get(0)));assertFalse(copy.waveCleared(4));
        p.setProperty("expedition.choices","10");assertThrows(IllegalArgumentException.class,()->Expedition.restore(p));
    }
    @Test void draftsRespectRankCapsAndDoNotRerollWhenReopened() {
        Expedition e=new Expedition();for(int wave=1;wave<=10;wave++)e.waveCleared(wave);
        for(int i=0;i<8;i++) {
            var offer=e.offers();assertEquals(offer,e.offers());assertTrue(e.choose(offer.get(0)));
        }
        for(var b:Expedition.Blessing.values())assertTrue(e.rank(b)<=3);
        assertTrue(e.offers().isEmpty());
    }
    @Test void cacheChannelRequiresProximityAndContinuousInteractionAndPaysOnlyOnce() {
        FieldEvent event=new FieldEvent();event.begin(1);float x=event.x(),z=event.z();
        event.update(1,x,z,true);assertEquals(1,event.progress());event.interrupt();assertEquals(0,event.progress());
        event.update(1,x+5,z,true);assertEquals(0,event.progress());event.update(1,x,z,true);event.update(1,x,z,true);
        assertEquals(FieldEvent.Status.COMPLETE,event.status());assertTrue(event.claim());assertFalse(event.claim());
        event.begin(1);assertEquals(FieldEvent.Status.COMPLETE,event.status());
        Properties p=new Properties();event.store(p);assertFalse(FieldEvent.restore(p).claim());
    }
    @Test void survivorMustBeLedHomeAndCanBeKilledOrAbandoned() {
        FieldEvent event=new FieldEvent();event.begin(2);float x=event.x(),z=event.z();
        event.update(.1f,x,z,true);assertTrue(event.vulnerable());
        float before=event.x();event.update(1,22,22,false);assertEquals(before,event.x());
        for(int i=0;i<200 && event.visible();i++)event.update(.1f,event.x()*.6f,event.z()*.6f,false);
        assertEquals(FieldEvent.Status.COMPLETE,event.status());assertTrue(event.claim());
        FieldEvent killed=new FieldEvent();killed.begin(2);killed.update(.1f,killed.x(),killed.z(),true);killed.damage(200);
        assertEquals(FieldEvent.Status.FAILED,killed.status());assertFalse(killed.claim());
        FieldEvent abandoned=new FieldEvent();abandoned.begin(2);abandoned.update(65,0,0,false);assertEquals(FieldEvent.Status.FAILED,abandoned.status());
    }
    @Test void beaconRequiresARealDefenseAndSavedTimersResume() {
        FieldEvent event=new FieldEvent();event.begin(3);event.update(.1f,event.x(),event.z(),true);event.damage(30);
        event.update(5,0,0,false);Properties p=new Properties();event.store(p);FieldEvent loaded=FieldEvent.restore(p);
        assertEquals(event.health(),loaded.health());assertEquals(event.time(),loaded.time());assertEquals(event.progress(),loaded.progress());
        loaded.update(9,0,0,false);assertTrue(loaded.claim());assertFalse(loaded.claim());
        p.setProperty("event.time","NaN");assertThrows(IllegalArgumentException.class,()->FieldEvent.restore(p));
        event.begin(5);assertFalse(event.visible());
    }
    @Test void formationsHaveDifferentPrioritiesAndNeverExceedTheEnemyRoster() {
        Set<List<Integer>> compositions=new HashSet<>();
        for(int wave:new int[]{1,2,3,4,6,7,8,9}) {
            List<Integer> roster=new ArrayList<>();for(int i=0;i<12;i++) { int v=WaveFormation.variant(wave,i);assertTrue(v>=0&&v<5);roster.add(v); }
            compositions.add(roster);
        }
        assertTrue(compositions.size()>=4);assertEquals(5,WaveFormation.variant(5,0));assertEquals(5,WaveFormation.variant(10,0));
    }
    @Test void objectivesAndBlessingsSaveWithTheWardenAndLegacyDefaultsRemainEmpty() {
        Progression p=new Progression();p.damagePlayer(30);p.rewardObjective();assertEquals(65,p.crystals());assertEquals(3,p.potions());
        assertEquals(85,p.health());assertTrue(p.expedition().choose(Expedition.Blessing.BLOODROOT));p.rewardEnemy(false);assertEquals(88,p.health());
        p.fieldEvent().begin(2);p.fieldEvent().update(.1f,p.fieldEvent().x(),p.fieldEvent().z(),true);
        Properties data=new Properties();p.store(data);Progression copy=Progression.restore(data);
        assertEquals(1,copy.expedition().rank(Expedition.Blessing.BLOODROOT));assertTrue(copy.fieldEvent().vulnerable());
        data.keySet().removeIf(k->k.toString().startsWith("expedition.") || k.toString().startsWith("blessing.") || k.toString().startsWith("event."));
        copy=Progression.restore(data);assertEquals(0,copy.expedition().embers());assertFalse(copy.fieldEvent().visible());
    }
}
