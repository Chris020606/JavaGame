package Scene;

import Entities.Entity;

import static com.raylib.Raylib.*;

public class CameraController {

    private Camera3D camera;
    private Entity target;
    private float yaw = 0.0f;
    private float pitch = 20.0f;
    private float distance = 7.0f;
    private float mouseSensitivity = 0.15f;

    public CameraController(Entity entity) {
        this.target = entity;

        Vector3 targetPosition = target.getPosition();

        camera = new Camera3D()._position(new Vector3() .x(targetPosition.x() + 5.0f)
                                                        .y(targetPosition.y() + 5.0f)
                                                        .z(targetPosition.z() + 6.0f));
        camera.fovy(90.0f);
        camera.up().x(0.0f).y(1.0f).z(0.0f);
        camera.projection(CAMERA_PERSPECTIVE);
        camera.target(new Vector3() .x(targetPosition.x())
                                    .y(targetPosition.y() + 1.0f)
                                    .z(targetPosition.z()));

        DisableCursor();
    }

    public void setCamera(Camera3D camera) {
        this.camera = camera;
    }

    public Camera3D getCamera() {
        return camera;
    }

    public Entity getTarget() {
        return target;
    }

    public void setTarget(Entity target) {
        this.target = target;
    }

    public void update(float dt) {

        Vector2 mouseDelta = GetMouseDelta();

        yaw -= mouseDelta.x() * mouseSensitivity;

        pitch -= mouseDelta.y() * mouseSensitivity;

        pitch = Math.max(-20.0f, Math.min(70.0f, pitch));

        Vector3 playerPosition = target.getPosition();

        float yawRadians = (float) Math.toRadians(yaw);

        float pitchRadians = (float) Math.toRadians(pitch);

        float horizontalDistance = distance * (float) Math.cos(pitchRadians);

        float verticalDistance = distance * (float) Math.sin(pitchRadians);

        float cameraX = playerPosition.x() + horizontalDistance * (float) Math.sin(yawRadians);

        float cameraZ = playerPosition.z() + horizontalDistance * (float) Math.cos(yawRadians);

        float cameraY =  playerPosition.y() + verticalDistance + 1.0f;

        camera._position( new Vector3()
                .x(cameraX)
                .y(cameraY)
                .z(cameraZ)
        );

        camera.target( new Vector3()
                .x(playerPosition.x())
                .y(playerPosition.y() + 1.0f)
                .z(playerPosition.z())

        );
    }
}
