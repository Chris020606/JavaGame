import assets.AssetPaths;
import Entities.Enemy;
import Entities.Player;
import static com.raylib.Raylib.*;
import static com.raylib.Colors.WHITE;

/** Shared GPU assets: load once, pose each raider immediately before drawing. */
public final class WorldAssets implements AutoCloseable {
    private final Model raider = LoadModel(AssetPaths.path("resources/outpost/raider.glb"));
    private final Model sword = LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/sword_1handed.gltf"));
    private final Model outpost = LoadModel(AssetPaths.path("resources/outpost/outpost.obj"));
    private final Model pine = LoadModel(AssetPaths.path("resources/outpost/pine.obj"));
    private final Model rock = LoadModel(AssetPaths.path("resources/outpost/rock.obj"));
    private final Model crate = LoadModel(AssetPaths.path("resources/outpost/crate.obj"));
    private final Shader daylight=LoadShader(AssetPaths.path("resources/outpost/daylight.vs"),AssetPaths.path("resources/outpost/daylight.fs"));
    private final java.util.Map<String,Model> nature=new java.util.LinkedHashMap<>();
    private final int[] count = new int[1];
    private final ModelAnimation animations = LoadModelAnimations(AssetPaths.path("resources/outpost/raider.glb"), count);
    private final Model archer=LoadModel(AssetPaths.path("resources/outpost/archer.glb"));
    private final Model shaman=LoadModel(AssetPaths.path("resources/outpost/shaman.glb"));
    private final Model bow=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/bow.gltf"));
    private final Model staff=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/staff.gltf"));
    private final int[] archerCount=new int[1], shamanCount=new int[1];
    private final ModelAnimation archerAnimations=LoadModelAnimations(AssetPaths.path("resources/outpost/archer.glb"),archerCount);
    private final ModelAnimation shamanAnimations=LoadModelAnimations(AssetPaths.path("resources/outpost/shaman.glb"),shamanCount);
    private int run, chop;
    public WorldAssets() {
        for (String name : new String[]{"tree_pineTallA_detailed","tree_pineRoundC","tree_oak",
                "stone_largeA","stone_tallB","plant_bushDetailed","grass_large","statue_columnDamaged",
                "statue_obelisk","tent_detailedOpen","log_stack","flower_purpleC","mushroom_redGroup","ground_grass","ground_pathCross",
                "ground_pathStraight","platform_stone","campfire_stones","fence_simple","statue_ring"}) {
            Model model=LoadModel(AssetPaths.path("resources/outpost/nature/"+name+".obj"));
            if (model.meshCount()==0) throw new IllegalStateException("Missing nature model: "+name);
            // Match the imported foliage to Outpost's quieter forest palette.
            for (int i=0;i<model.materialCount();i++) {
                MaterialMap diffuse=new Material(model.materials()).position(i).maps();
                Color c=diffuse.color();
                int r=Byte.toUnsignedInt(c.r()),g=Byte.toUnsignedInt(c.g()),b=Byte.toUnsignedInt(c.b());
                if (name.startsWith("stone_") || name.startsWith("statue_")) diffuse.color(Menu.color(135+r/5,140+r/5,145+r/5));
                else if (name.startsWith("tent_") && r>g*1.5f) diffuse.color(Menu.color(148,91,57));
                else if (g>r*2 && b>r*2) diffuse.color(Menu.color(76,125,77));
                else if (r>g*1.5f && g>b*1.2f) diffuse.color(Menu.color(113,78,52));
            }
            lightModel(model);
            nature.put(name,model);
        }
        for (Model model : new Model[]{raider,archer,shaman,bow,staff,sword,outpost,pine,rock,crate})
            if (model.meshCount() == 0) throw new IllegalStateException("Missing world model; run from project directory");
        if (count[0] != 2 || archerCount[0]!=4 || shamanCount[0]!=3) throw new IllegalStateException("Missing raider animation clips");
        for (int i=0;i<count[0];i++) {
            if (!IsModelAnimationValid(raider, new ModelAnimation(animations).position(i)) || !IsModelAnimationValid(archer,new ModelAnimation(archerAnimations).position(i)) || !IsModelAnimationValid(shaman,new ModelAnimation(shamanAnimations).position(i)))
                throw new IllegalStateException("Raider animation skeleton mismatch");
            String name = new ModelAnimation(animations).position(i).name().getString();
            if (name.equals("Running_A")) run=i;
            if (name.equals("Melee_1H_Attack_Chop")) chop=i;
        }
    }
    private static Vector3 v(float x,float y,float z) { return new Vector3().x(x).y(y).z(z); }
    private void draw(Model model,float x,float z,float angle,float scale) {
        DrawModelEx(model,v(x,0,z),v(0,1,0),angle,v(scale,scale,scale),WHITE);
    }
    public void lightModel(Model model) {
        for(int i=0;i<model.materialCount();i++) new Material(model.materials()).position(i).shader().put(daylight);
    }
    public void drawEnvironment(Arena arena) {
        if (arena==Arena.HIGHLANDS) { drawHighlands(); return; }
        boolean highland=arena==Arena.HIGHLANDS;
        // Layer the perimeter with varied silhouettes, leaving the entry trails clear.
        for (int i=0;i<64;i++) {
            double a=i*Math.PI*2/64;
            float radius=25+(i%4)*1.7f;
            float x=(float)Math.cos(a)*radius,z=(float)Math.sin(a)*radius;
            if (Math.abs(x)<3 || Math.abs(z)<3) continue;
            scenery(highland ? (i%3==0?"tree_pineRoundC":"stone_tallB") :
                (i%3==0?"tree_oak":"tree_pineTallA_detailed"),x,z,i*137,highland?5+i%3:4+i%3);
        }
        if (highland) for (int sx:new int[]{-1,1}) for(int sz:new int[]{-1,1}) {
            for(int j=0;j<4;j++) scenery("statue_columnDamaged",sx*(10+j*2),sz*13,j*90,4.5f);
            scenery("statue_obelisk",sx*17,sz*17,45,5);
            scenery("stone_largeA",sx*14,sz*10,27,4);
        }
        else {
            scenery("tent_detailedOpen",-9,-9,140,4);
            scenery("log_stack",-12,-8,20,3);
            scenery("tent_detailedOpen",10,-10,220,3.5f);
        }
        // Stone trails connect the four entry routes to the haven.
        Color earth=Menu.color(94,94,72), stone=Menu.color(125,132,118);
        DrawCube(v(0,-.035f,0),3,.05f,46,earth);
        DrawCube(v(0,-.03f,0),46,.05f,3,earth);
        for (int i=-14;i<=14;i++) {
            if (Math.abs(i)<3) continue;
            float p=i*1.5f;
            DrawCube(v((i%2)*.2f,.005f,p),1.6f,.06f,.65f,stone);
            DrawCube(v(p,.005f,(i%2)*.2f),.65f,.06f,1.6f,stone);
        }
        // Small garden islands, flowers and stones break up the clearing.
        for (int i=0;i<32;i++) {
            double angle=i*2.39996;
            float radius=8+(i%5)*2.8f;
            float x=(float)Math.cos(angle)*radius,z=(float)Math.sin(angle)*radius;
            if (Math.abs(x)<3 || Math.abs(z)<3) continue;
            DrawCylinder(v(x,-.01f,z),1.3f,1.3f,.035f,9,Menu.color(65,89,67));
            scenery(i%4==0?"stone_largeA":"plant_bushDetailed",x,z,i*29,i%4==0?2.7f:2.1f);
            scenery(highland?"grass_large":"flower_purpleC",x+.9f,z+.7f,i*41,1.8f);
            if (!highland && i%3==0) scenery("mushroom_redGroup",x-.6f,z+.5f,i*17,2);

        }
        for (int i=0;i<16;i++) {
            double angle=i*Math.PI/8;
            float x=(float)Math.cos(angle)*5.8f,z=(float)Math.sin(angle)*5.8f;
            if (i%4==0) continue;
            DrawCube(v(x,.04f,z),.45f,.12f,.45f,stone);
        }
        for (int sx:new int[]{-1,1}) for (int sz:new int[]{-1,1}) {
            float x=sx*4.4f,z=sz*4.4f;
            DrawCylinder(v(x,0,z),.12f,.18f,1.5f,6,Menu.color(78,57,42));
            DrawCube(v(x,1.65f,z),.35f,.4f,.35f,Menu.GOLD);
            DrawCube(v(x,1.9f,z),.48f,.1f,.48f,Menu.LINE);
            DrawCylinder(v(x,.015f,z),.75f,.75f,.02f,16,Menu.color(110,106,66));
            // Low split fences leave the main trails open.
            for (int j=0;j<3;j++) {
                float fx=sx*(7+j*1.1f),fz=sz*7;
                DrawCube(v(fx,.45f,fz),.13f,.9f,.13f,Menu.color(102,77,49));
            }
            DrawCube(v(sx*8.1f,.5f,sz*7),2.3f,.13f,.12f,Menu.color(129,99,62));
        }
        DrawCylinderWires(v(0,.13f,0),5.7f,5.7f,.03f,64,Menu.TEAL);
        DrawCylinder(v(0,.085f,0),2.4f,2.4f,.025f,24,Menu.color(44,61,55));
        draw(outpost,0,0,0,1);
        draw(crate,-2.4f,-1.8f,12,1); draw(crate,-3.3f,-1.6f,-8,.8f);
        draw(crate,2.5f,-2,25,1);
    }
    /** Every visible piece of stage II scenery comes from the downloaded pack. */
    private void drawHighlands() {
        // A tiled meadow with branching earth tracks replaces the old flat plane/crosswalk.
        for(int x=-8;x<=8;x++) for(int z=-8;z<=8;z++) {
            String tile=x==0 && z==0?"ground_pathCross":x==0 || z==0?"ground_pathStraight":"ground_grass";
            scenery(tile,x*4,z*4,z==0?90:0,4);
        }
        // The defended landmark is an ancient stone ring, surrounded by a small expedition camp.
        scenery("platform_stone",0,0,0,5.5f);
        scenery("statue_ring",0,0,0,5);
        scenery("tent_detailedOpen",-5,-3,120,4);
        scenery("tent_detailedOpen",5,-4,230,3.5f);
        scenery("log_stack",-5.5f,-6,25,2.5f);
        scenery("campfire_stones",3,-2,0,2.5f);
        for(int side:new int[]{-1,1}) {
            scenery("statue_columnDamaged",side*4,5,side*20,4.5f);
            scenery("statue_obelisk",side*7,8,side*15,4);
            scenery("fence_simple",side*7,-6,0,3);
        }
        for(int i=0;i<60;i++) {
            double a=i*2.39996;
            float radius=10+(i%6)*2.1f;
            float x=(float)Math.cos(a)*radius,z=(float)Math.sin(a)*radius;
            if(Math.abs(x)<3 || Math.abs(z)<3) continue;
            scenery(i%5==0?"stone_largeA":i%3==0?"plant_bushDetailed":"grass_large",x,z,i*53,i%5==0?3:2.5f);
            if(i%4==0) scenery("flower_purpleC",x+.7f,z+.5f,i*23,2);
            if(i%9==0) scenery("statue_columnDamaged",x,z,i*37,3.5f);
        }
        // Tall scenery stays beyond the playable square so the hero remains visible.
        for(int i=0;i<56;i++) {
            double a=i*Math.PI*2/56;
            float radius=26+(i%3)*2;
            float x=(float)Math.cos(a)*radius,z=(float)Math.sin(a)*radius;
            if(Math.abs(x)<3 || Math.abs(z)<3) continue;
            scenery(i%4==0?"stone_tallB":i%3==0?"tree_oak":"tree_pineTallA_detailed",x,z,i*137,i%4==0?5:4.5f+i%2);
        }
    }
    private void scenery(String name,float x,float z,float angle,float scale) {
        Model model=nature.get(name);
        // OBJ exports sit on the ground plane.
        DrawModelEx(model,v(x,0,z),v(0,1,0),angle,v(scale,scale,scale),WHITE);
    }
    public void drawEnemy(Enemy enemy) {
        Model character=enemy.variant==3?archer:enemy.variant==4?shaman:raider;
        ModelAnimation clips=enemy.variant==3?archerAnimations:enemy.variant==4?shamanAnimations:animations;
        int attackClip=enemy.variant==3?(enemy.attackTime>.25f?2:3):enemy.variant==4?2:chop;
        ModelAnimation clip=new ModelAnimation(clips).position(enemy.attackTime>0?attackClip:run);
        int frame=enemy.attackTime>0 ? Math.min(clip.keyframeCount()-1,(int)((enemy.attackDuration()-enemy.attackTime)*clip.keyframeCount()/enemy.attackDuration()))
            : (int)(enemy.animationTime*60)%Math.max(1,clip.keyframeCount());
        if (enemy.variant==3 && enemy.attackTime>0) {
            float phase=enemy.attackTime>.25f?(.7f-enemy.attackTime)/.45f:(.25f-enemy.attackTime)/.25f;
            frame=Math.min(clip.keyframeCount()-1,(int)(phase*clip.keyframeCount()));
        }
        UpdateModelAnimation(character,clip,frame);
        lightModel(character);
        Color tint=enemy.hitFlash>0 ? Menu.GOLD : enemy.boss()?Menu.GOLD:enemy.variant==2?Menu.color(220,178,157):enemy.variant==1?Menu.color(185,210,229):WHITE;
        DrawCylinder(v(enemy.x,.025f,enemy.z),.42f,.42f,.015f,12,Menu.color(38,54,47));
        DrawModelEx(character,v(enemy.x,0,enemy.z),v(0,1,0),enemy.facing,v(1,1,1),tint);
        if (enemy.boss()) {
            DrawCylinderWires(v(enemy.x,.08f,enemy.z),.85f,.85f,.12f,24,Menu.GOLD);
            DrawCube(v(enemy.x,2.05f,enemy.z),.5f,.18f,.5f,Menu.GOLD);
        }
        drawWeapon(character,v(enemy.x,0,enemy.z),enemy.facing,enemy.variant==3?bow:enemy.variant==4?staff:sword);
    }
    public void drawPlayerWeapon(Player player,int style,int tier) {
        lightModel(player.getModel());
        Color tint=tier==3?Menu.GOLD:tier==2?Menu.TEAL:tier==1?Menu.color(190,205,225):WHITE;
        drawWeapon(player.getModel(),player.getPosition(),player.getRotation().y(),style==1?bow:style==2?staff:sword,tint);
    }
    private void drawWeapon(Model character,Vector3 position,float facing,Model weapon) {
        drawWeapon(character,position,facing,weapon,WHITE);
    }
    private void drawWeapon(Model character,Vector3 position,float facing,Model weapon,Color tint) {
        ModelSkeleton skeleton=character.skeleton();
        for (int i=0;i<skeleton.boneCount();i++) {
            if (!new BoneInfo(skeleton.bones()).position(i).name().getString().equals(weapon==bow?"handslot.l":"handslot.r")) continue;
            Transform pose=new Transform(character.currentPose()).position(i);
            Vector4 q=pose.rotation();
            float x=q.x(),y=q.y(),z=q.z(),w=q.w();
            // The slot pose is in model space; DrawModelEx applies the character's world transform.
            Matrix m=new Matrix().m0(1-2*y*y-2*z*z).m1(2*x*y+2*w*z).m2(2*x*z-2*w*y).m3(0)
                .m4(2*x*y-2*w*z).m5(1-2*x*x-2*z*z).m6(2*y*z+2*w*x).m7(0)
                .m8(2*x*z+2*w*y).m9(2*y*z-2*w*x).m10(1-2*x*x-2*y*y).m11(0)
                .m12(pose.translation().x()).m13(pose.translation().y()).m14(pose.translation().z()).m15(1);
            if (weapon==bow) {
                // Left-hand slot needs a half turn around the bow's long (local Z) axis.
                // Rotate in slot space so facing and animation cannot undo the correction.
                m.m0(-m.m0()).m1(-m.m1()).m2(-m.m2());
                m.m4(-m.m4()).m5(-m.m5()).m6(-m.m6());
            }
            weapon.transform(m);
            DrawModelEx(weapon,position,v(0,1,0),facing,v(1,1,1),tint);
            return;
        }
    }
    @Override public void close() {
        for (Model model:nature.values()) UnloadModel(model);
        UnloadShader(daylight);
        UnloadModelAnimations(animations,count[0]);
        UnloadModelAnimations(archerAnimations,archerCount[0]); UnloadModelAnimations(shamanAnimations,shamanCount[0]);
        for (Model model:new Model[]{raider,archer,shaman,bow,staff,sword,outpost,pine,rock,crate}) UnloadModel(model);
    }
}
