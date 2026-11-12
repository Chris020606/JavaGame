import static com.raylib.Raylib.*;

/** Shared layout coordinates keep mouse targets aligned with the drawn rows. */
public final class WorkshopScreen {
    public static final int WIDTH = 940,
            HEIGHT = 600,
            CONTENT = 244,
            ROW_TOP = 140,
            ROW_HEIGHT = 52;
    private int tab;
    private int forgeTier = 1;
    private HeroClass activeMentor = null;
    private String purchaseMessage = "";
    private double purchaseMessageUntil;
    private String hoverAdvice = "";
    private static final String[] TABS = {"EQUIPMENT FORGE", "BASE & SUPPLIES"};

    public void selectTab(int index) {
        if (index == 2) tab = 1;
        else if (index == 1) tab = 0;
        else tab = Math.max(0, Math.min(TABS.length - 1, index));
    }

    public void setMentor(HeroClass mentor) {
        this.activeMentor = mentor;
    }

    public HeroClass mentor() {
        return activeMentor;
    }

    public boolean offersSupplies() {
        return activeMentor == null;
    }

    public boolean draw(Progression p, String closeKey, GameAudio audio) {
        // Shop ownership controls stock. A shared view must never grant another vendor's goods.
        tab = offersSupplies() ? 1 : 0;
        hoverAdvice = "";
        int x = (GetScreenWidth() - WIDTH) / 2, y = (GetScreenHeight() - HEIGHT) / 2;
        Menu.panel(x, y, WIDTH, HEIGHT);
        Menu.frame(x + 8, y + 8, 212, HEIGHT - 16, Menu.color(21, 29, 44), Menu.LINE);
        Menu.text(
                activeMentor != null
                        ? activeMentor.label.toUpperCase() + " FORGE"
                        : "TORVALD'S CARAVAN",
                x + 28,
                y + 28,
                activeMentor == null ? 18 : 22,
                Menu.TEXT);
        Menu.text("REST. REFINE. RETURN.", x + 28, y + 72, 12, Menu.MUTED);
        Menu.frame(x + 18, y + 118, 192, 70, Menu.color(31, 42, 60), Menu.LINE);
        Menu.icon(offersSupplies() ? "bag" : "sword", x + 30, y + 132, 20, Menu.GOLD);
        Menu.text(
                offersSupplies() ? "MERCHANT STOCK" : "CLASS EQUIPMENT",
                x + 58,
                y + 136,
                12,
                Menu.GOLD);
        Menu.wrapped(
                offersSupplies()
                        ? "Tonics, field repairs and caravan upgrades."
                        : "Weapons and armor for your class. Visit Torvald in Market Square for"
                                + " supplies.",
                x + 28,
                y + 206,
                178,
                13,
                Menu.MUTED);
        Menu.text("YOUR WARDEN", x + 28, y + 330, 13, Menu.GOLD);
        Menu.text("LEVEL " + p.characterLevel(), x + 130, y + 330, 13, Menu.TEAL);
        String[] stats = {
            p.maxHealth() + "  HEALTH",
            p.damage() + "  DAMAGE",
            p.maxStamina() + "  " + p.resourceLabel(),
            p.defense() + "  DEFENSE",
            "+" + Math.round(p.healRate()) + "  HEARTH/s"
        };
        for (int i = 0; i < stats.length; i++)
            Menu.text(stats[i], x + 28, y + 360 + i * 28, 15, i == 2 ? Menu.TEAL : Menu.TEXT);
        Menu.text(TABS[tab], x + CONTENT, y + 28, 28, Menu.TEXT);
        Menu.text(p.crystals() + " CRYSTALS", x + 530, y + 32, 18, Menu.GOLD);
        Menu.text("RANK: LV " + p.characterLevel(), x + 750, y + 32, 18, Menu.TEAL);
        Menu.text(
                tab == 0
                        ? "Forge weapons and armor with crystals. Requires Warden combat levels."
                        : "Fortify the Primordial Beacon and restock supplies with crystals.",
                x + CONTENT,
                y + 76,
                15,
                Menu.MUTED);
        DrawLine(x + CONTENT, y + 112, x + 916, y + 112, Menu.LINE);
        if (tab == 0) drawForge(p, audio, x, y);
        else drawSupplies(p, audio, x, y);
        DrawLine(x + CONTENT, y + 531, x + 916, y + 531, Menu.LINE);
        Menu.text(
                !hoverAdvice.isEmpty()
                        ? hoverAdvice
                        : GetTime() < purchaseMessageUntil
                                ? purchaseMessage
                                : "TIME PAUSED / Purchases are saved with your campaign.",
                x + CONTENT,
                y + 551,
                14,
                Menu.MUTED);
        return Menu.button("RETURN [" + closeKey + "]", x + 24, y + 536, 184, true);
    }

