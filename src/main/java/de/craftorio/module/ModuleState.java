package de.craftorio.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Everything a machine needs for modules: the slots, the effects (own modules plus the beacons around, looked up
 * every second), and the productivity bar.
 */
public final class ModuleState {
    private static final int REFRESH_TICKS = 20;

    private final ModuleInventory inventory;
    private final ModuleEffects.Bar bar = new ModuleEffects.Bar();
    private ModuleEffects effects = ModuleEffects.NONE;
    private int refreshIn;

    public ModuleState(int slots, Runnable onChange) {
        this.inventory = new ModuleInventory(slots, false, () -> {
            refreshIn = 0;
            onChange.run();
        });
    }

    public ModuleInventory inventory() {
        return inventory;
    }

    public int slots() {
        return inventory.getSlots();
    }

    public ModuleEffects.Bar bar() {
        return bar;
    }

    public boolean hasProductivity() {
        return inventory.hasProductivity();
    }

    /** Own modules plus beacons, refreshed once per second and whenever a module changes. */
    public ModuleEffects effects(Level level, BlockPos pos) {
        if (inventory.getSlots() == 0) {
            return ModuleEffects.NONE;
        }
        if (--refreshIn <= 0) {
            refreshIn = REFRESH_TICKS;
            effects = inventory.effects().plus(level.isClientSide ? ModuleEffects.NONE : Beacons.effectAt(level, pos));
        }
        return effects;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(slot));
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (inventory.getSlots() > 0) {
            tag.put("modules", inventory.serializeNBT(registries));
            tag.putDouble("productivity_bar", bar.progress());
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (inventory.getSlots() > 0 && tag.contains("modules")) {
            inventory.deserializeNBT(registries, tag.getCompound("modules"));
            bar.set(tag.getDouble("productivity_bar"));
        }
        refreshIn = 0;
    }
}
