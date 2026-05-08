#version 450

layout(location = 0) in vec3 inColor;
layout(location = 1) in vec2 inTexcoord0;
layout(location = 0) out vec4 fragColor;
layout(set = 0, binding = 0) uniform sampler2D baseColorTexture;
layout(set = 0, binding = 1) uniform sampler2D metallicRoughnessTexture;
layout(set = 0, binding = 2) uniform sampler2D normalTexture;
layout(set = 0, binding = 3) uniform sampler2D occlusionTexture;

layout(push_constant) uniform PushConstants {
    layout(offset = 64) vec4 baseColorFactor;
    layout(offset = 80) float metallicFactor;
    layout(offset = 84) float roughnessFactor;
    layout(offset = 88) float normalScale;
    layout(offset = 92) float occlusionStrength;
    layout(offset = 96) vec3 emissiveFactor;
    layout(offset = 108) int alphaMode;
    layout(offset = 112) int materialId;
    layout(offset = 116) int baseColorTextureReady;
    layout(offset = 120) int metallicRoughnessTextureReady;
    layout(offset = 124) int normalTextureReady;
    layout(offset = 128) int occlusionTextureReady;
} pc;

void main() {
    vec4 texel = pc.baseColorTextureReady != 0 ? texture(baseColorTexture, inTexcoord0) : vec4(1.0);
    vec4 mr = pc.metallicRoughnessTextureReady != 0 ? texture(metallicRoughnessTexture, inTexcoord0) : vec4(1.0);
    float roughness = clamp(pc.roughnessFactor * mr.g, 0.04, 1.0);
    float metallic = clamp(pc.metallicFactor * mr.b, 0.0, 1.0);
    vec3 normalSample = pc.normalTextureReady != 0 ? texture(normalTexture, inTexcoord0).xyz * 2.0 - 1.0 : vec3(0.0, 0.0, 1.0);
    float ao = pc.occlusionTextureReady != 0 ? mix(1.0, texture(occlusionTexture, inTexcoord0).r, pc.occlusionStrength) : 1.0;
    vec3 pbrDebug = vec3(metallic * 0.0, roughness * 0.0, normalSample.z * 0.0);
    vec3 rgb = (inColor * pc.baseColorFactor.rgb * texel.rgb * ao) + pc.emissiveFactor + pbrDebug;
    fragColor = vec4(rgb, pc.baseColorFactor.a * texel.a);
}
