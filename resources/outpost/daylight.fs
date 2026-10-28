#version 330
in vec2 uv;
in vec3 normal;
in vec3 world;
in vec4 tint;
uniform sampler2D texture0;
uniform vec4 colDiffuse;
out vec4 finalColor;
void main() {
    vec4 base=texture(texture0,uv)*colDiffuse*tint;
    vec3 n=normalize(normal);
    float sun=max(dot(n,normalize(vec3(-0.55,0.85,0.35))),0.0);
    vec3 light=vec3(0.34,0.43,0.48)+sun*vec3(0.81,0.73,0.57);
    vec3 rgb=base.rgb*light;
    float fog=smoothstep(24.0,48.0,length(world.xz))*0.48;
    finalColor=vec4(mix(rgb,vec3(0.53,0.65,0.67),fog),base.a);
}
