import Entities.Player;
import Scene.Scene;
import Scene.SceneManager;

import java.util.ArrayList;
import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;

public class Game {

    private Player player;
    private GameState state;
    private SceneManager sceneManager;
    private ModelAnimation animations;

    public Game() {
        sceneManager = new SceneManager();

        state = GameState.PLAYING;
        Scene gameScene = sceneManager.createScene("Game");
        sceneManager.setOpenScene(gameScene);

        Model playerModel = LoadModel("resources/knight.glb");

        Player player = new Player();
        player.setName("Mathew");
        player.setPosition(new Vector3());
        player.loadAnimations("resources/knight.glb");
        player.setModel(playerModel);
        setPlayer(player);

    }

    public void update(float dt) {

        switch (state) {
            case LOGIN -> {
                login();
            }
            case PAUSED -> {

            }
            case IDLE -> {

            }
            case GAME_OVER -> {

            }

            case MAIN_MENU -> {

            }

            case SETTING_MENU -> {

            }

            case PLAYING -> {
                sceneManager.getOpenScene().updatesEntities(dt);
            }

            case null, default -> {

            }
        }

    }

    public void render() {
        sceneManager.getOpenScene().renderEntities();
    }

    private void login() {

    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public void setPlayer(Player player) {
        this.player = player;

        Scene scene = sceneManager.getOpenScene();

        scene.addEntity(player);
        scene.setMainCamera(player);

        player.setCameraController(scene.getCameraController());
    }

    public Player getPlayer() {
        return player;
    }
}
