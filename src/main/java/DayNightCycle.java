/** Eight-minute solar cycle. It advances with gameplay and is persisted with the run. */
public final class DayNightCycle {
    public static final float PERIOD=480;
    private float phase=.36f;
    public record Rgb(float r,float g,float b) {
        public Rgb mix(Rgb other,float t) { return new Rgb(r+(other.r-r)*t,g+(other.g-g)*t,b+(other.b-b)*t); }
    }
    public record Frame(float phase,float sunX,float sunY,float sunZ,float lightX,float lightY,float lightZ,float daylight,Rgb light,Rgb top,Rgb horizon) {
        public String clock() { int minutes=(int)(phase*1440)%1440;return String.format("%02d:%02d",minutes/60,minutes%60); }
        public String label() { return daylight>.85f?"DAY":daylight<.15f?"NIGHT":sunX<0?"DAWN":"DUSK"; }
    }
    public static float initial(int chapter) { return switch(chapter) {case 1 -> .72f;case 2 -> .40f;case 3 -> .90f;default -> .36f;}; }
    public float phase() { return phase; }
    public void phase(float value) { if(!Float.isFinite(value)||value<0||value>=1)throw new IllegalArgumentException("Invalid day phase");phase=value; }
    public void update(float dt) { if(Float.isFinite(dt) && dt>0)phase=(phase+dt/PERIOD)%1; }
    private static float smooth(float a,float b,float x) { float t=Math.max(0,Math.min(1,(x-a)/(b-a)));return t*t*(3-2*t); }
    public Frame frame() { return sample(phase); }
    public static Frame sample(float phase) {
        double a=(phase-.25)*Math.PI*2;float x=-(float)Math.cos(a),y=(float)Math.sin(a),z=-.35f;
        float length=(float)Math.sqrt(x*x+y*y+z*z);x/=length;y/=length;z/=length;
        float day=smooth(-.13f,.18f,y),warm=1-smooth(.05f,.65f,Math.max(0,y));
        Rgb sunlight=new Rgb(3.35f,3.05f,2.6f).mix(new Rgb(3.8f,1.65f,.65f),warm);
        float strength=.3f+.7f*Math.max(0,y);sunlight=new Rgb(sunlight.r*strength,sunlight.g*strength,sunlight.b*strength);
        Rgb light=new Rgb(.32f,.44f,.75f).mix(sunlight,day);
        Rgb top=new Rgb(.025f,.045f,.11f).mix(new Rgb(.22f,.48f,.70f),day);
        Rgb horizon=new Rgb(.09f,.13f,.23f).mix(new Rgb(.68f,.79f,.80f),day);
        float twilight=(1-Math.abs(day*2-1))*.65f;horizon=horizon.mix(new Rgb(.9f,.39f,.20f),twilight);
        float sign=y>=0?1:-1;
        return new Frame(phase,x,y,z,x*sign,Math.max(.025f,y*sign),z*sign,day,light,top,horizon);
    }
}
