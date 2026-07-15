package com.solum.engine.environment.p63;

import java.util.Arrays;

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
        require(!controls.cloudsEnabled && !controls.starsEnabled && !controls.precipitationEnabled
            && !controls.surfaceWeatherEnabled && !controls.lightningEnabled && !controls.proceduralAudioEnabled,
            "P63.2A deferred systems default off");
        controls.sunLightLux = 4.0f;
        controls.sunVisualBrightness = 1.5f;
        controller.setTime(1200.0f);
        controller.update(0.1f);
        float sunLux = controller.getState().lighting.sunLux;
        float sunVisual = controller.getState().lighting.sunDiskBrightness;
        require(close(sunLux, 4.0f) && close(sunVisual, 1.5f), "sun visual/light separation values");
        controls.sunVisualBrightness = 0.25f;
        controller.update(0.1f);
        require(close(controller.getState().lighting.sunLux, sunLux), "sun visual edit does not change directional light");
        require(close(controller.getState().lighting.sunDiskBrightness, 0.25f), "sun visual edit applies independently");
        controls.moonPhase = 0.23f;
        controls.moonLightLux = 0.4f;
        controller.setTime(0.0f);
        controller.update(0.1f);
        require(close(controller.getState().lighting.moonPhase, 0.23f), "moon phase applies");
        require(controller.getState().lighting.moonLux > 0.0f && controller.getState().lighting.sunLux == 0.0f, "separate moon directional light at night");
        require(controller.getState().weather.rain == 0.0f && controller.getState().weather.snow == 0.0f
            && controller.getState().lighting.starVisibility == 0.0f, "celestial stage stays weather/star free");
        controls.sunLightLux = Float.NaN;
        controls.moonLightLux = Float.POSITIVE_INFINITY;
        controls.exposureCompensation = Float.NEGATIVE_INFINITY;
        controls.sanitize();
        require(close(controls.sunLightLux, 18.0f) && close(controls.moonLightLux, 0.15f)
            && close(controls.exposureCompensation, 0.0f), "no NaN or Infinity crosses state boundary");
        controls.sunLightLux = 999.0f; controls.moonLightLux = 999.0f; controls.bloomLikeResponse = 999.0f;
        controls.sanitize();
        require(close(controls.sunLightLux, 50.0f) && close(controls.moonLightLux, 2.0f)
            && close(controls.bloomLikeResponse, 0.12f), "safe light and post-process ranges");
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
    private static void require(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
