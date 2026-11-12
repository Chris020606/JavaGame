import static com.raylib.Raylib.*;

import java.util.*;

/** City orientation is nearby and contextual, leaving the main view open while travelling. */
public final class CityPresentation {
    private String district = "", candidate = "";
    private float dwell, remaining;

    public static String districtAt(float x, float z) {
        if (z > 25) return "SOUTHERN EXPEDITION GATE";
        if (z > 9) return x > 16 ? "RIVERSIDE WORKYARDS" : "MARKET SQUARE";
        if (z < -28) return "CASTLE APPROACH";
        if (x > 12 && z < -7) return "CATHEDRAL QUARTER";
        if (x < -5 && z < -2) return "ARCHERY GREEN";
        if (x < -5) return "IRONHAND FORGE";
        if (x > 5) return "ARCANIST SANCTUM";
        return "WARDEN'S AVENUE";
    }

    public void update(float dt, float x, float z) {
        String next = districtAt(x, z);
        if (!next.equals(candidate)) {
            candidate = next;
            dwell = 0;
            remaining = 0;
        }
        dwell += Math.max(0, dt);
        remaining = Math.max(0, remaining - dt);
        if (dwell >= .8f && !district.equals(candidate)) {
            district = candidate;
            remaining = 3.5f;
        }
    }

    public String currentDistrict() {
        return district;
    }

    public String candidateDistrict() {
        return candidate;
    }

    public float remainingTime() {
        return remaining;
    }

    public float dwellTime() {
        return dwell;
    }

    public void draw(CitadelHub hub, Progression p, float px, float pz, Camera3D camera) {
        if (remaining > 0) {
            int center = GetScreenWidth() / 2;
            int width = Menu.measure(district, 19) + 48;
            DrawRectangle(center - width / 2, 88, width, 48, Fade(Menu.INK, .82f));
            Menu.center(district, center, 100, 19, Menu.GOLD);
        }
        List<CitadelNpc> nearby = new ArrayList<>();
        nearby.addAll(
                Arrays.asList(CitadelNpc.values()).stream().filter(n -> !n.isSurvivor()).toList());
        nearby.addAll(Arrays.asList(CitadelNpc.rescuedNpcs(p.rescuedSurvivors())));
        nearby.sort(
                Comparator.comparingDouble(n -> Math.hypot(hub.npcX(n) - px, hub.npcZ(n) - pz)));
        int drawn = 0;
        for (CitadelNpc npc : nearby) {
            float nx = hub.npcX(npc), nz = hub.npcZ(npc);
            float dist = (float) Math.hypot(nx - px, nz - pz);
            if (dist > 9 || dist < 1.8f || drawn >= 2) continue;
            Vector3 eye = camera._position(), target = camera.target();
            if ((nx - eye.x()) * (target.x() - eye.x())
                            + (2.7f - eye.y()) * (target.y() - eye.y())
                            + (nz - eye.z()) * (target.z() - eye.z())
                    <= 0) continue;
            Vector2 screen = GetWorldToScreen(new Vector3().x(nx).y(2.7f).z(nz), camera);
            int width = Math.max(180, Menu.measure(npc.npcName(), 13) + 58);
            int x = (int) screen.x() - width / 2, y = (int) screen.y() - 44;
            // Screen bounds and HUD exclusion zones:
            // 1. Top-left Guide panel (x < 370 && y < 190)
            // 2. Top-right City Minimap / Radar (x + width > GetScreenWidth() - 195 && y < 260)
            // 3. Top Sanctuary Banner / District Title (y < 145)
            // 4. Bottom Controls Reminder / Interaction Prompt (y > GetScreenHeight() - 150)
            if (x < 8
                    || x + width > GetScreenWidth() - 8
                    || y < 145
                    || y > GetScreenHeight() - 150
                    || (x < 370 && y < 190)
                    || (x + width > GetScreenWidth() - 195 && y < 260)) continue;
            DrawRectangle(x, y, width, 44, Fade(Menu.INK, .85f));
            String serviceIcon =
                    npc.isSurvivor()
                            ? "crown"
                            : npc == CitadelNpc.CARAVAN_TRADER
                                    ? "bag"
                                    : npc == CitadelNpc.COMMANDER
                                            ? "shield"
                                            : npc.mentorClass() == HeroClass.MAGE
                                                    ? "staff"
                                                    : npc.mentorClass() == HeroClass.ARCHER
                                                            ? "bow"
                                                            : "sword";
            Menu.icon(serviceIcon, x + 8, y + 12, 20, npc.isSurvivor() ? Menu.TEAL : Menu.GOLD);
            Menu.center(
                    npc.npcName(),
                    x + width / 2 + 12,
                    y + 5,
                    13,
                    npc.isSurvivor() ? Menu.TEAL : Menu.GOLD);
            String role =
                    npc.isSurvivor()
                            ? "RESCUED CITIZEN"
                            : npc == CitadelNpc.CARAVAN_TRADER
                                    ? "MERCHANT"
                                    : npc == CitadelNpc.COMMANDER
                                            ? "EXPEDITIONS"
                                            : "WEAPONS & ARMOR";
            Menu.center(role, x + width / 2 + 12, y + 27, 10, Menu.MUTED);
            drawn++;
        }
    }
}
