package com.solum.engine.environment.p63;

/** UE FRichCurve evaluator for extracted, unweighted UDS curve keys. */
public final class SolumUdsRichCurve {
    public static final int LINEAR = 0;
    public static final int CONSTANT = 1;
    public static final int CUBIC = 2;

    public static final class Key {
        public final double time;
        public final double value;
        public final double arriveTangent;
        public final double leaveTangent;
        public final int interpolation;

        public Key(double time, double value, double arriveTangent, double leaveTangent,
                   int interpolation) {
            this.time = time;
            this.value = value;
            this.arriveTangent = arriveTangent;
            this.leaveTangent = leaveTangent;
            this.interpolation = interpolation;
        }
    }

    private final Key[] keys;

    public SolumUdsRichCurve(Key... keys) {
        if (keys == null || keys.length == 0) throw new IllegalArgumentException("uds_curve_keys_missing");
        this.keys = keys.clone();
        for (int index = 0; index < this.keys.length; index++) {
            Key key = this.keys[index];
            if (key == null || !Double.isFinite(key.time) || !Double.isFinite(key.value)
                    || !Double.isFinite(key.arriveTangent) || !Double.isFinite(key.leaveTangent)) {
                throw new IllegalArgumentException("uds_curve_key_invalid_" + index);
            }
            if (index > 0 && this.keys[index - 1].time >= key.time) {
                throw new IllegalArgumentException("uds_curve_key_order_invalid_" + index);
            }
        }
    }

    /**
     * Mirrors UE FRichCurve::Eval for the extracted constant/linear/unweighted-cubic subset.
     * Null/default pre/post-infinity modes use the endpoint value, matching the source assets.
     */
    public float evaluate(double time) {
        double input = Double.isFinite(time) ? time : keys[0].time;
        if (input <= keys[0].time) return (float)keys[0].value;
        Key last = keys[keys.length - 1];
        if (input >= last.time) return (float)last.value;
        for (int index = 1; index < keys.length; index++) {
            Key next = keys[index];
            if (input >= next.time) continue;
            Key previous = keys[index - 1];
            double difference = next.time - previous.time;
            double alpha = (input - previous.time) / difference;
            if (previous.interpolation == CONSTANT) return (float)previous.value;
            if (previous.interpolation == LINEAR) {
                return (float)lerp(previous.value, next.value, alpha);
            }
            if (previous.interpolation != CUBIC) {
                throw new IllegalStateException("uds_curve_interpolation_unsupported_" + previous.interpolation);
            }
            // UE CubicInterp uses tangents scaled by the segment duration for unweighted keys.
            return (float)cubic(previous.value, previous.leaveTangent * difference,
                next.value, next.arriveTangent * difference, alpha);
        }
        return (float)last.value;
    }

    private static double cubic(double p0, double t0, double p1, double t1, double alpha) {
        double alpha2 = alpha * alpha;
        double alpha3 = alpha2 * alpha;
        return (2.0 * alpha3 - 3.0 * alpha2 + 1.0) * p0
            + (alpha3 - 2.0 * alpha2 + alpha) * t0
            + (-2.0 * alpha3 + 3.0 * alpha2) * p1
            + (alpha3 - alpha2) * t1;
    }

    private static double lerp(double a, double b, double alpha) {
        return a + (b - a) * alpha;
    }
}
