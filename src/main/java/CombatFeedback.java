import java.util.ArrayList;
import static com.raylib.Raylib.*;

/** Short-lived world anchored damage/reward labels. Simulation time pauses with combat. */
public final class CombatFeedback {
    private static final class Popup {
        final float x,z; final String text; final Color color; final float lane;
        final int largeWidth,smallWidth;
        float time=1.05f;
        Popup(float x,float z,String text,Color color,float lane) { this.x=x;this.z=z;this.text=text;this.color=color;this.lane=lane;largeWidth=Menu.measure(text,26);smallWidth=Menu.measure(text,21); }
    }
    private final ArrayList<Popup> labels=new ArrayList<>();
    private int serial;
    public void add(float x,float z,String text,Color color) {
        if (labels.size()>=64) labels.remove(0);
        labels.add(new Popup(x,z,text,color,(serial++%3-1)*.28f));
    }

    public void update(float dt) {
        labels.removeIf(p -> { p.time-=dt;return p.time<=0; });
    }

    public void clear() {
        labels.clear(); serial=0;
    }

    public void draw(Camera3D camera) {
        for (Popup p:labels) {
            float age=1.05f-p.time;
            Vector3 world=new Vector3().x(p.x+p.lane).y(2.4f+age*1.3f).z(p.z);
            Vector2 point=GetWorldToScreen(world,camera);
            int size=age<.12f?26:21;
            int x=(int)point.x()-(age<.12f?p.largeWidth:p.smallWidth)/2,y=(int)point.y();
            Menu.text(p.text,x+2,y+2,size,Fade(Menu.INK,Math.min(1,p.time*3)));
            Menu.text(p.text,x,y,size,Fade(p.color,Math.min(1,p.time*3)));
        }
    }
}
