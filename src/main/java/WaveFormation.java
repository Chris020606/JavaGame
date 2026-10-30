/** Distinct squad compositions keep each siege wave readable and different. */
public final class WaveFormation {
    public static String label(int wave) { return switch(wave%4) { case 1 -> "SCOUT RAID";case 2 -> "SIEGE COLUMN";case 3 -> "HEXER ESCORT";default -> "MIXED AMBUSH"; }; }
    public static int variant(int wave,int index) {
        if(Campaign.chapter(wave).bossOnly())return 5;
        if(wave==1)return index%3==0?1:0;
        return switch(wave%4) { case 1 -> index%3==0?3:1;case 2 -> index%3==0?2:index%3==1?3:0;case 3 -> index%5==0?4:index%2==0?2:0;default -> new int[]{0,1,3,2,4}[index%5]; };
    }
    private WaveFormation() { }
}
