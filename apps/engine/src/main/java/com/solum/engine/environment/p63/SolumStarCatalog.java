package com.solum.engine.environment.p63;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SolumStarCatalog {
    public static final class Star {
        public final float x, y, z, size, brightness, temperature;
        Star(float x, float y, float z, float size, float brightness, float temperature) {
            this.x=x; this.y=y; this.z=z; this.size=size; this.brightness=brightness; this.temperature=temperature;
        }
    }

    private final List<Star> stars;

    public SolumStarCatalog(int seed, int count) {
        List<Star> result = new ArrayList<>();
        int state = seed == 0 ? 1 : seed;
        for (int i = 0; i < count; i++) {
            state = xorshift(state); float u = unsigned(state);
            state = xorshift(state); float v = unsigned(state);
            state = xorshift(state); float w = unsigned(state);
            float azimuth = u * (float) (Math.PI * 2.0);
            float milkyBand = (float) Math.pow(Math.abs(v - 0.5f) * 2.0f, 1.8f);
            float elevation = 0.12f + (1.0f - milkyBand) * 1.25f + (w - 0.5f) * 0.20f;
            float radius = 34.0f;
            float cos = (float) Math.cos(elevation);
            float x = (float) Math.sin(azimuth) * cos * radius;
            float y = (float) Math.sin(elevation) * radius;
            float z = -(float) Math.cos(azimuth) * cos * radius;
            state = xorshift(state); float size = 0.025f + (float) Math.pow(unsigned(state), 4.0) * 0.13f;
            state = xorshift(state); float brightness = 0.35f + unsigned(state) * 0.65f;
            state = xorshift(state); float temperature = 3200.0f + unsigned(state) * 6000.0f;
            result.add(new Star(x,y,z,size,brightness,temperature));
        }
        stars = Collections.unmodifiableList(result);
    }

    public List<Star> getStars() { return stars; }
    public long fingerprint() {
        long value = 1469598103934665603L;
        for (Star star : stars) {
            value ^= Float.floatToIntBits(star.x); value *= 1099511628211L;
            value ^= Float.floatToIntBits(star.y); value *= 1099511628211L;
            value ^= Float.floatToIntBits(star.size); value *= 1099511628211L;
        }
        return value;
    }
    private static int xorshift(int value) { value ^= value << 13; value ^= value >>> 17; value ^= value << 5; return value; }
    private static float unsigned(int value) { return (value & 0xffffffffL) / 4294967296.0f; }
}
