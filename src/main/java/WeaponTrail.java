/** Fixed-capacity history of the visible blade. Updated by simulation, never by rendering. */
public final class WeaponTrail {
    public static final int CAPACITY=24;
    private final float[][] points=new float[CAPACITY][6];
    private final float[] ages=new float[CAPACITY];
    private int first,count;
    private float lifetime=.09f;
    public int size() { return count; }
    public float coordinate(int sample,int axis) { return points[(first+sample)%CAPACITY][axis]; }
    public float opacity(int sample) { return Math.max(0,1-ages[(first+sample)%CAPACITY]/lifetime); }
    public void clear() { first=count=0; }
    public void begin(boolean heavy) { clear();lifetime=heavy?.12f:.09f; }
    public void update(float dt) {
        for(int i=0;i<count;i++)ages[(first+i)%CAPACITY]+=Math.max(0,dt);
        while(count>0 && ages[first]>=lifetime) { first=(first+1)%CAPACITY;count--; }
    }
    public void sample(float ax,float ay,float az,float bx,float by,float bz) {
        if(!Float.isFinite(ax+ay+az+bx+by+bz)) { clear();return; }
        if(count>0) {
            int previous=(first+count-1)%CAPACITY;
            float dx=bx-points[previous][3],dy=by-points[previous][4],dz=bz-points[previous][5],distance=dx*dx+dy*dy+dz*dz;
            if(distance>9)clear(); // Never draw a ribbon across a teleport or equipment change.
            else if(distance<.0001f)return;
        }
        if(count==CAPACITY) { first=(first+1)%CAPACITY;count--; }
        int next=(first+count++)%CAPACITY;float[] p=points[next];
        p[0]=ax;p[1]=ay;p[2]=az;p[3]=bx;p[4]=by;p[5]=bz;ages[next]=0;
    }
}
