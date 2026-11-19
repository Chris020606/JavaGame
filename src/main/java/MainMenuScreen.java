import static com.raylib.Raylib.*;

/** Title presentation only: actions are handled by Game, never by the scenery. */
public final class MainMenuScreen {
    private final TitleSky sky = new TitleSky();

    TitleSky sky() {
        return sky;
    }

    public enum Action {
        NONE,
        NEW_GAME,
        LOAD_GAME,
        QUIT,
        SETTINGS,
        CHRONICLE,
        PLAYER_HISTORY
    }

    public Action draw(String playerName, boolean hasSave, String message) {
        int width = GetScreenWidth(), height = GetScreenHeight();
        int x = Math.max(48, (width - 1100) / 2);
        int y = Math.max(48, (height - 540) / 2);
        Vector2 pointer = GetMousePosition();
        sky.update(
                GetFrameTime(),
                pointer.x(),
                pointer.y(),
                IsMouseButtonPressed(MOUSE_BUTTON_LEFT),
                IsMouseButtonDown(MOUSE_BUTTON_LEFT),
                width,
                height);
        winterBackdrop(width, height);
        boolean overSky = sky.hovered(pointer.x(), pointer.y(), width, height);
        SetMouseCursor(
                sky.dragging() || overSky ? MOUSE_CURSOR_POINTING_HAND : MOUSE_CURSOR_DEFAULT);
        // A compact action column leaves the citadel silhouette visible at every supported size.
        Menu.text("THE FROZEN CITADEL", x + 30, y, 15, Menu.TEAL);
        Menu.ember(x, y - 3, 18, 1, Menu.GOLD);
        Menu.text("OUTPOST", x - 1, y + 41, 76, Fade(Menu.GOLD, .22f));
        Menu.text("OUTPOST", x - 3, y + 38, 76, Menu.TEXT);
        Menu.text("A E T H E L G A R D", x + 3, y + 123, 11, Menu.MUTED);
        DrawRectangle(x, y + 149, 52, 2, Menu.GOLD);
        DrawLine(x + 64, y + 150, x + 290, y + 150, Fade(Menu.GOLD, .22f));
        Menu.text("The kingdom fell. Its last light remains.", x, y + 166, 20, Menu.GOLD);
        Menu.wrapped(
                "Venture beyond the walls. Bring the survivors home.",
                x,
                y + 204,
                420,
                17,
                Menu.MUTED);
        Menu.frame(x - 16, y + 270, 452, 250, Fade(Menu.PANEL, .88f), Fade(Menu.GOLD, .5f));
        DrawRectangle(x - 16, y + 270, 3, 250, Fade(Menu.GOLD, .65f));
        String name = playerName;
        boolean shortened = false;
        while (Menu.measure("WARDEN / " + name + (shortened ? "..." : ""), 15) > 280
                && name.length() > 1) {
            name = name.substring(0, name.length() - 1);
            shortened = true;
        }
        Menu.text("WARDEN / " + name + (shortened ? "..." : ""), x, y + 286, 15, Menu.TEXT);
        Menu.text(hasSave ? "JOURNEY SAVED" : "A NEW CHAPTER", x + 300, y + 288, 10, Menu.TEAL);
        Action action = Action.NONE;
        if (Menu.primaryButton(
                hasSave ? "CONTINUE YOUR JOURNEY" : "BEGIN YOUR JOURNEY", x, y + 321, 420))
            action = hasSave ? Action.LOAD_GAME : Action.NEW_GAME;
        if (Menu.button("NEW WARDEN", x, y + 380, 200, true)) action = Action.NEW_GAME;
        if (Menu.button("PLAYERS & SAVES", x + 220, y + 380, 200, true))
            action = Action.PLAYER_HISTORY;
        if (Menu.button("CHRONICLE", x, y + 439, 130, true)) action = Action.CHRONICLE;
        if (Menu.button("SETTINGS", x + 145, y + 439, 130, true)) action = Action.SETTINGS;
        if (Menu.button("QUIT", x + 290, y + 439, 130, true)) action = Action.QUIT;
        if (!message.isEmpty()) Menu.text(message, x, y + 532, 14, Menu.GOLD);
        Menu.text("v" + BuildInfo.VERSION, x, height - 32, 12, Menu.MUTED);
        return action;
    }

