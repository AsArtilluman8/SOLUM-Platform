package com.solum.engine.environment;

public final class WeatherVfxRecipe {
    private final String id;
    private final float spawnRate;
    private final float velocity;
    private final float lifetime;
    private final float windAffect;
    private final float splashRate;
    private final float rippleRate;
    private final float materialWetnessAffect;

    public WeatherVfxRecipe(String id, float spawnRate, float velocity, float lifetime,
            float windAffect, float splashRate, float rippleRate, float materialWetnessAffect) {
        this.id = id;
        this.spawnRate = spawnRate;
        this.velocity = velocity;
        this.lifetime = lifetime;
        this.windAffect = windAffect;
        this.splashRate = splashRate;
        this.rippleRate = rippleRate;
        this.materialWetnessAffect = materialWetnessAffect;
    }

    public String getId() { return id; }
    public float getSpawnRate() { return spawnRate; }
    public float getVelocity() { return velocity; }
    public float getLifetime() { return lifetime; }
    public float getWindAffect() { return windAffect; }
    public float getSplashRate() { return splashRate; }
    public float getRippleRate() { return rippleRate; }
    public float getMaterialWetnessAffect() { return materialWetnessAffect; }

    public static WeatherVfxRecipe rainFallback(WeatherRuntimeParameters weather) {
        if (weather == null || (weather.getRainIntensity() <= 0.0f && weather.getSnowIntensity() <= 0.0f)) {
            return new WeatherVfxRecipe("none", 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        float rain = weather.getRainIntensity();
        float snow = weather.getSnowIntensity();
        return new WeatherVfxRecipe(
            rain > 0.0f ? "rain_native_recipe_not_niagara" : "snow_native_recipe_not_niagara",
            Math.max(rain * 90.0f, snow * 45.0f),
            rain > 0.0f ? 18.0f + rain * 1.4f : 3.0f + snow * 0.8f,
            rain > 0.0f ? 1.0f : 3.0f,
            weather.getWindIntensity() / 10.0f,
            rain * 5.0f,
            rain * 3.0f,
            weather.getMaterialWetness()
        );
    }
}
