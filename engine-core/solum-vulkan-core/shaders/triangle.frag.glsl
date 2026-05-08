#version 450

layout(location = 0) in vec3 inColor;
layout(location = 1) in vec2 inTexcoord0;
layout(location = 0) out vec4 fragColor;
layout(set = 0, binding = 0) uniform sampler2D baseColorTexture;

layout(push_constant) uniform PushConstants {
    layout(offset = 64) vec4 baseColorFactor;
    layout(offset = 80) float metallicFactor;
    layout(offset = 84) float roughnessFactor;
    layout(offset = 96) vec3 emissiveFactor;
    layout(offset = 108) int alphaMode;
    layout(offset = 112) int materialId;
    layout(offset = 116) int baseColorTextureReady;
} pc;

void main() {
    vec4 texel = pc.baseColorTextureReady != 0 ? texture(baseColorTexture, inTexcoord0) : vec4(1.0);
    vec3 rgb = inColor * pc.baseColorFactor.rgb * texel.rgb + pc.emissiveFactor;
    fragColor = vec4(rgb, pc.baseColorFactor.a * texel.a);
}
