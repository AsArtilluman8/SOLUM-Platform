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
    layout(offset = 304) int glassRenderMode;
    layout(offset = 308) float glassClarity;
    layout(offset = 312) float glassThickness;
} pc;

float luminance(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

vec3 normalizeAlbedoEnergy(vec3 color, float strength) {
    float lum = luminance(color);
    float visualStrength = clamp(0.28 + strength * 0.72, 0.28, 1.0);
    float limit = mix(1.0, 0.76, visualStrength);
    if (lum > limit) color *= limit / max(lum, 0.001);
    return clamp(color, vec3(0.0), vec3(mix(1.0, 0.88, visualStrength)));
}

float remapRoughness(float roughness, float metallic, int hint, float strength) {
    float visualStrength = clamp(0.28 + strength * 0.72, 0.28, 1.0);
    float target = roughness;
    if (hint == 0) target = max(roughness, 0.88);
    else if (hint == 3) target = max(roughness, 0.68);
    else if (hint == 1) target = clamp(roughness, 0.18, 0.62);
    else if (hint == 2) target = clamp(roughness, 0.08, 0.52);
    else if (hint == 5) target = max(roughness, 0.48);
    else if (hint == 6) target = clamp(roughness, 0.18, 0.70);
    else if (hint == 7) target = clamp(roughness, 0.22, 0.68);
    else target = clamp(roughness, 0.22, 0.86);
    float presetBias = pc.calibrationPreset == 1 ? 0.18 : (pc.calibrationPreset == 3 ? -0.16 : 0.0);
    return clamp(mix(roughness, target + presetBias * (1.0 - metallic), visualStrength), 0.06, 1.0);
}

vec3 materialTypeColor(int hint) {
    if (hint == 0) return vec3(0.66, 0.55, 0.92);
    if (hint == 1) return vec3(0.90, 0.42, 0.32);
    if (hint == 2) return vec3(0.70, 0.78, 0.86);
    if (hint == 3) return vec3(0.08, 0.10, 0.11);
    if (hint == 5) return vec3(0.20, 0.86, 0.42);
    if (hint == 6) return vec3(0.38, 0.78, 0.95);
    if (hint == 7) return vec3(0.94, 0.78, 0.28);
    return vec3(0.45, 0.48, 0.50);
}

vec3 materialPresetColor(int preset) {
    if (preset == 1) return vec3(0.95, 0.28, 0.18);
    if (preset == 2) return vec3(0.76, 0.82, 0.90);
    if (preset == 3) return vec3(0.54, 0.42, 0.72);
    if (preset == 4) return vec3(0.06, 0.07, 0.07);
    if (preset == 5) return vec3(0.58, 0.62, 0.68);
    if (preset == 6) return vec3(0.38, 0.80, 0.96);
    if (preset == 7) return vec3(0.20, 0.86, 1.00);
    return vec3(0.52, 0.62, 0.60);
}

vec3 glassTintColor(int preset) {
    if (preset == 1) return vec3(0.88, 0.98, 1.00);
    if (preset == 2) return vec3(0.46, 0.72, 1.00);
    if (preset == 3) return vec3(0.55, 1.00, 0.72);
    if (preset == 4) return vec3(0.42, 0.46, 0.52);
    if (preset == 5) return vec3(1.00, 0.78, 0.52);
    if (preset == 6) return vec3(0.18, 0.22, 0.28);
    return vec3(0.94, 0.99, 1.00);
}

vec4 glassPresetParams(int preset) {
    if (preset == 1) return vec4(0.18, 1.95, 0.04, 1.40);
    if (preset == 2) return vec4(0.24, 1.70, 0.12, 1.16);
    if (preset == 3) return vec4(0.26, 1.62, 0.14, 1.10);
    if (preset == 4) return vec4(0.32, 1.42, 0.30, 0.90);
    if (preset == 5) return vec4(0.28, 1.52, 0.16, 1.04);
    if (preset == 6) return vec4(0.40, 1.34, 0.44, 0.78);
    return vec4(0.20, 1.78, 0.07, 1.30);
}

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

float distributionGGX(vec3 n, vec3 h, float roughness) {
    float a = max(roughness * roughness, 0.045);
    float a2 = a * a;
    float ndoth = max(dot(n, h), 0.0);
    float denom = ndoth * ndoth * (a2 - 1.0) + 1.0;
    return a2 / max(3.14159 * denom * denom, 0.0001);
}

float geometrySchlickGGX(float ndotv, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) * 0.125;
    return ndotv / max(ndotv * (1.0 - k) + k, 0.0001);
}

float geometrySmith(vec3 n, vec3 v, vec3 l, float roughness) {
    return geometrySchlickGGX(max(dot(n, v), 0.0), roughness) * geometrySchlickGGX(max(dot(n, l), 0.0), roughness);
}

vec3 saturateReflection(vec3 color, float saturation) {
    float lum = luminance(color);
    return mix(vec3(lum), color, clamp(saturation, 0.0, 2.0));
}

vec3 contrastReflection(vec3 color, float contrast) {
    vec3 pivot = vec3(0.42);
    return max(vec3(0.0), pivot + (color - pivot) * clamp(contrast, 0.0, 2.0));
}

