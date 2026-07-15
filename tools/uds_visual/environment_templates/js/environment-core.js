export const clamp = (value, low = 0, high = 1) => Math.max(low, Math.min(high, value));
export const lerp = (a, b, alpha) => a + (b - a) * alpha;
export const smoothstep = (low, high, value) => {
  const alpha = clamp((value - low) / Math.max(1e-6, high - low));
  return alpha * alpha * (3 - 2 * alpha);
};

function normalize3(out, x, y, z) {
  const length = Math.hypot(x, y, z) || 1;
  out[0] = x / length; out[1] = y / length; out[2] = z / length;
  return out;
}

function easeInOut(alpha, exponent = 2) {
  const value = clamp(alpha);
  return value < 0.5
    ? 0.5 * Math.pow(value * 2, exponent)
    : 1 - 0.5 * Math.pow((1 - value) * 2, exponent);
}

function angularLerp(a, b, alpha) {
  const delta = ((b - a + 540) % 360) - 180;
  return (a + delta * alpha + 360) % 360;
}

function cloneRuntime(source) {
  return Object.assign({}, source);
}

function color3(value, fallback) {
  const color=value?.value;
  return new Float32Array(color&&typeof color==='object'?[Number(color.r),Number(color.g),Number(color.b)]:fallback);
}

export class DeterministicRng {
  constructor(seed) { this.initialSeed = seed >>> 0 || 1; this.state = this.initialSeed; }
  reset() { this.state = this.initialSeed; }
  next() {
    let value = this.state;
    value ^= value << 13; value ^= value >>> 17; value ^= value << 5;
    this.state = value >>> 0;
    return this.state / 4294967296;
  }
}

export class SolumEnvironmentState {
  constructor(packageData, initialPreset = 'Partly_Cloudy') {
    this.package = packageData;
    this.initialPreset = initialPreset;
    this.weatherPreset = initialPreset;
    this.quality = 'Medium';
    this.timePaused = true;
    this.timeSpeed = Number(packageData.time.speed.value) || 1;
    this.manualRevision = 0;
  }
}

export class SolumTimeSystem {
  constructor(config) {
    this.config = config;
    this.value = Number(config.initial.value);
    this.speed = Number(config.speed.value) || 1;
    this.paused = true;
    this.transition = null;
  }
  static wrap(value) { return ((Number(value) % 2400) + 2400) % 2400; }
  static parts(value) {
    const wrapped = SolumTimeSystem.wrap(value);
    const hours = Math.floor(wrapped / 100);
    const minutes = Math.floor((wrapped - hours * 100) * 0.6 + 1e-6);
    return { value: wrapped, hours, minutes, decimalHours: wrapped / 100 };
  }
  set(value) { this.value = SolumTimeSystem.wrap(value); this.transition = null; }
  setPaused(paused) { this.paused = Boolean(paused); }
  setSpeed(speed) { this.speed = Math.max(0, Number(speed) || 0); }
  transitionTo(value, duration) {
    const target = SolumTimeSystem.wrap(value);
    let delta = target - this.value;
    if (delta > 1200) delta -= 2400;
    if (delta < -1200) delta += 2400;
    this.transition = { from: this.value, delta, elapsed: 0, duration: Math.max(0, Number(duration) || 0) };
    if (this.transition.duration === 0) { this.value = target; this.transition = null; }
  }
  update(deltaSeconds) {
    if (this.transition) {
      this.transition.elapsed += deltaSeconds;
      const alpha = clamp(this.transition.elapsed / this.transition.duration);
      this.value = SolumTimeSystem.wrap(this.transition.from + this.transition.delta * easeInOut(alpha, 2));
      if (alpha >= 1) this.transition = null;
    } else if (!this.paused && this.speed > 0) {
      const daySeconds = Number(this.config.previewDaySeconds.value) || 240;
      this.value = SolumTimeSystem.wrap(this.value + deltaSeconds * 2400 / daySeconds * this.speed);
    }
    return this.value;
  }
}

