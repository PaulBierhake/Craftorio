package de.craftorio.machine;

/** The processing machines; each defines its slot count and power draw in kW (= FE/t, see {@link de.craftorio.energy.Energy}). */
public enum MachineType {
    /** Vanilla smelting recipes, electrically heated and faster than a furnace. */
    ELECTRIC_FURNACE(1, 180),
    PRESS(1, 75),
    /** Crafts the recipe selected in its GUI. */
    ASSEMBLER(4, 75);

    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_INPUT = 2_000;
    /** Smelting time in ticks (vanilla furnace: 200). */
    public static final int SMELTING_TIME = 80;

    private final int inputSlots;
    private final int energyPerTick;

    MachineType(int inputSlots, int energyPerTick) {
        this.inputSlots = inputSlots;
        this.energyPerTick = energyPerTick;
    }

    public int inputSlots() {
        return inputSlots;
    }

    public int outputSlot() {
        return inputSlots;
    }

    public int energyPerTick() {
        return energyPerTick;
    }
}
