import java.util.Properties;

/** Optional timed encounters. Their simulation pauses with the rest of the game. */
public final class FieldEvent {
    public enum Kind { SUPPLIES, SURVIVOR, BEACON }
    public enum Status { NONE, WAITING, ACTIVE, COMPLETE, FAILED }
    private Kind kind=Kind.SUPPLIES;
    private Status status=Status.NONE;
    private int wave;
    private float x,z,time,progress,health=100,facing;
    private boolean rewarded;
    public Kind kind() { return kind; } public Status status() { return status; }
    public float x() { return x; } public float z() { return z; } public float facing() { return facing; }
    public float time() { return time; } public float health() { return health; }
    public float progress() { return progress; }
    public boolean visible() { return status==Status.WAITING || status==Status.ACTIVE; }
    public boolean vulnerable() { return status==Status.ACTIVE && kind!=Kind.SUPPLIES; }
    public String title() { return switch(kind) { case SUPPLIES -> "LOST SUPPLIES";case SURVIVOR -> "STRANDED SURVIVOR";case BEACON -> "WAKE THE WAYSTONE"; }; }
    public String instruction() { return switch(kind) {
        case SUPPLIES -> "Hold interact nearby for 2 seconds to recover the cache.";
        case SURVIVOR -> status==Status.WAITING?"Find the survivor and interact. Lead them to the sanctuary.":"Escort the survivor home. Stay close and protect them.";
        case BEACON -> status==Status.WAITING?"Interact nearby to awaken it. Defend it for 14 seconds.":"Protect the waystone until the ember returns.";
    }; }
    public void begin(int wave) {
        if(wave==this.wave)return;
        this.wave=wave;rewarded=false;progress=0;health=100;facing=0;
        if(Campaign.chapter(wave).bossOnly() || wave<1) { status=Status.NONE;return; }
        kind=switch(wave%3) { case 1 -> Kind.SUPPLIES;case 2 -> Kind.SURVIVOR;default -> Kind.BEACON; };
        x=wave%2==0?-12:12;z=wave>=6?-10:8;time=kind==Kind.SURVIVOR?65:45;status=Status.WAITING;
    }
    public void update(float dt,float px,float pz,boolean interact) {
        if(!visible())return;
        time=Math.max(0,time-dt);if(time<=0 || health<=0) { status=Status.FAILED;return; }
        float d=(float)Math.hypot(px-x,pz-z);
        if(kind==Kind.SUPPLIES) {
            progress=d<2.5f && interact?Math.min(2,progress+dt):0;
            if(progress>=2)status=Status.COMPLETE;
        } else {
            if(status==Status.WAITING && d<3 && interact) { status=Status.ACTIVE;progress=0; }
            if(status!=Status.ACTIVE)return;
            if(kind==Kind.BEACON) { progress+=dt;if(progress>=14)status=Status.COMPLETE; }
            else {
                if(d>2 && d<11) {
                    facing=(float)Math.toDegrees(Math.atan2(px-x,pz-z));
                    float step=Math.min(d-2,2.9f*dt);x+=(px-x)/d*step;z+=(pz-z)/d*step;
                }
                progress=Math.max(0,1-(float)Math.hypot(x,z)/17);
                if(Math.hypot(x,z)<5.5f)status=Status.COMPLETE;
            }
        }
    }
    public void interrupt() { if(kind==Kind.SUPPLIES)progress=0; }
    public void damage(float amount) { if(vulnerable()) { health=Math.max(0,health-Math.max(0,amount));if(health<=0)status=Status.FAILED; } }
    public boolean claim() { if(status!=Status.COMPLETE || rewarded)return false;rewarded=true;return true; }
    public void store(Properties p) {
        p.setProperty("event.wave",wave+"");p.setProperty("event.kind",kind.name());p.setProperty("event.status",status.name());
        p.setProperty("event.x",x+"");p.setProperty("event.z",z+"");p.setProperty("event.time",time+"");
        p.setProperty("event.progress",progress+"");p.setProperty("event.health",health+"");p.setProperty("event.facing",facing+"");p.setProperty("event.rewarded",rewarded+"");
    }
    public static FieldEvent restore(Properties p) {
        FieldEvent e=new FieldEvent();if(!p.containsKey("event.wave"))return e;
        e.wave=SaveGame.integer(p,"event.wave",0,10);e.kind=Kind.valueOf(p.getProperty("event.kind"));e.status=Status.valueOf(p.getProperty("event.status"));
        e.x=SaveGame.number(p,"event.x",-22,22);e.z=SaveGame.number(p,"event.z",-22,22);e.time=SaveGame.number(p,"event.time",0,65);
        e.progress=SaveGame.number(p,"event.progress",0,14.1f);e.health=SaveGame.number(p,"event.health",0,100);e.facing=SaveGame.number(p,"event.facing",-360,360);
        String paid=p.getProperty("event.rewarded");if(!"true".equals(paid) && !"false".equals(paid))throw new IllegalArgumentException("Invalid event reward");
        e.rewarded=Boolean.parseBoolean(paid);if(e.rewarded && e.status!=Status.COMPLETE)throw new IllegalArgumentException("Inconsistent event reward");
        return e;
    }
}
