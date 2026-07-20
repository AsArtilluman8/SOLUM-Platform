package com.solum.engine.environment.p63;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.nio.ByteBuffer;

public final class SolumEnvironmentCoreTest {
    private static final String[] IDS = {
        "Clear_Skies", "Cloudy", "Foggy", "Overcast", "Partly_Cloudy", "Rain", "Rain_Light",
        "Rain_Thunderstorm", "Sand_Dust_Calm", "Sand_Dust_Storm", "Snow", "Snow_Blizzard", "Snow_Light"
    };

    public static void main(String[] args) {
        SolumEnvironmentPackage env = fixture();
        env.validate();
        require(env.getPresets().size() == 13, "13 presets");
        require(SolumTimeSystem.wrap(2401.0f) == 1.0f, "time wraps above midnight");
        require(SolumTimeSystem.wrap(-1.0f) == 2399.0f, "time wraps below midnight");
        require(close(SolumTimeSystem.fromClock(17, 30), 1750.0f)
            && SolumTimeSystem.clockHour(1750.0f) == 17
            && SolumTimeSystem.clockMinute(1750.0f) == 30, "exact game clock conversion");
        SolumTimeSystem pacedTime = new SolumTimeSystem();
        pacedTime.set(1200.0f); pacedTime.setPaused(false); pacedTime.setDayLengthMinutes(60.0f);
        for (int i = 0; i < 100; i++) pacedTime.update(0.1f);
        require(close(pacedTime.getTime(), 1206.666f, 0.02f), "60 real minute game-day pacing");

        SolumEnvironmentController controller = new SolumEnvironmentController(env);
        for (String id : IDS) {
            controller.selectWeather(id, 0.0f); controller.update(0.1f);
            require(controller.getState().activePreset.equals(id), "preset applies: " + id);
        }
        controller.selectWeather("Partly_Cloudy", 0.0f);
        controller.selectWeather("Rain", 2.0f);
        for (int i = 0; i < 10; i++) controller.update(0.1f);
        require(controller.getState().weather.rain > 0.0f && controller.getState().weather.rain < 0.82f, "smooth weather midpoint");
        for (int i = 0; i < 11; i++) controller.update(0.1f);
        require(close(controller.getState().weather.rain, 0.82f), "weather reaches target");

        controller.setTime(1200.0f); controller.update(0.1f);
        require(controller.getState().lighting.sunLux > 1.0f, "real sun state active at noon");
        controller.setTime(0.0f); controller.update(0.1f);
        require(controller.getState().lighting.moonLux > 0.0f, "separate moon light state active at night");
        require(controller.getState().lighting.starVisibility > 0.1f, "stars visible at night");

        SolumStarCatalog starsA = new SolumStarCatalog(env.deterministicSeed, 192);
        SolumStarCatalog starsB = new SolumStarCatalog(env.deterministicSeed, 192);
        require(starsA.fingerprint() == starsB.fingerprint(), "deterministic world-space star catalog");
        require(starsA.getStars().get(0).size > 0.0f, "stars are sized geometry points");

        controller.selectWeather("Rain_Thunderstorm", 0.0f);
        controller.setCameraPosition(3.0f, 1.6f, 0.0f);
        controller.update(0.1f); controller.update(0.1f);
        require(controller.getState().cameraInside, "interior exclusion volume");
        require(controller.getOcclusion().blocksPrecipitation(3.0f, 1.6f, 0.0f), "interior precipitation blocked");
        require(controller.getState().surface.interiorSnow == 0.0f, "no interior snow accumulation");
        controller.setCameraPosition(-5.0f, 1.6f, -5.0f);
        for (int i = 0; i < 260; i++) controller.update(0.1f);
        require(controller.getState().precipitation.rainParticles > 0, "world-space rain budget active");
        require(controller.getState().surface.wetness > 0.0f, "wetness accumulates");
        require(controller.getState().surface.puddle > 0.0f, "puddle fills");
        require(controller.getState().lighting.iblSlot.equals("storm"), "prepared storm IBL selected");
        require(controller.getState().lightning.eventIndex > 0, "deterministic lightning event generated");
        require(controller.getState().lightning.thunderDelaySeconds > 0.0f, "distance-based thunder delay generated");
        require(controller.getState().audio.activeProfile.equals("rain"), "rain audio profile crossfade target");
        float wetPeak = controller.getState().surface.wetness;
        float puddlePeak = controller.getState().surface.puddle;
        controller.selectWeather("Clear_Skies", 0.0f); controller.setTime(1200.0f);
        for (int i = 0; i < 600; i++) controller.update(0.1f);
        require(controller.getState().surface.wetness < wetPeak, "wetness dries");
        require(controller.getState().surface.puddle < puddlePeak, "puddle drains");

        controller.selectWeather("Snow_Blizzard", 0.0f);
        for (int i = 0; i < 260; i++) controller.update(0.1f);
        require(controller.getState().precipitation.snowParticles > 0, "world-space snow budget active");
        require(controller.getState().surface.snowCover > 0.0f, "snow accumulation");
        require(controller.getState().lighting.iblSlot.equals("snow"), "prepared snow IBL selected");
        float snowPeak = controller.getState().surface.snowCover;
        controller.selectWeather("Clear_Skies", 0.0f); controller.setTime(1200.0f);
        for (int i = 0; i < 1000; i++) controller.update(0.1f);
        require(controller.getState().surface.snowCover < snowPeak, "snow melts/fades");

        require(controller.getState().getFeatureStatus().get("interior_exclusion") == EnvironmentFeatureStatus.FUNCTIONAL, "classification truth present");
        testExactUdsSunTrajectory(env);
        testExactUdsSunValues();
        testCanonicalCelestialCoordinates();
        testCameraGestureOwnership();
        testAnalyticSky();
        testCelestialOnly(env);
        testIntegratedWeather(env);
        testSeasonalWeatherPolicy();
        System.out.println("P63_CORE_TEST=PASS presets=" + Arrays.toString(IDS)
            + " stars=" + starsA.getStars().size()
            + " lightningEvents=" + controller.getState().lightning.eventIndex);
    }

