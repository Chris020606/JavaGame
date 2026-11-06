import Entities.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RoadmapSystemsTest {
    @TempDir Path temp;
    @Test void powerArrowsPierceThreeTargetsButMagicKeepsPiercing() {
        HeroShot arrow=new HeroShot(0,0,0,HeroClass.ARCHER.attack(1,true),25,false);arrow.heavy=true;arrow.update(.1f);
        assertTrue(arrow.hit(new Enemy(0,2,9)));assertTrue(arrow.continues());assertTrue(arrow.hit(new Enemy(0,2.5f,9)));assertTrue(arrow.continues());assertTrue(arrow.hit(new Enemy(0,3,9)));assertFalse(arrow.continues());assertFalse(arrow.hit(new Enemy(0,3,9)));
        HeroShot magic=new HeroShot(0,0,0,HeroClass.MAGE.attack(1,false),25,true);magic.update(.1f);
        for(int i=0;i<5;i++)assertTrue(magic.hit(new Enemy(0,1.8f,9)));assertTrue(magic.continues());
    }
    @Test void waveSuppliesRespectPotionCapacity() {
        Progression p=new Progression();for(int i=0;i<20;i++)p.waveSupplies();assertEquals(5,p.potions());
    }
    @Test void lootRollsPersistAndWeakDuplicatesNeverReplaceBest() {
        WeaponLoot loot=new WeaponLoot();Random rng=new Random(83);int score=0;Set<String> traits=new HashSet<>();
        for(int i=0;i<100;i++) { var drop=loot.roll(2,rng);traits.add(drop.trait().name());var best=loot.get(2);int now=best.damage()+best.recovery();assertTrue(now>=score);score=now; }
        assertEquals(3,traits.size());Properties p=new Properties();loot.store(p);assertEquals(loot.get(2),WeaponLoot.restore(p).get(2));
        p.setProperty("loot.2.power","9");assertThrows(IllegalArgumentException.class,()->WeaponLoot.restore(p));
    }
    @Test void championRosterMovesAndSaveRoundTripAreSupported() throws Exception {
        assertEquals(6,WaveFormation.variant(4,Campaign.enemies(4)-1));assertEquals(6,WaveFormation.variant(9,Campaign.enemies(9)-1));
        Enemy champion=new Enemy(0,12,9,6,.5f);champion.beginAttack(0,4,true);assertEquals(BossMove.SIGILS,champion.bossMove);champion.beginAttack(0,4,true);assertEquals(BossMove.SLAM,champion.bossMove);
        Progression p=new Progression(HeroClass.MAGE);p.unlockChapter(3);p.rewardEnemy(true);p.equipWeapon(1);
        Path save=temp.resolve("save.properties");SaveGame.write(save,new SaveGame.Snapshot("Test",0,4,0,p,new WaveDirector.State(9,0,1,8,0,21,0),List.of(champion)));
        var restored=SaveGame.read(save);assertEquals(6,restored.enemies().get(0).variant);assertEquals(3,restored.progression().highestChapter());assertEquals(p.loot().get(1),restored.progression().loot().get(1));assertEquals(p.damage(),restored.progression().damage());
    }
}
