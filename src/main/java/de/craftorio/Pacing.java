package de.craftorio;

/** Pure part of the pacing settings in {@link CraftorioConfig}. */
public final class Pacing {
    private Pacing() {
    }

    /** A time in ticks at the given machine speed: faster machines need fewer ticks, but at least one. */
    public static int scaledTicks(int ticks, double speed) {
        return Math.max(1, (int) Math.round(ticks / speed));
    }
}
