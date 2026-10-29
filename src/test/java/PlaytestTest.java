import Entities.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaytestTest {
    @Test void sandboxGrantsEveryItemWithoutInventingCampaignProgress() {
        for(HeroClass hero:HeroClass.values())for(boolean maxed:new boolean[]{false,true}) {
            Progression p=Progression.playtest(hero,Difficulty.HARD,maxed);
            assertEquals(100000,p.crystals());assertEquals(0,p.kills());assertEquals(5,p.potions());
            assertEquals(Difficulty.HARD,p.difficulty());assertEquals(hero,p.heroClass());
            assertEquals(3,p.weaponUnlocked());assertEquals(3,p.armorUnlocked());
            for(int tier=0;tier<=3;tier++) {
                assertTrue(p.equipWeapon(tier));
                for(ArmorSlot slot:ArmorSlot.values())assertTrue(p.equipArmor(slot,tier));
            }
            assertEquals(maxed?235:100,p.maxStamina());assertEquals(hero.health+(maxed?180:0),p.maxHealth());
            assertEquals(p.maxHealth(),p.health());assertEquals(p.maxBaseHealth(),p.baseHealth());
            for(Progression.Upgrade upgrade:Progression.TRAINING)assertEquals(maxed?10:1,p.level(upgrade));
            java.util.Properties properties=new java.util.Properties();p.store(properties);Progression restored=Progression.restore(properties);
            assertEquals(p.defense(),restored.defense());assertEquals(p.maxStamina(),restored.maxStamina());assertEquals(100000,restored.crystals());
        }
    }
}
