import static Config.Controls.Action.*;

import static com.raylib.Raylib.*;

import Config.Controls;

import Entities.CombatDiscipline;
import Entities.Player;

/** Outpost's ember vitals and six-slot action belt. Geometry is shared with input. */
public final class HudScreen {
    public record CombatState(
            float cooldown,
            float duration,
            float impact,
            float charge,
            int combo,
            float comboWindow,
            String mastery,
            boolean empowered,
            boolean intercept) {}

    private static final Controls.Action[] ACTIONS = {
        SLASH, HEAVY, PARRY, LUNGE, POTION, INVENTORY
    };

    private static final float[] COSTS = {
        CombatDiscipline.LIGHT,
        CombatDiscipline.HEAVY,
        CombatDiscipline.PARRY,
        CombatDiscipline.LUNGE
    };

    private float ghostHealth = -1;
    private float ghostDelay = 0;

    public void draw(Progression p, Player player, Controls controls, CombatState state) {
        HudLayout layout = new HudLayout(GetScreenWidth(), GetScreenHeight());
        int x = layout.x(), y = layout.y(), span = layout.span();
        boolean compact = GetScreenWidth() < 1000;
        float dt = Math.max(0, Math.min(.1f, GetFrameTime()));
        float currentHp = p.health();
        if (ghostHealth < 0 || currentHp >= ghostHealth) {
            ghostHealth = currentHp;
            ghostDelay = 0;
        } else {
            ghostDelay += dt;
            if (ghostDelay >= 0.28f) {
                ghostHealth =
                        Math.max(
                                currentHp,
                                ghostHealth - Math.max(30f, (ghostHealth - currentHp) * 4.5f) * dt);
            }
        }
        int frameWidth = compact ? 216 : 320, fx = 16, fy = 20;
        int panelH = compact ? 162 : 168;
        Menu.panel(fx, fy, frameWidth, panelH);
        DrawRectangle(fx + 1, fy + 10, 3, panelH - 20, Menu.GOLD);
        DrawLine(fx + 14, fy + 53, fx + frameWidth - 14, fy + 53, Fade(Menu.LINE, .5f));
        String crest =
                p.heroClass() == HeroClass.WARRIOR
                        ? "sword"
                        : p.heroClass() == HeroClass.ARCHER
                                ? (p.weaponTier() >= 2 ? "crossbow" : "bow")
                                : "staff";
        Menu.icon(crest, fx + 14, fy + 12, 18, Menu.GOLD);
        String identity = player.getName() + " / LV " + p.characterLevel();
        int identitySize = 14;
        while (identitySize > 9 && Menu.measure(identity, identitySize) > frameWidth - 56)
            identitySize--;
        Menu.text(identity, fx + 42, fy + 13, identitySize, Menu.TEXT);
        Menu.text(
                p.heroClass().label + " / " + p.difficulty().label + " / DEF " + p.defense(),
                fx + 14,
                fy + 38,
                compact ? 9 : 11,
                Menu.MUTED);
        Menu.resourceBar(
                fx + 14,
                fy + 58,
                frameWidth - 28,
                26,
                p.health(),
                ghostHealth,
                p.maxHealth(),
                p.health() < p.maxHealth() * .25f ? Menu.RED : Menu.color(49, 153, 94),
                "HEALTH");
        Menu.resourceBar(
                fx + 14,
                fy + 90,
                frameWidth - 28,
                22,
                player.discipline().stamina(),
                p.maxStamina(),
                p.heroClass() == HeroClass.MAGE
                        ? Menu.color(65, 135, 235)
                        : Menu.color(190, 147, 48),
                p.heroClass() == HeroClass.MAGE ? "MANA" : "STAMINA");
        int xp = p.xp(), xpNeeded = p.xpToNextLevel();
        Menu.resourceBar(
                fx + 14,
                fy + 118,
                frameWidth - 28,
                16,
                xp,
                xp,
                xpNeeded,
                Menu.color(135, 85, 215),
                "XP");
        Menu.text("[B] RECALL TO CITADEL", fx + 14, fy + 138, compact ? 9 : 11, Menu.TEAL);
        // Size the passive combat hint to its text, leaving more of the world unobscured.
        int hintWidth = Menu.measure(state.mastery(), 11) + 28;
        DrawRectangle(
                GetScreenWidth() / 2 - hintWidth / 2, y - 144, hintWidth, 24, Fade(Menu.INK, .88f));
        Menu.center(
                state.mastery(),
                GetScreenWidth() / 2,
                y - 139,
                11,
                state.empowered() ? Menu.GOLD : Menu.TEXT);
        Menu.frame(x - 8, y - 8, span + 16, 78, Menu.PANEL, Menu.LINE);
        Vector2 mouse = GetMousePosition();
        int hover = layout.hit(mouse.x(), mouse.y());
        String[] icons = {crest, "charge", "shield", "lunge", "potion", "bag"};
        Color classAccent =
                switch (p.heroClass()) {
                    case MAGE -> Menu.color(169, 153, 250);
                    case ARCHER -> Menu.color(133, 214, 169);
                    default -> Menu.GOLD;
                };
        String[] labels = {
            p.heroClass() == HeroClass.WARRIOR
                    ? "STRIKE"
                    : p.heroClass() == HeroClass.ARCHER ? "SHOOT" : "CAST",
            "CHARGE",
            "PARRY",
            "LUNGE",
            "HEAL",
            "PACK"
        };
        for (int i = 0; i < HudLayout.SLOTS; i++) {
            int sx = layout.slotX(i), size = layout.cell();
            boolean active =
                    i == 0 && player.isAttacking()
                            || i == 1 && state.charge() > 0
                            || i == 2 && player.discipline().parrying();
            boolean intercept = i == 2 && state.intercept();
            Menu.slot(sx, y, size, hover == i, active || intercept);
            Color accent =
                    active || intercept || hover == i
                            ? Menu.GOLD
                            : i == 4 ? Menu.RED : i == 5 ? Menu.MUTED : classAccent;
            boolean lowResource = i < COSTS.length && player.discipline().stamina() < COSTS[i];
            if (!active && (lowResource || i == 4 && p.potionHealing() == 0)) accent = Menu.MUTED;
            DrawRectangleGradientV(
                    sx + 2, y + 2, size - 4, size - 4, Fade(accent, .22f), Fade(Menu.INK, .64f));
            int iconSize = compact ? 25 : 30;
            Menu.icon(
                    icons[i],
                    sx + (size - iconSize) / 2,
                    y + size - iconSize - 5,
                    iconSize,
                    accent);
            String key = controls.label(ACTIONS[i]);
            int keySize = 12;
            while (keySize > 8 && Menu.measure(key, keySize) > size - 8) keySize--;
            DrawRectangle(sx + 2, y + 2, Menu.measure(key, keySize) + 6, keySize + 3, Menu.INK);
            Menu.text(key, sx + 5, y + 3, keySize, Menu.TEXT);
            if (i == 4)
                Menu.text(
                        Integer.toString(p.potions()),
                        sx + size - 15,
                        y + size - 18,
                        14,
                        Menu.TEXT);
            float ready =
                    (i == 0 || i == 3) && state.cooldown() > 0
                            ? 1 - Math.min(1, state.cooldown() / Math.max(.001f, state.duration()))
                            : 1;
            DrawRectangle(sx + 2, y + size - 3, (int) ((size - 4) * ready), 2, accent);
            Menu.center(
                    intercept ? "PARRY NOW" : labels[i],
                    sx + size / 2,
                    y + size + 2,
                    intercept ? 10 : 12,
                    intercept ? Menu.GOLD : Menu.TEXT);
        }
        if (state.charge() > 0) {
            float charge = state.charge();
            boolean inCritWindow = charge >= 0.75f && charge <= 1.05f;
            int barY = y - 36;
            int barH = 18;
            Menu.frame(
                    x - 2,
                    barY - 2,
                    span + 4,
                    barH + 4,
                    Menu.INK,
                    inCritWindow ? Menu.GOLD : Menu.LINE);
            int filled = (int) (span * Math.min(1.0f, charge / 1.4f));
            if (filled > 0) {
                DrawRectangleGradientH(
                        x,
                        barY,
                        filled,
                        barH,
                        Menu.color(180, 130, 30),
                        inCritWindow ? Menu.GOLD : Menu.color(220, 175, 45));
                DrawLine(x, barY, x + filled, barY, Fade(Menu.TEXT, .4f));
            }
            // Critical release window illuminated brackets
            int winStart = x + (int) (span * .75f / 1.4f);
            int winWidth = (int) (span * .30f / 1.4f);
            DrawRectangle(
                    winStart, barY, winWidth, barH, Fade(Menu.GOLD, inCritWindow ? .35f : .15f));
            DrawRectangleLines(
                    winStart, barY, winWidth, barH, Fade(Menu.GOLD, inCritWindow ? .95f : .55f));

            String spellName =
                    p.heroClass() == HeroClass.WARRIOR
                            ? "HEAVY CLEAVE"
                            : p.heroClass() == HeroClass.ARCHER
                                    ? "POWER SHOT"
                                    : p.weaponTier() >= 3 ? "DAWNWEAVER MEND" : "ARCANE SURGE";
            DrawRectangle(
                    x + 3,
                    barY + 1,
                    Menu.measure(spellName, 11) + 10,
                    barH - 4,
                    Fade(Menu.INK, .90f));
            Menu.text(spellName, x + 8, barY + 2, 11, Menu.TEXT);
            String cue =
                    inCritWindow
                            ? "Release now"
                            : charge < .75f ? "Hold for gold" : "Release to fire";
            Menu.text(cue, x + span - Menu.measure(cue, 13) - 8, barY + 2, 13, Menu.TEXT);
        } else {
            String timing =
                    player.discipline().riposteReady()
                            ? "Riposte / " + controls.label(SLASH)
                            : state.combo() > 0
                                            && state.comboWindow() > 0
                                            && state.cooldown() <= .22f
                                    ? "CHAIN NOW (COMBO " + state.combo() + " / 3)"
                                    : player.isAttacking() ? "ATTACK / RECOVERING" : null;
            if (timing != null) {
                Menu.center(
                        timing,
                        GetScreenWidth() / 2,
                        y - 25,
                        11,
                        player.discipline().riposteReady() ? Menu.GOLD : Menu.TEXT);
            }
        }
        if (hover >= 0) {
            int tx = Math.max(8, Math.min(GetScreenWidth() - 338, GetScreenWidth() / 2 - 165)),
                    ty = y - 174;
            Menu.panel(tx, ty, 330, 48);
            Menu.icon(icons[hover], tx + 12, ty + 12, 22, Menu.GOLD);
            String help =
                    switch (hover) {
                        case 0 ->
                                switch (p.heroClass()) {
                                    case ARCHER -> "Loose an arrow";
                                    case MAGE -> "Cast a bolt";
                                    default -> "Swing your weapon";
                                };
                        case 1 ->
                                p.heroClass() == HeroClass.MAGE && p.weaponTier() >= 3
                                        ? "Charged Dawnweaver shots mend allies"
                                        : "Hold to charge, release in gold";
                        case 2 -> "Parry a blow or intercept a shot";
                        case 3 -> "Close the gap with a lunge";
                        case 4 ->
                                p.potions() == 0
                                        ? "Restock with Torvald in Market Square"
                                        : p.potionHealing() == 0
                                                ? "Health is full"
                                                : "Restore "
                                                        + Math.round(p.potionHealing())
                                                        + " health";
                        default -> "Inspect and change equipment";
                    };
            Menu.text(
                    labels[hover] + " / " + controls.label(ACTIONS[hover]),
                    tx + 44,
                    ty + 7,
                    13,
                    Menu.GOLD);
            if (hover < COSTS.length)
                help +=
                        " / "
                                + (int) COSTS[hover]
                                + " "
                                + p.resourceLabel().toLowerCase(java.util.Locale.ROOT);
            Menu.text(help, tx + 44, ty + 27, 11, Menu.TEXT);
        }
    }
}