export class SolumCelestialSystem {
  constructor(config, timeConfig) {
    this.config = config;
    this.dawn = Number(timeConfig.dawn.value);
    this.dusk = Number(timeConfig.dusk.value);
    this.state = {
      sunDirection: new Float32Array(3), moonDirection: new Float32Array(3),
      sunHeight: 0, day: 0, twilight: 0, night: 0,
      moonPhase: Number(config.moonPhase.value), starVisibility: 0,
      sunIntensity: Number(config.sunIntensity.value), moonIntensity: Number(config.moonIntensity.value),
      sunDiskIntensity: Number(config.sunDiskIntensity.value), moonScale: Number(config.moonScale.value),
      starsIntensity: Number(config.starsIntensity.value), starsSpeed: Number(config.starsSpeed.value),
      twinkleAmount: Number(config.twinkleAmount.value), twinkleSpeed: Number(config.twinkleSpeed.value),
      sunColor: color3(config.sunColor,[1,1,1]), moonColor: color3(config.moonColor,[.45,.56,.86]),
    };
  }
  update(timeValue, cloudCoverage) {
    const span=Math.max(1,this.dusk-this.dawn);
    const angle=(SolumTimeSystem.wrap(timeValue)-this.dawn)/span*Math.PI;
    const sx = Math.cos(angle) * 0.9, sy = Math.sin(angle), sz = Math.sin(angle * 0.71) * 0.32;
    normalize3(this.state.sunDirection, sx, sy, sz);
    normalize3(this.state.moonDirection, -sx, -sy, -sz);
    this.state.sunHeight = this.state.sunDirection[1];
    this.state.day = smoothstep(-0.08, 0.16, this.state.sunHeight);
    this.state.twilight = (1 - smoothstep(0.05, 0.34, Math.abs(this.state.sunHeight))) * smoothstep(-0.22, 0.02, this.state.sunHeight);
    this.state.night = 1 - this.state.day;
    this.state.starVisibility = clamp(this.state.night * (1 - cloudCoverage * 0.78));
    return this.state;
  }
}

export class SolumWeatherController {
  constructor(packageData, initialPreset) {
    this.presets = new Map(packageData.weatherPresets.map(item => [item.id, item]));
    const preset = this.presets.get(initialPreset) || packageData.weatherPresets[0];
    this.currentPresetId = preset.id;
    this.current = cloneRuntime(preset.runtime);
    this.from = cloneRuntime(preset.runtime);
    this.target = cloneRuntime(preset.runtime);
    this.fields=Object.keys(preset.runtime);
    this.transition = null;
    this.status = { active: false, alpha: 1, elapsed: 0, duration: 0, target: preset.id };
  }
  select(presetId, durationSeconds = 4) {
    const preset = this.presets.get(presetId);
    if (!preset) throw new Error(`Unknown weather preset: ${presetId}`);
    Object.assign(this.from, this.current);
    Object.assign(this.target, preset.runtime);
    this.currentPresetId = presetId;
    const duration = Math.max(0, Number(durationSeconds) || 0);
    this.transition = duration > 0 ? { elapsed: 0, duration } : null;
    Object.assign(this.status, { active: Boolean(this.transition), alpha: this.transition ? 0 : 1, elapsed: 0, duration, target: presetId });
    if (!this.transition) Object.assign(this.current, this.target);
  }
  setLiveValue(name, value) {
    if (!(name in this.current)) return;
    this.transition = null;
    this.current[name] = Number(value);
    this.target[name] = Number(value);
    Object.assign(this.status, { active: false, alpha: 1, elapsed: 0, duration: 0 });
  }
  update(deltaSeconds) {
    if (!this.transition) return this.current;
    this.transition.elapsed += deltaSeconds;
    const raw = clamp(this.transition.elapsed / this.transition.duration);
    const alpha = easeInOut(raw, 2);
    for (const name of this.fields) {
      const targetValue=this.target[name];
      const fromValue = this.from[name];
      if (typeof targetValue === 'number' && typeof fromValue === 'number') {
        this.current[name] = name === 'windDirectionDeg'
          ? angularLerp(fromValue, targetValue, alpha)
          : lerp(fromValue, targetValue, alpha);
      } else {
        this.current[name] = raw < 0.5 ? fromValue : targetValue;
      }
    }
    Object.assign(this.status, { active: raw < 1, alpha: raw, elapsed: this.transition.elapsed });
    if (raw >= 1) { this.transition = null; Object.assign(this.current, this.target); }
    return this.current;
  }
}

export class SolumAtmosphereSystem {
  constructor() { this.state = { haze: 0, absorption: 0, humidity: 0, day: 0, twilight: 0 }; }
  update(weather, celestial) {
    this.state.haze = weather.atmosphereHaze;
    this.state.absorption = weather.atmosphereAbsorption;
    this.state.humidity = weather.humidity;
    this.state.day = celestial.day;
    this.state.twilight = celestial.twilight;
    return this.state;
  }
}

