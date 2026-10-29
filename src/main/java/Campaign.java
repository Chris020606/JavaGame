/** The finite story route. Global wave numbers keep older saves and enemy scaling compatible. */
public final class Campaign {
    public record Chapter(String title,int first,int last,boolean bossOnly,String story,String aftermath) {
        public int waves() { return last-first+1; }
        public int cleared(int wave,int queued,int living,int total) {
            int completed=wave-first+(queued==0 && living==0 && total>0?1:0);
            return Math.max(0,Math.min(waves(),completed));
        }
    }
    public static final Chapter[] CHAPTERS={
        new Chapter("THE LAST EMBER",1,4,false,
            "The skyfires died a century ago. Beneath these stones, one ember still warms the valley. Hold the sanctuary until its beacon wakes.",
            "The beacon answers. Across the valley, something ancient hears it too."),
        new Chapter("THE ASH WARDEN",5,5,true,
            "The Ash Cemetery shelters a broken oath. Its buried keeper has risen as the Ash Warden. Defeat him beneath the crypts and reclaim the northern road.",
            "The Warden falls. His broken seal reveals a road to the Crown of Dawn."),
        new Chapter("ROAD TO DAWN",6,9,false,
            "Snow buries the northern pass. Boneguards, dead archers and frost hexers hunt the last caravan. Hold the waystones until the refugees cross.",
            "The caravan reaches safety. Only the Hollow King stands between the valley and morning."),
        new Chapter("THE HOLLOW KING",10,10,true,
            "The Hollow King has swallowed every fire before yours. Face him alone among the crypts of the Hollow Necropolis. Let this be the last long night.",
            "At dawn, the valley remembers warmth. The stones shine again, and the road home is open. Your watch is complete.")
    };
    public static Chapter chapter(int wave) {
        for(Chapter chapter:CHAPTERS) if(wave<=chapter.last()) return chapter;
        return CHAPTERS[CHAPTERS.length-1];
    }
    public static int index(int wave) {
        for(int i=0;i<CHAPTERS.length;i++) if(wave<=CHAPTERS[i].last()) return i;
        return CHAPTERS.length-1;
    }
    public static boolean boundary(int wave) { return wave==4 || wave==5 || wave==9 || wave>=10; }
    public static int enemies(int wave) { return chapter(wave).bossOnly()?1:Math.min(36,4+wave*3); }
    private Campaign() {}
}
