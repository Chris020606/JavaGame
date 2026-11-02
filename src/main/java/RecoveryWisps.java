import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Small battlefield recoveries; saved with the run and frozen whenever gameplay pauses. */
public final class RecoveryWisps {
    public static final float HEALTH=18,STAMINA=26,LIFETIME=30,RADIUS=1.7f;
    public record Wisp(float x,float z,float life) {}
    private final ArrayList<Wisp> wisps=new ArrayList<>();
    private final List<Wisp> view=java.util.Collections.unmodifiableList(wisps);
    public List<Wisp> active() { return view; }
    public void enemyDefeated(float x,float z,int kills,boolean elite) {
        if(!elite && kills%5!=0)return;
        if(wisps.size()==12)wisps.remove(0);
        wisps.add(new Wisp(x,z,LIFETIME));
    }
    public void update(float dt) {
        for(int i=wisps.size()-1;i>=0;i--) {
            Wisp w=wisps.get(i);float life=w.life()-dt;
            if(life<=0)wisps.remove(i);else wisps.set(i,new Wisp(w.x(),w.z(),life));
        }
    }
    public boolean collect(float x,float z,boolean needsRecovery) {
        if(!needsRecovery)return false;
        for(int i=0;i<wisps.size();i++)if(Math.hypot(x-wisps.get(i).x(),z-wisps.get(i).z())<RADIUS) { wisps.remove(i);return true; }
        return false;
    }
    public void clear() { wisps.clear(); }
    public void store(Properties p) {
        p.setProperty("wisps.count",wisps.size()+"");
        for(int i=0;i<wisps.size();i++) { Wisp w=wisps.get(i);String k="wisps."+i+".";p.setProperty(k+"x",w.x()+"");p.setProperty(k+"z",w.z()+"");p.setProperty(k+"life",w.life()+""); }
    }
    public static RecoveryWisps restore(Properties p) {
        RecoveryWisps result=new RecoveryWisps();
        int count=p.containsKey("wisps.count")?SaveGame.integer(p,"wisps.count",0,12):0;
        for(int i=0;i<count;i++) { String k="wisps."+i+".";result.wisps.add(new Wisp(SaveGame.number(p,k+"x",-22,22),SaveGame.number(p,k+"z",-22,22),SaveGame.number(p,k+"life",0,LIFETIME))); }
        return result;
    }
}
