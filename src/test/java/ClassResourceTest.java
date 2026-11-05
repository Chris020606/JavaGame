import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ClassResourceTest {
    @Test void mageSprintingPreservesManaWhileWarriorSpendsStamina() {
        CoopWorld world=CoopWorldTest.started();
        for(int i=0;i<30;i++) {
            world.input(0,new CoopInput(i+2,1,0,90,CoopInput.SPRINT));
            world.input(1,new CoopInput(i+2,1,0,90,CoopInput.SPRINT));world.tick(1f/60);
        }
        var heroes=world.snapshot().heroes();
        assertEquals(heroes.get(1).maxStamina(),heroes.get(1).stamina(),.01f);
        assertTrue(heroes.get(0).stamina()<heroes.get(0).maxStamina());
    }
    @Test void mageTrainingAndResourceUseManaNames() {
        Progression mage=new Progression(HeroClass.MAGE);
        assertEquals("MANA",mage.resourceLabel());
        assertTrue(mage.upgradeEffect(Progression.Upgrade.ENDURANCE).contains("mana"));
        assertTrue(mage.upgradeEffect(Progression.Upgrade.RECOVERY).contains("mana"));
        assertEquals("STAMINA",new Progression(HeroClass.ARCHER).resourceLabel());
        Progression archer=new Progression(HeroClass.ARCHER);
        assertNotEquals(archer.weaponName(2),archer.weaponName(3));
    }
}
