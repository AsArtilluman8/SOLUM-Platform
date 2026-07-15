package com.solum.engine.environment.p63;

public final class SolumEnvironmentController {
    private final SolumEnvironmentPackage envPackage;
    private final SolumEnvironmentState state = new SolumEnvironmentState();
    private final SolumTimeSystem time = new SolumTimeSystem();
    private final SolumCelestialSystem celestial = new SolumCelestialSystem();
    private final SolumPrecipitationOcclusion occlusion;
    private SolumWeatherState weatherFrom;
    private SolumWeatherState weatherTarget;
    private float transitionElapsed;
    private int rngState;
    private float nextStrike;
    private float strikeElapsed;
    private float strikeDuration;
    private float cloudPhase;
    private float windPhase;
    private String lastIblSlot = "";
    private float cameraX;
    private float cameraY = 1.6f;
    private float cameraZ;
    private float sunScale = 1.0f, sunDiskScale = 1.0f, moonScale = 1.0f, moonDiskScale = 1.0f;
    private float starBrightness = 1.0f, starDensity = 1.0f, cloudSpeedScale = 1.0f, iblIntensityScale = 1.0f;
    private float manualPuddle = Float.NaN, manualSnowCover = Float.NaN, manualIce = Float.NaN, manualMoonPhase = Float.NaN;
    private long audioLightningEvent = -1L;
    private int sunTintMode, moonTintMode, cloudTintMode;

    public SolumEnvironmentController(SolumEnvironmentPackage envPackage) {
        if (envPackage == null) throw new IllegalArgumentException("environment_package_missing");
        envPackage.validate();
        this.envPackage = envPackage;
        rngState = envPackage.deterministicSeed == 0 ? 1 : envPackage.deterministicSeed;
        time.configure(envPackage.initialTime, envPackage.previewDaySeconds);
        celestial.configure(envPackage.dawn, envPackage.dusk, envPackage.sunIntensity, envPackage.sunDiskIntensity,
            envPackage.moonIntensity, envPackage.moonPhase, envPackage.starsIntensity);
        SolumWeatherState initial = envPackage.findPreset("Partly_Cloudy");
        if (initial == null) throw new IllegalArgumentException("environment_presets_empty");
        weatherFrom = initial.copy(); weatherTarget = initial.copy(); state.weather.set(initial);
        state.packageStatus = "loaded_" + envPackage.packageId + "_presets=" + envPackage.getPresets().size();
        state.timeOfDay = envPackage.initialTime;
        occlusion = new SolumPrecipitationOcclusion(16, 16, 1.0f, -8.0f, -8.0f);
        SolumInteriorExclusionVolume room = new SolumInteriorExclusionVolume("p63_test_room", 2.0f, 0.0f, -2.0f, 7.0f, 4.1f, 3.0f);
        occlusion.addExclusion(room);
        for (int z = 6; z <= 10; z++) for (int x = 10; x <= 15; x++) occlusion.setRoofHeight(x, z, 4.1f);
        update(0.0f);
    }

    public SolumEnvironmentState getState() { return state; }
    public SolumEnvironmentPackage getPackage() { return envPackage; }
    public SolumTimeSystem getTimeSystem() { return time; }
    public SolumPrecipitationOcclusion getOcclusion() { return occlusion; }

    public void selectWeather(String id, float transitionSeconds) {
        SolumWeatherState target = envPackage.findPreset(id);
        if (target == null || !id.equals(target.id)) throw new IllegalArgumentException("unknown_weather_preset_" + id);
        weatherFrom = state.weather.copy(); weatherTarget = target.copy(); transitionElapsed = 0.0f;
        state.requestedPreset = id; state.weatherTransitionDuration = Math.max(0.0f, transitionSeconds);
        state.weatherTransitionActive = state.weatherTransitionDuration > 0.0f;
        state.weatherTransitionAlpha = state.weatherTransitionActive ? 0.0f : 1.0f;
        if (!state.weatherTransitionActive) { state.weather.set(target); state.activePreset = id; }
    }

    public void nextWeather(float transitionSeconds) {
        int index = 0;
        for (int i = 0; i < envPackage.getPresets().size(); i++) if (state.requestedPreset.equals(envPackage.getPresets().get(i).id)) index = i;
        selectWeather(envPackage.getPresets().get((index + 1) % envPackage.getPresets().size()).id, transitionSeconds);
    }

