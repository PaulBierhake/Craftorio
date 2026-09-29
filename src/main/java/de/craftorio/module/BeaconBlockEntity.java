package de.craftorio.module;

import de.craftorio.energy.EnergyBuffer;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.menu.BeaconMenu;
import de.craftorio.menu.SplitIntData;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** 480 kW; works while it has power and at least one module. */
public final class BeaconBlockEntity extends BlockEntity implements MenuProvider, MachineBaseBlock.DropsContents {
    public static final int POWER = 480;
    public static final int SLOTS = 2;
    public static final int ENERGY_CAPACITY = 10_000;

    private final EnergyBuffer energy = new EnergyBuffer(ENERGY_CAPACITY, 2_000, 0, this::setChanged);
    private final ModuleInventory modules = new ModuleInventory(SLOTS, true, this::setChanged);
    private boolean active;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> SplitIntData.low(energy.getEnergyStored());
                case 1 -> SplitIntData.high(energy.getEnergyStored());
                case 2 -> active ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return BeaconMenu.DATA_COUNT;
        }
    };

    public BeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEACON.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public ModuleInventory modules() {
        return modules;
    }

    /** Does the beacon work right now (power and modules)? */
    public boolean active() {
        return active;
    }

    /** What this beacon gives to each machine in range. */
    public ModuleEffects transmitted() {
        return modules.effects().transmitted();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            Beacons.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            Beacons.remove(level, worldPosition);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) {
            Beacons.remove(level, worldPosition);
        }
        super.onChunkUnloaded();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BeaconBlockEntity beacon) {
        boolean wasActive = beacon.active;
        beacon.active = !beacon.modules.isEmptyOfModules() && beacon.energy.consume(POWER);
        if (wasActive != beacon.active) {
            MachineBaseBlock.setActive(level, pos, state, beacon.active);
            beacon.setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BeaconMenu(containerId, inventory, this, data);
    }

    @Override
    public void dropContents() {
        if (level != null) {
            for (int slot = 0; slot < modules.getSlots(); slot++) {
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), modules.getStackInSlot(slot));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("modules", modules.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        modules.deserializeNBT(registries, tag.getCompound("modules"));
        energy.setEnergy(tag.getInt("energy"));
    }
}
