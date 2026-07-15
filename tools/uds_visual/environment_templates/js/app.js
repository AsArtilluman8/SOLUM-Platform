import { SolumEnvironmentRuntime, SolumTimeSystem } from './environment-core.js';
import { SolumEnvironmentRenderer } from './renderer.js';

const $ = selector => document.querySelector(selector);
const packagePath = 'data/solum_environment_package.json';

function formatTime(value) {
  const parts = SolumTimeSystem.parts(value);
  return `${String(parts.hours).padStart(2,'0')}:${String(parts.minutes).padStart(2,'0')} · ${Math.round(parts.value)}`;
}

function formatNumber(value, digits=2) {
  return Number(value).toFixed(digits).replace(/\.0+$/,'').replace(/(\.\d*?)0+$/,'$1');
}

function download(name,text,type='application/json') {
  const url=URL.createObjectURL(new Blob([text],{type})),link=document.createElement('a');
  link.href=url;link.download=name;link.click();setTimeout(()=>URL.revokeObjectURL(url),0);
}

async function loadPackage() {
  const response=await fetch(packagePath,{cache:'no-store'});
  if(!response.ok)throw new Error(`${packagePath}: HTTP ${response.status}`);
  const value=await response.json();
  if(value.schema!=='solum.environment.package'||value.schemaVersion!==1||value.weatherPresets?.length!==13)throw new Error('compact package schema/preset count invalid');
  return value;
}

function statusClass(status) {
  return ({UDS_VERIFIED:'verified',UDS_DERIVED_MAPPING:'derived',SOLUM_NATIVE:'native',UNKNOWN:'unknown',UNAVAILABLE:'unavailable'})[status]||'unknown';
}

function makeStatus(status) {
  const element=document.createElement('em');element.className=`status ${statusClass(status)}`;element.textContent=status;return element;
}

function setupAccuracy(packageData) {
  const list=$('#accuracy-list');
  for(const module of packageData.modules){
    const card=document.createElement('article');card.className='truth-card';
    const header=document.createElement('header'),title=document.createElement('h3'),text=document.createElement('p');
    title.textContent=module.name;header.append(title,makeStatus(module.status));
    text.textContent=module.status==='SOLUM_NATIVE'?'Независимая реализация SOLUM; точная копия UDS не заявляется.':module.status==='UDS_DERIVED_MAPPING'?'Использует подтверждённые UDS значения через собственный WebGL2 adapter.':module.status==='UNKNOWN'?'Payload доступен только вручную; автоматическая привязка не установлена.':'Точное подтверждённое значение или payload UDS.';
    card.append(header,text);list.append(card);
  }
  for(const texture of packageData.resources.textures.slice(0,6)){
    const card=document.createElement('article');card.className='truth-card';
    const header=document.createElement('header'),title=document.createElement('h3'),text=document.createElement('p');
    title.textContent=texture.sourcePackage.split('/').pop();header.append(title,makeStatus(texture.status));text.textContent='Оригинальный texture payload не активирован; runtime использует явно помеченный процедурный fallback.';card.append(header,text);list.append(card);
  }
}

function setupAudio(packageData,runtime) {
  const list=$('#audio-list');
  for(const resource of packageData.resources.audio){
    const card=document.createElement('article');card.className='audio-card';
    const header=document.createElement('header'),title=document.createElement('h3'),details=document.createElement('p'),button=document.createElement('button');
    title.textContent=resource.sourcePackage.split('/').pop();header.append(title,makeStatus(resource.payloadStatus));
    details.textContent=`${resource.size} B · binding ${resource.bindingStatus} · auto OFF`;
    button.type='button';button.textContent='▶ Прослушать WAV вручную';
    button.addEventListener('click',async()=>{
      try{button.disabled=true;await runtime.audio.playManual(resource.id);button.textContent='■ Воспроизводится';setTimeout(()=>{button.disabled=false;button.textContent='▶ Прослушать WAV вручную';},1600);}
      catch(error){button.disabled=false;button.textContent='Ошибка воспроизведения';console.error(error);}
    });
    card.append(header,details,button);list.append(card);
  }
}