    private void drawForge(Progression p, GameAudio audio, int x, int y) {
        String[] qualities = {"IRON", "RUNIC", "DAWNSTEEL"};
        for (int tier = 1; tier <= 3; tier++) {
            int tx = x + CONTENT + (tier - 1) * 226;
            if (Menu.button(
                    qualities[tier - 1] + " / LV " + Progression.requiredLevel(tier),
                    tx,
                    y + 126,
                    214,
                    true)) forgeTier = tier;
            if (forgeTier == tier) DrawRectangle(tx + 3, y + 169, 208, 3, Menu.GOLD);
        }
        int tier = forgeTier, left = x + CONTENT, top = y + 188;
        boolean matching = activeMentor == p.heroClass();
        Color accent = tier == 3 ? Menu.GOLD : tier == 2 ? Menu.TEAL : Menu.TEXT;
        Menu.frame(left, top, 214, 326, Menu.color(27, 39, 55), Menu.LINE);
        Menu.text("CLASS WEAPON", left + 16, top + 14, 12, Menu.MUTED);
        Menu.slot(left + 64, top + 48, 86, false, p.ownsWeapon(tier));
        String weaponIcon =
                p.heroClass() == HeroClass.WARRIOR
                        ? "sword"
                        : p.heroClass() == HeroClass.MAGE
                                ? "staff"
                                : tier >= 2 ? "crossbow" : "bow";
        Menu.icon(weaponIcon, left + 84, top + 68, 44, accent);
        Menu.wrapped(p.weaponName(tier), left + 16, top + 151, 182, 19, accent);
        int delta = p.weaponDamage(tier) - p.damage();
        Menu.text("+" + p.weaponBonus(tier) + " weapon power", left + 16, top + 211, 13, Menu.TEXT);
        Menu.text(
                String.format("%+d vs equipped", delta),
                left + 16,
                top + 236,
                12,
                delta >= 0 ? Menu.TEAL : Menu.RED);
        String weaponLabel =
                p.ownsWeapon(tier)
                        ? "OWNED"
                        : p.characterLevel() < Progression.requiredLevel(tier)
                                ? "REQUIRES LV " + Progression.requiredLevel(tier)
                                : "FORGE / " + p.weaponPrice(tier) + " CR";
        if (Menu.button(weaponLabel, left + 12, top + 268, 190, matching && p.canBuyWeapon(tier))
                && p.buyWeapon(tier)) {
            audio.play("upgrade");
            purchaseMessage = "FORGED " + p.weaponName(tier) + " / Open inventory to equip it.";
            purchaseMessageUntil = GetTime() + 4;
        }
        for (Entities.ArmorSlot slot : Entities.ArmorSlot.values()) {
            int ax = left + 226 + (slot.ordinal() % 2) * 226;
            int ay = top + (slot.ordinal() / 2) * 168;
            boolean owned = p.ownsArmor(slot, tier),
                    enabled = matching && p.canBuyArmor(slot, tier);
            Menu.frame(ax, ay, 214, 158, Menu.color(25, 36, 50), owned ? Menu.TEAL : Menu.LINE);
            Menu.slot(ax + 12, ay + 12, 48, false, owned);
            Menu.icon(slot.icon(), ax + 23, ay + 23, 26, accent);
            Menu.text(slot.label.toUpperCase(), ax + 72, ay + 15, 14, Menu.TEXT);
            Menu.text("+" + p.armorBonus(slot, tier) + " DEFENSE", ax + 72, ay + 39, 11, accent);
            int gain = p.armorBonus(slot, tier) - p.armorBonus(slot, p.armorTier(slot));
            Menu.text(
                    String.format("%+d vs your equipped piece", gain),
                    ax + 14,
                    ay + 76,
                    12,
                    gain >= 0 ? Menu.TEAL : Menu.RED);
            String label =
                    owned
                            ? "OWNED / IN INVENTORY"
                            : p.characterLevel() < Progression.requiredLevel(tier)
                                    ? "REQUIRES LV " + Progression.requiredLevel(tier)
                                    : "FORGE / " + p.armorPrice(slot, tier) + " CR";
            if (Menu.button(label, ax + 12, ay + 104, 190, enabled) && p.buyArmor(slot, tier)) {
                audio.play("upgrade");
                purchaseMessage =
                        "FORGED " + p.armorName(slot, tier) + " / Open inventory to equip it.";
                purchaseMessageUntil = GetTime() + 4;
            }
            Vector2 pointer = GetMousePosition();
            if (pointer.x() >= ax
                    && pointer.x() < ax + 214
                    && pointer.y() >= ay
                    && pointer.y() < ay + 158)
                hoverAdvice = ForgeAdvice.armor(p, activeMentor, slot, tier);
        }
    }

