#version 130

varying vec2 texcoord;
varying vec3 normal;

const vec3 SUN_DIRECTION_1 = vec3(0.20000000298023224f, 1.0f, -0.699999988079071f);
const vec3 SUN_DIRECTION_2 = vec3(-0.20000000298023224f, 1.0f, 0.699999988079071f);

void main(){
    float sun1 = max(0.0f, dot(normal, SUN_DIRECTION_1));
    gl_FragColor = vec4(sun1 + vec3(0.5), 1.0f);
}