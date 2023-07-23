#version 130

varying vec2 texcoord;
varying vec3 normal;

void main(){
    gl_Position = ftransform();
    normal = gl_NormalMatrix * gl_Normal;
    texcoord = gl_MultiTexCoord0.xy;
}