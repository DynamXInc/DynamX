#version 130

attribute float myAttribute;
varying vec2 lightCoords;
varying vec2 TexCoords;

void main(){
    gl_Position = ftransform();
    TexCoords = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    float s = mod(myAttribute, 65536);
    float t = myAttribute / 65536;
    lightCoords = (gl_TextureMatrix[1] * vec4(s, t, gl_MultiTexCoord1.z,gl_MultiTexCoord0.w)).st;
}