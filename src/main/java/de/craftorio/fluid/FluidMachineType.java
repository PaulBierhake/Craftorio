package de.craftorio.fluid;

/** The machines with fluid inputs and outputs. Power draw in kW (= FE/t), crafting speed 1. */
public enum FluidMachineType {
    /** 3×3 in Factorio, one block here. */
    CHEMICAL_PLANT(210),
    OIL_REFINERY(420),
    /** Grows plants from seeds and water. */
    GREENHOUSE(90),
    /** Splits uranium ore, reprocesses used fuel cells and enriches uranium (no fluids, two output slots). */
    CENTRIFUGE(350);

    /** Item slots in front of the output slots. */
    public static final int INPUT_SLOTS = 2;
    /** First output slot; the centrifuge has a second one behind it. */
    public static final int OUTPUT_SLOT = INPUT_SLOTS;
    public static final int SECOND_OUTPUT_SLOT = INPUT_SLOTS + 1;
    /** Item slots of every machine of the family; machines with one product leave the last one unused. */
    public static final int ITEM_SLOTS = INPUT_SLOTS + 2;
    /** Two input tanks and three output tanks. */
    public static final int INPUT_TANKS = 2;
    public static final int TANK_CAPACITY = 1_000;
    /** Output tanks (tank index {@link #INPUT_TANKS} and up): the refinery's advanced oil processing makes three fluids. */
    public static final int OUTPUT_TANKS = 3;

    private final int power;

    FluidMachineType(int power) {
        this.power = power;
    }

    public int power() {
        return power;
    }

    /** Product slots this machine shows and offers to inserters. */
    public int outputSlots() {
        return this == CENTRIFUGE ? 2 : 1;
    }

    /** The centrifuge has no tanks and no pipe connections. */
    public boolean hasFluids() {
        return this != CENTRIFUGE;
    }
}
