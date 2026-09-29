package de.craftorio.heat;

/**
 * The rules of the heat network as plain numbers (no Minecraft classes, so they are unit-tested). Heat is counted in
 * FE like power: 1 kW = 1 FE/t, so a 40 MW reactor makes 40,000 per tick and a heat pipe (1 MJ per °C in Factorio,
 * 1 MJ = 20,000 FE at 20 ticks per second) stores 20,000 per °C.
 */
public final class HeatLogic {
    public static final double AMBIENT = 15;
    public static final double MAX_TEMPERATURE = 1000;
    /** Heat exchangers make steam only from this temperature up, and the steam is exactly this hot. */
    public static final double STEAM_TEMPERATURE = 500;

    /** Heat capacity in FE per °C (Factorio: pipe and exchanger 1 MJ/°C, reactor 10 MJ/°C). */
    public static final double PIPE_CAPACITY = 20_000;
    public static final double EXCHANGER_CAPACITY = 20_000;
    public static final double REACTOR_CAPACITY = 200_000;

    /** A fuel cell burns 200 s at 40 MW, whatever the load. */
    public static final int CELL_TICKS = 4_000;
    public static final int REACTOR_HEAT = 40_000;
    /** A heat exchanger takes up to 10 MW and turns it into hot steam: 0.097 MJ per unit (wiki), about 103 units per second. */
    public static final int EXCHANGER_HEAT = 10_000;
    public static final double HEAT_PER_STEAM = 1_940;
    public static final double EXCHANGER_STEAM_PER_TICK = EXCHANGER_HEAT / HEAT_PER_STEAM;
    /** A turbine takes 60 hot steam per second and gives 5.82 MW. */
    public static final int TURBINE_POWER = 5_820;
    public static final int TURBINE_STEAM_PER_TICK = 3;

    private HeatLogic() {
    }

    /**
     * Heat that flows from node A to node B in one tick (negative: from B to A). Each link conducts at half of the
     * smaller share of capacity per neighbour, so a node never overshoots the temperature of its neighbours, whatever
     * the order in which the links are handled.
     */
    public static double flow(double temperatureA, double capacityA, int neighboursA, double temperatureB, double capacityB, int neighboursB) {
        double conductance = Math.min(capacityA / Math.max(1, neighboursA), capacityB / Math.max(1, neighboursB)) / 2;
        return (temperatureA - temperatureB) * conductance;
    }

    /** The temperature after adding {@code heat} FE (negative: taking heat away), kept between ambient and the maximum. */
    public static double warm(double temperature, double capacity, double heat) {
        return Math.max(AMBIENT, Math.min(MAX_TEMPERATURE, temperature + heat / capacity));
    }

    /** Heat one running reactor makes per tick: 40 MW plus 100 % for every adjacent running reactor. */
    public static int reactorHeat(int runningNeighbours) {
        return REACTOR_HEAT * (1 + Math.max(0, runningNeighbours));
    }

    /** Heat above steam temperature that an exchanger of this temperature can still give away. */
    public static double usableHeat(double temperature, double capacity) {
        return Math.max(0, (temperature - STEAM_TEMPERATURE) * capacity);
    }

    /** May the reactor take a new fuel cell? Not while it is hotter than the limit the player set. */
    public static boolean mayLoadCell(double temperature, int limit) {
        return temperature < limit;
    }
}
