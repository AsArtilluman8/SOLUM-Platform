#version 450

layout(location = 0) in vec3 inColor;
layout(location = 1) in vec2 inTexcoord0;
layout(location = 2) in vec3 inNormal;
layout(location = 3) in vec4 inTangent;
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
    layout(offset = 132) float sunDirectionX;
    layout(offset = 136) float sunDirectionY;
    layout(offset = 140) float sunDirectionZ;
    layout(offset = 144) float sunIntensity;
    layout(offset = 148) float sunColorR;
    layout(offset = 152) float sunColorG;
    layout(offset = 156) float sunColorB;
    layout(offset = 160) float ambientIntensity;
    layout(offset = 164) float ambientColorR;
    layout(offset = 168) float ambientColorG;
    layout(offset = 172) float ambientColorB;
    layout(offset = 176) int lightPreset;
    layout(offset = 180) int activeDebugView;
    layout(offset = 184) int toneMappingMode;
    layout(offset = 188) float exposureValue;
    layout(offset = 192) float ambientFloor;
    layout(offset = 196) int brightnessPreset;
    layout(offset = 200) float specularBoost;
} pc;

vec3 toneMap(vec3 color) {
    if (pc.toneMappingMode == 2) {
        vec3 a = color * (2.51 * color + 0.03);
        vec3 b = color * (2.43 * color + 0.59) + 0.14;
        return clamp(a / max(b, vec3(0.001)), 0.0, 1.0);
    }
    if (pc.toneMappingMode == 1) {
        return color / (color + vec3(1.0));
    }
    return clamp(color, 0.0, 1.0);
}

vec3 fresnelSchlick(float cosTheta, vec3 f0) {
    float f = pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
    return f0 + (1.0 - f0) * f;
}

void main() {
    vec4 texel = pc.baseColorTextureReady != 0 ? texture(baseColorTexture, inTexcoord0) : vec4(1.0);
    vec4 mr = pc.metallicRoughnessTextureReady != 0 ? texture(metallicRoughnessTexture, inTexcoord0) : vec4(1.0);
    float roughness = clamp(pc.roughnessFactor * mr.g, 0.04, 1.0);
    float metallic = clamp(pc.metallicFactor * mr.b, 0.0, 1.0);
    float ao = pc.occlusionTextureReady != 0 ? mix(1.0, texture(occlusionTexture, inTexcoord0).r, pc.occlusionStrength) : 1.0;
    vec3 baseColor = inColor * pc.baseColorFactor.rgb * texel.rgb;
    if (pc.activeDebugView == 1) {
        fragColor = vec4(baseColor, pc.baseColorFactor.a * texel.a);
        return;
    }
    vec3 n = normalize(inNormal);
    if (pc.normalTextureReady != 0) {
        vec3 t = normalize(inTangent.xyz - n * dot(n, inTangent.xyz));
        vec3 b = normalize(cross(n, t) * (inTangent.w < 0.0 ? -1.0 : 1.0));
        vec3 mapN = texture(normalTexture, inTexcoord0).xyz * 2.0 - 1.0;
        mapN.xy *= pc.normalScale;
        n = normalize(mat3(t, b, n) * normalize(mapN));
    }
    vec3 l = normalize(-vec3(pc.sunDirectionX, pc.sunDirectionY, pc.sunDirectionZ));
    vec3 v = normalize(vec3(0.0, 0.0, 1.0));
    vec3 h = normalize(l + v);
    vec3 sunColor = vec3(pc.sunColorR, pc.sunColorG, pc.sunColorB);
    vec3 ambientColor = vec3(pc.ambientColorR, pc.ambientColorG, pc.ambientColorB);
    float ndotl = max(dot(n, l), 0.0);
    if (pc.activeDebugView == 2) {
        fragColor = vec4(n * 0.5 + 0.5, pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 3) {
        fragColor = vec4(vec3(roughness), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 4) {
        fragColor = vec4(vec3(metallic), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 5) {
        fragColor = vec4(vec3(ao), pc.baseColorFactor.a * texel.a);
        return;
    }
    vec3 specColor = mix(vec3(0.04), baseColor, metallic);
    vec3 f0 = specColor;
    vec3 fresnel = fresnelSchlick(max(dot(h, v), 0.0), f0);
    vec3 diffuseTerm = baseColor * (1.0 - metallic) * (vec3(1.0) - fresnel);
    vec3 diffuseLight = diffuseTerm * ndotl * sunColor * pc.sunIntensity;
    float specPower = mix(96.0, 8.0, roughness);
    float specWidth = pow(max(dot(n, h), 0.0), specPower);
    float specEnergy = mix(1.0, 0.28, roughness);
    float rim = pow(clamp(1.0 - max(dot(n, v), 0.0), 0.0, 1.0), mix(4.0, 1.6, roughness));
    vec3 analyticEnvColor = mix(vec3(0.42, 0.52, 0.62), vec3(0.88, 0.92, 1.0), clamp(n.y * 0.5 + 0.5, 0.0, 1.0));
    vec3 analyticSpecular = fresnel * analyticEnvColor * rim * mix(0.06, 0.28, metallic) * (1.0 - roughness * 0.55);
    vec3 specularLight = (fresnel * specWidth * specEnergy * ndotl * sunColor * pc.sunIntensity + analyticSpecular) * pc.specularBoost;
    if (pc.activeDebugView == 6) {
        fragColor = vec4(toneMap(diffuseLight * pc.exposureValue), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 7) {
        fragColor = vec4(toneMap(specularLight * pc.exposureValue * 3.0), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 8) {
        fragColor = vec4(f0, pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 9) {
        fragColor = vec4(pc.baseColorTextureReady != 0 ? 0.1 : 0.85, pc.metallicRoughnessTextureReady != 0 ? 0.85 : 0.1, pc.normalTextureReady != 0 ? 0.85 : 0.1, 1.0);
        return;
    }
    vec3 ambient = baseColor * (1.0 - metallic * 0.65) * ambientColor * max(pc.ambientIntensity, pc.ambientFloor);
    vec3 rgb = (diffuseLight + ambient) * ao + specularLight + pc.emissiveFactor;
    rgb *= pc.exposureValue;
    rgb = toneMap(rgb);
    fragColor = vec4(rgb, pc.baseColorFactor.a * texel.a);
}
