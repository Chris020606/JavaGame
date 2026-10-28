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
    public static void wrapped(String text,int x,int y,int width,int size,Color color) {
        String line="";int row=y;
        for(String word:text.split(" ")) {
            String next=line.isEmpty()?word:line+" "+word;
            if(measure(next,size)>width && !line.isEmpty()) { text(line,x,row,size,color);row+=size+5;line=word; }
            else line=next;
        }
        if(!line.isEmpty()) text(line,x,row,size,color);
    }
    /** Small vector pictograms remain crisp at both display scales. */
    public static void icon(String kind,int x,int y,int size,Color color) {
        int u=Math.max(1,size/8),cx=x+size/2,cy=y+size/2;
        switch(kind) {
            case "heart" -> {
                DrawRectangle(cx-u,y,u*2,size,color);DrawRectangle(x,cy-u,size,u*2,color);
            }
            case "sword" -> {
                DrawLine(x+u,y+size-u,x+size-u,y+u,color);
                DrawLine(x+2*u,y+size-u,x+size-u,y+2*u,color);
                DrawLine(x,y+size-3*u,x+3*u,y+size,color);
            }
            case "bag" -> {
                DrawRectangleLines(x+u,y+3*u,size-2*u,size-3*u,color);
                DrawRectangleLines(cx-2*u,y,u*4,u*3,color);
                DrawLine(x+u,cy+u,x+size-u,cy+u,color);
            }
            case "save" -> {
                DrawRectangleLines(x,y,size,size,color);
                DrawRectangle(x+u,y,u*4,u*3,color);
                DrawRectangleLines(x+u,cy+u,size-2*u,size/2-u,color);
            }
            case "book" -> {
                DrawRectangleLines(x,y+u,size,size-2*u,color);DrawLine(cx,y+u,cx,y+size-u,color);
                DrawLine(x+u,y+3*u,cx-u,y+3*u,color);DrawLine(cx+u,y+3*u,x+size-u,y+3*u,color);
            }
            case "gear" -> {
                DrawCircleLines(cx,cy,size/3f,color);DrawCircle(cx,cy,u,color);
                for(int i=0;i<8;i++) { double a=i*Math.PI/4;DrawLine(cx+(int)(Math.cos(a)*size*.3),cy+(int)(Math.sin(a)*size*.3),cx+(int)(Math.cos(a)*size*.5),cy+(int)(Math.sin(a)*size*.5),color); }
            }
            case "shield" -> {
                DrawLine(x,y+u,x+size,y+u,color);DrawLine(x,y+u,x+u,y+size-2*u,color);
                DrawLine(x+size,y+u,x+size-u,y+size-2*u,color);DrawLine(x+u,y+size-2*u,cx,y+size,color);
                DrawLine(cx,y+size,x+size-u,y+size-2*u,color);
            }
            case "crown" -> {
                DrawLine(x,y+u,x+u,y+size-u,color);DrawLine(x+size,y+u,x+size-u,y+size-u,color);
                DrawLine(x,y+u,cx,cy,color);DrawLine(cx,cy,cx,y,color);DrawLine(cx,y,x+size,cy,color);
                DrawLine(x+u,y+size-u,x+size-u,y+size-u,color);
            }
            default -> { DrawLine(x,cy,x+size,cy,color);DrawLine(x+size,cy,cx,y+u,color);DrawLine(x+size,cy,cx,y+size-u,color); }
        }
    }
    private static String buttonIcon(String label) {
        if(label.contains("POTION")||label.contains("Heal")) return "heart";
        if(label.contains("SAVE")) return "save";
        if(label.contains("INVENTORY")||label.contains("GEAR")) return "bag";
        if(label.contains("SETTINGS")) return "gear";
        if(label.contains("CHRONICLE")) return "book";
        if(label.contains("UPGRADE")) return "shield";
        if(label.contains("NEW GAME")||label.contains("BEGIN")) return "sword";
        if(label.contains("CONTINUE")||label.contains("RETURN")||label.contains("RESUME")||label.contains("LOAD GAME")) return "arrow";
        return null;
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
        Color foreground=enabled?(hover?INK:TEXT):MUTED;
        String kind=w>=140?buttonIcon(label):null;
        if(kind!=null) icon(kind,x+12,y+14,16,foreground);
        int inset=kind==null?0:24;
        int textSize=18;
        while(textSize>12 && measure(label,textSize)>w-inset-16) textSize--;
        text(label,x+inset+(w-inset-measure(label,textSize))/2,y+13,textSize,foreground);
        return enabled && hover && IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
    }
}
