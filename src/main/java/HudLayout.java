/** Shared drawing and hit targets in window pixels, including compact windows. */
public record HudLayout(int width,int height) {
    public static final int SLOTS=6,GAP=6;
    public int cell() { return width<1000?48:56; }
    public int span() { return SLOTS*(cell()+GAP)-GAP; }
    public int x() { return (width-span())/2; }
    public int y() { return height-86; }
    public int slotX(int index) { return x()+index*(cell()+GAP); }
    public boolean contains(float mx,float my) { return mx>=x()-8 && mx<x()+span()+8 && my>=y()-8 && my<y()+70; }
    public int hit(float mx,float my) {
        if(my<y() || my>=y()+cell())return -1;
        for(int i=0;i<SLOTS;i++)if(mx>=slotX(i) && mx<slotX(i)+cell())return i;
        return -1;
    }
}
