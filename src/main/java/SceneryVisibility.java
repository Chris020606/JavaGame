/** Conservative top-down occlusion test for decorative tree trunks and canopies. */
public final class SceneryVisibility {
    private SceneryVisibility() {}

    public static float treeOpacity(
            float treeX,
            float treeZ,
            float scale,
            float heroX,
            float heroZ,
            float cameraX,
            float cameraZ) {
        float dx = cameraX - heroX, dz = cameraZ - heroZ;
        float lengthSquared = dx * dx + dz * dz;
        if (lengthSquared < .001f) return 1;
        float along = ((treeX - heroX) * dx + (treeZ - heroZ) * dz) / lengthSquared;
        // Only fade trees between camera and hero. Trees beyond either end remain solid.
        return along > 0
                        && along < 1
                        && Combat.segmentDistance(treeX, treeZ, heroX, heroZ, cameraX, cameraZ)
                                < scale * .65f
                ? .22f
                : 1;
    }
}