    private void drawSupplies(Progression p, GameAudio audio, int x, int y) {
        // Left Column: Potions & Emergency Repairs
        int col1 = x + CONTENT + 12;
        Menu.frame(col1, y + 130, 310, 380, Menu.color(24, 32, 48), Menu.LINE);
        Menu.icon("potion", col1 + 16, y + 144, 28, Menu.RED);
        Menu.text("HEALING POTIONS", col1 + 54, y + 148, 18, Menu.TEXT);
        Menu.text("Restore up to 60 HP. Carry up to 5.", col1 + 16, y + 184, 13, Menu.MUTED);
        for (int i = 0; i < 5; i++) {
            Menu.slot(col1 + 16 + i * 56, y + 210, 48, false, i < p.potions());
            Menu.icon(
                    "potion",
                    col1 + 30 + i * 56,
                    y + 226,
                    18,
                    i < p.potions() ? Menu.RED : Menu.LINE);
        }
        if (Menu.button(
                        "RESTOCK / 30 CRYSTALS",
                        col1 + 16,
                        y + 276,
                        278,
                        p.crystals() >= 30 && p.potions() < 5)
                && p.buyPotion()) audio.play("upgrade");

        DrawLine(col1 + 16, y + 338, col1 + 294, y + 338, Menu.LINE);
        Menu.icon("crown", col1 + 16, y + 352, 24, Menu.GOLD);
        Menu.text("FIELD REPAIR", col1 + 48, y + 354, 16, Menu.TEXT);
        Menu.text(
                "Integrity: " + Math.round(p.baseHealth()) + " / " + p.maxBaseHealth(),
                col1 + 16,
                y + 386,
                13,
                Menu.MUTED);
        if (Menu.button(
                        "REPAIR +100 / 30 CRYSTALS",
                        col1 + 16,
                        y + 420,
                        278,
                        p.crystals() >= 30 && p.baseHealth() < p.maxBaseHealth())
                && p.repairBase()) audio.play("upgrade");

        // Right Column: Caravan Merchant Provisions (No manual upgrading)
        int col2 = x + CONTENT + 340;
        Menu.frame(col2, y + 130, 334, 380, Menu.color(24, 32, 48), Menu.LINE);
        Menu.icon("bag", col2 + 16, y + 144, 24, Menu.GOLD);
        Menu.text("CARAVAN PROVISIONS", col2 + 48, y + 148, 17, Menu.GOLD);
        Menu.text("Supplies imported by Torvald's caravan.", col2 + 16, y + 176, 12, Menu.MUTED);

        String[] names = {"HEARTWOOD CHARM", "TRAVELER'S FLASK", "RESTORATION INCENSE"};
        String[] effects = {
            "Permanent +5 maximum health",
            "Permanent +5 stamina or mana",
            "Permanent +1 resource recovery / sec"
        };
        for (int i = 0; i < 3; i++) {
            int top = y + 200 + i * 100;
            Menu.frame(col2 + 12, top, 310, 94, Menu.color(18, 26, 40), Menu.LINE);
            Menu.text(
                    names[i] + " / " + p.caravanRank(i) + " OF 5",
                    col2 + 24,
                    top + 8,
                    12,
                    Menu.GOLD);
            Menu.text(effects[i], col2 + 24, top + 29, 11, Menu.MUTED);
            if (Menu.button(
                            p.caravanRank(i) == 5
                                    ? "MASTERED"
                                    : "BUY / " + p.caravanPrice(i) + " CRYSTALS",
                            col2 + 24,
                            top + 47,
                            282,
                            p.canBuyCaravan(i))
                    && p.buyCaravan(i)) audio.play("upgrade");
        }
    }
}
