#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;

uniform vec2 uResolution;
uniform mat3 uCameraBasis;
uniform float uFov;
uniform float uClock;
uniform vec3 uSunDirection;
uniform vec3 uMoonDirection;
uniform vec3 uSunColor;
uniform vec3 uMoonColor;
uniform float uDay;
uniform float uTwilight;
uniform float uNight;
uniform float uStarVisibility;
uniform float uMoonPhase;
uniform float uMoonScale;
uniform float uSunDiskIntensity;
uniform float uStarsIntensity;
uniform float uStarsSpeed;
uniform float uTwinkleAmount;
uniform float uTwinkleSpeed;
uniform float uCloudCoverage;
uniform float uCloudDensity;
uniform float uCloudThickness;
uniform vec3 uCloudProfile;
uniform vec2 uCloudOffset;
uniform int uCloudSteps;
uniform float uFogDensity;
uniform float uHumidity;
uniform float uHaze;
uniform float uAbsorption;
uniform float uDust;
uniform float uLightningFlash;
uniform vec3 uLightningColor;
uniform float uExposure;

const float PI = 3.141592653589793;

float hash12(vec2 p) {
  vec3 p3 = fract(vec3(p.xyx) * 0.1031);
  p3 += dot(p3, p3.yzx + 33.33);
  return fract((p3.x + p3.y) * p3.z);
}

float noise2(vec2 p) {
  vec2 i = floor(p), f = fract(p);
  f = f * f * (3.0 - 2.0 * f);
  return mix(mix(hash12(i), hash12(i + vec2(1,0)), f.x),
             mix(hash12(i + vec2(0,1)), hash12(i + vec2(1,1)), f.x), f.y);
}

float cloudNoise(vec2 p) {
  float sum = 0.0, amplitude = 0.56;
  mat2 rotation = mat2(0.80, -0.60, 0.60, 0.80);
  for (int index = 0; index < 8; index++) {
    if (index >= uCloudSteps) break;
    sum += noise2(p) * amplitude;
    p = rotation * p * 2.03 + vec2(7.1, 3.7);
    amplitude *= 0.51;
  }
  return sum;
}

vec3 atmosphere(vec3 ray) {
  float horizon = clamp(1.0 - max(ray.y, 0.0), 0.0, 1.0);
  float airMass = 1.0 / max(0.08, ray.y + 0.17);
  float mu = clamp(dot(ray, uSunDirection), -1.0, 1.0);
  float rayleighPhase = 0.0597 * (1.0 + mu * mu);
  float g = 0.76;
  float miePhase = 0.119 * (1.0 - g * g) / pow(max(0.08, 1.0 + g * g - 2.0 * g * mu), 1.5);
  vec3 betaR = vec3(0.18, 0.42, 0.92);
  vec3 betaM = vec3(1.0, 0.55, 0.24);
  vec3 dayZenith = vec3(0.12, 0.34, 0.78) * (0.66 + 0.34 * max(ray.y, 0.0));
  vec3 dayHorizon = vec3(0.58, 0.72, 0.86) + betaM * uHumidity * 0.10;
  vec3 nightZenith = vec3(0.004, 0.009, 0.030);
  vec3 nightHorizon = vec3(0.018, 0.026, 0.052);
  vec3 dayColor = mix(dayZenith, dayHorizon, pow(horizon, 1.8));
  vec3 nightColor = mix(nightZenith, nightHorizon, pow(horizon, 1.4));
  vec3 color = mix(nightColor, dayColor, uDay);
  color += betaR * rayleighPhase * uDay * (0.22 + 0.2 * horizon);
  color += betaM * miePhase * uDay * (0.008 + uHaze * 0.012);
  vec3 sunset = vec3(1.0, 0.19, 0.035) * pow(max(mu, 0.0), 7.0) * pow(horizon, 0.35);
  color += sunset * (uTwilight * 1.35 + (1.0 - uDay) * 0.18);
  color *= exp(-vec3(0.10, 0.055, 0.025) * airMass * uAbsorption * 0.35);
  color = mix(color, vec3(0.56, 0.42, 0.25), uDust * (0.16 + 0.32 * horizon));
  return color;
}

float stars(vec3 ray) {
  float rotation = uClock * uStarsSpeed * 0.02;
  ray.xz = mat2(cos(rotation), -sin(rotation), sin(rotation), cos(rotation)) * ray.xz;
  vec2 sphere = vec2(atan(ray.z, ray.x) / (2.0 * PI) + 0.5, asin(clamp(ray.y, -1.0, 1.0)) / PI + 0.5);
  vec2 cells = floor(sphere * vec2(620.0, 310.0));
  float seed = hash12(cells + 1337.0);
  float bright = smoothstep(0.986, 0.9997, seed);
  float wave = 0.5 + 0.5 * sin(uClock * uTwinkleSpeed * (0.65 + seed * 2.1) + seed * 90.0);
  float twinkle = mix(1.0, wave, clamp(uTwinkleAmount, 0.0, 1.0));
  return bright * twinkle * uStarsIntensity * uStarVisibility * smoothstep(-0.02, 0.18, ray.y);
}