    private void winterBackdrop(int width, int height) {
        float light = sky.daylight();
        DrawRectangleGradientV(
                0,
                0,
                width,
                height,
                tint(16, 30, 48, 76, 145, 186, light),
                tint(6, 12, 22, 188, 208, 206, light));
        double time = GetTime();
        // Stars fade with daylight; clouds drift independently of the draggable sky clock.
        for (int i = 0; i < 45; i++) {
            int sx = (i * 193 + 41) % width;
            int sy = (i * 71 + 27) % Math.max(1, height / 2);
            DrawCircle(
                    sx,
                    sy,
                    1,
                    Fade(Menu.TEXT, (1 - light) * (.12f + .10f * (float) Math.sin(time * .5 + i))));
        }
        int horizon = height * 2 / 3;
        for (int i = 0; i < 2; i++) {
            TitleSky.Body body = sky.body(i, width, height);
            if (!body.visible()) continue;
            int bx = Math.round(body.x()), by = Math.round(body.y());
            Color glow = i == 0 ? Menu.GOLD : Menu.TEAL;
            DrawCircle(bx, by, 64, Fade(glow, .06f));
            DrawCircle(bx, by, 48, Fade(glow, .08f));
            DrawCircle(bx, by, 32, i == 0 ? Menu.color(255, 224, 153) : Menu.color(199, 220, 228));
            if (i == 1) {
                DrawCircle(bx - 9, by - 6, 7, Fade(Menu.INK, .12f));
                DrawCircle(bx + 12, by + 9, 5, Fade(Menu.INK, .10f));
                DrawCircle(bx + 6, by - 15, 4, Fade(Menu.INK, .10f));
            }
        }
        driftingClouds(width, height, time, light);
        for (int layer = 0; layer < 3; layer++) {
            Color shade =
                    tint(
                            22 - layer * 5,
                            42 - layer * 8,
                            58 - layer * 10,
                            104 - layer * 22,
                            146 - layer * 22,
                            164 - layer * 22,
                            light);
            for (int i = 0; i < 9; i++) {
                int cx = i * width / 7 + layer * 57;
                int peak = horizon - 170 + (i * 83 % 160) + layer * 65;
                triangle(cx + 240, height, cx, peak, cx - 180, height, shade);
                triangle(
                        cx,
                        peak,
                        cx + 240,
                        height,
                        cx + 25,
                        height,
                        colorBlend(shade, tint(13, 26, 42, 70, 112, 142, light), .32f));
                // Snow follows each mountain's slopes, with a broken lower edge.
                float coverage = .20f + (i % 3) * .035f;
                float snowY = peak + (height - peak) * coverage;
                Color cap =
                        tint(
                                74 - layer * 8,
                                98 - layer * 8,
                                117 - layer * 8,
                                211 - layer * 15,
                                226 - layer * 12,
                                230 - layer * 10,
                                light);
                triangle(cx, peak, cx - 180 * coverage, snowY, cx + 240 * coverage, snowY, cap);
                triangle(
                        cx,
                        peak,
                        cx + 240 * coverage,
                        snowY,
                        cx + 25 * coverage,
                        snowY,
                        colorBlend(cap, shade, .22f));
                triangle(cx - 180 * coverage, snowY, cx - 14, snowY + 14, cx + 7, snowY - 12, cap);
                triangle(cx + 240 * coverage, snowY, cx + 12, snowY + 20, cx - 5, snowY - 9, cap);
            }
        }
        // A restrained distance veil separates the mountain ranges from the foreground clearing.
        Color haze = tint(45, 63, 85, 166, 198, 215, light);
        DrawRectangleGradientV(0, horizon - 80, width, 75, Fade(haze, 0), Fade(haze, .10f));
        DrawRectangleGradientV(0, horizon - 5, width, 75, Fade(haze, .10f), Fade(haze, 0));
        citadel(width, height, horizon);
        // Deterministic particles need no allocations, textures or persistent simulation state.
        for (int i = 0; i < 65; i++) {
            int sx = (int) ((i * 137 + time * (8 + i % 5)) % width);
            int sy = (int) ((i * 97 + time * (16 + i % 11)) % height);
            DrawCircle(sx, sy, i % 3 == 0 ? 2 : 1, Fade(Menu.TEXT, .12f + (i % 4) * .05f));
        }
        DrawRectangleGradientH(
                0, 0, width * 3 / 5, height, Fade(Menu.INK, .85f), Fade(Menu.INK, 0));
    }