    private static void testExactUdsSunTrajectory(SolumEnvironmentPackage env) {
        SolumUdsSunTrajectory.Inputs inputs = new SolumUdsSunTrajectory.Inputs();
        SolumUdsSunTrajectory.Output output = new SolumUdsSunTrajectory.Output();

        inputs.timeOfDay = 600.0;
        SolumUdsSunTrajectory.evaluate(inputs, output);
        require(SolumUdsSunTrajectory.CONTRACT_STATUS.equals(output.status)
            && close(output.timeCycleDegrees, 90.0f)
            && vectorClose(output.filamentLightDirection, new float[] {-1.0f, 0.0f, 0.0f})
            && vectorClose(output.filamentVisualDirection, new float[] {1.0f, 0.0f, 0.0f}),
            "exact UDS dawn fixture and UE-to-Filament mapping");

        inputs.timeOfDay = 1200.0;
        SolumUdsSunTrajectory.evaluate(inputs, output);
        require(close(output.timeCycleDegrees, 180.0f)
            && vectorClose(output.ueCachedSunVector, new float[] {0.5f, 0.0f, -0.8660254f})
            && vectorClose(output.filamentVisualDirection, new float[] {0.0f, 0.8660254f, 0.5f}),
            "exact UDS noon fixture");

        inputs.timeOfDay = 1800.0;
        SolumUdsSunTrajectory.evaluate(inputs, output);
        require(close(output.timeCycleDegrees, 270.0f)
            && vectorClose(output.filamentVisualDirection, new float[] {-1.0f, 0.0f, 0.0f}),
            "exact UDS dusk fixture");

        inputs.timeOfDay = 1200.0;
        inputs.daylightSavingsTime = true;
        SolumUdsSunTrajectory.evaluate(inputs, output);
        require(close(output.timeInRange, 1100.0f) && close(output.timeCycleDegrees, 165.0f),
            "UDS daylight-savings subtracts exactly 100 time units");
        inputs.daylightSavingsTime = false;

        inputs.timeOfDay = 600.0;
        inputs.sunYawDegrees = 90.0;
        SolumUdsSunTrajectory.evaluate(inputs, output);
        require(vectorClose(output.filamentVisualDirection, new float[] {0.0f, 0.0f, 1.0f}),
            "UDS Sun Yaw rotates the world-space orbit");
        inputs.sunYawDegrees = 0.0;

        inputs.timeOfDay = 900.0;
        inputs.extendDawnAndDusk = 5.0;
        SolumUdsSunTrajectory.evaluate(inputs, output);
        require(close(output.extendDawnDuskZ, 0.2725f),
            "UDS Extend Dawn and Dusk source formula");
        inputs.extendDawnAndDusk = 0.0;

        inputs.simulateRealSun = true;
        SolumUdsSunTrajectory.evaluate(inputs, output);
        require(SolumUdsSunTrajectory.REAL_SUN_STATUS.equals(output.status)
            && close(length(output.filamentVisualDirection), 0.0f),
            "unimplemented real-date Sun branch fails closed without a fake orbit");
        inputs.simulateRealSun = false;
        inputs.manuallyPositionSunTarget = true;
        SolumUdsSunTrajectory.evaluate(inputs, output);
        require(SolumUdsSunTrajectory.MANUAL_TARGET_STATUS.equals(output.status)
            && close(length(output.filamentVisualDirection), 0.0f),
            "unimplemented world-target Sun branch fails closed without a coordinate approximation");

        SolumEnvironmentController controller = new SolumEnvironmentController(env);
        controller.setCelestialOnlyMode(true);
        controller.getCelestialControls().applyCloudPreset("Clear");
        controller.setTime(1200.0f);
        controller.update(0.0f);
        SolumEnvironmentLightingState lighting = controller.getState().lighting;
        require(SolumUdsSunTrajectory.CONTRACT_STATUS.equals(lighting.sunTrajectoryStatus)
            && close(lighting.sunTimeCycleDegrees, 180.0f)
            && vectorClose(lighting.sunVisualDirection, new float[] {0.0f, 0.8660254f, 0.5f})
            && SolumCelestialCoordinateSystem.consistentBodyAndLightDirection(
                lighting.sunVisualDirection, lighting.sunDirection),
            "controller publishes exact UDS Sun as the canonical renderer direction");
        require(close(length(lighting.moonVisualDirection), 1.0f),
            "exact Sun integration leaves the independent Moon path intact");
    }

