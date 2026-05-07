#version 450

layout(location = 0) in vec3 inColor;
layout(location = 0) out vec4 fragColor;

layout(push_constant) uniform PushConstants {
    layout(offset = 64) vec4 baseColorFactor;
    layout(offset = 80) float metallicFactor;
    layout(offset = 84) float roughnessFactor;
    layout(offset = 96) vec3 emissiveFactor;
    layout(offset = 108) int alphaMode;
    layout(offset = 112) int materialId;
} pc;

void main() {
    fragColor = vec4(inColor * pc.baseColorFactor.rgb + pc.emissiveFactor, pc.baseColorFactor.a);
}