function setupScenarios(packageData,runtime,renderer,transitionControl) {
  const list=$('#scenario-list');
  for(const scenario of packageData.scenarios){
    const button=document.createElement('button');button.type='button';button.className='scenario-card';
    const title=document.createElement('b'),text=document.createElement('span');title.textContent=scenario.label;text.textContent=scenario.expectedVisualSigns;button.append(title,text);
    button.addEventListener('click',()=>{
      runtime.time.set(scenario.input.timeOfDay);
      runtime.selectWeather(scenario.input.weatherPreset,0);
      const duration=Number(scenario.target.transitionSeconds)||0;
      transitionControl.value=duration;$('#transition-value').textContent=`${duration.toFixed(1)} s`;
      runtime.selectWeather(scenario.target.weatherPreset,duration);
      runtime.time.transitionTo(scenario.target.timeOfDay,duration);
      renderer.resetCamera(scenario.camera);
      $('#weather').value=scenario.target.weatherPreset;
      $('#lightning').disabled=(scenario.finalRuntime.lightningEnabled||0)<.5;
      document.querySelector('[data-tab="weather"]').click();
    });
    list.append(button);
  }
}

function setupTabs() {
  $('#tabs').addEventListener('click',event=>{
    const button=event.target.closest('button[data-tab]');if(!button)return;
    document.querySelectorAll('#tabs button').forEach(item=>item.classList.toggle('active',item===button));
    document.querySelectorAll('.page').forEach(item=>item.classList.toggle('active',item.dataset.page===button.dataset.tab));
  });
}

function setupPanel() {
  $('#panel-toggle').addEventListener('click',event=>{
    const collapsed=$('#panel').classList.toggle('collapsed');event.currentTarget.classList.toggle('collapsed',collapsed);
    event.currentTarget.textContent=collapsed?'Показать управление':'Скрыть управление';event.currentTarget.setAttribute('aria-expanded',String(!collapsed));
  });
}

