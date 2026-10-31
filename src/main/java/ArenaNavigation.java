import java.util.*;

/** Conservative trunk/stone footprints leave foliage, bridges and open tents traversable. */
public final class ArenaNavigation {
    public record Obstacle(float x,float z,float radius) { }
    public record Position(float x,float z) { }
    private static final List<List<Obstacle>> CHAPTERS=new ArrayList<>();
    static {
        for(int chapter=0;chapter<4;chapter++) {
            List<Obstacle> items=new ArrayList<>();
            for(var p:EnvironmentLayout.props(chapter)) {
                String name=p.model();
                boolean solid=name.startsWith("tree_") || name.startsWith("stone_") && !name.contains("wall") || name.startsWith("statue_") || name.equals("pillar-obelisk") || name.equals("pine-crooked") || name.equals("rocks-tall");
                if(solid && Math.abs(p.x())<21 && Math.abs(p.z())<21 && Math.hypot(p.x(),p.z())>8)
                    items.add(new Obstacle(p.x(),p.z(),Math.max(.35f,Math.min(1.2f,p.scale()*.18f))));
            }
            CHAPTERS.add(List.copyOf(items));
        }
    }
    public static List<Obstacle> obstacles(int chapter) { return CHAPTERS.get(Math.max(0,Math.min(3,chapter))); }
    public static Position move(int chapter,float fromX,float fromZ,float toX,float toZ,float actorRadius) {
        float dx=toX-fromX,dz=toZ-fromZ;int steps=Math.max(1,(int)Math.ceil(Math.hypot(dx,dz)/.2));
        float x=fromX,z=fromZ;
        for(int i=0;i<steps;i++) {
            x+=dx/steps;z+=dz/steps;
            for(int pass=0;pass<4;pass++)for(Obstacle o:obstacles(chapter)) {
                float ox=x-o.x,oz=z-o.z,d=(float)Math.hypot(ox,oz),limit=o.radius+actorRadius;
                if(d<limit) { if(d<.0001f) { ox=1;oz=0;d=1; } x=o.x+ox/d*(limit+.001f);z=o.z+oz/d*(limit+.001f); }
            }
        }
        return new Position(Math.max(-22,Math.min(22,x)),Math.max(-22,Math.min(22,z)));
    }
    public static Position steering(int chapter,float x,float z,float tx,float tz) {
        float sx=0,sz=0,dx=tx-x,dz=tz-z,d=(float)Math.hypot(dx,dz);if(d<.01f)return new Position(0,0);
        dx/=d;dz/=d;
        for(Obstacle o:obstacles(chapter)) {
            float ox=x-o.x,oz=z-o.z,dist=(float)Math.hypot(ox,oz),reach=o.radius+2;
            if(dist<reach && dist>.001f) {
                float force=(reach-dist)/reach*2.5f,side=o.x+o.z>=0?1:-1;
                sx+=ox/dist*force*.35f+dz*side*force;sz+=oz/dist*force*.35f-dx*side*force;
            }
        }
        return new Position(sx,sz);
    }
    private ArenaNavigation() { }
}
