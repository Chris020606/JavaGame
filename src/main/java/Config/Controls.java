package Config;

import java.util.EnumMap;
import static com.raylib.Raylib.*;

public final class Controls {
    public enum Action {
        FORWARD("Forward",KEY_W), BACK("Back",KEY_S), LEFT("Left",KEY_A), RIGHT("Right",KEY_D),
        SLASH("Slash",KEY_SPACE), HEAVY("Heavy",KEY_Q), WORKSHOP("Workshop",KEY_E), MUTE("Mute",KEY_M),
        SAVE("Quick save",KEY_F5), LOAD("Quick load",KEY_F9), INVENTORY("Inventory",KEY_TAB), POTION("Use potion",KEY_H);
        public final String label; public final int defaultKey;
        Action(String label,int key) { this.label=label; defaultKey=key; }
    }
    private final EnumMap<Action,Integer> keys=new EnumMap<>(Action.class);
    public Controls() { reset(); }
    public void reset() { for (Action a:Action.values()) keys.put(a,a.defaultKey); }
    public int key(Action action) { return keys.get(action); }
    public boolean pressed(Action action) { return IsKeyPressed(key(action)); }
    public boolean down(Action action) { return IsKeyDown(key(action)); }
    public void bind(Action action,int key) {
        if (!validKey(key)) throw new IllegalArgumentException("Use a letter, number, arrow, Space, Tab, or F1-F12.");
        for (Action other:Action.values()) if (other!=action && keys.get(other)==key)
            throw new IllegalArgumentException(name(key)+" is already assigned to "+other.label+".");
        keys.put(action,key);
    }
    public static boolean validKey(int key) { return key==32 || key==KEY_TAB || key>=48&&key<=57 || key>=65&&key<=90 || key>=262&&key<=265 || key>=290&&key<=301; }
    public static String name(int key) {
        if (key==32) return "SPACE";
        if (key==KEY_TAB) return "TAB";
        if (key>=290&&key<=301) return "F"+(key-289);
        return switch(key) { case 262 -> "RIGHT"; case 263 -> "LEFT"; case 264 -> "DOWN"; case 265 -> "UP"; default -> Character.toString((char)key); };
    }
    public String label(Action action) { return name(key(action)); }
    public void restore(java.util.Properties p) {
        EnumMap<Action,Integer> restored=new EnumMap<>(Action.class);
        java.util.HashSet<Integer> used=new java.util.HashSet<>();
        for (Action a:Action.values()) {
            int key=Integer.parseInt(p.getProperty("key."+a,a.defaultKey+""));
            if (!validKey(key)||!used.add(key)) throw new IllegalArgumentException("Invalid or duplicate key binding");
            restored.put(a,key);
        }
        keys.clear(); keys.putAll(restored);
    }
    public void store(java.util.Properties p) { for (Action a:Action.values()) p.setProperty("key."+a,key(a)+""); }
}
