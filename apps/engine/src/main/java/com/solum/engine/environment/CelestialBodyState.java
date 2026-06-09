package com.solum.engine.environment;

public class CelestialBodyState {
    private boolean enabled;
    private boolean visible;
    private float azimuthDeg;
    private float elevationDeg;
    private float directionX;
    private float directionY;
    private float directionZ;
    private float intensityLux;
    private float colorTemperatureKelvin;
    private float phase;
    private String status = "not_applied";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean value) { visible = value; }
    public float getAzimuthDeg() { return azimuthDeg; }
    public void setAzimuthDeg(float value) { azimuthDeg = value; }
    public float getElevationDeg() { return elevationDeg; }
    public void setElevationDeg(float value) { elevationDeg = value; }
    public float getDirectionX() { return directionX; }
    public float getDirectionY() { return directionY; }
    public float getDirectionZ() { return directionZ; }
    public void setDirection(float x, float y, float z) {
        directionX = x;
        directionY = y;
        directionZ = z;
    }
    public float getIntensityLux() { return intensityLux; }
    public void setIntensityLux(float value) { intensityLux = value; }
    public float getColorTemperatureKelvin() { return colorTemperatureKelvin; }
    public void setColorTemperatureKelvin(float value) { colorTemperatureKelvin = value; }
    public float getPhase() { return phase; }
    public void setPhase(float value) { phase = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value == null ? "" : value; }

    public void copyFrom(CelestialBodyState other) {
        if (other == null) return;
        enabled = other.enabled;
        visible = other.visible;
        azimuthDeg = other.azimuthDeg;
        elevationDeg = other.elevationDeg;
        directionX = other.directionX;
        directionY = other.directionY;
        directionZ = other.directionZ;
        intensityLux = other.intensityLux;
        colorTemperatureKelvin = other.colorTemperatureKelvin;
        phase = other.phase;
        status = other.status;
    }
}
