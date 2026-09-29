package de.craftorio.energy;

/** How much of its peak power a solar panel delivers at a time of day (Minecraft ticks, 0 = sunrise, 6000 = noon). */
public final class SolarLogic {
    /** Peak output of one panel in kW, as in Factorio. */
    public static final int PEAK_KW = 60;
    /** Full power until dusk starts, none from the middle of the evening until dawn ends. */
    private static final int DUSK_START = 11_500;
    private static final int NIGHT_START = 13_000;
    private static final int DAWN_START = 23_000;
    private static final int DAY_LENGTH = 24_000;

    private SolarLogic() {
    }

    /** Power in kW: linear ramps over dusk and dawn, zero at night. */
    public static int output(long dayTime, int peakKw) {
        int time = (int) Math.floorMod(dayTime, (long) DAY_LENGTH);
        if (time < DUSK_START) {
            return peakKw;
        }
        if (time < NIGHT_START) {
            return peakKw * (NIGHT_START - time) / (NIGHT_START - DUSK_START);
        }
        if (time < DAWN_START) {
            return 0;
        }
        return peakKw * (time - DAWN_START) / (DAY_LENGTH - DAWN_START);
    }
}
