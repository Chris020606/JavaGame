import Entities.Enemy;
import java.util.HashSet;
import java.util.Set;

/** Player projectiles keep their own hit set so piercing bolts hit each enemy once. */
public final class HeroShot {
    public float x,z,previousX,previousZ,remaining;
    public final float vx,vz,damage;
    public final boolean magic;
    private final Set<Enemy> struck=new HashSet<>();
    public HeroShot(float x,float z,float facing,Combat.Attack attack,int damage,boolean magic) {
        this.x=previousX=x;this.z=previousZ=z;this.magic=magic;
        float speed=magic?12:20;
        vx=(float)Math.sin(Math.toRadians(facing))*speed;vz=(float)Math.cos(Math.toRadians(facing))*speed;
        remaining=attack.range()/speed;this.damage=Math.round(damage*attack.multiplier());
    }
    public void update(float dt) {
        float step=Math.min(dt,Math.max(0,remaining));previousX=x;previousZ=z;x+=vx*step;z+=vz*step;remaining-=dt;
    }
    public boolean hit(Enemy enemy) {
        if (enemy.health<=0 || struck.contains(enemy)) return false;
        float dx=x-previousX,dz=z-previousZ,length=dx*dx+dz*dz;
        float t=length==0?0:Math.max(0,Math.min(1,((enemy.x-previousX)*dx+(enemy.z-previousZ)*dz)/length));
        if (Math.hypot(previousX+dx*t-enemy.x,previousZ+dz*t-enemy.z)>.6f) return false;
        struck.add(enemy);return true;
    }
}
