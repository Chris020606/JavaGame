import Entities.Enemy;
import Entities.BossMove;
import assets.AssetPaths;
import java.util.ArrayList;
import static com.raylib.Raylib.*;

/** Soft textured particles, weapon ribbons and authored spell decals. No collision rules live here. */
public final class CombatVfx implements AutoCloseable {
    private final Texture soft=texture("soft"),spark=texture("spark"),rune=texture("rune"),ring=texture("ring");
    private final Model ground=LoadModelFromMesh(GenMeshPlane(2,2,1,1));
    private final ArrayList<Particle> particles=new ArrayList<>();
    private int serial;
    private static final class Particle {
        float x,y,z,vx,vy,vz,life,total,size;Color color;
    }
    private static Texture texture(String name) {
        Texture t=LoadTexture(AssetPaths.path("resources/outpost/effects/"+name+".png"));
        if(t.id()==0)throw new IllegalStateException("Missing combat effect: "+name);
        SetTextureFilter(t,TEXTURE_FILTER_BILINEAR);return t;
    }
    private static Vector3 v(float x,float y,float z) { return new Vector3().x(x).y(y).z(z); }
    public void clear() { particles.clear(); }
    public void update(float dt) {
        particles.removeIf(p->{p.life-=dt;if(p.life<=0)return true;p.x+=p.vx*dt;p.y+=p.vy*dt;p.z+=p.vz*dt;p.vy-=5*dt;return false;});
    }
    public void impact(float x,float z,Color color,boolean heavy) {
        for(int i=0;i<(heavy?24:12);i++) {
            if(particles.size()>=240)particles.remove(0);
            double a=(serial++*2.399963);float force=1.4f+(i%5)*.45f;
            Particle p=new Particle();p.x=x;p.y=.8f;p.z=z;p.vx=(float)Math.cos(a)*force;p.vz=(float)Math.sin(a)*force;p.vy=.8f+(i%4)*.65f;
            p.total=p.life=.24f+(i%5)*.08f;p.size=heavy?.14f:.10f;p.color=color;particles.add(p);
        }
    }
    public void begin() { BeginBlendMode(BLEND_ADDITIVE);rlDisableDepthMask(); }
    public void end() { rlEnableDepthMask();EndBlendMode(); }
    private void glow(Camera3D camera,float x,float y,float z,float size,Color color,float alpha) {
        DrawBillboard(camera,soft,v(x,y,z),size,Fade(color,alpha));
    }
    private void decal(Texture texture,float x,float z,float radius,float yaw,Color color,float alpha) {
        new Material(ground.materials()).maps().texture(texture);
        // Ring artwork ends at .88 of the texture; keep its outer edge at the gameplay radius.
        float scale=radius/.88f;
        DrawModelEx(ground,v(x,.075f,z),v(0,1,0),yaw,v(scale,1,scale),Fade(color,alpha));
    }
    private void ribbon(float x,float z,float facing,float radius,float halfAngle,float height,Color color,float alpha) {
        for(int layer=0;layer<3;layer++)for(int i=0;i<40;i++) {
            float t=i/40f,t1=(i+1)/40f;double a=Math.toRadians(facing-halfAngle+2*halfAngle*t),b=Math.toRadians(facing-halfAngle+2*halfAngle*t1);
            float outer=radius+layer*.035f,inner=radius-(.07f+layer*.08f);
            float y=height+(float)Math.sin(t*Math.PI)*height*.3f,y1=height+(float)Math.sin(t1*Math.PI)*height*.3f;
            Vector3 p=v(x+(float)Math.sin(a)*inner,y,z+(float)Math.cos(a)*inner),q=v(x+(float)Math.sin(a)*outer,y,z+(float)Math.cos(a)*outer),
                r=v(x+(float)Math.sin(b)*outer,y1,z+(float)Math.cos(b)*outer),s=v(x+(float)Math.sin(b)*inner,y1,z+(float)Math.cos(b)*inner);
            Color c=Fade(color,alpha*(float)Math.sin((t+.0125)*Math.PI)/(1+layer*1.8f));
            DrawTriangle3D(p,q,r,c);DrawTriangle3D(p,r,s,c);DrawTriangle3D(r,q,p,c);DrawTriangle3D(s,r,p,c);
        }
    }
    public void slash(float x,float z,float facing,float radius,float halfAngle,float phase,Color color) {
        ribbon(x,z,facing,radius,halfAngle,.07f,color,.12f);
        if(phase>.60f)ribbon(x,z,facing-halfAngle+phase*halfAngle*2,radius*.82f,halfAngle*.55f,.9f,color,.8f*(1-phase));
    }
    public void shield(Camera3D camera,float x,float z,float time,Color color) {
        DrawBillboard(camera,rune,v(x,1.1f,z),2.2f,Fade(color,.6f));glow(camera,x,1.1f,z,2.7f,color,.12f);
    }
    public void charge(float x,float z,float amount,Color color) { decal(rune,x,z,.7f+amount*.3f,amount*80,color,.3f); }
    public void enemy(Camera3D camera,Enemy e,float time) {
        float phase=Math.max(0,Math.min(1,1-e.attackTime/e.attackDuration()));
        if(e.healingTarget!=null) {
            for(int i=0;i<18;i++) {
                float t=(i/18f+time*.7f)%1;
                float x=e.x+(e.healingTarget.x-e.x)*t,z=e.z+(e.healingTarget.z-e.z)*t;
                glow(camera,x+(float)Math.sin(t*16+time*4)*.18f,1.3f+(float)Math.sin(t*Math.PI)*.8f,z,.3f,Menu.TEAL,(float)Math.sin(t*Math.PI)*.7f);
            }
            decal(rune,e.healingTarget.x,e.healingTarget.z,1.05f,time*20,Menu.TEAL,.35f);return;
        }
        if(e.attackTime<=0)return;
        Color color=e.boss()?(e.bossMove.parryable?Menu.GOLD:Menu.RED):e.ranged()?Menu.GOLD:Menu.RED;
        if(!e.boss() || e.bossMove==BossMove.CLEAVE) {
            if(e.ranged()) {
                glow(camera,e.x,1.4f,e.z,.4f+phase*.6f,e.variant==4?Menu.TEAL:Menu.GOLD,phase*.6f);
                ribbon(e.x,e.z,e.facing,Math.min(8,e.distance(e.attackX,e.attackZ)),6,.06f,color,.24f);
            } else {
                float angle=e.boss()?100:65;
                ribbon(e.x,e.z,e.facing,e.meleeRange(),angle,.07f,color,.18f+phase*.20f);
                if(e.attackTime<=.25f)ribbon(e.x,e.z,e.facing-50+phase*100,e.meleeRange()*.82f,angle*.6f,.9f,color,.9f*(1-phase));
            }
        } else switch(e.bossMove) {
            case SLAM -> { decal(rune,e.attackX,e.attackZ,3.4f,time*9,color,.25f+phase*.4f);glow(camera,e.x,1.9f,e.z,1.3f,color,phase*.25f); }
            case CHARGE -> {
                float dx=e.chargeEndX-e.x,dz=e.chargeEndZ-e.z;
                for(int i=0;i<18;i++) {
                    float t=(i/18f+time*1.2f)%1;glow(camera,e.x+dx*t,.14f,e.z+dz*t,.45f,color,.12f+.3f*t);
                }
                decal(ring,e.chargeEndX,e.chargeEndZ,1.45f,0,color,.25f);
            }
            case SIGILS -> { glow(camera,e.x,3.5f,e.z,1.8f,Menu.TEAL,.45f);for(int i=0;i<6;i++){double a=time*2+i*Math.PI/3;glow(camera,e.x+(float)Math.cos(a)*.65f,3.5f,e.z+(float)Math.sin(a)*.65f,.4f,Menu.GOLD,.5f);} }
            default -> { }
        }
    }
    public void hazard(Camera3D camera,BossHazard h,float time) {
        Color color=h.kind==BossHazard.Kind.SIGIL?Menu.color(182,105,255):Menu.GOLD;
        if(h.delay>0) { decal(rune,h.x,h.z,h.radius,time*14,color,.3f+.1f*(float)Math.sin(time*9));return; }
        decal(h.kind==BossHazard.Kind.SIGIL?rune:ring,h.x,h.z,h.radius,time*8,color,h.kind==BossHazard.Kind.SIGIL?.8f:.42f);
        int count=h.kind==BossHazard.Kind.SIGIL?18:32;
        for(int i=0;i<count;i++) {
            double a=i*Math.PI*2/count;float r=h.radius*(h.kind==BossHazard.Kind.SIGIL?.3f+(i%5)*.14f:1),rise=h.kind==BossHazard.Kind.SIGIL?h.age*5+(i%4)*.15f:.15f+(i%3)*.15f;
            glow(camera,h.x+(float)Math.cos(a)*r,rise,h.z+(float)Math.sin(a)*r,.3f+(i%3)*.16f,color,Math.max(0,.65f-h.age*.4f));
        }
    }
    public void projectile(Camera3D camera,float x,float z,float vx,float vz,boolean magic,Color color,float time) {
        if(magic) {
            for(int i=5;i>=0;i--)glow(camera,x-vx*i*.026f,1+(float)Math.sin(time*14+i)*.035f,z-vz*i*.026f,.24f+(5-i)*.07f,color,.20f+(5-i)*.08f);
            glow(camera,x,1,z,.18f,Menu.TEXT,.9f);
        } else {
            DrawCylinderEx(v(x-vx*.045f,1,z-vz*.045f),v(x,1,z),.018f,.012f,5,Menu.TEXT);
            DrawCylinderEx(v(x,1,z),v(x+vx*.012f,1,z+vz*.012f),.05f,0,5,color);
            for(int i=1;i<4;i++)glow(camera,x-vx*i*.025f,1,z-vz*i*.025f,.10f,color,.18f/i);
        }
    }
    public void particles(Camera3D camera) {
        for(Particle p:particles) {
            float fade=p.life/p.total;DrawBillboard(camera,spark,v(p.x,Math.max(.08f,p.y),p.z),p.size*(.6f+fade),Fade(p.color,fade));
        }
    }
    @Override public void close() { UnloadModel(ground);for(Texture t:new Texture[]{soft,spark,rune,ring})UnloadTexture(t); }
}