    private static void testCelestialOnly(SolumEnvironmentPackage env) {
        SolumEnvironmentController controller = new SolumEnvironmentController(env);
        controller.setCelestialOnlyMode(true);
        SolumCelestialControlState controls = controller.getCelestialControls();
        require(controls.oldIblActive && !controls.p63IblEnabled, "old IBL remains active");
        require(close(controls.cameraOrbitSensitivity, 0.002f), "camera orbit defaults to one-fifth sensitivity");
        require(close(controls.dayLengthMinutes, 60.0f), "celestial day length no longer defaults to four minutes");
        require(controls.cloudsEnabled && controls.starsEnabled && controls.precipitationEnabled
            && controls.surfaceWeatherEnabled && controls.lightningEnabled
            && controls.verifiedWeatherAudioEnabled && controls.weatherDrivesSky
            && !controls.proceduralAudioEnabled,
            "P63.8 enables integrated weather while generated audio remains off");
        require(controls.analyticSky && controls.analyticSun && controls.analyticMoon
            && controls.analyticStars && controls.analyticClouds && controls.legacyCelestialFallback,
            "analytic feature flags default on with legacy fallback");
        controls.weatherDrivesSky = false;
        controller.setSmartWeatherEnabled(false);
        require(!controls.weatherDrivesSky,
            "disabling smart weather preserves restored manual cloud ownership");
        controller.setSmartWeatherEnabled(true);
        require(controls.weatherDrivesSky && controls.smartWeatherEnabled,
            "enabling smart weather intentionally owns analytic clouds");
        controller.takeManualCloudControl();
        require(!controls.weatherDrivesSky && !controls.smartWeatherEnabled
            && "manual_cloud_override".equals(controller.getState().smartWeatherDecision),
            "manual cloud edit has stable ownership and stops procedural overwrite");
        controls.sunLightLux = 4.0f;
        controls.sunVisualBrightness = 1.5f;
        controls.applyCloudPreset("Clear");
        controller.setTime(1200.0f);
        controller.update(0.1f);
        float sunLux = controller.getState().lighting.sunLux;
        float[] sunDisk = controller.getState().lighting.sunDiskColor.clone();
        require(close(sunLux, 4.0f) && sunDisk[0] > 0.0f,
            "UDS Sun disk intensity and direct light are evaluated separately");
        controls.sunVisualBrightness = 0.25f;
        controls.sunEmissive = 1.35f;
        controller.update(0.1f);
        require(close(controller.getState().lighting.sunLux, sunLux), "sun visual edit does not change directional light");
        require(controller.getState().lighting.sunDiskColor[0] < sunDisk[0] * 0.18f,
            "UDS Sun Disk Intensity changes disk energy without changing direct lux");
        require(close(controls.sunEmissive, 1.35f) && close(controller.getState().lighting.sunLux, sunLux)
            && controller.getState().analyticSky.udsExactSunValues,
            "legacy emissive state is ignored while exact UDS Sun values own the shader");
        float[] exactDiskBeforeLegacyGain = controller.getState().lighting.sunDiskColor.clone();
        controls.sunEmissive = 5_000.0f;
        controller.update(0.1f);
        require(close(controller.getState().analyticSky.sunEmissiveGain, 5_000.0f)
            && close(controller.getState().lighting.sunLux, sunLux)
            && vectorClose(controller.getState().lighting.sunDiskColor, exactDiskBeforeLegacyGain),
            "legacy persisted HDR gain remains finite but cannot replace exact UDS disk values");
        controls.sunDiscVisibility = 0.0f;
        controller.update(0.1f);
        require(close(controller.getState().analyticSky.sunDiscVisibility, 0.0f)
            && close(controller.getState().analyticSky.sunEmissiveGain, 5_000.0f),
            "Sun visibility reaches the shader independently from the saved HDR gain control");
        controls.sunDiscVisibility = 1.0f;
        controls.moonPhaseAngleDegrees = 138.6f;
        controls.moonLightLux = 0.4f;
        controller.setTime(0.0f);
        controller.update(0.1f);
        require(close(controller.getState().lighting.moonPhase, 0.23f), "continuous moon phase angle applies");
        require(controller.getState().lighting.moonLux > 0.0f && controller.getState().lighting.sunLux == 0.0f, "separate moon directional light at night");
        float moonLux = controller.getState().lighting.moonLux;
        controls.moonEmissive = 1.1f;
        controller.update(0.1f);
        require(close(controller.getState().lighting.moonLux, moonLux), "moon emissive is independent from moon directional light intensity");
        require(controller.getState().weather.rain == 0.0f && controller.getState().weather.snow == 0.0f
            && controller.getState().lighting.starVisibility > 0.0f, "celestial stage stays precipitation-free and shows night stars");
        controls.applyCloudPreset("Cloudy");
        controller.update(0.1f);
        require(controls.cloudsEnabled && close(controls.cloudCoverage, 0.82f)
            && close(controller.getState().clouds.coverage, 0.82f)
            && "Stratocumulus".equals(controls.cloudType), "cloud preset updates density family and renderer state");
        controls.moonLightLux = 0.0f;
        controller.update(0.1f);
        require(close(controller.getState().analyticSky.moonLightLux, 0.0f),
            "night cloud Moon input follows direct Moon lux with no constant emission");
        controls.applyCloudPreset("Clear");
        controller.update(0.1f);
        require(!controls.cloudsEnabled && close(controller.getState().clouds.coverage, 0.0f), "Clear preset hides cloud layers");
        controls.starDensity = 0.55f; controls.starBrightness = 1.2f; controls.starSize = 1.4f;
        controls.starTwinkleAmount = 0.65f; controls.setStarTint(0.2f, 0.4f, 0.8f); controls.sanitize();
        require(close(controls.starDensity, 0.55f) && close(controls.starBrightness, 1.2f)
            && close(controls.starSize, 1.4f) && close(controls.starTint[2], 0.8f), "star controls remain bounded and independent");
        SolumAnalyticSkyState analytic = controller.getState().analyticSky;
        require(analytic.analyticSky && analytic.analyticSun && analytic.analyticMoon
            && analytic.analyticStars && !analytic.analyticClouds, "controller publishes per-feature analytic state");
        require("Low".equals(analytic.cloudQuality) && analytic.oldIbl && !analytic.p63DynamicIbl,
            "Low default, old IBL true, dynamic IBL false");
        controls.sunLightLux = Float.NaN;
        controls.sunDiscLuminanceNits = Float.NaN;
        controls.moonLightLux = Float.POSITIVE_INFINITY;
        controls.exposureCompensation = Float.NEGATIVE_INFINITY;
        controls.sanitize();
        require(close(controls.sunLightLux, SolumCelestialControlState.UDS_DEFAULT_SUN_LIGHT_INTENSITY_LUX)
            && close(controls.sunDiscLuminanceNits, 35_000.0f)
            && close(controls.moonLightLux, 0.15f)
            && close(controls.exposureCompensation, 0.0f), "no NaN or Infinity crosses state boundary");
        controls.sunLightLux = 2_000_000.0f; controls.sunDiscLuminanceNits = 2_000_000.0f;
        controls.sunEmissive = 2_000_000.0f;
        controls.moonLightLux = 999.0f; controls.bloomLikeResponse = 999.0f;
        controls.lensFlareIntensity = Float.POSITIVE_INFINITY;
        controls.lensFlareGhostCount = 999.0f;
        controls.sanitize();
        require(close(controls.sunLightLux, SolumAnalyticSkyMaterial.SUN_LUX_SAFETY_MAX)
            && close(controls.sunDiscLuminanceNits, SolumAnalyticSkyMaterial.SUN_LUMINANCE_SAFETY_MAX_NITS)
            && close(controls.sunEmissive, SolumCelestialControlState.SUN_EMISSIVE_GAIN_SAFETY_MAX)
            && close(controls.moonLightLux, SolumAnalyticSkyMaterial.MOON_LUX_SAFETY_MAX)
            && close(controls.bloomLikeResponse, 0.25f)
            && close(controls.lensFlareIntensity, 0.55f) && close(controls.lensFlareGhostCount, 8.0f),
            "large but finite physical and lens-flare safety ranges");
        controls.applyLensFlarePreset("Cinematic");
        require(controls.lensFlareEnabled && controls.lensFlareStarburst
            && controls.lensFlareGhostCount == 6.0f && controls.lensFlareIntensity > 0.5f,
            "renderer-native lens flare preset control truth");
        testContinuousMoonDirections();
        controls.applyScenarioPreset("Milky Way Night");
        require(close(controls.time, 0.0f) && close(controls.milkyWayIntensity, 0.75f)
            && !controls.cloudsEnabled && controls.timePaused, "analytic scenario preset inputs");
        controls.applyScenarioPreset("Aurora Night");
        controller.update(0.1f);
        require(controls.auroraEnabled && close(controls.auroraIntensity, 0.90f)
            && controller.getState().analyticSky.auroraEnabled,
            "verified UDS-derived Aurora preset reaches analytic uniform state");
    }

