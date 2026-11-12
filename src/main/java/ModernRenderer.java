import static com.raylib.Colors.WHITE;
import static com.raylib.Raylib.*;

import assets.AssetPaths;

import org.bytedeco.javacpp.FloatPointer;

/** Shadow atlas and scene-only post-processing. The HUD is drawn afterwards at native DPI. */
public final class ModernRenderer implements AutoCloseable {
    public final Shader shadowShader =
            LoadShader(
                    AssetPaths.path("resources/outpost/daylight.vs"),
                    AssetPaths.path("resources/outpost/shadow.fs"));
    private final Shader post = LoadShader(null, AssetPaths.path("resources/outpost/post.fs"));
    private final Camera3D sun =
            new Camera3D()
                    ._position(new Vector3().x(-35).y(55).z(28))
                    .target(new Vector3())
                    .up(new Vector3().y(1))
                    .projection(CAMERA_ORTHOGRAPHIC)
                    .fovy(78);
    private final int shadowTime = GetShaderLocation(shadowShader, "uTime"),
            postResolution = GetShaderLocation(post, "resolution"),
            postBloom = GetShaderLocation(post, "bloomStrength");
    private RenderTexture shadow, scene;
    private Matrix sunVP;
    private int width, height, shadowSize;
    private boolean high = true;
    private final FloatPointer values = new FloatPointer(4);
    private final Camera2D screen = new Camera2D().zoom(1);
    private final Rectangle source = new Rectangle();
    private final Rectangle destination = new Rectangle();
    private final Vector2 origin = new Vector2();

    public ModernRenderer() {
        if (!IsShaderValid(shadowShader) || !IsShaderValid(post))
            throw new IllegalStateException("Modern rendering shaders did not load");
        quality(true);
    }

    public void quality(boolean high) {
        this.high = high;
        int size = high ? 2048 : 1024;
        if (shadowSize == size) return;
        if (shadow != null) UnloadRenderTexture(shadow);
        shadow = LoadRenderTexture(size, size);
        shadowSize = size;
        if (!IsRenderTextureValid(shadow))
            throw new IllegalStateException("Shadow atlas unavailable");
        SetTextureFilter(shadow.texture(), TEXTURE_FILTER_POINT);
    }

    public void beginShadow(float time, DayNightCycle.Frame light) {
        sun._position().x(light.lightX() * 75).y(light.lightY() * 75).z(light.lightZ() * 75);
        BeginTextureMode(shadow);
        ClearBackground(WHITE);
        BeginMode3D(sun);
        sunVP = MatrixMultiply(rlGetMatrixModelview(), rlGetMatrixProjection());
        values.put(0, time);
        SetShaderValue(shadowShader, shadowTime, values, SHADER_UNIFORM_FLOAT);
    }

    public void endShadow() {
        EndMode3D();
        EndTextureMode();
    }

    public Matrix sunVP() {
        return sunVP;
    }

    public Texture shadowTexture() {
        return shadow.texture();
    }

    public int shadowSize() {
        return shadowSize;
    }

    public void beginScene() {
        int w = Math.max(1, Math.min(GetRenderWidth(), high ? 1920 : 1440)),
                h =
                        Math.max(
                                1,
                                Math.round(
                                        (float) GetRenderHeight()
                                                * w
                                                / Math.max(1, GetRenderWidth())));
        if (scene == null || w != width || h != height) {
            if (scene != null) UnloadRenderTexture(scene);
            width = w;
            height = h;
            scene = LoadRenderTexture(w, h);
            if (!IsRenderTextureValid(scene))
                throw new IllegalStateException("Scene framebuffer unavailable");
            SetTextureFilter(scene.texture(), TEXTURE_FILTER_BILINEAR);
        }
        BeginTextureMode(scene);
        // The scene pass always paints its own sky/gradient. A dark fallback prevents a
        // partially-rendered frame from presenting as a full white screen.
        ClearBackground(Menu.INK);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void present() {
        EndTextureMode();
        values.put(0, width).put(1, height);
        SetShaderValue(post, postResolution, values, SHADER_UNIFORM_VEC2);
        values.put(0, high ? .16f : .06f);
        SetShaderValue(post, postBloom, values, SHADER_UNIFORM_FLOAT);
        screen.zoom((float) GetRenderWidth() / Math.max(1, GetScreenWidth()));
        source.x(0).y(0).width(width).height(-height);
        destination.x(0).y(0).width(GetScreenWidth()).height(GetScreenHeight());
        BeginMode2D(screen);
        BeginShaderMode(post);
        DrawTexturePro(scene.texture(), source, destination, origin, 0, WHITE);
        EndShaderMode();
        EndMode2D();
    }

    boolean shadowAtlasValid() {
        Image image = LoadImageFromTexture(shadow.texture());
        try {
            Vector3 q = Vector3Transform(new Vector3(), sunVP);
            int x = Math.max(0, Math.min(shadowSize - 1, (int) ((q.x() * .5f + .5f) * shadowSize)));
            int y = Math.max(0, Math.min(shadowSize - 1, (int) ((q.y() * .5f + .5f) * shadowSize)));
            Color c = GetImageColor(image, x, y);
            float stored =
                    Byte.toUnsignedInt(c.r()) / 255f
                            + Byte.toUnsignedInt(c.g()) / 65025f
                            + Byte.toUnsignedInt(c.b()) / 16581375f;
            return stored < q.z() * .5f + .5f - .0001f;
        } finally {
            UnloadImage(image);
        }
    }

    @Override
    public void close() {
        if (scene != null) UnloadRenderTexture(scene);
        if (shadow != null) UnloadRenderTexture(shadow);
        UnloadShader(shadowShader);
        UnloadShader(post);
        values.close();
    }
}