    /** Opaque blended silhouettes prevent overlapping puffs from showing oval seams. */
    private static void driftingClouds(int width, int height, double time, float light) {
        for (int layer = 0; layer < 2; layer++) {
            for (int i = 0; i < 3; i++) {
                int seed = i * 97 + layer * 53;
                float scale = .75f + (seed % 5) * .12f + layer * .15f;
                float x =
                        (float) ((seed * 11 + time * (5 + layer * 5 + i * .7)) % (width + 440))
                                - 220;
                float y = height * (.12f + (seed % 7) * .032f + layer * .08f);
                Color top =
                        tint(
                                47 + layer * 5,
                                63 + layer * 5,
                                83 + layer * 5,
                                228 + layer * 5,
                                237 + layer * 4,
                                239 + layer * 3,
                                light);
                Color bottom =
                        tint(28, 43, 62, 159 + layer * 9, 188 + layer * 7, 204 + layer * 5, light);
                // Sample one continuous outline; each column shares the same vertical lighting.
                for (int column = -128; column < 128; column++) {
                    float upper = 100, lower = -100;
                    for (int puff = 0; puff < 5; puff++) {
                        float center = -72 + puff * 36;
                        float radius = 31 + ((seed + puff * 19) % 19);
                        float dx = (column + .5f - center) / radius;
                        if (Math.abs(dx) >= 1) continue;
                        float bulge = (float) Math.sqrt(1 - dx * dx);
                        float lift = puff == 2 ? -11 : puff == 1 ? -5 : 1;
                        upper = Math.min(upper, lift - radius * .64f * bulge);
                        lower = Math.max(lower, 7 + radius * .24f * bulge);
                    }
                    if (upper >= lower) continue;
                    float t = Math.max(0, Math.min(1, (upper + 44) / 65));
                    Color start = colorBlend(top, bottom, t);
                    DrawRectangleGradientV(
                            Math.round(x + column * scale),
                            Math.round(y + upper * scale),
                            Math.max(1, (int) Math.ceil(scale)),
                            Math.max(1, Math.round((lower - upper) * scale)),
                            start,
                            bottom);
                }
            }
        }
    }

    private static Color colorBlend(Color a, Color b, float t) {
        return Menu.color(
                Math.round(Byte.toUnsignedInt(a.r()) * (1 - t) + Byte.toUnsignedInt(b.r()) * t),
                Math.round(Byte.toUnsignedInt(a.g()) * (1 - t) + Byte.toUnsignedInt(b.g()) * t),
                Math.round(Byte.toUnsignedInt(a.b()) * (1 - t) + Byte.toUnsignedInt(b.b()) * t));
    }

