#version 130

varying vec2 texcoord;
varying vec3 normal;

void main(){
    // Scale the UV coordinates to create a repeating color pattern
    vec2 scaledUV = texcoord * 10.0;

    // Use the fractional part of the scaled coordinates to create a repeating pattern
    vec2 fractUV = fract(scaledUV);
    gl_FragColor = vec4(normal, 1.0f);
}