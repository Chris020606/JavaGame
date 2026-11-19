import java.util.HashMap;

/** Prevent a crowd's simultaneous impacts from restarting the same sound dozens of times. */
final class AudioCueGate {
    private double clock;
    private final HashMap<String, Double> last = new HashMap<>();

    void update(float dt) {
        clock += Math.max(0, Math.min(.1f, dt));
    }

    boolean allow(String name) {
        String family = name.startsWith("hit") ? "hit" : name;
        double gap =
                switch (family) {
                    case "hit" -> .045;
                    case "kill" -> .09;
                    case "hurt" -> .08;
                    default -> 0;
                };
        if (gap == 0) return true;
        Double previous = last.get(family);
        if (previous != null && clock - previous < gap) return false;
        last.put(family, clock);
        return true;
    }
}
