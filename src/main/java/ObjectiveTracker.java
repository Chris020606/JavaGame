import static com.raylib.Raylib.*;

/** Compact objective card and a screen-safe marker; no simulation or reward changes. */
final class ObjectiveTracker {
    record Marker(float x, float y, float angle, boolean edge) {}

    static Marker place(float sx, float sy, boolean behind, int width, int height) {
        float left = width < 1000 ? Math.min(240, width * .38f) : 420;
        float right = Math.max(left + 1, width - 200);
        float top = Math.min(140, height * .32f),
                bottom = Math.max(top + 1, height - (height < 600 ? 130 : 285));
        float cx = (left + right) / 2, cy = (top + bottom) / 2;
        if (!Float.isFinite(sx) || !Float.isFinite(sy)) {
            sx = cx;
            sy = cy + 1;
            behind = true;
        }
        float dx = sx - cx, dy = sy - cy;
        if (behind) {
            dx = -dx;
            dy = -dy;
        }
        if (Math.abs(dx) + Math.abs(dy) < .01f) dy = -1;
        boolean edge = behind || sx < left || sx > right || sy < top || sy > bottom;
        float scale =
                edge
                        ? Math.min(
                                (right - left) / 2 / Math.max(.01f, Math.abs(dx)),
                                (bottom - top) / 2 / Math.max(.01f, Math.abs(dy)))
                        : 1;
        return new Marker(
                Math.max(left, Math.min(right, edge ? cx + dx * scale : sx)),
                Math.max(top, Math.min(bottom, edge ? cy + dy * scale : sy)),
                (float) Math.toDegrees(Math.atan2(dy, dx)),
                edge);
    }

    static void draw(
            FieldEvent event,
            String citizen,
            DefenseWarnings.Threat threat,
            float px,
            float pz,
            String key,
            Camera3D camera) {
        ObjectiveStatus status = ObjectiveStatus.of(event, citizen, px, pz, key);
        Color accent =
                event.underAttack()
                                || event.time() < 15
                                || event.vulnerable() && event.health() < 30
                        ? Menu.RED
                        : Menu.TEAL;
        if (threat != null && !event.underAttack()) accent = Menu.GOLD;
        Menu.panel(24, 338, 270, 112);
        Menu.icon(
                event.kind() == FieldEvent.Kind.SURVIVOR ? "heart" : "crystal",
                38,
                350,
                18,
                accent);
        Menu.fittedText(status.title(), 66, 352, 210, 14, Menu.GOLD);
        Menu.fittedText(
                threat != null ? threat.label() : status.instruction(),
                38,
                380,
                242,
                13,
                Menu.TEXT);
        Menu.fittedText(status.detail(), 38, 405, 242, 12, accent);
        Menu.bar(38, 430, 242, status.progress(), 1, accent);
        // Lead the player home while the escort is following. If separated, point back to them.
        boolean guideHome = status.guideToBeacon() && threat == null;
        float targetX = guideHome ? 0 : event.x();
        float targetZ = guideHome ? 0 : event.z();
        Vector3 world = new Vector3().x(targetX).y(2.8f).z(targetZ);
        Vector2 screen = GetWorldToScreen(world, camera);
        float dot =
                (world.x() - camera._position().x())
                                * (camera.target().x() - camera._position().x())
                        + (world.y() - camera._position().y())
                                * (camera.target().y() - camera._position().y())
                        + (world.z() - camera._position().z())
                                * (camera.target().z() - camera._position().z());
        Marker marker =
                place(screen.x(), screen.y(), dot <= 0, GetScreenWidth(), GetScreenHeight());
        int x = Math.round(marker.x()), y = Math.round(marker.y());
        if (marker.edge()) {
            float a = (float) Math.toRadians(marker.angle());
            Vector2 tip =
                    new Vector2().x(x + (float) Math.cos(a) * 12).y(y + (float) Math.sin(a) * 12);
            Vector2 one =
                    new Vector2()
                            .x(x + (float) Math.cos(a + 2.4) * 9)
                            .y(y + (float) Math.sin(a + 2.4) * 9);
            Vector2 two =
                    new Vector2()
                            .x(x + (float) Math.cos(a - 2.4) * 9)
                            .y(y + (float) Math.sin(a - 2.4) * 9);
            DrawTriangle(tip, two, one, accent);
        } else
            Menu.icon(
                    guideHome
                            ? "shield"
                            : event.kind() == FieldEvent.Kind.SURVIVOR ? "heart" : "crystal",
                    x - 9,
                    y - 20,
                    18,
                    accent);
        String label =
                (guideHome
                                ? "BEACON"
                                : switch (event.kind()) {
                                    case SURVIVOR -> citizen;
                                    case SUPPLIES -> "CACHE";
                                    case BEACON -> "WAYSTONE";
                                })
                        + " / "
                        + Math.round(Math.hypot(targetX - px, targetZ - pz))
                        + "m";
        int labelWidth = Menu.measure(label, 11) + 12;
        int labelX = Math.min(x - labelWidth / 2, GetScreenWidth() - 192 - labelWidth);
        DrawRectangle(labelX, y + 15, labelWidth, 22, Fade(Menu.INK, .9f));
        Menu.center(label, labelX + labelWidth / 2, y + 20, 11, accent);
    }
}
