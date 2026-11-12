import static Config.Controls.Action.*;

import static com.raylib.Raylib.*;

import Config.Controls;

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
            boolean empowered) {}

    private static final Controls.Action[] ACTIONS = {
        SLASH, HEAVY, PARRY, LUNGE, POTION, INVENTORY
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
        String weapon =
                p.heroClass() == HeroClass.WARRIOR
                        ? "sword"
                        : p.heroClass() == HeroClass.ARCHER
                                ? (p.weaponTier() >= 2 ? "crossbow" : "bow")
                                : "staff";
        String[] icons = {weapon, "charge", "shield", "lunge", "potion", "bag"};
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
            Menu.slot(sx, y, size, hover == i, active);
            if ((i == 0 || i == 3) && state.cooldown() > 0)
                DrawRectangle(
                        sx + 3,
                        y + 3,
                        size - 6,
                        (int) ((size - 6) * Math.min(1, state.cooldown() / state.duration())),
                        Fade(Menu.INK, .55f));
            Menu.text(controls.label(ACTIONS[i]), sx + 5, y + 4, 9, Menu.MUTED);
            Menu.icon(
                    icons[i],
                    sx + size / 2 - 12,
                    y + 18,
                    24,
                    i == 4 ? Menu.RED : active ? Menu.GOLD : Menu.TEXT);
            if (i == 4) Menu.text("" + p.potions(), sx + size - 15, y + size - 18, 13, Menu.GOLD);
            Menu.center(labels[i], sx + size / 2, y + size + 3, 9, Menu.MUTED);
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
            Menu.text(spellName, x + 8, barY + 2, 11, inCritWindow ? Menu.GOLD : Menu.TEXT);
            Menu.text(
                    String.format("%.2fs / 1.40s", charge), x + span - 84, barY + 2, 11, Menu.GOLD);
            if (inCritWindow) {
                Menu.center(
                        "CRITICAL RELEASE WINDOW!", GetScreenWidth() / 2, barY - 16, 11, Menu.GOLD);
            } else {
                Menu.center(
                        "HOLD TO CHARGE / RELEASE IN GOLD",
                        GetScreenWidth() / 2,
                        barY - 16,
                        10,
                        Menu.MUTED);
            }
        } else {
            String timing =
                    player.discipline().riposteReady()
                            ? "RIPOSTE READY! [PRESS STRIKE]"
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
                        case 0 -> "Strike toward your facing";
                        case 1 ->
                                p.heroClass() == HeroClass.MAGE && p.weaponTier() >= 3
                                        ? "Charged Dawnweaver shots mend allies"
                                        : "Hold to charge, release in gold";
                        case 2 -> "Time your block before a gold hit";
                        case 3 -> "Close the gap with a lunge";
                        case 4 -> "Restore up to 60 health";
                        default -> "Inspect and change equipment";
                    };
            Menu.text(
                    labels[hover] + " / " + controls.label(ACTIONS[hover]),
                    tx + 44,
                    ty + 7,
                    13,
                    Menu.GOLD);
            Menu.text(help, tx + 44, ty + 27, 11, Menu.TEXT);
        }
    }
}
