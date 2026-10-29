#version 330
in vec3 vertexPosition;
in vec2 vertexTexCoord;
in vec3 vertexNormal;
in vec4 vertexColor;
uniform mat4 mvp;
uniform mat4 matModel;
uniform mat4 matNormal;
uniform vec4 colDiffuse;
uniform float uTime;
out vec2 uv;
out vec3 normal;
out vec3 world;
out vec4 tint;
void main() {
    vec3 p=vertexPosition;
    bool foliage=colDiffuse.g>colDiffuse.r*1.2 && colDiffuse.g>colDiffuse.b*1.15;
    if(foliage) p.x+=sin(uTime*1.3+p.y*2.0+p.z+matModel[3].x*.3)*max(0.0,p.y-.15)*.035;
    uv=vertexTexCoord;
    normal=normalize(vec3(matNormal*vec4(vertexNormal,0.0)));
    world=vec3(matModel*vec4(p,1.0));tint=vertexColor;
    gl_Position=mvp*vec4(p,1.0);
}
