package Scene;

import static com.raylib.Raylib.*;

import Entities.Entity;

public class CameraController {

    private Camera3D camera;
    private Entity target;
    private float yaw = 0.0f;
    private float pitch = 24.0f;
    private float distance = 17.0f;
    private float mouseSensitivity = 0.15f;
    private float followX, followY, followZ, impact, impactClock;
    private boolean following;

    public void impact(float strength) {
        impact = Math.max(impact, Math.min(.15f, Math.max(0, strength)));
        impactClock = 0;
    }

    public void setDistance(float distance) {
        this.distance = Math.max(4.0f, Math.min(18.0f, distance));
    }

    public float getDistance() {
        return distance;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = Math.max(5.0f, Math.min(70.0f, pitch));
    }

    public float getPitch() {
        return pitch;
    }

    public CameraController(Entity entity) {
        this.target = entity;

        Vector3 targetPosition = target.getPosition();

        camera =
                new Camera3D()
                        ._position(
                                new Vector3()
                                        .x(targetPosition.x() + 5.0f)
                                        .y(targetPosition.y() + 5.0f)
                                        .z(targetPosition.z() + 6.0f));
        camera.fovy(60.0f);
        camera.up().x(0.0f).y(1.0f).z(0.0f);
        camera.projection(CAMERA_PERSPECTIVE);
        camera.target(
                new Vector3()
                        .x(targetPosition.x())
                        .y(targetPosition.y() + 1.0f)
                        .z(targetPosition.z()));
    }

    public void setSensitivity(float value) {
        mouseSensitivity = value;
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
        if (IsMouseButtonDown(MOUSE_BUTTON_RIGHT)) {
            yaw -= mouseDelta.x() * mouseSensitivity;
            pitch -= mouseDelta.y() * mouseSensitivity;
        }
        pitch = Math.max(5.0f, Math.min(70.0f, pitch));

        float wheel = GetMouseWheelMove();
        if (wheel != 0) {
            distance = Math.max(4.0f, Math.min(18.0f, distance - wheel * 1.5f));
        }

        Vector3 playerPosition = target.getPosition();

        float yawRadians = (float) Math.toRadians(yaw);
        float pitchRadians = (float) Math.toRadians(pitch);

        float horizontalDistance = distance * (float) Math.cos(pitchRadians);
        float verticalDistance = distance * (float) Math.sin(pitchRadians);

        if (!following
                || dt <= 0
                || Math.hypot(playerPosition.x() - followX, playerPosition.z() - followZ) > 8) {
            followX = playerPosition.x();
            followY = playerPosition.y();
            followZ = playerPosition.z();
            following = true;
        }
        float blend = 1 - (float) Math.exp(-Math.max(0, dt) * 22);
        followX += (playerPosition.x() - followX) * blend;
        followY += (playerPosition.y() - followY) * blend;
        followZ += (playerPosition.z() - followZ) * blend;
        impactClock += Math.max(0, dt);
        impact *= Math.exp(-Math.max(0, dt) * 23);
        float recoil = (float) Math.sin(impactClock * 75) * impact;
        float cameraX = followX + horizontalDistance * (float) Math.sin(yawRadians);
        float cameraZ = followZ + horizontalDistance * (float) Math.cos(yawRadians);
        float cameraY = followY + verticalDistance + 1.0f;

        camera._position(new Vector3().x(cameraX + recoil).y(cameraY + recoil * .3f).z(cameraZ));

        camera.target(new Vector3().x(followX).y(followY + 1.0f).z(followZ));
    }
}
