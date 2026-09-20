package Scene;

import java.util.ArrayList;

public class SceneManager {
    private ArrayList<Scene> scenes;
    private Scene openScene;
    private int openSceneID;
    private int count = 0;

    public SceneManager() {
        scenes = new ArrayList<>();
    }

    public Scene createScene(String name) {
        Scene scene = new Scene(name, count);
        count += 1;
        scenes.add(scene);
        return scene;
    }

    public Scene getOpenScene() {
        return openScene;
    }

    public int getOpenSceneID() {
        return openSceneID;
    }

    public void setOpenScene(Scene openScene) {
        this.openScene = openScene;
        this.openSceneID = openScene.getID();
    }

    public void setOpenSceneID(int id) {
        openSceneID = id;
    }

    public Scene getScene(int id) {
        for (Scene scene : scenes) {
            if (scene.getID() == id) {
                return scene;
            }
        }

        return null;
    }
}
