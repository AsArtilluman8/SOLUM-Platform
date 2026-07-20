package com.solum.engine.environment.p63;

/**
 * Exact default/manual Ultra Dynamic Sky Sun trajectory translated to Filament coordinates.
 *
 * <p>Source: {@code Ultra_Dynamic_Sky.Cache Sun and Moon Orientation},
 * {@code Set Time Cycle Degrees}, and {@code Sun Z Vector}; machine-readable evidence lives in
 * {@code P63_10_UDS_SUN_VALUES_CONTRACT.json}. This class intentionally does not approximate the
 * separate real-date/latitude Sun branch. Callers must leave {@link Inputs#simulateRealSun} false
 * until that source-backed branch is implemented.</p>
 */
public final class SolumUdsSunTrajectory {
    public static final String CONTRACT_STATUS = "VERIFIED_UDS_DEFAULT_MANUAL_SUN_TRAJECTORY";
    public static final String REAL_SUN_STATUS = "NOT_IMPLEMENTED_FAIL_CLOSED";
    public static final String MANUAL_TARGET_STATUS = "NOT_IMPLEMENTED_MANUAL_TARGET_FAIL_CLOSED";
    private static final double NORMAL_TOLERANCE = 0.0001;

    public static final class Inputs {
        public double timeOfDay = 960.0;
        public double dawnTime = 600.0;
        public double duskTime = 1800.0;
        public boolean daylightSavingsTime;
        public boolean simulateRealSun;
        public boolean simulateRealMoon;
        public boolean manuallyPositionSunTarget;
        public double sunTargetX;
        public double sunTargetY;
        public double sunTargetZ;
        public double sunPitchDegrees = 30.0;
        public double sunYawDegrees;
        public double sunVerticalOffset;
        public double extendDawnAndDusk;
        public double actorYawDegrees;
    }

    public static final class Output {
        public final float[] ueCachedSunVector = new float[3];
        public final float[] filamentLightDirection = new float[3];
        public final float[] filamentVisualDirection = new float[3];
        public final float[] ueCachedSunZVector = new float[3];
        public float timeInRange;
        public float timeCycleDegrees;
        public float extendDawnDuskZ = 1.0f;
        public boolean daytime;
        public String status = "NOT_EVALUATED";
    }

    private SolumUdsSunTrajectory() { }

    public static void evaluate(Inputs in, Output out) {
        if (in == null || out == null) throw new IllegalArgumentException("uds_sun_trajectory_arguments_missing");
        if (in.simulateRealSun) {
            zero(out);
            out.status = REAL_SUN_STATUS;
            return;
        }
        if (in.manuallyPositionSunTarget) {
            // UDS resolves a world-space target against the sky actor transform. SOLUM does not
            // yet expose that actor transform, so accepting only a target vector here would be a
            // silent coordinate-space approximation.
            zero(out);
            out.status = MANUAL_TARGET_STATUS;
            return;
        }

        double timeInRange = positiveModulo(
            finite(in.timeOfDay, 960.0) + (in.daylightSavingsTime ? -100.0 : 0.0) + 2400.0,
            2400.0);
        double dawn = finite(in.dawnTime, 600.0);
        double dusk = finite(in.duskTime, 1800.0);
        if (!(dawn >= 0.0 && dawn < dusk && dusk <= 2400.0)) {
            dawn = 600.0;
            dusk = 1800.0;
        }
        boolean daytime = timeInRange > dawn && timeInRange < dusk;
        double cycle;
        if (daytime) {
            cycle = mapRangeClamped(timeInRange, dawn, dusk, 90.0, 270.0);
        } else {
            double wrappedNight = timeInRange <= dawn ? timeInRange + 2400.0 : timeInRange;
            cycle = positiveModulo(mapRangeClamped(
                wrappedNight, dusk, dawn + 2400.0, 270.0, 450.0), 360.0);
        }

        double extendZ = 1.0;
        double extend = finite(in.extendDawnAndDusk, 0.0);
        if (extend > 0.0) {
            double base = mapRangeClamped(extend, 0.0, 5.0, 1.0, 0.03);
            double alpha = clamp(Math.pow(Math.abs(fraction(finite(in.timeOfDay, 960.0) / 1200.0) - 0.5) * 2.0, 2.0), 0.0, 1.0);
            extendZ = lerp(base, 1.0, alpha);
        }

        double[] candidate = new double[3];
        double pitch = Math.toRadians(finite(in.sunPitchDegrees, 30.0));
        double[] orbitAxis = {Math.cos(pitch), 0.0, Math.sin(pitch)};
        double[] pitchedBase = {-Math.sin(pitch), 0.0, Math.cos(pitch)};
        double[] timeRotated = new double[3];
        rotateAngleAxis(timeRotated, pitchedBase, Math.toRadians(cycle), orbitAxis);
        rotateAroundZ(candidate, timeRotated[0], timeRotated[1], timeRotated[2],
            finite(in.sunYawDegrees, 0.0) + finite(in.actorYawDegrees, 0.0));
        candidate[2] = (candidate[2] - finite(in.sunVerticalOffset, 0.0)) * extendZ;
        normalize(out.ueCachedSunVector, candidate[0], candidate[1], candidate[2]);

        double[] zReference = new double[3];
        rotateAroundZ(zReference, Math.cos(pitch), 0.0, Math.sin(pitch),
            finite(in.sunYawDegrees, 0.0) + finite(in.actorYawDegrees, 0.0));
        normalize(out.ueCachedSunZVector, zReference[0], zReference[1], zReference[2]);

        // Exact verified mapping: UE (X,Y,Z) -> Filament (Y,Z,-X). Cached UDS vector is the
        // directional-light ray direction; the visible body direction is its inverse.
        out.filamentLightDirection[0] = out.ueCachedSunVector[1];
        out.filamentLightDirection[1] = out.ueCachedSunVector[2];
        out.filamentLightDirection[2] = -out.ueCachedSunVector[0];
        out.filamentVisualDirection[0] = -out.filamentLightDirection[0];
        out.filamentVisualDirection[1] = -out.filamentLightDirection[1];
        out.filamentVisualDirection[2] = -out.filamentLightDirection[2];
        out.timeInRange = (float)timeInRange;
        out.timeCycleDegrees = (float)cycle;
        out.extendDawnDuskZ = (float)extendZ;
        out.daytime = daytime;
        out.status = CONTRACT_STATUS;
    }

