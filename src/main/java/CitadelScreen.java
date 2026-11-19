import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;

import Config.Controls;

/** Renders the Citadel Hub HUD, NPC interaction prompts, and RPG dialogue windows. */
public final class CitadelScreen {
    public enum Action {
        NONE,
        CLAIM_STARTER_WEAPON,
        OPEN_FORGE,
        OPEN_SUPPLIES,
        OPEN_TRAINING,
        DEPLOY_MISSION,
        OPEN_JOURNAL,
        OPEN_RESCUE_LEDGER,
        CLOSE_DIALOGUE
    }

    public Action draw(
            CitadelHub hub,
            Progression progress,
            Controls controls,
            float playerX,
            float playerZ,
            int chapterIndex) {
        int w = GetScreenWidth(), h = GetScreenHeight();
        Action action = Action.NONE;

        // Top Sanctuary Banner
        int saved = progress.rescuedSurvivors();
        int bw = saved > 0 ? 540 : 460, bx = (w - bw) / 2;
        Menu.panel(bx, 16, bw, 52);
        Menu.icon("crown", bx + 16, 26, 22, Menu.GOLD);
        Menu.text("AETHELGARD CITADEL", bx + 48, 24, 17, Menu.GOLD);
        Menu.text(
                "Safe Sanctuary / "
                        + (saved > 0
                                ? saved + (saved == 1 ? " Citizen Saved / " : " Citizens Saved / ")
                                : "")
                        + "Lv "
                        + progress.characterLevel()
                        + " "
                        + progress.heroClass().label
                        + " / "
                        + progress.crystals()
                        + " Crystals",
                bx + 48,
                46,
                11,
                Menu.TEAL);

        if (hub.activeNpc() == null && !hub.isTeleporting()) {
            CitadelGuide.Destination destination = CityMinimap.destination(progress);
            int guideWidth = 340, gx = 16;
            Menu.panel(gx, 88, guideWidth, 90);
            Menu.text("NEXT STEP", gx + 16, 99, 11, Menu.TEAL);
            Menu.text(destination.title(), gx + 16, 119, 17, Menu.GOLD);
            Menu.text(destination.instruction(), gx + 16, 145, 12, Menu.TEXT);
            Menu.text(
                    destination.direction(playerX, playerZ)
                            + " / "
                            + destination.distance(playerX, playerZ)
                            + "m",
                    gx + 242,
                    100,
                    12,
                    Menu.MUTED);
        }

        if (hub.activeNpc() == null && !hub.isTeleporting())
            CityMinimap.draw(progress, playerX, playerZ);

        // Bottom Controls Reminder
        int cw = 580, cx = (w - cw) / 2;
        Menu.panel(cx, h - 44, cw, 34);
        Menu.center(
                "MOVE: "
                        + controls.label(Controls.Action.FORWARD)
                        + "/"
                        + controls.label(Controls.Action.LEFT)
                        + "/"
                        + controls.label(Controls.Action.BACK)
                        + "/"
                        + controls.label(Controls.Action.RIGHT)
                        + "  /  TALK: "
                        + controls.label(Controls.Action.INTERACT)
                        + "  /  PACK: "
                        + controls.label(Controls.Action.INVENTORY)
                        + "  /  CODEX: J  /  MENU: ESC",
                w / 2,
                h - 32,
                11,
                Menu.MUTED);

        if (hub.activeNpc() == null
                && !hub.isTeleporting()
                && Menu.button(
                        "RESCUE LEDGER / "
                                + Math.min(
                                        progress.rescuedSurvivors(),
                                        CitadelNpc.RESCUED_SURVIVORS.length)
                                + " OF "
                                + CitadelNpc.RESCUED_SURVIVORS.length,
                        w - 274,
                        h - 104,
                        250,
                        true)) action = Action.OPEN_RESCUE_LEDGER;

        CitadelNpc active = hub.activeNpc();
        if (active != null) {
            // Draw Modal RPG Dialogue Box
            action = drawDialogueModal(hub, active, progress, w, h, chapterIndex);
        } else {
            // Check for nearby NPC or Portal prompt
            CitadelNpc nearby = hub.checkNearbyNpc(playerX, playerZ, progress.rescuedSurvivors());
            if (nearby != null) {
                if (drawInteractionPrompt(
                        "["
                                + controls.label(Controls.Action.INTERACT)
                                + "] TALK TO "
                                + nearby.npcName().toUpperCase(),
                        w,
                        h,
                        true)) hub.openNpc(nearby, progress);
            } else if (hub.isNearPortal(playerX, playerZ)) {
                if (!progress.hasWeapon()) {
                    drawInteractionPrompt(
                            "PORTAL SEALED [RECEIVE WEAPON FROM "
                                    + progress.heroClass().label
                                    + " MENTOR BEFORE DEPLOYING]",
                            w,
                            h);
                } else {
                    drawInteractionPrompt("STEP INTO EXPEDITION PORTAL [DEPLOY MISSION]", w, h);
                }
            }
        }

        // Teleportation flash overlay
        if (hub.isTeleporting()) {
            float t = hub.teleportTimer();
            float progressRatio = Math.max(0, Math.min(1, 1.2f - t));
            DrawRectangle(0, 0, w, h, Fade(Menu.INK, progressRatio * .65f));
            DrawRectangle(0, 0, w, h, Fade(Menu.GOLD, progressRatio * .35f));
            Menu.center(
                    "DEPLOYING TO EXPEDITION DEFENSE...",
                    w / 2,
                    h / 2,
                    24,
                    Fade(Menu.GOLD, progressRatio));
        }

        return action;
    }

