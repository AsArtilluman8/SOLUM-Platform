package com.solum.engine.environment;

public final class WeatherPreset {
    public static final WeatherPreset RAIN = new WeatherPreset(
        "rain",
        "Rain",
        7.5f,
        7.0f,
        4.0f,
        3.0f,
        3.0f,
        1.0f
    );

    private final String id;
    private final String label;
    private final float cloudCoverage;
    private final float rainIntensity;
    private final float thunderIntensity;
    private final float windIntensity;
    private final float fogDensity;
    private final float materialWetness;

    public WeatherPreset(String id, String label, float cloudCoverage, float rainIntensity,
            float thunderIntensity, float windIntensity, float fogDensity, float materialWetness) {
        this.id = id;
        this.label = label;
        this.cloudCoverage = cloudCoverage;
        this.rainIntensity = rainIntensity;
        this.thunderIntensity = thunderIntensity;
        this.windIntensity = windIntensity;
        this.fogDensity = fogDensity;
        this.materialWetness = materialWetness;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public float getCloudCoverage() { return cloudCoverage; }
    public float getRainIntensity() { return rainIntensity; }
    public float getThunderIntensity() { return thunderIntensity; }
    public float getWindIntensity() { return windIntensity; }
    public float getFogDensity() { return fogDensity; }
    public float getMaterialWetness() { return materialWetness; }
}