export class SolumCloudSystem {
  constructor(config) { this.config = config; this.state = { coverage: 0, density: 0, height: 15, thickness: 0, profile: new Float32Array(3), offsetX: 0, offsetZ: 0 }; }
  update(deltaSeconds, weather, wind) {
    const speed = Number(this.config.speed.value) || 0.35;
    this.state.coverage = weather.cloudCoverage;
    this.state.density = weather.cloudDensity;
    this.state.height = weather.cloudHeight;
    this.state.thickness = weather.cloudThickness;
    this.state.profile[0]=weather.cloudProfileLow;this.state.profile[1]=weather.cloudProfileMid;this.state.profile[2]=weather.cloudProfileHigh;
    this.state.offsetX = (this.state.offsetX + wind.vector[0] * speed * deltaSeconds * 0.025) % 1000;
    this.state.offsetZ = (this.state.offsetZ + wind.vector[2] * speed * deltaSeconds * 0.025) % 1000;
    return this.state;
  }
}

export class SolumFogSystem {
  constructor() { this.state = { density: 0, heightFalloff: 0.065, colorHumidity: 0 }; }
  update(weather) {
    this.state.density = weather.fogDensity;
    this.state.heightFalloff = weather.fogHeightFalloff;
    this.state.colorHumidity = weather.humidity;
    return this.state;
  }
}

export class SolumWindSystem {
  constructor(config) {
    this.config = config;
    this.gustSpeed=Math.max(0.01,Number(config.gustSpeed.value));
    this.phase = 0;
    this.state = { directionDeg: 180, speed: 0, gust: 0, turbulence: 0, vector: new Float32Array(3) };
  }
  update(deltaSeconds, weather) {
    this.phase += deltaSeconds * this.gustSpeed * (1.1 + weather.windTurbulence * 3.4);
    const gustWave = 0.55 + 0.28 * Math.sin(this.phase) + 0.17 * Math.sin(this.phase * 2.37 + 1.1);
    this.state.directionDeg = weather.windDirectionDeg;
    this.state.speed = weather.windSpeed;
    this.state.gust = clamp(weather.windGust * gustWave);
    this.state.turbulence = weather.windTurbulence;
    const radians = weather.windDirectionDeg * Math.PI / 180;
    const magnitude = weather.windSpeed * (0.35 + this.state.gust * 0.85);
    this.state.vector[0] = Math.sin(radians) * magnitude;
    this.state.vector[1] = 0;
    this.state.vector[2] = Math.cos(radians) * magnitude;
    return this.state;
  }
}

export class SolumPrecipitationSystem {
  constructor(config, qualityTiers) {
    this.config = config; this.qualityTiers = qualityTiers;
    const rainSpawn=Math.max(1,Number(config.rainSpawnSource.value)),snowSpawn=Math.max(1,Number(config.snowSpawnSource.value)),spawnReference=Math.max(rainSpawn,snowSpawn);
    this.rainSpawnWeight=rainSpawn/spawnReference;this.snowSpawnWeight=snowSpawn/spawnReference;
    this.state = {
      rain: 0, snow: 0, dust: 0, rainCount: 0, snowCount: 0, dustCount: 0, particleLimit: 0,
      rainWindScale:Number(config.rainWindVelocity.value)/1800,snowWindScale:Number(config.snowWindVelocity.value)/1800,
      rainAlpha:clamp(Number(config.rainAlpha.value)),snowAlpha:clamp(Number(config.snowAlpha.value)),
      rainScale:Math.max(.1,Number(config.rainScale.value)),snowScale:Math.max(.1,Number(config.snowScale.value)),
      rainVelocityRandomization:clamp(Number(config.rainVelocityRandomization.value)),snowVelocityRandomization:clamp(Number(config.snowVelocityRandomization.value)),
    };
  }
  update(weather, quality) {
    const limit = this.qualityTiers[quality].particleLimit;
    this.state.rain = weather.rain; this.state.snow = weather.snow; this.state.dust = weather.dust;
    this.state.particleLimit = limit;
    const rainWeight=weather.rain*this.rainSpawnWeight,snowWeight=weather.snow*this.snowSpawnWeight,dustWeight=weather.dust;
    const total = Math.max(1e-6, rainWeight + snowWeight + dustWeight);
    const active = Math.floor(limit * clamp(total));
    this.state.rainCount = Math.floor(active * rainWeight / total);
    this.state.snowCount = Math.floor(active * snowWeight / total);
    this.state.dustCount = Math.max(0, active - this.state.rainCount - this.state.snowCount);
    return this.state;
  }
}