void environmentPalette(out vec3 groundColor, out vec3 horizonColor, out vec3 skyColor, out vec3 sideColor, out vec3 glintColor) {
    if (pc.environmentPreset == 1) {
        groundColor = vec3(0.23, 0.31, 0.19);
        horizonColor = vec3(0.76, 0.84, 0.92);
        skyColor = vec3(0.42, 0.72, 1.00);
        sideColor = vec3(0.96, 0.82, 0.48);
        glintColor = vec3(1.00, 0.92, 0.62);
    } else if (pc.environmentPreset == 2) {
        groundColor = vec3(0.34, 0.29, 0.24);
        horizonColor = vec3(0.82, 0.60, 0.38);
        skyColor = vec3(0.98, 0.86, 0.68);
        sideColor = vec3(0.92, 0.42, 0.18);
        glintColor = vec3(1.00, 0.72, 0.38);
    } else if (pc.environmentPreset == 3) {
        groundColor = vec3(0.20, 0.25, 0.30);
        horizonColor = vec3(0.52, 0.68, 0.86);
        skyColor = vec3(0.72, 0.86, 1.00);
        sideColor = vec3(0.22, 0.62, 0.96);
        glintColor = vec3(0.58, 0.80, 1.00);
    } else if (pc.environmentPreset == 4) {
        groundColor = vec3(0.28, 0.20, 0.24);
        horizonColor = vec3(0.98, 0.48, 0.26);
        skyColor = vec3(0.34, 0.42, 0.70);
        sideColor = vec3(1.00, 0.42, 0.20);
        glintColor = vec3(1.00, 0.74, 0.28);
    } else if (pc.environmentPreset == 5) {
        groundColor = vec3(0.08, 0.09, 0.10);
        horizonColor = vec3(0.18, 0.21, 0.26);
        skyColor = vec3(0.26, 0.32, 0.44);
        sideColor = vec3(0.16, 0.24, 0.30);
        glintColor = vec3(0.38, 0.50, 0.62);
    } else {
        groundColor = vec3(0.30, 0.28, 0.26);
        horizonColor = vec3(0.64, 0.70, 0.78);
        skyColor = vec3(0.82, 0.88, 0.96);
        sideColor = vec3(0.72, 0.76, 0.82);
        glintColor = vec3(1.00, 0.96, 0.84);
    }
}

vec3 environmentColor(vec3 dir, float roughness) {
    float sky = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    float side = pow(clamp(1.0 - abs(dir.x), 0.0, 1.0), 1.25) * (1.0 - smoothstep(0.62, 1.0, abs(dir.y)));
    vec3 groundColor;
    vec3 horizonColor;
    vec3 skyColor;
    vec3 sideColor;
    vec3 glintColor;
    environmentPalette(groundColor, horizonColor, skyColor, sideColor, glintColor);
    vec3 sharpColor = mix(mix(groundColor, horizonColor, smoothstep(0.0, 0.52, sky)), skyColor, smoothstep(0.44, 1.0, sky));
    float horizonLine = 1.0 - smoothstep(0.028, 0.24, abs(dir.y - 0.08));
    float glint = pow(max(dot(normalize(dir), normalize(vec3(0.58, 0.22, 0.78))), 0.0), mix(14.0, 5.0, roughness));
    float rimSide = pow(clamp(1.0 - abs(dir.z), 0.0, 1.0), 1.4) * (1.0 - roughness * 0.55);
    sharpColor = mix(sharpColor, sideColor, clamp(side * (1.0 - roughness * 0.62) * 0.72 + rimSide * 0.18, 0.0, 0.82));
    sharpColor += glintColor * horizonLine * (1.0 - roughness * 0.65) * clamp(pc.horizonStrength, 0.0, 1.0) * 1.18;
    sharpColor += glintColor * glint * (1.0 - roughness) * 0.50;
    vec3 blurredColor = mix(mix(groundColor, horizonColor, 0.45), mix(horizonColor, skyColor, 0.55), sky);
    vec3 color = mix(sharpColor, blurredColor, clamp(roughness, 0.0, 1.0));
    color = contrastReflection(saturateReflection(color, pc.reflectionSaturation), pc.reflectionContrast);
    return color * clamp(pc.environmentIntensity, 0.0, 2.0);
}

float materialGlossWeight(int hint, float metallic, float roughness) {
    float glossSlider = clamp(pc.glossSliderValue, 0.0, 1.0);
    float paintSlider = clamp(pc.paintGlossSliderValue, 0.0, 1.0);
    float base = mix(0.18, 1.18, glossSlider) * mix(0.30, 1.0, metallic);
    if (hint == 0) base *= mix(0.04, 0.14, glossSlider);
    else if (hint == 3) base *= mix(0.08, 0.28, glossSlider);
    else if (hint == 5) base *= mix(0.12, 0.40, glossSlider);
    else if (hint == 6) base *= 0.38;
    else if (hint == 7) base *= mix(0.18, 0.55, glossSlider);
    else if (hint == 1) base *= mix(0.50, 1.18, paintSlider);
    else if (hint == 2) base *= pc.paintGlossRouting == 2 ? mix(1.04, 1.46, paintSlider) : 1.28;
    else if (hint == 4 && pc.paintGlossRouting == 3) base *= mix(0.78, 1.08, paintSlider);
    return clamp(base * (1.0 - roughness * mix(0.72, 0.38, glossSlider)), 0.0, 1.45);
}

