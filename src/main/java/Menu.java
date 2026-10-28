import static com.raylib.Raylib.*;

public class Menu {
    public static final Color INK = color(14, 22, 30), PANEL = color(24, 36, 46),
        LINE = color(53, 73, 83), TEXT = color(235, 239, 229), MUTED = color(153, 174, 178),
        GOLD = color(235, 190, 101), TEAL = color(86, 200, 174), RED = color(224, 105, 100);
    public static Color color(int r, int g, int b) {
        return new Color().r((byte)r).g((byte)g).b((byte)b).a((byte)255);
    }
    private static Font font;
    private static Font font() {
        if(font==null) { font=LoadFontEx(assets.AssetPaths.path("resources/outpost/interface.ttf"),48,(int[])null,0); SetTextureFilter(font.texture(),TEXTURE_FILTER_BILINEAR); }
        return font;
    }
    public static int measure(String text,int size) { return Math.round(MeasureTextEx(font(),text,size*1.15f,.5f).x()); }
    public static void center(String text,int x,int y,int size,Color color) { text(text,x-measure(text,size)/2,y,size,color); }
    public static void close() { if(font!=null) { UnloadFont(font);font=null; } }
    public static void panel(int x, int y, int w, int h) {
        DrawRectangleRounded(new Rectangle().x(x+2).y(y+4).width(w).height(h),.16f,8,Fade(INK,.3f));
        DrawRectangleRounded(new Rectangle().x(x).y(y).width(w).height(h),.16f,8,Fade(PANEL,.92f));
        DrawLine(x+16,y+h-1,x+w-16,y+h-1,Fade(GOLD,.3f));
    }
    public static void text(String text, int x, int y, int size, Color color) {
        DrawTextEx(font(),text,new Vector2().x(x).y(y),size*1.15f,.5f,color);
    }
    public static void bar(int x, int y, int w, float value, float max, Color color) {
        DrawRectangle(x, y, w, 8, LINE);
        DrawRectangle(x, y, (int)(w * Math.max(0, Math.min(1, value / max))), 8, color);
    }
    public static boolean button(String label, int x, int y, int w, boolean enabled) {
        Vector2 mouse = GetMousePosition();
        boolean hover = mouse.x() >= x && mouse.x() <= x+w && mouse.y() >= y && mouse.y() <= y+44;
        DrawRectangleRounded(new Rectangle().x(x).y(y).width(w).height(44),.2f,8,enabled?(hover?TEAL:color(57,75,80)):LINE);
        DrawLine(x+12,y+43,x+w-12,y+43,enabled?GOLD:LINE);
        text(label, x + (w - measure(label, 18))/2, y+13, 18, enabled ? (hover?INK:TEXT) : MUTED);
        return enabled && hover && IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
    }
}
