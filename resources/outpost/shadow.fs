#version 330
out vec4 finalColor;
void main() {
    // RGB stores 24-bit depth; opaque alpha prevents blending from corrupting it.
    vec3 packed=fract(gl_FragCoord.z*vec3(1.0,255.0,65025.0));
    packed-=packed.yzz*vec3(1.0/255.0,1.0/255.0,0.0);
    finalColor=vec4(packed,1.0);
}
