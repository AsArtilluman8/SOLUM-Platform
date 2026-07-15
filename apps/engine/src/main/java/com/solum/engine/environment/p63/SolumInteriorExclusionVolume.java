package com.solum.engine.environment.p63;

public final class SolumInteriorExclusionVolume {
    public final String id;
    public final float minX, minY, minZ, maxX, maxY, maxZ;

    public SolumInteriorExclusionVolume(String id, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.id = id;
        this.minX = Math.min(minX, maxX); this.maxX = Math.max(minX, maxX);
        this.minY = Math.min(minY, maxY); this.maxY = Math.max(minY, maxY);
        this.minZ = Math.min(minZ, maxZ); this.maxZ = Math.max(minZ, maxZ);
    }

    public boolean contains(float x, float y, float z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean covers(float x, float z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