    private static void testExactUdsSunValues() {
        require(close(SolumUdsSunValues.directionalIntensityCurve(-0.3393186405301094),
            0.8499964f, 0.00001f), "UDS Directional_Light_Intensity cubic fixture");
        require(close(SolumUdsSunValues.directionalIntensityCurve(-0.028693096712231636),
            0.2592117f, 0.00001f), "UDS directional intensity second cubic segment fixture");

        float[] color = new float[4];
        SolumUdsSunValues.sunDiskColorCurve(0.5389501750469208, color);
        require(close(color[0], 0.5716651f, 0.00001f)
            && close(color[1], 0.10358332f, 0.00001f)
            && close(color[2], 0.028450984f, 0.00001f),
            "UDS Sun_Disk_Color cubic fixture");
        SolumUdsSunValues.sunLightColorCurve(0.55, color);
        require(close(color[0], 0.85f, 0.00001f)
            && close(color[1], 0.4082125f, 0.00001f)
            && close(color[2], 0.09264997f, 0.00001f),
            "UDS Sun_Light_Color linear fixture");

        SolumUdsSunValues.Inputs inputs = new SolumUdsSunValues.Inputs();
        SolumUdsSunValues.Output output = new SolumUdsSunValues.Output();
        inputs.cachedSunVectorZ = -0.8660254037844386;
        inputs.usingSkyAtmosphere = true;
        SolumUdsSunValues.evaluate(inputs, output);
        require(SolumUdsSunValues.CONTRACT_STATUS.equals(output.formulaStatus)
            && close(output.currentSunRadiusRadians, (float)Math.toRadians(1.2), 0.000001f)
            && close(output.currentSunLightIntensityLux, 5.0f)
            && close(output.currentSunDiskIntensity, 860.21506f, 0.001f)
            && close(output.currentSunDiskColor[0], 412.68817f, 0.001f)
            && close(output.currentSunDiskColor[1], 360.71938f, 0.001f)
            && close(output.currentSunDiskColor[2], 262.96768f, 0.001f),
            "UDS noon radius/light/disk formula fixture");
    }

