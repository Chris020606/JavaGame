import Entities.Enemy;
import Entities.Player;
import java.util.List;
import static com.raylib.Raylib.*;

/** World-projected aiming feedback; it never changes projectile direction or collision rules. */
public final class CombatCueScreen {
    private final Vector3 start=new Vector3(),end=new Vector3();
    public void draw(Player player,HeroClass hero,int chapter,List<Enemy> enemies,Camera3D camera) {
        if(hero==HeroClass.WARRIOR)return;
        float x=player.getPosition().x(),z=player.getPosition().z(),range=hero.attack(1,false).range();
        double angle=Math.toRadians(player.getRotation().y());float tx=x+(float)Math.sin(angle)*range,tz=z+(float)Math.cos(angle)*range;
        float contact=ArenaNavigation.projectileFraction(chapter,x,z,tx,tz);
        Vector2 a=GetWorldToScreen(start.x(x).y(.18f).z(z),camera),b=GetWorldToScreen(end.x(x+(tx-x)*contact).y(.18f).z(z+(tz-z)*contact),camera);
        Color color=contact<1?Menu.RED:hero==HeroClass.MAGE?Menu.TEAL:Menu.GOLD;
        for(int i=1;i<=12;i++) { float t=i/12f;DrawCircle((int)(a.x()+(b.x()-a.x())*t),(int)(a.y()+(b.y()-a.y())*t),i==12?4:1.5f,Fade(color,.55f)); }
        Enemy target=RangedAim.target(x,z,player.getRotation().y(),range,enemies,e->ArenaNavigation.projectileFraction(chapter,x,z,e.x,e.z)>=1);
        if(target!=null) { Vector2 p=GetWorldToScreen(end.x(target.x).y(2.4f).z(target.z),camera);DrawCircleLines((int)p.x(),(int)p.y(),12,color);Menu.center("IN RANGE",(int)p.x(),(int)p.y()-30,10,color); }
    }
}
