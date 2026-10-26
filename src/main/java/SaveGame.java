import Entities.Enemy;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;

/** Versioned, validated data only: never Java object deserialization. */
public final class SaveGame {
    public record Snapshot(String name,float x,float z,float facing,Progression progression,WaveDirector.State wave,List<Enemy> enemies) {}
    public static int integer(Properties p,String key,int min,int max) {
        int value=Integer.parseInt(p.getProperty(key));
        if (value<min||value>max) throw new IllegalArgumentException("Invalid "+key);
        return value;
    }
    public static float number(Properties p,String key,float min,float max) {
        float value=Float.parseFloat(p.getProperty(key));
        if (!Float.isFinite(value)||value<min||value>max) throw new IllegalArgumentException("Invalid "+key);
        return value;
    }
    public static void write(Path file,Snapshot s) throws IOException {
        Properties p=new Properties();
        p.setProperty("version","2"); p.setProperty("name",Settings.cleanName(s.name));
        p.setProperty("player.x",s.x+""); p.setProperty("player.z",s.z+""); p.setProperty("player.facing",s.facing+"");
        s.progression.store(p);
        WaveDirector.State w=s.wave;
        p.setProperty("wave",w.wave()+""); p.setProperty("remaining",w.remaining()+""); p.setProperty("total",w.total()+"");
        p.setProperty("rest",w.rest()+""); p.setProperty("arrival",w.arrival()+"");
        p.setProperty("spawn.x",w.x()+""); p.setProperty("spawn.z",w.z()+""); p.setProperty("enemies",s.enemies.size()+"");
        for (int i=0;i<s.enemies.size();i++) {
            Enemy e=s.enemies.get(i); String prefix="enemy."+i+".";
            p.setProperty(prefix+"x",e.x+""); p.setProperty(prefix+"z",e.z+""); p.setProperty(prefix+"health",e.health+"");
            p.setProperty(prefix+"variant",e.variant+""); p.setProperty(prefix+"personality",e.personality+"");
            p.setProperty(prefix+"facing",e.facing+""); p.setProperty(prefix+"cooldown",e.cooldown+"");
        }
        // Validate the complete snapshot before replacing an existing file.
        try { decode(p); } catch (RuntimeException e) { throw new IOException("Cannot save invalid game state",e); }
        AppData.write(file,p);
    }
    public static Snapshot read(Path file) throws IOException {
        try { return decode(AppData.read(file)); }
        catch (RuntimeException e) { throw new IOException("Save is damaged or uses an unsupported version",e); }
    }
    private static Snapshot decode(Properties p) {
        boolean legacy="1".equals(p.getProperty("version"));
        if (!legacy && !"2".equals(p.getProperty("version"))) throw new IllegalArgumentException("Unsupported save version");
        if (!legacy) { integer(p,"weaponUnlocked",0,3); integer(p,"weaponTier",0,3); }
        Progression progress=Progression.restore(p);
        int wave=integer(p,"wave",0,10000),total=integer(p,"total",0,36),remaining=integer(p,"remaining",0,total);
        int count=integer(p,"enemies",0,36);
        if (count+remaining>total || wave==0&&(count>0||total>0)) throw new IllegalArgumentException("Inconsistent wave");
        WaveDirector.State director=new WaveDirector.State(wave,remaining,total,number(p,"rest",0,8),number(p,"arrival",-.1f,4),number(p,"spawn.x",-23,23),number(p,"spawn.z",-23,23));
        List<Enemy> enemies=new ArrayList<>();
        for (int i=0;i<count;i++) {
            String k="enemy."+i+".";
            Enemy e=new Enemy(number(p,k+"x",-30,30),number(p,k+"z",-30,30),wave,integer(p,k+"variant",0,legacy?4:5),number(p,k+"personality",0,1));
            e.health=number(p,k+"health",.001f,e.maxHealth); e.facing=number(p,k+"facing",-360,360);
            // Resume with a short grace period; interrupted windups/projectiles are cancelled.
            e.cooldown=Math.max(.6f,number(p,k+"cooldown",0,5)); enemies.add(e);
        }
        return new Snapshot(Settings.cleanName(p.getProperty("name")),number(p,"player.x",-22,22),number(p,"player.z",-22,22),number(p,"player.facing",-360,360),progress,director,List.copyOf(enemies));
    }
}
