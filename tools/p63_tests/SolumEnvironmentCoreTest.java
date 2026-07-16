package com.solum.engine.environment.p63;

import java.util.Arrays;
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
        testCanonicalCelestialCoordinates();
        testCameraGestureOwnership();
        testAnalyticSky();
        testCelestialOnly(env);
        System.out.println("P63_CORE_TEST=PASS presets=" + Arrays.toString(IDS)
            + " stars=" + starsA.getStars().size()
            + " lightningEvents=" + controller.getState().lightning.eventIndex);
    }

    private static void testCelestialOnly(SolumEnvironmentPackage env) {
        SolumEnvironmentController controller = new SolumEnvironmentController(env);
        controller.setCelestialOnlyMode(true);
        SolumCelestialControlState controls = controller.getCelestialControls();
        require(controls.oldIblActive && !controls.p63IblEnabled, "old IBL remains active");
        require(controls.cloudsEnabled && controls.starsEnabled && !controls.precipitationEnabled
            && !controls.surfaceWeatherEnabled && !controls.lightningEnabled && !controls.proceduralAudioEnabled,
            "P63.3 enables analytic stars/clouds while deferred weather remains off");
        require(controls.analyticSky && controls.analyticSun && controls.analyticMoon
            && controls.analyticStars && controls.analyticClouds && controls.legacyCelestialFallback,
            "analytic feature flags default on with legacy fallback");
        controls.sunLightLux = 4.0f;
        controls.sunVisualBrightness = 1.5f;
        controller.setTime(1200.0f);
        controller.update(0.1f);
        float sunLux = controller.getState().lighting.sunLux;
        float sunVisual = controller.getState().lighting.sunDiskBrightness;
        require(close(sunLux, 4.0f) && close(sunVisual, 1.5f), "sun visual/light separation values");
        controls.sunVisualBrightness = 0.25f;
        controls.sunEmissive = 1.35f;
        controller.update(0.1f);
        require(close(controller.getState().lighting.sunLux, sunLux), "sun visual edit does not change directional light");
        require(close(controller.getState().lighting.sunDiskBrightness, 0.25f), "sun visual edit applies independently");
        require(close(controls.sunEmissive, 1.35f) && close(controller.getState().lighting.sunLux, sunLux),
            "sun emissive is independent from sun directional light intensity");
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
            && close(controller.getState().clouds.coverage, 0.82f), "cloud preset updates renderer state");
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
        require(close(controls.sunLightLux, 35_000.0f) && close(controls.sunDiscLuminanceNits, 35_000.0f)
            && close(controls.moonLightLux, 0.15f)
            && close(controls.exposureCompensation, 0.0f), "no NaN or Infinity crosses state boundary");
        controls.sunLightLux = 2_000_000.0f; controls.sunDiscLuminanceNits = 2_000_000.0f;
        controls.moonLightLux = 999.0f; controls.bloomLikeResponse = 999.0f;
        controls.sanitize();
        require(close(controls.sunLightLux, SolumAnalyticSkyMaterial.SUN_LUX_SAFETY_MAX)
            && close(controls.sunDiscLuminanceNits, SolumAnalyticSkyMaterial.SUN_LUMINANCE_SAFETY_MAX_NITS)
            && close(controls.moonLightLux, SolumAnalyticSkyMaterial.MOON_LUX_SAFETY_MAX)
            && close(controls.bloomLikeResponse, 0.12f), "large but finite physical safety ranges");
        testContinuousMoonDirections();
        controls.applyScenarioPreset("Milky Way Night");
        require(close(controls.time, 0.0f) && close(controls.milkyWayIntensity, 0.75f)
            && !controls.cloudsEnabled, "analytic scenario preset inputs");
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
            state.cloudCoverage = id.contains("Clear") ? 0.08f : 0.55f;
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
    private static float length(float[] value) { return (float)Math.sqrt(value[0] * value[0] + value[1] * value[1] + value[2] * value[2]); }
    private static float dot(float[] a, float[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
    private static boolean vectorClose(float[] a, float[] b) { return close(a[0], b[0]) && close(a[1], b[1]) && close(a[2], b[2]); }
    private static float colorDistance(float[] a, float[] b) { return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]); }
    private static void require(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
