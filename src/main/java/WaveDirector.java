import java.util.Random;

/** One announced arrival at a time; clearing the field never skips queued raiders. */
public final class WaveDirector {
    private final Random random;
    private int wave, remaining, total;
    private float rest=5, arrival, x, z;
    public record Spawn(float x,float z,int wave,int variant) {}
    public WaveDirector(Random random) { this.random=random; }
    public void reset() { wave=remaining=total=0; rest=5; arrival=0; }
    public Spawn update(float dt, int living) {
        if (remaining == 0) {
            if (living > 0) return null;
            rest-=dt;
            if (rest>0) return null;
            rest=0; wave++; total=remaining=Math.min(36,4+wave*3); schedule();
            return null;
        }
        arrival-=dt;
        if (arrival>0) return null;
        Spawn result=new Spawn(x,z,wave,random.nextInt(wave>=3?5:wave>=2?4:3));
        remaining--;
        if (remaining>0) schedule(); else rest=8;
        return result;
    }
    private void schedule() {
        // Four approach routes, with lateral and distance variation.
        int gate=random.nextInt(4);
        double angle=gate*Math.PI/2+(random.nextFloat()-.5)*.55;
        float radius=20+random.nextFloat()*2;
        x=(float)Math.sin(angle)*radius; z=(float)Math.cos(angle)*radius;
        arrival=.9f+random.nextFloat()*1.4f;
        if (remaining%3==0) arrival+=1.2f;
    }
    public record State(int wave,int remaining,int total,float rest,float arrival,float x,float z) {}
    public State snapshot() { return new State(wave,remaining,total,rest,arrival,x,z); }
    public void restore(State s) { wave=s.wave;remaining=s.remaining;total=s.total;rest=s.rest;arrival=s.arrival;x=s.x;z=s.z; }
    public int wave() { return wave; }
    public int remaining() { return remaining; }
    public int total() { return total; }
    public float rest() { return rest; }
    public boolean warning() { return remaining>0 && arrival<=1; }
    public float x() { return x; }
    public float z() { return z; }
}
