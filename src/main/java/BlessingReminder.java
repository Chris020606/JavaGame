/** A short reminder once per safe interval; combat always suppresses it immediately. */
public final class BlessingReminder {
    private boolean wasSafe;
    private int previousEmbers;
    private float remaining;

    public void update(float dt, boolean safe, int embers) {
        if (!safe || embers <= 0) remaining = 0;
        else if (!wasSafe || embers > previousEmbers) remaining = 4;
        else remaining = Math.max(0, remaining - Math.max(0, dt));
        wasSafe = safe;
        previousEmbers = embers;
    }

    public boolean visible() {
        return remaining > 0;
    }
}
