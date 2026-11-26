import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;

import Config.Controls;

/** Renders the Citadel Hub HUD, NPC interaction prompts, and RPG dialogue windows. */
public final class CitadelScreen {
    private CitadelNpc previousSpeaker;

    public enum Action {
        NONE,
        CLAIM_STARTER_WEAPON,
        OPEN_FORGE,
        OPEN_SUPPLIES,
        OPEN_TRAINING,
        DEPLOY_MISSION,
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

        CitadelNpc speaker = hub.activeNpc();
        if (speaker != null && !hub.isTeleporting()) {
            boolean acceptKeys = previousSpeaker == speaker;
            previousSpeaker = speaker;
            return drawDialogueModal(hub, speaker, progress, w, h, chapterIndex, acceptKeys);
        }
        previousSpeaker = null;

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
            CitadelGuide.Destination destination = CityMinimap.destination(progress, hub);
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
            CityMinimap.draw(hub, progress, playerX, playerZ);

        // Bottom Controls Reminder
        int cw = 580, cx = (w - cw) / 2;
        Menu.panel(cx, h - 44, cw, 34);
        Menu.center(
                "Move  "
                        + controls.label(Controls.Action.FORWARD)
                        + "/"
                        + controls.label(Controls.Action.LEFT)
                        + "/"
                        + controls.label(Controls.Action.BACK)
                        + "/"
                        + controls.label(Controls.Action.RIGHT)
                        + "    Talk  "
                        + controls.label(Controls.Action.INTERACT)
                        + "    Pack  "
                        + controls.label(Controls.Action.INVENTORY)
                        + "    Esc  Menu",
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

        if (!hub.isTeleporting()) {
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
        Menu.icon(clickable ? "talk" : "arrow", px + 14, py + 12, 20, Menu.GOLD);
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
            CitadelHub hub,
            CitadelNpc npc,
            Progression progress,
            int w,
            int h,
            int chapterIndex,
            boolean acceptKeys) {
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
        boolean first = acceptKeys && IsKeyPressed(KEY_ONE);
        boolean second = acceptKeys && IsKeyPressed(KEY_TWO);
        boolean commander = npc == CitadelNpc.COMMANDER;
        if (npc.mentorClass() != null) {
            if (npc.mentorClass() != progress.heroClass()) {
                Menu.button(
                        npc.mentorClass().label.toUpperCase() + " EQUIPMENT",
                        mx + 24,
                        btnY,
                        330,
                        false);
            } else if (progress.hasWeapon()) {
                if (Menu.primaryButton("[1] Craft equipment", symbol, mx + 24, btnY, 330) || first)
                    return Action.OPEN_FORGE;
            } else if (Menu.primaryButton(
                            "[1] Receive " + progress.weaponName(0), symbol, mx + 24, btnY, 330)
                    || first) {
                progress.grantStarterWeapon();
                hub.setLoreDialogue(
                        switch (npc) {
                            case BLACKSMITH ->
                                    "Good fit. Bring me crystals from the valley when you need a"
                                            + " stronger blade or a piece of armor.";
                            case ARCHER ->
                                    "That'll draw true. Bring me crystals when you're ready for a"
                                            + " new bow or a piece of ranger armor.";
                            default ->
                                    "There. It's yours. Bring me crystals when you're ready to"
                                            + " strengthen your staff or weave your first robes.";
                        });
                return Action.CLAIM_STARTER_WEAPON;
            }
        } else if (commander) {
            if (!progress.hasWeapon())
                Menu.button(
                        "Receive your first weapon before deploying", mx + 24, btnY, 692, false);
            else if (Menu.primaryButton(
                            "[1] Deploy to " + Campaign.CHAPTERS[chapterIndex].title(),
                            "arrow",
                            mx + 24,
                            btnY,
                            692)
                    || first) return Action.DEPLOY_MISSION;
        } else if (npc == CitadelNpc.CARAVAN_TRADER) {
            if (Menu.primaryButton("[1] Browse goods", "bag", mx + 24, btnY, 330) || first)
                return Action.OPEN_SUPPLIES;
        } else {
            if (Menu.primaryButton("[1] Talk", "talk", mx + 24, btnY, 330) || first)
                hub.setLoreDialogue(npc.lore());
            if (Menu.button("[2] Offer a blessing", mx + 386, btnY, 330, true) || second)
                hub.setLoreDialogue(npc.blessingReply());
        }
        if (!npc.isSurvivor()
                && (Menu.button(
                                "[2] Talk",
                                commander ? mx + 24 : mx + 386,
                                commander ? btnY + 54 : btnY,
                                330,
                                true)
                        || second)) hub.setLoreDialogue(npc.lore());
        if (Menu.button(
                "Leave [Esc]",
                commander ? mx + 386 : mx + 24,
                btnY + 54,
                commander ? 330 : 692,
                true)) return Action.CLOSE_DIALOGUE;
        return Action.NONE;
    }
}
