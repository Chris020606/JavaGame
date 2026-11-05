import java.nio.file.*;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static com.raylib.Raylib.*;

/** Launches two independent JVMs/windows, each using its actual UI, input and network client. */
public final class CoopNativeTest {
    private static final Set<Integer> held=new HashSet<>();
    private static void event(int type,int a,int b) { try(AutomationEvent e=new AutomationEvent().type(type)) { e.params(0,a).params(1,b);PlayAutomationEvent(e); } }
    private static void key(int code,boolean down) { if(down?held.add(code):held.remove(code))event(down?2:1,code,0); }
    private static void capture(Path path) { Image i=LoadImageFromScreen();try { if(!ExportImage(i,path.toString()))throw new AssertionError("Capture failed"); }finally { UnloadImage(i); } }
    private static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        if(args.length>0 && args[0].equals("--role")) { role(args);return; }
        Path root=Files.createTempDirectory(Path.of("build"),"coop-native-").toAbsolutePath();
        HeroClass[][] pairs={{HeroClass.WARRIOR,HeroClass.MAGE},{HeroClass.ARCHER,HeroClass.WARRIOR},{HeroClass.MAGE,HeroClass.MAGE}};
        int[] chapters={0,1,3};
        for(int round=0;round<(Arrays.asList(args).contains("--quick")?1:pairs.length);round++) {
            Path folder=root.resolve("round-"+round);Files.createDirectories(folder);int port;try(ServerSocket socket=new ServerSocket(0)) { port=socket.getLocalPort(); }
            for(String role:List.of("host","guest")) { Path profile=folder.resolve(role+"-profile");Files.createDirectories(profile);Settings settings=new Settings();settings.highQuality=false;settings.save(profile.resolve("settings.properties"));
                if(role.equals("host")) { Properties p=new CoopWorld(Entities.Difficulty.NORMAL,42).checkpoint();p.setProperty("chapter",chapters[round]+"");AppData.write(profile.resolve("coop-checkpoint.properties"),p); }
            }
            Process host=launch(folder,"host",pairs[round][0],port);Process guest=null;
            try {
                long deadline=System.nanoTime()+30_000_000_000L;while(!Files.exists(folder.resolve("invite.txt")) && host.isAlive() && System.nanoTime()<deadline)Thread.sleep(50);
                check(Files.exists(folder.resolve("invite.txt")),"Native host did not publish invite; see "+folder);
                guest=launch(folder,"guest",pairs[round][1],port);
                check(host.waitFor(75,TimeUnit.SECONDS),"Native host timed out: "+folder);check(guest.waitFor(20,TimeUnit.SECONDS),"Native guest timed out: "+folder);
                check(host.exitValue()==0 && guest.exitValue()==0,"Native co-op failed; see "+folder);
                check(Files.exists(folder.resolve("host.png")) && Files.exists(folder.resolve("guest.png")),"Missing two-client gameplay captures");
                System.out.println("PASS: two native JVMs / "+Arrays.toString(pairs[round])+" / chapter "+(chapters[round]+1)+" / "+folder);
            }finally { host.destroyForcibly();if(guest!=null)guest.destroyForcibly(); }
        }
        System.out.println("PASS: two-client native co-op, all hero rigs, shared combat and checkpoint isolation; captures "+root);
    }
    private static Process launch(Path folder,String role,HeroClass hero,int port) throws Exception {
        List<String> command=new ArrayList<>();command.add(Path.of(System.getProperty("java.home"),"bin","java").toString());command.add("--enable-native-access=ALL-UNNAMED");if(System.getProperty("os.name").toLowerCase().contains("mac"))command.add("-XstartOnFirstThread");
        command.add("-Doutpost.dataDir="+folder.resolve(role+"-profile"));command.add("-cp");command.add(System.getProperty("java.class.path"));command.add("CoopNativeTest");Collections.addAll(command,"--role",role,hero.name(),port+"",folder.toString());
        return new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(folder.resolve(role+".log").toFile()).start();
    }
    private static void role(String[] args) throws Exception {
        String role=args[1];HeroClass hero=HeroClass.valueOf(args[2]);int port=Integer.parseInt(args[3]);Path folder=Path.of(args[4]);
        SetTraceLogLevel(LOG_WARNING);SetConfigFlags(FLAG_WINDOW_RESIZABLE|FLAG_WINDOW_HIGHDPI);InitWindow(1000,680,"Outpost co-op / "+role);SetExitKey(KEY_NULL);SetTargetFPS(30);
        try(CoopGame game=new CoopGame()) {
            String invite=role.equals("host")?"":Files.readString(folder.resolve("invite.txt"));
            game.configure(new String[]{"--coop-name="+role,"--coop-class="+hero.name(),role.equals("host")?"--coop-resume="+port:"--coop-join=127.0.0.1:"+port,"--coop-code="+invite});
            if(role.equals("host"))Files.writeString(folder.resolve("invite.txt"),game.hosted().code());
            int played=0;boolean moved=false,hit=false,done=false;long seq=1;
            for(int frame=0;frame<1800;frame++) {
                CoopState state=game.snapshot();int slot=role.equals("host")?0:1;
                if(state!=null) {
                    CoopInput input=CoopBot.input(state,slot,seq++);
                    var me=state.heroes().stream().filter(h->h.slot()==slot).findFirst().orElse(null);
                    if(me!=null) {
                        if(state.phase()==CoopState.Phase.PLAYING && !state.paused())played++;
                        moved|=Math.hypot(me.x()-(slot==0?-2:2),me.z()-4)>.5f;
                        hit|=state.kills()>0 || state.enemies().stream().anyMatch(e->e.health()<e.maxHealth());
                        Camera3D camera=game.camera();float fx=camera.target().x()-camera._position().x(),fz=camera.target().z()-camera._position().z(),length=(float)Math.hypot(fx,fz);fx/=length;fz/=length;
                        float forward=input.moveX()*fx+input.moveZ()*fz,right=-input.moveX()*fz+input.moveZ()*fx;
                        key(KEY_W,forward>.2);key(KEY_S,forward<-.2);key(KEY_D,right>.2);key(KEY_A,right<-.2);
                        key(KEY_SPACE,(input.buttons()&CoopInput.LIGHT)!=0);key(KEY_Q,(input.buttons()&CoopInput.HEAVY)!=0);key(KEY_F,(input.buttons()&CoopInput.PARRY)!=0);key(KEY_LEFT_CONTROL,(input.buttons()&CoopInput.DODGE)!=0);key(KEY_H,(input.buttons()&CoopInput.POTION)!=0);key(KEY_G,(input.buttons()&CoopInput.INTERACT)!=0);key(KEY_ENTER,(input.buttons()&CoopInput.READY)!=0 && frame%15==0);
                        Vector2 mouse=GetWorldToScreen(new Vector3().x(me.x()+(float)Math.sin(Math.toRadians(input.aim()))*7).y(1.1f).z(me.z()+(float)Math.cos(Math.toRadians(input.aim()))*7),camera);event(7,(int)mouse.x(),(int)mouse.y());
                    }
                }
                event(5,MOUSE_BUTTON_LEFT,0);event(5,MOUSE_BUTTON_RIGHT,0);
                if(state!=null && state.phase()!=CoopState.Phase.PLAYING && state.phase()!=CoopState.Phase.VICTORY && frame%30==10) {
                    var own=state.heroes().stream().filter(h->h.slot()==slot).findFirst().orElse(null);
                    if(own!=null && !own.ready()) { event(7,(GetScreenWidth()-900)/2+250,(GetScreenHeight()-560)/2+440);event(6,MOUSE_BUTTON_LEFT,0); }
                }
                game.update(1f/30);BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();
                if(frame%150==0 && game.snapshot()!=null)System.out.println("NATIVE STATE "+role+" "+game.snapshot().phase()+" paused="+game.snapshot().paused()+" heroes="+game.snapshot().heroes());
                if(frame==180)capture(folder.resolve(role+"-diagnostic.png"));
                if(played>=160 && !Files.exists(folder.resolve(role+".png")))capture(folder.resolve(role+".png"));
                if(!done && played>=300) { check(moved,"Local native player did not move");check(hit,"Native players never damaged a shared enemy");check(game.snapshot().heroes().size()==2,"Second player missing");Files.writeString(folder.resolve(role+".done"),"PASS moved and shared damage; tick="+game.snapshot().tick());done=true; }
                if(done && Files.exists(folder.resolve((role.equals("host")?"guest":"host")+".done")))break;
                if(!done && game.network()!=null)check(game.network().error().isEmpty(),game.network().error());
            }
            check(done,"Native co-op never reached ten seconds of shared combat");
            check(!Files.exists(AppData.directory().resolve("save.properties")),"Co-op overwrote solo save");
            System.out.println("PASS native "+role+" "+hero+" played="+played);
        }finally { CloseWindow(); }
    }
}
