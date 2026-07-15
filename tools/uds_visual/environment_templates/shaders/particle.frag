#version 300 es
precision highp float;
in float vType;
in float vFade;
uniform vec3 uAlpha;
out vec4 outColor;
void main() {
  vec2 point = gl_PointCoord * 2.0 - 1.0;
  if (vType < 0.5) {
    float streak = smoothstep(0.23, 0.02, abs(point.x)) * smoothstep(1.0, -0.7, point.y);
    if (streak < 0.02) discard;
    outColor = vec4(0.52, 0.72, 1.0, streak * 0.72 * vFade * uAlpha.x);
  } else if (vType < 1.5) {
    float flake = smoothstep(1.0, 0.35, length(point));
    if (flake < 0.02) discard;
    outColor = vec4(0.92, 0.96, 1.0, flake * 0.82 * vFade * uAlpha.y);
  } else {
    float mote = smoothstep(1.0, 0.05, length(point));
    if (mote < 0.02) discard;
    outColor = vec4(0.72, 0.48, 0.24, mote * uAlpha.z * vFade);
  }
}