    public void setQuality(String quality) { state.quality = envPackage.findQuality(quality).name; }
    public void setTime(float hundredths) { time.set(hundredths); }
    public void transitionTime(float hundredths, float seconds) { time.transitionTo(hundredths, seconds); }
    public void setTimePaused(boolean paused) { time.setPaused(paused); }
    public void setTimeSpeed(float speed) { time.setSpeed(speed); }
    public void setAudioVolume(float value) { state.audio.masterVolume = clamp(value); }
    public void setAudioMuted(boolean muted) { state.audio.muted = muted; }
    public void setCameraPosition(float x, float y, float z) { cameraX=x; cameraY=y; cameraZ=z; }
    public void triggerLightning() { if (state.weather.lightningEnabled >= 0.5f) beginStrike(); }
    public void setSunScale(float value) { sunScale = clamp(value); }
    public void setSunDiskScale(float value) { sunDiskScale = Math.max(0.25f, Math.min(2.0f, value)); }
    public void setMoonScale(float value) { moonScale = clamp(value); }
    public void setMoonDiskScale(float value) { moonDiskScale = Math.max(0.25f, Math.min(2.0f, value)); }
    public void setStarBrightness(float value) { starBrightness = Math.max(0.0f, Math.min(2.0f, value)); }
    public void setStarDensity(float value) { starDensity = clamp(value); }
    public void setCloudSpeedScale(float value) { cloudSpeedScale = Math.max(0.0f, Math.min(2.0f, value)); }
    public void setIblIntensityScale(float value) { iblIntensityScale = Math.max(0.0f, Math.min(2.0f, value)); }
    public void setManualPuddle(float value) { manualPuddle = clamp(value); }
    public void setManualSnowCover(float value) { manualSnowCover = clamp(value); }
    public void setManualIce(float value) { manualIce = clamp(value); }
    public void setMoonPhase(float value) { manualMoonPhase = clamp(value); }
    public void setSunTintMode(int mode) { sunTintMode = Math.floorMod(mode, 3); }
    public void setMoonTintMode(int mode) { moonTintMode = Math.floorMod(mode, 3); }
    public void setCloudTintMode(int mode) { cloudTintMode = Math.floorMod(mode, 3); }
    public float getSunDiskScale() { return sunDiskScale; }
    public float getMoonDiskScale() { return moonDiskScale; }
    public float getStarDensity() { return starDensity; }
    public float getCameraX() { return cameraX; }
    public float getCameraZ() { return cameraZ; }

    public void resetManualOverrides() {
        sunScale=1;sunDiskScale=1;moonScale=1;moonDiskScale=1;starBrightness=1;starDensity=1;cloudSpeedScale=1;iblIntensityScale=1;
        manualPuddle=Float.NaN;manualSnowCover=Float.NaN;manualIce=Float.NaN;manualMoonPhase=Float.NaN;sunTintMode=0;moonTintMode=0;cloudTintMode=0;
        state.audio.masterVolume=0.45f;state.audio.muted=false;
    }

    public void setWeatherValue(String field, float value) {
        state.weatherTransitionActive = false;
        if ("cloudCoverage".equals(field)) state.weather.cloudCoverage=clamp(value);
        else if ("cloudDensity".equals(field)) state.weather.cloudDensity=clamp(value);
        else if ("fogDensity".equals(field)) state.weather.fogDensity=clamp(value)*0.12f;
        else if ("rain".equals(field)) state.weather.rain=clamp(value);
        else if ("snow".equals(field)) state.weather.snow=clamp(value);
        else if ("windSpeed".equals(field)) state.weather.windSpeed=clamp(value);
        else if ("lightningPotential".equals(field)) { state.weather.lightningPotential=clamp(value); state.weather.lightningEnabled=value>0.01f?1.0f:0.0f; }
        else if ("wetnessTarget".equals(field)) state.weather.wetnessTarget=clamp(value);
        else if ("snowTarget".equals(field)) state.weather.snowTarget=clamp(value);
        state.requestedPreset = "Manual"; state.activePreset = "Manual"; state.weather.id="Manual"; state.weather.name="Manual";
        weatherFrom=state.weather.copy(); weatherTarget=state.weather.copy();
    }

    public SolumEnvironmentState update(float deltaSeconds) {
        float dt = Math.max(0.0f, Math.min(0.1f, deltaSeconds));
        updateWeather(dt);
        state.timeOfDay = time.update(dt);
        celestial.update(state.timeOfDay, state.weather, state.lighting);
        applyCelestialOverrides();
        updateWind(dt); updateClouds(dt); updateAtmosphere(); updateFog(); updatePrecipitation();
        updateLightning(dt); updateSurface(dt); applySurfaceOverrides(); updateAudio(dt); updateIbl();
        state.cameraInside = occlusion.isInterior(cameraX, cameraY, cameraZ);
        state.cameraUnderRoof = occlusion.hasRoofAbove(cameraX, cameraY, cameraZ);
        state.frameRevision++;
        return state;
    }

