package com.solum.engine.render;

public class RenderActualState {
    public static final String REQUESTED = "requested";
    public static final String APPLIED = "applied";
    public static final String NOT_VERIFIED = "not_verified";
    public static final String NOT_EXPOSED = "not_exposed";
    public static final String REQUIRES_RECREATE = "requires_recreate";
    public static final String FAILED = "failed";

    private boolean actualDynamicResolution = true;
    private int actualMsaa = 2;
    private boolean actualTaa = false;
    private String actualAo = "OFF";
    private String actualBloom = "OFF";
    private String actualShadows = "OFF";
    private String dynamicResolutionApplyStatus = NOT_VERIFIED;
    private String msaaApplyStatus = NOT_VERIFIED;
    private String taaApplyStatus = NOT_VERIFIED;
    private String aoApplyStatus = NOT_VERIFIED;
    private String bloomApplyStatus = NOT_VERIFIED;
    private String shadowsApplyStatus = NOT_VERIFIED;
    private String fxaaApplyStatus = NOT_VERIFIED;
    private String ditheringApplyStatus = NOT_VERIFIED;
    private String ssrApplyStatus = NOT_VERIFIED;
    private String refractionApplyStatus = NOT_VERIFIED;
    private String renderScaleApplyStatus = NOT_VERIFIED;
    private String colorApplyStatus = "activity_local";
    private String fogApplyStatus = "activity_local";
    private float actualBloomStrength = 0.0f;
    private float actualBloomHighlight = 1000.0f;
    private float dynamicMinScale = 0.72f;
    private float dynamicMaxScale = 0.95f;

    public boolean isActualDynamicResolution() { return actualDynamicResolution; }
    public void setActualDynamicResolution(boolean value) { actualDynamicResolution = value; }
    public int getActualMsaa() { return actualMsaa; }
    public void setActualMsaa(int value) { actualMsaa = value; }
    public boolean isActualTaa() { return actualTaa; }
    public void setActualTaa(boolean value) { actualTaa = value; }
    public String getActualAo() { return actualAo; }
    public void setActualAo(String value) { actualAo = value; }
    public String getActualBloom() { return actualBloom; }
    public void setActualBloom(String value) { actualBloom = value; }
    public String getActualShadows() { return actualShadows; }
    public void setActualShadows(String value) { actualShadows = value; }
    public String getDynamicResolutionApplyStatus() { return dynamicResolutionApplyStatus; }
    public void setDynamicResolutionApplyStatus(String value) { dynamicResolutionApplyStatus = value; }
    public String getMsaaApplyStatus() { return msaaApplyStatus; }
    public void setMsaaApplyStatus(String value) { msaaApplyStatus = value; }
    public String getTaaApplyStatus() { return taaApplyStatus; }
    public void setTaaApplyStatus(String value) { taaApplyStatus = value; }
    public String getAoApplyStatus() { return aoApplyStatus; }
    public void setAoApplyStatus(String value) { aoApplyStatus = value; }
    public String getBloomApplyStatus() { return bloomApplyStatus; }
    public void setBloomApplyStatus(String value) { bloomApplyStatus = value; }
    public String getShadowsApplyStatus() { return shadowsApplyStatus; }
    public void setShadowsApplyStatus(String value) { shadowsApplyStatus = value; }
    public String getFxaaApplyStatus() { return fxaaApplyStatus; }
    public void setFxaaApplyStatus(String value) { fxaaApplyStatus = value; }
    public String getDitheringApplyStatus() { return ditheringApplyStatus; }
    public void setDitheringApplyStatus(String value) { ditheringApplyStatus = value; }
    public String getSsrApplyStatus() { return ssrApplyStatus; }
    public void setSsrApplyStatus(String value) { ssrApplyStatus = value; }
    public String getRefractionApplyStatus() { return refractionApplyStatus; }
    public void setRefractionApplyStatus(String value) { refractionApplyStatus = value; }
    public String getRenderScaleApplyStatus() { return renderScaleApplyStatus; }
    public void setRenderScaleApplyStatus(String value) { renderScaleApplyStatus = value; }
    public String getColorApplyStatus() { return colorApplyStatus; }
    public void setColorApplyStatus(String value) { colorApplyStatus = value; }
    public String getFogApplyStatus() { return fogApplyStatus; }
    public void setFogApplyStatus(String value) { fogApplyStatus = value; }
    public float getActualBloomStrength() { return actualBloomStrength; }
    public void setActualBloomStrength(float value) { actualBloomStrength = value; }
    public float getActualBloomHighlight() { return actualBloomHighlight; }
    public void setActualBloomHighlight(float value) { actualBloomHighlight = value; }
    public float getDynamicMinScale() { return dynamicMinScale; }
    public void setDynamicMinScale(float value) { dynamicMinScale = value; }
    public float getDynamicMaxScale() { return dynamicMaxScale; }
    public void setDynamicMaxScale(float value) { dynamicMaxScale = value; }
}
