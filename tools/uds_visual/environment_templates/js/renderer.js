import { DeterministicRng, clamp } from './environment-core.js';

const V3 = {
  sub(out, a, b) { out[0] = a[0]-b[0]; out[1] = a[1]-b[1]; out[2] = a[2]-b[2]; return out; },
  cross(out, a, b) {
    const ax=a[0], ay=a[1], az=a[2], bx=b[0], by=b[1], bz=b[2];
    out[0]=ay*bz-az*by; out[1]=az*bx-ax*bz; out[2]=ax*by-ay*bx; return out;
  },
  norm(out, a) { const length=Math.hypot(a[0],a[1],a[2])||1; out[0]=a[0]/length; out[1]=a[1]/length; out[2]=a[2]/length; return out; }
};

function multiply(out, a, b) {
  for (let column=0; column<4; column++) for (let row=0; row<4; row++) {
    let value=0; for (let index=0; index<4; index++) value += a[index*4+row]*b[column*4+index];
    out[column*4+row]=value;
  }
  return out;
}

function perspective(out, fov, aspect, near, far) {
  const f=1/Math.tan(fov/2), range=1/(near-far);
  out[0]=f/aspect;out[1]=0;out[2]=0;out[3]=0;out[4]=0;out[5]=f;out[6]=0;out[7]=0;
  out[8]=0;out[9]=0;out[10]=(far+near)*range;out[11]=-1;out[12]=0;out[13]=0;out[14]=2*far*near*range;out[15]=0;
  return out;
}

function lookAt(out, eye, center, up, x, y, z) {
  V3.norm(z, V3.sub(z, eye, center)); V3.norm(x, V3.cross(x, up, z)); V3.cross(y, z, x);
  out[0]=x[0];out[1]=y[0];out[2]=z[0];out[3]=0;out[4]=x[1];out[5]=y[1];out[6]=z[1];out[7]=0;
  out[8]=x[2];out[9]=y[2];out[10]=z[2];out[11]=0;
  out[12]=-x[0]*eye[0]-x[1]*eye[1]-x[2]*eye[2];out[13]=-y[0]*eye[0]-y[1]*eye[1]-y[2]*eye[2];out[14]=-z[0]*eye[0]-z[1]*eye[1]-z[2]*eye[2];out[15]=1;
  return out;
}

function model(x,y,z,sx,sy,sz) {
  return new Float32Array([sx,0,0,0, 0,sy,0,0, 0,0,sz,0, x,y,z,1]);
}

function compile(gl, type, source, label) {
  const shader=gl.createShader(type); gl.shaderSource(shader,source); gl.compileShader(shader);
  if (!gl.getShaderParameter(shader,gl.COMPILE_STATUS)) throw new Error(`${label}: ${gl.getShaderInfoLog(shader)}`);
  return shader;
}

function createProgram(gl, vertex, fragment, label) {
  const program=gl.createProgram();
  gl.attachShader(program,compile(gl,gl.VERTEX_SHADER,vertex,`${label}.vert`));
  gl.attachShader(program,compile(gl,gl.FRAGMENT_SHADER,fragment,`${label}.frag`));
  gl.linkProgram(program);
  if (!gl.getProgramParameter(program,gl.LINK_STATUS)) throw new Error(`${label}: ${gl.getProgramInfoLog(program)}`);
  return program;
}

function locations(gl, program, names) {
  const result={}; for (const name of names) result[name]=gl.getUniformLocation(program,name); return result;
}

function mesh(gl, vertices, indices) {
  const vao=gl.createVertexArray(); gl.bindVertexArray(vao);
  const vertex=gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER,vertex);
  gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(vertices),gl.STATIC_DRAW);
  gl.enableVertexAttribArray(0); gl.vertexAttribPointer(0,3,gl.FLOAT,false,24,0);
  gl.enableVertexAttribArray(1); gl.vertexAttribPointer(1,3,gl.FLOAT,false,24,12);
  const element=gl.createBuffer(); gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER,element);
  gl.bufferData(gl.ELEMENT_ARRAY_BUFFER,new Uint16Array(indices),gl.STATIC_DRAW);
  gl.bindVertexArray(null); return {vao,count:indices.length};
}