    private static void testIntegratedWeather(SolumEnvironmentPackage env) {
        SolumEnvironmentController controller = new SolumEnvironmentController(env);
        controller.setCelestialOnlyMode(true);
        SolumCelestialControlState controls = controller.getCelestialControls();
        controller.selectWeather("Clear_Skies", 0.0f);
        controller.update(0.1f);
        require(close(controller.getState().precipitation.rain, 0.0f)
            && close(controller.getState().precipitation.snow, 0.0f),
            "clear analytic sky cannot precipitate");
        float finiteCoverage = controller.getState().weather.cloudCoverage;
        controller.setWeatherValue("cloudCoverage", Float.NaN);
        controller.setWeatherValue("windDirectionDeg", Float.POSITIVE_INFINITY);
        require(close(controller.getState().weather.cloudCoverage, finiteCoverage)
            && Float.isFinite(controller.getState().weather.windDirectionDeg),
            "weather runtime boundary rejects NaN and Infinity");

        controller.selectWeather("Rain", 2.0f);
        for (int i = 0; i < 8; i++) controller.update(0.1f);
        require(controller.getState().precipitation.rain < controller.getState().weather.rain,
            "rain is suppressed while storm clouds are still building");
        for (int i = 0; i < 20; i++) controller.update(0.1f);
        require(controller.getState().precipitation.rain > 0.05f
            && close(controller.getState().precipitation.snow, 0.0f),
            "rain starts only after sufficient cloud coverage/density");
        require(controller.getState().analyticSky.weatherRain > 0.05f,
            "resolved rain reaches analytic cloud-lighting uniform");

        controller.setCameraPosition(4.6f, 1.6f, 5.5f);
        for (int i = 0; i < 30; i++) controller.update(0.1f);
        require(controller.getState().cameraInside && controller.getState().cameraUnderRoof,
            "celestial diagnostic room is an interior roof volume");
        require(controller.getState().audio.rainGain < 0.02f,
            "rain audio is suppressed under the celestial roof");

        controller.setWeatherValue("snow", 0.8f);
        controller.setWeatherValue("cloudCoverage", 0.9f);
        controller.setWeatherValue("cloudDensity", 0.9f);
        controller.update(0.1f);
        require(controller.getState().precipitation.snow > 0.0f
            && close(controller.getState().precipitation.rain, 0.0f)
            && !controller.getState().lightning.enabled,
            "snow excludes rain and storm lightning");

        controller.selectWeather("Rain_Thunderstorm", 0.0f);
        controller.setCameraPosition(-5.0f, 1.6f, -5.0f);
        controller.update(0.1f);
        controller.triggerLightning();
        controller.update(0.1f);
        require(controller.getState().lightning.eventIndex > 0
            && controller.getState().precipitation.rain > 0.72f,
            "lightning can trigger only in a dense heavy-rain storm");

        controls.precipitationEnabled = false;
        controller.update(0.1f);
        require(close(controller.getState().precipitation.rain, 0.0f),
            "weather precipitation feature can fail closed independently");
    }

