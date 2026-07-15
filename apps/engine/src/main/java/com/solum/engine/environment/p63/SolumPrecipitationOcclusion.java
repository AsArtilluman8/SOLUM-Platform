package com.solum.engine.environment.p63;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SolumPrecipitationOcclusion {
    private final List<SolumInteriorExclusionVolume> exclusions = new ArrayList<>();
    private final int width;
    private final int depth;
    private final float cellSize;
    private final float originX;
    private final float originZ;
    private final float[] roofHeight;

    public SolumPrecipitationOcclusion(int width, int depth, float cellSize, float originX, float originZ) {
        this.width = width; this.depth = depth; this.cellSize = cellSize;
        this.originX = originX; this.originZ = originZ;
        this.roofHeight = new float[Math.max(1, width * depth)];
        for (int i = 0; i < roofHeight.length; i++) roofHeight[i] = Float.NaN;
    }

    public void addExclusion(SolumInteriorExclusionVolume volume) { if (volume != null) exclusions.add(volume); }
    public List<SolumInteriorExclusionVolume> getExclusions() { return Collections.unmodifiableList(exclusions); }

    public void setRoofHeight(int x, int z, float height) {
        if (x >= 0 && x < width && z >= 0 && z < depth) roofHeight[z * width + x] = height;
    }

    public boolean isInterior(float x, float y, float z) {
        for (SolumInteriorExclusionVolume volume : exclusions) if (volume.contains(x, y, z)) return true;
        return false;
    }

    public boolean hasRoofAbove(float x, float y, float z) {
        int ix = (int) Math.floor((x - originX) / cellSize);
        int iz = (int) Math.floor((z - originZ) / cellSize);
        if (ix < 0 || ix >= width || iz < 0 || iz >= depth) return false;
        float height = roofHeight[iz * width + ix];
        return !Float.isNaN(height) && height > y;
    }

    public boolean blocksPrecipitation(float x, float y, float z) {
        if (isInterior(x, y, z) || hasRoofAbove(x, y, z)) return true;
        for (SolumInteriorExclusionVolume volume : exclusions) if (volume.covers(x, z) && volume.maxY > y) return true;
        return false;
    }
}
