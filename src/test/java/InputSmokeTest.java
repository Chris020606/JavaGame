import static com.raylib.Raylib.*;

/** Run with ./gradlew inputSmokeTest on a desktop; no OS input permissions needed. */
public class InputSmokeTest {
    // raylib AutomationEventType, from src/rcore.c:
    // https://github.com/raysan5/raylib/blob/master/src/rcore.c
    private static final int KEY_UP = 1, KEY_DOWN = 2, MOUSE_UP = 5, MOUSE_DOWN = 6, MOUSE_POSITION = 7;
    private static final java.util.Set<Integer> heldKeys=new java.util.HashSet<>(),heldButtons=new java.util.HashSet<>();
    private static void event(int type,int first,int second) {
        if (type==KEY_DOWN) { heldKeys.add(first); while(GetKeyPressed()!=0) { } }
        if (type==KEY_UP) heldKeys.remove(first);
        if (type==MOUSE_DOWN) heldButtons.add(first);
        if (type==MOUSE_UP) heldButtons.remove(first);
        rawEvent(type,first,second);
    }
    private static void rawEvent(int type, int first, int second) {
        try (AutomationEvent event = new AutomationEvent().type(type)) {
            event.params(0, first).params(1, second);
            PlayAutomationEvent(event);
        }
    }
    private static void frame(Game game) {
        // SDL can deliver desktop input even to a hidden window on some hosts.
        // Keep this test's virtual input state independent of the user's keyboard/mouse.
        for (int key=32;key<=348;key++) {
            if (IsKeyDown(key) && !heldKeys.contains(key)) rawEvent(KEY_UP,key,0);
            else if (!IsKeyDown(key) && heldKeys.contains(key)) rawEvent(KEY_DOWN,key,0);
        }
        for (int button=0;button<3;button++) {
            if (IsMouseButtonDown(button)&&!heldButtons.contains(button)) rawEvent(MOUSE_UP,button,0);
        }
        game.update(1f / 60);
        BeginDrawing(); ClearBackground(Menu.INK); game.render(); EndDrawing();
    }
    private static void key(Game game, int key) {
        event(KEY_DOWN, key, 0); frame(game);
        event(KEY_UP, key, 0); frame(game);
    }
    private static void click(Game game,int x,int y) {
        event(MOUSE_POSITION,x,y);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
        event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static float handHeight(Game game) {
        Model model = game.getPlayer().getModel();
        for (int i=0;i<model.skeleton().boneCount();i++) {
            if (new BoneInfo(model.skeleton().bones()).position(i).name().getString().equals("handslot.r"))
                return new Transform(model.currentPose()).position(i).translation().y();
        }
        throw new AssertionError("Sword attachment bone missing");
    }
    private static float[] handPose(Game game) {
        Model model=game.getPlayer().getModel();
        for(int i=0;i<model.skeleton().boneCount();i++)if(new BoneInfo(model.skeleton().bones()).position(i).name().getString().equals("handslot.r")) {
            Vector3 p=new Transform(model.currentPose()).position(i).translation();return new float[]{p.x(),p.y(),p.z()};
        }
        throw new AssertionError("Right-hand pose missing");
    }
    private static void capture(String name) {
        Image image = LoadImageFromScreen();
        try {
            check(ExportImage(image, "build/" + name + ".png"), "Could not export " + name);
            if (name.startsWith("ui-menu")) {
                Color corner = GetImageColor(image, image.width()*9/10, image.height()*9/10);
                check(Byte.toUnsignedInt(corner.r()) < 40 && Byte.toUnsignedInt(corner.g()) < 50,
                    "Menu dim overlay does not cover the framebuffer (DPI regression)");
            }
        }
        finally { UnloadImage(image); }
    }
    private static Entities.Enemy encounter(Game game,java.nio.file.Path profile,int wave,int variant,float z) throws Exception {
        Entities.Enemy target=new Entities.Enemy(0,z,wave,variant,.5f);
        SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Warden",0,8,0,new Progression(),new WaveDirector.State(wave,0,1,8,0,21,0),java.util.List.of(target),Arena.HIGHLANDS));
        check(game.loadCurrent(),"Advanced encounter did not load");key(game,KEY_ENTER);
        target=game.getEnemies().get(0);target.stun=100;target.cooldown=100;for(int i=0;i<20;i++)frame(game);return target;
    }
    public static void main(String[] args) throws Exception {
        java.nio.file.Path profile=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("build"),"smoke-profile-");
        System.setProperty("outpost.dataDir",profile.toAbsolutePath().toString());
        Settings initialSettings=new Settings();initialSettings.playerName="Ada";initialSettings.save(profile.resolve("settings.properties"));
        boolean standardDpi = java.util.Arrays.asList(args).contains("--standard-dpi");
        SetConfigFlags(FLAG_WINDOW_RESIZABLE | (standardDpi ? 0 : FLAG_WINDOW_HIGHDPI));
        InitWindow(1280, 800, "Outpost input regression check");
        SetExitKey(KEY_NULL); SetTargetFPS(60);
        Game game = null;
        try {
            // Desktop pointer events can arrive even when virtual button states are cleared.
            // Inject only the combat click; menus continue to use native automation events.
            java.util.concurrent.atomic.AtomicBoolean combatClick=new java.util.concurrent.atomic.AtomicBoolean();
            game = new Game(()->combatClick.getAndSet(false));
            game.getSceneManager().getOpenScene().getCameraController().setSensitivity(0);
            if(java.util.Arrays.asList(args).contains("--sky-review")) {
                key(game,KEY_ENTER);key(game,KEY_ENTER);for(int i=0;i<12;i++)frame(game);game.photoMode(true);
                Camera3D skyCamera=game.getSceneManager().getOpenScene().getCameraController().getCamera();
                float[] phases={.36f,.68f,.86f};String[] names={"cycle-day","cycle-sunset","cycle-night"};
                for(int i=0;i<phases.length;i++) {
                    game.dayPhase(phases[i]);skyCamera._position().x(22).y(16).z(28);skyCamera.target().x(0).y(1).z(-5);
                    for(int f=0;f<3;f++){BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();}capture(names[i]);
                }
                game.dayPhase(.32f);skyCamera._position().x(25).y(6).z(25);skyCamera.target().x(-50).y(30).z(-30);
                for(int f=0;f<3;f++){BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();}capture("cycle-sunrise");
                game.dayPhase(.86f);skyCamera._position().x(25).y(6).z(-25);skyCamera.target().x(-50).y(30).z(30);
                for(int f=0;f<3;f++){BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();}capture("cycle-moon");
                game.photoMode(false);System.out.println("PASS: moving daylight, sunset, moonlight and mountain review");return;
            }
            if(java.util.Arrays.asList(args).contains("--gallery")) {
                key(game,KEY_ENTER);for(int i=0;i<20;i++)frame(game);capture("player-welcome");
                int loginX=(GetScreenWidth()-940)/2,loginY=(GetScreenHeight()-600)/2;
                click(game,loginX+304,loginY+238);for(int i=0;i<5;i++)frame(game);capture("player-ranger-choice");
                for(HeroClass hero:HeroClass.values()) {
                    Progression kit=Progression.playtest(hero,Entities.Difficulty.NORMAL,false);
                    var foes=java.util.List.of(new Entities.Enemy(-5,12,1,0,.5f),new Entities.Enemy(7,9,1,1,.8f),new Entities.Enemy(-9,-4,1,2,.3f));
                    SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Ember",0,7,15,kit,new WaveDirector.State(1,0,3,8,0,21,0),foes,Arena.HIGHLANDS));
                    check(game.loadCurrent(),"Gallery hero did not load");kit=game.getProgression();key(game,KEY_ENTER);for(int i=0;i<10;i++)frame(game);
                    Camera3D view=game.getSceneManager().getOpenScene().getCameraController().getCamera();
                    game.photoMode(true);view._position().x(14).y(12).z(22);view.target().x(-2).y(.5f).z(-5);
                    for(int i=0;i<3;i++){BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();}
                    capture("player-meadow-"+hero.name().toLowerCase());
                    game.getPlayer().getPosition().x(-7).z(-6);game.getPlayer().getRotation().y(35);
                    for(int tier=1;tier<=3;tier++) {
                        for(Entities.ArmorSlot slot:Entities.ArmorSlot.values())kit.equipArmor(slot,tier);
                        for(Entities.PlayerAnimation pose:new Entities.PlayerAnimation[]{Entities.PlayerAnimation.UNARMED_IDLE,Entities.PlayerAnimation.RUN,Entities.PlayerAnimation.DODGE_FORWARD}) {
                            game.getPlayer().setAnimation(pose);game.getPlayer().playAnimationProgress(.42f);
                            view._position().x(-3).y(3).z(-1);view.target().x(-7).y(1.2f).z(-6);
                            for(int i=0;i<3;i++){BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();}
                        }
                        game.getPlayer().setAnimation(hero==HeroClass.ARCHER?Entities.PlayerAnimation.CROSSBOW_IDLE:Entities.PlayerAnimation.UNARMED_IDLE);game.getPlayer().playAnimationProgress(.15f);
                        for(int i=0;i<3;i++){BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();}
                        capture("player-armor-"+hero.name().toLowerCase()+"-"+tier);
                    }
                    if(hero==HeroClass.WARRIOR) {
                        game.getPlayer().attack(2,false,.5f);for(int i=0;i<11;i++)frame(game);
                        view._position().x(-2).y(3.2f).z(-1);view.target().x(-7).y(1.1f).z(-6);
                        for(int i=0;i<3;i++){BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();}capture("player-warrior-swing");
                    }
                    game.photoMode(false);
                }
                System.out.println("PASS: class-fitted armor across all tiers, running, dodging, and player gallery");return;
            }
            if(java.util.Arrays.asList(args).contains("--visual-only")) {
                key(game,KEY_ENTER);key(game,KEY_ENTER);for(int i=0;i<12;i++)frame(game);capture("modern-materials");check(game.shadowAtlasValid(),"Shadow atlas did not capture the sanctuary caster");
                for(int bossWave:new int[]{5,10}) {
                    Entities.Enemy modelBoss=new Entities.Enemy(0,13,bossWave,5,.5f);
                    SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Warden",0,8,0,new Progression(),new WaveDirector.State(bossWave,0,1,8,0,21,0),java.util.List.of(modelBoss),Arena.HIGHLANDS));
                    check(game.loadCurrent(),"Boss preview load failed");key(game,KEY_ENTER);
                    for(int i=0;i<4;i++)frame(game);
                    Camera3D review=game.getSceneManager().getOpenScene().getCameraController().getCamera();
                    review._position().x(10).y(6).z(1);review.target().x(0).y(2).z(11);
                    for(int i=0;i<3;i++) { BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing(); }
                    capture("modern-boss-"+bossWave);
                }
                for(int chapter=0;chapter<4;chapter++) {
                    int stageWave=new int[]{1,5,6,10}[chapter];
                    Entities.Enemy stageEnemy=new Entities.Enemy(5,14,stageWave,chapter==1||chapter==3?5:chapter==2?4:0,.5f);
                    SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Warden",0,8,0,new Progression(),new WaveDirector.State(stageWave,0,1,8,0,21,0),java.util.List.of(stageEnemy),Arena.HIGHLANDS));
                    check(game.loadCurrent(),"Stage preview load failed");key(game,KEY_ENTER);for(int i=0;i<4;i++)frame(game);
                    Camera3D view=game.getSceneManager().getOpenScene().getCameraController().getCamera();
                    view._position().x(20).y(24).z(28);view.target().x(0).y(0).z(0);
                    for(int i=0;i<3;i++) { BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing(); }
                    capture("expansion-chapter-"+chapter);
                }
                Progression gear=game.getProgression();for(int i=0;i<300;i++)gear.rewardKill();gear.buyWeapon(3);gear.buyArmor(3);gear.equipWeapon(3);
                for(Entities.ArmorSlot slot:Entities.ArmorSlot.values()) gear.equipArmor(slot,3);
                gear.purchase(Progression.Upgrade.ENDURANCE);gear.purchase(Progression.Upgrade.VITALITY);
                key(game,KEY_TAB);for(int i=0;i<4;i++)frame(game);capture("expansion-inventory");
                key(game,KEY_ESCAPE);game.getPlayer().getPosition().x(0).z(4);key(game,KEY_E);for(int i=0;i<4;i++)frame(game);capture("expansion-workshop");
                int wx=(GetScreenWidth()-940)/2,wy=(GetScreenHeight()-600)/2;
                click(game,wx+110,wy+202);capture("polish-forge");click(game,wx+110,wy+262);capture("polish-supplies");
                key(game,KEY_ESCAPE);
                for(HeroClass hero:HeroClass.values()) {
                    Progression dressed=new Progression(hero,Entities.Difficulty.HARD);for(int i=0;i<300;i++)dressed.rewardKill();
                    dressed.buyWeapon(3);dressed.equipWeapon(3);dressed.buyArmor(3);
                    for(Entities.ArmorSlot slot:Entities.ArmorSlot.values())dressed.equipArmor(slot,3);
                    SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Warden",0,8,0,dressed,new WaveDirector.State(1,0,1,8,0,21,0),java.util.List.of(new Entities.Enemy(0,20,1,0,.5f,Entities.Difficulty.HARD)),Arena.HIGHLANDS));
                    check(game.loadCurrent(),"Class gear preview load failed");key(game,KEY_ENTER);key(game,KEY_TAB);
                    for(int i=0;i<3;i++)frame(game);capture("expansion-gear-"+hero.name().toLowerCase());key(game,KEY_ESCAPE);
                }
                Entities.Enemy effectsBoss=new Entities.Enemy(4,12,10,5,.5f);
                SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Warden",-2,8,50,new Progression(),new WaveDirector.State(10,0,1,8,0,21,0),java.util.List.of(effectsBoss),Arena.HIGHLANDS));
                check(game.loadCurrent(),"Combat effects review load failed");key(game,KEY_ENTER);
                effectsBoss=game.getEnemies().get(0);effectsBoss.cooldown=100;effectsBoss.beginAttack(-2,8,true);
                while(effectsBoss.bossMove!=Entities.BossMove.CHARGE)effectsBoss.beginAttack(-2,8,true);
                effectsBoss.attackTime=0;effectsBoss.impactPending=false;effectsBoss.cooldown=0;frame(game);
                check(!game.getBossHazards().isEmpty(),"Boss did not create its visual-review sigils");
                Camera3D effectsCamera=game.getSceneManager().getOpenScene().getCameraController().getCamera();
                effectsCamera._position().x(-12).y(9).z(18);effectsCamera.target().x(1).y(1).z(10);
                for(int i=0;i<3;i++){BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();}
                capture("polish-sigil-warning");
                int detonationFrames=(int)Math.ceil((game.getBossHazards().get(0).delay+.08f)*60);
                for(int i=0;i<detonationFrames;i++)frame(game);
                effectsCamera._position().x(-12).y(9).z(18);effectsCamera.target().x(1).y(1).z(10);
                for(int i=0;i<3;i++){BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();}
                capture("polish-sigil-impact");
                System.out.println("PASS: modern visual review");return;
            }
            for (int i = 0; i < 5; i++) frame(game);
            check(game.weaponMaterialsValid(),"Generated weapon materials loaded black");
            check(!game.audio().ready() || game.audio().loadedMusic()==6 && game.audio().musicStreaming(),"Music assets missing or streams not playing");
            check(game.audio().theme()==4,"Title music theme not selected");
            capture("ui-menu-retina");
            // Click the center of the visible Begin Defense button, in window pixels.
            event(MOUSE_POSITION, (GetScreenWidth()-640)/2+140, (GetScreenHeight()-420)/2+307);
            event(MOUSE_DOWN, MOUSE_BUTTON_LEFT, 0); frame(game);
            check(game.getState() == GameState.LOGIN, "Name entry did not open");
            for(int i=0;i<3;i++)frame(game);
            capture("ui-name-entry");
            event(MOUSE_UP, MOUSE_BUTTON_LEFT, 0); frame(game);
            key(game, KEY_ENTER);
            check(game.getPlayer().getName().equals("Ada"),"Player name not applied");
            check(game.getState() == GameState.PLAYING, "Begin Defense click failed");
            check(game.arena()==Arena.HIGHLANDS,"New games should show the imported-model sanctuary immediately");
            event(MOUSE_UP, MOUSE_BUTTON_LEFT, 0); frame(game);
            float initialZ = game.getPlayer().getPosition().z();
            event(KEY_DOWN, KEY_W, 0);
            for (int i=0;i<15;i++) frame(game);
            event(KEY_UP, KEY_W, 0); frame(game);
            check(game.getPlayer().getPosition().z() < initialZ - .5f, "W movement failed");
            key(game, KEY_SPACE);
            check(game.getPlayer().isAttacking(), "Attack input failed");
            key(game, KEY_E);
            check(game.isWorkshopOpen(), "E did not open workshop");
            capture("ui-workshop-retina");
            float stoppedZ = game.getPlayer().getPosition().z();
            event(KEY_DOWN, KEY_W, 0); frame(game);
            event(KEY_UP, KEY_W, 0); frame(game);
            check(game.getPlayer().getPosition().z() == stoppedZ, "Workshop did not stop movement");
            key(game, KEY_ESCAPE);
            check(!game.isWorkshopOpen(), "Escape did not close workshop");
            key(game, KEY_ESCAPE);
            check(game.getState() == GameState.PAUSED, "Escape did not pause");
            key(game, KEY_ENTER);
            check(game.getState() == GameState.PLAYING, "Enter did not resume");
            SetWindowSize(1000, 680);
            for (int i=0;i<5;i++) frame(game);
            capture("ui-hud-small");
            game.setState(GameState.MAIN_MENU);
            for (int i=0;i<3;i++) frame(game);
            capture("ui-menu-small");
            event(MOUSE_POSITION, (GetScreenWidth()-640)/2+140, (GetScreenHeight()-420)/2+307);
            event(MOUSE_DOWN, MOUSE_BUTTON_LEFT, 0); frame(game);
            check(game.getState() == GameState.LOGIN, "Resized name entry failed");
            event(MOUSE_UP, MOUSE_BUTTON_LEFT, 0); frame(game);
            key(game, KEY_ENTER);
            check(game.getState() == GameState.PLAYING, "Resized menu click failed");
            event(MOUSE_UP, MOUSE_BUTTON_LEFT, 0); frame(game);
            game.setState(GameState.MAIN_MENU); frame(game);
            key(game, KEY_ENTER);
            check(game.getState() == GameState.LOGIN, "Enter did not ask for name");
            key(game, KEY_ENTER);
            check(game.getState() == GameState.PLAYING, "Enter did not start");
            for (int i=0;i<720;i++) frame(game);
            capture("ui-raiders");
            key(game, KEY_SPACE);
            float startHand = handHeight(game);
            for (int i=0;i<12;i++) frame(game);
            check(Math.abs(handHeight(game)-startHand) > .01f, "Attack did not animate sword hand");
            check(game.getPlayer().isAttacking(), "Sword swing ended too early");
            capture("ui-sword-swing");
            for (int i=0;i<20;i++) frame(game);
            check(!game.getPlayer().isAttacking(), "Sword swing did not finish: "+game.getState()+" remaining "+game.getPlayer().attackRemaining()+" clip "+game.getPlayer().getAnimationIndex());
            combatClick.set(true);frame(game);
            check(game.getPlayer().isAttacking(), "Mouse combat attack did not restart");
            for (int i=0;i<45;i++) frame(game);
            check(!game.getEnemies().isEmpty(), "Staggered wave did not spawn enemies");
            // Put a living raider in front, with a long cooldown to isolate player impact.
            Entities.Enemy target=game.getEnemies().get(0);
            game.getPlayer().getPosition().x(0).z(8);
            game.getPlayer().getRotation().y(0);
            for (Entities.Enemy enemy:game.getEnemies()) { enemy.x=18; enemy.z=-18; enemy.cooldown=10; enemy.attackTime=0; enemy.impactPending=false; }
            target.x=0; target.z=10; target.health=200; target.stun=2;
            float before=target.health;
            key(game,KEY_Q);
            check(target.health==before, "Heavy attack dealt damage before impact");
            for (int i=0;i<24;i++) frame(game);
            check(target.health<before, "Heavy attack did not hit the enemy in front");
            capture("ui-damage-dealt");
            float after=target.health;
            for (int i=0;i<34;i++) frame(game);
            check(target.health==after, "One swing applied damage more than once");
            target.health=target.maxHealth; target.x=0; target.z=6; target.stun=2;
            key(game,KEY_SPACE);
            for (int i=0;i<20;i++) frame(game);
            check(target.health==target.maxHealth, "Sword hit enemy behind player");
            for (int i=0;i<20;i++) frame(game);
            target.stun=0; target.x=0; target.z=9.5f;
            target.beginAttack(0,8,true);
            float hp=game.getProgression().health();
            for (int i=0;i<30;i++) frame(game);
            check(game.getProgression().health()<hp, "Telegraphed enemy attack did not deal damage");
            capture("ui-damage-taken");
            key(game,KEY_M);check(game.audio().muted(),"Mute did not affect the music/effects mixer");
            // Save/load restores state, and corrupted files do not replace a live defense.
            target.health=target.maxHealth;
            for (Entities.Enemy enemy:game.getEnemies()) enemy.cooldown=1;
            check(game.saveCurrent(),"Save failed");
            float savedHealth=game.getProgression().health();
            game.getProgression().damagePlayer(20);
            check(game.loadCurrent(),"Load failed");
            check(game.getState()==GameState.PAUSED,"Loading should pause");
            check(game.getProgression().health()==savedHealth,"Load did not restore health");
            capture("ui-save-loaded");
            java.nio.file.Files.writeString(profile.resolve("save.properties"),"version=999");
            check(!game.loadCurrent(),"Invalid save was accepted");
            check(game.getProgression().health()==savedHealth,"Failed load changed game");
            // Open pause settings and remap forward to I through real UI events.
            int sx=(GetScreenWidth()-440)/2,sy=(GetScreenHeight()-440)/2;
            event(MOUSE_POSITION,sx+100,sy+230);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
            event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);
            check(game.getState()==GameState.SETTING_MENU,"Settings button failed");
            for(int i=0;i<3;i++)frame(game);
            capture("ui-settings");
            int bx=(GetScreenWidth()-820)/2,by=(GetScreenHeight()-650)/2;
            event(MOUSE_POSITION,bx+100,by+230);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
            event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);key(game,KEY_I);
            key(game,KEY_ESCAPE);
            Settings persisted=new Settings();persisted.load(profile.resolve("settings.properties"));
            check(persisted.controls.key(Config.Controls.Action.FORWARD)==KEY_I,"Rebinding did not persist");
            key(game,KEY_ENTER);
            game.getSceneManager().getOpenScene().getCameraController().setSensitivity(0);
            float startZ=game.getPlayer().getPosition().z();
            event(KEY_DOWN,KEY_I,0);for(int i=0;i<12;i++)frame(game);event(KEY_UP,KEY_I,0);frame(game);
            check(game.getPlayer().getPosition().z()<startZ-.4f,"Remapped movement failed");
            Entities.Enemy archer=new Entities.Enemy(-5,8,3,3,.4f), shaman=new Entities.Enemy(6,9,3,4,.7f);
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Ada",0,8,0,new Progression(),new WaveDirector.State(3,0,2,8,0,21,0),java.util.List.of(archer,shaman)));
            check(game.loadCurrent(),"Ranged enemy save did not load");key(game,KEY_ENTER);
            for(int i=0;i<25;i++)frame(game);
            capture("ui-ranged-enemies");
            for(int i=0;i<55;i++)frame(game);
            capture("ui-projectiles");
            // Exercise class selection, ranged impact, and inventory through real UI/input.
            game.setState(GameState.MAIN_MENU);key(game,KEY_ENTER);
            int hx=(GetScreenWidth()-940)/2+24,hy=(GetScreenHeight()-600)/2;
            event(MOUSE_POSITION,hx+280,hy+238);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
            event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);key(game,KEY_ENTER);
            check(game.getProgression().heroClass()==HeroClass.ARCHER,"Archer selection failed");
            game.getPlayer().getPosition().x(0).z(8);game.getPlayer().getRotation().y(0);
            Entities.Enemy rangedTarget=new Entities.Enemy(0,14,1);
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Ada",0,8,0,game.getProgression(),new WaveDirector.State(1,0,1,8,0,21,0),java.util.List.of(rangedTarget)));
            check(game.loadCurrent(),"Archer load failed");key(game,KEY_ENTER);
            Entities.Enemy loadedTarget=game.getEnemies().get(0);loadedTarget.stun=5;loadedTarget.cooldown=5;
            for(int i=0;i<20;i++)frame(game);
            float targetHp=loadedTarget.health;key(game,KEY_SPACE);
            for(int i=0;i<35;i++)frame(game);
            check(loadedTarget.health<targetHp,"Player arrow failed to hit at range");capture("ui-archer-hero");
            // Side view makes the bow's grip/string orientation reviewable.
            Camera3D bowCamera=game.getSceneManager().getOpenScene().getCameraController().getCamera();
            bowCamera._position().x(4).y(2.6f).z(10);
            bowCamera.target().x(0).y(1.2f).z(8);
            for(int i=0;i<3;i++) { BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing(); }
            capture("ui-bow-side");
            game.getSceneManager().getOpenScene().getCameraController().update(0);
            game.getProgression().damagePlayer(65);
            key(game,KEY_TAB);float frozen=game.getProgression().health(),frozenPhase=game.dayPhase();
            event(KEY_DOWN,KEY_I,0);for(int i=0;i<10;i++)frame(game);event(KEY_UP,KEY_I,0);frame(game);
            check(game.getProgression().health()==frozen,"Inventory did not pause simulation");check(game.dayPhase()==frozenPhase,"Inventory did not pause the solar cycle");capture("ui-inventory");
            key(game,KEY_ESCAPE);int potions=game.getProgression().potions();key(game,KEY_H);
            check(game.getProgression().potions()==potions-1,"Potion hotkey failed");
            check(game.saveCurrent() && game.loadCurrent(),"Class/inventory save failed");
            check(game.getProgression().heroClass()==HeroClass.ARCHER && game.getProgression().potions()==potions-1,"Class/inventory did not restore");
            Progression mage=new Progression(HeroClass.MAGE);
            Entities.Enemy first=new Entities.Enemy(0,12,1),second=new Entities.Enemy(0,15,1);
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Mage",0,8,0,mage,new WaveDirector.State(1,0,2,8,0,21,0),java.util.List.of(first,second)));
            check(game.loadCurrent(),"Mage load failed");key(game,KEY_ENTER);
            for(Entities.Enemy enemy:game.getEnemies()) { enemy.stun=5;enemy.cooldown=5; }
            for(int i=0;i<20;i++)frame(game);key(game,KEY_Q);
            for(int i=0;i<65;i++)frame(game);
            check(game.getEnemies().size()<2 || game.getEnemies().stream().allMatch(e->e.health<e.maxHealth),"Mage surge did not pierce both targets");
            capture("ui-mage-hero");
            // Boss attacks, loot collection, real inventory equipment buttons, and saves.
            Entities.Enemy warlord=new Entities.Enemy(0,10,5,5,.5f);
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Warden",0,8,0,new Progression(),new WaveDirector.State(5,0,1,8,0,21,0),java.util.List.of(warlord)));
            check(game.loadCurrent(),"Warlord save failed to load");key(game,KEY_ENTER);
            Entities.Enemy liveBoss=game.getEnemies().get(0);liveBoss.beginAttack(0,8,true);
            for(int i=0;i<58;i++)frame(game);
            check(game.getProgression().health()<100,"Warlord impact failed");capture("ui-warlord");
            liveBoss.health=1;key(game,KEY_Q);for(int i=0;i<35;i++)frame(game);
            check(game.getProgression().weaponUnlocked()==1,"Boss did not unlock loot");
            check(game.getProgression().crystals()==120,"Boss crystal reward incorrect");
            key(game,KEY_TAB);
            int ix=(GetScreenWidth()-940)/2,iy=(GetScreenHeight()-600)/2;
            event(MOUSE_POSITION,ix+507,iy+122);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
            event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);
            check(game.getProgression().weaponTier()==0,"Selecting an item should compare before equipping");
            click(game,ix+530,iy+430);
            check(game.getProgression().weaponTier()==1 && game.getProgression().damage()==33,"Inventory equip failed");for(int i=0;i<3;i++)frame(game);capture("ui-weapon-loot");
            key(game,KEY_ESCAPE);check(game.saveCurrent() && game.loadCurrent(),"Equipment save failed");
            check(game.getProgression().weaponTier()==1,"Equipment did not persist");
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Highlands",0,8,0,new Progression(HeroClass.MAGE),new WaveDirector.State(6,0,0,8,0,21,0),java.util.List.of()));
            check(game.loadCurrent(),"Highland save failed");
            check(game.arena()==Arena.HIGHLANDS,"Wave 6 must restore the second stage");
            key(game,KEY_ENTER);for(int i=0;i<5;i++)frame(game);check(game.audio().theme()==2,"Frozen Pass music did not follow restored chapter");capture("ui-highland-ruins");
            check(game.saveCurrent() && game.loadCurrent(),"Sanctuary save failed");
            check(game.arena()==Arena.HIGHLANDS,"Sanctuary selection was lost");key(game,KEY_ENTER);
            check(game.getPlayer().getAnimationCount()>3,"Hero animation clips missing");
            check(game.saveCurrent() && game.loadCurrent() && game.arena()==Arena.HIGHLANDS,"Second stage did not persist");
            key(game,KEY_ENTER);
            Camera3D overview=game.getSceneManager().getOpenScene().getCameraController().getCamera();
            overview._position().x(30).y(30).z(34);overview.target().x(0).y(0).z(0);
            for(int i=0;i<3;i++) { BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing(); }
            capture("ui-nature-overview");
            // Sprint, dodge cooldown, world bounds, and the warrior's distinct attack clips.
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Warden",0,8,180,new Progression(),new WaveDirector.State(1,0,0,8,0,21,0),java.util.List.of(),Arena.HIGHLANDS));
            check(game.loadCurrent(),"Movement profile failed");key(game,KEY_ENTER);
            game.getProgression().damagePlayer(20);game.getPlayer().getPosition().x(0).z(5.8f);
            float havenHp=game.getProgression().health();for(int i=0;i<30;i++)frame(game);
            check(game.getProgression().health()>havenHp+1,"Inside the marked healing ring did not heal");
            game.getPlayer().getPosition().z(6.2f);float outsideHp=game.getProgression().health();
            for(int i=0;i<30;i++)frame(game);
            check(game.getProgression().health()==outsideHp,"Healing extended outside the marked six-unit zone");
            game.getPlayer().getPosition().x(0).z(8);
            float walkX=game.getPlayer().getPosition().x(),walkZ=game.getPlayer().getPosition().z();
            event(KEY_DOWN,KEY_I,0);for(int i=0;i<30;i++)frame(game);event(KEY_UP,KEY_I,0);frame(game);
            float walked=(float)Math.hypot(game.getPlayer().getPosition().x()-walkX,game.getPlayer().getPosition().z()-walkZ);
            game.getPlayer().getPosition().x(walkX).z(walkZ);game.getPlayer().resetCombat();
            event(KEY_DOWN,KEY_LEFT_SHIFT,0);event(KEY_DOWN,KEY_I,0);for(int i=0;i<30;i++)frame(game);
            check(game.getPlayer().isSprinting(),"Sprint state did not engage");
            check(Math.hypot(game.getPlayer().getPosition().x()-walkX,game.getPlayer().getPosition().z()-walkZ)>walked*1.4f,"Sprint did not increase movement speed");
            capture("ui-sprint");event(KEY_UP,KEY_I,0);event(KEY_UP,KEY_LEFT_SHIFT,0);frame(game);
            game.getPlayer().resetCombat();game.getPlayer().getPosition().x(0).z(8);
            float dodgeX=game.getPlayer().getPosition().x(),dodgeZ=game.getPlayer().getPosition().z();
            key(game,KEY_LEFT_CONTROL);check(game.getPlayer().isDodging(),"Dodge did not start");
            for(int i=0;i<8;i++)frame(game);capture("ui-dodge");
            for(int i=0;i<15;i++)frame(game);
            check(Math.hypot(game.getPlayer().getPosition().x()-dodgeX,game.getPlayer().getPosition().z()-dodgeZ)>3,"Dodge did not move player");
            key(game,KEY_LEFT_CONTROL);check(!game.getPlayer().isDodging(),"Dodge ignored its cooldown");
            for(int i=0;i<100;i++)frame(game);
            game.getPlayer().getPosition().x(21.9f).z(8);game.getPlayer().getRotation().y(90);
            key(game,KEY_LEFT_CONTROL);for(int i=0;i<25;i++)frame(game);
            check(game.getPlayer().getPosition().x()<=22 && game.getPlayer().getPosition().z()<=22,"Dodge escaped the visual-only boundary");
            game.getPlayer().getPosition().x(0).z(8);game.getPlayer().resetCombat();
            java.util.Set<Integer> swordClips=new java.util.HashSet<>();
            for(int combo=1;combo<=3;combo++) {
                game.getPlayer().attack(combo,false,.5f);swordClips.add(game.getPlayer().getAnimationIndex());
                float[] beforeHand=handPose(game);for(int i=0;i<11;i++)frame(game);float[] afterHand=handPose(game);
                check(Math.abs(beforeHand[0]-afterHand[0])+Math.abs(beforeHand[1]-afterHand[1])+Math.abs(beforeHand[2]-afterHand[2])>.08f,"Warrior swing "+combo+" did not animate its weapon hand");
                capture("ui-warrior-combo-"+combo);
                for(int i=0;i<30;i++)frame(game);
            }
            check(swordClips.size()==3,"Warrior combo needs three distinct clips");
            game.getPlayer().attack(1,true,.9f);check(!swordClips.contains(game.getPlayer().getAnimationIndex()),"Heavy cleave should use its own animation");
            for(int i=0;i<20;i++)frame(game);capture("ui-warrior-heavy");for(int i=0;i<40;i++)frame(game);
            // Low orbit shows the sky and the terrain outside the movement boundary.
            Camera3D horizon=game.getSceneManager().getOpenScene().getCameraController().getCamera();
            horizon._position().x(12).y(5).z(22);horizon.target().x(0).y(5).z(0);
            for(int i=0;i<3;i++) { BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing(); }
            capture("ui-living-horizon");
            // Chapter completion must pause; the next chapter spawns exactly one boss.
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Chapter",0,4,0,new Progression(),new WaveDirector.State(4,0,16,8,0,21,0),java.util.List.of(),Arena.HIGHLANDS));
            check(game.loadCurrent() && game.getState()==GameState.LEVEL_COMPLETE,"Chapter-end save did not restore its story screen");
            for(int i=0;i<3;i++)frame(game);capture("ui-chapter-complete");
            key(game,KEY_ENTER);check(game.chapterIndex()==1 && game.getState()==GameState.PLAYING,"Continue failed to enter boss chapter");
            check(game.saveCurrent() && game.loadCurrent(),"Next chapter save failed");key(game,KEY_ENTER);
            for(int i=0;i<600 && game.getEnemies().isEmpty();i++)frame(game);
            check(game.getEnemies().size()==1 && game.getEnemies().get(0).boss(),"Boss chapter should contain exactly one boss");
            for(int i=0;i<4;i++)frame(game);capture("ui-boss-only-level");
            game.getEnemies().get(0).health=0;frame(game);
            check(game.getState()==GameState.LEVEL_COMPLETE,"Boss defeat did not finish its chapter");
            key(game,KEY_ENTER);check(game.chapterIndex()==2,"Second siege did not follow boss chapter");
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Dawn",0,4,0,new Progression(),new WaveDirector.State(10,0,1,8,0,21,0),java.util.List.of(),Arena.HIGHLANDS));
            check(game.loadCurrent() && game.getState()==GameState.VICTORY,"Final completion did not restore victory");
            for(int i=0;i<3;i++)frame(game);capture("ui-campaign-victory");
            key(game,KEY_ENTER);check(game.getState()==GameState.MAIN_MENU,"Victory did not return to menu");
            int mx=(GetScreenWidth()-640)/2,my=(GetScreenHeight()-420)/2;
            event(MOUSE_POSITION,mx+420,my+366);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
            event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);
            check(game.getState()==GameState.JOURNAL,"Chronicle button failed");
            for(int i=0;i<3;i++)frame(game);capture("ui-chronicle");
            key(game,KEY_ENTER);check(game.getState()==GameState.MAIN_MENU,"Chronicle return failed");
            // New combat mechanics through real input and live damage resolution.
            Entities.Enemy parryTarget=encounter(game,profile,1,0,9.8f);
            parryTarget.beginAttack(0,8,true);parryTarget.attackTime=.33f;
            float parryHp=game.getProgression().health();key(game,KEY_F);for(int i=0;i<8;i++)frame(game);
            check(game.getProgression().health()==parryHp && game.getPlayer().discipline().riposteReady(),"Timed parry did not defend and open a riposte");
            key(game,KEY_SPACE);check(game.currentAttack().label().equals("RIPOSTE"),"Parry reward was not consumed by the next strike");
            for(int i=0;i<40;i++)frame(game);check(parryTarget.health<=7,"Riposte did not increase damage");
            game.getPlayer().discipline().stamina(0);key(game,KEY_SPACE);
            check(!game.getPlayer().isAttacking(),"Empty stamina still allowed an attack");
            check(game.saveCurrent(),"Stamina save failed");
            check(SaveGame.read(profile.resolve("save.properties")).stamina()==game.getPlayer().discipline().stamina(),"Stamina did not persist");
            Entities.Enemy backstab=encounter(game,profile,1,0,9.8f);
            game.getPlayer().getRotation().y(180);backstab.beginAttack(0,8,true);backstab.attackTime=.33f;
            key(game,KEY_F);for(int i=0;i<8;i++)frame(game);
            check(game.getProgression().health()<100 && !game.getPlayer().discipline().riposteReady(),"Parry incorrectly blocked an attack behind the hero");
            Entities.Enemy comboTarget=encounter(game,profile,3,0,10);
            key(game,KEY_SPACE);for(int i=0;i<25;i++)frame(game);key(game,KEY_SPACE);for(int i=0;i<8;i++)frame(game);
            check(game.currentAttack().label().startsWith("TIMED CHAIN 2"),"Late combo input did not chain");
            Entities.Enemy chargedTarget=encounter(game,profile,3,0,10);
            event(KEY_DOWN,KEY_Q,0);for(int i=0;i<48;i++)frame(game);event(KEY_UP,KEY_Q,0);frame(game);
            check(game.currentAttack().label().equals("PERFECT CHARGE"),"Charge timing window did not reward a release");
            for(int i=0;i<30;i++)frame(game);check(chargedTarget.health==0,"Perfect charge did not apply bonus damage");capture("modern-perfect-charge");
            Entities.Enemy lungeTarget=encounter(game,profile,1,0,11.7f);
            key(game,KEY_R);for(int i=0;i<26;i++)frame(game);
            check(game.getPlayer().getPosition().z()>9 && lungeTarget.health<lungeTarget.maxHealth,"Lunge did not advance and hit at extended reach");capture("modern-lunge");
            Entities.Enemy slamBoss=encounter(game,profile,5,5,14);
            slamBoss.beginAttack(0,8,true);slamBoss.beginAttack(0,8,true);
            check(slamBoss.bossMove==Entities.BossMove.SLAM,"Boss slam pattern changed");slamBoss.attackTime=.30f;
            key(game,KEY_F);for(int i=0;i<6;i++)frame(game);
            check(game.getProgression().health()<100 && !game.getPlayer().discipline().riposteReady(),"Unblockable slam was incorrectly parried");
            check(game.getBossHazards().stream().anyMatch(h->h.kind==BossHazard.Kind.SHOCKWAVE),"Slam did not emit its shockwave");capture("modern-boss-slam");
            Entities.Enemy rushBoss=encounter(game,profile,5,5,14);rushBoss.beginAttack(0,8,true);rushBoss.attackTime=.30f;
            key(game,KEY_LEFT_CONTROL);for(int i=0;i<22;i++)frame(game);
            check(game.getProgression().health()==100,"A correctly timed dodge failed against the charge");capture("modern-charge-dodge");
            Entities.Enemy parryBoss=encounter(game,profile,5,5,14);parryBoss.beginAttack(0,8,true);parryBoss.attackTime=.30f;
            for(int i=0;i<16;i++)frame(game);key(game,KEY_F);for(int i=0;i<10;i++)frame(game);
            check(parryBoss.chargeTime==0 && game.getPlayer().discipline().riposteReady(),"Charge parry did not stop the boss");
            Entities.Enemy sigilBoss=encounter(game,profile,10,5,14);
            while(sigilBoss.bossMove!=Entities.BossMove.CHARGE) sigilBoss.beginAttack(0,8,true);
            sigilBoss.stun=0;sigilBoss.attackTime=0;sigilBoss.impactPending=false;sigilBoss.cooldown=0;
            frame(game);check(sigilBoss.bossMove==Entities.BossMove.SIGILS && game.getBossHazards().size()==3,"King did not cast a warned sigil pattern");
            for(int i=0;i<5;i++)frame(game);check(game.getProgression().health()==100,"Sigils dealt damage during their warning");capture("modern-king-sigils");
            for(int i=0;i<92;i++)frame(game);check(game.getProgression().health()<100,"Sigils did not detonate");
            sigilBoss.health=sigilBoss.maxHealth*.4f;check(sigilBoss.enraged(),"Boss phase two did not activate");for(int i=0;i<4;i++)frame(game);capture("modern-king-enraged");
            sigilBoss.stun=100;sigilBoss.cooldown=100;sigilBoss.attackTime=0;sigilBoss.impactPending=false;
            for(int i=0;i<35;i++)frame(game);
            sigilBoss.x=0;sigilBoss.z=10;
            while(sigilBoss.bossMove!=Entities.BossMove.CHARGE) sigilBoss.beginAttack(0,8,true);
            sigilBoss.stun=0;sigilBoss.cooldown=0;sigilBoss.attackTime=0;sigilBoss.impactPending=false;frame(game);
            check(game.getBossHazards().size()==5,"Enraged King did not expand its sigil pattern");
            for(int i=0;i<4;i++)frame(game);capture("modern-enraged-sigils");
            // Graphics quality must recreate the atlas and scene buffers without breaking UI or shadows.
            key(game,KEY_ESCAPE);
            int gx=(GetScreenWidth()-440)/2,gy=(GetScreenHeight()-440)/2;
            event(MOUSE_POSITION,gx+100,gy+230);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
            event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);check(game.getState()==GameState.SETTING_MENU,"Graphics settings did not open");
            int qx=(GetScreenWidth()-820)/2,qy=(GetScreenHeight()-650)/2;
            event(MOUSE_POSITION,qx+350,qy+510);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
            event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);key(game,KEY_ESCAPE);key(game,KEY_ENTER);
            Settings balanced=new Settings();balanced.load(profile.resolve("settings.properties"));check(!balanced.highQuality,"Balanced graphics choice did not persist");
            for(int i=0;i<4;i++)frame(game);capture("modern-balanced");check(game.shadowAtlasValid(),"Balanced quality lost its shadow caster");
            // New-game difficulty controls, independent training, forge and slot interactions.
            for(Entities.Difficulty difficulty:Entities.Difficulty.values()) {
                game.setState(GameState.MAIN_MENU);key(game,KEY_ENTER);
                int dx=(GetScreenWidth()-940)/2+24,dy=(GetScreenHeight()-600)/2;
                click(game,dx+difficulty.ordinal()*142+60,dy+372);key(game,KEY_ENTER);
                check(game.getProgression().difficulty()==difficulty,"Difficulty button failed: "+difficulty);
                Settings chosen=new Settings();chosen.load(profile.resolve("settings.properties"));
                check(chosen.difficulty==difficulty,"Difficulty preference did not persist");
            }
            Progression equipment=game.getProgression();for(int i=0;i<300;i++)equipment.rewardKill();
            game.getPlayer().getPosition().x(0).z(4);key(game,KEY_E);check(game.isWorkshopOpen(),"Workshop did not open");
            int ex=(GetScreenWidth()-940)/2,ey=(GetScreenHeight()-600)/2;
            click(game,ex+830,ey+162);click(game,ex+830,ey+214);click(game,ex+830,ey+266);
            check(equipment.maxHealth()==120 && equipment.damage()==30 && equipment.maxStamina()==115,"Training cards did not independently upgrade HP, damage and stamina");
            click(game,ex+110,ey+202);click(game,ex+820,ey+335);click(game,ex+820,ey+477);
            check(equipment.weaponUnlocked()==3 && equipment.armorUnlocked()==3,"Forge purchase failed");capture("expansion-forge");
            click(game,ex+110,ey+282);int beforeRestock=equipment.potions();click(game,ex+520,ey+390);
            check(equipment.potions()==beforeRestock+1,"Supplies sidebar or potion purchase failed");capture("polish-supplies");
            key(game,KEY_ESCAPE);key(game,KEY_TAB);
            click(game,ex+658,ey+122);click(game,ex+530,ey+430);check(equipment.weaponTier()==3,"Legendary weapon grid selection failed");
            // Tier-three helmet: drag from the grid into the head slot.
            event(MOUSE_POSITION,ex+433,ey+274);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
            event(MOUSE_POSITION,ex+332,ey+122);frame(game);event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);
            check(equipment.armorTier(Entities.ArmorSlot.HEAD)==3,"Armor drag did not equip into head slot");
            event(MOUSE_DOWN,MOUSE_BUTTON_RIGHT,0);frame(game);event(MOUSE_UP,MOUSE_BUTTON_RIGHT,0);frame(game);
            check(equipment.armorTier(Entities.ArmorSlot.HEAD)==0,"Right-click did not remove armor");
            for(int item=12;item<16;item++) { click(game,ex+433+(item%6)*75,ey+122+(item/6)*76);key(game,KEY_ENTER); }
            check(equipment.defense()==21,"Armor grid did not equip the complete set");
            click(game,ex+650,ey+68);click(game,ex+433,ey+122);key(game,KEY_ENTER);check(equipment.weaponTier()==0,"Weapon filter/keyboard equip failed");
            click(game,ex+658,ey+122);click(game,ex+658,ey+122);check(equipment.weaponTier()==3,"Double-click equip failed");
            click(game,ex+850,ey+68);click(game,ex+433,ey+122);key(game,KEY_ENTER);check(equipment.armorTier(Entities.ArmorSlot.HEAD)==1,"Armor filter equip failed");
            click(game,ex+700,ey+507);check(equipment.defense()==21,"Best Gear did not restore the full armor set");
            click(game,ex+480,ey+68);
            for(int i=0;i<3;i++)frame(game);capture("expansion-inventory");key(game,KEY_ESCAPE);
            game.getPlayer().discipline().stamina(110);check(game.saveCurrent() && game.loadCurrent(),"Expanded equipment save failed");
            check(game.getProgression().difficulty()==Entities.Difficulty.EXTREME && game.getProgression().defense()==21 && game.getProgression().weaponTier()==3,"Difficulty or equipment lost on load");
            check(game.getPlayer().discipline().maximum()==115 && game.getPlayer().discipline().stamina()==110,"Upgraded stamina failed to restore");
            // A final hit must finish its visible swing before the chapter screen interrupts it.
            Entities.Enemy finalSwing=encounter(game,profile,5,5,10);finalSwing.health=1;
            key(game,KEY_SPACE);for(int i=0;i<14;i++)frame(game);
            check(finalSwing.health==0 && game.getPlayer().isAttacking() && game.getState()==GameState.PLAYING,"Chapter completion cut off the finishing swing");
            for(int i=0;i<30;i++)frame(game);check(game.getState()==GameState.LEVEL_COMPLETE,"Chapter did not complete after the swing finished");
            // Exercise the crossbow clip/socket through a real ranged hit.
            Progression crossbowHero=new Progression(HeroClass.ARCHER);for(int i=0;i<100;i++)crossbowHero.rewardKill();crossbowHero.buyWeapon(3);crossbowHero.equipWeapon(3);
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Archer",0,8,0,crossbowHero,new WaveDirector.State(6,0,1,8,0,21,0),java.util.List.of(new Entities.Enemy(0,14,6)),Arena.HIGHLANDS));
            check(game.loadCurrent(),"Crossbow profile did not load");key(game,KEY_ENTER);
            Entities.Enemy crossbowTarget=game.getEnemies().get(0);crossbowTarget.stun=100;crossbowTarget.cooldown=100;
            for(int i=0;i<20;i++)frame(game);float crossbowHp=crossbowTarget.health;key(game,KEY_SPACE);
            for(int i=0;i<35;i++)frame(game);check(crossbowTarget.health<crossbowHp,"Equipped crossbow did not hit");capture("expansion-crossbow-world");
            // Frost hexers channel a real ward; interruption cancels the pending heal.
            Entities.Enemy hexer=new Entities.Enemy(6,10,6,4,.5f),patient=new Entities.Enemy(6,12,6);patient.health=patient.maxHealth*.3f;
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Warden",0,8,0,new Progression(),new WaveDirector.State(6,0,2,8,0,21,0),java.util.List.of(hexer,patient),Arena.HIGHLANDS));
            check(game.loadCurrent(),"Support squad did not load");key(game,KEY_ENTER);
            hexer=game.getEnemies().get(0);patient=game.getEnemies().get(1);patient.stun=100;hexer.cooldown=100;hexer.healCooldown=0;
            frame(game);check(hexer.healingTarget==patient,"Hexer did not choose wounded ally");float patientHp=patient.health;
            for(int i=0;i<45;i++)frame(game);check(patient.health>patientHp,"Hexer ward did not heal ally");
            hexer.attackTime=0;hexer.healCooldown=0;frame(game);check(hexer.healingTarget==patient,"Second ward did not begin");
            hexer.hit(1,0,0,0);patientHp=patient.health;for(int i=0;i<45;i++)frame(game);
            check(patient.health==patientHp && hexer.healingTarget==null,"Interrupted hexer still healed ally");
            { // A complete playtest kit must never replace the campaign save.
            for(Entities.Enemy enemy:game.getEnemies())enemy.cooldown=Math.min(2,enemy.cooldown);
            check(game.saveCurrent(),"Campaign checkpoint before playtest failed");
            byte[] campaignBefore=java.nio.file.Files.readAllBytes(profile.resolve("save.properties"));
            game.setState(GameState.MAIN_MENU);
            int playMenuX=(GetScreenWidth()-640)/2,playMenuY=(GetScreenHeight()-420)/2;
            click(game,playMenuX+140,playMenuY+424);check(game.getState()==GameState.PLAYTEST_SETUP,"Playtest entry did not open");
            int tx=(GetScreenWidth()-800)/2,ty=(GetScreenHeight()-600)/2;
            click(game,tx+400,ty+134);click(game,tx+580,ty+314);
            for(int t=0;t<3;t++)click(game,tx+580,ty+442);capture("playtest-setup");
            click(game,tx+180,ty+512);check(game.isPlaytest(),"Playtest did not start");
            check(game.getProgression().heroClass()==HeroClass.ARCHER && game.chapterIndex()==1,"Playtest class/chapter selection failed");
            check(game.dayPhase()==0,"Playtest midnight preset failed");
            check(game.getProgression().weaponUnlocked()==3 && game.getProgression().armorUnlocked()==3 && game.getProgression().potions()==5,"Playtest gear not unlocked");
            check(game.getProgression().crystals()==100000 && game.getProgression().kills()==0 && game.getPlayer().discipline().maximum()==235,"Playtest resources/training incorrect");
            for(int i=0;i<4;i++)frame(game);capture("playtest-all-gear");
            check(game.saveCurrent() && java.util.Arrays.equals(campaignBefore,java.nio.file.Files.readAllBytes(profile.resolve("save.properties"))),"Playtest overwrote campaign save");
            check(java.nio.file.Files.exists(profile.resolve("playtest-save.properties")),"Separate playtest save missing");
            key(game,KEY_ESCAPE);key(game,KEY_ESCAPE);
            int px=(GetScreenWidth()-440)/2,py=(GetScreenHeight()-440)/2;
            click(game,px+220,py+340);check(game.getState()==GameState.PLAYTEST_SETUP,"Pause playtest tools failed");
            click(game,tx+650,ty+512);check(game.isPlaytest() && game.getState()==GameState.PAUSED,"Separate playtest resume failed");
            check(game.getProgression().heroClass()==HeroClass.ARCHER && game.chapterIndex()==1,"Saved playtest kit/chapter lost");
            check(game.loadCurrent() && !game.isPlaytest(),"Returning to normal campaign failed");
            check(java.util.Arrays.equals(campaignBefore,java.nio.file.Files.readAllBytes(profile.resolve("save.properties"))),"Campaign changed after playtest resume");
            }
            System.out.println("PASS: all-unlocked playtest, class/chapter controls, separate save/resume and campaign preservation; streamed title/chapter music, mixer mute, redesigned workshop and textured combat effects; crossbow impact and interruptible allied healing; four difficulty selections, independent training, forge purchases, armor click/drag/remove, equipment preview and expanded save state; stamina/parry/riposte, timed chains, charged heavies, lunges, boss slams/charges/sigils/enrage, four-chapter campaign, boss-only levels, victory, sprint/dodge, bounds, warrior animation set, chronicle, second stage and hero models,  name entry, menus, combat, save/load, corrupt-save recovery, settings, persisted key remapping, ranged enemies, resize, Archer and Mage projectiles, inventory, potions, boss impact and rewards, equipment");
        } finally {
            if (game != null) game.dispose();
            CloseWindow();
        }
    }
}
