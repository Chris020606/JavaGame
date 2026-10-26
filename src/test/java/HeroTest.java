import Entities.Enemy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HeroTest {
    @TempDir Path directory;
    @Test void potionsHaveCapacityCostAndCannotBeWastedOrRevive() {
        Progression p=new Progression(HeroClass.MAGE);
        assertEquals(80,p.health());assertFalse(p.usePotion());assertEquals(2,p.potions());
        p.damagePlayer(70);assertTrue(p.usePotion());assertEquals(70,p.health());
        assertFalse(p.buyPotion());
        for(int i=0;i<10;i++)p.rewardKill();
        for(int i=0;i<4;i++)assertTrue(p.buyPotion());
        assertEquals(5,p.potions());assertEquals(30,p.crystals());assertFalse(p.buyPotion());assertEquals(30,p.crystals());
        p.damagePlayer(1000);assertFalse(p.usePotion());assertFalse(p.buyPotion());
    }
    @Test void classAndInventorySurviveSaveLoadAndOldSavesDefaultToWarrior() throws Exception {
        for(HeroClass hero:HeroClass.values()) {
            Progression p=new Progression(hero);p.damagePlayer(30);p.usePotion();
            Path file=directory.resolve(hero.name());
            SaveGame.write(file,new SaveGame.Snapshot("Test",0,4,0,p,new WaveDirector(new Random()).snapshot(),List.of()));
            Progression loaded=SaveGame.read(file).progression();
            assertEquals(hero,loaded.heroClass());assertEquals(1,loaded.potions());assertEquals(hero.health,loaded.health());
            Properties props=AppData.read(file);props.remove("heroClass");props.remove("potions");props.setProperty("health","75");AppData.write(file,props);
            assertEquals(HeroClass.WARRIOR,SaveGame.read(file).progression().heroClass());assertEquals(2,SaveGame.read(file).progression().potions());
            props.setProperty("potions","6");AppData.write(file,props);assertThrows(java.io.IOException.class,()->SaveGame.read(file));
        }
    }
    @Test void rangedShotsSweepTheirPathAndDoNotRepeatHitsOrExceedRange() {
        HeroShot arrow=new HeroShot(0,0,0,HeroClass.ARCHER.attack(1,false),25,false);
        Enemy front=new Enemy(0,4,1),behind=new Enemy(0,-2,1),side=new Enemy(2,4,1);
        arrow.update(.3f);assertTrue(arrow.hit(front));assertFalse(arrow.hit(front));assertFalse(arrow.hit(behind));assertFalse(arrow.hit(side));
        HeroShot bolt=new HeroShot(0,0,0,HeroClass.MAGE.attack(1,true),25,true);
        bolt.update(10);assertEquals(12,bolt.z,.001);assertTrue(bolt.hit(new Enemy(0,5,1)));assertTrue(bolt.hit(new Enemy(0,10,1)));assertFalse(bolt.hit(new Enemy(0,14,1)));
        assertTrue(HeroClass.ARCHER.speed>HeroClass.WARRIOR.speed);
    }
}
