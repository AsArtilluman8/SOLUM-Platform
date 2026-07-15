package com.solum.engine.environment.p63;

/**
 * Deterministic owner for one camera gesture. A gesture can never change from pan to pinch
 * after ownership is acquired, so orbit, pan and dolly cannot leak into one another.
 */
public final class SolumCameraGestureState {
    public enum Mode { IDLE, ORBIT, TWO_FINGER_PENDING, PAN, PINCH, BLOCKED_UNTIL_UP }

    private static final float TAP_SLOP_PX = 10.0f;
    private static final float PAN_LOCK_PX = 7.0f;
    private static final float PINCH_LOCK_PX = 9.0f;

    private Mode mode = Mode.IDLE;
    private float downX;
    private float downY;
    private float initialMidX;
    private float initialMidY;
    private float initialSeparation;
    private float previousSeparation;
    private float pinchDelta;
    private boolean tapCandidate;

    public Mode beginPrimary(float x, float y) {
        mode = Mode.ORBIT;
        downX = x; downY = y; tapCandidate = true; pinchDelta = 0.0f;
        return mode;
    }

    public Mode beginTwo(float x0, float y0, float x1, float y1) {
        mode = Mode.TWO_FINGER_PENDING;
        initialMidX = (x0 + x1) * 0.5f;
        initialMidY = (y0 + y1) * 0.5f;
        initialSeparation = distance(x0, y0, x1, y1);
        previousSeparation = initialSeparation;
        pinchDelta = 0.0f;
        tapCandidate = false;
        return mode;
    }

    public Mode updatePrimary(float x, float y) {
        if (mode == Mode.ORBIT && distance(downX, downY, x, y) > TAP_SLOP_PX) tapCandidate = false;
        return mode;
    }

    public Mode updateTwo(float x0, float y0, float x1, float y1) {
        float midX = (x0 + x1) * 0.5f;
        float midY = (y0 + y1) * 0.5f;
        float separation = distance(x0, y0, x1, y1);
        if (mode == Mode.TWO_FINGER_PENDING) {
            float separationTravel = Math.abs(separation - initialSeparation);
            float midpointTravel = distance(initialMidX, initialMidY, midX, midY);
            if (separationTravel >= PINCH_LOCK_PX && separationTravel >= midpointTravel) {
                mode = Mode.PINCH;
            } else if (midpointTravel >= PAN_LOCK_PX) {
                mode = Mode.PAN;
            }
        }
        pinchDelta = mode == Mode.PINCH ? previousSeparation - separation : 0.0f;
        previousSeparation = separation;
        return mode;
    }

    public void blockUntilAllPointersUp() {
        mode = Mode.BLOCKED_UNTIL_UP;
        tapCandidate = false;
        pinchDelta = 0.0f;
    }

    public void end() {
        mode = Mode.IDLE;
        tapCandidate = false;
        pinchDelta = 0.0f;
    }

    public Mode getMode() { return mode; }
    public boolean changesOrbit() { return mode == Mode.ORBIT; }
    public boolean changesTarget() { return mode == Mode.PAN; }
    public boolean changesDistance() { return mode == Mode.PINCH; }
    public boolean isTapCandidate() { return mode == Mode.ORBIT && tapCandidate; }
    public float consumePinchDelta() { float value = pinchDelta; pinchDelta = 0.0f; return value; }
    public float midpointX(float x0, float x1) { return (x0 + x1) * 0.5f; }
    public float midpointY(float y0, float y1) { return (y0 + y1) * 0.5f; }

    private static float distance(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }
}
