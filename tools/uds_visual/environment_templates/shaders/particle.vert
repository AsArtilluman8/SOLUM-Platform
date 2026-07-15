#version 300 es
precision highp float;
layout(location=0) in vec3 aPosition;
layout(location=1) in float aSize;
layout(location=2) in float aType;
uniform mat4 uViewProjection;
uniform vec3 uCamera;
out float vType;
out float vFade;
void main() {
  vec4 clip = uViewProjection * vec4(aPosition, 1.0);
  gl_Position = clip;
  float distanceScale = clamp(18.0 / max(2.0, length(uCamera - aPosition)), 0.45, 2.0);
  gl_PointSize = aSize * distanceScale;
  vType = aType;
  vFade = clamp((aPosition.y + 0.1) / 1.5, 0.15, 1.0);
}
