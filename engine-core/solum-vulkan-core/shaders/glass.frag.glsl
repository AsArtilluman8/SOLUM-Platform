#version 450

layout(location = 0) in vec3 inColor;
layout(location = 1) in vec2 inTexcoord0;
layout(location = 2) in vec3 inNormal;
layout(location = 3) in vec4 inTangent;
layout(location = 4) in vec3 inLocalPosition;
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
    layout(offset = 204) float reflectionIntensity;
    layout(offset = 208) float contactShadowIntensity;
    layout(offset = 212) int calibrationPreset;
    layout(offset = 216) float calibrationStrength;
    layout(offset = 220) int materialTypeHint;
    layout(offset = 224) float glossSliderValue;
    layout(offset = 228) float paintGlossSliderValue;
    layout(offset = 232) int paintGlossRouting;
    layout(offset = 236) float environmentIntensity;
    layout(offset = 240) int environmentPreset;
    layout(offset = 244) float horizonStrength;
    layout(offset = 248) float alphaCutoff;
    layout(offset = 252) float emissiveIntensity;
    layout(offset = 256) int materialPresetHint;
    layout(offset = 260) float clearcoatIntensity;
    layout(offset = 264) float clearcoatRoughness;
    layout(offset = 268) float reflectionContrast;
    layout(offset = 272) float reflectionSaturation;
    layout(offset = 276) float motionReflectionScale;
    layout(offset = 280) float motionClearcoatScale;
    layout(offset = 284) int glassEnabled;
    layout(offset = 288) float glassOpacity;
    layout(offset = 292) float glassEdge;
    layout(offset = 296) float glassRoughness;
    layout(offset = 300) int glassTintPreset;
} pc;

vec3 glassTintColor(int preset) {
    if (preset == 1) return vec3(0.55, 0.82, 1.00);
    if (preset == 2) return vec3(0.55, 1.00, 0.72);
    if (preset == 3) return vec3(0.48, 0.52, 0.58);
    if (preset == 4) return vec3(1.00, 0.78, 0.52);
    if (preset == 5) return vec3(0.82, 0.58, 1.00);
    if (preset == 6) return vec3(0.70, 0.74, 0.72);
    if (preset == 7) return vec3(0.86, 0.98, 1.00);
    return vec3(0.92, 0.98, 1.00);
}

vec3 toneMap(vec3 color) {
    if (pc.toneMappingMode == 2) {
        vec3 a = color * (2.51 * color + 0.03);
        vec3 b = color * (2.43 * color + 0.59) + 0.14;
        return clamp(a / max(b, vec3(0.001)), 0.0, 1.0);
    }
    if (pc.toneMappingMode == 1) return color / (color + vec3(1.0));
    return clamp(color, 0.0, 1.0);
}