float paintGlossTargetWeight(int hint) {
    float paintSlider = clamp(pc.paintGlossSliderValue, 0.0, 1.0);
    if (hint == 1 && pc.paintGlossRouting == 1) return paintSlider;
    if (hint == 2 && pc.paintGlossRouting == 2) return paintSlider * 0.55;
    if (hint == 4 && pc.paintGlossRouting == 3) return paintSlider * 0.24;
    return 0.0;
}

float clearcoatMaterialWeight(int hint) {
    float coat = clamp(pc.clearcoatIntensity, 0.0, 2.0);
    if (hint == 0 || hint == 3 || hint == 6) return 0.0;
    if (hint == 1 || pc.materialPresetHint == 1) return coat;
    if (hint == 2) return coat * 0.28;
    if (hint == 4) return coat * 0.18;
    return 0.0;
}

float contactGroundingMask(vec3 localPos, vec3 normalDir) {
    float bottom = smoothstep(-0.92, -0.58, localPos.y);
    bottom = 1.0 - bottom;
    float radial = 1.0 - smoothstep(0.15, 1.35, length(localPos.xz));
    float upward = 1.0 - smoothstep(0.18, 0.78, normalDir.y);
    return clamp(bottom * mix(0.45, 1.0, radial) * mix(0.55, 1.0, upward), 0.0, 1.0);
}

