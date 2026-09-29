package de.craftorio.module;

/**
 * The sum of module (and beacon) effects of one machine, in percent (see docs/FACTORIO-UMBAU.md §11.4). Plain numbers,
 * no Minecraft classes, so the rules are unit-tested. Speed and energy never drop below 20 % of the base value.
 */
public record ModuleEffects(double speed, double energy, double productivity) {
    public static final ModuleEffects NONE = new ModuleEffects(0, 0, 0);
    /** Factorio: speed and energy consumption can not be lowered below 20 %. */
    public static final double MINIMUM = 0.2;
    /** Productivity is capped at +300 %. */
    public static final double MAX_PRODUCTIVITY = 300;
    /** Beacons hand on half of the effect of their modules. */
    public static final double BEACON_SHARE = 0.5;

    public ModuleEffects plus(ModuleEffects other) {
        return new ModuleEffects(speed + other.speed, energy + other.energy, productivity + other.productivity);
    }

    public ModuleEffects times(double factor) {
        return new ModuleEffects(speed * factor, energy * factor, productivity * factor);
    }

    /** What a beacon gives to the machines around it: speed and energy at half strength, no productivity. */
    public ModuleEffects transmitted() {
        return new ModuleEffects(speed * BEACON_SHARE, energy * BEACON_SHARE, 0);
    }

    /** Crafting speed relative to the machine's base speed. */
    public double speedFactor() {
        return Math.max(MINIMUM, 1 + speed / 100);
    }

    /** Power draw relative to the machine's base draw. */
    public double energyFactor() {
        return Math.max(MINIMUM, 1 + energy / 100);
    }

    /** Extra products per craft, as a fraction (0.1 = +10 %). */
    public double productivityBonus() {
        return Math.max(0, Math.min(MAX_PRODUCTIVITY, productivity)) / 100;
    }

    public boolean isNone() {
        return speed == 0 && energy == 0 && productivity == 0;
    }

    /** Ticks a craft of {@code baseTicks} takes with these effects (at least one). */
    public int ticks(int baseTicks) {
        return Math.max(1, (int) Math.round(baseTicks / speedFactor()));
    }

    /** Power per tick of a machine that draws {@code base} without modules (at least one). */
    public int power(int base) {
        return base <= 0 ? 0 : Math.max(1, (int) Math.round(base * energyFactor()));
    }

    /**
     * Productivity bar: every craft adds the bonus; each full 100 % is one extra craft's worth of products that costs no
     * ingredients.
     */
    public static final class Bar {
        private double progress;

        /** Adds the bonus of one craft and returns how many extra products this gives (0 or more). */
        public int add(double bonus) {
            progress += bonus;
            int extra = (int) Math.floor(progress);
            progress -= extra;
            return extra;
        }

        public double progress() {
            return progress;
        }

        public void set(double progress) {
            this.progress = Math.max(0, progress);
        }
    }
}