async function main() {
  const packageData=await loadPackage(),runtime=new SolumEnvironmentRuntime(packageData),renderer=await SolumEnvironmentRenderer.create($('#viewport'),packageData);
  const presetById=new Map(packageData.weatherPresets.map(item=>[item.id,item]));
  const weather=$('#weather'),transition=$('#transition'),quality=$('#quality'),time=$('#time'),timeSpeed=$('#time-speed');
  for(const preset of packageData.weatherPresets){const option=document.createElement('option');option.value=preset.id;option.textContent=preset.name;weather.append(option);}
  weather.value=runtime.state.initialPreset;time.value=runtime.time.value;timeSpeed.value=runtime.time.speed;

  const sliderBindings={
    clouds:['cloudCoverage','#clouds-value',2],fog:['fogDensity','#fog-value',3],rain:['rain','#rain-value',2],snow:['snow','#snow-value',2],wind:['windSpeed','#wind-value',2],humidity:['humidity','#humidity-value',2],wetness:['wetnessTarget','#wetness-value',2],lightning:['lightningPotential','#lightning-value',2],
  };
  for(const [id,[field,output,digits]] of Object.entries(sliderBindings)){
    const element=$(`#${id}`);element.addEventListener('input',()=>{
      runtime.weather.setLiveValue(field,Number(element.value));
      if(id==='lightning')runtime.weather.setLiveValue('lightningEnabled',runtime.state.weatherPreset==='Rain_Thunderstorm'&&Number(element.value)>0.01?1:0);
      $(output).textContent=formatNumber(element.value,digits);
    });
  }

  weather.addEventListener('change',()=>{
    runtime.selectWeather(weather.value,Number(transition.value));
    $('#lightning').disabled=(presetById.get(weather.value)?.runtime.lightningEnabled||0)<.5;
  });
  transition.addEventListener('input',()=>{$('#transition-value').textContent=`${Number(transition.value).toFixed(1)} s`;});
  quality.addEventListener('change',()=>{runtime.setQuality(quality.value);renderer.setQuality(quality.value);});
  time.addEventListener('input',()=>{runtime.time.set(Number(time.value));$('#time-value').textContent=formatTime(time.value);});
  timeSpeed.addEventListener('input',()=>{runtime.time.setSpeed(Number(timeSpeed.value));$('#speed-value').textContent=`${Number(timeSpeed.value).toFixed(1)}×`;});
  $('#time-play').addEventListener('click',event=>{runtime.time.setPaused(!runtime.time.paused);event.currentTarget.textContent=runtime.time.paused?'▶ Время':'Ⅱ Пауза';});
  $('#camera-reset').addEventListener('click',()=>renderer.resetCamera());
  $('#reset').addEventListener('click',()=>{
    runtime.reset();renderer.resetCamera();weather.value=runtime.state.initialPreset;quality.value='Medium';transition.value=4;timeSpeed.value=runtime.time.speed;
    $('#transition-value').textContent='4.0 s';$('#speed-value').textContent=`${runtime.time.speed.toFixed(1)}×`;$('#time-play').textContent='▶ Время';$('#lightning').disabled=(presetById.get(runtime.state.initialPreset)?.runtime.lightningEnabled||0)<.5;
  });

  let lastMetrics={fps:0,frameMs:0,quality:'Medium',particles:0},lastSnapshot=null;
  renderer.onMetrics(metrics=>{lastMetrics=metrics;$('#fps').textContent=Math.round(metrics.fps);$('#frame-ms').textContent=metrics.frameMs.toFixed(1);});
  renderer.onState(snapshot=>{
    lastSnapshot=snapshot;time.value=snapshot.time;$('#time-value').textContent=formatTime(snapshot.time);
    const current=snapshot.weather;
    for(const [id,[field,output,digits]] of Object.entries(sliderBindings)){
      const element=$(`#${id}`);if(document.activeElement!==element)element.value=current[field];$(output).textContent=formatNumber(id==='wetness'?snapshot.wetness.value:current[field],digits);
    }
    const status=runtime.weather.status;$('#transition-status').textContent=status.active?`${Math.round(status.alpha*100)}%`:'стабильно';
    const preset=presetById.get(runtime.state.weatherPreset);$('#weather-status').textContent=`${preset?.name||runtime.state.weatherPreset} · ${formatTime(snapshot.time)} · ${snapshot.quality}`;
    const strip=$('#state-strip'),values=[['облака',current.cloudCoverage],['туман',snapshot.fog.density],['дождь',current.rain],['снег',current.snow],['ветер',snapshot.wind.speed],['wet',snapshot.wetness.value],['частицы',snapshot.precipitation.rainCount+snapshot.precipitation.snowCount+snapshot.precipitation.dustCount]];
    strip.replaceChildren(...values.map(([label,value])=>{const chip=document.createElement('span');chip.className='state-chip';const b=document.createElement('b');b.textContent=typeof value==='number'?formatNumber(value,value>10?0:2):value;chip.append(`${label} `,b);return chip;}));
  });

  $('#export-report').addEventListener('click',()=>{
    if(!lastSnapshot)return;
    const preset=presetById.get(runtime.state.weatherPreset),report={
      schema:'solum.environment.visual-report',schemaVersion:1,packageId:packageData.packageId,
      current:{timeOfDay:lastSnapshot.time,weatherPreset:runtime.state.weatherPreset,weather:{...lastSnapshot.weather},wetness:lastSnapshot.wetness.value,lightningEvent:lastSnapshot.lightning.eventIndex,quality:lastSnapshot.quality},
      performance:{...lastMetrics,timing:'requestAnimationFrame CPU wall time'},
      provenance:{presetStatus:preset.status,runtimeProvenance:preset.runtimeProvenance,automaticUdsParityClaim:false},
      resources:{audioAutomatic:false,verifiedAudioPayloads:packageData.resources.audio.length,unavailableTextures:packageData.resources.textures.length},
    };
    download('SOLUM_ENVIRONMENT_VISUAL_REPORT.json',JSON.stringify(report,null,2)+'\n');
  });

  setupAccuracy(packageData);setupAudio(packageData,runtime);setupScenarios(packageData,runtime,renderer,transition);setupTabs();setupPanel();
  $('#lightning').disabled=(presetById.get(runtime.state.initialPreset)?.runtime.lightningEnabled||0)<.5;$('#transition-value').textContent=`${Number(transition.value).toFixed(1)} s`;$('#speed-value').textContent=`${Number(timeSpeed.value).toFixed(1)}×`;$('#time-value').textContent=formatTime(runtime.time.value);
  renderer.start(runtime);
}

try {
  await main();
} catch (error) {
  const fatal=$('#fatal');fatal.hidden=false;fatal.textContent=`P62B runtime остановлен:\n${error.message}`;console.error(error);
}