    private void drawInteractionPrompt(String text, int w, int h) {
        drawInteractionPrompt(text, w, h, false);
    }

    private boolean drawInteractionPrompt(String text, int w, int h, boolean clickable) {
        int pw = Menu.measure(text, 14) + 64;
        int px = (w - pw) / 2;
        int py = h - 110;
        Menu.panel(px, py, pw, 46);
        Menu.icon("crown", px + 14, py + 12, 20, Menu.GOLD);
        Vector2 pointer = GetMousePosition();
        boolean hover =
                clickable
                        && pointer.x() >= px
                        && pointer.x() <= px + pw
                        && pointer.y() >= py
                        && pointer.y() <= py + 46;
        if (hover) DrawRectangleLines(px - 1, py - 1, pw + 2, 48, Menu.GOLD);
        Menu.text(text, px + 44, py + 14, 14, hover ? Menu.TEXT : Menu.GOLD);
        return hover && IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
    }

    private Action drawDialogueModal(
            CitadelHub hub, CitadelNpc npc, Progression progress, int w, int h, int chapterIndex) {
        Action action = Action.NONE;
        int mw = Math.min(740, w - 48);
        int textSize = 18;
        int bodyHeight = Menu.wrappedHeight(hub.npcDialogue(), mw - 48, textSize);
        int mh = Math.min(h - 48, Math.max(350, bodyHeight + 244));
        while (textSize > 14 && bodyHeight > mh - 244) {
            textSize--;
            bodyHeight = Menu.wrappedHeight(hub.npcDialogue(), mw - 48, textSize);
        }
        int mx = (w - mw) / 2, my = (h - mh) / 2;

        DrawRectangle(0, 0, w, h, Fade(Menu.INK, .45f));
        Menu.panel(mx, my, mw, mh);

        // Header
        String symbol =
                switch (npc) {
                    case BLACKSMITH -> "sword";
                    case ARCANIST -> "staff";
                    case ARCHER -> "bow";
                    case CARAVAN_TRADER -> "bag";
                    case COMMANDER -> "crown";
                    default -> "heart";
                };
        Menu.icon(symbol, mx + 24, my + 20, 24, Menu.GOLD);
        Menu.text(npc.npcName().toUpperCase(), mx + 56, my + 20, 20, Menu.GOLD);
        Menu.text(npc.title(), mx + 56, my + 46, 13, Menu.TEAL);
        DrawLine(mx + 20, my + 68, mx + mw - 20, my + 68, Menu.LINE);

        // Spoken Dialogue
        Menu.wrapped(hub.npcDialogue(), mx + 24, my + 86, mw - 48, textSize, Menu.TEXT);

        int btnY = my + mh - 132;
        DrawLine(mx + 20, btnY - 14, mx + mw - 20, btnY - 14, Menu.LINE);

        // Interactive options stay below the measured paragraph, never over its last lines.
        Vector2 pointer = GetMousePosition();
        if (IsMouseButtonPressed(MOUSE_BUTTON_LEFT)
                && (pointer.x() < mx
                        || pointer.x() > mx + mw
                        || pointer.y() < my
                        || pointer.y() > my + mh)) return Action.CLOSE_DIALOGUE;
        boolean matchesMentor = npc.mentorClass() == progress.heroClass();

        switch (npc) {
            case BLACKSMITH -> {
                if (!progress.hasWeapon()) {
                    if (matchesMentor) {
                        if (Menu.primaryButton(
                                "RECEIVE " + progress.weaponName(0).toUpperCase(),
                                mx + 24,
                                btnY,
                                330)) {
                            progress.grantStarterWeapon();
                            action = Action.CLAIM_STARTER_WEAPON;
                            hub.setLoreDialogue(
                                    "Take this blade, brother in arms! The Beacon's fire tempered"
                                        + " this steel. Raise your battle rank and bring crystals"
                                        + " when you are ready to craft heavier weapons and plate"
                                        + " armor!");
                        }
                    } else {
                        Menu.button(
                                npc.mentorClass().label.toUpperCase() + " EQUIPMENT",
                                mx + 24,
                                btnY,
                                330,
                                false);
                    }
                } else {
                    if (matchesMentor) {
                        if (Menu.primaryButton("CRAFT", mx + 24, btnY, 330)) {
                            action = Action.OPEN_FORGE;
                        }
                    } else {
                        Menu.button(
                                npc.mentorClass().label.toUpperCase() + " EQUIPMENT",
                                mx + 24,
                                btnY,
                                330,
                                false);
                    }
                }
                if (Menu.button("TALK", mx + 386, btnY, 330, true)) {
                    hub.setLoreDialogue(npc.lore());
                }
                if (Menu.button("LEAVE [ESC]", mx + 24, btnY + 54, 692, true)) {
                    action = Action.CLOSE_DIALOGUE;
                }
            }
            case ARCANIST -> {
                if (!progress.hasWeapon()) {
                    if (matchesMentor) {
                        if (Menu.primaryButton(
                                "RECEIVE " + progress.weaponName(0).toUpperCase(),
                                mx + 24,
                                btnY,
                                330)) {
                            progress.grantStarterWeapon();
                            action = Action.CLAIM_STARTER_WEAPON;
                            hub.setLoreDialogue(
                                    "Take this sun-wand, apprentice! May the Beacon's embers ignite"
                                            + " your spells! Return to me when you earn crystals to"
                                            + " craft greater staves.");
                        }
                    } else {
                        Menu.button(
                                npc.mentorClass().label.toUpperCase() + " EQUIPMENT",
                                mx + 24,
                                btnY,
                                330,
                                false);
                    }
                } else {
                    if (matchesMentor) {
                        if (Menu.primaryButton("CRAFT", mx + 24, btnY, 330)) {
                            action = Action.OPEN_FORGE;
                        }
                    } else {
                        Menu.button(
                                npc.mentorClass().label.toUpperCase() + " EQUIPMENT",
                                mx + 24,
                                btnY,
                                330,
                                false);
                    }
                }
                if (Menu.button("TALK", mx + 386, btnY, 330, true)) {
                    hub.setLoreDialogue(npc.lore());
                }
                if (Menu.button("LEAVE [ESC]", mx + 24, btnY + 54, 692, true)) {
                    action = Action.CLOSE_DIALOGUE;
                }
            }
            case CARAVAN_TRADER -> {
                if (Menu.primaryButton("BROWSE TORVALD'S GOODS", mx + 24, btnY, 330)) {
                    action = Action.OPEN_SUPPLIES;
                }
                if (Menu.button("TALK", mx + 386, btnY, 330, true)) {
                    hub.setLoreDialogue(npc.lore());
                }
                if (Menu.button("LEAVE [ESC]", mx + 24, btnY + 54, 692, true)) {
                    action = Action.CLOSE_DIALOGUE;
                }
            }
            case ARCHER -> {
                if (!progress.hasWeapon()) {
                    if (matchesMentor) {
                        if (Menu.primaryButton(
                                "RECEIVE " + progress.weaponName(0).toUpperCase(),
                                mx + 24,
                                btnY,
                                330)) {
                            progress.grantStarterWeapon();
                            action = Action.CLAIM_STARTER_WEAPON;
                            hub.setLoreDialogue(
                                    "Here is your hunting bow, marksman! Keep your eye sharp and"
                                        + " your aim steady! Return to me when you have crystals to"
                                        + " craft heavier recurve bows and crossbows.");
                        }
                    } else {
                        Menu.button(
                                npc.mentorClass().label.toUpperCase() + " EQUIPMENT",
                                mx + 24,
                                btnY,
                                330,
                                false);
                    }
                } else {
                    if (matchesMentor) {
                        if (Menu.primaryButton("CRAFT", mx + 24, btnY, 330)) {
                            action = Action.OPEN_FORGE;
                        }
                    } else {
                        Menu.button(
                                npc.mentorClass().label.toUpperCase() + " EQUIPMENT",
                                mx + 24,
                                btnY,
                                330,
                                false);
                    }
                }
                if (Menu.button("TALK", mx + 386, btnY, 330, true)) {
                    hub.setLoreDialogue(npc.lore());
                }
                if (Menu.button("LEAVE [ESC]", mx + 24, btnY + 54, 692, true)) {
                    action = Action.CLOSE_DIALOGUE;
                }
            }
            case COMMANDER -> {
                if (!progress.hasWeapon()) {
                    Menu.button(
                            "RECEIVE YOUR FIRST WEAPON BEFORE DEPLOYING",
                            mx + 24,
                            btnY,
                            692,
                            false);
                } else {
                    String deployLabel =
                            "DEPLOY EXPEDITION: CHAPTER "
                                    + (chapterIndex + 1)
                                    + " / "
                                    + Campaign.CHAPTERS[chapterIndex].title();
                    if (Menu.primaryButton(deployLabel, mx + 24, btnY, 692)) {
                        action = Action.DEPLOY_MISSION;
                    }
                }
                if (Menu.button("TALK", mx + 24, btnY + 52, 330, true)) {
                    hub.setLoreDialogue(npc.lore());
                }
                if (Menu.button("LEAVE [ESC]", mx + 386, btnY + 52, 330, true)) {
                    action = Action.CLOSE_DIALOGUE;
                }
            }
            default -> {
                if (Menu.primaryButton("TALK", mx + 24, btnY, 330)) {
                    hub.setLoreDialogue(npc.lore());
                }
                if (Menu.button("OFFER WARDEN'S BLESSING", mx + 386, btnY, 330, true)) {
                    hub.setLoreDialogue(
                            "The survivor bows their head in deep gratitude. 'The warmth of the"
                                    + " Primordial Beacon and your courage keep us alive,"
                                    + " Warden!'");
                }
                if (Menu.button("LEAVE [ESC]", mx + 24, btnY + 52, 692, true)) {
                    action = Action.CLOSE_DIALOGUE;
                }
            }
        }

        return action;
    }
}
