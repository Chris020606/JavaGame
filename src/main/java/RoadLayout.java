import java.util.*;

/** A continuous, gently curved ribbon rather than rotated, overlapping path tiles. */
public final class RoadLayout {
    public record Sample(float x,float z,float nx,float nz,float width) { }
    private static final List<List<Sample>> ROADS=new ArrayList<>();
    static { for(int c=0;c<4;c++)ROADS.add(sample(extendToHorizon(EnvironmentLayout.path(c)))); }
    private static List<EnvironmentLayout.Point> extendToHorizon(List<EnvironmentLayout.Point> path) {
        var first=path.getFirst();var next=path.get(1);var before=path.get(path.size()-2);var last=path.getLast();
        float startLength=(float)Math.hypot(next.x()-first.x(),next.z()-first.z());
        float endLength=(float)Math.hypot(last.x()-before.x(),last.z()-before.z());
        float extension=27;
        List<EnvironmentLayout.Point> extended=new ArrayList<>(path.size()+2);
        extended.add(new EnvironmentLayout.Point(first.x()-(next.x()-first.x())/startLength*extension,first.z()-(next.z()-first.z())/startLength*extension));
        extended.addAll(path);
        extended.add(new EnvironmentLayout.Point(last.x()+(last.x()-before.x())/endLength*extension,last.z()+(last.z()-before.z())/endLength*extension));
        return List.copyOf(extended);
    }
    private static float curve(float a,float b,float c,float d,float t) { return .5f*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t); }
    public static List<Sample> sample(List<EnvironmentLayout.Point> points) {
        List<Sample> result=new ArrayList<>();
        for(int i=0;i<points.size()-1;i++) {
            var a=points.get(Math.max(0,i-1));var b=points.get(i);var c=points.get(i+1);var d=points.get(Math.min(points.size()-1,i+2));
            int steps=Math.max(2,(int)Math.ceil(Math.hypot(c.x()-b.x(),c.z()-b.z())/.65));
            for(int j=0;j<steps;j++) {
                float t=(float)j/steps,nt=Math.min(1,t+.01f),x=curve(a.x(),b.x(),c.x(),d.x(),t),z=curve(a.z(),b.z(),c.z(),d.z(),t);
                float dx=curve(a.x(),b.x(),c.x(),d.x(),nt)-x,dz=curve(a.z(),b.z(),c.z(),d.z(),nt)-z,len=(float)Math.hypot(dx,dz);
                result.add(new Sample(x,z,-dz/len,dx/len,1.25f+.15f*(float)Math.sin(z*.23f)+.045f*(float)Math.sin(z*1.3f)));
            }
        }
        var end=points.getLast();var prev=result.getLast();result.add(new Sample(end.x(),end.z(),prev.nx,prev.nz,prev.width));return List.copyOf(result);
    }
    public static List<Sample> road(int chapter) { return ROADS.get(chapter); }
    private RoadLayout() { }
}
