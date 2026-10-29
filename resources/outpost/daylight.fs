#version 330
in vec2 uv;
in vec3 normal;
in vec3 world;
in vec4 tint;
uniform sampler2D texture0;
uniform sampler2D shadowMap;
uniform vec4 colDiffuse;
uniform mat4 sunVP;
uniform vec3 viewPos;
uniform vec3 bossPos;
uniform float bossGlow;
uniform float shadowSize;
uniform vec2 material;
uniform float environment;
uniform float uTime;
out vec4 finalColor;
const float PI=3.14159265;
float depthAt(vec2 p) { return dot(texture(shadowMap,p).rgb,vec3(1.0,1.0/255.0,1.0/65025.0)); }
float visibility(vec3 n,vec3 l) {
    vec4 q=sunVP*vec4(world,1.0);vec3 p=q.xyz/q.w*.5+.5;
    if(p.x<=.001 || p.x>=.999 || p.y<=.001 || p.y>=.999 || p.z>=1.0) return 1.0;
    float bias=max(.000018,.000065*(1.0-dot(n,l)));
    float sum=0.0;
    for(int x=-1;x<=1;x++) for(int y=-1;y<=1;y++)
        sum+=p.z-bias<=depthAt(p.xy+vec2(x,y)/shadowSize)?1.0:0.0;
    return .18+.82*sum/9.0;
}
vec3 fresnel(float cosine,vec3 f0) { return f0+(1.0-f0)*pow(1.0-cosine,5.0); }
void main() {
    vec4 tex=texture(texture0,uv)*colDiffuse*tint;
    vec3 albedo=pow(max(tex.rgb,vec3(0.0)),vec3(2.2));
    vec3 n=normalize(normal);
    if(material.x<.28 && material.y<.15 && colDiffuse.b>colDiffuse.g && colDiffuse.b>colDiffuse.r*1.4)
        n=normalize(n+vec3(sin(world.x*3.0+uTime*1.6)*.065,0,cos(world.z*4.0+uTime*1.2)*.055));
    vec3 v=normalize(viewPos-world),l=normalize(vec3(-35.0,55.0,28.0)),h=normalize(v+l);
    float rough=clamp(material.x,.16,.95),metal=material.y;
    float nv=max(dot(n,v),.001),nl=max(dot(n,l),0.0),nh=max(dot(n,h),0.0),hv=max(dot(h,v),0.0);
    float a=rough*rough,a2=a*a,d=nh*nh*(a2-1.0)+1.0;
    float distribution=a2/max(PI*d*d,.001);
    float k=(rough+1.0)*(rough+1.0)/8.0;
    float geometry=nv/(nv*(1.0-k)+k)*nl/max(nl*(1.0-k)+k,.001);
    vec3 f=fresnel(hv,mix(vec3(.04),albedo,metal));
    vec3 spec=distribution*geometry*f/max(4.0*nv*max(nl,.001),.001);
    float shadow=visibility(n,l);
    vec3 sun=((1.0-f)*(1.0-metal)*albedo/PI+spec)*(environment<.5?vec3(3.4,2.85,2.2):environment<1.5?vec3(1.6,.9,.6):environment<2.5?vec3(2.4,2.8,3.3):vec3(.35,.42,.9))*nl*shadow;
    vec3 hemi=mix(vec3(.16,.19,.12),environment>2.5?vec3(.20,.22,.43):environment>1.5?vec3(.53,.65,.82):environment>.5?vec3(.35,.30,.41):vec3(.43,.57,.72),n.y*.5+.5);
    vec3 color=albedo*hemi*(environment<.5?.52:.75)+sun;
    // Local emissive sources illuminate the camp, sanctuary, and boss silhouette.
    vec3 camp=environment<.5?vec3(-9,.7,-8):environment>1.5 && environment<2.5?vec3(-9,.7,8):vec3(0,.7,-18);
    color+=albedo*vec3(2.4,.65,.12)/(1.0+dot(world-camp,world-camp)*.6);
    color+=albedo*vec3(.08,.6,.5)/(1.0+dot(world-vec3(0,1.5,0),world-vec3(0,1.5,0))*.14);
    color+=albedo*vec3(.75,.12,1.4)*bossGlow/(1.0+dot(world-bossPos,world-bossPos)*.3);
    color+=pow(1.0-nv,4.0)*vec3(.025,.05,.065);
    float dist=length(viewPos-world);
    float fog=clamp((1.0-exp(-dist*(environment>2.5?.016:environment>.5 && environment<1.5?.014:environment<.5?.004:.008)))*exp(-max(world.y,0.0)*.06),0.0,.70);
    color=mix(color,environment>2.5?vec3(.03,.035,.075):environment>1.5?vec3(.42,.55,.65):environment>.5?vec3(.16,.12,.16):vec3(.23,.37,.43),fog);
    finalColor=vec4(color,tex.a);
}