function planeData() {
  return {vertices:[-1,0,-1,0,1,0, 1,0,-1,0,1,0, 1,0,1,0,1,0, -1,0,1,0,1,0],indices:[0,2,1,0,3,2]};
}

function cubeData() {
  const vertices=[],indices=[];
  const faces=[
    [[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1],[0,0,1]], [[1,-1,-1],[-1,-1,-1],[-1,1,-1],[1,1,-1],[0,0,-1]],
    [[1,-1,1],[1,-1,-1],[1,1,-1],[1,1,1],[1,0,0]], [[-1,-1,-1],[-1,-1,1],[-1,1,1],[-1,1,-1],[-1,0,0]],
    [[-1,1,1],[1,1,1],[1,1,-1],[-1,1,-1],[0,1,0]], [[-1,-1,-1],[1,-1,-1],[1,-1,1],[-1,-1,1],[0,-1,0]],
  ];
  for (const face of faces) { const base=vertices.length/6; for(let i=0;i<4;i++)vertices.push(...face[i],...face[4]); indices.push(base,base+1,base+2,base,base+2,base+3); }
  return {vertices,indices};
}

function sphereData(rows=18,columns=26) {
  const vertices=[],indices=[];
  for(let y=0;y<=rows;y++){const v=y/rows,phi=v*Math.PI;for(let x=0;x<=columns;x++){const u=x/columns,theta=u*Math.PI*2;const nx=Math.sin(phi)*Math.cos(theta),ny=Math.cos(phi),nz=Math.sin(phi)*Math.sin(theta);vertices.push(nx,ny,nz,nx,ny,nz);}}
  for(let y=0;y<rows;y++)for(let x=0;x<columns;x++){const a=y*(columns+1)+x,b=a+columns+1;indices.push(a,b,a+1,b,b+1,a+1);}
  return {vertices,indices};
}

async function shaderSource(path) {
  const response=await fetch(path,{cache:'no-store'}); if(!response.ok)throw new Error(`${path}: HTTP ${response.status}`); return response.text();
}

class ViewportInputRouter {
  constructor(canvas, camera, onReset) {
    this.canvas=canvas; this.camera=camera; this.onReset=onReset; this.pointers=new Map();
    this.gesture=null; this.lastTap={time:0,x:0,y:0}; this.install();
  }
  beginGesture() {
    const points=[...this.pointers.values()];
    if(points.length===1)this.gesture={mode:'orbit',x:points[0].x,y:points[0].y};
    else if(points.length>=2){const a=points[0],b=points[1];this.gesture={mode:'multi',distance:Math.hypot(a.x-b.x,a.y-b.y),x:(a.x+b.x)*.5,y:(a.y+b.y)*.5};}
  }
  install() {
    const canvas=this.canvas;
    canvas.addEventListener('pointerdown',event=>{
      event.preventDefault(); canvas.setPointerCapture(event.pointerId);
      const now=performance.now(),distance=Math.hypot(event.clientX-this.lastTap.x,event.clientY-this.lastTap.y);
      if(now-this.lastTap.time<320&&distance<28){this.onReset();this.lastTap.time=0;}else this.lastTap={time:now,x:event.clientX,y:event.clientY};
      this.pointers.set(event.pointerId,{x:event.clientX,y:event.clientY}); this.beginGesture(); document.body.classList.add('viewport-drag');
    });
    canvas.addEventListener('pointermove',event=>{
      const point=this.pointers.get(event.pointerId); if(!point)return;
      point.x=event.clientX;point.y=event.clientY;const points=[...this.pointers.values()];
      if(points.length===1&&this.gesture?.mode==='orbit'){
        const dx=point.x-this.gesture.x,dy=point.y-this.gesture.y;this.gesture.x=point.x;this.gesture.y=point.y;
        this.camera.yaw-=dx*.0065;this.camera.pitch=clamp(this.camera.pitch+dy*.0052,-1.28,1.34);
      }else if(points.length>=2){
        const a=points[0],b=points[1],distance=Math.max(8,Math.hypot(a.x-b.x,a.y-b.y)),x=(a.x+b.x)*.5,y=(a.y+b.y)*.5;
        if(this.gesture?.mode!=='multi'){this.beginGesture();return;}
        this.camera.distance=clamp(this.camera.distance*this.gesture.distance/distance,3.8,34);
        const scale=this.camera.distance*.00135;this.camera.target[0]-=(x-this.gesture.x)*scale;this.camera.target[2]+=(y-this.gesture.y)*scale;this.camera.target[1]=Math.max(0,this.camera.target[1]);
        this.gesture.distance=distance;this.gesture.x=x;this.gesture.y=y;
      }
    });
    const end=event=>{this.pointers.delete(event.pointerId);this.beginGesture();if(!this.pointers.size)document.body.classList.remove('viewport-drag');};
    canvas.addEventListener('pointerup',end);canvas.addEventListener('pointercancel',end);
    canvas.addEventListener('wheel',event=>{event.preventDefault();this.camera.distance=clamp(this.camera.distance*Math.exp(event.deltaY*.001),3.8,34);},{passive:false});
  }
}

