import Entities.Enemy;

public final class Projectile {
    public float x,z,previousX,previousZ,life=3;
    public final float vx,vz,damage;
    public final boolean playerTarget,magic;
    public final boolean travelerTarget;
    public Projectile(Enemy enemy,int wave) {
        x=previousX=enemy.x; z=previousZ=enemy.z;
        float dx=enemy.attackX-x,dz=enemy.attackZ-z,len=(float)Math.hypot(dx,dz);
        magic=enemy.variant==4; playerTarget=enemy.targetsPlayer;travelerTarget=enemy.targetsTraveler; damage=((magic?14:8)+wave*2)*enemy.difficulty.damage;
        float speed=magic?6:10;
        vx=len>.001f?dx/len*speed:0; vz=len>.001f?dz/len*speed:0;
    }
    public void update(float dt) { previousX=x;previousZ=z;x+=vx*dt;z+=vz*dt;life-=dt; }
    public boolean hits(float px,float pz,float radius) {
        float dx=x-previousX,dz=z-previousZ,length=dx*dx+dz*dz;
        float t=length==0?0:Math.max(0,Math.min(1,((px-previousX)*dx+(pz-previousZ)*dz)/length));
        return Math.hypot(previousX+dx*t-px,previousZ+dz*t-pz)<=radius;
    }
}
