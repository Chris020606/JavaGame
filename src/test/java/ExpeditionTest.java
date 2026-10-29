import Entities.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpeditionTest {
    @TempDir Path directory;
    private Progression funded() { Progression p=new Progression(HeroClass.WARRIOR,Difficulty.EXTREME);for(int i=0;i<15000;i++)p.rewardKill();return p; }
    @Test void difficultiesScaleThreatsButNeverMultiplyBosses() {
        Enemy easy=new Enemy(0,0,6,2,.5f,Difficulty.EASY),extreme=new Enemy(0,0,6,2,.5f,Difficulty.EXTREME);
        assertTrue(extreme.maxHealth>easy.maxHealth);assertTrue(extreme.speed>easy.speed);
        assertTrue(extreme.meleeDamage(6)>easy.meleeDamage(6));
        easy.beginAttack(0,1,true);extreme.beginAttack(0,1,true);assertTrue(extreme.cooldown<easy.cooldown);
        for(Difficulty d:Difficulty.values()) {
            WaveDirector waves=new WaveDirector(new Random(4));waves.difficulty(d);waves.update(6,0);
            assertEquals(d.count(Campaign.enemies(1),false),waves.total());
            waves.restore(new WaveDirector.State(4,0,0,0,0,0,0));waves.update(1,0);
            assertEquals(1,waves.total());assertEquals(5,waves.update(10,0).variant());
            assertEquals(36,d.count(1000,false));
        }
    }
    @Test void rangedEnemiesRetreatAndRepositionWhileScoutsFlank() {
        Enemy archer=new Enemy(0,0,6,3,.8f);archer.cooldown=0;
        archer.navigate(0,2,0,0,.1f);assertTrue(archer.z<0);assertEquals("RETREAT",archer.action);
        archer.x=archer.z=0;archer.cooldown=1;archer.navigate(0,6,0,0,.1f);
        assertEquals("STRAFE",archer.action);assertTrue(archer.x>0);
        Enemy scout=new Enemy(0,0,6,1,.8f);scout.navigate(0,8,0,0,.1f);
        assertEquals("FLANK",scout.action);assertTrue(scout.x>0);assertTrue(scout.z>0);
        scout.x=scout.z=22;for(int i=0;i<100;i++)scout.navigate(40,40,1,1,.1f);
        assertTrue(scout.x<=22 && scout.z<=22);
    }
    @Test void targetCommitmentAndHysteresisAvoidFrameByFrameFlipping() {
        Enemy raider=new Enemy(0,0,1);assertTrue(raider.chooseHero(0,7,.01f));
        assertTrue(raider.chooseHero(0,20,.1f));assertTrue(raider.chooseHero(0,9,1));
        assertFalse(raider.chooseHero(0,11,1));assertFalse(raider.chooseHero(0,9,1));
    }
    @Test void squadsLimitAssaultsAvoidBlockedShotsAndSupportWoundedAllies() {
        Enemy attacker=new Enemy(0,0,6,0,.5f,Difficulty.EASY),busy=new Enemy(0,2,6);
        busy.beginAttack(0,5,true);assertFalse(SquadTactics.canJoinAssault(attacker,List.of(attacker,busy)));
        busy.targetsPlayer=false;assertTrue(SquadTactics.canJoinAssault(attacker,List.of(attacker,busy)));
        Enemy shooter=new Enemy(0,0,6,3,.5f);assertFalse(SquadTactics.clearShot(shooter,0,8,List.of(shooter,busy)));
        busy.x=2;assertTrue(SquadTactics.clearShot(shooter,0,8,List.of(shooter,busy)));
        Enemy healer=new Enemy(0,0,6,4,.5f),wounded=new Enemy(0,3,6);wounded.health*=.3f;
        assertSame(wounded,SquadTactics.woundedAlly(healer,List.of(healer,busy,wounded)));
        healer.healingTarget=wounded;healer.beginAttack(0,3,false);healer.hit(1,0,0,0);
        assertNull(healer.healingTarget);assertFalse(healer.impactPending);
    }
    @Test void independentTrainingAndEquipmentHaveTransactionalCapsAndBonuses() {
        Progression p=funded();int damage=p.damage(),hp=p.maxHealth();float speed=p.movementSpeed();
        assertTrue(p.purchase(Progression.Upgrade.VITALITY));assertEquals(hp+20,p.maxHealth());assertEquals(damage,p.damage());
        assertTrue(p.purchase(Progression.Upgrade.POWER));assertEquals(damage+5,p.damage());
        assertTrue(p.purchase(Progression.Upgrade.ENDURANCE));assertEquals(115,p.maxStamina());
        assertTrue(p.purchase(Progression.Upgrade.RECOVERY));assertEquals(25,p.staminaRecovery());
        assertTrue(p.purchase(Progression.Upgrade.AGILITY));assertTrue(p.movementSpeed()>speed);
        assertFalse(p.equipArmor(ArmorSlot.CHEST,3));assertTrue(p.buyArmor(3));
        for(ArmorSlot slot:ArmorSlot.values()) assertTrue(p.equipArmor(slot,3));assertEquals(21,p.defense());
        assertTrue(p.equipArmor(ArmorSlot.CHEST,0));assertEquals(12,p.defense());
        assertTrue(p.buyWeapon(3));assertTrue(p.equipWeapon(2));assertEquals(damage+21,p.damage());
        for(Progression.Upgrade upgrade:Progression.TRAINING) {
            while(p.level(upgrade)<p.maxLevel(upgrade)) assertTrue(p.purchase(upgrade));
            int crystals=p.crystals();assertFalse(p.purchase(upgrade));assertEquals(crystals,p.crystals());
        }
        int crystals=p.crystals();assertFalse(p.buyArmor(3));assertFalse(p.buyWeapon(2));assertEquals(crystals,p.crystals());
        CombatDiscipline stamina=new CombatDiscipline();stamina.configure(p.maxStamina(),p.staminaRecovery());
        assertEquals(235,stamina.maximum());assertEquals(235,stamina.stamina());stamina.stamina(100);stamina.tick(1,false);assertEquals(149,stamina.stamina());
    }
    @Test void newRunStateRoundTripsAndLegacyPropertiesRemainCompatible() throws Exception {
        Progression p=funded();p.purchase(Progression.Upgrade.ENDURANCE);p.purchase(Progression.Upgrade.POWER);p.buyArmor(2);p.equipArmor(ArmorSlot.CHEST,2);p.buyWeapon(3);p.equipWeapon(3);
        Path file=directory.resolve("save.properties");Enemy enemy=new Enemy(2,4,6,2,.5f,Difficulty.EXTREME);
        SaveGame.write(file,new SaveGame.Snapshot("Warden",0,8,0,p,new WaveDirector.State(6,0,1,8,0,21,0),List.of(enemy),Arena.HIGHLANDS,112));
        var saved=SaveGame.read(file);assertEquals(112,saved.stamina());assertEquals(Difficulty.EXTREME,saved.progression().difficulty());
        assertEquals(2,saved.progression().armorTier(ArmorSlot.CHEST));assertEquals(3,saved.progression().weaponTier());
        assertEquals(enemy.maxHealth,saved.enemies().get(0).maxHealth);assertTrue(saved.enemies().get(0).undead);
        Properties old=new Properties();new Progression().store(old);
        for(String key:List.of("difficulty","vitality","power","endurance","recovery","agility","armorUnlocked"))old.remove(key);
        for(ArmorSlot slot:ArmorSlot.values())old.remove("equipped."+slot);
        var legacy=Progression.restore(old);assertEquals(Difficulty.NORMAL,legacy.difficulty());assertEquals(100,legacy.maxStamina());assertEquals(0,legacy.defense());
        Settings settings=new Settings();settings.difficulty=Difficulty.HARD;Path config=directory.resolve("settings.properties");settings.save(config);
        Settings restored=new Settings();restored.load(config);assertEquals(Difficulty.HARD,restored.difficulty);
    }
    @Test void frozenPassUsesItsOpenApproachRoutesAndChaptersHaveDistinctLooks() {
        assertEquals(4,Arrays.stream(ChapterLook.values()).map(l->l.label).distinct().count());
        WaveDirector waves=new WaveDirector(new Random(7));waves.restore(new WaveDirector.State(5,0,0,0,0,0,0));waves.update(1,0);
        while(waves.remaining()>0) { var spawn=waves.update(10,1);assertNotNull(spawn);assertTrue(Math.abs(spawn.x())<6);assertTrue(Math.abs(spawn.z())>18); }
        assertFalse(new Enemy(0,0,1).undead);assertTrue(new Enemy(0,0,6).undead);
    }
}