    private void updateWeather(float dt) {
        if (!state.weatherTransitionActive) return;
        transitionElapsed += dt;
        float alpha = state.weatherTransitionDuration <= 0.0f ? 1.0f : clamp(transitionElapsed / state.weatherTransitionDuration);
        state.weather.interpolate(weatherFrom, weatherTarget, alpha);
        state.weatherTransitionAlpha = alpha;
        if (alpha >= 1.0f) { state.weatherTransitionActive=false; state.weather.set(weatherTarget); state.activePreset=weatherTarget.id; }
    }

    private void updateWind(float dt) {
        windPhase += dt * (0.5f + state.weather.windTurbulence * 2.4f);
        float wave = 0.55f + 0.28f * (float)Math.sin(windPhase) + 0.17f * (float)Math.sin(windPhase*2.37f+1.1f);
        SolumWindState w=state.wind; w.directionDeg=state.weather.windDirectionDeg; w.speed=state.weather.windSpeed;
        w.gust=clamp(state.weather.windGust*wave); w.turbulence=state.weather.windTurbulence; w.phase=windPhase;
        double radians=Math.toRadians(w.directionDeg); float magnitude=w.speed*(0.35f+w.gust*0.85f);
        w.x=(float)Math.sin(radians)*magnitude; w.z=(float)Math.cos(radians)*magnitude;
    }

    private void updateClouds(float dt) {
        SolumCloudState c=state.clouds; cloudPhase+=dt;
        c.coverage=state.weather.cloudCoverage; c.density=state.weather.cloudDensity; c.height=state.weather.cloudHeight; c.thickness=state.weather.cloudThickness;
        c.offsetX += state.wind.x*dt*0.35f*cloudSpeedScale; c.offsetZ += state.wind.z*dt*0.35f*cloudSpeedScale;
        c.lightAttenuation=1.0f-c.coverage*0.68f; c.lightningIllumination=state.lightning.flash;
        float base=0.86f-c.coverage*0.35f-state.weather.dust*0.18f;c.color[0]=base;c.color[1]=base*1.02f;c.color[2]=base*1.08f;
        if(cloudTintMode==1){c.color[0]*=1.08f;c.color[1]*=0.94f;c.color[2]*=0.82f;}else if(cloudTintMode==2){c.color[0]*=0.82f;c.color[1]*=0.94f;c.color[2]*=1.12f;}
        SolumEnvironmentQuality q=envPackage.findQuality(state.quality); c.visibleGroups=q.cloudGroups; c.quality=q.name;
    }

    private void updateAtmosphere() {
        SolumAtmosphereState a=state.atmosphere; float sunHeight=state.lighting.sunElevation;
        float day=clamp((sunHeight+0.08f)/0.24f); float twilight=clamp(1.0f-Math.abs(sunHeight)/0.24f);
        a.rayleigh=0.8f+state.weather.humidity*0.35f; a.mie=0.025f+state.weather.humidity*0.12f+state.weather.dust*0.22f;
        a.horizonScattering=0.25f+a.mie*2.2f; a.twilight=twilight;
        a.skyColor[0]=lerp(0.012f,0.20f,day)+twilight*0.34f; a.skyColor[1]=lerp(0.020f,0.43f,day)+twilight*0.10f; a.skyColor[2]=lerp(0.055f,0.82f,day)-twilight*0.20f;
        float dark=1.0f-state.weather.cloudCoverage*0.55f-state.weather.dust*0.30f;
        for(int i=0;i<3;i++)a.skyColor[i]=Math.max(0.005f,a.skyColor[i]*Math.max(0.18f,dark));
        a.horizonColor[0]=lerp(0.08f,0.68f,day)+twilight*0.24f; a.horizonColor[1]=lerp(0.10f,0.72f,day); a.horizonColor[2]=lerp(0.18f,0.82f,day)-twilight*0.26f;
    }

    private void updateFog() {
        SolumFogState f=state.fog; f.density=Math.min(0.12f,state.weather.fogDensity); f.distance=lerp(145.0f,18.0f,clamp(f.density/0.1055f));
        f.height=-0.5f; f.heightFalloff=state.weather.fogHeightFalloff; f.maximumOpacity=clamp(0.38f+f.density*5.0f);
        float night=1.0f-clamp((state.lighting.sunElevation+0.05f)/0.2f);
        f.color[0]=lerp(0.62f,0.08f,night); f.color[1]=lerp(0.68f,0.10f,night); f.color[2]=lerp(0.72f,0.18f,night);
        if(state.weather.dust>0){f.color[0]=0.48f;f.color[1]=0.31f;f.color[2]=0.16f;}
    }

