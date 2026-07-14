import { DiagnosticRenderer } from './renderer.js';

const $ = selector => document.querySelector(selector);
const statusRu = value => ({
  VERIFIED:'ПОДТВЕРЖДЕНО', PARTIAL:'ЧАСТИЧНО', UNKNOWN:'НЕИЗВЕСТНО',
  UNSUPPORTED_HTML:'НЕ ПОДДЕРЖИВАЕТСЯ В HTML', STATE_ONLY:'ЧАСТИЧНО',
  SOURCE_VERIFIED_BROWSER_ADAPTER:'ЧАСТИЧНО'
}[value] || value);

function formatTime(value) {
  const normalized = ((Number(value) % 2400) + 2400) % 2400;
  const hours = Math.floor(normalized / 100), minutes = Math.round((normalized - hours * 100) * .6) % 60;
  return `${String(hours).padStart(2,'0')}:${String(minutes).padStart(2,'0')} · ${Number(value).toFixed(0)}`;
}

function scalar(value) {
  if (typeof value === 'number') return Number.isInteger(value) ? String(value) : value.toFixed(3).replace(/0+$/,'').replace(/\.$/,'');
  if (typeof value === 'boolean') return value ? 'true' : 'false';
  if (typeof value === 'string') return value;
  if (value && typeof value === 'object' && typeof value.source === 'string') return value.source;
  return JSON.stringify(value);
}

function download(name, text, type) {
  const url = URL.createObjectURL(new Blob([text], { type })), link = document.createElement('a');
  link.href = url; link.download = name; link.click(); setTimeout(() => URL.revokeObjectURL(url), 0);
}

async function loadJson(path) {
  const response = await fetch(path, { cache:'no-store' });
  if (!response.ok) throw new Error(`${path}: HTTP ${response.status}`);
  return response.json();
}

