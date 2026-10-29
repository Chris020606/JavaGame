import Entities.Player;
import Entities.PlayerAnimation;
import assets.AssetPaths;
import static com.raylib.Raylib.*;
import static com.raylib.Colors.WHITE;

/** An independent animated character stage; selecting a class never changes the active hero. */
public final class HeroPortrait implements AutoCloseable {
    private final Player[] heroes=new Player[3];
    private final RenderTexture target=LoadRenderTexture(600,800);
    private final Shader shader=LoadShader(AssetPaths.path("resources/outpost/daylight.vs"),AssetPaths.path("resources/outpost/preview.fs"));
    private final Camera3D camera=new Camera3D()._position(new Vector3().x(3.5f).y(2.8f).z(6)).target(new Vector3().y(1.3f)).up(new Vector3().y(1)).fovy(34).projection(CAMERA_PERSPECTIVE);
    public void prepare(WorldAssets assets,HeroClass hero) {
        Player player=heroes[hero.ordinal()];
        if(player==null) {
            player=new Player();String file=hero==HeroClass.WARRIOR?"resources/knight.glb":hero==HeroClass.ARCHER?"resources/outpost/hero_archer.glb":"resources/outpost/hero_mage.glb";
            player.setModel(LoadModel(AssetPaths.path(file)));player.loadAnimations(AssetPaths.path(file));player.setWeaponStyle(hero.ordinal());player.setAnimationFPS(60);
            player.setAnimation(hero==HeroClass.ARCHER?PlayerAnimation.BOW_IDLE:PlayerAnimation.UNARMED_IDLE);heroes[hero.ordinal()]=player;
        }
        float time=(float)GetTime();player.getRotation().y(18+(float)Math.sin(time*.5f)*9);player.updateAnimation(1/60f);
        BeginTextureMode(target);ClearBackground(Menu.color(21,36,45));BeginMode3D(camera);
        DrawCylinder(new Vector3().y(-.08f),1.05f,1.2f,.08f,48,Menu.color(48,66,69));
        assets.previewPass(shader);assets.preparePlayer(player);assets.drawPlayerBody(player,new Progression(hero),WHITE);assets.drawPlayerWeapon(player,hero.ordinal(),0);assets.endPreview();
        EndMode3D();EndTextureMode();
    }
    public void draw(int x,int y,int w,int h) {
        DrawTexturePro(target.texture(),new Rectangle().width(600).height(-800),new Rectangle().x(x).y(y).width(w).height(h),new Vector2(),0,WHITE);
    }
    @Override public void close() { for(Player p:heroes)if(p!=null)p.dispose();UnloadRenderTexture(target);UnloadShader(shader); }
}