    private static void testSeasonalWeatherPolicy() {
        SolumSeasonalWeatherPolicy policy = new SolumSeasonalWeatherPolicy(1597463007);
        Calendar winter = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        winter.clear();
        winter.set(2026, Calendar.JANUARY, 15);
        for (int index = 0; index < 32; index++) {
            SolumSeasonalWeatherPolicy.Decision arid = policy.decide(
                SolumSeasonalWeatherPolicy.ARID, index, winter);
            require(!arid.presetId.startsWith("Snow"),
                "arid smart weather never invents seasonal snow");
            SolumSeasonalWeatherPolicy.Decision tropical = policy.decide(
                SolumSeasonalWeatherPolicy.TROPICAL, index, winter);
            require(!tropical.presetId.startsWith("Snow") && !tropical.presetId.startsWith("Sand"),
                "tropical smart weather remains rain/cloud/clear bounded");
        }
        SolumSeasonalWeatherPolicy.Decision north = policy.decide(
            SolumSeasonalWeatherPolicy.TEMPERATE_NORTH, 4, winter);
        SolumSeasonalWeatherPolicy.Decision repeated = policy.decide(
            SolumSeasonalWeatherPolicy.TEMPERATE_NORTH, 4, winter);
        require(north.presetId.equals(repeated.presetId)
            && close(north.transitionSeconds, repeated.transitionSeconds)
            && close(north.holdSeconds, repeated.holdSeconds)
            && "Winter".equals(north.season),
            "season/date policy is deterministic for an explicit climate profile");
    }

    private static void testContinuousMoonDirections() {
        float[] moon = {0.34f, 0.22f, -0.914111f};
        float[] full = new float[3];
        float[] quarter = new float[3];
        float[] crescentA = new float[3];
        float[] crescentB = new float[3];
        float[] fresh = new float[3];
        SolumAnalyticSkyMaterial.moonToSunDirection(moon, 0.0f, full);
        SolumAnalyticSkyMaterial.moonToSunDirection(moon, 90.0f, quarter);
        SolumAnalyticSkyMaterial.moonToSunDirection(moon, 149.9f, crescentA);
        SolumAnalyticSkyMaterial.moonToSunDirection(moon, 150.1f, crescentB);
        SolumAnalyticSkyMaterial.moonToSunDirection(moon, 180.0f, fresh);
        float moonLength = length(moon);
        float[] unitMoon = {moon[0] / moonLength, moon[1] / moonLength, moon[2] / moonLength};
        require(dot(full, unitMoon) < -0.999f, "full moon light points toward observer-facing normal");
        require(Math.abs(dot(quarter, unitMoon)) < 0.002f, "quarter moon direction is orthogonal");
        require(dot(fresh, unitMoon) > 0.999f, "new moon light points away from observer-facing normal");
        require(dot(crescentA, crescentB) > 0.9999f, "phase direction changes continuously without an index jump");
        for (float value : crescentA) require(Float.isFinite(value), "continuous Moon direction remains finite");
    }

