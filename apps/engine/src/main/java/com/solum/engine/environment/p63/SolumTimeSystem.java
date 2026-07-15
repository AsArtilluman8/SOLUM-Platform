package com.solum.engine.environment.p63;

public final class SolumTimeSystem {
    private float time = 960.0f;
    private float speed = 1.0f;
    private float previewDaySeconds = 240.0f;
    private boolean paused = true;
    private boolean transitioning;
    private float transitionFrom;
    private float transitionDelta;
    private float transitionElapsed;
    private float transitionDuration;

    public void configure(float initial, float previewSeconds) { time = wrap(initial); previewDaySeconds = Math.max(1.0f, previewSeconds); }
    public float getTime() { return time; }
    public float getDecimalHours() { return time / 100.0f; }
    public boolean isPaused() { return paused; }
    public float getSpeed() { return speed; }
    public boolean isTransitioning() { return transitioning; }
    public void setPaused(boolean value) { paused = value; }
    public void setSpeed(float value) { speed = Math.max(0.0f, Math.min(100.0f, value)); }
    public void set(float value) { time = wrap(value); transitioning = false; }

    public void transitionTo(float value, float duration) {
        float target = wrap(value);
        float delta = target - time;
        if (delta > 1200.0f) delta -= 2400.0f;
        if (delta < -1200.0f) delta += 2400.0f;
        if (duration <= 0.0f) { set(target); return; }
        transitionFrom = time; transitionDelta = delta; transitionElapsed = 0.0f;
        transitionDuration = duration; transitioning = true;
    }

    public float update(float deltaSeconds) {
        float dt = Math.max(0.0f, Math.min(0.1f, deltaSeconds));
        if (transitioning) {
            transitionElapsed += dt;
            float raw = Math.min(1.0f, transitionElapsed / transitionDuration);
            float eased = raw * raw * (3.0f - 2.0f * raw);
            time = wrap(transitionFrom + transitionDelta * eased);
            if (raw >= 1.0f) transitioning = false;
        } else if (!paused && speed > 0.0f) {
            time = wrap(time + dt * 2400.0f / previewDaySeconds * speed);
        }
        return time;
    }

    public static float wrap(float value) {
        float out = value % 2400.0f;
        return out < 0.0f ? out + 2400.0f : out;
    }
}
