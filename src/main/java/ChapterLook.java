import static com.raylib.Raylib.Color;

/** A chapter changes geometry, ground palette, atmosphere and enemy roster together. */
public enum ChapterLook {
    MEADOW("EMBER MEADOW",new int[]{62,113,147},new int[]{152,182,162},new int[]{75,116,71},new int[]{75,125,73},new int[]{155,168,162},new int[]{116,84,54}),
    CEMETERY("ASH CEMETERY",new int[]{47,42,64},new int[]{126,86,72},new int[]{69,76,64},new int[]{83,77,65},new int[]{119,120,119},new int[]{84,79,72}),
    FROST("FROZEN PASS",new int[]{54,92,133},new int[]{174,204,223},new int[]{195,211,223},new int[]{133,168,184},new int[]{151,176,194},new int[]{112,133,153}),
    NECROPOLIS("HOLLOW NECROPOLIS",new int[]{12,14,35},new int[]{42,42,68},new int[]{43,48,66},new int[]{58,64,89},new int[]{93,97,130},new int[]{55,58,79});
    public final String label;
    private final int[] skyA,skyB,grass,leaf,stone,dirt;
    ChapterLook(String label,int[] skyA,int[] skyB,int[] grass,int[] leaf,int[] stone,int[] dirt) { this.label=label;this.skyA=skyA;this.skyB=skyB;this.grass=grass;this.leaf=leaf;this.stone=stone;this.dirt=dirt; }
    private static Color c(int[] rgb) { return Menu.color(rgb[0],rgb[1],rgb[2]); }
    public Color skyTop() { return c(skyA); }public Color skyBottom() { return c(skyB); }
    public Color grass() { return c(grass); }public Color leaf() { return c(leaf); }
    public Color stone() { return c(stone); }public Color dirt() { return c(dirt); }
    public static ChapterLook at(int chapter) { return values()[Math.max(0,Math.min(3,chapter))]; }
}