    private static void testCanonicalCelestialCoordinates() {
        float[] fixedTimes = {0.0f, 600.0f, 900.0f, 1200.0f, 1700.0f, 1800.0f, 2100.0f};
        SolumEnvironmentLightingState first = new SolumEnvironmentLightingState();
        SolumEnvironmentLightingState second = new SolumEnvironmentLightingState();
        for (float time : fixedTimes) {
            SolumCelestialCoordinateSystem.update(time, 0.0f, first);
            SolumCelestialCoordinateSystem.update(time, 0.0f, second);
            require(vectorClose(first.sunVisualDirection, second.sunVisualDirection), "deterministic sun direction at " + time);
            require(vectorClose(first.moonVisualDirection, second.moonVisualDirection), "deterministic moon direction at " + time);
            require(close(length(first.sunVisualDirection), 1.0f) && close(length(first.moonVisualDirection), 1.0f), "normalized celestial directions");
            require(SolumCelestialCoordinateSystem.consistentBodyAndLightDirection(first.sunVisualDirection, first.sunDirection),
                "sun visual and Filament light derive from one canonical direction");
            require(SolumCelestialCoordinateSystem.consistentBodyAndLightDirection(first.moonVisualDirection, first.moonDirection),
                "moon visual and Filament light derive from one canonical direction");
        }
        for (float time : new float[] {900.0f, 1200.0f, 1700.0f}) {
            SolumCelestialCoordinateSystem.update(time, 0.0f, first);
            require(first.sunAboveHorizon, "sun visible at " + time);
        }
        for (float time : new float[] {0.0f, 2100.0f}) {
            SolumCelestialCoordinateSystem.update(time, 0.0f, first);
            require(first.moonAboveHorizon, "moon visible at " + time);
        }
        SolumCelestialCoordinateSystem.update(1200.0f, 0.0f, first);
        float[] positionA = new float[3];
        float[] positionB = new float[3];
        SolumCelestialCoordinateSystem.positionRelativeToCamera(positionA, 0.0f, 2.0f, 4.0f,
            first.sunVisualDirection, SolumCelestialCoordinateSystem.SKY_RADIUS);
        SolumCelestialCoordinateSystem.positionRelativeToCamera(positionB, 7.0f, 5.0f, -3.0f,
            first.sunVisualDirection, SolumCelestialCoordinateSystem.SKY_RADIUS);
        require(close(positionB[0] - positionA[0], 7.0f) && close(positionB[1] - positionA[1], 3.0f)
            && close(positionB[2] - positionA[2], -7.0f), "disk position translates with camera without parallax");
        require(positionA[1] > 0.0f && first.sunAboveHorizon, "visible disk cannot be placed below ground by world origin");
        float[] eye = {2.0f, 3.0f, 5.0f};
        float[] target = new float[3];
        SolumCelestialCoordinateSystem.focusTarget(target, eye, first.sunVisualDirection, 12.0f);
        require(SolumCelestialCoordinateSystem.focusDirectionAligned(eye, target, first.sunVisualDirection),
            "Focus Sun lands camera on visible sun direction");
        SolumCelestialCoordinateSystem.update(0.0f, 0.0f, 2.0f, first);
        SolumCelestialCoordinateSystem.focusTarget(target, eye, first.moonVisualDirection, 12.0f);
        require(SolumCelestialCoordinateSystem.focusDirectionAligned(eye, target, first.moonVisualDirection),
            "Focus Moon lands camera on visible moon direction");
    }

