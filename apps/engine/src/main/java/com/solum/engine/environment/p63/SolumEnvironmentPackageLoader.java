package com.solum.engine.environment.p63;

import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class SolumEnvironmentPackageLoader {
    public static final String ASSET_PATH = "env/solum_environment_runtime.json";

    private SolumEnvironmentPackageLoader() { }

    public static SolumEnvironmentPackage load(AssetManager assets) throws Exception {
        if (assets == null) throw new IllegalArgumentException("asset_manager_missing");
        try (InputStream input = assets.open(ASSET_PATH); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return parse(new String(output.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    public static SolumEnvironmentPackage parse(String jsonText) throws Exception {
        JSONObject root = new JSONObject(jsonText);
        SolumEnvironmentPackage out = new SolumEnvironmentPackage();
        out.schema = root.optString("schema", "");
        out.schemaVersion = root.optInt("schemaVersion", 0);
        out.packageId = root.optString("packageId", "");
        out.deterministicSeed = root.optInt("deterministicSeed", 1597463007);
        JSONObject time = root.optJSONObject("time");
        if (time != null) {
            out.initialTime = f(time, "initial", out.initialTime); out.dawn = f(time, "dawn", out.dawn);
            out.dusk = f(time, "dusk", out.dusk); out.previewDaySeconds = f(time, "previewDaySeconds", out.previewDaySeconds);
        }
        JSONObject celestial = root.optJSONObject("celestial");
        if (celestial != null) {
            out.sunIntensity = f(celestial, "sunIntensity", out.sunIntensity);
            out.sunDiskIntensity = f(celestial, "sunDiskIntensity", out.sunDiskIntensity);
            out.moonIntensity = f(celestial, "moonIntensity", out.moonIntensity);
            out.moonPhase = f(celestial, "moonPhase", out.moonPhase);
            out.moonScale = f(celestial, "moonScale", out.moonScale);
            out.starsIntensity = f(celestial, "starsIntensity", out.starsIntensity);
        }
        JSONObject lightning = root.optJSONObject("lightning");
        if (lightning != null) {
            out.lightningFrequency = f(lightning, "frequency", out.lightningFrequency);
            out.lightningSpawnPeriod = f(lightning, "spawnPeriod", out.lightningSpawnPeriod);
            out.lightningDurationMin = f(lightning, "durationMin", out.lightningDurationMin);
            out.lightningDurationMax = f(lightning, "durationMax", out.lightningDurationMax);
            out.lightningLightIntensity = f(lightning, "lightIntensity", out.lightningLightIntensity);
            out.thunderDelayPerKm = f(lightning, "thunderDelayPerKm", out.thunderDelayPerKm);
        }
        JSONObject surface = root.optJSONObject("surface");
        if (surface != null) {
            out.wetCoverageSeconds = f(surface, "wetCoverageSeconds", out.wetCoverageSeconds);
            out.drySeconds = f(surface, "drySeconds", out.drySeconds);
            out.puddleCoverage = f(surface, "puddleCoverage", out.puddleCoverage);
            out.waterRoughness = f(surface, "waterRoughness", out.waterRoughness);
        }
        JSONObject qualities = root.optJSONObject("qualityTiers");
        if (qualities != null) {
            JSONArray names = qualities.names();
            if (names != null) for (int i = 0; i < names.length(); i++) {
                String name = names.optString(i, ""); JSONObject item = qualities.optJSONObject(name);
                if (item != null) out.addQuality(new SolumEnvironmentQuality(name, item.optInt("particleLimit", 1100),
                    item.optInt("cloudGroups", 8), item.optInt("starCount", 128), f(item, "renderScale", 0.86f)));
            }
        }
        JSONArray presets = root.optJSONArray("weatherPresets");
        if (presets != null) for (int i = 0; i < presets.length(); i++) {
            JSONObject item = presets.optJSONObject(i); if (item == null) continue;
            SolumWeatherState p = new SolumWeatherState();
            p.id = item.optString("id", ""); p.name = item.optString("name", p.id);
            p.cloudCoverage = f(item,"cloudCoverage",0); p.cloudDensity=f(item,"cloudDensity",0); p.cloudHeight=f(item,"cloudHeight",15); p.cloudThickness=f(item,"cloudThickness",0);
            p.fogDensity=f(item,"fogDensity",0); p.fogHeightFalloff=f(item,"fogHeightFalloff",0.065f); p.rain=f(item,"rain",0); p.snow=f(item,"snow",0); p.dust=f(item,"dust",0);
            p.windSpeed=f(item,"windSpeed",0); p.windGust=f(item,"windGust",0); p.windTurbulence=f(item,"windTurbulence",0); p.windDirectionDeg=f(item,"windDirectionDeg",180);
            p.lightningPotential=f(item,"lightningPotential",0); p.lightningEnabled=f(item,"lightningEnabled",0); p.wetnessTarget=f(item,"wetnessTarget",0); p.snowTarget=f(item,"snowTarget",0);
            p.ambientScale=f(item,"ambientScale",1); p.lightingScale=f(item,"lightingScale",1); p.exposure=f(item,"exposure",1); p.humidity=f(item,"humidity",0);
            out.addPreset(p);
        }
        out.validate();
        return out;
    }

    private static float f(JSONObject object, String key, float fallback) { return (float) object.optDouble(key, fallback); }
}
