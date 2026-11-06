import Entities.Enemy;
import static com.raylib.Raylib.*;

/** World labels and nearby recovery prompts, kept out of combat simulation. */
public final class EnemyHealthScreen {
    private static Vector3 v(float x,float y,float z) { return new Vector3().x(x).y(y).z(z); }
    public void draw(Progression progress,Entities.Player player,java.util.List<Enemy> enemies,Camera3D camera,int wave,boolean training) {
        RecoveryWisps.Wisp nearestWisp=null;double nearestDistance=10;
        for(var wisp:progress.recoveryWisps().active()) {
            double distance=Math.hypot(wisp.x()-player.getPosition().x(),wisp.z()-player.getPosition().z());
            if(distance<nearestDistance) { nearestDistance=distance;nearestWisp=wisp; }
        }
        if(nearestWisp!=null) {
            Vector2 point=GetWorldToScreen(v(nearestWisp.x(),1.6f,nearestWisp.z()),camera);
            int x=(int)point.x(),y=(int)point.y();
            Menu.panel(x-84,y-5,168,36);Menu.center("RECOVERY WISP",x,y,11,Menu.TEAL);
            Menu.center("18 HP + 26 "+progress.resourceLabel(),x,y+15,10,Menu.TEXT);
        }
        for (Enemy enemy : enemies) {
            Vector2 screen = GetWorldToScreen(v(enemy.x, enemy.boss()?(enemy.king?6.0f:5.2f):2.1f, enemy.z), camera);
            Menu.bar((int)screen.x()-22, (int)screen.y(), 44, enemy.health, enemy.maxHealth, enemy.boss()||enemy.elite||enemy.variant==2?Menu.GOLD:Menu.RED);
            String label=training?enemy.attackTime>.25f?"PARRY THE GOLD STRIKE":"PRACTICE PARTNER":enemy.attackTime>.25f?"! ATTACK":enemy.boss()?(enemy.variant==6?enemy.label():wave==10?"HOLLOW KING":wave==5?"ASH WARDEN":"WARLORD"):enemy.label();
            DrawRectangle((int)screen.x()-Menu.measure(label,10)/2-4,(int)screen.y()-17,Menu.measure(label,10)+8,15,Fade(Menu.INK,.85f));
            Menu.text(label,(int)screen.x()-Menu.measure(label,10)/2,(int)screen.y()-14,10,training?Menu.GOLD:enemy.attackTime>.25f?Menu.RED:Menu.TEXT);
        }
    }
}