export class SolumLightningSystem {
  constructor(config) {
    this.config = config;
    this.rng = new DeterministicRng(Number(config.seed.value));
    this.durationRange = config.flashDurationRange.value.map(Number);
    this.lightIntensity=Number(config.lightIntensity.value);
    this.lightColor=color3(config.lightColor,[.5,.62,1]);
    this.frequency = Math.max(0.1, Number(config.frequency.value));
    this.spawnPeriod = Math.max(0.1, Number(config.spawnPeriod.value));
    this.timingRandomization=clamp(Number(config.timingRandomization.value));
    this.nextStrike = 0;
    this.elapsed = 0;
    this.duration = 0;
    this.bolt = new Float32Array(3 * 18);
    this.state = { enabled: false, flash: 0, active: false, bolt: this.bolt, boltPoints: 0, eventIndex: 0, lightIntensity:this.lightIntensity, lightColor:this.lightColor };
  }
  reset() { this.rng.reset(); this.nextStrike = 0; this.elapsed = 0; this.state.eventIndex = 0; this.state.flash = 0; this.state.active = false; }
  schedule() {
    const average = 60 / this.frequency;
    this.nextStrike = this.spawnPeriod + average * (1 + (this.rng.next() - .5) * 1.2 * this.timingRandomization);
  }
  generateBolt() {
    const points = 14;
    const baseX = (this.rng.next() - 0.5) * 13;
    const baseZ = -2 - this.rng.next() * 10;
    let x = baseX, z = baseZ;
    for (let index = 0; index < points; index++) {
      const alpha = index / (points - 1);
      if (index > 0 && index < points - 1) {
        x += (this.rng.next() - 0.5) * 1.25;
        z += (this.rng.next() - 0.5) * 0.65;
      }
      const offset = index * 3;
      this.bolt[offset] = x; this.bolt[offset + 1] = 12.5 * (1 - alpha) + 0.12; this.bolt[offset + 2] = z;
    }
    this.state.boltPoints = points;
  }
  strike() {
    this.duration = lerp(this.durationRange[0], this.durationRange[1], this.rng.next());
    this.elapsed = 0;
    this.state.active = true;
    this.state.eventIndex += 1;
    this.generateBolt();
    this.schedule();
  }
  update(deltaSeconds, weather) {
    this.state.enabled = weather.lightningEnabled >= 0.5 && weather.lightningPotential > 0.01;
    if (!this.state.enabled) {
      this.state.flash = 0; this.state.active = false; this.nextStrike = 0;
      return this.state;
    }
    if (this.nextStrike <= 0 && !this.state.active) this.schedule();
    if (this.state.active) {
      this.elapsed += deltaSeconds;
      const alpha = clamp(this.elapsed / this.duration);
      const pulse = Math.pow(Math.max(0, Math.sin(alpha * Math.PI * 7)), 12);
      this.state.flash = weather.lightningPotential * Math.max(Math.exp(-alpha * 8), pulse * (1 - alpha));
      if (alpha >= 1) { this.state.active = false; this.state.flash = 0; }
    } else {
      this.nextStrike -= deltaSeconds;
      if (this.nextStrike <= 0) this.strike();
    }
    return this.state;
  }
}

export class SolumWetnessSystem {
  constructor(config) {
    this.config = config;
    this.value = 0;
    this.state = { value: 0, target: 0, accumulationRate: 0, dryingRate: 0 };
  }
  reset(value = 0) { this.value = clamp(value); }
  update(deltaSeconds, weather, celestial) {
    const target = clamp(Math.max(weather.wetnessTarget, weather.rain * 0.96, weather.humidity * 0.22));
    const coverageDuration = Math.max(1, Number(this.config.coverageDuration.value));
    const dryDuration = Math.max(1, Number(this.config.dryDuration.value));
    const sunlightSpeed = Number(this.config.drySpeedSun.value);
    const shadeSpeed = Number(this.config.drySpeedShade.value);
    const accumulationRate = 1 / coverageDuration * (0.35 + weather.rain * 1.65);
    const dryingScale = lerp(shadeSpeed, sunlightSpeed, celestial.day * (1 - weather.cloudCoverage));
    const dryingRate = 1 / dryDuration * dryingScale;
    if (this.value < target) this.value = Math.min(target, this.value + deltaSeconds * accumulationRate);
    else this.value = Math.max(target, this.value - deltaSeconds * dryingRate);
    this.state.value = this.value; this.state.target = target;
    this.state.accumulationRate = accumulationRate; this.state.dryingRate = dryingRate;
    return this.state;
  }
}