    private void updatePrecipitation() {
        SolumEnvironmentQuality q=envPackage.findQuality(state.quality); SolumPrecipitationState p=state.precipitation;
        p.rain=state.weather.rain; p.snow=state.weather.snow; p.dust=state.weather.dust; p.particleLimit=q.particleLimit;
        float total=Math.max(0.0001f,p.rain+p.snow+p.dust); int active=(int)(q.particleLimit*clamp(total));
        p.rainParticles=(int)(active*p.rain/total);p.snowParticles=(int)(active*p.snow/total);p.dustParticles=Math.max(0,active-p.rainParticles-p.snowParticles);
        p.windTiltX=state.wind.x;p.windTiltZ=state.wind.z;p.blockedCells=0;p.exposedCells=0;
        for(int z=-3;z<=3;z++)for(int x=-3;x<=3;x++){float wx=cameraX+x*2.0f,wz=cameraZ+z*2.0f;if(occlusion.blocksPrecipitation(wx,1.5f,wz))p.blockedCells++;else p.exposedCells++;}
    }

    private void updateLightning(float dt) {
        SolumLightningState l=state.lightning; l.enabled=state.weather.lightningEnabled>=0.5f&&state.weather.lightningPotential>0.01f;
        if(!l.enabled){l.active=false;l.flash=0;nextStrike=0;return;}
        if(l.active){strikeElapsed+=dt;float alpha=clamp(strikeElapsed/Math.max(0.1f,strikeDuration));float pulse=(float)Math.pow(Math.max(0.0,Math.sin(alpha*Math.PI*7.0)),12.0);l.flash=state.weather.lightningPotential*Math.max((float)Math.exp(-alpha*8.0),pulse*(1.0f-alpha));if(alpha>=1){l.active=false;l.flash=0;}}
        else {if(nextStrike<=0)nextStrike=scheduleStrike();nextStrike-=dt;if(nextStrike<=0)beginStrike();}
        state.lighting.lightningLumens=l.flash*l.lightIntensity*4200.0f;
    }

    private void beginStrike(){SolumLightningState l=state.lightning;l.active=true;l.eventIndex++;strikeElapsed=0;rngState=xorshift(rngState);strikeDuration=lerp(envPackage.lightningDurationMin,envPackage.lightningDurationMax,random());rngState=xorshift(rngState);l.strikeX=(random()-0.5f)*18.0f;rngState=xorshift(rngState);l.strikeZ=-3.0f-random()*14.0f;l.distanceKm=0.25f+random()*2.75f;l.thunderDelaySeconds=l.distanceKm*envPackage.thunderDelayPerKm;nextStrike=scheduleStrike();}
    private float scheduleStrike(){rngState=xorshift(rngState);float average=60.0f/Math.max(0.1f,envPackage.lightningFrequency);return envPackage.lightningSpawnPeriod+average*(0.4f+random()*1.2f);}

    private void updateSurface(float dt){SolumSurfaceWeatherState s=state.surface;float exposure=state.cameraInside||state.cameraUnderRoof?0.0f:1.0f;float target=clamp(Math.max(state.weather.wetnessTarget,state.weather.rain*0.96f))*(1.0f-state.weather.dust*0.82f);float wetRate=(0.35f+state.weather.rain*1.65f)/Math.max(1,envPackage.wetCoverageSeconds);float dryRate=(0.3f+0.7f*clamp(state.lighting.sunElevation)+state.weather.dust*1.4f)/Math.max(1,envPackage.drySeconds);s.wetness=approach(s.wetness,target,dt*(s.wetness<target?wetRate:dryRate));s.exposedWetness=s.wetness;s.coveredWetness=approach(s.coveredWetness,0,dt*dryRate*0.3f);float puddleTarget=clamp((s.wetness-0.45f)*1.8f)*envPackage.puddleCoverage*(0.35f+state.weather.rain);s.puddle=approach(s.puddle,puddleTarget,dt*(state.weather.rain>0?0.06f:0.012f));float snowTarget=clamp(Math.max(state.weather.snowTarget,state.weather.snow*0.9f));s.snowCover=approach(s.snowCover,snowTarget,dt*(state.weather.snow>0?0.025f+state.weather.snow*0.05f:0.008f));s.exposedSnow=s.snowCover;s.interiorSnow=0.0f;s.ice=clamp(Math.min(s.wetness*0.72f,s.snowCover*0.58f+state.weather.snow*0.22f));s.dust=approach(s.dust,state.weather.dust,dt*(state.weather.dust>0?0.05f:0.008f));s.roughnessScale=lerp(1.0f,0.24f,s.wetness);s.reflectionBoost=s.wetness*0.75f+s.puddle*0.6f+s.ice*0.35f;if(exposure==0){s.interiorSnow=0;s.coveredWetness=Math.min(s.coveredWetness,0.08f);}}

