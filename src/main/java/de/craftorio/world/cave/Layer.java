package de.craftorio.world.cave;

/** The two unlockable underground layers. Pure data (heights and hall shape) so it can be unit tested. */
public enum Layer {
    CAVES(CaveLayers.CAVE_BOTTOM, CaveLayers.CAP_ONE_BOTTOM - 1, new CaveShape.Params(7, 3, 25, 9, 0.42, 1)),
    /** Lower, tighter tunnels with many pillars. */
    MINES(CaveLayers.MINE_BOTTOM, CaveLayers.CAP_TWO_BOTTOM - 1, new CaveShape.Params(-51, 2, -39, 4, 0.30, 2));

    private final int minY;
    private final int maxY;
    private final CaveShape.Params shape;

    Layer(int minY, int maxY, CaveShape.Params shape) {
        this.minY = minY;
        this.maxY = maxY;
        this.shape = shape;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public CaveShape.Params shape() {
        return shape;
    }

    public boolean contains(int y) {
        return y >= minY && y <= maxY;
    }
}