export class SolumEnvironmentAudioSystem {
  constructor(config, resources) {
    this.config = config;
    this.resources = new Map(resources.map(item => [item.id, item]));
    this.active = null;
  }
  stop() {
    if (this.active) { this.active.pause(); this.active.currentTime = 0; this.active = null; }
  }
  async playManual(id) {
    const resource = this.resources.get(id);
    if (!resource) throw new Error(`Unknown audio payload: ${id}`);
    this.stop();
    const audio = new Audio(resource.path);
    audio.volume = 0.7;
    audio.addEventListener('ended', () => { if (this.active === audio) this.active = null; }, { once: true });
    this.active = audio;
    await audio.play();
  }
}

export class SolumEnvironmentLightingState {
  constructor() { this.state = { sun: 0, moon: 0, ambient: 0, exposure: 1, flash: 0 }; }
  update(weather, celestial, lightning) {
    this.state.sun = celestial.day * weather.lightingScale * celestial.sunIntensity;
    this.state.moon = celestial.night * weather.lightingScale * celestial.moonIntensity;
    this.state.ambient = weather.ambientScale * lerp(0.24, 1, celestial.day);
    this.state.exposure = weather.exposure;
    this.state.flash = lightning.flash * lightning.lightIntensity;
    return this.state;
  }
}

export class SolumEnvironmentRuntime {
  constructor(packageData) {
    this.package = packageData;
    this.state = new SolumEnvironmentState(packageData);
    this.time = new SolumTimeSystem(packageData.time);
    this.celestial = new SolumCelestialSystem(packageData.celestial,packageData.time);
    this.weather = new SolumWeatherController(packageData, this.state.initialPreset);
    this.atmosphere = new SolumAtmosphereSystem(packageData.atmosphere);
    this.clouds = new SolumCloudSystem(packageData.clouds);
    this.fog = new SolumFogSystem(packageData.fog);
    this.wind = new SolumWindSystem(packageData.wind);
    this.precipitation = new SolumPrecipitationSystem(packageData.precipitation, packageData.qualityTiers);
    this.lightning = new SolumLightningSystem(packageData.lightning);
    this.wetness = new SolumWetnessSystem(packageData.wetness);
    this.audio = new SolumEnvironmentAudioSystem(packageData.audio, packageData.resources.audio);
    this.lighting = new SolumEnvironmentLightingState();
    this.snapshot = {
      weather: this.weather.current, time: this.time.value, celestial: this.celestial.state,
      atmosphere: this.atmosphere.state, clouds: this.clouds.state, fog: this.fog.state,
      wind: this.wind.state, precipitation: this.precipitation.state,
      lightning: this.lightning.state, wetness: this.wetness.state, lighting: this.lighting.state,
      quality: this.state.quality,
    };
  }
  reset() {
    this.state.weatherPreset = this.state.initialPreset;
    this.state.quality = 'Medium';
    this.time.set(Number(this.package.time.initial.value));
    this.time.setSpeed(Number(this.package.time.speed.value));
    this.time.setPaused(true);
    this.weather.select(this.state.initialPreset, 0);
    this.lightning.reset(); this.wetness.reset(0); this.audio.stop();
  }
  setQuality(name) {
    if (!this.package.qualityTiers[name]) throw new Error(`Unknown quality tier: ${name}`);
    this.state.quality = name;
  }
  selectWeather(id, duration) { this.state.weatherPreset = id; this.weather.select(id, duration); }
  update(deltaSeconds) {
    const dt = Math.max(0, Math.min(0.05, deltaSeconds));
    this.snapshot.time = this.time.update(dt);
    this.snapshot.weather = this.weather.update(dt);
    this.snapshot.celestial = this.celestial.update(this.snapshot.time, this.snapshot.weather.cloudCoverage);
    this.snapshot.wind = this.wind.update(dt, this.snapshot.weather);
    this.snapshot.clouds = this.clouds.update(dt, this.snapshot.weather, this.snapshot.wind);
    this.snapshot.fog = this.fog.update(this.snapshot.weather);
    this.snapshot.precipitation = this.precipitation.update(this.snapshot.weather, this.state.quality);
    this.snapshot.lightning = this.lightning.update(dt, this.snapshot.weather);
    this.snapshot.wetness = this.wetness.update(dt, this.snapshot.weather, this.snapshot.celestial);
    this.snapshot.atmosphere = this.atmosphere.update(this.snapshot.weather, this.snapshot.celestial);
    this.snapshot.lighting = this.lighting.update(this.snapshot.weather, this.snapshot.celestial, this.snapshot.lightning);
    this.snapshot.quality = this.state.quality;
    return this.snapshot;
  }
}
