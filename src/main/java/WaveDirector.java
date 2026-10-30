import java.util.Random;

/** One announced arrival at a time; clearing the field never skips queued raiders. */
public final class WaveDirector {
    private final Random random;
    private Entities.Difficulty difficulty=Entities.Difficulty.NORMAL;
    public void difficulty(Entities.Difficulty difficulty) { this.difficulty=difficulty; }
    private int wave, remaining, total;
    private float rest=5, arrival, x, z;
    public record Spawn(float x,float z,int wave,int variant) {}
    public WaveDirector(Random random) { this.random=random; }
    public void reset() { wave=remaining=total=0; rest=5; arrival=0; }
    public Spawn update(float dt, int living) {
        if (remaining == 0) {
            if (living > 0) return null;
            if (levelCleared(living) || wave>=10) return null;
            rest-=dt;
            if (rest>0) return null;
            rest=0; wave++; total=remaining=difficulty.count(Campaign.enemies(wave),Campaign.chapter(wave).bossOnly()); schedule();
            return null;
        }
        arrival-=dt;
        if (arrival>0) return null;
        Spawn result=new Spawn(x,z,wave,WaveFormation.variant(wave,total-remaining));
        remaining--;
        if (remaining>0) schedule(); else rest=8;
        return result;
    }
    public boolean levelCleared(int living) { return wave>0 && Campaign.boundary(wave) && (total>0 || wave>=10) && remaining==0 && living==0; }
    public void advanceLevel() {
        if(wave>=10) return;
        if(!levelCleared(0)) throw new IllegalStateException("Level is not clear");
        total=0;rest=5;
    }
    public Campaign.Chapter chapter() { return Campaign.chapter(total==0?wave+1:wave); }
    public int chapterIndex() { return Campaign.index(total==0?wave+1:wave); }
    private void schedule() {
        // Four approach routes, with lateral and distance variation.
        int gate=Campaign.index(wave)==2?(random.nextBoolean()?0:2):random.nextInt(4);
        double angle=gate*Math.PI/2+(random.nextFloat()-.5)*.55;
        float radius=20+random.nextFloat()*2;
        x=(float)Math.sin(angle)*radius; z=(float)Math.cos(angle)*radius;
        arrival=(.9f+random.nextFloat()*1.4f)*difficulty.cadence;
        if (remaining%3==0) arrival+=1.2f;
    }
    public record State(int wave,int remaining,int total,float rest,float arrival,float x,float z) {}
    public State snapshot() { return new State(wave,remaining,total,rest,arrival,x,z); }
    public void restore(State s) { wave=s.wave;remaining=s.remaining;total=s.total;rest=s.rest;arrival=s.arrival;x=s.x;z=s.z; }
    public boolean bossWave() { return wave>0 && wave%5==0; }
    public boolean bossIncoming() { return bossWave() && remaining==total; }
    public int wave() { return wave; }
    public int remaining() { return remaining; }
    public int total() { return total; }
    public float rest() { return rest; }
    public boolean warning() { return remaining>0 && arrival<=1; }
    public float x() { return x; }
    public float z() { return z; }
}
