const V3 = {
  sub: (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]],
  cross: (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]],
  norm(a) { const d = Math.hypot(a[0], a[1], a[2]) || 1; return [a[0] / d, a[1] / d, a[2] / d]; }
};

function multiply(a, b) {
  const out = new Float32Array(16);
  for (let column = 0; column < 4; column++) {
    for (let row = 0; row < 4; row++) {
      let value = 0;
      for (let k = 0; k < 4; k++) value += a[k * 4 + row] * b[column * 4 + k];
      out[column * 4 + row] = value;
    }
  }
  return out;
}

function perspective(fov, aspect, near, far) {
  const f = 1 / Math.tan(fov / 2), range = 1 / (near - far);
  return new Float32Array([f / aspect,0,0,0, 0,f,0,0, 0,0,(far + near) * range,-1, 0,0,2 * far * near * range,0]);
}

function lookAt(eye, center, up) {
  const z = V3.norm(V3.sub(eye, center));
  const x = V3.norm(V3.cross(up, z));
  const y = V3.cross(z, x);
  return new Float32Array([
    x[0],y[0],z[0],0, x[1],y[1],z[1],0, x[2],y[2],z[2],0,
    -x[0]*eye[0]-x[1]*eye[1]-x[2]*eye[2],
    -y[0]*eye[0]-y[1]*eye[1]-y[2]*eye[2],
    -z[0]*eye[0]-z[1]*eye[1]-z[2]*eye[2],1
  ]);
}

function model(x, y, z, sx, sy, sz) {
  return new Float32Array([sx,0,0,0, 0,sy,0,0, 0,0,sz,0, x,y,z,1]);
}

function compile(gl, type, source) {
  const shader = gl.createShader(type);
  gl.shaderSource(shader, source);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(shader));
  return shader;
}

function createProgram(gl, vertex, fragment) {
  const program = gl.createProgram();
  gl.attachShader(program, compile(gl, gl.VERTEX_SHADER, vertex));
  gl.attachShader(program, compile(gl, gl.FRAGMENT_SHADER, fragment));
  gl.linkProgram(program);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(program));
  return program;
}

function mesh(gl, vertices, indices) {
  const vao = gl.createVertexArray();
  gl.bindVertexArray(vao);
  const buffer = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(vertices), gl.STATIC_DRAW);
  gl.enableVertexAttribArray(0);
  gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 24, 0);
  gl.enableVertexAttribArray(1);
  gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 24, 12);
  const element = gl.createBuffer();
  gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, element);
  gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new Uint16Array(indices), gl.STATIC_DRAW);
  gl.bindVertexArray(null);
  return { vao, count: indices.length };
}

function planeData() {
  return {
    vertices: [-1,0,-1, 0,1,0, 1,0,-1, 0,1,0, 1,0,1, 0,1,0, -1,0,1, 0,1,0],
    indices: [0,1,2, 0,2,3]
  };
}

function cubeData() {
  const vertices = [], indices = [];
  const faces = [
    [[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1],[0,0,1]],
    [[1,-1,-1],[-1,-1,-1],[-1,1,-1],[1,1,-1],[0,0,-1]],
    [[1,-1,1],[1,-1,-1],[1,1,-1],[1,1,1],[1,0,0]],
    [[-1,-1,-1],[-1,-1,1],[-1,1,1],[-1,1,-1],[-1,0,0]],
    [[-1,1,1],[1,1,1],[1,1,-1],[-1,1,-1],[0,1,0]],
    [[-1,-1,-1],[1,-1,-1],[1,-1,1],[-1,-1,1],[0,-1,0]]
  ];
  faces.forEach((face, faceIndex) => {
    const base = vertices.length / 6, normal = face[4];
    for (let i = 0; i < 4; i++) vertices.push(...face[i], ...normal);
    indices.push(base,base+1,base+2, base,base+2,base+3);
  });
  return { vertices, indices };
}

