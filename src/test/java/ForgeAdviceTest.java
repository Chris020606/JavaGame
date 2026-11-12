import static org.junit.jupiter.api.Assertions.*;

import Entities.ArmorSlot;

import org.junit.jupiter.api.Test;

import java.util.Properties;

class ForgeAdviceTest {
    @Test
    void explainsClassLevelCurrencyAndOwnership() {
        Progression p = new Progression();
        assertTrue(
                ForgeAdvice.armor(p, HeroClass.MAGE, ArmorSlot.BOOTS, 1).contains("class mentor"));
        assertTrue(
                ForgeAdvice.armor(p, HeroClass.WARRIOR, ArmorSlot.BOOTS, 1)
                        .contains("Requires level"));
        p.addXp(300);
        assertTrue(
                ForgeAdvice.armor(p, HeroClass.WARRIOR, ArmorSlot.BOOTS, 1)
                        .contains("more crystals"));
        Properties properties = new Properties();
        p.store(properties);
        properties.setProperty("crystals", "1000");
        p = Progression.restore(properties);
        assertTrue(
                ForgeAdvice.armor(p, HeroClass.WARRIOR, ArmorSlot.BOOTS, 1).contains("+1 defense"));
        p.buyArmor(ArmorSlot.BOOTS, 1);
        assertTrue(ForgeAdvice.armor(p, HeroClass.WARRIOR, ArmorSlot.BOOTS, 1).startsWith("Owned"));
    }
}