float disk(vec3 ray, vec3 direction, float inner, float outer) {
  return smoothstep(outer, inner, 1.0 - dot(ray, direction));
}

void main() {
  vec2 p = vUv * 2.0 - 1.0;
  p.x *= uResolution.x / max(1.0, uResolution.y);
  float focal = 1.0 / tan(uFov * 0.5);
  vec3 ray = normalize(uCameraBasis * normalize(vec3(p, focal)));
  vec3 color = atmosphere(ray);

  float starLight = stars(ray);
  color += starLight * mix(vec3(0.62, 0.75, 1.0), vec3(1.0, 0.84, 0.62), hash12(floor(vUv * 900.0))) * 2.4;

  float sunCore = disk(ray, uSunDirection, 0.00005, 0.00045);
  float sunGlow = pow(max(dot(ray, uSunDirection), 0.0), 256.0);
  color += vec3(1.0, 0.48, 0.12) * sunGlow * (0.45 + uDay * 1.8);
  color += uSunColor * vec3(1.0, 0.88, 0.58) * sunCore * uSunDiskIntensity * (0.55 + uDay * 1.25);

  float moonArea = uMoonScale * uMoonScale;
  float moonMask = disk(ray, uMoonDirection, 0.00035 * moonArea, 0.0014 * moonArea) * uNight;
  vec3 moonRight = normalize(cross(abs(uMoonDirection.y) > 0.9 ? vec3(1,0,0) : vec3(0,1,0), uMoonDirection));
  float phaseCoordinate = dot(ray - uMoonDirection, moonRight) * 46.0;
  float phaseCut = cos((uMoonPhase * 2.0 - 1.0) * PI);
  float moonLit = smoothstep(-0.10, 0.10, phaseCoordinate + phaseCut * 0.62);
  color += uMoonColor * vec3(0.72, 0.86, 1.0) * moonMask * mix(0.08, 2.5, moonLit);
  color += vec3(0.18, 0.28, 0.52) * pow(max(dot(ray, uMoonDirection), 0.0), 180.0) * uNight * 0.4;

  if (ray.y > 0.005 && uCloudCoverage > 0.005) {
    vec2 cloudUv = ray.xz / max(0.075, ray.y + 0.18);
    cloudUv = cloudUv * 0.34 + uCloudOffset;
    float low = cloudNoise(cloudUv * 0.72 + vec2(3.0, 11.0));
    float mid = cloudNoise(cloudUv);
    float high = cloudNoise(cloudUv * 2.17 + vec2(12.0, 4.0));
    float profileSum = max(0.001, dot(uCloudProfile, vec3(1.0)));
    float base = dot(vec3(low, mid, high), uCloudProfile) / profileSum;
    float detail = mix(mid, high, 0.62);
    float threshold = mix(0.82, 0.34, uCloudCoverage);
    float cloud = smoothstep(threshold, threshold + 0.22, mix(base, detail, 0.28));
    cloud *= smoothstep(0.005, 0.12, ray.y) * (1.0 - smoothstep(0.78, 0.99, ray.y));
    cloud = clamp(cloud * uCloudDensity * (0.65 + uCloudThickness * 0.7), 0.0, 0.96);
    float sunFacing = clamp(dot(ray, uSunDirection) * 0.5 + 0.5, 0.0, 1.0);
    vec3 cloudDark = mix(vec3(0.055, 0.075, 0.12), vec3(0.18, 0.22, 0.28), uDay);
    vec3 cloudLight = mix(vec3(0.24, 0.31, 0.48), vec3(0.88, 0.91, 0.92), uDay);
    vec3 cloudColor = mix(cloudDark, cloudLight, 0.25 + sunFacing * 0.65);
    cloudColor += uLightningColor * uLightningFlash * (0.3 + cloud * 1.3);
    color = mix(color, cloudColor, cloud);
  }

  float horizonFog = clamp(uFogDensity * 6.0 * pow(1.0 - max(ray.y, 0.0), 2.2), 0.0, 0.78);
  vec3 fogColor = mix(vec3(0.12, 0.17, 0.25), vec3(0.56, 0.66, 0.70), uDay);
  color = mix(color, fogColor, horizonFog);
  color *= uExposure;
  color = color / (vec3(1.0) + color);
  outColor = vec4(pow(max(color, 0.0), vec3(1.0 / 2.2)), 1.0);
}
