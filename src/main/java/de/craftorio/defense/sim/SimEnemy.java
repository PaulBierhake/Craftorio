package de.craftorio.defense.sim;

/** One enemy in a {@link TdSimulation}: its current layer, modifiers and place on the path. */
public final class SimEnemy {
    final int id;
    EnemyDef def;
    double hp;
    boolean camo;
    boolean regrow;
    boolean fortified;
    /** The form a regrowing enemy heals back to. */
    String regrowTop;
    /** Layers healed by regrowing that are still intact: popping them pays nothing. */
    int freeLayers;
    int regrowTimer;
    double distance;
    double x;
    double y;
    double z;
    int segment;
    boolean alive = true;
    /** Red-blue-equivalents left, kept up to date for targeting. */
    double rbe;
    /** Slowdown and similar effects: multiplies the speed. */
    double speedFactor = 1;

    SimEnemy(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public EnemyDef def() {
        return def;
    }

    public double hp() {
        return hp;
    }

    public boolean camo() {
        return camo;
    }

    public boolean regrow() {
        return regrow;
    }

    public boolean fortified() {
        return fortified;
    }

    public boolean alive() {
        return alive;
    }

    /** Distance along the path in blocks. */
    public double distance() {
        return distance;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public double rbe() {
        return rbe;
    }

    public double distanceSqr(double px, double py, double pz) {
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Modifier bits as sent to the clients: 1 camo, 2 regrow, 4 fortified. */
    public int flags() {
        return (camo ? 1 : 0) | (regrow ? 2 : 0) | (fortified ? 4 : 0);
    }
}