async function main() {
  const [contract, evidence, capabilities, assets, gate] = await Promise.all([
    loadJson('data/UDS_VISUAL_CONTRACT.json'), loadJson('data/UDS_VISUAL_EVIDENCE.json'),
    loadJson('data/UDS_VISUAL_CAPABILITIES.json'), loadJson('data/UDS_VISUAL_ASSET_MANIFEST.json'),
    loadJson('reports/VISUAL_HTML_GATE.json')
  ]);
  const renderer = await DiagnosticRenderer.create($('#viewport'));
  const parameters = evidence.parameters, presets = contract.source_truth.weather_presets;
  const findParameter = name => parameters.find(item => item.name === name);
  const timeParameter = findParameter('Time of Day');
  const rangeMeta = evidence.blueprint_variable_metadata.find(item => item.name === 'Time of Day');
  if (!timeParameter || !rangeMeta?.metadata?.UIMin || !rangeMeta?.metadata?.UIMax) throw new Error('Time of Day evidence отсутствует');
  const time = $('#time');
  time.min = rangeMeta.metadata.UIMin; time.max = rangeMeta.metadata.UIMax;
  time.value = timeParameter.default; time.disabled = false;
  $('#time-note').textContent = rangeMeta?.metadata?.tooltip || 'Точное source state; визуальная небесная связь заблокирована.';
  const weather = $('#weather');
  presets.forEach(item => { const option = document.createElement('option'); option.value = item.id; option.textContent = item.name; weather.append(option); });
  const initialWeather = presets.find(item => item.id === 'Partly_Cloudy');
  if (!initialWeather) throw new Error('Partly_Cloudy evidence отсутствует');
  weather.value = initialWeather.id; weather.disabled = false;

  const defaultState = Object.freeze({ timeOfDay:Number(time.value), weatherPreset:weather.value });
  const state = { ...defaultState };
  function selectedPreset() { return presets.find(item => item.id === state.weatherPreset); }
  function report() {
    return {
      schema_version:contract.schema_version,
      current_state:{ ...state },
      computed_parameters:selectedPreset()?.values || {},
      accuracy:capabilities.systems,
      gate:{ status:gate.status, automatic_visual_equivalence_claim:false },
      assets:assets.assets.map(item => ({ id:item.id, output_sha256:item.output_sha256, browser_active:item.browser_active })),
      map:{ gate:contract.map_gate.status, demo_map_rendered:false }
    };
  }
  function renderState() {
    $('#time-value').textContent = formatTime(state.timeOfDay); time.value = state.timeOfDay; weather.value = state.weatherPreset;
    const values = selectedPreset()?.values || {};
    $('#state-values').replaceChildren(...Object.entries(values).filter(([,value]) => ['number','boolean','string'].includes(typeof value)).map(([name,value]) => {
      const div = document.createElement('div'); div.className = 'value-item';
      const title = document.createElement('b'), output = document.createElement('span'); title.textContent = name; output.textContent = scalar(value); div.append(title,output); return div;
    }));
    $('#state-json').textContent = JSON.stringify(report(), null, 2);
  }
  time.addEventListener('input', () => { state.timeOfDay = Number(time.value); renderState(); });
  weather.addEventListener('change', () => { state.weatherPreset = weather.value; renderState(); });

  const systems = $('#system-controls');
  capabilities.controls.filter(item => !['time_of_day','weather_preset','camera','panel','reset','export'].includes(item.id)).forEach(item => {
    const button = document.createElement('button'); button.type = 'button'; button.disabled = !item.active;
    button.textContent = `${item.id} · ${item.active ? 'ON' : 'LOCK'}`; button.title = item.reason; systems.append(button);
  });

  const accuracy = $('#accuracy-list');
  capabilities.systems.forEach(item => {
    const card = document.createElement('article'); card.className = 'accuracy-card';
    const title = document.createElement('h3'); title.innerHTML = `<span></span><em class="status"></em>`;
    title.querySelector('span').textContent = item.system; title.querySelector('em').textContent = statusRu(item.evidence_status);
    const rows = document.createElement('div'); rows.className = 'truth-grid';
    const facts = [
      ['Источник найден', item.source_found ? 'ПОДТВЕРЖДЕНО' : 'НЕИЗВЕСТНО'],
      ['Значения извлечены', item.values_extracted ? 'ПОДТВЕРЖДЕНО' : 'НЕИЗВЕСТНО'],
      ['Логика извлечена', statusRu(item.logic_extracted)],
      ['Ресурсы извлечены', item.resources_extracted ? 'ПОДТВЕРЖДЕНО' : 'НЕИЗВЕСТНО'],
      ['WebGL-адаптер', statusRu(item.webgl_adapter)],
      ['Визуально проверено', item.visually_verified ? 'ПОДТВЕРЖДЕНО' : 'НЕ ПРОВЕРЕНО ВИЗУАЛЬНО']
    ];
    facts.forEach(([label,value]) => { const a=document.createElement('span'), b=document.createElement('b'); a.textContent=label; b.textContent=value; rows.append(a,b); });
    const blocker = document.createElement('p'); blocker.className='blocker'; blocker.textContent=item.limitations;
    card.append(title,rows,blocker); accuracy.append(card);
  });

  contract.scenarios.forEach(item => {
    const card=document.createElement('article'); card.className='scenario-card';
    const title=document.createElement('h3'), stateText=document.createElement('pre'), blocker=document.createElement('p');
    title.textContent=item.id; stateText.textContent=JSON.stringify(item.input_state,null,2); blocker.className='blocker'; blocker.textContent=item.blockers.join('; ');
    card.append(title,stateText,blocker); $('#scenario-list').append(card);
  });
  $('#summary').innerHTML = `<div class="truth-grid"><span>Root packages</span><b>${evidence.roots.length}</b><span>Decoded parameters</span><b>${parameters.length}</b><span>Weather presets</span><b>${presets.length}</b><span>Curves / keys</span><b>${evidence.curves.length} / ${evidence.curves.reduce((n,item)=>n+item.total_key_count,0)}</b><span>Material contracts / MPC</span><b>${evidence.material_contracts.length} / ${evidence.material_parameter_collections.length}</b><span>Niagara contracts</span><b>${evidence.niagara_contracts.length}</b><span>Exact audio payloads</span><b>${assets.assets.length}</b><span>Exact texture payloads</span><b>${assets.texture_assets.length}</b></div>`;

  const weatherMpc = evidence.material_parameter_collections.find(item => item.source_package?.endsWith('/UltraDynamicWeather_Parameters'));
  const vector = weatherMpc?.vector_parameters?.find(item => item.name?.value === 'Sun Vector')?.default_value?.value;
  const sunColorValue = findParameter('Sun Light Color')?.default;
  const sunIntensityValue = findParameter('Sun Light Intensity')?.default;
  if (!vector || !sunColorValue || typeof sunIntensityValue !== 'number') throw new Error('Sun adapter evidence отсутствует');
  const sunIntensity = Number(sunIntensityValue);
  const direction = [vector.r,vector.b,-vector.g];
  const color = [sunColorValue.r,sunColorValue.g,sunColorValue.b];
  renderer.setSourceLight(direction,color,sunIntensity);

  $('#gate-chip').textContent = `VISUAL GATE ${gate.status}`;
  $('#reset').addEventListener('click', () => { Object.assign(state,defaultState); renderer.reset(); renderState(); });
  $('#export-json').addEventListener('click', () => download('UDS_CURRENT_TRUTH.json', JSON.stringify(report(),null,2)+'\n', 'application/json'));
  $('#export-text').addEventListener('click', () => {
    const r=report(), lines=[`UDS P62 · ${r.gate.status}`,`Time: ${formatTime(r.current_state.timeOfDay)}`,`Weather: ${r.current_state.weatherPreset}`,'',...r.accuracy.map(item=>`${item.system}: ${statusRu(item.evidence_status)} — ${item.limitations}`)];
    download('UDS_CURRENT_TRUTH.txt',lines.join('\n')+'\n','text/plain');
  });
  $('#tabs').addEventListener('click', event => {
    const button=event.target.closest('button[data-tab]'); if(!button)return;
    document.querySelectorAll('#tabs button').forEach(item=>item.classList.toggle('active',item===button));
    document.querySelectorAll('.page').forEach(item=>item.classList.toggle('active',item.dataset.page===button.dataset.tab));
  });
  $('#panel-toggle').addEventListener('click', event => {
    const collapsed=$('#panel').classList.toggle('collapsed'); event.currentTarget.classList.toggle('collapsed',collapsed);
    event.currentTarget.textContent=collapsed?'Показать панель':'Скрыть панель'; event.currentTarget.setAttribute('aria-expanded',String(!collapsed));
  });
  renderState();
}

main().catch(error => { const fatal=$('#fatal'); fatal.hidden=false; fatal.textContent=`P62 preview остановлен: ${error.message}`; console.error(error); });
