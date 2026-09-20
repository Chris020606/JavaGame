package Scene;

import Entities.Entity;

import java.util.ArrayList;

import static com.raylib.Colors.BLACK;
import static com.raylib.Colors.WHITE;
import static com.raylib.Raylib.*;


public class Scene {

    private String name;
    private ArrayList<Entity> entities;
    private boolean mainCamera;
    private int ID;
    private CameraController cameraController;

    public Scene(String name, int id) {
        this.name = name;
        this.ID = id;
        this.entities = new ArrayList<>();
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Entity> getEntities() {
        return entities;
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    public void setEntities(ArrayList<Entity> entities) {
        this.entities = entities;
    }

    public void setMainCamera(Entity entity) {
        cameraController = new CameraController(entity);
    }

    public CameraController getCameraController() {
        return cameraController;
    }

    public void renderEntities() {
        if (cameraController == null) {
            return;
        }

        BeginMode3D(cameraController.getCamera());
        for (Entity entity : entities ) {
            DrawModelEx(entity.getModel(),
                entity.getPosition(),
                new Vector3()
                    .x(0)
                    .y(1)
                    .z(0)
                ,entity.getRotation().y(),
                new Vector3()
                    .x(entity.getScale())
                    .y(entity.getScale())
                    .z(entity.getScale()),
                WHITE);


        }

        DrawGrid(50, 1.0f);

        EndMode3D();
        DrawText(name, 20, 20, 100, BLACK);
    }

    public void updatesEntities(float dt) {

        if (cameraController != null) {
            cameraController.update(dt);
        }

        for (Entity entity : entities ) {
            entity.update(dt);
        }
    }
}
