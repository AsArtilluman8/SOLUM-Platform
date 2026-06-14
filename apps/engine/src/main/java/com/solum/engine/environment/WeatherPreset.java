package com.solum.engine.environment;

public final class WeatherPreset {
    public static final WeatherPreset RAIN = new WeatherPreset(
        "rain",
        "Rain",
        7.5f,
        7.0f,
        0.0f,
        4.0f,
        3.0f,
        3.0f,
        1.0f
    );

    public static final WeatherPreset CLEAR = new WeatherPreset(
        "clear", "Clear", 0.8f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f
    );

    public static final WeatherPreset SNOW = new WeatherPreset(
        "snow", "Snow", 6.5f, 0.0f, 6.0f, 0.0f, 2.5f, 2.0f, 0.0f
    );

    public static final WeatherPreset STORM = new WeatherPreset(
        "storm", "Storm", 9.0f, 9.0f, 0.0f, 8.0f, 7.0f, 4.5f, 1.0f
    );

    public static final WeatherPreset OVERCAST = new WeatherPreset(
        "overcast", "Overcast", 8.0f, 0.0f, 0.0f, 0.0f, 2.5f, 2.0f, 0.0f
    );

    public static final WeatherPreset NIGHT = new WeatherPreset(
        "night", "Night", 2.5f, 0.0f, 0.0f, 0.0f, 1.2f, 0.8f, 0.0f
    );

    private final String id;
    private final String label;
    private final float cloudCoverage;
    private final float rainIntensity;
    private final float snowIntensity;
    private final float thunderIntensity;
    private final float windIntensity;
    private final float fogDensity;
    private final float materialWetness;

    public WeatherPreset(String id, String label, float cloudCoverage, float rainIntensity,
            float snowIntensity, float thunderIntensity, float windIntensity, float fogDensity, float materialWetness) {
        this.id = id;
        this.label = label;
        this.cloudCoverage = cloudCoverage;
        this.rainIntensity = rainIntensity;
        this.snowIntensity = snowIntensity;
        this.thunderIntensity = thunderIntensity;
        this.windIntensity = windIntensity;
        this.fogDensity = fogDensity;
        this.materialWetness = materialWetness;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public float getCloudCoverage() { return cloudCoverage; }
    public float getRainIntensity() { return rainIntensity; }
    public float getSnowIntensity() { return snowIntensity; }
    public float getThunderIntensity() { return thunderIntensity; }
    public float getWindIntensity() { return windIntensity; }
    public float getFogDensity() { return fogDensity; }
    public float getMaterialWetness() { return materialWetness; }

    public static WeatherPreset fromId(String value) {
        String id = value == null ? "clear" : value.trim().toLowerCase();
        if ("rain".equals(id)) return RAIN;
        if ("snow".equals(id)) return SNOW;
        if ("storm".equals(id)) return STORM;
        if ("overcast".equals(id)) return OVERCAST;
        if ("night".equals(id)) return NIGHT;
        if ("clear".equals(id)) return CLEAR;
        return null;
    }
}
