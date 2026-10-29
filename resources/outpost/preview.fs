#version 330
in vec2 uv;
in vec3 normal;
in vec4 tint;
uniform sampler2D texture0;
uniform vec4 colDiffuse;
out vec4 finalColor;
void main() {
    vec4 c=texture(texture0,uv)*colDiffuse*tint;
    float light=.65+.55*max(dot(normalize(normal),normalize(vec3(-.5,1,.8))),0.0);
    finalColor=vec4(c.rgb*light,c.a);
}