    /** Castle coordinates remain fixed above the descending wooded mountainside. */
    private void citadel(int width, int height, int horizon) {
        int cx = width * 3 / 4, base = horizon + 110;
        float light = sky.daylight();
        Color stone = tint(22, 35, 47, 105, 126, 136, light);
        Color shade = tint(12, 23, 34, 62, 84, 98, light);
        Color trim = tint(56, 73, 85, 183, 201, 207, light);
        Color snow = tint(90, 114, 133, 221, 231, 232, light);
        Color roof = tint(15, 30, 41, 43, 77, 93, light);

        valleyFloor(width, height, cx, base, light);
        mountainside(cx, base, height, light);

        // Rear keep rises above unequal flanking towers, with a shaded side face.
        tower(cx - 42, base - 226, 86, 202, 61, stone, shade, roof, trim, snow, light);
        tower(cx - 141, base - 175, 49, 153, 46, stone, shade, roof, trim, snow, light);
        tower(cx + 95, base - 192, 56, 170, 52, stone, shade, roof, trim, snow, light);
        tower(cx - 88, base - 135, 35, 112, 36, shade, shade, roof, trim, snow, light);
        tower(cx + 54, base - 143, 34, 120, 37, shade, shade, roof, trim, snow, light);
        banner(cx - 116, base - 223, 0, trim);
        banner(cx + 124, base - 246, 2, trim);

        // Crenellated front wall with sparse masonry courses and projecting buttresses.
        masonry(cx - 158, base - 68, 320, 60, stone, shade);
        for (int i = 0; i < 17; i++) {
            int bx = cx - 158 + i * 19;
            DrawRectangle(bx, base - 78, 11, 13, stone);
            DrawRectangle(bx, base - 79, 12, 3, snow);
        }
        DrawRectangle(cx - 162, base - 64, 328, 4, trim);
        for (int bx : new int[] {cx - 142, cx - 102, cx + 102, cx + 142}) {
            DrawRectangle(bx, base - 56, 10, 48, shade);
            DrawRectangle(bx - 2, base - 58, 14, 3, snow);
        }

        // Gatehouse: an arched entry, portcullis, heavy stone surround and a lit approach.
        masonry(cx - 42, base - 100, 88, 93, stone, shade);
        DrawRectangle(cx + 35, base - 100, 11, 93, shade);
        for (int i = 0; i < 6; i++) {
            DrawRectangle(cx - 44 + i * 16, base - 111, 10, 14, stone);
            DrawRectangle(cx - 44 + i * 16, base - 112, 11, 3, snow);
        }
        DrawRectangle(cx - 45, base - 96, 94, 4, trim);
        DrawCircle(cx + 2, base - 42, 23, trim);
        DrawRectangle(cx - 21, base - 42, 46, 35, trim);
        DrawCircle(cx + 2, base - 42, 17, shade);
        DrawRectangle(cx - 15, base - 42, 34, 35, shade);
        DrawRectangle(cx - 11, base - 42, 25, 35, tint(20, 24, 28, 42, 49, 51, light));
        for (int i = 0; i < 5; i++)
            DrawLine(cx - 11 + i * 6, base - 53, cx - 11 + i * 6, base - 8, Fade(trim, .65f));
        DrawLine(cx - 14, base - 30, cx + 18, base - 30, trim);
        DrawLine(cx - 14, base - 17, cx + 18, base - 17, trim);
        Menu.ember(cx - 6, base - 88, 16, 1, Menu.GOLD);
        for (int side : new int[] {-1, 1}) {
            int tx = cx + 2 + side * 32;
            DrawRectangle(tx - 2, base - 39, 4, 14, shade);
            DrawCircle(tx, base - 41, 15, Fade(Menu.GOLD, .035f * (1 - light)));
            Menu.ember(tx - 3, base - 46, 7, .8f, Menu.GOLD);
        }

        // Grounded trees are in front of the lower wall, sorted by their feet rather than crown
        // tips.
        for (TitleForest.Tree tree : TitleForest.at(height)) {
            DrawEllipse(
                    cx + tree.x() + tree.size() / 7,
                    tree.y() + 1,
                    tree.size() * .25f,
                    2 + tree.size() * .025f,
                    Fade(tint(12, 32, 45, 85, 126, 142, light), .26f));
            approachTree(cx + tree.x(), tree.y(), tree.size(), tree.crown(), light);
        }

        int beaconY = base - 291;
        float pulse = .85f + .15f * (float) Math.sin(GetTime() * 1.3);
        for (int r = 4; r > 0; r--)
            DrawCircle(cx + 1, beaconY, r * 9, Fade(Menu.GOLD, .018f * pulse));
        Menu.ember(cx - 6, beaconY - 8, 15, pulse, Menu.GOLD);
        Menu.center("A E T H E L G A R D", cx + 10, horizon - 225, 12, Fade(Menu.TEXT, .6f));
    }

