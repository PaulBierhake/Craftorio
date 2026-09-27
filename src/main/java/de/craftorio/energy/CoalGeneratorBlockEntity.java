package de.craftorio.energy;

import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.menu.GeneratorMenu;
import de.craftorio.menu.SplitIntData;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** Burns furnace fuel into FE for the power grid. Only burns while its buffer has room, so no fuel is wasted. */
public final class CoalGeneratorBlockEntity extends BlockEntity implements PowerSource, MenuProvider, MachineBaseBlock.DropsContents {
    public static final int FE_PER_TICK = 60;
    public static final int CAPACITY = 20_000;
    public static final int MAX_OUTPUT = 1_000;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 0, MAX_OUTPUT, this::setChanged);
    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getBurnTime(RecipeType.SMELTING) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int burnTicks;
    private int burnTotal;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> SplitIntData.low(energy.getEnergyStored());
                case 1 -> SplitIntData.high(energy.getEnergyStored());
                case 2 -> burnTicks;
                case 3 -> burnTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return GeneratorMenu.DATA_COUNT;
        }
    };

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_GENERATOR.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public ItemStackHandler fuel() {
        return fuel;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CoalGeneratorBlockEntity generator) {
        if (generator.burnTicks <= 0 && generator.energy.freeSpace() >= FE_PER_TICK) {
            generator.startBurning();
        }
        boolean burning = generator.burnTicks > 0;
        if (burning) {
            generator.burnTicks--;
            generator.energy.generate(FE_PER_TICK);
        }
        MachineBaseBlock.setActive(level, pos, state, burning);
    }

    private void startBurning() {
        ItemStack stack = fuel.getStackInSlot(0);
        int burnTime = stack.getBurnTime(RecipeType.SMELTING);
        if (burnTime <= 0) {
            return;
        }
        burnTicks = burnTotal = burnTime;
        ItemStack remainder = stack.getCraftingRemainingItem();
        stack.shrink(1);
        fuel.setStackInSlot(0, stack.isEmpty() ? remainder : stack);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new GeneratorMenu(containerId, inventory, this, data);
    }

    @Override
    public void dropContents() {
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), fuel.getStackInSlot(0));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fuel", fuel.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("burn_ticks", burnTicks);
        tag.putInt("burn_total", burnTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel.deserializeNBT(registries, tag.getCompound("fuel"));
        energy.setEnergy(tag.getInt("energy"));
        burnTicks = tag.getInt("burn_ticks");
        burnTotal = tag.getInt("burn_total");
    }
}
