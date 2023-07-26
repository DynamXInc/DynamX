#version 130

uniform sampler2D diffuseMap;
uniform sampler2D lightMap;
varying vec2 lightCoords;
varying vec2 TexCoords;

const vec3 SUN_DIRECTION_1 = vec3(0.20000000298023224f, 1.0f, -0.699999988079071f);
const vec3 SUN_DIRECTION_2 = vec3(-0.20000000298023224f, 1.0f, 0.699999988079071f);

const vec3 AMBIENT_LIGHT_MODEL = vec3(0.4f);
const vec3 DIFFUSE_LIGHT_COLOR = vec3(0.6f);

vec3 minecraftFlatShading(vec3 normal){
    vec3 sunDir1 = normalize(SUN_DIRECTION_1);
    vec3 sunDir2 = normalize(SUN_DIRECTION_2);
    vec3 diffuse1 = vec3(max(dot(sunDir1, normal), 0.0));
    vec3 diffuse2 = vec3(max(dot(sunDir2, normal), 0.0));
    return AMBIENT_LIGHT_MODEL + (diffuse1 * DIFFUSE_LIGHT_COLOR) + (diffuse2 * DIFFUSE_LIGHT_COLOR);
}


void main(){
    vec4 diffuse = texture2D(diffuseMap, TexCoords);
    vec4 light = texture2D(lightMap, lightCoords);
    //light = clamp(light, 0.f, 1.f);
    gl_FragColor = light * diffuse;
}