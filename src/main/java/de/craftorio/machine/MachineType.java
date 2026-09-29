package de.craftorio.machine;

/**
 * The processing machines. Power draw is in kW (= FE/t, see {@link de.craftorio.energy.Energy}), speed as in Factorio:
 * a recipe of {@code t} ticks takes {@code t / speed} ticks. Fuel machines have a fuel slot instead of a power connection.
 */
public enum MachineType {
    /** Burns fuel (90 kW) and smelts like the vanilla furnace recipes at Factorio speed. */
    STONE_FURNACE(1, 90, true, 1.0),
    /** Vanilla smelting recipes, electrically heated; twice the speed of a stone furnace. */
    ELECTRIC_FURNACE(1, 180, false, 2.0),
    /** Burns fuel (90 kW) like the stone furnace, at twice its speed. */
    STEEL_FURNACE(1, 90, true, 2.0),
    /** Crafts the recipe selected in its GUI (assembling machine 1). */
    ASSEMBLER(4, 75, false, 0.5),
    /** Assembling machine 2: faster than machine 1 at twice the power draw. */
    ASSEMBLER_2(4, 150, false, 0.75),
    /** Assembling machine 3: speed 1.25 for 375 kW, four module slots. */
    ASSEMBLER_3(4, 375, false, 1.25);

    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_INPUT = 2_000;
    /** Factorio smelts in 3.2 s what the vanilla furnace takes 10 s for. */
    private static final double SMELTING_FACTOR = 0.32;

    private final int inputSlots;
    private final int power;
    private final boolean fuel;
    private final double speed;

    MachineType(int inputSlots, int power, boolean fuel, double speed) {
        this.inputSlots = inputSlots;
        this.power = power;
        this.fuel = fuel;
        this.speed = speed;
    }

    public int inputSlots() {
        return inputSlots;
    }

    public int outputSlot() {
        return inputSlots;
    }

    /** Fuel machines have one more slot behind the output slot. */
    public int fuelSlots() {
        return fuel ? 1 : 0;
    }

    public int fuelSlot() {
        return inputSlots + 1;
    }

    public int slotCount() {
        return inputSlots + 1 + fuelSlots();
    }

    /** Assemblers craft the recipe chosen in their GUI; furnaces smelt what is put in. */
    public boolean assembling() {
        return this == ASSEMBLER || this == ASSEMBLER_2 || this == ASSEMBLER_3;
    }

    /** Assembling machines 2 and 3 can take a fluid ingredient through a pipe (concrete, lubricant recipes). */
    public boolean fluidInput() {
        return this == ASSEMBLER_2 || this == ASSEMBLER_3;
    }

    /** Module slots (Factorio 1.1): assembling machine 2 has 2, machine 3 has 4, the electric furnace 2; fuel machines and machine 1 none. */
    public int moduleSlots() {
        return switch (this) {
            case ELECTRIC_FURNACE, ASSEMBLER_2 -> 2;
            case ASSEMBLER_3 -> 4;
            default -> 0;
        };
    }

    public boolean usesFuel() {
        return fuel;
    }

    /** Power draw in kW: grid power per tick, or the fuel burn rate. */
    public int energyPerTick() {
        return power;
    }

    public double speed() {
        return speed;
    }

    /** Ticks a recipe of {@code baseTicks} (at speed 1) takes in this machine. */
    public int ticks(int baseTicks) {
        return Math.max(1, (int) Math.round(baseTicks / speed));
    }

    /** Base ticks of a vanilla smelting recipe with the given cooking time, as Factorio times it. */
    public static int smeltingTicks(int cookingTime) {
        return Math.max(1, (int) Math.round(cookingTime * SMELTING_FACTOR));
    }
}