function sphereData(rows = 18, columns = 24) {
  const vertices = [], indices = [];
  for (let y = 0; y <= rows; y++) {
    const v = y / rows, phi = v * Math.PI;
    for (let x = 0; x <= columns; x++) {
      const u = x / columns, theta = u * Math.PI * 2;
      const nx = Math.sin(phi) * Math.cos(theta), ny = Math.cos(phi), nz = Math.sin(phi) * Math.sin(theta);
      vertices.push(nx, ny, nz, nx, ny, nz);
    }
  }
  for (let y = 0; y < rows; y++) for (let x = 0; x < columns; x++) {
    const a = y * (columns + 1) + x, b = a + columns + 1;
    indices.push(a,b,a+1, b,b+1,a+1);
  }
  return { vertices, indices };
}

export class DiagnosticRenderer {
  static async create(canvas) {
    const gl = canvas.getContext('webgl2', { antialias: true, alpha: false, powerPreference: 'high-performance' });
    if (!gl) throw new Error('WebGL2 недоступен');
    const [vertex, fragment] = await Promise.all([
      fetch('shaders/scene.vert').then(response => { if (!response.ok) throw new Error('scene.vert'); return response.text(); }),
      fetch('shaders/scene.frag').then(response => { if (!response.ok) throw new Error('scene.frag'); return response.text(); })
    ]);
    return new DiagnosticRenderer(canvas, gl, createProgram(gl, vertex, fragment));
  }

  constructor(canvas, gl, program) {
    this.canvas = canvas; this.gl = gl; this.program = program;
    this.camera = { yaw: .72, pitch: .42, distance: 13, target: [0, 1.15, 0] };
    this.light = { direction: [-.35, -1, -.2], color: [1, 1, 1], intensity: 1 };
    this.locations = {};
    ['uModel','uViewProjection','uCamera','uBaseColor','uLightDirection','uLightColor','uLightIntensity','uRoughness','uMetalness','uAlpha'].forEach(name => this.locations[name] = gl.getUniformLocation(program, name));
    const plane = planeData(), cube = cubeData(), sphere = sphereData();
    this.meshes = { plane: mesh(gl, plane.vertices, plane.indices), cube: mesh(gl, cube.vertices, cube.indices), sphere: mesh(gl, sphere.vertices, sphere.indices) };
    this.objects = [
      { mesh:'plane', matrix:model(0,0,0,9,1,9), color:[.21,.24,.25], roughness:.82, metalness:0, alpha:1 },
      { mesh:'sphere', matrix:model(-2.8,1.15,0,1.15,1.15,1.15), color:[.46,.18,.12], roughness:.76, metalness:0, alpha:1 },
      { mesh:'sphere', matrix:model(0,1.15,0,1.15,1.15,1.15), color:[.42,.46,.49], roughness:.2, metalness:1, alpha:1 },
      { mesh:'cube', matrix:model(2.75,1.05,0,1,1.05,1), color:[.16,.34,.42], roughness:.08, metalness:0, alpha:.34 },
      { mesh:'plane', matrix:model(0,.035,-4.8,3.2,1,1.8), color:[.05,.19,.23], roughness:.06, metalness:.08, alpha:.72 }
    ];
    this.pointers = new Map();
    this.installInput();
    this.reset();
    requestAnimationFrame(() => this.frame());
  }

  setSourceLight(direction, color, intensity) {
    if (Array.isArray(direction) && direction.length >= 3) this.light.direction = V3.norm(direction.slice(0, 3));
    if (Array.isArray(color) && color.length >= 3) this.light.color = color.slice(0, 3).map(value => Number(value) || 0);
    if (Number.isFinite(intensity)) this.light.intensity = Math.max(0, intensity);
  }

  reset() { this.camera = { yaw: .72, pitch: .42, distance: 13, target: [0, 1.15, 0] }; }

