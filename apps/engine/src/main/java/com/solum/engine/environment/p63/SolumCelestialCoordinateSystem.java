package com.solum.engine.environment.p63;

/**
 * Canonical P63 celestial basis.
 *
 * Filament world space is right-handed: +Y is up, +X is east/right and -Z is north/forward.
 * Body directions point from the observer toward the body. Filament directional-light vectors
 * point in the direction travelled by the light, therefore they are the exact inverse.
 */
public final class SolumCelestialCoordinateSystem {
    public static final float SKY_RADIUS = 90.0f;
    private static final float MAX_SOLAR_ELEVATION_DEGREES = 65.0f;

    private SolumCelestialCoordinateSystem() { }

    public static void update(float timeHundredths, float sunElevationOffsetDegrees,
                              SolumEnvironmentLightingState out) {
        update(timeHundredths, sunElevationOffsetDegrees, 0.0f, out);
    }

    public static void update(float timeHundredths, float sunElevationOffsetDegrees,
                              float moonElevationOffsetDegrees, SolumEnvironmentLightingState out) {
        if (out == null) throw new IllegalArgumentException("celestial_lighting_state_missing");
        float hours = decimalHours(timeHundredths);
        double orbit = (hours - 6.0) / 24.0 * Math.PI * 2.0;
        double maxElevation = Math.toRadians(MAX_SOLAR_ELEVATION_DEGREES);

        float east = (float)Math.cos(orbit);
        float meridian = (float)Math.sin(orbit);
        float sunX = east;
        float sunY = meridian * (float)Math.sin(maxElevation);
        float sunZ = -meridian * (float)Math.cos(maxElevation);
        normalize(out.sunVisualDirection, sunX, sunY, sunZ);
        applyElevationOffset(out.sunVisualDirection, sunElevationOffsetDegrees);
        normalize(out.moonVisualDirection, -east,
            -meridian * (float)Math.sin(maxElevation),
            meridian * (float)Math.cos(maxElevation));
        applyElevationOffset(out.moonVisualDirection, moonElevationOffsetDegrees);

        invert(out.sunDirection, out.sunVisualDirection);
        invert(out.moonDirection, out.moonVisualDirection);
        out.sunElevationDegrees = elevationDegrees(out.sunVisualDirection);
        out.sunAzimuthDegrees = azimuthDegrees(out.sunVisualDirection);
        out.moonElevationDegrees = elevationDegrees(out.moonVisualDirection);
        out.moonAzimuthDegrees = azimuthDegrees(out.moonVisualDirection);
        out.sunElevation = out.sunVisualDirection[1];
        out.moonElevation = out.moonVisualDirection[1];
        out.sunAboveHorizon = out.sunElevationDegrees > 0.0f;
        out.moonAboveHorizon = out.moonElevationDegrees > 0.0f;
    }

    public static float decimalHours(float timeHundredths) {
        float wrapped = SolumTimeSystem.wrap(timeHundredths);
        int hour = Math.min(23, Math.max(0, (int)(wrapped / 100.0f)));
        return hour + (wrapped - hour * 100.0f) / 100.0f;
    }

    public static void positionRelativeToCamera(float[] out, float cameraX, float cameraY,
                                                float cameraZ, float[] bodyDirection, float radius) {
        if (out == null || out.length < 3 || bodyDirection == null || bodyDirection.length < 3) {
            throw new IllegalArgumentException("celestial_position_arguments_invalid");
        }
        float safeRadius = finiteClamp(radius, 10.0f, 120.0f, SKY_RADIUS);
        out[0] = cameraX + bodyDirection[0] * safeRadius;
        out[1] = cameraY + bodyDirection[1] * safeRadius;
        out[2] = cameraZ + bodyDirection[2] * safeRadius;
    }

    public static boolean consistentBodyAndLightDirection(float[] bodyDirection, float[] lightDirection) {
        if (bodyDirection == null || lightDirection == null || bodyDirection.length < 3 || lightDirection.length < 3) return false;
        float dot = bodyDirection[0] * lightDirection[0]
            + bodyDirection[1] * lightDirection[1]
            + bodyDirection[2] * lightDirection[2];
        return Math.abs(dot + 1.0f) < 0.0005f;
    }

    public static void focusTarget(float[] out, float[] eye, float[] bodyDirection, float distance) {
        if (out == null || out.length < 3 || eye == null || eye.length < 3
                || bodyDirection == null || bodyDirection.length < 3) {
            throw new IllegalArgumentException("celestial_focus_arguments_invalid");
        }
        float safeDistance = finiteClamp(distance, 1.0f, SKY_RADIUS, 12.0f);
        out[0] = eye[0] + bodyDirection[0] * safeDistance;
        out[1] = eye[1] + bodyDirection[1] * safeDistance;
        out[2] = eye[2] + bodyDirection[2] * safeDistance;
    }

    public static boolean focusDirectionAligned(float[] eye, float[] target, float[] bodyDirection) {
        if (eye == null || target == null || bodyDirection == null
                || eye.length < 3 || target.length < 3 || bodyDirection.length < 3) return false;
        float dx = target[0] - eye[0];
        float dy = target[1] - eye[1];
        float dz = target[2] - eye[2];
        float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Float.isFinite(length) || length < 0.0001f) return false;
        float dot = dx / length * bodyDirection[0] + dy / length * bodyDirection[1]
            + dz / length * bodyDirection[2];
        return dot > 0.9995f;
    }

    private static void applyElevationOffset(float[] direction, float offsetDegrees) {
        float elevation = (float)Math.asin(clamp(direction[1], -1.0f, 1.0f));
        elevation += (float)Math.toRadians(finiteClamp(offsetDegrees, -15.0f, 15.0f, 0.0f));
        elevation = clamp(elevation, (float)Math.toRadians(-89.5), (float)Math.toRadians(89.5));
        float azimuth = (float)Math.atan2(direction[0], -direction[2]);
        float horizontal = (float)Math.cos(elevation);
        normalize(direction, (float)Math.sin(azimuth) * horizontal,
            (float)Math.sin(elevation), -(float)Math.cos(azimuth) * horizontal);
    }

    private static float elevationDegrees(float[] direction) {
        return (float)Math.toDegrees(Math.asin(clamp(direction[1], -1.0f, 1.0f)));
    }

    private static float azimuthDegrees(float[] direction) {
        float degrees = (float)Math.toDegrees(Math.atan2(direction[0], -direction[2]));
        return degrees < 0.0f ? degrees + 360.0f : degrees;
    }

    private static void invert(float[] out, float[] direction) {
        out[0] = -direction[0]; out[1] = -direction[1]; out[2] = -direction[2];
    }

    private static void normalize(float[] out, float x, float y, float z) {
        float length = (float)Math.sqrt(x * x + y * y + z * z);
        if (!Float.isFinite(length) || length < 0.0001f) {
            out[0] = 0.0f; out[1] = 1.0f; out[2] = 0.0f;
            return;
        }
        out[0] = x / length; out[1] = y / length; out[2] = z / length;
    }

    private static float finiteClamp(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) return fallback;
        return clamp(value, min, max);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