    private static void testCameraGestureOwnership() {
        SolumCameraGestureState gesture = new SolumCameraGestureState();
        gesture.beginPrimary(20.0f, 20.0f);
        gesture.updatePrimary(60.0f, 35.0f);
        require(gesture.changesOrbit() && !gesture.changesDistance() && !gesture.changesTarget(),
            "orbit does not change camera distance or target");
        gesture.beginTwo(10.0f, 20.0f, 50.0f, 20.0f);
        gesture.updateTwo(0.0f, 20.0f, 65.0f, 20.0f);
        require(gesture.changesDistance() && !gesture.changesOrbit() && !gesture.changesTarget(),
            "pinch changes only camera distance");
        require(Math.abs(gesture.consumePinchDelta()) > 0.0f, "pinch produces a distance delta");
        gesture.updateTwo(20.0f, 50.0f, 85.0f, 50.0f);
        require(gesture.changesDistance(), "pinch ownership stays locked when midpoint also moves");
        gesture.beginTwo(10.0f, 20.0f, 50.0f, 20.0f);
        gesture.updateTwo(25.0f, 35.0f, 65.0f, 35.0f);
        require(gesture.changesTarget() && !gesture.changesDistance() && !gesture.changesOrbit(),
            "two-finger pan changes only target");
    }

    private static void testAnalyticSky() {
        float[] sun = {0.0f, 0.75f, -0.6614378f};
        float[] above = {0.8f, 0.001f, -0.6f};
        float[] below = {0.8f, -0.001f, -0.6f};
        float[] nadir = {0.0f, -1.0f, 0.0f};
        float[] horizonColor = new float[3];
        float[] belowColor = new float[3];
        float[] nadirColor = new float[3];
        SolumAnalyticSky.linearColor(above, sun, horizonColor);
        SolumAnalyticSky.linearColor(below, sun, belowColor);
        SolumAnalyticSky.linearColor(nadir, sun, nadirColor);
        require(colorDistance(horizonColor, belowColor) < 0.02f, "no upper/lower hemisphere seam");
        require(colorDistance(belowColor, nadirColor) > 0.02f, "lower hemisphere remains atmospheric, not a flat fill");
        for (float value : horizonColor) require(Float.isFinite(value) && value > 0.0f, "sky has finite non-black floor");
        ByteBuffer cubemap = SolumAnalyticSky.createSrgbCubemap(sun);
        require(cubemap.remaining() == SolumAnalyticSky.FACE_COUNT * SolumAnalyticSky.CUBEMAP_SIZE
            * SolumAnalyticSky.CUBEMAP_SIZE * 4, "full six-face sRGB sky cubemap");
    }

    private static SolumEnvironmentPackage fixture() {
        SolumEnvironmentPackage env = new SolumEnvironmentPackage();
        env.addQuality(new SolumEnvironmentQuality("Low", 420, 5, 96, 0.72f));
        env.addQuality(new SolumEnvironmentQuality("Medium", 1100, 8, 192, 0.86f));
        env.addQuality(new SolumEnvironmentQuality("High", 2200, 12, 320, 1.0f));
        env.addQuality(new SolumEnvironmentQuality("Manual", 1600, 10, 256, 0.92f));
        for (String id : IDS) {
            SolumWeatherState state = new SolumWeatherState();
            state.id = id; state.name = id; state.cloudHeight = 15.0f; state.cloudDensity = 0.45f;
            state.cloudCoverage = id.contains("Clear") ? 0.08f : 0.78f;
            state.cloudDensity = id.contains("Clear") ? 0.34f : 0.82f;
            state.windDirectionDeg = 180.0f; state.windSpeed = id.contains("Storm") || id.contains("Blizzard") ? 1.0f : 0.28f;
            state.windGust = 0.45f; state.windTurbulence = 0.35f; state.humidity = 0.5f;
            if (id.startsWith("Rain")) { state.rain = id.endsWith("Light") ? 0.28f : 0.82f; state.wetnessTarget = 0.82f; }
            if (id.startsWith("Snow")) { state.snow = id.endsWith("Light") ? 0.28f : 0.86f; state.snowTarget = 0.86f; }
            if (id.startsWith("Sand")) state.dust = id.endsWith("Storm") ? 0.92f : 0.35f;
            if (id.equals("Foggy")) state.fogDensity = 0.1055f;
            if (id.equals("Rain_Thunderstorm")) { state.rain = 1.0f; state.lightningEnabled = 1.0f; state.lightningPotential = 1.0f; }
            env.addPreset(state);
        }
        return env;
    }

    private static boolean close(float a, float b) { return Math.abs(a - b) < 0.001f; }
    private static boolean close(float a, float b, float tolerance) { return Math.abs(a - b) < tolerance; }
    private static float length(float[] value) { return (float)Math.sqrt(value[0] * value[0] + value[1] * value[1] + value[2] * value[2]); }
    private static float dot(float[] a, float[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
    private static boolean vectorClose(float[] a, float[] b) { return close(a[0], b[0]) && close(a[1], b[1]) && close(a[2], b[2]); }
    private static float colorDistance(float[] a, float[] b) { return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]); }
    private static void require(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