    /**
     * A shared ground plane, perspective river and clustered woods connect the mountain to its
     * valley.
     */
    private static void valleyFloor(int width, int height, int cx, int base, float light) {
        Color far = tint(34, 51, 65, 171, 195, 203, light);
        Color near = tint(19, 37, 49, 132, 167, 179, light);
        // A shallow curved skyline avoids the hard horizontal stripe of the former ground panel.
        for (int x = 0; x < width; x += 4) {
            int top =
                    Math.round(
                            height * .75f
                                    + 13 * (float) Math.sin(x * .004f)
                                    + 15 * (float) Math.cos(x * .008f));
            DrawRectangleGradientV(x, top, 4, height - top, far, near);
        }
        Color shore = tint(67, 91, 109, 215, 230, 232, light);
        Color water = tint(21, 60, 88, 49, 127, 165, light);
        // The channel narrows into the distance and widens toward the viewer, rather than
        // cutting across the valley like a horizontal line. Both ends disappear outside view.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 80; i++) {
                float t = i / 79f, next = (i + 1) / 79f;
                Vector2 from = riverPoint(width, height, t);
                Vector2 to = riverPoint(width, height, next);
                DrawLineEx(
                        from,
                        to,
                        4 + t * t * 29 + (pass == 0 ? 4 + t * 7 : 0),
                        pass == 0 ? shore : water);
            }
        }
        // A darker inner current and a broken bright bank give the water depth without outlining
        // it.
        for (int i = 0; i < 65; i++) {
            float t = i / 64f;
            Vector2 from = riverPoint(width, height, t);
            Vector2 to = riverPoint(width, height, (i + 1) / 64f);
            DrawLineEx(from, to, 1 + t * t * 11, Fade(tint(10, 38, 63, 31, 91, 131, light), .5f));
        }
        Vector2 source = riverPoint(width, height, 0);
        DrawEllipse(Math.round(source.x() + 8), Math.round(source.y() - 10), 40, 15, far);
        Color glint = Fade(tint(90, 142, 173, 174, 219, 232, light), .65f);
        for (int i = 0; i < 24; i++) {
            float t = .06f + .94f * (float) ((i / 24.0 + GetTime() * .008) % 1);
            Vector2 point = riverPoint(width, height, t);
            float offset = (i % 3 - 1) * (1 + t * 5);
            DrawLine(
                    Math.round(point.x() + offset),
                    Math.round(point.y()),
                    Math.round(point.x() + offset + 2 + t * 7),
                    Math.round(point.y()),
                    glint);
        }
        // Irregular groups with open snow between them replace evenly spaced rows.
        for (int[] tree : valleyTrees(width, height)) {
            Color shadow = Fade(tint(13, 31, 44, 93, 137, 150, light), .35f);
            DrawEllipse(tree[0] + tree[2] / 6, tree[1] + 1, tree[2] * .35f, 3, shadow);
            approachTree(tree[0], tree[1], tree[2], Math.max(3, tree[2] / 9), light);
        }
        // Low snow shelves nestle into the bends rather than sitting as isolated decorations.
        for (float t : new float[] {.28f, .34f, .62f, .83f}) {
            Vector2 point = riverPoint(width, height, t);
            int side = t < .5f ? -1 : 1;
            int x = Math.round(point.x() + side * (9 + t * 18));
            int y = Math.round(point.y());
            DrawEllipse(x, y + 2, 6 + t * 12, 2 + t * 2, tint(44, 66, 84, 128, 160, 178, light));
            DrawEllipse(x - 1, y, 6 + t * 12, 2 + t * 2, shore);
        }
        // Small riverbank rocks establish scale without decorating every stretch of shoreline.
        for (float t : new float[] {.43f, .48f, .72f, .77f, .91f}) {
            Vector2 point = riverPoint(width, height, t);
            int x = Math.round(point.x() + 13 + t * 23), y = Math.round(point.y());
            DrawEllipse(x, y, 3 + t * 5, 2 + t, tint(41, 61, 75, 101, 127, 138, light));
            DrawEllipse(x - 1, y - 1, 2 + t * 4, 1, shore);
        }
    }

    private static int valleyWidth = -1, valleyHeight = -1;
    private static java.util.List<int[]> cachedValley = java.util.List.of();

    /**
     * Placement and river clearance are cached; only lighting and water motion change per frame.
     */
    private static java.util.List<int[]> valleyTrees(int width, int height) {
        if (width == valleyWidth && height == valleyHeight) return cachedValley;
        java.util.Random placement = new java.util.Random(67329);
        java.util.ArrayList<int[]> trees = new java.util.ArrayList<>();
        for (int i = 0; i < 600; i++) {
            float t = placement.nextFloat();
            int x = Math.round(placement.nextFloat() * (width + 40)) - 20;
            int y = Math.round(height * .77f + t * height * .25f);
            int size = Math.round(16 + t * 35 + placement.nextFloat() * 13);
            boolean clear = true;
            // Check the whole crown at three elevations to keep the meandering river readable.
            for (int check = 0; check < 3; check++) {
                float py = y - size * check / 2f;
                float riverT = (py - height * .77f) / (height * .29f);
                if (riverT >= 0 && riverT <= 1.1f) {
                    float channelX = riverPoint(width, height, riverT).x();
                    if (Math.abs(x - channelX) < 28 + riverT * 20) clear = false;
                }
            }
            if (clear) {
                for (int[] other : trees) {
                    float dx = (x - other[0]) / (25 + t * 12), dy = (y - other[1]) / 15f;
                    if (dx * dx + dy * dy < 1) {
                        clear = false;
                        break;
                    }
                }
                if (clear) trees.add(new int[] {x, y, size});
            }
        }
        trees.sort(java.util.Comparator.comparingInt(t -> t[1]));
        valleyWidth = width;
        valleyHeight = height;
        return cachedValley = java.util.List.copyOf(trees);
    }

    private static Vector2 riverPoint(int width, int height, float t) {
        return new Vector2()
                .x(width * (.52f - t * .22f) - (float) Math.sin(t * Math.PI * 1.7) * 48)
                .y(height * (.77f + t * .29f));
    }

    /**
     * Only the terrain below the original gate elevation changes; distant mountains stay intact.
     */
    private static void mountainside(int cx, int base, int height, float light) {
        float depth = height - base;
        Color snow = tint(52, 75, 91, 205, 221, 221, light);
        // A rounded shoulder merges into the valley instead of a faceted cut-out rock.
        Color foothill = tint(23, 42, 56, 132, 167, 179, light);
        for (int y = base - 8; y < height + 45; y += 3) {
            float t = Math.max(0, Math.min(1, (y - base + 8) / (depth + 53)));
            float shoulder = 183 + 207 * (float) Math.pow(t, .75);
            Color shade = colorBlend(snow, foothill, .18f + t * .82f);
            DrawRectangle(cx - Math.round(shoulder), y, Math.round(shoulder * 2), 3, shade);
        }

        // Switchbacks descend from the gate; widening turns establish distance without moving the
        // castle.
        float[] routeX = {2, 45, 90, 24, -76, -23};
        float[] routeY = {-.045f, .13f, .3f, .5f, .75f, 1.15f};
        Color road = tint(70, 75, 72, 150, 144, 123, light);
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < routeX.length - 1; i++) {
                Vector2 from = new Vector2().x(cx + routeX[i]).y(base + depth * routeY[i]);
                Vector2 to = new Vector2().x(cx + routeX[i + 1]).y(base + depth * routeY[i + 1]);
                float thickness = 5 + i * 2.4f + (pass == 0 ? 3 : 0);
                Color color = pass == 0 ? snow : road;
                DrawLineEx(from, to, thickness, color);
                DrawCircle(Math.round(to.x()), Math.round(to.y()), thickness * .5f, color);
            }
        }
    }

    private static void approachTree(int x, int y, int size, int crown, float light) {
        int seed = Math.floorMod(x * 31 + y * 17, 997), kind = seed % 2;
        int radius = crown + 12;
        Color bark = tint(32, 38, 41, 99, 92, 80, light);
        Color pine =
                tint(
                        10 + seed % 9,
                        29 + seed % 13,
                        34 + seed % 11,
                        30 + seed % 18,
                        67 + seed % 24,
                        57 + seed % 20,
                        light);
        Color snow = tint(62, 84, 99, 201, 218, 216, light);
        Color foliageLight = colorBlend(pine, tint(49, 70, 77, 83, 122, 108, light), .45f);
        // Small snow cushions tie trunks into the terrain instead of leaving a pasted-on edge.
        DrawEllipse(x + 1, y, Math.max(3, crown * .65f), 1.5f, Fade(snow, .38f));
        DrawRectangle(x - 1, y - size * 3 / 4, 3, size * 3 / 4, bark);
        if (kind == 1) {
            // Rounded, wind-shaped pines have offset lobes instead of repeated triangular tiers.
            DrawEllipse(x, y - size * 3 / 5, radius - 2, size * .37f, pine);
            DrawEllipse(x - radius / 3, y - size * 2 / 3, radius * .72f, size * .23f, pine);
            DrawEllipse(x + radius / 3, y - size / 2, radius * .7f, size * .23f, pine);
            DrawEllipse(x - radius / 3, y - size * 2 / 3, radius * .44f, size * .18f, foliageLight);
            DrawEllipse(x + radius / 5, y - size / 2, radius * .32f, size * .12f, foliageLight);
            DrawEllipse(x - 2, y - size * 8 / 9, radius * .48f, size * .045f, snow);
            DrawEllipse(x - radius / 3, y - size * 3 / 4, radius * .43f, size * .04f, snow);
            DrawEllipse(x + radius / 3, y - size * 3 / 5, radius * .42f, size * .04f, snow);
        } else {
            int lean = seed % 5 - 2;
            int top = y - size;
            int bottom = y - size / 7;
            triangle(x - radius, bottom, x + lean, top, x + radius, bottom, pine);
            triangle(x - radius, bottom, x + lean, top, x - radius * .4f, bottom, foliageLight);
            triangle(
                    x + lean,
                    top,
                    x + radius,
                    bottom,
                    x + 2,
                    bottom,
                    tint(8, 23, 30, 25, 60, 57, light));
            float snowY = top + size * (.25f + seed % 4 * .025f);
            triangle(x + lean, top, x - radius * .4f, snowY, x + radius * .35f, snowY + 3, snow);
        }
    }

    private void tower(
            int x,
            int y,
            int width,
            int height,
            int roofHeight,
            Color stone,
            Color shade,
            Color roof,
            Color trim,
            Color snow,
            float light) {
        masonry(x, y, width, height, stone, shade);
        DrawRectangle(x + width * 3 / 4, y, width / 4, height, shade);
        int peak = x + width / 2;
        triangle(x - 7, y, peak, y - roofHeight, x + width + 7, y, roof);
        triangle(peak, y - roofHeight, x + width * 3 / 4, y, x + width + 7, y, shade);
        DrawLine(x - 7, y, peak, y - roofHeight, snow);
        DrawLine(x - 4, y - 2, peak, y - roofHeight + 3, Fade(snow, .4f));
        DrawRectangle(x - 4, y, width + 8, 5, trim);
        DrawRectangle(x - 5, y - 2, width + 10, 3, snow);
        for (int row = 0; row < (height - 20) / 36; row++) {
            int wy = y + 21 + row * 36;
            int wx = x + width / 2 - 5;
            DrawCircle(wx + 4, wy, 6, shade);
            DrawRectangle(wx - 2, wy, 12, 16, shade);
            Color lamp = tint(224, 177, 87, 60, 79, 91, light);
            DrawCircle(wx + 4, wy, 3, lamp);
            DrawRectangle(wx + 1, wy, 6, 12, lamp);
            DrawLine(wx + 4, wy - 3, wx + 4, wy + 12, shade);
            DrawLine(wx + 1, wy + 5, wx + 7, wy + 5, shade);
            DrawRectangle(wx - 3, wy + 14, 14, 2, trim);
        }
    }

    private static void masonry(int x, int y, int width, int height, Color face, Color mortar) {
        DrawRectangle(x, y, width, height, face);
        for (int row = 1; row * 13 < height; row++) {
            int lineY = y + row * 13;
            DrawLine(x + 1, lineY, x + width - 1, lineY, Fade(mortar, .22f));
            for (int bx = 10 + row % 2 * 12; bx < width; bx += 25)
                DrawLine(x + bx, lineY - 12, x + bx, lineY, Fade(mortar, .18f));
        }
    }

    private static void banner(int x, int y, int seed, Color pole) {
        DrawLine(x, y, x, y - 27, pole);
        float flutter = (float) Math.sin(GetTime() * 1.7 + seed) * 3;
        triangle(x, y - 26, x + 20, y - 23 + flutter, x, y - 12, Fade(Menu.TEAL, .8f));
    }

    /** Keep all scenery triangles front-facing, regardless of their caller's vertex order. */
    private static void triangle(
            float ax, float ay, float bx, float by, float cx, float cy, Color color) {
        if ((bx - ax) * (cy - ay) - (by - ay) * (cx - ax) > 0) {
            float swapX = bx, swapY = by;
            bx = cx;
            by = cy;
            cx = swapX;
            cy = swapY;
        }
        DrawTriangle(
                new Vector2().x(ax).y(ay),
                new Vector2().x(bx).y(by),
                new Vector2().x(cx).y(cy),
                color);
    }

    private static Color tint(int r, int g, int b, int dayR, int dayG, int dayB, float light) {
        return Menu.color(
                Math.round(r + (dayR - r) * light),
                Math.round(g + (dayG - g) * light),
                Math.round(b + (dayB - b) * light));
    }
}
