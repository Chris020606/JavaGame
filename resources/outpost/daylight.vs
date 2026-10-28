#version 330
in vec3 vertexPosition;
in vec2 vertexTexCoord;
in vec3 vertexNormal;
in vec4 vertexColor;
uniform mat4 mvp;
uniform mat4 matModel;
uniform mat4 matNormal;
out vec2 uv;
out vec3 normal;
out vec3 world;
out vec4 tint;
void main() {
    uv=vertexTexCoord;
    normal=normalize(vec3(matNormal*vec4(vertexNormal,0.0)));
    world=vec3(matModel*vec4(vertexPosition,1.0));
    tint=vertexColor;
    gl_Position=mvp*vec4(vertexPosition,1.0);
}
