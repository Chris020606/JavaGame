package Entities;

/** Frame indices passed to raylib must stay inside the clip's keyframe array. */
public final class AnimationFrames {
    private AnimationFrames() { }

    public static int clamp(int frame, int count) {
        if (count <= 0) throw new IllegalArgumentException("Animation has no keyframes");
        return Math.max(0, Math.min(count - 1, frame));
    }

    public static int progress(float progress, int count) {
        float safe = Float.isNaN(progress) ? 0 : Math.max(0, Math.min(1, progress));
        return clamp((int)(safe * count), count);
    }

    public static int loop(float frames, int count) {
        if (count <= 0) throw new IllegalArgumentException("Animation has no keyframes");
        if (!Float.isFinite(frames)) return 0;
        return Math.floorMod((long)Math.floor(frames), count);
    }
}
