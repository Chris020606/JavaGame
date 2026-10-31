package Entities;
import static com.raylib.Raylib.*;

/** Bounds of deformed vertices, rather than the undeformed bind-pose box. */
public final class SkinBounds {
    private static float[] scratch=new float[4096];
    private SkinBounds() { }
    public static float[] vertical(Model model) {
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(int m=0;m<model.meshCount();m++) {
            Mesh mesh=new Mesh(model.meshes()).position(m);int count=mesh.vertexCount()*3;if(count<=0)continue;
            var vertices=mesh.animVertices();if(vertices==null || vertices.isNull())vertices=mesh.vertices();
            if(vertices==null || vertices.isNull())continue;
            if(scratch.length<count)scratch=new float[count];vertices.position(0).get(scratch,0,count);
            for(int i=1;i<count;i+=3) { min=Math.min(min,scratch[i]);max=Math.max(max,scratch[i]); }
        }
        return Float.isFinite(min)&&Float.isFinite(max)?new float[]{min,max}:new float[]{0,0};
    }
}
