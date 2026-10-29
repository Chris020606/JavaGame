#version 330
in vec2 fragTexCoord;
in vec4 fragColor;
uniform sampler2D texture0;
uniform vec2 resolution;
uniform float bloomStrength;
out vec4 finalColor;
float luma(vec3 c) { return dot(c,vec3(.299,.587,.114)); }
vec3 sampleAt(vec2 uv) { return texture(texture0,clamp(uv,vec2(.001),vec2(.999))).rgb; }
vec3 aces(vec3 x) { return clamp((x*(2.51*x+.03))/(x*(2.43*x+.59)+.14),0.0,1.0); }
void main() {
    vec2 uv=fragTexCoord,p=1.0/resolution;
    vec3 nw=sampleAt(uv+vec2(-1,-1)*p),ne=sampleAt(uv+vec2(1,-1)*p);
    vec3 sw=sampleAt(uv+vec2(-1,1)*p),se=sampleAt(uv+vec2(1,1)*p),center=sampleAt(uv);
    float lnw=luma(nw),lne=luma(ne),lsw=luma(sw),lse=luma(se),lc=luma(center);
    vec2 dir=vec2(-(lnw+lne-lsw-lse),lnw+lsw-lne-lse);
    float reduce=max((lnw+lne+lsw+lse)*.03125,.0078);
    dir=clamp(dir/(min(abs(dir.x),abs(dir.y))+reduce),vec2(-6),vec2(6))*p;
    vec3 aa=.5*(sampleAt(uv+dir*(1.0/3.0-.5))+sampleAt(uv+dir*(2.0/3.0-.5)));
    vec3 ab=aa*.5+.25*(sampleAt(uv-dir*.5)+sampleAt(uv+dir*.5));
    float lb=luma(ab),low=min(lc,min(min(lnw,lne),min(lsw,lse))),high=max(lc,max(max(lnw,lne),max(lsw,lse)));
    vec3 color=(lb<low || lb>high)?aa:ab;
    vec3 bloom=vec3(0);
    for(int i=0;i<8;i++) {
        float a=float(i)*.785398;
        vec2 o=vec2(cos(a),sin(a))*p;
        bloom+=max(sampleAt(uv+o*4.0)-.68,0.0)*.07;
        bloom+=max(sampleAt(uv+o*10.0)-.68,0.0)*.055;
    }
    color=aces(color*1.15+bloom*bloomStrength*4.0);
    color=pow(color,vec3(1.0/2.2));
    float vignette=1.0-.10*pow(length((uv-.5)*vec2(1.0,.8)),1.5);
    finalColor=vec4(color*vignette,1);
}
