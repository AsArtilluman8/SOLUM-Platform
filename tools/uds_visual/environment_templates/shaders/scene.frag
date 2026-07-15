#version 300 es
precision highp float;
in vec3 vWorld;
in vec3 vNormal;
out vec4 outColor;
uniform vec3 uCamera;
uniform vec3 uBaseColor;
uniform vec3 uSunDirection;
uniform vec3 uMoonDirection;
uniform vec3 uSunColor;
uniform vec3 uMoonColor;
uniform float uSunLight;
uniform float uMoonLight;
uniform float uAmbient;
uniform float uLightningFlash;
uniform vec3 uLightningColor;
uniform float uRoughness;
uniform float uMetalness;
uniform float uAlpha;
uniform float uWetness;
uniform float uSnow;
uniform float uDust;
uniform float uFogDensity;
uniform float uFogHeightFalloff;
uniform float uDay;
uniform int uMaterialType;

void main() {
  vec3 normal = normalize(vNormal);
  vec3 view = normalize(uCamera - vWorld);
  vec3 sun = normalize(uSunDirection);
  vec3 moon = normalize(uMoonDirection);
  float sunNdl = max(dot(normal, sun), 0.0);
  float moonNdl = max(dot(normal, moon), 0.0);
  float wet = clamp(uWetness, 0.0, 1.0);
  float roughness = mix(uRoughness, 0.055, wet * (uMaterialType == 0 ? 0.82 : 0.28));
  vec3 base = uBaseColor;
  if (uMaterialType == 0) {
    base = mix(base, base * vec3(0.42, 0.48, 0.52), wet * 0.72);
    base = mix(base, vec3(0.72, 0.76, 0.78), clamp(uSnow * 0.88, 0.0, 0.92));
    base = mix(base, vec3(0.52, 0.35, 0.18), clamp(uDust * 0.72, 0.0, 0.85));
  }
  vec3 halfSun = normalize(sun + view);
  vec3 halfMoon = normalize(moon + view);
  float power = mix(180.0, 4.0, roughness);
  float sunSpec = pow(max(dot(normal, halfSun), 0.0), power) * sunNdl;
  float moonSpec = pow(max(dot(normal, halfMoon), 0.0), power) * moonNdl;
  vec3 f0 = mix(vec3(0.035), base, clamp(uMetalness, 0.0, 1.0));
  vec3 diffuse = base * (1.0 - uMetalness) * (uAmbient + sunNdl * uSunLight * uSunColor + moonNdl * uMoonLight * uMoonColor);
  vec3 color = diffuse + f0 * (sunSpec * uSunLight * uSunColor + moonSpec * uMoonLight * uMoonColor);
  color += uLightningColor * uLightningFlash * (0.16 + max(dot(normal, vec3(0,1,0)), 0.0));
  if (uMaterialType == 1) {
    float fresnel = pow(1.0 - max(dot(normal, view), 0.0), 4.0);
    vec3 reflectedSky = mix(vec3(0.04, 0.09, 0.13), vec3(0.22, 0.48, 0.68), uDay);
    color = mix(color, reflectedSky, 0.48 + fresnel * 0.42);
  } else if (uMaterialType == 2) {
    float fresnel = 0.08 + 0.82 * pow(1.0 - max(dot(normal, view), 0.0), 4.0);
    color = mix(vec3(0.05, 0.13, 0.16), color + vec3(0.15, 0.28, 0.34), fresnel);
  }
  float distanceToCamera = length(uCamera - vWorld);
  float heightFactor = exp(-max(vWorld.y, 0.0) * max(0.001, uFogHeightFalloff) * 3.0);
  float fog = 1.0 - exp(-uFogDensity * distanceToCamera * (0.55 + heightFactor));
  vec3 fogColor = mix(vec3(0.10, 0.14, 0.22), vec3(0.55, 0.64, 0.68), uDay);
  color = mix(color, fogColor, clamp(fog, 0.0, 0.94));
  color = color / (vec3(1.0) + color);
  outColor = vec4(pow(max(color, 0.0), vec3(1.0 / 2.2)), uAlpha);
}
