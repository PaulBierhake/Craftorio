package de.craftorio.energy;

import de.craftorio.fluid.FluidBuffer;
import de.craftorio.fluid.FluidHelper;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.menu.BoilerMenu;
import de.craftorio.menu.SplitIntData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import de.craftorio.registry.ModFluids;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;


/**
 * Burns fuel and turns water into steam: 1.8 MW make {@link #STEAM_PER_TICK} steam per tick (60 per second) from a
 * tenth of that in water, enough for two steam engines. Water comes in and steam goes out through the fluid network
 * (pipes, offshore pump next to it); the steam goes to every neighbour that takes it.
 */
public final class BoilerBlockEntity extends BlockEntity implements MenuProvider, MachineBaseBlock.DropsContents {
    /** One boiler feeds two engines of 1.5 steam per tick each. */
    public static final int STEAM_PER_TICK = 3;
    public static final int MAX_ENGINES = 2;
    /** Heat (FE) a unit of steam takes: 1.8 MW for 3 steam per tick. */
    public static final int HEAT_PER_STEAM = 600;
    public static final int TANK = 200;
    private static final int STEAM_PUSH = 20;

    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return Fuel.isFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Heat in FE (1 kW for one tick = 1 FE) left from the fuel item that is burning. */
    private int heat;
    private int heatTotal;
    private boolean steamed;
    /** Steam made since the last unit of water was used up: a tenth of a water unit per steam unit. */
    private int waterDebt;
    private final FluidBuffer water = new FluidBuffer(TANK, FluidBuffer.only(Fluids.WATER), this::setChanged);
    private final FluidBuffer steam = new FluidBuffer(TANK, FluidBuffer.only(ModFluids.STEAM.get()), this::setChanged);
    /** Water in, steam out. */
    private final IFluidHandler fluids = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? water.getFluid() : steam.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && water.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return water.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return steam.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return steam.drain(maxDrain, action);
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> SplitIntData.low(heat);
                case 1 -> SplitIntData.high(heat);
                case 2 -> SplitIntData.low(heatTotal);
                case 3 -> SplitIntData.high(heatTotal);
                case 4 -> water.isEmpty() ? 0 : 1;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return BoilerMenu.DATA_COUNT;
        }
    };

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(de.craftorio.registry.ModBlockEntities.BOILER.get(), pos, state);
    }

    public ItemStackHandler fuel() {
        return fuel;
    }

    public IFluidHandler fluids() {
        return fluids;
    }

    public FluidBuffer waterTank() {
        return water;
    }

    public FluidBuffer steamTank() {
        return steam;
    }

    public boolean hasWater() {
        return !water.isEmpty();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
        int wanted = Math.min(STEAM_PER_TICK, boiler.steam.space());
        boolean active = false;
        if (wanted > 0 && (!boiler.water.isEmpty() || boiler.waterDebt < 7)) {
            // A new fuel item is only burned when the heat runs low.
            if (boiler.heat < wanted * HEAT_PER_STEAM && !boiler.water.isEmpty()) {
                boiler.startBurning();
            }
            int made = Math.min(wanted, boiler.heat / HEAT_PER_STEAM);
            if (made > 0) {
                boiler.heat -= made * HEAT_PER_STEAM;
                boiler.steam.fill(new FluidStack(ModFluids.STEAM.get(), made), IFluidHandler.FluidAction.EXECUTE);
                boiler.waterDebt += made;
                if (boiler.waterDebt >= 10) {
                    boiler.waterDebt -= 10;
                    boiler.water.use(1);
                }
                active = true;
            }
        }
        if (!boiler.steam.isEmpty()) {
            for (Direction direction : Direction.values()) {
                IFluidHandler neighbour = FluidHelper.neighbour(level, pos, direction);
                if (neighbour != null) {
                    FluidHelper.push(boiler.steam, neighbour, STEAM_PUSH);
                }
            }
        }
        boiler.steamed = active;
        MachineBaseBlock.setActive(level, pos, state, active);
    }

    private void startBurning() {
        ItemStack stack = fuel.getStackInSlot(0);
        double megajoules = Fuel.megajoules(stack);
        if (megajoules <= 0) {
            return;
        }
        int gained = (int) Math.round(megajoules * Energy.FE_PER_MJ);
        heat += gained;
        heatTotal = heat;
        ItemStack remainder = stack.getCraftingRemainingItem();
        stack.shrink(1);
        fuel.setStackInSlot(0, stack.isEmpty() ? remainder : stack);
        setChanged();
    }

    @Override
    public void dropContents() {
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), fuel.getStackInSlot(0));
            fuel.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BoilerMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fuel", fuel.serializeNBT(registries));
        tag.putInt("heat", heat);
        tag.putInt("heat_total", heatTotal);
        tag.putInt("water_debt", waterDebt);
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel.deserializeNBT(registries, tag.getCompound("fuel"));
        heat = tag.getInt("heat");
        heatTotal = tag.getInt("heat_total");
        waterDebt = tag.getInt("water_debt");
        water.readFromNBT(registries, tag.getCompound("water"));
        steam.readFromNBT(registries, tag.getCompound("steam"));
    }
}
