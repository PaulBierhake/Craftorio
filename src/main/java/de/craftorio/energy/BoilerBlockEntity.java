package de.craftorio.energy;

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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns fuel into steam heat and hands it to at most {@link #MAX_ENGINES} steam engines next to it, each drawing
 * {@link SteamEngineBlockEntity#POWER} kW: a boiler is 1.8 MW, so one coal (4 MJ) feeds two engines for 2.2 seconds.
 * Without an offshore pump at the shore next to it there is no water and no steam.
 */
public final class BoilerBlockEntity extends BlockEntity implements MenuProvider, MachineBaseBlock.DropsContents {
    public static final int MAX_ENGINES = 2;
    private static final int WATER_CHECK_INTERVAL = 20;

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
    private boolean water;
    private int waterCheckIn;
    private long servedTick = -1;
    private final List<BlockPos> served = new ArrayList<>();
    private boolean steamed;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> SplitIntData.low(heat);
                case 1 -> SplitIntData.high(heat);
                case 2 -> SplitIntData.low(heatTotal);
                case 3 -> SplitIntData.high(heatTotal);
                case 4 -> water ? 1 : 0;
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

    /** Called by a steam engine once per tick; true if it gets steam for this tick. */
    public boolean tryServe(BlockPos engine, long gameTime) {
        if (servedTick != gameTime) {
            servedTick = gameTime;
            served.clear();
        }
        if (!water || served.contains(engine) || served.size() >= MAX_ENGINES || heat < SteamEngineBlockEntity.POWER) {
            return false;
        }
        heat -= SteamEngineBlockEntity.POWER;
        served.add(engine.immutable());
        steamed = true;
        setChanged();
        return true;
    }

    public boolean hasWater() {
        return water;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
        if (--boiler.waterCheckIn <= 0) {
            boiler.waterCheckIn = WATER_CHECK_INTERVAL;
            boiler.water = boiler.findWater(level, pos);
        }
        // Keep two engines' worth of heat ready; a new fuel item is only burned when the heat runs low.
        if (boiler.water && boiler.heat < MAX_ENGINES * SteamEngineBlockEntity.POWER) {
            boiler.startBurning();
        }
        boolean active = boiler.steamed;
        boiler.steamed = false;
        MachineBaseBlock.setActive(level, pos, state, active);
    }

    private boolean findWater(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (level.getBlockState(next).getBlock() instanceof OffshorePumpBlock && OffshorePumpBlock.hasWater(level, next)) {
                return true;
            }
        }
        return false;
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel.deserializeNBT(registries, tag.getCompound("fuel"));
        heat = tag.getInt("heat");
        heatTotal = tag.getInt("heat_total");
    }
}
