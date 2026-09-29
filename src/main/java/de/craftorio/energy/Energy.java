package de.craftorio.energy;

/**
 * The energy unit of the whole mod, chosen so Factorio's numbers can be used as they are: <b>1 kW = 1 FE/t</b>.
 * A tick is 1/20 s, so one second at 1 kW is 20 FE (Factorio's 1 kJ) and 1 MJ is {@value #FE_PER_MJ} FE.
 */
public final class Energy {
    public static final int FE_PER_KJ = 20;
    public static final int FE_PER_MJ = 1_000 * FE_PER_KJ;

    private Energy() {
    }

    /** Ticks a device drawing {@code kw} kilowatts runs on {@code megajoules} of fuel. */
    public static int burnTicks(double megajoules, int kw) {
        return kw <= 0 ? 0 : (int) Math.round(megajoules * FE_PER_MJ / kw);
    }
}
