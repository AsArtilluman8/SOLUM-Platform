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
    layout(offset = 248) int environmentPadding;
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
    else target = clamp(roughness, 0.22, 0.86);
    float presetBias = pc.calibrationPreset == 1 ? 0.18 : (pc.calibrationPreset == 3 ? -0.16 : 0.0);
    return clamp(mix(roughness, target + presetBias * (1.0 - metallic), visualStrength), 0.06, 1.0);
}

vec3 materialTypeColor(int hint) {
    if (hint == 0) return vec3(0.66, 0.55, 0.92);
    if (hint == 1) return vec3(0.90, 0.42, 0.32);
    if (hint == 2) return vec3(0.70, 0.78, 0.86);
    if (hint == 3) return vec3(0.08, 0.10, 0.11);
    return vec3(0.45, 0.48, 0.50);
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

void environmentPalette(out vec3 groundColor, out vec3 horizonColor, out vec3 skyColor, out vec3 accentColor) {
    if (pc.environmentPreset == 1) {
        groundColor = vec3(0.34, 0.29, 0.24);
        horizonColor = vec3(0.74, 0.58, 0.42);
        skyColor = vec3(0.98, 0.86, 0.68);
        accentColor = vec3(0.58, 0.34, 0.20);
    } else if (pc.environmentPreset == 2) {
        groundColor = vec3(0.20, 0.25, 0.30);
        horizonColor = vec3(0.48, 0.62, 0.76);
        skyColor = vec3(0.72, 0.86, 1.00);
        accentColor = vec3(0.25, 0.48, 0.70);
    } else if (pc.environmentPreset == 3) {
        groundColor = vec3(0.25, 0.32, 0.22);
        horizonColor = vec3(0.66, 0.76, 0.82);
        skyColor = vec3(0.55, 0.76, 1.00);
        accentColor = vec3(0.78, 0.72, 0.48);
    } else if (pc.environmentPreset == 4) {
        groundColor = vec3(0.28, 0.20, 0.24);
        horizonColor = vec3(0.88, 0.45, 0.30);
        skyColor = vec3(0.34, 0.42, 0.70);
        accentColor = vec3(1.00, 0.58, 0.25);
    } else {
        groundColor = vec3(0.30, 0.28, 0.26);
        horizonColor = vec3(0.58, 0.64, 0.70);
        skyColor = vec3(0.82, 0.88, 0.96);
        accentColor = vec3(0.46, 0.50, 0.56);
    }
}

vec3 environmentColor(vec3 dir, float roughness) {
    float sky = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 groundColor;
    vec3 horizonColor;
    vec3 skyColor;
    vec3 accentColor;
    environmentPalette(groundColor, horizonColor, skyColor, accentColor);
    vec3 sharpColor = mix(mix(groundColor, horizonColor, smoothstep(0.0, 0.55, sky)), skyColor, smoothstep(0.45, 1.0, sky));
    float horizonLine = 1.0 - smoothstep(0.035, 0.22, abs(dir.y - 0.08));
    sharpColor += accentColor * horizonLine * (1.0 - roughness) * clamp(pc.horizonStrength, 0.0, 1.0);
    vec3 blurredColor = mix(mix(groundColor, horizonColor, 0.45), mix(horizonColor, skyColor, 0.55), sky);
    return mix(sharpColor, blurredColor, clamp(roughness, 0.0, 1.0)) * clamp(pc.environmentIntensity, 0.0, 2.0);
}

float materialGlossWeight(int hint, float metallic, float roughness) {
    float glossSlider = clamp(pc.glossSliderValue, 0.0, 1.0);
    float paintSlider = clamp(pc.paintGlossSliderValue, 0.0, 1.0);
    float base = mix(0.18, 1.18, glossSlider) * mix(0.30, 1.0, metallic);
    if (hint == 0) base *= mix(0.04, 0.14, glossSlider);
    else if (hint == 3) base *= mix(0.08, 0.28, glossSlider);
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
    float roughness = remapRoughness(roughnessRaw, metallic, hint, calibration);
    float glossSlider = clamp(pc.glossSliderValue, 0.0, 1.0);
    float paintSlider = clamp(pc.paintGlossSliderValue, 0.0, 1.0);
    float paintTarget = paintGlossTargetWeight(hint);
    roughness = mix(roughness, clamp(0.76 - paintTarget * 0.64, 0.16, 0.76), 0.62 * paintTarget);
    roughness = clamp(mix(roughness, roughness * mix(1.22, 0.62, glossSlider), 0.64), 0.05, 1.0);
    float aoRaw = pc.occlusionTextureReady != 0 ? texture(occlusionTexture, inTexcoord0).r : 1.0;
    float calibrationVisual = clamp(0.28 + calibration * 0.72, 0.28, 1.0);
    float aoStrength = clamp(mix(pc.occlusionStrength * 0.72, pc.occlusionStrength * 1.35, calibrationVisual), 0.0, 1.0);
    float ao = pc.occlusionTextureReady != 0 ? mix(1.0, aoRaw, aoStrength) : 1.0;
    vec3 rawBaseColor = inColor * pc.baseColorFactor.rgb * texel.rgb;
    vec3 baseColor = normalizeAlbedoEnergy(rawBaseColor, calibration);
    if (pc.activeDebugView == 1) {
        fragColor = vec4(rawBaseColor, pc.baseColorFactor.a * texel.a);
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
    vec3 iblSpecular = fresnel * iblSpecularColor * reflectionRoughnessEnergy * reflectionMaterialWeight * pc.reflectionIntensity;
    vec3 analyticSpecular = fresnel * iblSpecularColor * rim * mix(0.035, 0.22, metallic) * glossWeight * pc.reflectionIntensity;
    vec3 directSpecular = fresnel * (specDistribution * specGeometry / specDenom) * specEnergy * ndotl * sunColor * pc.sunIntensity;
    directSpecular *= mix(0.34, 1.0, glossWeight);
    vec3 specularLight = (directSpecular + analyticSpecular + iblSpecular) * pc.specularBoost;
    specularLight *= mix(0.55, 1.35, glossSlider);
    if (hint == 0) specularLight *= 0.55;
    specularLight *= mix(1.0, 1.36, paintTarget);
    float specLum = luminance(specularLight);
    float specGuard = mix(1.45, 2.10, metallic) * mix(1.0, 0.72, calibration);
    if (specLum > specGuard) specularLight *= mix(1.0, specGuard / max(specLum, 0.001), 0.55);
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
        fragColor = vec4(toneMap(iblSpecular * pc.exposureValue * 2.0), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 10) {
        fragColor = vec4(toneMap(iblDiffuseColor * baseColor * (1.0 - metallic) * pc.exposureValue), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 11) {
        fragColor = vec4(toneMap(iblSpecular * pc.exposureValue * 3.0), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 12) {
        fragColor = vec4(pc.baseColorTextureReady != 0 ? 0.1 : 0.85, pc.metallicRoughnessTextureReady != 0 ? 0.85 : 0.1, pc.normalTextureReady != 0 ? 0.85 : 0.1, 1.0);
        return;
    }
    vec3 ambient = baseColor * (1.0 - metallic * 0.75) * mix(ambientColor * max(pc.ambientIntensity, pc.ambientFloor), iblDiffuseColor, 0.65);
    vec3 rgb = (diffuseLight + ambient) * ao + specularLight + pc.emissiveFactor;
    float contactMask = contactGroundingMask(inLocalPosition, n);
    if (pc.activeDebugView == 13) {
        fragColor = vec4(vec3(contactMask * clamp(pc.contactShadowIntensity / 1.5, 0.0, 1.0)), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 14) {
        fragColor = vec4(baseColor, pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 15) {
        fragColor = vec4(materialTypeColor(hint), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 16) {
        fragColor = vec4(vec3(1.0 - ao), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 17) {
        float rawLum = luminance(rawBaseColor);
        float clampedLum = luminance(baseColor);
        fragColor = vec4(clamp(vec3(clampedLum, rawLum - clampedLum, rawLum), 0.0, 1.0), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 18) {
        fragColor = vec4(vec3(glossWeight), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 19) {
        fragColor = vec4(vec3(clamp(specLum / max(specGuard, 0.001), 0.0, 1.0)), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 20) {
        fragColor = vec4(vec3(paintTarget), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 21) {
        fragColor = vec4(vec3(metallic, glossWeight, roughness), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 22) {
        fragColor = vec4(materialTypeColor(hint) * mix(0.35, 1.0, paintTarget), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 23) {
        fragColor = vec4(vec3(calibrationVisual, 1.0 - roughness, 1.0 - ao), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 24) {
        fragColor = vec4(toneMap(environmentColor(n, 0.45) * pc.exposureValue), pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 25) {
        fragColor = vec4(reflectionDir * 0.5 + 0.5, pc.baseColorFactor.a * texel.a);
        return;
    }
    if (pc.activeDebugView == 26) {
        fragColor = vec4(toneMap(iblSpecularColor * pc.exposureValue), pc.baseColorFactor.a * texel.a);
        return;
    }
    rgb *= 1.0 - contactMask * clamp(pc.contactShadowIntensity, 0.0, 1.5) * 0.22;
    float diffuseLum = luminance(diffuseLight + ambient);
    float diffuseLimit = mix(2.4, 1.55, calibration);
    if (diffuseLum > diffuseLimit) rgb *= mix(1.0, diffuseLimit / max(diffuseLum, 0.001), 0.65 * calibration);
    float litLum = luminance(rgb);
    float guardLimit = mix(3.2, 2.1, calibration);
    if (litLum > guardLimit && luminance(pc.emissiveFactor) <= 0.001) rgb *= guardLimit / max(litLum, 0.001);
    rgb *= pc.exposureValue;
    rgb = toneMap(rgb);
    fragColor = vec4(rgb, pc.baseColorFactor.a * texel.a);
}