    private void updateAudio(float dt){SolumEnvironmentAudioState a=state.audio;float rate=Math.min(1,dt*1.8f);a.rainGain=lerp(a.rainGain,state.weather.rain,rate);a.windGain=lerp(a.windGain,clamp(state.weather.windSpeed+state.weather.windGust*0.3f),rate);a.snowGain=lerp(a.snowGain,state.weather.snow,rate);a.sandGain=lerp(a.sandGain,state.weather.dust*clamp(state.weather.windSpeed+0.2f),rate);float interior=state.cameraInside||state.cameraUnderRoof?0.28f:1.0f;a.interiorAttenuation=lerp(a.interiorAttenuation,interior,Math.min(1,dt*2.5f));a.lowPassMix=1.0f-a.interiorAttenuation;a.activeProfile=state.weather.rain>0.05f?"rain":(state.weather.snow>0.05f?"snow":(state.weather.dust>0.05f?"sand":(state.weather.windSpeed>0.25f?"wind":"calm")));if(state.lightning.eventIndex!=audioLightningEvent){audioLightningEvent=state.lightning.eventIndex;a.thunderPendingSeconds=state.lightning.thunderDelaySeconds;}else if(a.thunderPendingSeconds>=0)a.thunderPendingSeconds-=dt;}

    private void updateIbl(){String slot;if(state.weather.snow>0.12f)slot="snow";else if(state.weather.lightningEnabled>=0.5f)slot="storm";else if(state.weather.rain>0.1f||state.weather.cloudCoverage>0.64f)slot="overcast";else if(state.weather.dust>0.1f)slot="sand";else if(state.lighting.sunElevation<0.03f)slot="night";else if(state.lighting.sunElevation<0.26f)slot="sunset";else slot="day";if(!slot.equals(lastIblSlot)){lastIblSlot=slot;state.lighting.iblSlot=slot;state.lighting.iblRevision++;}state.lighting.iblBlend=(state.weatherTransitionActive?0.58f+Math.abs(state.weatherTransitionAlpha-0.5f)*0.84f:1.0f)*iblIntensityScale;}

    private void applyCelestialOverrides(){SolumEnvironmentLightingState l=state.lighting;if(!Float.isNaN(manualMoonPhase))l.moonPhase=manualMoonPhase;l.sunLux*=sunScale;l.sunDiskBrightness*=sunScale;l.moonLux*=moonScale;l.moonDiskBrightness*=moonScale;l.starVisibility=clamp(l.starVisibility*starBrightness);if(sunTintMode==1){l.sunColor[0]=1;l.sunColor[1]*=0.84f;l.sunColor[2]*=0.62f;}else if(sunTintMode==2){l.sunColor[0]*=0.82f;l.sunColor[1]*=0.92f;l.sunColor[2]=1;}if(moonTintMode==1){l.moonColor[0]=0.68f;l.moonColor[1]=0.72f;l.moonColor[2]=0.78f;}else if(moonTintMode==2){l.moonColor[0]=0.35f;l.moonColor[1]=0.50f;l.moonColor[2]=1.0f;}}
    private void applySurfaceOverrides(){if(!Float.isNaN(manualPuddle))state.surface.puddle=manualPuddle;if(!Float.isNaN(manualSnowCover)){state.surface.snowCover=manualSnowCover;state.surface.exposedSnow=manualSnowCover;}if(!Float.isNaN(manualIce))state.surface.ice=manualIce;state.surface.interiorSnow=0;}

    private float random(){return (rngState&0xffffffffL)/4294967296.0f;}private static int xorshift(int v){v^=v<<13;v^=v>>>17;v^=v<<5;return v;}private static float clamp(float v){return Math.max(0,Math.min(1,v));}private static float lerp(float a,float b,float t){return a+(b-a)*t;}private static float approach(float v,float target,float amount){return v<target?Math.min(target,v+amount):Math.max(target,v-amount);}
}
