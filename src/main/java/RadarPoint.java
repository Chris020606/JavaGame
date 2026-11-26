/** Circular projection leaves space for the marker itself inside the radar frame. */
record RadarPoint(float x, float y) {
    static RadarPoint of(float worldX, float worldZ, float scale, float radius) {
        if (!Float.isFinite(worldX) || !Float.isFinite(worldZ)) return new RadarPoint(0, 0);
        float x = worldX * scale, y = worldZ * scale;
        double distance = Math.hypot(x, y);
        if (distance > radius) {
            x *= radius / distance;
            y *= radius / distance;
        }
        return new RadarPoint(x, y);
    }
}
