import static com.raylib.Colors.WHITE;
import static com.raylib.Raylib.*;

import Entities.Player;
import Entities.PlayerAnimation;

import assets.AssetPaths;

/** An independent animated character stage; selecting a class never changes the active hero. */
public final class HeroPortrait implements AutoCloseable {
    private final Player[] heroes = new Player[3];
    private final Progression[] kits = new Progression[HeroClass.values().length];
    private final RenderTexture target = LoadRenderTexture(600, 800);
    private final Shader shader =
            LoadShader(
                    AssetPaths.path("resources/outpost/daylight.vs"),
                    AssetPaths.path("resources/outpost/preview.fs"));
    private final Camera3D camera =
            new Camera3D()
                    ._position(new Vector3().x(3.5f).y(2.8f).z(6))
                    .target(new Vector3().y(1.3f))
                    .up(new Vector3().y(1))
                    .fovy(34)
                    .projection(CAMERA_PERSPECTIVE);

    public void prepare(WorldAssets assets, HeroClass hero) {
        Player player = heroes[hero.ordinal()];
        if (player == null) {
            player = new Player();
            String file =
                    hero == HeroClass.WARRIOR
                            ? "resources/outpost/hero_warrior.glb"
                            : hero == HeroClass.ARCHER
                                    ? "resources/outpost/hero_archer.glb"
                                    : "resources/outpost/hero_mage.glb";
            player.setModel(LoadModel(AssetPaths.path(file)));
            player.loadAnimations(AssetPaths.path(file));
            player.setWeaponStyle(hero.ordinal());
            player.setAnimationFPS(60);
            player.setAnimation(
                    hero == HeroClass.ARCHER
                            ? PlayerAnimation.BOW_IDLE
                            : PlayerAnimation.MELEE_IDLE);
            kits[hero.ordinal()] = new Progression(hero);
            heroes[hero.ordinal()] = player;
        }
        float time = (float) GetTime();
        // A three-quarter view keeps the bow limbs and staff clear of the face.
        player.getRotation()
                .y((hero == HeroClass.ARCHER ? -17 : 52) + (float) Math.sin(time * .5f) * 6);
        player.updateAnimation(Math.max(0, Math.min(.08f, GetFrameTime())));
        BeginTextureMode(target);
        Color accent = accent(hero);
        ClearBackground(Menu.INK);
        DrawRectangleGradientV(0, 0, 600, 800, Menu.color(39, 52, 68), Menu.INK);
        // A stitched class standard hangs behind the plinth.
        int left = 158, right = 442, hem = 470;
        DrawRectangleGradientV(left, 0, right - left, hem, Fade(accent, .44f), Fade(accent, .16f));
        DrawTriangle(
                new Vector2().x(left).y(hem),
                new Vector2().x(300).y(hem + 90),
                new Vector2().x(right).y(hem),
                Fade(accent, .16f));
        DrawLine(left + 10, 0, left + 10, hem - 3, Fade(accent, .64f));
        DrawLine(right - 10, 0, right - 10, hem - 3, Fade(accent, .64f));
        Menu.icon(symbol(hero), 266, 52, 68, Fade(accent, .8f));
        BeginMode3D(camera);
        DrawCylinder(new Vector3().y(-.16f), 1.15f, 1.22f, .12f, 24, Menu.color(53, 62, 74));
        DrawCylinder(new Vector3().y(-.055f), 1.12f, 1.14f, .038f, 24, accent);
        DrawCylinder(new Vector3().y(-.027f), 1.08f, 1.12f, .04f, 24, Menu.color(69, 79, 86));
        assets.previewPass(shader);
        assets.preparePlayer(player);
        assets.drawPlayerBody(player, kits[hero.ordinal()], WHITE);
        assets.drawPlayerWeapon(player, hero.ordinal(), 0);
        assets.endPreview();
        EndMode3D();
        EndTextureMode();
    }

    static Color accent(HeroClass hero) {
        return switch (hero) {
            case WARRIOR -> Menu.GOLD;
            case ARCHER -> Menu.TEAL;
            case MAGE -> Menu.color(187, 158, 238);
        };
    }

    static String symbol(HeroClass hero) {
        return switch (hero) {
            case WARRIOR -> "sword";
            case ARCHER -> "bow";
            case MAGE -> "staff";
        };
    }

    public void draw(int x, int y, int w, int h) {
        DrawTexturePro(
                target.texture(),
                new Rectangle().width(600).height(-800),
                new Rectangle().x(x).y(y).width(w).height(h),
                new Vector2(),
                0,
                WHITE);
    }

    @Override
    public void close() {
        for (Player p : heroes) if (p != null) p.dispose();
        UnloadRenderTexture(target);
        UnloadShader(shader);
    }
}
