import java.util.*;

/** Authored landmarks and seeded vegetation clusters, shared by both render passes. */
public final class EnvironmentLayout {
    public record Prop(boolean graveyard,String model,float x,float y,float z,float yaw,float scale) {}
    public record Point(float x,float z) {}
    private static final List<List<Prop>> CHAPTERS=new ArrayList<>();
    private static final List<List<Point>> PATHS=List.of(
        List.of(new Point(-5,-32),new Point(-4,-23),new Point(-2,-13),new Point(0,0),new Point(2,10),new Point(7,20),new Point(10,32)),
        List.of(new Point(2,-30),new Point(2,-21),new Point(0,-10),new Point(0,0),new Point(3,12),new Point(2,23),new Point(2,31)),
        List.of(new Point(-5,-32),new Point(-3,-22),new Point(0,-12),new Point(0,0),new Point(3,12),new Point(5,23),new Point(6,32)),
        List.of(new Point(0,-32),new Point(0,-23),new Point(-2,-12),new Point(0,0),new Point(4,13),new Point(3,24),new Point(3,32)));
    static { for(int chapter=0;chapter<4;chapter++) CHAPTERS.add(build(chapter)); }
    private static void n(List<Prop> p,String name,float x,float z,float yaw,float scale) { p.add(new Prop(false,name,x,0,z,yaw,scale)); }
    private static void g(List<Prop> p,String name,float x,float z,float yaw,float scale) { p.add(new Prop(true,name,x,0,z,yaw,scale)); }
    private static List<Prop> build(int chapter) {
        List<Prop> p=new ArrayList<>();Random random=new Random(9201+chapter*31);
        if(chapter==0) {
            n(p,"tent_detailedOpen",-13,-10,58,3.7f);n(p,"tent_detailedOpen",-10,-15,12,3.1f);
            n(p,"campfire_stones",-9,-8,0,2.2f);n(p,"log_stack",-15,-13,103,2.1f);
            n(p,"fence_simple",-17,-8,17,2.4f);n(p,"fence_simple",-17,-11,7,2.2f);
            n(p,"statue_columnDamaged",9,14,11,3.4f);n(p,"statue_columnDamaged",13,16,82,2.5f);
            n(p,"statue_obelisk",11,19,-9,3.1f);
            cluster(p,random,12,-13,7);cluster(p,random,-15,12,9);cluster(p,random,17,5,5);
        } else if(chapter==1) {
            burialPlot(p,random,-13,-11,9,3,3);burialPlot(p,random,13,11,-12,3,3);
            burialPlot(p,random,14,-14,-5,2,3);burialPlot(p,random,-16,14,16,2,2);
            g(p,"crypt",-28,-14,80,5);g(p,"crypt",24,-28,-23,5.8f);
            g(p,"pine-crooked",-21,-8,33,5);g(p,"pine-crooked",20,17,202,5.8f);
            cemeteryWall(p);g(p,"iron-fence-border-gate",2,-24,0,5);
            g(p,"lightpost-single",-3,-19,20,3);g(p,"lightpost-single",7,-20,-15,3);
        } else if(chapter==2) {
            // Uneven outcrops suggest a winding cut through the mountain, not two tiled rails.
            for(int side:new int[]{-1,1}) for(int i=0;i<8;i++) {
                float z=-29+i*8+random.nextFloat()*4,x=side*(25+random.nextFloat()*7);
                n(p,"cliff_half_stone",x,z,side*90+random.nextFloat()*32-16,5+random.nextFloat()*4);
                n(p,"stone_tallB",x-side*4,z+2,random.nextFloat()*360,2.8f+random.nextFloat()*2);
            }
            cluster(p,random,-16,-14,6);cluster(p,random,17,13,6);
            n(p,"tent_detailedOpen",-12,6,85,3.2f);n(p,"log_stack",-15,9,112,2.3f);
            n(p,"campfire_stones",-9,8,0,2);n(p,"bridge_stone",0,-12,0,3.5f);
        } else {
            g(p,"crypt-large",0,-30,0,5);p.add(new Prop(true,"crypt-large-roof",0,5,-30,0,5));
            g(p,"crypt-large",30,-8,270,4);p.add(new Prop(true,"crypt-large-roof",30,4,-8,270,4));
            g(p,"crypt",-28,10,90,6);g(p,"crypt",23,29,193,5);
            for(int i=0;i<4;i++) {
                g(p,"pillar-obelisk",-12+i*.6f,-18+i*9,3+i*7,3.6f-i*.2f);
                if(i!=2)g(p,"pillar-obelisk",13+i*.7f,-17+i*9,-8-i*9,3.6f-i*.3f);
            }
            burialPlot(p,random,-18,-17,24,2,2);burialPlot(p,random,19,14,-20,2,2);
            g(p,"coffin",-17,19,27,2.6f);g(p,"gravestone-broken",-15,21,32,2);
            cemeteryWall(p);g(p,"iron-fence-border-gate",0,-24,0,5);
            g(p,"fire-basket",-4,-17,0,2.7f);g(p,"fire-basket",5,-18,0,2.4f);
        }
        // Mixed-scale groves form an irregular horizon beyond the playable boundary.
        float[][] groves={{-35,-27},{-41,4},{-28,35},{5,42},{34,29},{43,-4},{29,-34},{-4,-44}};
        for(float[] grove:groves) for(int i=0;i<9;i++) {
            float x=grove[0]+(float)random.nextGaussian()*7,z=grove[1]+(float)random.nextGaussian()*7;
            if(Math.abs(x)<25 && Math.abs(z)<25)continue;
            float yaw=random.nextFloat()*360,scale=3.5f+random.nextFloat()*5;
            if(chapter==1 || chapter==3) g(p,i%4==0?"rocks-tall":"pine-crooked",x,z,yaw,scale);
            else n(p,i%4==0?"stone_tallB":i%3==0?"tree_oak":"tree_pineTallA_detailed",x,z,yaw,scale);
        }
        return List.copyOf(p);
    }
    private static void cemeteryWall(List<Prop> p) {
        // A deliberate built boundary with a gate opening and broken east/west sections.
        for(int i=-6;i<=6;i++) {
            if(i!=0 && i!=1)g(p,"stone-wall",i*4,-26,0,4);
            if(i>-5 && i<5 && i!=1)g(p,"stone-wall",i*4,26,0,4);
            if(i%4!=0)g(p,"stone-wall",-26,i*4,90,4);
            if(i<-2 || i>2)g(p,"stone-wall",26,i*4,90,4);
        }
    }
    private static void burialPlot(List<Prop> p,Random r,float cx,float cz,float yaw,int rows,int columns) {
        double a=Math.toRadians(yaw);
        for(int row=0;row<rows;row++)for(int col=0;col<columns;col++) {
            float dx=(col-(columns-1)*.5f)*3.2f+(r.nextFloat()-.5f)*.35f,dz=(row-(rows-1)*.5f)*4.3f;
            float x=cx+(float)(dx*Math.cos(a)+dz*Math.sin(a)),z=cz+(float)(dz*Math.cos(a)-dx*Math.sin(a));
            float tilt=yaw+(r.nextFloat()-.5f)*8;
            g(p,"grave",x,z,tilt,2.6f);
            g(p,(row+col)%4==0?"gravestone-broken":(row+col)%3==0?"gravestone-cross":"gravestone-round",x-(float)Math.sin(a),z-(float)Math.cos(a),tilt,1.5f+r.nextFloat()*.3f);
            if(row==0 && col==0)g(p,"lantern-candle",x+1.1f,z,tilt,2.2f);
        }
    }
    private static void cluster(List<Prop> p,Random r,float cx,float cz,int count) {
        n(p,"stone_largeA",cx,cz,r.nextFloat()*360,2.3f+r.nextFloat());
        for(int i=0;i<count;i++) {
            float x=cx+(float)r.nextGaussian()*2.5f,z=cz+(float)r.nextGaussian()*2.5f;
            if(Math.hypot(x,z)<8)continue;
            n(p,i%4==0?"plant_bushDetailed":"grass_large",x,z,r.nextFloat()*360,1.2f+r.nextFloat());
            if(i%3==0)n(p,"flower_purpleC",x+.4f,z+.2f,r.nextFloat()*360,.8f+r.nextFloat()*.5f);
        }
    }
    public static List<Prop> props(int chapter) { return CHAPTERS.get(Math.max(0,Math.min(3,chapter))); }
    public static List<Point> path(int chapter) { return PATHS.get(Math.max(0,Math.min(3,chapter))); }
    private EnvironmentLayout() {}
}
