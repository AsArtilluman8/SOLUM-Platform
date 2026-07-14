#version 300 es
precision highp float;
in vec3 vWorld;
in vec3 vNormal;
uniform vec3 uCamera;
uniform vec3 uBaseColor;
uniform vec3 uLightDirection;
uniform vec3 uLightColor;
uniform float uLightIntensity;
uniform float uRoughness;
uniform float uMetalness;
uniform float uAlpha;
out vec4 outColor;
void main() {
  vec3 n = normalize(vNormal);
  vec3 l = normalize(-uLightDirection);
  vec3 v = normalize(uCamera - vWorld);
  vec3 h = normalize(l + v);
  float ndl = max(dot(n, l), 0.0);
  float ndh = max(dot(n, h), 0.0);
  float shine = mix(96.0, 5.0, clamp(uRoughness, 0.0, 1.0));
  float specular = pow(ndh, shine) * ndl;
  vec3 dielectric = vec3(0.035);
  vec3 f0 = mix(dielectric, uBaseColor, clamp(uMetalness, 0.0, 1.0));
  vec3 diffuse = uBaseColor * (1.0 - uMetalness) * (0.07 + ndl * uLightColor * uLightIntensity);
  vec3 color = diffuse + f0 * specular * uLightColor * uLightIntensity;
  color = color / (vec3(1.0) + color);
  color = pow(color, vec3(1.0 / 2.2));
  outColor = vec4(color, uAlpha);
}