void main() {
    vec4 texel = pc.baseColorTextureReady != 0 ? texture(baseColorTexture, inTexcoord0) : vec4(1.0);
    vec4 mr = pc.metallicRoughnessTextureReady != 0 ? texture(metallicRoughnessTexture, inTexcoord0) : vec4(1.0);
    float calibration = clamp(pc.calibrationStrength, 0.0, 1.0);
    int hint = pc.materialTypeHint;
    float roughnessRaw = clamp(pc.roughnessFactor * mr.g, 0.04, 1.0);
    float metallic = clamp(pc.metallicFactor * mr.b, 0.0, 1.0);
    bool materialRoleIsGlass = hint == 6 || pc.materialPresetHint == 6;
    bool glassActive = materialRoleIsGlass && pc.glassEnabled != 0;
    vec4 glassParams = glassPresetParams(pc.glassTintPreset);
    float glassClarityInput = clamp(pc.glassClarity, 0.0, 1.0);
    float glassThicknessInput = clamp(pc.glassThickness, 0.0, 1.0);
    float glassOpacityInput = clamp(pc.glassOpacity, 0.0, 1.0);
    float glassEdgeInput = clamp(pc.glassEdge, 0.0, 2.0);
    float glassRoughInput = clamp(pc.glassRoughness, 0.0, 1.0);
    float glassReflectionInput = glassParams.w;
    float roughness = remapRoughness(roughnessRaw, metallic, hint, calibration);
    if (glassActive) roughness = clamp(mix(roughness, max(glassRoughInput, 0.04), 0.84), 0.04, 1.0);
    float glossSlider = clamp(pc.glossSliderValue, 0.0, 1.0);
    float paintSlider = clamp(pc.paintGlossSliderValue, 0.0, 1.0);
    float paintTarget = paintGlossTargetWeight(hint);
    float clearcoatWeight = clearcoatMaterialWeight(hint);
    float clearcoatRough = clamp(pc.clearcoatRoughness, 0.04, 1.0);
    roughness = mix(roughness, clamp(0.76 - paintTarget * 0.64, 0.16, 0.76), 0.62 * paintTarget);
    roughness = mix(roughness, clamp(0.42 - clearcoatWeight * 0.12, 0.18, 0.58), 0.20 * clamp(clearcoatWeight, 0.0, 1.0));
    roughness = clamp(mix(roughness, roughness * mix(1.22, 0.62, glossSlider), 0.64), 0.05, 1.0);
    float aoRaw = pc.occlusionTextureReady != 0 ? texture(occlusionTexture, inTexcoord0).r : 1.0;
    float calibrationVisual = clamp(0.28 + calibration * 0.72, 0.28, 1.0);
    float aoStrength = clamp(mix(pc.occlusionStrength * 0.72, pc.occlusionStrength * 1.35, calibrationVisual), 0.0, 1.0);
    float ao = pc.occlusionTextureReady != 0 ? mix(1.0, aoRaw, aoStrength) : 1.0;
    float alpha = clamp(pc.baseColorFactor.a * texel.a, 0.0, 1.0);
    float cutoff = clamp(pc.alphaCutoff, 0.0, 1.0);
    bool transparentGlassRequested = materialRoleIsGlass && pc.glassEnabled != 0 && pc.glassRenderMode == 1;
    if (!transparentGlassRequested && pc.alphaMode == 1 && alpha < cutoff) {
        discard;
    }
    vec3 rawBaseColor = inColor * pc.baseColorFactor.rgb * texel.rgb;
    vec3 baseColor = normalizeAlbedoEnergy(rawBaseColor, calibration);
    vec3 glassTint = glassTintColor(pc.glassTintPreset);
    if (glassActive && !transparentGlassRequested) {
        metallic = 0.0;
        vec3 cleanFallback = mix(vec3(0.92, 0.98, 1.0), glassTint, pc.glassTintPreset == 6 ? 0.22 : 0.10);
        float weakMaterial = pc.baseColorTextureReady == 0 ? 1.0 : 0.0;
        float dirtyGrey = 1.0 - clamp(abs(baseColor.r - baseColor.g) + abs(baseColor.g - baseColor.b), 0.0, 1.0);
        baseColor = mix(baseColor, cleanFallback, clamp(0.78 * weakMaterial + 0.42 * dirtyGrey * glassClarityInput, 0.0, 0.94));
        baseColor = mix(baseColor, glassTint, pc.glassTintPreset == 0 || pc.glassTintPreset == 1 ? 0.06 : (pc.glassTintPreset == 6 ? 0.18 : 0.14));
    } else if (glassActive) {
        metallic = 0.0;
    }
    if (pc.activeDebugView == 1) {
        fragColor = vec4(rawBaseColor, alpha);
        return;
    }
    vec3 n = normalize(inNormal);
    if (!gl_FrontFacing) n = -n;
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
        fragColor = vec4(n * 0.5 + 0.5, alpha);
        return;
    }
    if (pc.activeDebugView == 3) {
        fragColor = vec4(vec3(roughness), alpha);
        return;
    }
    if (pc.activeDebugView == 4) {
        fragColor = vec4(vec3(metallic), alpha);
        return;
    }
    if (pc.activeDebugView == 5) {
        fragColor = vec4(vec3(ao), alpha);
        return;
    }
    vec3 specColor = mix(vec3(0.04), baseColor, metallic);
    vec3 f0 = specColor;
    vec3 fresnel = fresnelSchlick(max(dot(h, v), 0.0), f0);
    vec3 diffuseTerm = baseColor * (1.0 - metallic) * (vec3(1.0) - fresnel);
    vec3 diffuseLight = diffuseTerm * ndotl * sunColor * pc.sunIntensity;
    float specDistribution = distributionGGX(n, h, roughness);
    float specGeometry = geometrySmith(n, v, l, roughness);
    float specDenom = max(4.0 * max(dot(n, v), 0.0) * ndotl, 0.001);
    float glossWeight = materialGlossWeight(hint, metallic, roughness);
    float specEnergy = mix(1.12, 0.34, roughness) * mix(0.72, 1.22, metallic);
    float rim = pow(clamp(1.0 - max(dot(n, v), 0.0), 0.0, 1.0), mix(4.0, 1.6, roughness));
    vec3 reflectionDir = reflect(-v, n);
    vec3 iblDiffuseColor = environmentColor(n, 1.0) * max(pc.ambientIntensity, pc.ambientFloor);
    vec3 iblSpecularColor = environmentColor(reflectionDir, roughness);
    float reflectionRoughnessEnergy = mix(1.08, 0.18, roughness);
    float reflectionMaterialWeight = mix(0.16, 1.12, metallic) * max(glossWeight, 0.16);
    float motionReflection = clamp(pc.motionReflectionScale, 0.38, 1.0);
    float motionClearcoat = clamp(pc.motionClearcoatScale, 0.35, 1.0);
    vec3 metalTint = mix(vec3(1.0), baseColor, metallic * 0.58);
    if (hint == 0) metalTint *= 0.22;
    vec3 iblSpecular = fresnel * iblSpecularColor * metalTint * reflectionRoughnessEnergy * reflectionMaterialWeight * pc.reflectionIntensity * motionReflection;
    vec3 analyticSpecular = fresnel * iblSpecularColor * metalTint * rim * mix(0.035, 0.24, metallic) * glossWeight * pc.reflectionIntensity * motionReflection;
    vec3 directSpecular = fresnel * (specDistribution * specGeometry / specDenom) * specEnergy * ndotl * sunColor * pc.sunIntensity;
    directSpecular *= mix(0.34, 1.0, glossWeight);
    float clearcoatFresnel = 0.04 + 0.96 * pow(clamp(1.0 - max(dot(n, v), 0.0), 0.0, 1.0), 5.0);
    float coatDistribution = distributionGGX(n, h, clearcoatRough);
    float coatHighlight = coatDistribution * geometrySmith(n, v, l, clearcoatRough) / specDenom;
    vec3 clearcoatLight = sunColor * pc.sunIntensity * ndotl * coatHighlight * clearcoatFresnel;
    clearcoatLight += environmentColor(reflectionDir, clearcoatRough) * clearcoatFresnel * rim * pc.reflectionIntensity * motionReflection * 0.56;
    clearcoatLight *= clearcoatWeight * mix(0.86, 1.42, paintSlider) * motionClearcoat;
    vec3 specularLight = (directSpecular + analyticSpecular + iblSpecular) * pc.specularBoost;
    specularLight *= mix(0.55, 1.35, glossSlider);
    if (hint == 0) specularLight *= 0.55;
    float ndotv = max(dot(n, v), 0.0);
    float glassFresnelBase = pow(clamp(1.0 - ndotv, 0.0, 1.0), 3.2);
    float glassFresnel = clamp(glassFresnelBase * mix(0.92, 1.55, clamp(glassEdgeInput * 0.5, 0.0, 1.0)) + 0.06, 0.0, 1.0);
    float glassThickness = clamp((glassFresnel * 0.92 + (1.0 - ndotv) * 0.18) * mix(0.12, 1.22, glassThicknessInput), 0.0, 1.0);
    vec3 glassZone = environmentColor(reflectionDir, clamp(roughness * 0.72, 0.02, 1.0));
    vec3 glassReflection = glassZone * (0.16 + glassFresnel * clamp(glassEdgeInput, 0.0, 2.0)) * mix(1.14, 0.34, roughness) * pc.reflectionIntensity * glassReflectionInput * motionReflection;
    float reflectionLum = luminance(glassReflection);
    if (reflectionLum > 1.65) glassReflection *= 1.65 / max(reflectionLum, 0.001);
    float glassOpacity = clamp(glassOpacityInput, 0.0, 1.0);
    float glassCenterAlpha = clamp(glassOpacity, 0.0, 1.0);
    float edgeAlpha = clamp(glassFresnel * mix(0.20, 0.50, clamp(glassEdgeInput * 0.5, 0.0, 1.0)), 0.0, 0.52);
    float transparentEdgeAlpha = clamp(glassFresnelBase * mix(0.18, 0.48, clamp(glassEdgeInput * 0.5, 0.0, 1.0)), 0.0, 0.50);
    float glassSurface = clamp(glassCenterAlpha + edgeAlpha * 0.58, 0.0, 0.88);
    float transparentGlassAlpha = transparentGlassRequested ? clamp(max(glassCenterAlpha, transparentEdgeAlpha), 0.0, 0.96) : clamp(glassCenterAlpha, 0.0, 0.96);
    vec3 transparentGlassRgb = vec3(0.0);
    if (glassActive) {
        vec3 hazeColor = mix(glassTint, vec3(luminance(glassTint)), 0.65);
        vec3 centerTint = mix(glassTint, hazeColor, clamp(1.0 - glassClarityInput, 0.0, 1.0));
        vec3 thicknessTint = mix(glassTint * mix(0.82, 0.45, glassThicknessInput), glassTint * 1.12, glassFresnel);
        float centerEnergy = transparentGlassRequested ? mix(0.0, 0.10, 1.0 - glassClarityInput) * glassOpacity : glassSurface;
        float centerTintEnergy = transparentGlassRequested ? centerEnergy : glassThickness * 0.22;
        diffuseLight *= transparentGlassRequested ? centerEnergy : glassCenterAlpha * mix(0.08, 0.22, 1.0 - glassClarityInput);
        specularLight = specularLight * (transparentGlassRequested ? 0.04 : 0.24) + glassReflection * mix(1.12, 1.42, glassClarityInput);
        specularLight += thicknessTint * glassFresnel * glassEdgeInput * mix(0.18, 0.46, glassThicknessInput);
        baseColor = mix(vec3(0.0), centerTint, centerTintEnergy);
        baseColor += thicknessTint * glassThickness * glassFresnel * 0.18;
        if (transparentGlassRequested) {
            vec3 edgeTint = mix(vec3(1.0), glassTint, pc.glassTintPreset == 0 || pc.glassTintPreset == 1 ? 0.06 : 0.26);
            vec3 cleanCenter = glassTint * glassOpacity * mix(0.00, 0.14, 1.0 - glassClarityInput);
            vec3 frostHaze = mix(glassTint, vec3(0.72), 1.0 - glassClarityInput) * (1.0 - glassClarityInput) * glassOpacity * 0.10;
            vec3 thicknessEdge = thicknessTint * transparentEdgeAlpha * mix(0.26, 0.72, glassThicknessInput);
            vec3 edgeGlint = edgeTint * transparentEdgeAlpha * clamp(glassEdgeInput, 0.0, 2.0) * mix(0.34, 0.78, glassClarityInput);
            vec3 specGlint = glassReflection * mix(0.52, 1.10, glassClarityInput) + edgeGlint;
            transparentGlassRgb = cleanCenter + frostHaze + thicknessEdge + specGlint;
        }
        if (transparentGlassRequested) alpha = transparentGlassAlpha;
    }
    specularLight *= mix(1.0, 1.36, paintTarget);
    specularLight += clearcoatLight;
    float specLum = luminance(specularLight);
    float specGuard = mix(1.45, 2.10, metallic) * mix(1.0, 0.72, calibration) + clearcoatWeight * 0.55;
    if (specLum > specGuard) specularLight *= mix(1.0, specGuard / max(specLum, 0.001), 0.55);
    vec3 emissiveColor = clamp(pc.emissiveFactor, vec3(0.0), vec3(1.0)) * clamp(pc.emissiveIntensity, 0.0, 2.0);
    float emissiveLum = luminance(emissiveColor);
    if (emissiveLum > 1.35) emissiveColor *= 1.35 / max(emissiveLum, 0.001);
    if (pc.activeDebugView == 6) {
        fragColor = vec4(toneMap(diffuseLight * pc.exposureValue), alpha);
        return;
    }
    if (pc.activeDebugView == 7) {
        fragColor = vec4(toneMap(specularLight * pc.exposureValue * 3.0), alpha);
        return;
    }
    if (pc.activeDebugView == 8) {
        fragColor = vec4(f0, alpha);
        return;
    }
    if (pc.activeDebugView == 9) {
        fragColor = vec4(toneMap(iblSpecular * pc.exposureValue * 2.0), alpha);
        return;
    }
    if (pc.activeDebugView == 10) {
        fragColor = vec4(toneMap(iblDiffuseColor * baseColor * (1.0 - metallic) * pc.exposureValue), alpha);
        return;
    }
    if (pc.activeDebugView == 11) {
        fragColor = vec4(toneMap(iblSpecular * pc.exposureValue * 3.0), alpha);
        return;
    }
    if (pc.activeDebugView == 12) {
        fragColor = vec4(pc.baseColorTextureReady != 0 ? 0.1 : 0.85, pc.metallicRoughnessTextureReady != 0 ? 0.85 : 0.1, pc.normalTextureReady != 0 ? 0.85 : 0.1, 1.0);
        return;
    }
    vec3 ambient = baseColor * (1.0 - metallic * 0.75) * mix(ambientColor * max(pc.ambientIntensity, pc.ambientFloor), iblDiffuseColor, 0.65);
    if (glassActive) ambient *= transparentGlassRequested ? mix(0.0, 0.06, 1.0 - glassClarityInput) * glassOpacity : mix(0.18, 0.56, glassOpacity);
    vec3 rgb = (diffuseLight + ambient) * ao + specularLight + emissiveColor;
    if (transparentGlassRequested) {
        rgb = transparentGlassRgb;
    }
    float contactMask = contactGroundingMask(inLocalPosition, n);
    if (pc.activeDebugView == 13) {
        fragColor = vec4(vec3(contactMask * clamp(pc.contactShadowIntensity / 1.5, 0.0, 1.0)), alpha);
        return;
    }
    if (pc.activeDebugView == 14) {
        fragColor = vec4(baseColor, alpha);
        return;
    }
    if (pc.activeDebugView == 15) {
        fragColor = vec4(materialTypeColor(hint), alpha);
        return;
    }
    if (pc.activeDebugView == 16) {
        fragColor = vec4(vec3(1.0 - ao), alpha);
        return;
    }
    if (pc.activeDebugView == 17) {
        float rawLum = luminance(rawBaseColor);
        float clampedLum = luminance(baseColor);
        fragColor = vec4(clamp(vec3(clampedLum, rawLum - clampedLum, rawLum), 0.0, 1.0), alpha);
        return;
    }
    if (pc.activeDebugView == 18) {
        fragColor = vec4(vec3(glossWeight), alpha);
        return;
    }
    if (pc.activeDebugView == 19) {
        fragColor = vec4(vec3(clamp(specLum / max(specGuard, 0.001), 0.0, 1.0)), alpha);
        return;
    }
    if (pc.activeDebugView == 20) {
        fragColor = vec4(vec3(paintTarget), alpha);
        return;
    }
    if (pc.activeDebugView == 21) {
        fragColor = vec4(vec3(metallic, glossWeight, roughness), alpha);
        return;
    }
    if (pc.activeDebugView == 22) {
        fragColor = vec4(materialTypeColor(hint) * mix(0.35, 1.0, paintTarget), alpha);
        return;
    }
    if (pc.activeDebugView == 23) {
        fragColor = vec4(vec3(calibrationVisual, 1.0 - roughness, 1.0 - ao), alpha);
        return;
    }
    if (pc.activeDebugView == 24) {
        fragColor = vec4(toneMap(environmentColor(n, 0.45) * pc.exposureValue), alpha);
        return;
    }
    if (pc.activeDebugView == 25) {
        fragColor = vec4(reflectionDir * 0.5 + 0.5, alpha);
        return;
    }
    if (pc.activeDebugView == 26) {
        fragColor = vec4(toneMap(iblSpecularColor * pc.exposureValue), alpha);
        return;
    }
    if (pc.activeDebugView == 27) {
        fragColor = vec4(materialTypeColor(hint), alpha);
        return;
    }
    if (pc.activeDebugView == 28) {
        fragColor = vec4(vec3(clamp(pc.glossSliderValue, 0.0, 1.0), clamp(pc.paintGlossSliderValue, 0.0, 1.0), 1.0 - roughness), alpha);
        return;
    }
    if (pc.activeDebugView == 29) {
        fragColor = vec4(vec3(metallic), alpha);
        return;
    }
    if (pc.activeDebugView == 30) {
        fragColor = vec4(vec3(roughness), alpha);
        return;
    }
    if (pc.activeDebugView == 31) {
        fragColor = vec4(vec3(ao), alpha);
        return;
    }
    if (pc.activeDebugView == 32) {
        fragColor = vec4(vec3(alpha >= cutoff ? 1.0 : 0.0), 1.0);
        return;
    }
    if (pc.activeDebugView == 33) {
        fragColor = vec4(pc.alphaMode == 1 ? vec3(0.1, 0.9, 0.25) : (pc.alphaMode == 2 ? vec3(1.0, 0.70, 0.12) : vec3(0.45, 0.55, 0.65)), 1.0);
        return;
    }
    if (pc.activeDebugView == 34) {
        fragColor = vec4(gl_FrontFacing ? vec3(0.2, 0.75, 1.0) : vec3(1.0, 0.35, 0.2), 1.0);
        return;
    }
    if (pc.activeDebugView == 35) {
        fragColor = vec4((hint == 5 || pc.alphaMode == 1 || pc.alphaMode == 2) ? vec3(0.3, 0.95, 0.55) : materialTypeColor(hint) * 0.55, 1.0);
        return;
    }
    if (pc.activeDebugView == 36) {
        fragColor = vec4(pc.alphaMode == 2 ? vec3(1.0, 0.45, 0.12) : (pc.alphaMode == 1 ? vec3(0.2, 0.9, 0.35) : vec3(0.25, 0.45, 0.9)), 1.0);
        return;
    }
    if (pc.activeDebugView == 37) {
        fragColor = vec4(toneMap(emissiveColor * max(pc.exposureValue, 1.0)), 1.0);
        return;
    }
    if (pc.activeDebugView == 38) {
        fragColor = vec4(materialPresetColor(pc.materialPresetHint), 1.0);
        return;
    }
    if (pc.activeDebugView == 39) {
        fragColor = vec4(vec3(clamp(1.0 - roughness, 0.0, 1.0), metallic, clamp(pc.emissiveIntensity / 2.0, 0.0, 1.0)), 1.0);
        return;
    }
    if (pc.activeDebugView == 40) {
        fragColor = vec4(vec3(clamp(luminance(rgb) / 2.1, 0.0, 1.0)), 1.0);
        return;
    }
    if (pc.activeDebugView == 41) {
        fragColor = vec4(materialPresetColor(pc.materialPresetHint) * mix(0.45, 1.0, clamp(pc.emissiveIntensity / 2.0, 0.0, 1.0)), 1.0);
        return;
    }
    if (pc.activeDebugView == 42) {
        fragColor = vec4(vec3(clamp(clearcoatWeight * 0.5, 0.0, 1.0)), 1.0);
        return;
    }
    if (pc.activeDebugView == 43) {
        fragColor = vec4(toneMap(clearcoatLight * pc.exposureValue * 4.0), 1.0);
        return;
    }
    if (pc.activeDebugView == 44) {
        fragColor = vec4(vec3(clamp(paintTarget + clearcoatWeight * 0.5, 0.0, 1.0), clamp(1.0 - roughness, 0.0, 1.0), clearcoatFresnel), 1.0);
        return;
    }
    if (pc.activeDebugView == 45) {
        fragColor = vec4(vec3(clamp(luminance(specularLight) / max(specGuard, 0.001), 0.0, 1.0)), 1.0);
        return;
    }
    if (pc.activeDebugView == 46) {
        fragColor = vec4(toneMap(environmentColor(reflectionDir, roughness) * pc.exposureValue), 1.0);
        return;
    }
    if (pc.activeDebugView == 47) {
        fragColor = vec4(vec3(clamp(reflectionDir.y * 0.5 + 0.5, 0.0, 1.0), clamp(1.0 - abs(reflectionDir.y), 0.0, 1.0), clamp(abs(reflectionDir.x), 0.0, 1.0)), 1.0);
        return;
    }
    if (pc.activeDebugView == 48) {
        fragColor = vec4(toneMap(iblSpecularColor * pc.reflectionContrast), 1.0);
        return;
    }
    if (pc.activeDebugView == 49) {
        fragColor = vec4(vec3(clamp(luminance(specularLight) / max(specGuard, 0.001), 0.0, 1.0)), 1.0);
        return;
    }
    if (pc.activeDebugView == 50) {
        fragColor = vec4(toneMap(clearcoatLight * pc.exposureValue * 4.0), 1.0);
        return;
    }
    if (pc.activeDebugView == 51) {
        fragColor = vec4(vec3(motionReflection, motionClearcoat, clamp(pc.motionReflectionScale, 0.0, 1.0)), 1.0);
        return;
    }
    if (pc.activeDebugView == 52) {
        fragColor = vec4(glassActive ? vec3(0.36, 0.82, 1.0) : vec3(0.04), 1.0);
        return;
    }
    if (pc.activeDebugView == 53) {
        fragColor = vec4(vec3(glassOpacity), 1.0);
        return;
    }
    if (pc.activeDebugView == 54) {
        fragColor = vec4(vec3(glassFresnel), 1.0);
        return;
    }
    if (pc.activeDebugView == 55) {
        fragColor = vec4(toneMap(glassReflection * pc.exposureValue), 1.0);
        return;
    }
    if (pc.activeDebugView == 56) {
        fragColor = vec4(glassTint, 1.0);
        return;
    }
    if (pc.activeDebugView == 57) {
        fragColor = vec4(glassActive ? vec3(0.1, 0.9, 0.55) : vec3(0.35, 0.42, 0.48), 1.0);
        return;
    }
    if (pc.activeDebugView == 58) {
        fragColor = vec4(vec3(glassFresnel, glassThickness, glassReflectionInput * 0.5), 1.0);
        return;
    }
    if (pc.activeDebugView == 59) {
        fragColor = vec4(vec3(glassFresnel), 1.0);
        return;
    }
    if (pc.activeDebugView == 60) {
        fragColor = vec4(vec3(glassThickness), 1.0);
        return;
    }
    if (pc.activeDebugView == 61) {
        fragColor = vec4(toneMap(glassReflection * pc.exposureValue), 1.0);
        return;
    }
    if (pc.activeDebugView == 62) {
        fragColor = vec4(toneMap(clearcoatLight * pc.exposureValue * 4.0), 1.0);
        return;
    }
    if (pc.activeDebugView == 63) {
        fragColor = vec4(toneMap((iblSpecular + clearcoatLight) * pc.exposureValue * 2.0), 1.0);
        return;
    }
    if (pc.activeDebugView == 64) {
        fragColor = vec4(glassActive && pc.glassRenderMode == 1 ? vec3(0.10, 0.85, 1.0) : vec3(0.04), 1.0);
        return;
    }
    if (pc.activeDebugView == 65) {
        fragColor = vec4(vec3(glassActive ? transparentGlassAlpha : 1.0), 1.0);
        return;
    }
    if (pc.activeDebugView == 66) {
        fragColor = vec4(pc.glassRenderMode == 1 ? vec3(0.2, 0.85, 0.4) : vec3(0.75, 0.55, 0.20), 1.0);
        return;
    }
    if (pc.activeDebugView == 67) {
        fragColor = vec4(pc.glassRenderMode == 1 ? vec3(0.08, 0.30, 0.58) : vec3(0.75, 0.42, 0.18), 1.0);
        return;
    }
    if (pc.activeDebugView == 68) {
        fragColor = vec4(vec3(glassActive ? 0.18 : 0.55, pc.glassRenderMode == 1 ? 0.9 : 0.35, 0.55), 1.0);
        return;
    }
    if (pc.activeDebugView == 69) {
        fragColor = vec4(baseColor, 1.0);
        return;
    }
    if (pc.activeDebugView == 70) {
        fragColor = vec4(glassActive ? vec3(0.30, 0.90, 1.00) : vec3(0.16, 0.18, 0.20), 1.0);
        return;
    }
    if (pc.activeDebugView == 71) {
        fragColor = vec4(pc.glassRenderMode == 1 && glassActive ? vec3(0.90, 1.00, 0.45) : vec3(0.20, 0.22, 0.24), 1.0);
        return;
    }
    if (pc.activeDebugView == 72) {
        fragColor = vec4(pc.glassRenderMode == 1 ? vec3(0.20, 0.70, 1.00) : vec3(0.80, 0.64, 0.26), 1.0);
        return;
    }
    if (pc.activeDebugView == 73) {
        fragColor = vec4(glassActive || pc.materialTypeHint != 6 ? vec3(0.12, 0.82, 0.42) : vec3(0.95, 0.24, 0.18), 1.0);
        return;
    }
    if (pc.activeDebugView == 74) {
        fragColor = vec4(vec3(glassCenterAlpha), 1.0);
        return;
    }
    if (pc.activeDebugView == 75) {
        fragColor = vec4(toneMap(glassReflection * pc.exposureValue), 1.0);
        return;
    }
    if (pc.activeDebugView == 76) {
        fragColor = vec4(vec3(glassThickness), 1.0);
        return;
    }
    if (pc.activeDebugView == 77) {
        fragColor = vec4(vec3(glassClarityInput), 1.0);
        return;
    }
    if (pc.activeDebugView == 78) {
        fragColor = vec4(glassActive && pc.baseColorTextureReady == 0 ? vec3(0.20, 0.90, 1.0) : vec3(0.12, 0.18, 0.22), 1.0);
        return;
    }
    if (pc.activeDebugView == 79) {
        fragColor = vec4(toneMap((baseColor * glassSurface + glassReflection) * pc.exposureValue), 1.0);
        return;
    }
    rgb *= 1.0 - contactMask * clamp(pc.contactShadowIntensity, 0.0, 1.5) * 0.22;
    float diffuseLum = luminance(diffuseLight + ambient);
    float diffuseLimit = mix(2.4, 1.55, calibration);
    if (diffuseLum > diffuseLimit) rgb *= mix(1.0, diffuseLimit / max(diffuseLum, 0.001), 0.65 * calibration);
    float litLum = luminance(rgb);
    float guardLimit = mix(3.2, 2.1, calibration);
    if (litLum > guardLimit && emissiveLum <= 0.001) rgb *= guardLimit / max(litLum, 0.001);
    rgb *= pc.exposureValue;
    rgb = toneMap(rgb);
    fragColor = vec4(rgb, alpha);
}
