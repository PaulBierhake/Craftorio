package de.craftorio.machine;

/**
 * Output of a drill: every ore field block in its area contributes a fixed rate. More blocks means more output,
 * up to the area size; a better drill tier raises the rate per block. Counts in millionths of an item so long
 * runs don't drift from rounding.
 */
public final class DrillProduction {
    private static final long ONE_ITEM = 1_000_000L;

    private final long perBlockPerTick;
    private long progress;

    public DrillProduction(double itemsPerBlockPerSecond) {
        this.perBlockPerTick = Math.round(itemsPerBlockPerSecond * ONE_ITEM / 20.0);
    }

    /** Advances one working tick; returns how many items finished. */
    public int tick(int fieldBlocks) {
        return tick(fieldBlocks, 1);
    }

    /** Like {@link #tick(int)} at {@code 1 / slowdown} of the rate: uranium ore is mined at half speed. */
    public int tick(int fieldBlocks, int slowdown) {
        return tick(fieldBlocks, 1.0 / slowdown);
    }

    /** Like {@link #tick(int)} at {@code factor} times the rate (module speed, slowdown of uranium ore). */
    public int tick(int fieldBlocks, double factor) {
        if (fieldBlocks <= 0) {
            return 0;
        }
        progress += Math.round(fieldBlocks * perBlockPerTick * factor);
        int finished = (int) (progress / ONE_ITEM);
        progress -= finished * ONE_ITEM;
        return finished;
    }

    /** Progress towards the next item, 0 to 1. */
    public double progress() {
        return (double) progress / ONE_ITEM;
    }

    public void setProgress(double progress) {
        this.progress = Math.round(Math.max(0, Math.min(progress, 1)) * ONE_ITEM) % ONE_ITEM;
    }

    public double itemsPerSecond(int fieldBlocks) {
        return (double) fieldBlocks * perBlockPerTick * 20 / ONE_ITEM;
    }
}
