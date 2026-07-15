package com.solum.engine.environment.p63;

public final class SolumWeatherState {
    public String id = "Partly_Cloudy";
    public String name = "Partly Cloudy";
    public float cloudCoverage;
    public float cloudDensity;
    public float cloudHeight = 15.0f;
    public float cloudThickness;
    public float fogDensity;
    public float fogHeightFalloff = 0.065f;
    public float rain;
    public float snow;
    public float dust;
    public float windSpeed;
    public float windGust;
    public float windTurbulence;
    public float windDirectionDeg = 180.0f;
    public float lightningPotential;
    public float lightningEnabled;
    public float wetnessTarget;
    public float snowTarget;
    public float ambientScale = 1.0f;
    public float lightingScale = 1.0f;
    public float exposure = 1.0f;
    public float humidity;

    public SolumWeatherState copy() {
        SolumWeatherState out = new SolumWeatherState();
        out.set(this);
        return out;
    }

    public void set(SolumWeatherState other) {
        id = other.id; name = other.name;
        cloudCoverage = other.cloudCoverage; cloudDensity = other.cloudDensity;
        cloudHeight = other.cloudHeight; cloudThickness = other.cloudThickness;
        fogDensity = other.fogDensity; fogHeightFalloff = other.fogHeightFalloff;
        rain = other.rain; snow = other.snow; dust = other.dust;
        windSpeed = other.windSpeed; windGust = other.windGust; windTurbulence = other.windTurbulence;
        windDirectionDeg = other.windDirectionDeg; lightningPotential = other.lightningPotential;
        lightningEnabled = other.lightningEnabled; wetnessTarget = other.wetnessTarget;
        snowTarget = other.snowTarget; ambientScale = other.ambientScale;
        lightingScale = other.lightingScale; exposure = other.exposure; humidity = other.humidity;
    }

    public void interpolate(SolumWeatherState from, SolumWeatherState to, float alpha) {
        float t = smooth(alpha);
        id = alpha < 1.0f ? from.id + "→" + to.id : to.id;
        name = alpha < 1.0f ? from.name + " → " + to.name : to.name;
        cloudCoverage = lerp(from.cloudCoverage, to.cloudCoverage, t);
        cloudDensity = lerp(from.cloudDensity, to.cloudDensity, t);
        cloudHeight = lerp(from.cloudHeight, to.cloudHeight, t);
        cloudThickness = lerp(from.cloudThickness, to.cloudThickness, t);
        fogDensity = lerp(from.fogDensity, to.fogDensity, t);
        fogHeightFalloff = lerp(from.fogHeightFalloff, to.fogHeightFalloff, t);
        rain = lerp(from.rain, to.rain, t); snow = lerp(from.snow, to.snow, t); dust = lerp(from.dust, to.dust, t);
        windSpeed = lerp(from.windSpeed, to.windSpeed, t); windGust = lerp(from.windGust, to.windGust, t);
        windTurbulence = lerp(from.windTurbulence, to.windTurbulence, t);
        windDirectionDeg = angularLerp(from.windDirectionDeg, to.windDirectionDeg, t);
        lightningPotential = lerp(from.lightningPotential, to.lightningPotential, t);
        lightningEnabled = alpha < 0.5f ? from.lightningEnabled : to.lightningEnabled;
        wetnessTarget = lerp(from.wetnessTarget, to.wetnessTarget, t);
        snowTarget = lerp(from.snowTarget, to.snowTarget, t);
        ambientScale = lerp(from.ambientScale, to.ambientScale, t);
        lightingScale = lerp(from.lightingScale, to.lightingScale, t);
        exposure = lerp(from.exposure, to.exposure, t);
        humidity = lerp(from.humidity, to.humidity, t);
    }

    static float clamp(float value) { return Math.max(0.0f, Math.min(1.0f, value)); }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float smooth(float value) { float t = clamp(value); return t * t * (3.0f - 2.0f * t); }
    static float angularLerp(float a, float b, float t) {
        float delta = ((b - a + 540.0f) % 360.0f) - 180.0f;
        float out = (a + delta * t) % 360.0f;
        return out < 0.0f ? out + 360.0f : out;
    }
}
