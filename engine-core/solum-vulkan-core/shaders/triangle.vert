#version 450

layout(push_constant) uniform PushConstants {
    mat4 mvp;
    vec4 baseColorFactor;
    layout(offset = 80) float metallicFactor;
    layout(offset = 84) float roughnessFactor;
    layout(offset = 96) vec3 emissiveFactor;
    layout(offset = 108) int alphaMode;
    layout(offset = 112) int materialId;
} pc;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inTexcoord0;
layout(location = 3) in vec3 inColor;
layout(location = 0) out vec3 outColor;
layout(location = 1) out vec2 outTexcoord0;

void main() {
    outColor = inColor;
    outTexcoord0 = inTexcoord0;
    gl_Position = pc.mvp * vec4(inPosition, 1.0);
}
