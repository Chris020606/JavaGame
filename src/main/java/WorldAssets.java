import assets.AssetPaths;
import Entities.Enemy;
import Entities.Player;
import Entities.BossMove;
import Entities.ArmorSlot;
import org.bytedeco.javacpp.FloatPointer;
import static com.raylib.Raylib.*;
import static com.raylib.Colors.WHITE;

/** Shared GPU assets: load once, pose each raider immediately before drawing. */
public final class WorldAssets implements AutoCloseable {
    private final Model raider = LoadModel(AssetPaths.path("resources/outpost/raider.glb"));
    private final Model sword = LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/sword_1handed.gltf"));
    private final Shader silhouette=LoadShader(null,AssetPaths.path("resources/outpost/silhouette.fs"));
    private final Shader daylight=LoadShader(AssetPaths.path("resources/outpost/daylight.vs"),AssetPaths.path("resources/outpost/daylight.fs"));
    private final java.util.Map<String,Model> nature=new java.util.LinkedHashMap<>();
    private final java.util.Map<String,Model> graveyard=new java.util.LinkedHashMap<>(),equipment=new java.util.LinkedHashMap<>();
    private final java.util.Map<String,int[][]> baseNatureColors=new java.util.HashMap<>();
    private final java.util.Map<Model,java.util.Map<String,Integer>> humanClips=new java.util.IdentityHashMap<>();
    private int lookIndex=-1;
    private ChapterLook look=ChapterLook.MEADOW;
    private final Model skeletonMinion=LoadModel(AssetPaths.path("resources/outpost/bosses/Skeleton_Minion.glb"));
    private final Model skeletonRogue=LoadModel(AssetPaths.path("resources/outpost/bosses/Skeleton_Rogue.glb"));
    private final Model skeletonMage=LoadModel(AssetPaths.path("resources/outpost/bosses/Skeleton_Mage.glb"));
    private final Model longsword=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/sword_2handed.gltf"));
    private final Model waraxe=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/axe_2handed.gltf"));
    private final Model stringBow=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/bow_withString.gltf"));
    private final Model crossbow=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/crossbow_2handed.gltf"));
    private final Model wand=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/wand.gltf"));
    private final int[] count = new int[1];
    private final ModelAnimation animations = LoadModelAnimations(AssetPaths.path("resources/outpost/raider.glb"), count);
    private final Model archer=LoadModel(AssetPaths.path("resources/outpost/archer.glb"));
    private final Model shaman=LoadModel(AssetPaths.path("resources/outpost/shaman.glb"));
    private final Model bow=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/bow.gltf"));
    private final Model staff=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/staff.gltf"));
    private final int[] archerCount=new int[1], shamanCount=new int[1];
    private final ModelAnimation archerAnimations=LoadModelAnimations(AssetPaths.path("resources/outpost/archer.glb"),archerCount);
    private final ModelAnimation shamanAnimations=LoadModelAnimations(AssetPaths.path("resources/outpost/shaman.glb"),shamanCount);
    private final Model bossWarrior=LoadModel(AssetPaths.path("resources/outpost/bosses/Skeleton_Warrior.glb"));
    private final Model bossKing=LoadModel(AssetPaths.path("resources/outpost/bosses/Hollow_King.glb"));
    private final Model bossAxe=LoadModel(AssetPaths.path("resources/outpost/bosses/Skeleton_Axe.gltf"));
    private final Model bossStaff=LoadModel(AssetPaths.path("resources/outpost/bosses/Skeleton_Staff.gltf"));
    private final int[] bossCount=new int[1],kingCount=new int[1];
    private final ModelAnimation bossAnimations=LoadModelAnimations(AssetPaths.path("resources/outpost/bosses/Skeleton_Warrior.glb"),bossCount);
    private final ModelAnimation kingAnimations=LoadModelAnimations(AssetPaths.path("resources/outpost/bosses/Hollow_King.glb"),kingCount);
    private final java.util.Map<String,Integer> bossClips=new java.util.HashMap<>();
    private Shader passShader=daylight;
    private Texture shadowTexture;
    private boolean shadowPass;
    private final FloatPointer uniforms=new FloatPointer(4);
    private final int surfaceLoc=GetShaderLocation(daylight,"material");
    private int run, chop;
    public WorldAssets() {
        if(!IsShaderValid(daylight)) throw new IllegalStateException("Surface shader did not compile");
        daylight.locs().put(SHADER_LOC_MAP_BRDF,GetShaderLocation(daylight,"shadowMap"));
        for(Model model:new Model[]{bossWarrior,bossKing,bossAxe,bossStaff})
            if(model.meshCount()==0) throw new IllegalStateException("Missing skeleton boss asset");
        if(bossCount[0]==0 || kingCount[0]!=bossCount[0]) throw new IllegalStateException("Boss animation clips missing");
        for(int i=0;i<bossCount[0];i++) {
            ModelAnimation a=new ModelAnimation(bossAnimations).position(i),b=new ModelAnimation(kingAnimations).position(i);
            if(!IsModelAnimationValid(bossWarrior,a)||!IsModelAnimationValid(bossKing,b)) throw new IllegalStateException("Boss skeleton mismatch");
            bossClips.put(a.name().getString(),i);
        }
        for (String name : new String[]{"tree_pineTallA_detailed","tree_pineRoundC","tree_oak",
                "stone_largeA","stone_tallB","plant_bushDetailed","grass_large","statue_columnDamaged",
                "statue_obelisk","tent_detailedOpen","log_stack","flower_purpleC","mushroom_redGroup","ground_grass","ground_pathCross",
                "ground_pathStraight","platform_stone","campfire_stones","fence_simple","statue_ring","bridge_stone","cliff_half_stone"}) {
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
            int[][] colors=new int[model.materialCount()][3];
            for(int i=0;i<model.materialCount();i++) { Color c=new Material(model.materials()).position(i).maps().color();colors[i]=new int[]{Byte.toUnsignedInt(c.r()),Byte.toUnsignedInt(c.g()),Byte.toUnsignedInt(c.b())}; }
            baseNatureColors.put(name,colors);
        }
        for (Model model : new Model[]{raider,archer,shaman,bow,staff,sword})
            if (model.meshCount() == 0) throw new IllegalStateException("Missing world model; run from project directory");
        loadClips(raider,animations,count[0]);loadClips(archer,archerAnimations,archerCount[0]);loadClips(shaman,shamanAnimations,shamanCount[0]);
        for(Model model:new Model[]{skeletonMinion,skeletonRogue,skeletonMage,longsword,waraxe,stringBow,crossbow,wand})
            if(model.meshCount()==0) throw new IllegalStateException("Missing equipment or skeleton roster model");
        for(Model model:new Model[]{skeletonMinion,skeletonRogue,skeletonMage})
            if(!IsModelAnimationValid(model,new ModelAnimation(bossAnimations).position(0))) throw new IllegalStateException("Skeleton roster rig mismatch");
        for(String name:new String[]{"crypt","crypt-large","crypt-large-roof","crypt-door","gravestone-cross","gravestone-round","gravestone-broken","grave","coffin","iron-fence","iron-fence-border-gate","stone-wall","stone-wall-column","altar-stone","pillar-obelisk","pine-crooked","trunk","lantern-candle","lightpost-single","fire-basket","road","rocks-tall"}) {
            Model model=LoadModel(AssetPaths.path("resources/outpost/graveyard/"+name+".obj"));
            if(model.meshCount()==0) throw new IllegalStateException("Missing cemetery model: "+name);graveyard.put(name,model);
        }
        for(int tier=1;tier<=3;tier++) for(String slot:new String[]{"head","chest","shoulder","legs","boots"}) loadEquipment(slot+"-"+tier);
        loadEquipment("dawnblade");loadEquipment("dawnstaff");

    }
    private void loadEquipment(String name) {
        Model model=LoadModel(AssetPaths.path("resources/outpost/equipment/"+name+".obj"));
        if(model.meshCount()==0) throw new IllegalStateException("Missing equipment model: "+name);equipment.put(name,model);
    }
    private void loadClips(Model model,ModelAnimation animations,int count) {
        if(count==0) throw new IllegalStateException("No enemy animation clips");
        java.util.Map<String,Integer> names=new java.util.HashMap<>();
        for(int i=0;i<count;i++) { ModelAnimation clip=new ModelAnimation(animations).position(i);if(!IsModelAnimationValid(model,clip))throw new IllegalStateException("Enemy rig mismatch");names.put(clip.name().getString(),i); }
        humanClips.put(model,names);
    }
    public void previewPass(Shader shader) { passShader=shader;shadowPass=true; }
    public void endPreview() { passShader=daylight;shadowPass=false; }
    private void prepareLook(int chapter) {
        if(lookIndex==chapter) return;lookIndex=chapter;look=ChapterLook.at(chapter);
        for(var entry:nature.entrySet()) {
            String name=entry.getKey();Model model=entry.getValue();int[][] colors=baseNatureColors.get(name);
            for(int i=0;i<model.materialCount();i++) {
                int[] c=colors[i];Color color=Menu.color(c[0],c[1],c[2]);
                if(name.startsWith("stone")||name.startsWith("statue")||name.startsWith("platform")||name.startsWith("cliff")||name.equals("bridge_stone")) color=look.stone();
                else if(c[1]>c[0]*1.2f && c[1]>c[2]*1.1f) color=name.startsWith("ground")?look.grass():look.leaf();
                else if(c[0]>c[1]*1.2f && name.startsWith("ground")) color=look.dirt();
                new Material(model.materials()).position(i).maps().color(color);
            }
        }
    }
    private static Vector3 v(float x,float y,float z) { return new Vector3().x(x).y(y).z(z); }
    public void lightModel(Model model) {
        for(int i=0;i<model.materialCount();i++) {
            Material material=new Material(model.materials()).position(i);material.shader().put(passShader);
            MaterialMap map=new MaterialMap(material.maps()).position(MATERIAL_MAP_BRDF);
            if(!shadowPass && shadowTexture!=null) map.texture(shadowTexture);else map.texture().id(0);
        }
    }
    public void pass(Shader shader,boolean shadow) {
        passShader=shader;shadowPass=shadow;
        for(Model model:nature.values()) lightModel(model);
        for(Model model:graveyard.values()) lightModel(model);
    }
    public void litPass(ModernRenderer renderer,Camera3D camera,float time,Enemy boss) {
        shadowTexture=renderer.shadowTexture();pass(daylight,false);
        SetShaderValueMatrix(daylight,GetShaderLocation(daylight,"sunVP"),renderer.sunVP());
        uniforms.put(0,camera._position().x()).put(1,camera._position().y()).put(2,camera._position().z());
        SetShaderValue(daylight,GetShaderLocation(daylight,"viewPos"),uniforms,SHADER_UNIFORM_VEC3);
        uniforms.put(0,time);SetShaderValue(daylight,GetShaderLocation(daylight,"uTime"),uniforms,SHADER_UNIFORM_FLOAT);
        uniforms.put(0,lookIndex);SetShaderValue(daylight,GetShaderLocation(daylight,"environment"),uniforms,SHADER_UNIFORM_FLOAT);
        uniforms.put(0,renderer.shadowSize());SetShaderValue(daylight,GetShaderLocation(daylight,"shadowSize"),uniforms,SHADER_UNIFORM_FLOAT);
        uniforms.put(0,boss==null?0:boss.x).put(1,2).put(2,boss==null?0:boss.z);
        SetShaderValue(daylight,GetShaderLocation(daylight,"bossPos"),uniforms,SHADER_UNIFORM_VEC3);
        uniforms.put(0,boss==null?0:boss.enraged()?1.2f:.5f);SetShaderValue(daylight,GetShaderLocation(daylight,"bossGlow"),uniforms,SHADER_UNIFORM_FLOAT);
    }
    public void finishLitPass() { rlActiveTextureSlot(10);rlDisableTexture();rlActiveTextureSlot(0); }
    private void surface(float rough,float metal) {
        if(shadowPass) return;
        uniforms.put(0,rough).put(1,metal);SetShaderValue(daylight,surfaceLoc,uniforms,SHADER_UNIFORM_VEC2);
    }
    public void drawEnvironment(int chapter,float time) {
        prepareLook(chapter);surface(.82f,.02f);drawBackdrop(time);
        switch(chapter) { case 1 -> drawCemetery(false,time);case 2 -> drawFrozen(time);case 3 -> drawCemetery(true,time);default -> drawHighlands(0,time); }
        if(shadowPass) return;
        // This radius matches Game.nearBase exactly. Pulsing motes point toward recovery.
        Color glow=Fade(Menu.TEAL,.65f+.2f*(float)Math.sin(time*2));
        DrawCylinderWires(v(0,.09f,0),6,6,.035f,96,glow);
        for(int i=0;i<4;i++) {
            double a=i*Math.PI/2;float hx=(float)Math.cos(a)*5.5f,hz=(float)Math.sin(a)*5.5f;
            DrawLine3D(v(hx-.22f,.12f,hz),v(hx+.22f,.12f,hz),Menu.TEAL);
            DrawLine3D(v(hx,.12f,hz-.22f),v(hx,.12f,hz+.22f),Menu.TEAL);
        }
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/12+time*.07;
            float y=.15f+(time*.35f+i*.13f)%1.2f;
            DrawSphere(v((float)Math.cos(a)*5.85f,y,(float)Math.sin(a)*5.85f),.035f,Fade(Menu.TEAL,1-y/1.5f));
        }
    }
    private void drawBackdrop(float time) {
        // Surrounding land extends well beyond the movement boundary, hiding the square edge.
        for(int x=-6;x<=6;x++) for(int z=-6;z<=6;z++) {
            if(Math.abs(x)<=2 && Math.abs(z)<=2) continue;
            sceneryAt("ground_grass",x*12,-.25f,z*12,0,12);
        }
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            if(x==0 && z==0) continue;
            sceneryAt("ground_grass",x*80,-.4f,z*80,0,80);
        }
        if(shadowPass) return;
        // Clouds drift above the distant hills. They are atmosphere, not reachable terrain.
        for(int i=0;i<10;i++) {
            float x=-85+(i*19+time*.65f)%170,z=-58+(i%3)*47;
            for(int j=0;j<3;j++) DrawSphereEx(v(x+j*3,17+i%3*2,z),4-j*.5f,6,8,Fade(Menu.color(224,235,228),.85f));
        }
        for(int i=0;i<6;i++) {
            float x=(float)Math.sin(time*.11f+i)*38,z=(float)Math.cos(time*.11f+i)*38,y=13+i%3;
            float wing=.45f+(float)Math.sin(time*4+i)*.25f;
            DrawLine3D(v(x-.7f,y+wing,z),v(x,y,z),Menu.INK);
            DrawLine3D(v(x,y,z),v(x+.7f,y+wing,z),Menu.INK);
        }
    }
    private void drawHighlands(int chapter,float time) { drawAuthoredLayout(0,time); }
    private void drawCemetery(boolean necropolis,float time) { drawAuthoredLayout(necropolis?3:1,time); }
    private void drawFrozen(float time) { drawAuthoredLayout(2,time); }
    private void drawAuthoredLayout(int chapter,float time) {
        for(int x=-8;x<=8;x++) for(int z=-8;z<=8;z++) scenery("ground_grass",x*4,z*4,0,4);
        var path=EnvironmentLayout.path(chapter);
        for(int i=1;i<path.size();i++) {
            var from=path.get(i-1);var to=path.get(i);float dx=to.x()-from.x(),dz=to.z()-from.z();
            int steps=Math.max(1,(int)Math.ceil(Math.hypot(dx,dz)/3.5));float angle=(float)Math.toDegrees(Math.atan2(dx,dz));
            for(int j=0;j<steps;j++) {
                float t=(j+.5f)/steps,x=from.x()+dx*t,z=from.z()+dz*t;
                if(chapter==1 || chapter==3)graveAt("road",x,-.265f,z,angle,3.6f);
                else sceneryAt("ground_pathStraight",x,.012f,z,angle,3.8f);
            }
        }
        if(chapter==0 || chapter==2)scenery("platform_stone",0,0,0,5.2f);
        else grave("altar-stone",0,0,0,4);
        scenery("statue_ring",0,0,0,4.5f);
        for(var prop:EnvironmentLayout.props(chapter)) {
            if(prop.graveyard())graveAt(prop.model(),prop.x(),prop.y(),prop.z(),prop.yaw(),prop.scale());
            else sceneryAt(prop.model(),prop.x(),prop.y(),prop.z(),prop.yaw(),prop.scale());
        }
        if(shadowPass)return;
        if(chapter==0 || chapter==2) {
            float fx=-9,fz=chapter==0?-8:8;
            for(int i=0;i<12;i++) {
                float rise=(time*.8f+i*.17f)%1.7f;
                DrawSphere(v(fx+(float)Math.sin(time*3+i)*.12f,.35f+rise,fz+(float)Math.cos(time*2+i)*.1f),.10f*(1-rise/2),Fade(Menu.GOLD,1-rise/1.7f));
            }
        }
        if(chapter==2)for(int i=0;i<55;i++) {
            float x=-24+(i*7.13f+time*1.5f)%48,z=-24+(i*11.71f)%48,y=12-(time*.8f+i*.34f)%12;
            DrawSphere(v(x,y,z),.035f,Menu.TEXT);
        }
    }
    private void grave(String name,float x,float z,float angle,float scale) {
        graveAt(name,x,name.equals("road")?-.28f:0,z,angle,scale);
    }
    private void graveAt(String name,float x,float y,float z,float angle,float scale) {
        Model model=graveyard.get(name);lightModel(model);
        Color tint=name.equals("grave")?Menu.color(132,142,126):name.equals("pine-crooked")?Menu.color(118,142,128):WHITE;
        DrawModelEx(model,v(x,y,z),v(0,1,0),angle,v(scale,scale,scale),tint);
    }
    private void scenery(String name,float x,float z,float angle,float scale) {
        sceneryAt(name,x,0,z,angle,scale);
    }
    private void sceneryAt(String name,float x,float y,float z,float angle,float scale) {
        Model model=nature.get(name);
        DrawModelEx(model,v(x,y,z),v(0,1,0),angle,v(scale,scale,scale),WHITE);
    }
    private void drawBoss(Enemy enemy) {
        Model model=enemy.king?bossKing:bossWarrior;
        ModelAnimation animations=enemy.king?kingAnimations:bossAnimations;
        String name=enemy.stun>0?"Hit_A":enemy.chargeTime>0?"Running_A":enemy.attackTime>0?
            switch(enemy.bossMove) { case CLEAVE -> "2H_Melee_Attack_Slice";case SLAM -> "2H_Melee_Attack_Chop";case CHARGE -> "Taunt";case SIGILS -> "Spellcast_Raise"; }:"Walking_D_Skeletons";
        ModelAnimation clip=new ModelAnimation(animations).position(bossClips.get(name));
        int frame=enemy.attackTime>0?Math.min(clip.keyframeCount()-1,(int)((1-enemy.attackTime/enemy.attackDuration())*clip.keyframeCount())):
            (int)(enemy.animationTime*45)%Math.max(1,clip.keyframeCount());
        UpdateModelAnimation(model,clip,frame);lightModel(model);surface(.38f,.25f);
        float scale=enemy.king?2.45f:2.1f;
        Color tint=enemy.hitFlash>0?Menu.TEXT:enemy.king?Menu.color(190,190,215):Menu.color(210,184,151);
        DrawModelEx(model,v(enemy.x,0,enemy.z),v(0,1,0),enemy.facing,v(scale,scale,scale),tint);
        drawWeapon(model,v(enemy.x,0,enemy.z),enemy.facing,enemy.king?bossStaff:bossAxe,WHITE,scale);
        if(shadowPass) return;
        Color energy=enemy.enraged()?Menu.RED:enemy.king?Menu.color(185,105,255):Menu.GOLD;
        DrawCylinderWires(v(enemy.x,.08f,enemy.z),1.35f,1.35f,.1f,32,energy);
        // Eye lights follow the animated head rather than floating over the character.
        for(int i=0;i<model.skeleton().boneCount();i++) if(new BoneInfo(model.skeleton().bones()).position(i).name().getString().equals("head")) {
            Vector3 head=new Transform(model.currentPose()).position(i).translation();
            double a=Math.toRadians(enemy.facing);
            if(enemy.king) {
                float y=(head.y()+.95f)*scale;
                DrawCylinderWires(v(enemy.x,y,enemy.z),.5f,.5f,.12f,24,Menu.GOLD);
                for(int j=0;j<7;j++) {
                    double ca=j*Math.PI*2/7;
                    float cx=enemy.x+(float)Math.cos(ca)*.47f,cz=enemy.z+(float)Math.sin(ca)*.47f;
                    DrawCylinderEx(v(cx,y,cz),v(cx,y+.25f+(j%2)*.13f,cz),.075f,0,5,Menu.GOLD);
                }
            }
            for(float side:new float[]{-.17f,.17f}) {
                float lx=head.x()+side,lz=head.z()+.30f;
                DrawSphere(v(enemy.x+(float)(lx*Math.cos(a)+lz*Math.sin(a))*scale,(head.y()+.40f)*scale,enemy.z+(float)(lz*Math.cos(a)-lx*Math.sin(a))*scale),.075f,energy);
            }
            break;
        }
    }
    public void drawEnemy(Enemy enemy) {
        if(enemy.boss()) { drawBoss(enemy);return; }
        surface(.64f,.08f);
        boolean bone=enemy.undead;
        Model character=bone?(enemy.variant==4?skeletonMage:enemy.variant==2?bossWarrior:enemy.variant==1||enemy.variant==3?skeletonRogue:skeletonMinion):enemy.variant==3?archer:enemy.variant==4?shaman:raider;
        ModelAnimation clips=bone?bossAnimations:enemy.variant==3?archerAnimations:enemy.variant==4?shamanAnimations:animations;
        String clipName;
        if(enemy.attackTime>0) clipName=enemy.variant==3?(bone?"2H_Ranged_Shoot":enemy.attackTime>.25f?"Ranged_Bow_Draw":"Ranged_Bow_Release"):enemy.variant==4?(bone?"Spellcasting":"Ranged_Magic_Spellcasting"):(bone?"1H_Melee_Attack_Chop":"Melee_1H_Attack_Chop");
        else if(enemy.locomotion<.1f) clipName=bone?"Idle_Combat":"Melee_2H_Idle";
        else if(enemy.action.equals("STRAFE")) clipName="Running_Strafe_Left";
        else clipName=enemy.variant==1?"Running_A":bone?"Walking_D_Skeletons":"Walking_A.001";
        Integer index=(bone?bossClips:humanClips.get(character)).get(clipName);
        if(index==null) throw new IllegalStateException("Missing enemy clip "+clipName);
        ModelAnimation clip=new ModelAnimation(clips).position(index);
        float phase=enemy.attackTime>0?1-enemy.attackTime/enemy.attackDuration():0;
        if(!bone && enemy.variant==3 && enemy.attackTime>0) phase=enemy.attackTime>.25f?(enemy.attackDuration()-enemy.attackTime)/Math.max(.01f,enemy.attackDuration()-.25f):(.25f-enemy.attackTime)/.25f;
        int frame=enemy.attackTime>0?Math.min(clip.keyframeCount()-1,(int)(Math.max(0,phase)*clip.keyframeCount())):(int)(enemy.animationTime*60)%Math.max(1,clip.keyframeCount());
        UpdateModelAnimation(character,clip,frame);lightModel(character);
        float scale=enemy.variant==2?1.18f:1;
        Color tint=enemy.hitFlash>0?Menu.GOLD:bone?Menu.color(174,205,228):WHITE;
        DrawModelEx(character,v(enemy.x,0,enemy.z),v(0,1,0),enemy.facing,v(scale,scale,scale),tint);
        drawWeapon(character,v(enemy.x,0,enemy.z),enemy.facing,enemy.variant==3?bow:enemy.variant==4?staff:enemy.variant==2?waraxe:sword,WHITE,scale);
    }
    public void drawArmor(Player player,Progression progress) {
        surface(.35f,.55f);
        for(ArmorSlot slot:ArmorSlot.values()) {
            int tier=progress.armorTier(slot);if(tier==0) continue;
            Model armor=equipment.get(slot.icon()+"-"+tier);
            String bone=switch(slot) { case HEAD -> "head";case CHEST -> "chest";case LEGS -> "lowerleg.l";case BOOTS -> "foot.l"; };
            attach(armor,player,bone);
            if(slot==ArmorSlot.LEGS || slot==ArmorSlot.BOOTS) attach(armor,player,slot==ArmorSlot.LEGS?"lowerleg.r":"foot.r");
            if(slot==ArmorSlot.CHEST) { attach(equipment.get("shoulder-"+tier),player,"upperarm.l");attach(equipment.get("shoulder-"+tier),player,"upperarm.r"); }
        }
    }
    private void attach(Model armor,Player player,String bone) { drawAttached(player.getModel(),player.getPosition(),player.getRotation().y(),armor,WHITE,1,bone,false); }
    public void drawHeroSilhouette(Player player) {
        Model model=player.getModel();
        for(int i=0;i<model.materialCount();i++) new Material(model.materials()).position(i).shader().put(silhouette);
        rlDisableDepthTest();rlDisableDepthMask();
        DrawModelEx(model,player.getPosition(),v(0,1,0),player.getRotation().y(),v(1,1,1),WHITE);
        rlEnableDepthMask();rlEnableDepthTest();lightModel(model);
    }
    public void preparePlayer(Player player) { lightModel(player.getModel());surface(.48f,.15f); }
    public void drawPlayerBody(Player player,Progression progress,Color tint) {
        // These bundled rigs keep each named headwear mesh separate, in source order.
        // Suppress original headwear while an equipped helmet is drawn over the head.
        int[] hidden=progress.armorTier(Entities.ArmorSlot.HEAD)==0?new int[0]:switch(progress.heroClass()) {
            case WARRIOR -> new int[]{5,6};case MAGE -> new int[]{4};case ARCHER -> new int[0];
        };
        int[] triangles=new int[hidden.length],vertices=new int[hidden.length];Model model=player.getModel();
        for(int i=0;i<hidden.length;i++) {
            Mesh mesh=new Mesh(model.meshes()).position(hidden[i]);triangles[i]=mesh.triangleCount();vertices[i]=mesh.vertexCount();
            mesh.triangleCount(0).vertexCount(0);
        }
        try { DrawModelEx(model,player.getPosition(),v(0,1,0),player.getRotation().y(),v(1,1,1),tint); }
        finally { for(int i=0;i<hidden.length;i++)new Mesh(model.meshes()).position(hidden[i]).triangleCount(triangles[i]).vertexCount(vertices[i]); }
    }
    public void drawPlayerWeapon(Player player,int style,int tier) {
        lightModel(player.getModel());
        Color tint=tier==3?Menu.GOLD:tier==2?Menu.TEAL:tier==1?Menu.color(190,205,225):WHITE;
        drawWeapon(player.getModel(),player.getPosition(),player.getRotation().y(),style==1?(tier==0?bow:tier==1?stringBow:crossbow):style==2?(tier==0?staff:tier==1?wand:tier==2?bossStaff:equipment.get("dawnstaff")):(tier==0?sword:tier==1?longsword:tier==2?waraxe:equipment.get("dawnblade")),tint);
    }
    private void drawWeapon(Model character,Vector3 position,float facing,Model weapon) {
        drawWeapon(character,position,facing,weapon,WHITE);
    }
    private void drawWeapon(Model character,Vector3 position,float facing,Model weapon,Color tint) {
        drawWeapon(character,position,facing,weapon,tint,1);
    }
    private void drawWeapon(Model character,Vector3 position,float facing,Model weapon,Color tint,float scale) {
        lightModel(weapon);surface(.3f,.45f);
        boolean left=weapon==bow || weapon==stringBow;
        drawAttached(character,position,facing,weapon,tint,scale,left?"handslot.l":"handslot.r",left && weapon!=crossbow);
    }
    private void drawAttached(Model character,Vector3 position,float facing,Model weapon,Color tint,float scale,String slot,boolean flip) {
        lightModel(weapon);
        ModelSkeleton skeleton=character.skeleton();
        for (int i=0;i<skeleton.boneCount();i++) {
            if (!new BoneInfo(skeleton.bones()).position(i).name().getString().equals(slot)) continue;
            Transform pose=new Transform(character.currentPose()).position(i);
            Vector4 q=pose.rotation();
            float x=q.x(),y=q.y(),z=q.z(),w=q.w();
            // The slot pose is in model space; DrawModelEx applies the character's world transform.
            Matrix m=new Matrix().m0(1-2*y*y-2*z*z).m1(2*x*y+2*w*z).m2(2*x*z-2*w*y).m3(0)
                .m4(2*x*y-2*w*z).m5(1-2*x*x-2*z*z).m6(2*y*z+2*w*x).m7(0)
                .m8(2*x*z+2*w*y).m9(2*y*z-2*w*x).m10(1-2*x*x-2*y*y).m11(0)
                .m12(pose.translation().x()).m13(pose.translation().y()).m14(pose.translation().z()).m15(1);
            if (flip) {
                // Left-hand slot needs a half turn around the bow's long (local Z) axis.
                // Rotate in slot space so facing and animation cannot undo the correction.
                m.m0(-m.m0()).m1(-m.m1()).m2(-m.m2());
                m.m4(-m.m4()).m5(-m.m5()).m6(-m.m6());
            }
            weapon.transform(m);
            DrawModelEx(weapon,position,v(0,1,0),facing,v(scale,scale,scale),tint);
            return;
        }
    }
    @Override public void close() {
        for (Model model:nature.values()) UnloadModel(model);
        for(Model model:graveyard.values()) UnloadModel(model);
        for(Model model:equipment.values()) UnloadModel(model);
        for(Model model:new Model[]{skeletonMinion,skeletonRogue,skeletonMage,longsword,waraxe,stringBow,crossbow,wand}) UnloadModel(model);

        UnloadModelAnimations(animations,count[0]);
        UnloadModelAnimations(archerAnimations,archerCount[0]); UnloadModelAnimations(shamanAnimations,shamanCount[0]);
        for (Model model:new Model[]{raider,archer,shaman,bow,staff,sword,bossWarrior,bossKing,bossAxe,bossStaff}) UnloadModel(model);
        UnloadModelAnimations(bossAnimations,bossCount[0]);UnloadModelAnimations(kingAnimations,kingCount[0]);
        UnloadShader(daylight);UnloadShader(silhouette);uniforms.close();
    }
}
