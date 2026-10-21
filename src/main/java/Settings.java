import Config.Controls;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

public final class Settings {
    public final Controls controls=new Controls();
    public float volume=.6f, sensitivity=.15f;
    public boolean muted, fullscreen;
    public String playerName="Warden", warning="";
    public void load(Path file) {
        if (!Files.exists(file)) return;
        try {
            Properties p=AppData.read(file);
            if (!p.getProperty("version","").equals("1")) throw new IllegalArgumentException("Unsupported settings");
            float v=Float.parseFloat(p.getProperty("volume","")),s=Float.parseFloat(p.getProperty("sensitivity",""));
            if (!Float.isFinite(v)||v<0||v>1||!Float.isFinite(s)||s<.05f||s>.5f) throw new IllegalArgumentException("Invalid settings");
            controls.restore(p); volume=v; sensitivity=s;
            muted=Boolean.parseBoolean(p.getProperty("muted")); fullscreen=Boolean.parseBoolean(p.getProperty("fullscreen"));
            playerName=cleanName(p.getProperty("name","Warden"));
        } catch (IOException|IllegalArgumentException e) { warning="Settings could not be read; using defaults."; }
    }
    public void save(Path file) throws IOException {
        Properties p=new Properties(); p.setProperty("version","1");
        p.setProperty("volume",volume+""); p.setProperty("sensitivity",sensitivity+"");
        p.setProperty("muted",muted+""); p.setProperty("fullscreen",fullscreen+"");
        p.setProperty("name",cleanName(playerName)); controls.store(p); AppData.write(file,p);
    }
    public static String cleanName(String name) {
        String clean=name==null?"":name.replaceAll("[^a-zA-Z0-9 _-]","").trim();
        return clean.isBlank()?"Warden":clean.substring(0,Math.min(16,clean.length()));
    }
}
