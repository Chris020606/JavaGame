package assets;
import java.nio.file.*;

/** Locate external glTF/texture bundles independently of the launcher's working directory. */
public final class AssetPaths {
    private static final Path ROOT=locate();
    private static Path locate() {
        String override=System.getProperty("outpost.assets");
        if (override!=null) return Path.of(override).toAbsolutePath();
        try {
            Path code=Path.of(AssetPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path parent=Files.isDirectory(code)?code:code.getParent();
            for (Path p:new Path[]{parent.resolve("resources"),parent.resolve("../resources"),Path.of("resources")})
                if (Files.isRegularFile(p.resolve("knight.glb"))) return p.toAbsolutePath().normalize();
        } catch (Exception e) { throw new IllegalStateException("Cannot locate game resources",e); }
        throw new IllegalStateException("Missing resources folder next to the game. Extract the entire download first.");
    }
    public static String path(String relative) { return ROOT.resolve(relative.replaceFirst("^resources/","")).toString(); }
}