export class SolumEnvironmentRenderer {
  static async create(canvas, packageData) {
    const gl=canvas.getContext('webgl2',{antialias:true,alpha:false,depth:true,powerPreference:'high-performance'});
    if(!gl)throw new Error('WebGL2 недоступен');
    const names=['sky.vert','sky.frag','scene.vert','scene.frag','particle.vert','particle.frag','line.vert','line.frag'];
    const sources=await Promise.all(names.map(name=>shaderSource(`shaders/${name}`)));
    return new SolumEnvironmentRenderer(canvas,gl,packageData,{
      sky:createProgram(gl,sources[0],sources[1],'sky'),scene:createProgram(gl,sources[2],sources[3],'scene'),
      particle:createProgram(gl,sources[4],sources[5],'particle'),line:createProgram(gl,sources[6],sources[7],'line'),
    });
  }

  constructor(canvas,gl,packageData,programs) {
    this.canvas=canvas;this.gl=gl;this.package=packageData;this.programs=programs;
    this.camera={yaw:.72,pitch:.34,distance:13.5,target:new Float32Array([0,1.2,0]),fov:Math.PI/3};
    this.eye=new Float32Array(3);this.cameraBasis=new Float32Array(9);this.view=new Float32Array(16);this.projection=new Float32Array(16);this.viewProjection=new Float32Array(16);
    this.viewportSize=new Int32Array(2);
    this.viewX=new Float32Array(3);this.viewY=new Float32Array(3);this.viewZ=new Float32Array(3);this.worldUp=new Float32Array([0,1,0]);
    this.clock=0;this.lastFrame=performance.now();this.frameCallback=now=>this.frame(now);
    this.quality='Medium';this.metricsCallback=null;this.stateCallback=null;this.runtime=null;
    this.frameSamples=new Float32Array(90);this.frameCursor=0;this.frameCount=0;this.lastMetricUpdate=0;
    this.setupPrograms();this.setupGeometry();this.setupParticles();this.setupWindLines();
    this.input=new ViewportInputRouter(canvas,this.camera,()=>this.resetCamera());
  }

  setupPrograms() {
    const gl=this.gl,p=this.programs;
    this.uSky=locations(gl,p.sky,['uResolution','uCameraBasis','uFov','uClock','uSunDirection','uMoonDirection','uSunColor','uMoonColor','uDay','uTwilight','uNight','uStarVisibility','uMoonPhase','uMoonScale','uSunDiskIntensity','uStarsIntensity','uStarsSpeed','uTwinkleAmount','uTwinkleSpeed','uCloudCoverage','uCloudDensity','uCloudThickness','uCloudProfile','uCloudOffset','uCloudSteps','uFogDensity','uHumidity','uHaze','uAbsorption','uDust','uLightningFlash','uLightningColor','uExposure']);
    this.uScene=locations(gl,p.scene,['uModel','uViewProjection','uCamera','uBaseColor','uSunDirection','uMoonDirection','uSunColor','uMoonColor','uSunLight','uMoonLight','uAmbient','uLightningFlash','uLightningColor','uRoughness','uMetalness','uAlpha','uWetness','uSnow','uDust','uFogDensity','uFogHeightFalloff','uDay','uMaterialType']);
    this.uParticle=locations(gl,p.particle,['uViewProjection','uCamera','uAlpha']);
    this.uLine=locations(gl,p.line,['uViewProjection','uColor']);
  }

