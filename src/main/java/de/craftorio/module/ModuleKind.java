package de.craftorio.module;

/** The three kinds of module and their effects per tier (Factorio 1.1 wiki). */
public enum ModuleKind {
    /** +20/30/50 % speed for +50/60/70 % energy. */
    SPEED(new double[]{20, 30, 50}, new double[]{50, 60, 70}, new double[]{0, 0, 0}),
    /** −30/40/50 % energy. */
    EFFICIENCY(new double[]{0, 0, 0}, new double[]{-30, -40, -50}, new double[]{0, 0, 0}),
    /** +4/6/10 % products for −5/10/15 % speed and +40/60/80 % energy. */
    PRODUCTIVITY(new double[]{-5, -10, -15}, new double[]{40, 60, 80}, new double[]{4, 6, 10});

    public static final int TIERS = 3;

    private final double[] speed;
    private final double[] energy;
    private final double[] productivity;

    ModuleKind(double[] speed, double[] energy, double[] productivity) {
        this.speed = speed;
        this.energy = energy;
        this.productivity = productivity;
    }

    /** Effects of a module of this kind; {@code tier} is 1 to 3. */
    public ModuleEffects effects(int tier) {
        return new ModuleEffects(speed[tier - 1], energy[tier - 1], productivity[tier - 1]);
    }

    public String id(int tier) {
        return name().toLowerCase(java.util.Locale.ROOT) + "_module_" + tier;
    }

    /** Beacons take speed and efficiency modules only. */
    public boolean beaconAllowed() {
        return this != PRODUCTIVITY;
    }
}
