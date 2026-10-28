import com.raylib.Raylib.Color;

/** Stage is derived from the wave so old and new saves follow the same route. */
public enum Arena {
    WOODLAND("I / WOODLAND HAVEN", 57,79,65),
    HIGHLANDS("II / HIGHLAND RUINS", 83,88,101);
    public final String label;
    private final int r,g,b;
    Arena(String label,int r,int g,int b) { this.label=label;this.r=r;this.g=g;this.b=b; }
    public Color ground() { return Menu.color(r,g,b); }
    public static Arena forWave(int wave) { return wave>=6 ? HIGHLANDS : WOODLAND; }
}
