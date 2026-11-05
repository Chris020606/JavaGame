import assets.AssetPaths;
import Entities.Enemy;
import Entities.Player;
import Entities.BossMove;
import Entities.ArmorSlot;
import Entities.AnimationFrames;
import org.bytedeco.javacpp.FloatPointer;
import static com.raylib.Raylib.*;
import static com.raylib.Colors.WHITE;

/** Shared GPU assets: load once, pose each raider immediately before drawing. */
public final class WorldAssets implements AutoCloseable {
    private final Model raider = LoadModel(AssetPaths.path("resources/outpost/raider.glb"));
    private final Model sword = LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/sword_1handed.gltf"));
    private final Model supplyCache=LoadModel(AssetPaths.path("resources/outpost/crate.obj"));
    private final Shader silhouette=LoadShader(null,AssetPaths.path("resources/outpost/silhouette.fs"));
    private final Shader daylight=LoadShader(AssetPaths.path("resources/outpost/daylight.vs"),AssetPaths.path("resources/outpost/daylight.fs"));
    private final java.util.Map<String,Model> nature=new java.util.LinkedHashMap<>();
    private final java.util.Map<String,Model> graveyard=new java.util.LinkedHashMap<>(),equipment=new java.util.LinkedHashMap<>();
    private final java.util.Map<String,int[][]> baseNatureColors=new java.util.HashMap<>();
    private final java.util.Map<Model,java.util.Map<String,Integer>> humanClips=new java.util.IdentityHashMap<>();
    private record RoadPart(Model model,boolean shoulder) {}
    private record PropTransform(Vector3 position,Vector3 axis,Vector3 scale,Color tint) {}
    private record StaticDraw(Model model,PropTransform transform,float yaw) {}
    private record TerrainTiles(java.util.List<StaticDraw> ground,java.util.List<StaticDraw> water,
                                java.util.List<StaticDraw> outer,java.util.List<StaticDraw> shadowOuter) {}
    private final java.util.Map<Integer,java.util.List<RoadPart>> roadModels=new java.util.HashMap<>();
    private final java.util.Map<EnvironmentLayout.Prop,PropTransform> propTransforms=new java.util.IdentityHashMap<>();
    private final java.util.Map<Integer,TerrainTiles> terrainLayers=new java.util.HashMap<>();
    private final java.util.List<StaticDraw> backdrops=new java.util.ArrayList<>();
    private int lookIndex=-1;
    private final Texture whiteAlbedo=whiteAlbedo();
    private static Texture whiteAlbedo() { Image pixels=GenImageColor(2,2,WHITE);try { return LoadTextureFromImage(pixels); }finally { UnloadImage(pixels); } }
    private final Model mountains=LoadModel(AssetPaths.path("resources/outpost/landscape/mountains.obj")),frostMountains=LoadModel(AssetPaths.path("resources/outpost/landscape/mountains-frost.obj"));
    private final Texture skyGlow=LoadTexture(AssetPaths.path("resources/outpost/effects/soft.png"));
    private float currentDay=1;
    private float viewerX,viewerZ,heroX,heroZ;
    private Camera3D viewCamera;
    private final float[] starDirections=starDirections();
    private final Vector3[] starPositions=vectors(85),cloudPositions=vectors(30);
    private final Vector3[] birdPositions=vectors(18);
    private final Vector3 sunPosition=new Vector3(),moonPosition=new Vector3();
    private final Color starTint=new Color().r((byte)255).g((byte)255).b((byte)255).a((byte)0);
    private final Color cloudTint=new Color(),sunTint=Menu.color(255,229,170),moonTint=Menu.color(200,215,236);
    private final Color sunGlowTint=Fade(Menu.GOLD,.42f),moonGlowTint=Fade(Menu.color(155,184,229),.18f);
    public void view(Player player,Camera3D camera) { viewCamera=camera;heroX=player.getPosition().x();heroZ=player.getPosition().z();viewerX=camera._position().x();viewerZ=camera._position().z(); }
    private ChapterLook look=ChapterLook.MEADOW;
    private final Model skeletonMinion=LoadModel(AssetPaths.path("resources/outpost/bosses/Skeleton_Minion.glb"));
    private final Model skeletonRogue=LoadModel(AssetPaths.path("resources/outpost/bosses/Skeleton_Rogue.glb"));
    private final Model skeletonMage=LoadModel(AssetPaths.path("resources/outpost/bosses/Skeleton_Mage.glb"));
    private final Model longsword=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/sword_2handed.gltf"));
    private final Model waraxe=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/axe_2handed.gltf"));
    private final Model stringBow=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/bow_withString.gltf"));
    private final Model handCrossbow=LoadModel(AssetPaths.path("resources/KayKit_Adventurers_2.0_FREE/Assets/gltf/crossbow_1handed.gltf"));
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
    private final java.util.Map<String,Integer> uniformLocations=new java.util.HashMap<>();
    private final java.util.Map<Model,int[]> modelBindings=new java.util.IdentityHashMap<>();
    private final java.util.Map<ModelAnimation,java.util.Map<Integer,ModelAnimation>> animationViews=new java.util.IdentityHashMap<>();
    private final Vector3 axisUp=v(0,1,0),axisX=v(1,0,0),unitScale=v(1,1,1);
    private int uniform(String name) { return uniformLocations.computeIfAbsent(name,n->GetShaderLocation(daylight,n)); }
    private Shader passShader=daylight;
    private Texture shadowTexture;
    private boolean shadowPass;
    private final FloatPointer uniforms=new FloatPointer(4);
    private final int surfaceLoc=uniform("material");
    private int run, chop;
    private final java.util.Set<Model> verifiedArmor=new java.util.HashSet<>();
    private final float[] bladeLengths=new float[4];
    public WorldAssets() {
        if(!IsShaderValid(daylight)) throw new IllegalStateException("Surface shader did not compile");
        daylight.locs().put(SHADER_LOC_MAP_BRDF,uniform("shadowMap"));
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
                "ground_pathStraight","platform_stone","campfire_stones","fence_simple","statue_ring","bridge_stone","cliff_half_stone","ground_riverStraight","ground_riverBend","ground_riverRocks","bridge_wood","lily_small","flower_redB","flower_yellowC","grass_leafsLarge","stump_old","log_large"}) {
            Model model=LoadModel(AssetPaths.path("resources/outpost/nature/"+name+".obj"));
            if (model.meshCount()==0) throw new IllegalStateException("Missing nature model: "+name);
            // Match the imported foliage to Outpost's quieter forest palette.
            for (int i=0;i<model.materialCount();i++) {
                MaterialMap diffuse=new Material(model.materials()).position(i).maps();
                Color c=diffuse.color();
                int r=Byte.toUnsignedInt(c.r()),g=Byte.toUnsignedInt(c.g()),b=Byte.toUnsignedInt(c.b());
                if (name.startsWith("stone_") || name.startsWith("statue_")) diffuse.color(Menu.color(135+r/5,140+r/5,145+r/5));
                else if (name.startsWith("tent_") && r>g*1.5f) diffuse.color(Menu.color(148,91,57));
                else if(name.startsWith("ground_river") && b>=g && b>r*1.2f) diffuse.color(Menu.color(35,104,190));
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
        for(Model model:new Model[]{skeletonMinion,skeletonRogue,skeletonMage,longsword,waraxe,stringBow,crossbow,handCrossbow,wand})
            if(model.meshCount()==0) throw new IllegalStateException("Missing equipment or skeleton roster model");
        for(Model model:new Model[]{skeletonMinion,skeletonRogue,skeletonMage})
            if(!IsModelAnimationValid(model,new ModelAnimation(bossAnimations).position(0))) throw new IllegalStateException("Skeleton roster rig mismatch");
        for(String name:new String[]{"crypt","crypt-large","crypt-large-roof","crypt-door","gravestone-cross","gravestone-round","gravestone-broken","grave","coffin","iron-fence","iron-fence-border-gate","stone-wall","stone-wall-column","altar-stone","pillar-obelisk","pine-crooked","trunk","lantern-candle","fire-basket","road","rocks-tall"}) {
            Model model=LoadModel(AssetPaths.path("resources/outpost/graveyard/"+name+".obj"));
            if(model.meshCount()==0) throw new IllegalStateException("Missing cemetery model: "+name);graveyard.put(name,model);
        }
        for(String hero:new String[]{"warrior","archer","mage"})for(int tier=1;tier<=3;tier++)for(String slot:new String[]{"head","chest","legs","boots"}) {
            String name=hero+"-"+slot+"-"+tier;Model armor=LoadModel(AssetPaths.path("resources/outpost/equipment/"+name+".glb"));
            if(armor.meshCount()==0)throw new IllegalStateException("Missing fitted armor: "+name);equipment.put(name,armor);
        }
        loadEquipment("dawnblade");loadEquipment("dawnstaff");
        for(int tier=0;tier<bladeLengths.length;tier++) {
            Model weapon=weaponModel(0,tier);
            for(int mesh=0;mesh<weapon.meshCount();mesh++)bladeLengths[tier]=Math.max(bladeLengths[tier],GetMeshBoundingBox(new Mesh(weapon.meshes()).position(mesh)).max().y());
        }
        for(int chapter=0;chapter<4;chapter++)for(var prop:EnvironmentLayout.props(chapter)) {
            Color tint=prop.model().equals("grave")?Menu.color(132,142,126):prop.model().equals("pine-crooked")?Menu.color(118,142,128):WHITE;
            propTransforms.put(prop,transform(prop.x(),prop.y(),prop.z(),prop.scale(),tint));
        }
        prepareTerrainDraws();
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)if(Math.abs(x)>2 || Math.abs(z)>2)
            backdrops.add(staticDraw("ground_grass",x*12,-.25f,z*12,0,12,WHITE));
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0 || z!=0)
            backdrops.add(staticDraw("ground_grass",x*80,-.4f,z*80,0,80,WHITE));

    }
    private void loadEquipment(String name) {
        Model model=LoadModel(AssetPaths.path("resources/outpost/equipment/"+name+".obj"));
        if(model.meshCount()==0) throw new IllegalStateException("Missing equipment model: "+name);equipment.put(name,model);
    }
    private PropTransform transform(float x,float y,float z,float scale,Color tint) {
        return new PropTransform(v(x,y,z),axisUp,v(scale,scale,scale),tint);
    }
    private StaticDraw staticDraw(String model,float x,float y,float z,float yaw,float scale,Color tint) {
        return new StaticDraw(nature.get(model),transform(x,y,z,scale,tint),yaw);
    }
    private ModelAnimation animationView(ModelAnimation source,int index) {
        java.util.Map<Integer,ModelAnimation> views=animationViews.get(source);
        if(views==null) { views=new java.util.HashMap<>();animationViews.put(source,views); }
        ModelAnimation view=views.get(index);
        if(view==null) { view=new ModelAnimation(source).position(index);views.put(index,view); }
        return view;
    }
    private void prepareTerrainDraws() {
        for(int chapter=0;chapter<4;chapter++) {
            java.util.List<StaticDraw> water=new java.util.ArrayList<>(),outer=new java.util.ArrayList<>(),shadowOuter=new java.util.ArrayList<>();
            java.util.List<StaticDraw> ground=new java.util.ArrayList<>();
            for(var strip:EnvironmentLayout.groundStrips(chapter))
                ground.add(new StaticDraw(nature.get("ground_grass"),new PropTransform(v(strip.x(),0,strip.z()),axisUp,v(strip.width(),1,strip.depth()),WHITE),0));
            for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++) {
                var river=chapter==0?EnvironmentLayout.riverTile(x,z):null;
                if(river!=null)water.add(staticDraw(river.model(),x*4,0,z*4,river.yaw(),4,WHITE));
            }
            if(chapter==0)for(int x=-EnvironmentLayout.RIVER_EXTENT;x<=EnvironmentLayout.RIVER_EXTENT;x++)if(Math.abs(x)>8)for(int z=-6;z<=-4;z++) {
                var river=EnvironmentLayout.riverTile(x,z);
                if(river!=null) {
                    StaticDraw draw=staticDraw(river.model(),x*4,0,z*4,river.yaw(),4,WHITE);
                    outer.add(draw);if(Math.abs(x)<=16)shadowOuter.add(draw);
                }
            }
            terrainLayers.put(chapter,new TerrainTiles(java.util.List.copyOf(ground),java.util.List.copyOf(water),java.util.List.copyOf(outer),java.util.List.copyOf(shadowOuter)));
        }
    }
    private void drawStatic(java.util.List<StaticDraw> draws) {
        for(int i=0,size=draws.size();i<size;i++) { StaticDraw draw=draws.get(i);PropTransform t=draw.transform();DrawModelEx(draw.model(),t.position(),t.axis(),draw.yaw(),t.scale(),t.tint()); }
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
    private static Vector3[] vectors(int count) { Vector3[] result=new Vector3[count];for(int i=0;i<count;i++)result[i]=new Vector3();return result; }
    private static float[] starDirections() {
        float[] result=new float[85*3];
        for(int i=0;i<85;i++) { double a=i*2.399963;float y=.12f+(i%13)*.058f,r=(float)Math.sqrt(1-y*y);result[i*3]=(float)Math.cos(a)*r;result[i*3+1]=y;result[i*3+2]=(float)Math.sin(a)*r; }
        return result;
    }
    public void lightModel(Model model) {
        int shaderId=passShader.id(),shadowId=!shadowPass && shadowTexture!=null?shadowTexture.id():0;
        int[] previous=modelBindings.get(model);
        if(previous!=null && previous[0]==shaderId && previous[1]==shadowId && previous[2]==(shadowPass?1:0))return;
        for(int i=0;i<model.materialCount();i++) {
            Material material=new Material(model.materials()).position(i);material.shader().put(passShader);
            MaterialMap albedo=new MaterialMap(material.maps()).position(MATERIAL_MAP_ALBEDO);
            if(albedo.texture().id()==0)albedo.texture(whiteAlbedo);
            MaterialMap map=new MaterialMap(material.maps()).position(MATERIAL_MAP_BRDF);
            if(!shadowPass && shadowTexture!=null) map.texture(shadowTexture);else map.texture().id(0);
        }
        if(previous==null)modelBindings.put(model,new int[]{shaderId,shadowId,shadowPass?1:0});
        else { previous[0]=shaderId;previous[1]=shadowId;previous[2]=shadowPass?1:0; }
    }
    public void pass(Shader shader,boolean shadow) {
        passShader=shader;shadowPass=shadow;
        for(Model model:nature.values()) lightModel(model);
        for(Model model:graveyard.values()) lightModel(model);
    }
    public void litPass(ModernRenderer renderer,Camera3D camera,float time,Enemy boss,DayNightCycle.Frame light) {
        currentDay=light.daylight();shadowTexture=renderer.shadowTexture();pass(daylight,false);
        SetShaderValueMatrix(daylight,uniform("sunVP"),renderer.sunVP());
        uniforms.put(0,camera._position().x()).put(1,camera._position().y()).put(2,camera._position().z());
        SetShaderValue(daylight,uniform("viewPos"),uniforms,SHADER_UNIFORM_VEC3);
        uniforms.put(0,time);SetShaderValue(daylight,uniform("uTime"),uniforms,SHADER_UNIFORM_FLOAT);
        uniforms.put(0,light.lightX()).put(1,light.lightY()).put(2,light.lightZ());SetShaderValue(daylight,uniform("lightDir"),uniforms,SHADER_UNIFORM_VEC3);
        uniforms.put(0,light.light().r()).put(1,light.light().g()).put(2,light.light().b());SetShaderValue(daylight,uniform("lightColor"),uniforms,SHADER_UNIFORM_VEC3);
        uniforms.put(0,light.daylight());SetShaderValue(daylight,uniform("dayAmount"),uniforms,SHADER_UNIFORM_FLOAT);
        uniforms.put(0,lookIndex);SetShaderValue(daylight,uniform("environment"),uniforms,SHADER_UNIFORM_FLOAT);
        uniforms.put(0,renderer.shadowSize());SetShaderValue(daylight,uniform("shadowSize"),uniforms,SHADER_UNIFORM_FLOAT);
        uniforms.put(0,boss==null?0:boss.x).put(1,2).put(2,boss==null?0:boss.z);
        SetShaderValue(daylight,uniform("bossPos"),uniforms,SHADER_UNIFORM_VEC3);
        uniforms.put(0,boss==null?0:boss.enraged()?1.2f:.5f);SetShaderValue(daylight,uniform("bossGlow"),uniforms,SHADER_UNIFORM_FLOAT);
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
    public void drawSky(Camera3D camera,DayNightCycle.Frame light) {
        Vector3 eye=camera._position();
        Vector3 sun=sunPosition.x(eye.x()+light.sunX()*180).y(eye.y()+light.sunY()*180).z(eye.z()+light.sunZ()*180);
        Vector3 moon=moonPosition.x(eye.x()-light.sunX()*180).y(eye.y()-light.sunY()*180).z(eye.z()-light.sunZ()*180);
        if(light.sunY()>-.08f) {
            DrawSphereEx(sun,5.5f,16,24,sunTint);BeginBlendMode(BLEND_ADDITIVE);rlDisableDepthMask();DrawBillboard(camera,skyGlow,sun,30,sunGlowTint);rlEnableDepthMask();EndBlendMode();
        }
        if(light.sunY()<.08f) {
            DrawSphereEx(moon,4.2f,16,24,moonTint);BeginBlendMode(BLEND_ADDITIVE);rlDisableDepthMask();DrawBillboard(camera,skyGlow,moon,16,moonGlowTint);rlEnableDepthMask();EndBlendMode();
        }
        if(light.daylight()<.7f) {
            starTint.a((byte)Math.round((1-light.daylight())*.8f*255));
            for(int i=0;i<85;i++) {
                int p=i*3;Vector3 star=starPositions[i];star.x(eye.x()+starDirections[p]*175).y(eye.y()+starDirections[p+1]*175).z(eye.z()+starDirections[p+2]*175);
                DrawSphere(star,.10f+(i%3)*.04f,starTint);
            }
        }
    }
    private void drawBackdrop(float time) {
        // The backdrop sits outside the playable/shadowed area and cannot cast onto combat actors.
        if(shadowPass)return;
        Model ridge=lookIndex==2?frostMountains:mountains;lightModel(ridge);
        DrawModel(ridge,v(0,-.4f,0),1,WHITE);
        // Surrounding land extends well beyond the movement boundary, hiding the square edge.
        drawStatic(backdrops);
        // Clouds drift above the distant hills. They are atmosphere, not reachable terrain.
        cloudTint.r((byte)(int)(48+176*currentDay)).g((byte)(int)(61+174*currentDay)).b((byte)(int)(88+140*currentDay)).a((byte)Math.round(.85f*255));
        for(int i=0;i<10;i++) {
            float x=-85+(i*19+time*.65f)%170,z=-58+(i%3)*47;
            for(int j=0;j<3;j++) { Vector3 cloud=cloudPositions[i*3+j];cloud.x(x+j*3).y(55+i%3*7).z(z);DrawSphereEx(cloud,4-j*.5f,6,8,cloudTint); }
        }
        for(int i=0;i<6;i++) {
            float x=(float)Math.sin(time*.11f+i)*38,z=(float)Math.cos(time*.11f+i)*38,y=13+i%3;
            float wing=.45f+(float)Math.sin(time*4+i)*.25f;
            Vector3 left=birdPositions[i*3].x(x-.7f).y(y+wing).z(z),body=birdPositions[i*3+1].x(x).y(y).z(z),right=birdPositions[i*3+2].x(x+.7f).y(y+wing).z(z);
            DrawLine3D(left,body,Menu.INK);DrawLine3D(body,right,Menu.INK);
        }
    }
    private Model roadMesh(java.util.List<RoadLayout.Sample> points,float extra,float height) {
        Mesh mesh=GenMeshPlane(1,1,1,points.size()-1);
        if(mesh.vertexCount()!=points.size()*2)throw new IllegalStateException("Unexpected road grid geometry");
        for(int row=0;row<points.size();row++)for(int side=0;side<2;side++) {
            var p=points.get(row);float sign=side==0?1:-1;int offset=(row*2+side)*3;
            mesh.vertices().put(offset,p.x()+p.nx()*(p.width()+extra)*sign).put(offset+1,height).put(offset+2,p.z()+p.nz()*(p.width()+extra)*sign);
        }
        UpdateMeshBuffer(mesh,0,mesh.vertices(),mesh.vertexCount()*3*4,0);
        Model model=LoadModelFromMesh(mesh);lightModel(model);return model;
    }
    private void addRoadParts(java.util.List<RoadPart> parts,java.util.List<RoadLayout.Sample> points) {
        if(points.size()<2)return;
        parts.add(new RoadPart(roadMesh(points,.25f,.025f),true));parts.add(new RoadPart(roadMesh(points,0,.034f),false));
    }
    private java.util.List<RoadPart> buildRoads(int chapter) {
        java.util.List<RoadPart> parts=new java.util.ArrayList<>();java.util.List<RoadLayout.Sample> run=new java.util.ArrayList<>();
        for(var point:RoadLayout.road(chapter)) {
            boolean water=chapter==0 && EnvironmentLayout.riverTile(Math.round(point.x()/4),Math.round(point.z()/4))!=null;
            if(water) { addRoadParts(parts,run);run.clear(); }else run.add(point);
        }
        addRoadParts(parts,run);
        if(chapter==0 || chapter==2) {
            var branch=RoadLayout.sample(java.util.List.of(new EnvironmentLayout.Point(0,0),new EnvironmentLayout.Point(-5,chapter==0?-4:2),new EnvironmentLayout.Point(-10,chapter==0?-9:6)));
            addRoadParts(parts,branch.stream().map(p->new RoadLayout.Sample(p.x(),p.z(),p.nx(),p.nz(),.65f)).toList());
        }
        return java.util.List.copyOf(parts);
    }
    private void drawRoad(int chapter) {
        var parts=roadModels.computeIfAbsent(chapter,this::buildRoads);
        Color core=chapter==2?Menu.color(118,135,145):chapter==0?Menu.color(122,99,68):chapter==1?Menu.color(96,92,82):Menu.color(83,87,104);
        Color edge=chapter==2?Menu.color(164,180,189):chapter==0?Menu.color(104,109,69):Menu.color(77,80,72);
        surface(.95f,0);
        for(var part:parts) { lightModel(part.model());DrawModel(part.model(),v(0,0,0),1,part.shoulder()?edge:core); }
        var road=RoadLayout.road(chapter);
        for(int i=0;i<road.size();i+=11) {
            var a=road.get(i);if(chapter==0 && EnvironmentLayout.riverTile(Math.round(a.x()/4),Math.round(a.z()/4))!=null)continue;
            float distance=a.width()+.38f+.12f*(float)Math.sin(i*2.3f);
            DrawCube(v(a.x()+a.nx()*distance,.055f,a.z()+a.nz()*distance),.12f,.045f,.19f,Fade(edge,.8f));
        }
        surface(.82f,.02f);
    }
    private void drawHighlands(int chapter,float time) { drawAuthoredLayout(0,time); }
    private void drawCemetery(boolean necropolis,float time) { drawAuthoredLayout(necropolis?3:1,time); }
    private void drawFrozen(float time) { drawAuthoredLayout(2,time); }
    private void drawAuthoredLayout(int chapter,float time) {
        TerrainTiles tiles=terrainLayers.get(chapter);
        // Flat terrain receives shadows but does not cast them. Including it in the
        // shadow atlas caused grazing-angle self-shadow stripes on Balanced quality.
        if(!shadowPass) {
            surface(.82f,.02f);drawStatic(tiles.ground());
            surface(.26f,.1f);drawStatic(tiles.outer());drawStatic(tiles.water());surface(.82f,.02f);
        }
        if(!shadowPass)drawRoad(chapter);
        if(chapter==0 || chapter==2)scenery("platform_stone",0,0,0,5.2f);
        else grave("altar-stone",0,0,0,4);
        scenery("statue_ring",0,0,0,4.5f);
        java.util.List<EnvironmentLayout.Prop> props=EnvironmentLayout.props(chapter);
        for(int i=0,size=props.size();i<size;i++) {
            var prop=props.get(i);
            if(shadowPass && prop.x()*prop.x()+prop.z()*prop.z()>72*72)continue;
            PropTransform transform=propTransforms.get(prop);
            Model model=prop.graveyard()?graveyard.get(prop.model()):nature.get(prop.model());
            boolean foliage=prop.model().startsWith("tree") || prop.model().startsWith("grass") || prop.model().startsWith("flower");
            float wind=foliage?(float)Math.sin(time*.85f+prop.x()*.19f+prop.z()*.13f)*.8f:0;
            DrawModelEx(model,transform.position(),transform.axis(),prop.yaw()+wind,transform.scale(),transform.tint());
        }
        if(shadowPass)return;
        if(chapter==0 || chapter==2) {
            float fx=-9,fz=chapter==0?-8:8;
            for(int i=0;i<12;i++) {
                float rise=(time*.8f+i*.17f)%1.7f;
                DrawSphere(v(fx+(float)Math.sin(time*3+i)*.12f,.35f+rise,fz+(float)Math.cos(time*2+i)*.1f),.10f*(1-rise/2),Fade(Menu.GOLD,1-rise/1.7f));
            }
        }
        if(chapter==0)for(int seed=0;seed<18;seed++) {
            float drift=(time*.35f+seed*.71f)%8;
            float sx=(seed%2==0?11:-14)+drift,sz=(seed%2==0?-8:12)+(float)Math.sin(time*.2f+seed)*2;
            DrawBillboard(viewCamera,skyGlow,v(sx,.6f+(seed%4)*.3f+(float)Math.sin(time+seed)*.15f,sz),.045f,Fade(Menu.TEXT,.3f));
        }
        for(int bird=0;bird<5;bird++) {
            float bx=(float)Math.sin(time*.035f+bird*.07f)*36,bz=27+(float)Math.cos(time*.035f)*12+bird*.7f,by=13+bird*.22f;
            float wing=.3f*(float)Math.sin(time*5+bird);
            DrawLine3D(v(bx-.35f,by+wing,bz),v(bx,by,bz+.12f),Menu.color(54,65,72));
            DrawLine3D(v(bx,by,bz+.12f),v(bx+.35f,by+wing,bz),Menu.color(54,65,72));
        }
        if(chapter==0)for(int i=0;i<9;i++) {
            float t=time*.6f+i*1.7f,bx=(i<5?13:-12)+(float)Math.sin(t)*2.6f,bz=(i<5?-7:15)+(float)Math.cos(t*.8f)*2,by=.9f+(float)Math.sin(t*2)*.3f;
            float wing=.05f+(float)Math.sin(time*17+i)*.13f;Color butterfly=i%2==0?Menu.GOLD:Menu.color(116,178,231);
            Vector3 a=v(bx,by,bz),b=v(bx-.16f,by+wing,bz+.12f),c=v(bx-.12f,by+wing,bz-.10f),d=v(bx+.16f,by-wing,bz+.12f),e=v(bx+.12f,by-wing,bz-.10f);
            DrawTriangle3D(a,b,c,butterfly);DrawTriangle3D(a,c,b,butterfly);DrawTriangle3D(a,d,e,butterfly);DrawTriangle3D(a,e,d,butterfly);
        }
        if(chapter==2)for(int i=0;i<55;i++) {
            float x=-24+(i*7.13f+time*1.5f)%48,z=-24+(i*11.71f)%48,y=12-(time*.8f+i*.34f)%12;
            DrawSphere(v(x,y,z),.035f,Menu.TEXT);
        }
        if(chapter==1 || chapter==3)for(int i=0;i<24;i++) {
            float x=-24+(i*7.13f+time*.14f)%48,z=-24+(i*11.71f)%48,y=.3f+(float)Math.sin(time*.7f+i)*.2f;
            Color mote=chapter==1?Menu.color(208,151,100):Menu.color(127,151,230);
            DrawBillboard(viewCamera,skyGlow,v(x,y,z),chapter==1?.20f:.36f,Fade(mote,.22f+.14f*(float)Math.sin(time+i)));
        }
        if(chapter==0 && currentDay<.5f)for(int i=0;i<18;i++) {
            float x=(i%2==0?-15:17)+(float)Math.sin(time*.3f+i)*3,z=-12+(i%6)*5;
            DrawSphere(v(x,1+(float)Math.sin(time+i)*.4f,z),.035f,Fade(Menu.GOLD,(.5f-currentDay)*1.5f));
        }
    }
    private void grave(String name,float x,float z,float angle,float scale) {
        graveAt(name,x,name.equals("road")?-.28f:0,z,angle,scale);
    }
    private void graveAt(String name,float x,float y,float z,float angle,float scale) {
        Model model=graveyard.get(name);
        Color tint=name.equals("grave")?Menu.color(132,142,126):name.equals("pine-crooked")?Menu.color(118,142,128):WHITE;
        DrawModelEx(model,v(x,y,z),axisUp,angle,v(scale,scale,scale),tint);
    }
    private void scenery(String name,float x,float z,float angle,float scale) {
        sceneryAt(name,x,0,z,angle,scale);
    }
    private void sceneryAt(String name,float x,float y,float z,float angle,float scale) {
        Model model=nature.get(name);
        DrawModelEx(model,v(x,y,z),axisUp,angle,v(scale,scale,scale),WHITE);
    }
    private void drawBoss(Enemy enemy) {
        Model model=enemy.king?bossKing:bossWarrior;
        ModelAnimation animations=enemy.king?kingAnimations:bossAnimations;
        String name=enemy.health<=0?"Death_A":enemy.stun>0?"Hit_A":enemy.chargeTime>0?"Running_A":enemy.attackTime>0?
            switch(enemy.bossMove) { case CLEAVE -> "2H_Melee_Attack_Slice";case SLAM -> "2H_Melee_Attack_Chop";case CHARGE -> "Taunt";case SIGILS -> "Spellcast_Raise"; }:"Walking_D_Skeletons";
        ModelAnimation clip=animationView(animations,bossClips.get(name));
        float frame=enemy.health<=0?AnimationFrames.sample(enemy.deathTime/1.6f,clip.keyframeCount()):enemy.attackTime>0?AnimationFrames.sample(1-enemy.attackTime/enemy.attackDuration(),clip.keyframeCount()):
            AnimationFrames.looping(enemy.animationTime*45,clip.keyframeCount());
        if(enemy.health<=0)poseCorpse(model,animations,clip,frame,bossClips.get("Lie_Pose"),enemy.deathTime,true);
        else UpdateModelAnimation(model,clip,frame);
        lightModel(model);surface(.38f,.25f);
        float scale=enemy.king?2.45f:2.1f;
        Color tint=enemy.hitFlash>0?Menu.TEXT:enemy.king?Menu.color(190,190,215):Menu.color(210,184,151);
        float opacity=1;
        if(!shadowPass && enemy.health>0) {
            float dx=viewerX-heroX,dz=viewerZ-heroZ,t=((enemy.x-heroX)*dx+(enemy.z-heroZ)*dz)/Math.max(.001f,dx*dx+dz*dz);
            if(t>0 && t<1)opacity=.30f+.70f*Entities.AttackMotion.smooth((Combat.segmentDistance(enemy.x,enemy.z,heroX,heroZ,viewerX,viewerZ)-.8f)/1.4f);
            tint=Fade(tint,opacity);
        }
        if(enemy.health<=0)tint=Fade(tint,CorpsePresentation.opacity(enemy.deathTime,true));
        float modelY=enemy.health<=0?corpseSupport(enemy,model,scale):0;
        DrawModelEx(model,v(enemy.x,modelY,enemy.z),axisUp,enemy.facing,v(scale,scale,scale),tint);
        if(enemy.health>0)drawWeapon(model,v(enemy.x,0,enemy.z),enemy.facing,enemy.king?bossStaff:bossAxe,Fade(WHITE,opacity),scale);
        if(shadowPass || enemy.health<=0) return;
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
    public void drawRecoveryWisps(RecoveryWisps wisps,float time) {
        if(shadowPass)return;
        for(var wisp:wisps.active()) {
            float bob=.65f+.16f*(float)Math.sin(time*3+wisp.x()),alpha=Math.min(1,wisp.life()/4);
            Vector3 position=v(wisp.x(),bob,wisp.z());
            DrawSphereEx(position,.15f,8,12,Fade(Menu.TEAL,alpha));
            BeginBlendMode(BLEND_ADDITIVE);rlDisableDepthMask();
            DrawBillboard(viewCamera,skyGlow,position,1.15f,Fade(Menu.TEAL,.55f*alpha));
            rlEnableDepthMask();EndBlendMode();
            DrawCylinderWires(v(wisp.x(),.07f,wisp.z()),.45f,.45f,.03f,20,Fade(Menu.TEAL,.65f*alpha));
        }
    }
    public void drawFieldEvent(FieldEvent event,float time) {
        if(!event.visible() && !(event.kind()==FieldEvent.Kind.SURVIVOR && event.status()==FieldEvent.Status.COMPLETE))return;
        surface(.55f,.15f);
        if(event.kind()==FieldEvent.Kind.SURVIVOR) {
            String name=event.status()==FieldEvent.Status.ACTIVE?"Walking_A.001":"Melee_2H_Idle";
            ModelAnimation clip=animationView(animations,humanClips.get(raider).get(name));
            UpdateModelAnimation(raider,clip,AnimationFrames.looping(time*40,clip.keyframeCount()));lightModel(raider);
            DrawModelEx(raider,v(event.x(),0,event.z()),axisUp,event.facing(),v(.9f,.9f,.9f),Menu.TEAL);
        } else if(event.kind()==FieldEvent.Kind.SUPPLIES) {
            lightModel(supplyCache);DrawModelEx(supplyCache,v(event.x(),0,event.z()),axisUp,25,v(1.4f,1.4f,1.4f),WHITE);
        } else scenery("statue_obelisk",event.x(),event.z(),0,1.8f);
        if(!shadowPass && event.visible()) {
            DrawCylinderWires(v(event.x(),.06f,event.z()),2.5f,2.5f,.08f,32,Menu.TEAL);
            for(int i=0;i<6;i++) { double a=time+i*Math.PI/3;DrawSphere(v(event.x()+(float)Math.cos(a)*.8f,1.4f+(float)Math.sin(time*2+i)*.2f,event.z()+(float)Math.sin(a)*.8f),.055f,Menu.GOLD); }
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
        else if(!bone && enemy.variant==3) clipName=enemy.locomotion<.1f?"Ranged_Bow_Idle":enemy.action.equals("RETREAT")?"Bow_Backward":enemy.action.equals("STRAFE")?(enemy.personality>.5f?"Bow_Left":"Bow_Right"):"Bow_Forward";
        else if(enemy.locomotion<.1f) clipName=bone?"Idle_Combat":"Melee_2H_Idle";
        else if(enemy.action.equals("STRAFE")) clipName="Running_Strafe_Left";
        else clipName=enemy.variant==1?"Running_A":bone?"Walking_D_Skeletons":"Walking_A.001";
        if(enemy.health<=0)clipName="Death_A";
        Integer index=(bone?bossClips:humanClips.get(character)).get(clipName);
        if(index==null) throw new IllegalStateException("Missing enemy clip "+clipName);
        ModelAnimation clip=animationView(clips,index);
        float phase=enemy.attackTime>0?1-enemy.attackTime/enemy.attackDuration():0;
        if(!bone && enemy.variant==3 && enemy.attackTime>0) phase=enemy.attackTime>.25f?(enemy.attackDuration()-enemy.attackTime)/Math.max(.01f,enemy.attackDuration()-.25f):(.25f-enemy.attackTime)/.25f;
        float frame=enemy.health<=0?AnimationFrames.sample(enemy.deathTime,clip.keyframeCount()):enemy.attackTime>0?AnimationFrames.sample(phase,clip.keyframeCount()):AnimationFrames.looping(enemy.animationTime*60,clip.keyframeCount());
        if(enemy.health<=0)poseCorpse(character,clips,clip,frame,(bone?bossClips:humanClips.get(character)).get(bone?"Lie_Pose":"Lie_Idle"),enemy.deathTime,false);
        else UpdateModelAnimation(character,clip,frame);
        lightModel(character);
        float scale=enemy.variant==2?1.18f:1;
        Color tint=enemy.hitFlash>0?Menu.GOLD:bone?Menu.color(174,205,228):WHITE;
        if(enemy.health<=0)tint=Fade(tint,CorpsePresentation.opacity(enemy.deathTime,false));
        DrawModelEx(character,v(enemy.x,enemy.health<=0?corpseSupport(enemy,character,scale):0,enemy.z),axisUp,enemy.facing,v(scale,scale,scale),tint);
        if(enemy.health>0)drawWeapon(character,v(enemy.x,0,enemy.z),enemy.facing,enemy.variant==3?bow:enemy.variant==4?staff:enemy.variant==2?waraxe:sword,WHITE,scale);
    }
    private float corpseSupport(Enemy enemy,Model model,float scale) {
        if(!enemy.corpseSettled) {
            float[] bounds=Entities.SkinBounds.vertical(model);
            enemy.corpseLift=CorpsePresentation.support(bounds[0],scale);enemy.corpseHeight=(bounds[1]-bounds[0])*scale;
            enemy.corpseMinimum=bounds[0]*scale+enemy.corpseLift;
            enemy.corpseSettled=enemy.deathTime>=(enemy.boss()?1.6f:1.2f);
        }
        return enemy.corpseLift;
    }
    private void poseCorpse(Model model,ModelAnimation clips,ModelAnimation fall,float frame,int restIndex,float age,boolean boss) {
        float settle=Entities.AttackMotion.smooth((age-(boss?1.1f:.8f))/(boss?.5f:.4f));
        ModelAnimation rest=animationView(clips,restIndex);
        UpdateModelAnimationEx(model,fall,frame,rest,0,settle);
    }
    public void drawArmor(Player player,Progression progress) {
        surface(.35f,.55f);
        for(ArmorSlot slot:ArmorSlot.values()) {
            int tier=progress.armorTier(slot);if(tier==0)continue;
            Model armor=equipment.get(progress.heroClass().name().toLowerCase(java.util.Locale.ROOT)+"-"+slot.icon()+"-"+tier);
            if(verifiedArmor.add(armor) && !IsModelAnimationValid(armor,new ModelAnimation(player.getAnimation()).position(player.getAnimationIndex())))throw new IllegalStateException("Fitted armor rig mismatch");
            player.copyPoseTo(armor);lightModel(armor);
            DrawModelEx(armor,player.renderPosition(),axisUp,player.getRotation().y(),unitScale,WHITE);
        }
    }
    public void drawHeroSilhouette(Player player) {
        Model model=player.getModel();
        for(int i=0;i<model.materialCount();i++) new Material(model.materials()).position(i).shader().put(silhouette);
        rlDisableDepthTest();rlDisableDepthMask();
        DrawModelEx(model,player.renderPosition(),axisUp,player.getRotation().y(),unitScale,WHITE);
        rlEnableDepthMask();rlEnableDepthTest();modelBindings.remove(model);lightModel(model);
    }
    public void preparePlayer(Player player) { lightModel(player.getModel());surface(.48f,.15f); }
    public void drawPlayerBody(Player player,Progression progress,Color tint) {
        java.util.ArrayList<Integer> masks=new java.util.ArrayList<>();
        if(progress.armorTier(ArmorSlot.HEAD)>0) {
            if(progress.heroClass()==HeroClass.WARRIOR) { masks.add(5);masks.add(6); }
            else if(progress.heroClass()==HeroClass.MAGE)masks.add(4);
        }
        // Covered original surfaces are replaced, not drawn through the fitted shell.
        if(progress.armorTier(ArmorSlot.CHEST)>0) { masks.add(0);masks.add(1);masks.add(2); }
        if(progress.armorTier(ArmorSlot.LEGS)>0 && progress.armorTier(ArmorSlot.BOOTS)>0) {
            int leg=progress.heroClass()==HeroClass.WARRIOR?7:progress.heroClass()==HeroClass.ARCHER?5:6;masks.add(leg);masks.add(leg+1);
        }
        int[] hidden=masks.stream().mapToInt(Integer::intValue).toArray();
        int[] triangles=new int[hidden.length],vertices=new int[hidden.length];Model model=player.getModel();
        for(int i=0;i<hidden.length;i++) {
            Mesh mesh=new Mesh(model.meshes()).position(hidden[i]);triangles[i]=mesh.triangleCount();vertices[i]=mesh.vertexCount();
            mesh.triangleCount(0).vertexCount(0);
        }
        try { DrawModelEx(model,player.renderPosition(),axisUp,player.getRotation().y(),unitScale,tint); }
        finally { for(int i=0;i<hidden.length;i++)new Mesh(model.meshes()).position(hidden[i]).triangleCount(triangles[i]).vertexCount(vertices[i]); }
    }
    private Model weaponModel(int style,int tier) { return style==1?(tier==0?bow:tier==1?stringBow:tier==2?handCrossbow:crossbow):style==2?(tier==0?staff:tier==1?wand:tier==2?bossStaff:equipment.get("dawnstaff")):(tier==0?sword:tier==1?longsword:tier==2?waraxe:equipment.get("dawnblade")); }
    boolean weaponMaterialsValid() {
        for(Model model:new Model[]{equipment.get("dawnblade"),equipment.get("dawnstaff"),mountains,frostMountains}) {
            for(int i=0;i<model.materialCount();i++) {
                Color c=new Material(model.materials()).position(i).maps().color();
                if(Byte.toUnsignedInt(c.r())==0 || Byte.toUnsignedInt(c.g())==0 || Byte.toUnsignedInt(c.b())==0)return false;
            }
        }
        return true;
    }
    public void drawInventoryItem(Player player,Progression progress,int item) {
        Model model;
        if(item<4)model=weaponModel(progress.heroClass().ordinal(),item);
        else { ArmorSlot slot=ArmorSlot.values()[(item-4)%4];model=equipment.get(progress.heroClass().name().toLowerCase(java.util.Locale.ROOT)+"-"+slot.icon()+"-"+(1+(item-4)/4));player.copyPoseTo(model); }
        Matrix original=new Matrix().put(model.transform());model.transform(MatrixIdentity());lightModel(model);
        boolean bow=item<2 && progress.heroClass()==HeroClass.ARCHER;
        DrawModelEx(model,v(0,0,0),axisX,bow?90:0,unitScale,WHITE);model.transform(original);
    }
    public void drawPlayerWeapon(Player player,int style,int tier) {
        Model weapon=weaponModel(style,tier);drawWeapon(player.getModel(),player.renderPosition(),player.getRotation().y(),weapon,WHITE);
        if(player.isDrawingBow()) {
            Vector3 from=player.handPosition("handslot.r"),bowHand=player.handPosition("handslot.l");double a=Math.toRadians(player.getRotation().y());
            Vector3 tip=v(from.x()+(float)Math.sin(a)*.95f,bowHand.y()+.03f,from.z()+(float)Math.cos(a)*.95f);
            DrawCylinderEx(from,tip,.012f,.010f,5,Menu.color(181,141,90));
            DrawCylinderEx(tip,v(tip.x()+(float)Math.sin(a)*.12f,tip.y(),tip.z()+(float)Math.cos(a)*.12f),.045f,0,4,WHITE);
        }
    }
    public void sampleBlade(Player player,int tier,WeaponTrail trail) {
        int bone=player.boneIndex("handslot.r");if(bone<0)return;
        Transform hand=new Transform(player.getModel().currentPose()).position(bone);
        Vector4 q=hand.rotation();Vector3 p=hand.translation(),origin=player.getPosition();
        // The same slot-space Y axis used by drawAttached: local blade length -> world.
        float dx=2*q.x()*q.y()-2*q.w()*q.z(),dy=1-2*q.x()*q.x()-2*q.z()*q.z(),dz=2*q.y()*q.z()+2*q.w()*q.x();
        double angle=Math.toRadians(player.getRotation().y());float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
        float x=origin.x()+p.x()*c+p.z()*s,y=origin.y()+p.y(),z=origin.z()+p.z()*c-p.x()*s;
        float axisX=dx*c+dz*s,axisZ=dz*c-dx*s,length=bladeLengths[Math.max(0,Math.min(3,tier))];
        float root=length*.42f,tip=length*.98f;
        trail.sample(x+axisX*root,y+dy*root,z+axisZ*root,x+axisX*tip,y+dy*tip,z+axisZ*tip);
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
            DrawModelEx(weapon,position,axisUp,facing,v(scale,scale,scale),tint);
            return;
        }
    }
    @Override public void close() {
        animationViews.clear();modelBindings.clear();propTransforms.clear();terrainLayers.clear();backdrops.clear();
        for(var parts:roadModels.values())for(var part:parts)UnloadModel(part.model());roadModels.clear();
        for (Model model:nature.values()) UnloadModel(model);
        for(Model model:graveyard.values()) UnloadModel(model);
        for(Model model:equipment.values()) UnloadModel(model);
        for(Model model:new Model[]{skeletonMinion,skeletonRogue,skeletonMage,longsword,waraxe,stringBow,crossbow,handCrossbow,wand}) UnloadModel(model);

        UnloadModelAnimations(animations,count[0]);
        UnloadModelAnimations(archerAnimations,archerCount[0]); UnloadModelAnimations(shamanAnimations,shamanCount[0]);
        for (Model model:new Model[]{raider,archer,shaman,bow,staff,sword,supplyCache,bossWarrior,bossKing,bossAxe,bossStaff}) UnloadModel(model);
        UnloadModelAnimations(bossAnimations,bossCount[0]);UnloadModelAnimations(kingAnimations,kingCount[0]);
        UnloadModel(mountains);UnloadModel(frostMountains);UnloadTexture(skyGlow);UnloadShader(daylight);UnloadShader(silhouette);UnloadTexture(whiteAlbedo);uniforms.close();
    }
}
