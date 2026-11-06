import static com.raylib.Raylib.*;

/** Zone selection is a view; Game owns starting and saving the selected expedition. */
public final class ChapterSelectScreen {
    public int draw(int unlocked,int current) {
        int x=(GetScreenWidth()-740)/2,y=(GetScreenHeight()-570)/2;
        Menu.panel(x,y,740,570);Menu.text("THE VALLEY / CHAPTERS",x+24,y+22,28,Menu.GOLD);
        Menu.wrapped("Revisit unlocked chapters with your current equipment. Entering a chapter restarts its encounters and replaces the active checkpoint.",x+24,y+66,680,16,Menu.TEXT);
        for(int i=0;i<4;i++) {
            var c=Campaign.CHAPTERS[i];int top=y+130+i*86;
            Menu.text((i+1)+" / "+c.title(),x+24,top,21,i<=unlocked?Menu.TEXT:Menu.MUTED);
            Menu.text(c.bossOnly()?"BOSS ENCOUNTER":c.waves()+" WAVES / "+(i==2?"MOUNTAIN PASS":"MEADOW DEFENSE"),x+24,top+32,13,Menu.MUTED);
            if(Menu.button(i>unlocked?"LOCKED":i==current?"RESTART":"TRAVEL",x+540,top-4,174,i<=unlocked))return i;
        }
        if(Menu.button("BACK",x+24,y+506,220,true))return -2;
        return -1;
    }
}
