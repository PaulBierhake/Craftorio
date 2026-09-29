package de.craftorio.module;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The module slots of a machine or beacon: takes modules only, and beacons no productivity modules. */
public final class ModuleInventory extends ItemStackHandler {
    private final boolean beacon;
    private final Runnable onChange;

    public ModuleInventory(int slots, boolean beacon, Runnable onChange) {
        super(slots);
        this.beacon = beacon;
        this.onChange = onChange;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof ModuleItem module && (!beacon || module.kind().beaconAllowed());
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChange.run();
    }

    /** The effects of all modules in the slots. */
    public ModuleEffects effects() {
        ModuleEffects total = ModuleEffects.NONE;
        for (int slot = 0; slot < getSlots(); slot++) {
            if (getStackInSlot(slot).getItem() instanceof ModuleItem module) {
                total = total.plus(module.effects());
            }
        }
        return total;
    }

    public boolean hasProductivity() {
        for (int slot = 0; slot < getSlots(); slot++) {
            if (getStackInSlot(slot).getItem() instanceof ModuleItem module && module.kind() == ModuleKind.PRODUCTIVITY) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmptyOfModules() {
        for (int slot = 0; slot < getSlots(); slot++) {
            if (!getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
