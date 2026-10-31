import static com.raylib.Raylib.*;
/** Diagnostic of actual vertex deformation, rather than only moving socket bones. */
public class SkinningProbe {
    public static void main(String[] args) {
        SetConfigFlags(FLAG_WINDOW_HIDDEN);InitWindow(64,64,"Skinning probe");
        String file=args.length>0?args[0]:"resources/outpost/raider.glb",name=args.length>1?args[1]:"Death_A";Model model=LoadModel(file);int[] count={0};ModelAnimation clips=LoadModelAnimations(file,count);
        Shader shader=LoadShader("resources/outpost/daylight.vs","resources/outpost/daylight.fs");
        for(int i=0;i<model.materialCount();i++)new Material(model.materials()).position(i).shader().put(shader);
        int clipIndex=0;for(int i=0;i<count[0];i++)if(new ModelAnimation(clips).position(i).name().getString().equals(name))clipIndex=i;
        ModelAnimation clip=new ModelAnimation(clips).position(clipIndex);
        for(float f:new float[]{0,clip.keyframeCount()*.4f,clip.keyframeCount()*.8f,clip.keyframeCount()-2,clip.keyframeCount()-1}) {
            UpdateModelAnimation(model,clip,f);float min=100,max=-100;boolean animated=false;
            for(int m=0;m<model.meshCount();m++) {
                Mesh mesh=new Mesh(model.meshes()).position(m);var vertices=mesh.animVertices();
                if(vertices==null || vertices.isNull())vertices=mesh.vertices();else animated=true;
                for(int v=0;v<mesh.vertexCount();v++) { min=Math.min(min,vertices.get(v*3+1));max=Math.max(max,vertices.get(v*3+1)); }
            }
            System.out.println("PROBE frame="+f+" animated="+animated+" minY="+min+" maxY="+max+" shaderBoneIds="+shader.locs().get(SHADER_LOC_VERTEX_BONEIDS));
            for(int b=0;b<model.skeleton().boneCount();b++)if(new BoneInfo(model.skeleton().bones()).position(b).name().getString().equals("head"))System.out.println("HEAD "+new Transform(model.currentPose()).position(b).translation().y());
        }
        UnloadModelAnimations(clips,count[0]);UnloadModel(model);UnloadShader(shader);CloseWindow();
    }
}