    private static void rotateAngleAxis(double[] out, double[] vector, double angle, double[] axis) {
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        double dot = vector[0] * axis[0] + vector[1] * axis[1] + vector[2] * axis[2];
        out[0] = vector[0] * cosine + (axis[1] * vector[2] - axis[2] * vector[1]) * sine + axis[0] * dot * (1.0 - cosine);
        out[1] = vector[1] * cosine + (axis[2] * vector[0] - axis[0] * vector[2]) * sine + axis[1] * dot * (1.0 - cosine);
        out[2] = vector[2] * cosine + (axis[0] * vector[1] - axis[1] * vector[0]) * sine + axis[2] * dot * (1.0 - cosine);
    }

    private static void rotateAroundZ(double[] out, double x, double y, double z, double degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        out[0] = x * cosine - y * sine;
        out[1] = x * sine + y * cosine;
        out[2] = z;
    }

    private static void normalize(float[] out, double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (!Double.isFinite(length) || length < NORMAL_TOLERANCE) {
            out[0] = 0.0f; out[1] = 0.0f; out[2] = 0.0f;
            return;
        }
        out[0] = (float)(x / length); out[1] = (float)(y / length); out[2] = (float)(z / length);
    }

    private static double mapRangeClamped(double value, double inA, double inB, double outA, double outB) {
        if (inA == inB) return outA;
        return lerp(outA, outB, clamp((value - inA) / (inB - inA), 0.0, 1.0));
    }

    private static double positiveModulo(double value, double modulus) {
        double result = value % modulus;
        return result < 0.0 ? result + modulus : result;
    }

    private static double fraction(double value) { return value - Math.floor(value); }
    private static double lerp(double a, double b, double alpha) { return a + (b - a) * alpha; }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private static double finite(double value, double fallback) { return Double.isFinite(value) ? value : fallback; }

    private static void zero(Output out) {
        for (int index = 0; index < 3; index++) {
            out.ueCachedSunVector[index] = 0.0f;
            out.ueCachedSunZVector[index] = 0.0f;
            out.filamentLightDirection[index] = 0.0f;
            out.filamentVisualDirection[index] = 0.0f;
        }
        out.timeInRange = 0.0f;
        out.timeCycleDegrees = 0.0f;
        out.extendDawnDuskZ = 1.0f;
        out.daytime = false;
    }
}