  setupGeometry() {
    const gl=this.gl,plane=planeData(),cube=cubeData(),sphere=sphereData();
    this.meshes={plane:mesh(gl,plane.vertices,plane.indices),cube:mesh(gl,cube.vertices,cube.indices),sphere:mesh(gl,sphere.vertices,sphere.indices)};
    this.objects=[
      {mesh:'plane',matrix:model(0,0,0,14,1,14),color:[.18,.22,.19],roughness:.82,metalness:0,alpha:1,type:0},
      {mesh:'plane',matrix:model(0,.045,-5.3,3.8,1,1.65),color:[.035,.16,.19],roughness:.04,metalness:.05,alpha:.88,type:1},
      {mesh:'sphere',matrix:model(-3.0,1.15,.2,1.15,1.15,1.15),color:[.46,.18,.11],roughness:.78,metalness:0,alpha:1,type:3},
      {mesh:'sphere',matrix:model(0,1.15,.2,1.15,1.15,1.15),color:[.48,.52,.56],roughness:.16,metalness:1,alpha:1,type:3},
      {mesh:'cube',matrix:model(3.0,1.05,.2,1,1.05,1),color:[.12,.34,.39],roughness:.06,metalness:0,alpha:.34,type:2},
      {mesh:'cube',matrix:model(-5.5,2.0,-5.8,.32,2,.32),color:[.28,.30,.32],roughness:.62,metalness:.1,alpha:1,type:3},
      {mesh:'cube',matrix:model(5.4,3.0,-6.8,.42,3,.42),color:[.32,.34,.35],roughness:.52,metalness:.15,alpha:1,type:3},
      {mesh:'cube',matrix:model(3.7,1.9,-8.2,.28,1.9,.28),color:[.26,.28,.29],roughness:.68,metalness:0,alpha:1,type:3},
    ];
    this.opaqueObjects=this.objects.filter(item=>item.alpha>=1);this.transparentObjects=this.objects.filter(item=>item.alpha<1);this.objectGroups=[this.opaqueObjects,this.transparentObjects];
    const quad=new Float32Array([-1,-1,1,-1,-1,1,-1,1,1,-1,1,1]);
    this.skyVao=gl.createVertexArray();gl.bindVertexArray(this.skyVao);
    const buffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buffer);gl.bufferData(gl.ARRAY_BUFFER,quad,gl.STATIC_DRAW);
    gl.enableVertexAttribArray(0);gl.vertexAttribPointer(0,2,gl.FLOAT,false,0,0);gl.bindVertexArray(null);
  }

  setupParticles() {
    const gl=this.gl,max=this.package.qualityTiers.High.particleLimit;
    this.maxParticles=max;this.particleRng=new DeterministicRng(9001);
    this.particlePosition=new Float32Array(max*3);this.particleSeed=new Float32Array(max);this.particleType=new Uint8Array(max);this.particleVertex=new Float32Array(max*5);
    this.particleVao=gl.createVertexArray();gl.bindVertexArray(this.particleVao);
    this.particleBuffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,this.particleBuffer);gl.bufferData(gl.ARRAY_BUFFER,this.particleVertex.byteLength,gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(0);gl.vertexAttribPointer(0,3,gl.FLOAT,false,20,0);gl.enableVertexAttribArray(1);gl.vertexAttribPointer(1,1,gl.FLOAT,false,20,12);gl.enableVertexAttribArray(2);gl.vertexAttribPointer(2,1,gl.FLOAT,false,20,16);gl.bindVertexArray(null);
    for(let index=0;index<max;index++){this.particleSeed[index]=this.particleRng.next();this.particleType[index]=255;this.respawnParticle(index,0,true);}
  }

  respawnParticle(index,type,initial=false) {
    const base=index*3,spread=18;
    this.particlePosition[base]=(this.particleRng.next()-.5)*spread;
    this.particlePosition[base+1]=initial?this.particleRng.next()*14+1:12+this.particleRng.next()*8;
    this.particlePosition[base+2]=(this.particleRng.next()-.5)*spread-1;
    this.particleType[index]=type;
  }

  setupWindLines() {
    const gl=this.gl;this.windVertex=new Float32Array(384);this.windVertexCount=0;this.flagPoles=new Float32Array([-6,-2.5,3.4,5.8,-2.2,2.9]);
    this.lineVao=gl.createVertexArray();gl.bindVertexArray(this.lineVao);this.lineBuffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,this.lineBuffer);gl.bufferData(gl.ARRAY_BUFFER,this.windVertex.byteLength,gl.DYNAMIC_DRAW);gl.enableVertexAttribArray(0);gl.vertexAttribPointer(0,3,gl.FLOAT,false,12,0);gl.bindVertexArray(null);
    this.boltVao=gl.createVertexArray();gl.bindVertexArray(this.boltVao);this.boltBuffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,this.boltBuffer);gl.bufferData(gl.ARRAY_BUFFER,18*3*4,gl.DYNAMIC_DRAW);gl.enableVertexAttribArray(0);gl.vertexAttribPointer(0,3,gl.FLOAT,false,12,0);gl.bindVertexArray(null);
  }

  resetCamera(camera=null) {
    const value=camera||{yaw:.72,pitch:.34,distance:13.5,target:[0,1.2,0]};
    this.camera.yaw=value.yaw;this.camera.pitch=value.pitch;this.camera.distance=value.distance;this.camera.target.set(value.target);
  }
  setQuality(name){this.quality=name;}
  onMetrics(callback){this.metricsCallback=callback;}
  onState(callback){this.stateCallback=callback;}
  start(runtime){this.runtime=runtime;this.lastFrame=performance.now();this.frame(this.lastFrame);}

  updateCamera(width,height) {
    const c=this.camera,cp=Math.cos(c.pitch);
    this.eye[0]=c.target[0]+Math.sin(c.yaw)*cp*c.distance;
    this.eye[1]=Math.max(.28,c.target[1]+Math.sin(c.pitch)*c.distance);
    this.eye[2]=c.target[2]+Math.cos(c.yaw)*cp*c.distance;
    lookAt(this.view,this.eye,c.target,this.worldUp,this.viewX,this.viewY,this.viewZ);perspective(this.projection,c.fov,width/height,.05,140);multiply(this.viewProjection,this.projection,this.view);
    this.cameraBasis[0]=this.viewX[0];this.cameraBasis[1]=this.viewX[1];this.cameraBasis[2]=this.viewX[2];
    this.cameraBasis[3]=this.viewY[0];this.cameraBasis[4]=this.viewY[1];this.cameraBasis[5]=this.viewY[2];
    this.cameraBasis[6]=-this.viewZ[0];this.cameraBasis[7]=-this.viewZ[1];this.cameraBasis[8]=-this.viewZ[2];
    return this.viewProjection;
  }

  resize() {
    const tier=this.package.qualityTiers[this.quality],ratio=Math.min(devicePixelRatio||1,tier.pixelRatioMax);
    let width=Math.max(1,Math.floor(this.canvas.clientWidth*ratio)),height=Math.max(1,Math.floor(this.canvas.clientHeight*ratio));
    const edgeScale=Math.min(1,tier.renderLongEdgeMax/Math.max(width,height));width=Math.max(1,Math.floor(width*edgeScale));height=Math.max(1,Math.floor(height*edgeScale));
    if(this.canvas.width!==width||this.canvas.height!==height){this.canvas.width=width;this.canvas.height=height;}
    this.gl.viewport(0,0,width,height);this.viewportSize[0]=width;this.viewportSize[1]=height;return this.viewportSize;
  }

  drawSky(snapshot,width,height) {
    const gl=this.gl,u=this.uSky,s=snapshot;
    gl.disable(gl.DEPTH_TEST);gl.depthMask(false);gl.useProgram(this.programs.sky);gl.bindVertexArray(this.skyVao);
    gl.uniform2f(u.uResolution,width,height);gl.uniformMatrix3fv(u.uCameraBasis,false,this.cameraBasis);gl.uniform1f(u.uFov,this.camera.fov);gl.uniform1f(u.uClock,this.clock);
    gl.uniform3fv(u.uSunDirection,s.celestial.sunDirection);gl.uniform3fv(u.uMoonDirection,s.celestial.moonDirection);gl.uniform3fv(u.uSunColor,s.celestial.sunColor);gl.uniform3fv(u.uMoonColor,s.celestial.moonColor);gl.uniform1f(u.uDay,s.celestial.day);gl.uniform1f(u.uTwilight,s.celestial.twilight);gl.uniform1f(u.uNight,s.celestial.night);gl.uniform1f(u.uStarVisibility,s.celestial.starVisibility);gl.uniform1f(u.uMoonPhase,s.celestial.moonPhase);gl.uniform1f(u.uMoonScale,s.celestial.moonScale);gl.uniform1f(u.uSunDiskIntensity,s.celestial.sunDiskIntensity);gl.uniform1f(u.uStarsIntensity,s.celestial.starsIntensity);gl.uniform1f(u.uStarsSpeed,s.celestial.starsSpeed);gl.uniform1f(u.uTwinkleAmount,s.celestial.twinkleAmount);gl.uniform1f(u.uTwinkleSpeed,s.celestial.twinkleSpeed);
    gl.uniform1f(u.uCloudCoverage,s.clouds.coverage);gl.uniform1f(u.uCloudDensity,s.clouds.density);gl.uniform1f(u.uCloudThickness,s.clouds.thickness);gl.uniform3fv(u.uCloudProfile,s.clouds.profile);gl.uniform2f(u.uCloudOffset,s.clouds.offsetX,s.clouds.offsetZ);gl.uniform1i(u.uCloudSteps,this.package.qualityTiers[this.quality].cloudSteps);
    gl.uniform1f(u.uFogDensity,s.fog.density);gl.uniform1f(u.uHumidity,s.atmosphere.humidity);gl.uniform1f(u.uHaze,s.atmosphere.haze);gl.uniform1f(u.uAbsorption,s.atmosphere.absorption);gl.uniform1f(u.uDust,s.weather.dust);gl.uniform1f(u.uLightningFlash,s.lighting.flash*.64);gl.uniform3fv(u.uLightningColor,s.lightning.lightColor);gl.uniform1f(u.uExposure,s.lighting.exposure);
    gl.drawArrays(gl.TRIANGLES,0,6);gl.bindVertexArray(null);gl.depthMask(true);gl.enable(gl.DEPTH_TEST);
  }

  sceneGlobals(snapshot,vp) {
    const gl=this.gl,u=this.uScene,s=snapshot;gl.useProgram(this.programs.scene);
    gl.uniformMatrix4fv(u.uViewProjection,false,vp);gl.uniform3fv(u.uCamera,this.eye);gl.uniform3fv(u.uSunDirection,s.celestial.sunDirection);gl.uniform3fv(u.uMoonDirection,s.celestial.moonDirection);
    gl.uniform3fv(u.uSunColor,s.celestial.sunColor);gl.uniform3fv(u.uMoonColor,s.celestial.moonColor);gl.uniform1f(u.uSunLight,s.lighting.sun*.44);gl.uniform1f(u.uMoonLight,s.lighting.moon*1.47);gl.uniform1f(u.uAmbient,s.lighting.ambient*.42);gl.uniform1f(u.uLightningFlash,s.lighting.flash*.64);gl.uniform3fv(u.uLightningColor,s.lightning.lightColor);
    gl.uniform1f(u.uWetness,s.wetness.value);gl.uniform1f(u.uSnow,s.weather.surfaceSnowTarget||s.weather.snow*.6);gl.uniform1f(u.uDust,s.weather.surfaceDustTarget||s.weather.dust*.7);gl.uniform1f(u.uFogDensity,s.fog.density);gl.uniform1f(u.uFogHeightFalloff,s.fog.heightFalloff);gl.uniform1f(u.uDay,s.celestial.day);
  }

  drawScene(snapshot,vp) {
    const gl=this.gl,u=this.uScene;this.sceneGlobals(snapshot,vp);
    for(const group of this.objectGroups)for(const object of group){
      if(object.alpha<1){gl.enable(gl.BLEND);gl.blendFunc(gl.SRC_ALPHA,gl.ONE_MINUS_SRC_ALPHA);gl.depthMask(false);}else{gl.disable(gl.BLEND);gl.depthMask(true);}
      gl.uniformMatrix4fv(u.uModel,false,object.matrix);gl.uniform3fv(u.uBaseColor,object.color);gl.uniform1f(u.uRoughness,object.roughness);gl.uniform1f(u.uMetalness,object.metalness);gl.uniform1f(u.uAlpha,object.alpha);gl.uniform1i(u.uMaterialType,object.type);
      const item=this.meshes[object.mesh];gl.bindVertexArray(item.vao);gl.drawElements(gl.TRIANGLES,item.count,gl.UNSIGNED_SHORT,0);
    }
    gl.depthMask(true);gl.disable(gl.BLEND);gl.bindVertexArray(null);
  }

  updateParticles(deltaSeconds,snapshot) {
    const p=snapshot.precipitation,total=p.rainCount+p.snowCount+p.dustCount,wind=snapshot.wind.vector;
    let rainEnd=p.rainCount,snowEnd=rainEnd+p.snowCount;
    for(let index=0;index<total;index++){
      const type=index<rainEnd?0:index<snowEnd?1:2,base=index*3,seed=this.particleSeed[index];
      if(this.particleType[index]!==type)this.respawnParticle(index,type,true);
      if(type===0){this.particlePosition[base+1]-=deltaSeconds*(14+seed*8*p.rainVelocityRandomization);this.particlePosition[base]+=wind[0]*deltaSeconds*5*p.rainWindScale;this.particlePosition[base+2]+=wind[2]*deltaSeconds*5*p.rainWindScale;}
      else if(type===1){this.particlePosition[base+1]-=deltaSeconds*(1.1+seed*1.9*p.snowVelocityRandomization);this.particlePosition[base]+=wind[0]*deltaSeconds*3*p.snowWindScale+Math.sin(this.clock*1.7+index)*deltaSeconds*.45;this.particlePosition[base+2]+=wind[2]*deltaSeconds*3*p.snowWindScale;}
      else{this.particlePosition[base+1]+=Math.sin(this.clock*.8+index)*deltaSeconds*.22;this.particlePosition[base]+=wind[0]*deltaSeconds*7;this.particlePosition[base+2]+=wind[2]*deltaSeconds*7;}
      if(this.particlePosition[base+1]<.02||Math.abs(this.particlePosition[base])>13||Math.abs(this.particlePosition[base+2])>15)this.respawnParticle(index,type,false);
      const vertex=index*5;this.particleVertex[vertex]=this.particlePosition[base];this.particleVertex[vertex+1]=this.particlePosition[base+1];this.particleVertex[vertex+2]=this.particlePosition[base+2];this.particleVertex[vertex+3]=type===0?11*p.rainScale:type===1?5.5*p.snowScale:7;this.particleVertex[vertex+4]=type;
    }
    return total;
  }

  drawParticles(snapshot,vp,deltaSeconds) {
    const gl=this.gl,count=this.updateParticles(deltaSeconds,snapshot);if(!count)return;
    gl.useProgram(this.programs.particle);gl.uniformMatrix4fv(this.uParticle.uViewProjection,false,vp);gl.uniform3fv(this.uParticle.uCamera,this.eye);gl.uniform3f(this.uParticle.uAlpha,snapshot.precipitation.rainAlpha,snapshot.precipitation.snowAlpha,.34);
    gl.bindVertexArray(this.particleVao);gl.bindBuffer(gl.ARRAY_BUFFER,this.particleBuffer);gl.bufferSubData(gl.ARRAY_BUFFER,0,this.particleVertex,0,count*5);
    gl.enable(gl.BLEND);gl.blendFunc(gl.SRC_ALPHA,gl.ONE_MINUS_SRC_ALPHA);gl.depthMask(false);gl.drawArrays(gl.POINTS,0,count);gl.depthMask(true);gl.disable(gl.BLEND);gl.bindVertexArray(null);
  }

  updateWindLines(snapshot) {
    let cursor=0;const data=this.windVertex,push=(ax,ay,az,bx,by,bz)=>{data[cursor++]=ax;data[cursor++]=ay;data[cursor++]=az;data[cursor++]=bx;data[cursor++]=by;data[cursor++]=bz;};
    const strength=snapshot.wind.speed*(.55+snapshot.wind.gust),sway=Math.sin(this.clock*2.1)*strength;
    for(let pole=0;pole<this.flagPoles.length;pole+=3){const x=this.flagPoles[pole],z=this.flagPoles[pole+1],height=this.flagPoles[pole+2];
      push(x,0,z,x,height,z);const dx=snapshot.wind.vector[0]*1.5+sway*.65,dz=snapshot.wind.vector[2]*1.5;
      push(x,height,z,x+1.8+dx,height-.22*Math.abs(sway),z+dz);push(x+1.8+dx,height-.22*Math.abs(sway),z+dz,x+1.58+dx,height-.92,z+dz*.9);push(x+1.58+dx,height-.92,z+dz*.9,x,height-.68,z);
    }
    for(let index=0;index<18;index++){const x=-7+index*.82,z=3.8+Math.sin(index*1.7)*1.5,h=.6+(index%4)*.13,bend=strength*(.22+.08*(index%3));push(x,0,z,x+bend*Math.sin(this.clock*2+index),h,z+bend*Math.cos(this.clock*1.6+index));}
    this.windVertexCount=cursor/3;
  }

  drawLines(snapshot,vp) {
    const gl=this.gl,u=this.uLine;this.updateWindLines(snapshot);gl.useProgram(this.programs.line);gl.uniformMatrix4fv(u.uViewProjection,false,vp);gl.uniform4f(u.uColor,.28,.92,.67,.78);gl.bindVertexArray(this.lineVao);gl.bindBuffer(gl.ARRAY_BUFFER,this.lineBuffer);gl.bufferSubData(gl.ARRAY_BUFFER,0,this.windVertex,0,this.windVertexCount*3);gl.drawArrays(gl.LINES,0,this.windVertexCount);
    if(snapshot.lightning.active&&snapshot.lightning.boltPoints>1){const color=snapshot.lightning.lightColor;gl.bindVertexArray(this.boltVao);gl.bindBuffer(gl.ARRAY_BUFFER,this.boltBuffer);gl.bufferSubData(gl.ARRAY_BUFFER,0,snapshot.lightning.bolt,0,snapshot.lightning.boltPoints*3);gl.enable(gl.BLEND);gl.blendFunc(gl.SRC_ALPHA,gl.ONE);gl.uniform4f(u.uColor,color[0],color[1],color[2],.22+snapshot.lightning.flash*.78);gl.drawArrays(gl.LINE_STRIP,0,snapshot.lightning.boltPoints);gl.disable(gl.BLEND);}
    gl.bindVertexArray(null);
  }

  updateMetrics(now,frameMs,snapshot) {
    this.frameSamples[this.frameCursor]=frameMs;this.frameCursor=(this.frameCursor+1)%this.frameSamples.length;this.frameCount=Math.min(this.frameCount+1,this.frameSamples.length);
    if(now-this.lastMetricUpdate<500)return;this.lastMetricUpdate=now;let total=0;for(let i=0;i<this.frameCount;i++)total+=this.frameSamples[i];const average=total/Math.max(1,this.frameCount);
    if(this.metricsCallback)this.metricsCallback({fps:average>0?1000/average:0,frameMs:average,quality:this.quality,particles:snapshot.precipitation.rainCount+snapshot.precipitation.snowCount+snapshot.precipitation.dustCount});
    if(this.stateCallback)this.stateCallback(snapshot);
  }

  frame(now) {
    if(!this.runtime)return;const frameMs=Math.min(50,Math.max(0,now-this.lastFrame)),deltaSeconds=frameMs/1000;this.lastFrame=now;this.clock+=deltaSeconds;
    const snapshot=this.runtime.update(deltaSeconds);this.quality=snapshot.quality;const [width,height]=this.resize(),vp=this.updateCamera(width,height),gl=this.gl;
    gl.clearColor(.01,.02,.03,1);gl.clearDepth(1);gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);gl.enable(gl.DEPTH_TEST);gl.enable(gl.CULL_FACE);gl.cullFace(gl.BACK);
    this.drawSky(snapshot,width,height);this.drawScene(snapshot,vp);this.drawParticles(snapshot,vp,deltaSeconds);this.drawLines(snapshot,vp);this.updateMetrics(now,frameMs,snapshot);
    requestAnimationFrame(this.frameCallback);
  }
}
