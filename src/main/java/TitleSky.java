/** Menu-only clock and pointer projection. No changes to campaign time or saved games. */
final class TitleSky {
    static final double CYCLE_SECONDS = 180;
    private double phase = .72;
    private int grabbed = -1;
    private double grabOffset;

    record Body(float x, float y, boolean visible) {}

    double phase() {
        return phase;
    }

    void phase(double value) {
        phase = wrap(value);
    }

    float daylight() {
        return (float) Math.max(0, Math.min(1, .5 + Math.sin(phase * Math.PI * 2) * 1.5));
    }

    Body body(int index, int width, int height) {
        double angle = Math.PI - phase * Math.PI * 2 + index * Math.PI;
        return new Body(
                (float) (width * .76 + Math.cos(angle) * width * .23),
                (float) (height * .42 - Math.sin(angle) * height * .34),
                Math.sin(angle) >= -.04);
    }

    boolean dragging() {
        return grabbed >= 0;
    }

    boolean hovered(float x, float y, int width, int height) {
        return hit(x, y, width, height) >= 0;
    }

    void update(float dt, float x, float y, boolean pressed, boolean down, int width, int height) {
        if (pressed) {
            grabbed = hit(x, y, width, height);
            if (grabbed >= 0) grabOffset = phase - pointerPhase(x, y, width, height, grabbed);
        }
        if (grabbed >= 0 && down) {
            // The angle follows the pointer through the horizon, allowing day/night changes in one
            // drag.
            double dx = (x - width * .76) / (width * .23);
            double dy = (height * .42 - y) / (height * .34);
            if (dx * dx + dy * dy > .015)
                phase = wrap(pointerPhase(x, y, width, height, grabbed) + grabOffset);
        } else {
            grabbed = -1;
            phase = wrap(phase + Math.max(0, Math.min(.1, dt)) / CYCLE_SECONDS);
        }
    }

    private int hit(float x, float y, int width, int height) {
        for (int i = 0; i < 2; i++) {
            Body b = body(i, width, height);
            if (b.visible && Math.hypot(x - b.x, y - b.y) <= 44) return i;
        }
        return -1;
    }

    private double pointerPhase(float x, float y, int width, int height, int index) {
        double angle =
                Math.atan2((height * .42 - y) / (height * .34), (x - width * .76) / (width * .23));
        return (Math.PI + index * Math.PI - angle) / (Math.PI * 2);
    }

    private static double wrap(double value) {
        return value - Math.floor(value);
    }
}