  installInput() {
    const canvas = this.canvas;
    canvas.addEventListener('pointerdown', event => { canvas.setPointerCapture(event.pointerId); this.pointers.set(event.pointerId, [event.clientX,event.clientY]); });
    canvas.addEventListener('pointerup', event => this.pointers.delete(event.pointerId));
    canvas.addEventListener('pointercancel', event => this.pointers.delete(event.pointerId));
    canvas.addEventListener('pointermove', event => {
      const previous = this.pointers.get(event.pointerId); if (!previous) return;
      const dx = event.clientX - previous[0], dy = event.clientY - previous[1];
      this.pointers.set(event.pointerId, [event.clientX,event.clientY]);
      if (this.pointers.size === 1) {
        this.camera.yaw -= dx * .007; this.camera.pitch = Math.max(.06, Math.min(1.35, this.camera.pitch + dy * .006));
      } else {
        this.camera.target[0] -= dx * .012; this.camera.target[2] += dy * .012;
        this.camera.target[1] = Math.max(0, this.camera.target[1]);
      }
    });
    canvas.addEventListener('wheel', event => { event.preventDefault(); this.camera.distance = Math.max(4, Math.min(30, this.camera.distance * Math.exp(event.deltaY * .001))); }, { passive:false });
    let lastDistance = null;
    canvas.addEventListener('touchmove', event => {
      if (event.touches.length !== 2) { lastDistance = null; return; }
      const a = event.touches[0], b = event.touches[1];
      const distance = Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
      if (lastDistance) this.camera.distance = Math.max(4, Math.min(30, this.camera.distance * lastDistance / distance));
      lastDistance = distance;
    }, { passive:false });
    canvas.addEventListener('touchend', () => { lastDistance = null; });
  }

  frame() {
    const gl = this.gl, ratio = Math.min(devicePixelRatio || 1, 2);
    const width = Math.max(1, Math.floor(this.canvas.clientWidth * ratio)), height = Math.max(1, Math.floor(this.canvas.clientHeight * ratio));
    if (this.canvas.width !== width || this.canvas.height !== height) { this.canvas.width = width; this.canvas.height = height; }
    gl.viewport(0,0,width,height); gl.clearColor(.018,.022,.028,1); gl.clearDepth(1); gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
    gl.enable(gl.DEPTH_TEST); gl.useProgram(this.program);
    const c = this.camera, cp = Math.cos(c.pitch), eye = [
      c.target[0] + Math.sin(c.yaw) * cp * c.distance,
      Math.max(.22, c.target[1] + Math.sin(c.pitch) * c.distance),
      c.target[2] + Math.cos(c.yaw) * cp * c.distance
    ];
    const vp = multiply(perspective(Math.PI / 3, width / height, .05, 100), lookAt(eye, c.target, [0,1,0]));
    gl.uniformMatrix4fv(this.locations.uViewProjection, false, vp); gl.uniform3fv(this.locations.uCamera, eye);
    gl.uniform3fv(this.locations.uLightDirection, this.light.direction); gl.uniform3fv(this.locations.uLightColor, this.light.color); gl.uniform1f(this.locations.uLightIntensity, this.light.intensity);
    for (const object of this.objects) {
      const item = this.meshes[object.mesh];
      gl.uniformMatrix4fv(this.locations.uModel, false, object.matrix); gl.uniform3fv(this.locations.uBaseColor, object.color);
      gl.uniform1f(this.locations.uRoughness, object.roughness); gl.uniform1f(this.locations.uMetalness, object.metalness); gl.uniform1f(this.locations.uAlpha, object.alpha);
      if (object.alpha < 1) { gl.enable(gl.BLEND); gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA); gl.depthMask(false); } else { gl.disable(gl.BLEND); gl.depthMask(true); }
      gl.bindVertexArray(item.vao); gl.drawElements(gl.TRIANGLES, item.count, gl.UNSIGNED_SHORT, 0);
    }
    gl.depthMask(true); gl.bindVertexArray(null); requestAnimationFrame(() => this.frame());
  }
}
