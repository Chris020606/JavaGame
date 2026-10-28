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
            game = new Game();
            for (int i = 0; i < 5; i++) frame(game);
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
            check(!game.getPlayer().isAttacking(), "Sword swing did not finish");
            key(game, KEY_SPACE);
            check(game.getPlayer().isAttacking(), "Repeated sword attack did not restart");
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
            key(game,KEY_M);
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
            int bx=(GetScreenWidth()-820)/2,by=(GetScreenHeight()-580)/2;
            event(MOUSE_POSITION,bx+100,by+230);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
            event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);key(game,KEY_I);
            key(game,KEY_ESCAPE);
            Settings persisted=new Settings();persisted.load(profile.resolve("settings.properties"));
            check(persisted.controls.key(Config.Controls.Action.FORWARD)==KEY_I,"Rebinding did not persist");
            key(game,KEY_ENTER);
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
            int hx=(GetScreenWidth()-560)/2,hy=(GetScreenHeight()-450)/2;
            event(MOUSE_POSITION,hx+280,hy+207);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
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
            key(game,KEY_TAB);float frozen=game.getProgression().health();
            event(KEY_DOWN,KEY_I,0);for(int i=0;i<10;i++)frame(game);event(KEY_UP,KEY_I,0);frame(game);
            check(game.getProgression().health()==frozen,"Inventory did not pause simulation");capture("ui-inventory");
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
            int ix=(GetScreenWidth()-620)/2,iy=(GetScreenHeight()-520)/2;
            event(MOUSE_POSITION,ix+465,iy+177);event(MOUSE_DOWN,MOUSE_BUTTON_LEFT,0);frame(game);
            event(MOUSE_UP,MOUSE_BUTTON_LEFT,0);frame(game);
            check(game.getProgression().weaponTier()==1 && game.getProgression().damage()==33,"Inventory equip failed");for(int i=0;i<3;i++)frame(game);capture("ui-weapon-loot");
            key(game,KEY_ESCAPE);check(game.saveCurrent() && game.loadCurrent(),"Equipment save failed");
            check(game.getProgression().weaponTier()==1,"Equipment did not persist");
            SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Highlands",0,8,0,new Progression(HeroClass.MAGE),new WaveDirector.State(6,0,0,8,0,21,0),java.util.List.of()));
            check(game.loadCurrent(),"Highland save failed");
            check(game.arena()==Arena.HIGHLANDS,"Wave 6 must restore the second stage");
            key(game,KEY_ENTER);for(int i=0;i<5;i++)frame(game);capture("ui-highland-ruins");
            check(game.saveCurrent() && game.loadCurrent(),"Sanctuary save failed");
            check(game.arena()==Arena.HIGHLANDS,"Sanctuary selection was lost");key(game,KEY_ENTER);
            check(game.getPlayer().getAnimationCount()>3,"Hero animation clips missing");
            check(game.saveCurrent() && game.loadCurrent() && game.arena()==Arena.HIGHLANDS,"Second stage did not persist");
            key(game,KEY_ENTER);
            Camera3D overview=game.getSceneManager().getOpenScene().getCameraController().getCamera();
            overview._position().x(30).y(30).z(34);overview.target().x(0).y(0).z(0);
            for(int i=0;i<3;i++) { BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing(); }
            capture("ui-nature-overview");
            System.out.println("PASS: second stage and hero models,  name entry, menus, combat, save/load, corrupt-save recovery, settings, persisted key remapping, ranged enemies, resize, Archer and Mage projectiles, inventory, potions, boss impact and rewards, equipment");
        } finally {
            if (game != null) game.dispose();
            CloseWindow();
        }
    }
}
