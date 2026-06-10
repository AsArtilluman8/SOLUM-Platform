package com.solum.engine.skyweather;

public enum WeatherPreset {
    CLEAR("Clear"),
    CLOUDY("Cloudy"),
    RAIN("Rain"),
    SNOW("Snow"),
    STORM("Storm placeholder");

    public final String label;

    WeatherPreset(String label) {
        this.label = label;
    }

    public WeatherPreset next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
