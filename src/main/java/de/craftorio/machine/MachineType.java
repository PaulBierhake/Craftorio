package de.craftorio.machine;

/** The processing machines; each defines its slot count and power draw. */
public enum MachineType {
    /** Vanilla smelting recipes, electrically heated and faster than a furnace. */
    ELECTRIC_FURNACE(1, 20),
    PRESS(1, 15),
    /** Crafts the recipe selected in its GUI. */
    ASSEMBLER(4, 25);

    public static final int ENERGY_CAPACITY = 10_000;
    public static final int MAX_INPUT = 500;
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
