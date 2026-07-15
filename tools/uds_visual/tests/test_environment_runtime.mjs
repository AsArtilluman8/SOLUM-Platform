import fs from 'node:fs';
import assert from 'node:assert/strict';
import {
  SolumTimeSystem,
  SolumCelestialSystem,
  SolumWeatherController,
  SolumWindSystem,
  SolumCloudSystem,
  SolumWetnessSystem,
  SolumPrecipitationSystem,
  SolumLightningSystem,
} from '../environment_templates/js/environment-core.js';

const packagePath=process.argv[2];
if(!packagePath)throw new Error('package path argument is required');
const data=JSON.parse(fs.readFileSync(packagePath,'utf8'));

assert.equal(data.weatherPresets.length,13,'13 presets');
assert.equal(SolumTimeSystem.wrap(2401),1,'time wraps above midnight');
assert.equal(SolumTimeSystem.wrap(-1),2399,'time wraps below midnight');

const celestial=new SolumCelestialSystem(data.celestial,data.time);
assert.ok(celestial.update(1200,0).day>.95,'noon is day');
assert.ok(celestial.update(0,0).night>.95,'midnight is night');
assert.equal(celestial.state.starsIntensity,data.celestial.starsIntensity.value,'verified stars intensity is active');

const weather=new SolumWeatherController(data,'Clear_Skies');
weather.select('Rain',4);
weather.update(2);
assert.ok(weather.current.rain>0&&weather.current.rain<data.weatherPresets.find(item=>item.id==='Rain').runtime.rain,'weather transition interpolates');
weather.update(2);
assert.equal(weather.current.rain,data.weatherPresets.find(item=>item.id==='Rain').runtime.rain,'weather transition reaches target');
assert.equal(weather.current.lightningEnabled,0,'non-storm preset does not enable lightning');

const wind=new SolumWindSystem(data.wind),clouds=new SolumCloudSystem(data.clouds);
const windState=wind.update(1,weather.current),before=clouds.state.offsetX;
clouds.update(1,weather.current,windState);
assert.notEqual(clouds.state.offsetX,before,'wind propagates to clouds');
assert.ok(Math.hypot(windState.vector[0],windState.vector[2])>0,'wind vector is active');
assert.equal(clouds.state.profile[1],weather.current.cloudProfileMid,'verified cloud curve profile is active');

const wetness=new SolumWetnessSystem(data.wetness);
const rainState={...weather.current,rain:1,wetnessTarget:1,humidity:1,cloudCoverage:1};
const day={day:1};
for(let index=0;index<60;index++)wetness.update(0.5,rainState,day);
const wet=wetness.value;
assert.ok(wet>0.5,'wetness accumulates');
const dryState={...rainState,rain:0,wetnessTarget:0,humidity:0,cloudCoverage:0};
for(let index=0;index<240;index++)wetness.update(0.5,dryState,day);
assert.ok(wetness.value<wet,'wetness dries');

const precipitation=new SolumPrecipitationSystem(data.precipitation,data.qualityTiers);
for(const tier of ['Low','Medium','High']){
  const state=precipitation.update({...rainState,rain:1,snow:1,dust:1},tier);
  assert.ok(state.rainCount+state.snowCount+state.dustCount<=data.qualityTiers[tier].particleLimit,`${tier} particle limit`);
}

const lightningA=new SolumLightningSystem(data.lightning),lightningB=new SolumLightningSystem(data.lightning);
const storm={lightningEnabled:1,lightningPotential:1};
for(let index=0;index<900;index++){lightningA.update(0.05,storm);lightningB.update(0.05,storm);}
assert.ok(lightningA.state.eventIndex>0,'lightning produces events');
assert.equal(lightningA.state.eventIndex,lightningB.state.eventIndex,'lightning event count deterministic');
assert.deepEqual(Array.from(lightningA.bolt),Array.from(lightningB.bolt),'lightning bolt deterministic');

console.log(JSON.stringify({status:'PASS',presets:13,lightningEvents:lightningA.state.eventIndex,wetnessPeak:wet}));
