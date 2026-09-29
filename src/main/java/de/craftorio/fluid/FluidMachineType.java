package de.craftorio.fluid;

/** The machines with fluid inputs and outputs. Power draw in kW (= FE/t), crafting speed 1. */
public enum FluidMachineType {
    /** 3×3 in Factorio, one block here. */
    CHEMICAL_PLANT(210),
    OIL_REFINERY(420);

    /** Item slots in front of the output slot; the output slot is the last one. */
    public static final int INPUT_SLOTS = 2;
    public static final int OUTPUT_SLOT = INPUT_SLOTS;
    /** Two input tanks and one output tank (tank index 2). */
    public static final int INPUT_TANKS = 2;
    public static final int TANK_CAPACITY = 1_000;

    private final int power;

    FluidMachineType(int power) {
        this.power = power;
    }

    public int power() {
        return power;
    }
}
