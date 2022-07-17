package fr.dynamx.utils.debug;

public enum TerrainDebugRenderer {
    BLOCKS(0, 0, 1),
    STAIRS(0.5f, 0, 1),
    DYNAMXBLOCKS(0, 1, 0),
    SLOPES(1, 1, 0),
    CUSTOM_SLOPE(1, 0, 0);

    private final float r, g, b;

    TerrainDebugRenderer(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public float getR() {
        return r;
    }

    public float getG() {
        return g;
    }

    public float getB() {
        return b;
    }
}
