import static com.raylib.Raylib.*;

/** Readable stat cards explain the actual rules, without inventing percentages or combat perks. */
public final class CharacterStatsScreen {
    private int selected;

    public void draw(Progression p, int x, int y, int width, int height) {
        Menu.frame(x, y, width, height, Menu.color(22, 32, 48), Menu.LINE);
        Menu.text("YOUR CHARACTER / LEVEL " + p.characterLevel(), x + 18, y + 16, 20, Menu.GOLD);
        Menu.text(
                p.xp() + " / " + p.xpToNextLevel() + " XP", x + width - 160, y + 23, 12, Menu.TEAL);
        Menu.bar(x + 18, y + 56, width - 36, p.xp(), p.xpToNextLevel(), Menu.TEAL);
        String[] labels = {
            "MAXIMUM HEALTH",
            "ATTACK POWER",
            "DEFENSE",
            p.resourceLabel(),
            "RESOURCE RECOVERY",
            "MOVEMENT SPEED"
        };
        String[] values = {
            "" + p.maxHealth(),
            "" + p.damage(),
            "" + p.defense(),
            "" + p.maxStamina(),
            "+" + p.staminaRecovery() + " / sec",
            String.format(java.util.Locale.ROOT, "%.1f m/s", p.movementSpeed())
        };
        String[] hints = {
            "+" + p.nextLevelHealthGain() + " at next level",
            "+" + p.nextLevelDamageGain() + " at next level",
            "Damage blocked per hit",
            "Ability resource capacity",
            "Resource regained each second",
            "Your walking pace"
        };
        String[] icons = {
            "heart",
            "sword",
            "shield",
            p.heroClass() == HeroClass.MAGE ? "staff" : "charge",
            "potion",
            "boots"
        };
        String[] help = {
            "Health keeps you alive. Leveling raises your maximum and restores health. Vitality"
                    + " training and Heartwood Charms can raise it further.",
            "Power is the base damage used by your attacks. Weapons, level growth and Power"
                    + " training raise it. Different attacks apply their own damage multipliers.",
            "Defense subtracts from incoming damage. It is a flat amount, not a percentage. Even"
                    + " with heavy armor, a damaging hit can still hurt you.",
            p.heroClass() == HeroClass.MAGE
                    ? "Mana fuels your Mage abilities. Endurance training and Traveler's Flasks"
                            + " raise its capacity. Sprinting does not spend mana."
                    : "Stamina supports demanding combat actions and sprinting. Endurance training"
                            + " and Traveler's Flasks raise its capacity.",
            "Resource recovery restores stamina or mana over time. Recovery training and Torvald's"
                    + " Restoration Incense increase this rate.",
            "Movement is your walking speed. Higher-tier equipped boots can improve it. Use"
                    + " movement to create space and avoid enemy attacks."
        };
        int cardWidth = (width - 48) / 2;
        Vector2 mouse = GetMousePosition();
        for (int i = 0; i < 6; i++) {
            int cx = x + 18 + i % 2 * (cardWidth + 12), cy = y + 80 + i / 2 * 80;
            boolean hover =
                    mouse.x() >= cx
                            && mouse.x() < cx + cardWidth
                            && mouse.y() >= cy
                            && mouse.y() < cy + 72;
            if (hover && IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) selected = i;
            Menu.frame(
                    cx,
                    cy,
                    cardWidth,
                    72,
                    Menu.color(27, 40, 57),
                    selected == i ? Menu.GOLD : Menu.LINE);
            Menu.icon(icons[i], cx + 12, cy + 13, 20, selected == i ? Menu.GOLD : Menu.TEAL);
            Menu.text(labels[i], cx + 42, cy + 10, 11, Menu.MUTED);
            Menu.text(values[i], cx + 42, cy + 28, 20, Menu.TEXT);
            Menu.text(hints[i], cx + 42, cy + 55, 10, Menu.TEAL);
        }
        Menu.wrapped(help[selected], x + 18, y + 330, width - 36, 13, Menu.TEXT);
        Menu.text("SELECT A STAT TO LEARN HOW IT WORKS", x + 18, y + height - 22, 10, Menu.MUTED);
    }
}