void main() {
    vec4 texel = pc.baseColorTextureReady != 0 ? texture(baseColorTexture, inTexcoord0) : vec4(1.0);
    vec4 mr = pc.metallicRoughnessTextureReady != 0 ? texture(metallicRoughnessTexture, inTexcoord0) : vec4(1.0);
    vec3 n = normalize(inNormal);
    if (!gl_FrontFacing) n = -n;
    if (pc.normalTextureReady != 0) {
        vec3 t = normalize(inTangent.xyz - n * dot(n, inTangent.xyz));
        vec3 b = normalize(cross(n, t) * (inTangent.w < 0.0 ? -1.0 : 1.0));
        vec3 mapN = texture(normalTexture, inTexcoord0).xyz * 2.0 - 1.0;
        mapN.xy *= pc.normalScale;
        n = normalize(mat3(t, b, n) * normalize(mapN));
    }

    float roughness = clamp(mix(pc.roughnessFactor * mr.g, pc.glassRoughness, 0.72), 0.04, 1.0);
    float sourceAlpha = clamp(pc.baseColorFactor.a * texel.a, 0.0, 1.0);
    bool semanticOpaqueGlass = (pc.materialTypeHint == 6 || pc.materialPresetHint == 6) && pc.alphaMode == 0 && sourceAlpha > 0.98;
    float uiOpacity = clamp(pc.glassOpacity, 0.0, 1.0);
    float semanticDensity = pow(uiOpacity, 0.86);
    float semanticBodyAlpha = clamp(0.15 + semanticDensity * 0.67, 0.16, 0.84);
    float semanticBaseInfluence = mix(0.10, 0.22, uiOpacity);
    float semanticTintInfluence = 1.0 - semanticBaseInfluence;
    float semanticRimStrength = mix(1.18, 1.58, clamp(pc.glassEdge, 0.0, 2.0) * 0.5);
    float semanticSpecularStrength = mix(0.82, 1.24, clamp(pc.glassEdge, 0.0, 2.0) * 0.5);
    float physicalAlpha = clamp(sourceAlpha * mix(0.28, 1.0, clamp(pc.glassOpacity, 0.0, 1.0)), 0.08, 0.94);
    float alpha = semanticOpaqueGlass ? semanticBodyAlpha : physicalAlpha;
    vec3 presetTint = glassTintColor(pc.glassTintPreset);
    vec3 semanticClearTint = mix(vec3(0.58, 0.72, 0.82), presetTint, 0.28);
    vec3 semanticTint = pc.glassTintPreset == 0 ? semanticClearTint : presetTint;
    vec3 tint = mix(vec3(1.0), semanticOpaqueGlass ? semanticTint : presetTint, semanticOpaqueGlass ? 0.98 : 0.74);
    vec3 sourceBase = max(inColor * pc.baseColorFactor.rgb * texel.rgb, vec3(0.18));
    if (semanticOpaqueGlass) {
        sourceBase = mix(sourceBase, vec3(dot(sourceBase, vec3(0.2126, 0.7152, 0.0722))), 0.80);
    }
    vec3 base = semanticOpaqueGlass ? tint * semanticTintInfluence + sourceBase * semanticBaseInfluence : mix(sourceBase, tint, 0.64);
    vec3 v = normalize(vec3(0.0, 0.0, 1.0));
    float smoothNormalWeight = clamp(0.12 + roughness * 0.12, 0.0, 0.24);
    n = normalize(mix(n, v, smoothNormalWeight));
    float ndotv = clamp(dot(n, v), 0.0, 1.0);
    float rim = pow(1.0 - ndotv, mix(2.25, 1.05, clamp(pc.glassEdge, 0.0, 2.0) * 0.5));
    vec3 l = normalize(-vec3(pc.sunDirectionX, pc.sunDirectionY, pc.sunDirectionZ));
    vec3 h = normalize(l + v);
    vec3 r = reflect(-v, n);
    float sky = clamp(r.y * 0.5 + 0.5, 0.0, 1.0);
    float stripe = pow(1.0 - abs(r.y * 1.8 - 0.18), 12.0) * (1.0 - roughness * 0.72);
    float specPower = mix(96.0, 18.0, roughness);
    float spec = pow(max(dot(n, h), 0.0), specPower) * mix(1.45, 0.38, roughness);
    vec3 sun = vec3(pc.sunColorR, pc.sunColorG, pc.sunColorB) * pc.sunIntensity;
    vec3 ambient = vec3(pc.ambientColorR, pc.ambientColorG, pc.ambientColorB) * max(pc.ambientIntensity, pc.ambientFloor);
    vec3 edge = tint * rim * clamp(pc.glassEdge, 0.0, 2.0);
    vec3 fakeReflection = mix(vec3(0.12, 0.16, 0.18), vec3(0.72, 0.86, 1.0), sky);
    fakeReflection += vec3(0.95, 0.98, 1.0) * stripe * 0.30;
    float bodyVisibility = semanticOpaqueGlass ? mix(0.56, 1.08, semanticDensity) : 1.0;
    float rimVisibility = semanticOpaqueGlass ? semanticRimStrength : 1.0;
    vec3 rgb = base * (ambient * 0.50 + vec3(0.24)) * bodyVisibility;
    rgb += fakeReflection * (0.28 + rim * 0.46 + alpha * 0.12) * mix(1.0, 0.40, roughness) * rimVisibility;
    rgb += sun * edge * mix(0.62, 1.22, 1.0 - roughness) * rimVisibility;
    rgb += sun * spec * (0.32 + clamp(pc.glassEdge, 0.0, 2.0) * 0.34) * rimVisibility * (semanticOpaqueGlass ? semanticSpecularStrength : 1.0);
    if (semanticOpaqueGlass) {
        float diagonal = pow(1.0 - abs(fract(gl_FragCoord.x * 0.010 + gl_FragCoord.y * 0.018) - 0.5) * 2.0, 18.0);
        vec3 semanticHighlight = vec3(0.78, 0.90, 1.0) * (rim * 0.24 + spec * 0.30 + stripe * 0.12 + diagonal * 0.055);
        rgb += semanticHighlight * rimVisibility;
    }
    rgb = max(rgb, tint * (semanticOpaqueGlass ? mix(0.18, 0.34, semanticDensity) : 0.08));
    rgb = toneMap(rgb * pc.exposureValue);
    if (pc.activeDebugView == 58) {
        float pulse = 0.5 + 0.5 * step(0.5, fract(gl_FragCoord.x * 0.08 + gl_FragCoord.y * 0.08));
        fragColor = vec4(mix(vec3(0.0, 1.0, 0.95), vec3(1.0, 0.12, 0.95), pulse), 0.82);
        return;
    }
    fragColor = vec4(rgb, alpha);
}
