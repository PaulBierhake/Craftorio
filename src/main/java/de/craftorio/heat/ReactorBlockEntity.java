package de.craftorio.heat;

import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.menu.ReactorMenu;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModItems;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Burns one uranium fuel cell in 200 s whatever the load, making 40 MW of heat (plus 100 % for every adjacent running
 * reactor). New cells only go in while the reactor is cooler than the limit set in its GUI, the used cell goes to the
 * output slot.
 */
public final class ReactorBlockEntity extends BlockEntity implements HeatNode, MenuProvider, MachineBaseBlock.DropsContents {
    public static final int FUEL_SLOT = 0;
    public static final int USED_SLOT = 1;
    public static final int MIN_LIMIT = 500;
    public static final int MAX_LIMIT = 1000;
    public static final int LIMIT_STEP = 50;
    public static final int DEFAULT_LIMIT = 900;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FUEL_SLOT && stack.is(ModItems.URANIUM_FUEL_CELL.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler automation = new Automation();
    private double temperature = HeatLogic.AMBIENT;
    private int burnTicks;
    private int limit = DEFAULT_LIMIT;
    private int heat;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.round(temperature);
                case 1 -> burnTicks;
                case 2 -> limit;
                case 3 -> heat;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return ReactorMenu.DATA_COUNT;
        }
    };

    public ReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automation() {
        return automation;
    }

    public boolean burning() {
        return burnTicks > 0;
    }

    /** Heat this reactor makes per tick right now, in FE. */
    public int heatPerTick() {
        return heat;
    }

    public int limit() {
        return limit;
    }

    public void changeLimit(int steps) {
        limit = Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, limit + steps * LIMIT_STEP));
        setChanged();
    }

    @Override
    public double temperature() {
        return temperature;
    }

    @Override
    public void setTemperature(double temperature) {
        this.temperature = temperature;
        setChanged();
    }

    @Override
    public double capacity() {
        return HeatLogic.REACTOR_CAPACITY;
    }

    private int runningNeighbours(Level level) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof ReactorBlockEntity other && other.burning()) {
                count++;
            }
        }
        return count;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReactorBlockEntity reactor) {
        if (reactor.burnTicks <= 0) {
            reactor.tryLoadCell();
        }
        if (reactor.burnTicks > 0) {
            reactor.heat = HeatLogic.reactorHeat(reactor.runningNeighbours(level));
            reactor.addHeat(reactor.heat);
            if (--reactor.burnTicks == 0) {
                ItemStack used = reactor.items.getStackInSlot(USED_SLOT);
                if (used.isEmpty()) {
                    reactor.items.setStackInSlot(USED_SLOT, new ItemStack(ModItems.USED_UP_FUEL_CELL.get()));
                } else {
                    used.grow(1);
                    reactor.items.setStackInSlot(USED_SLOT, used);
                }
            }
        } else {
            reactor.heat = 0;
        }
        HeatNode.conduct(level, pos, reactor);
        MachineBaseBlock.setActive(level, pos, state, reactor.burnTicks > 0);
    }

    private void tryLoadCell() {
        ItemStack cell = items.getStackInSlot(FUEL_SLOT);
        ItemStack used = items.getStackInSlot(USED_SLOT);
        if (cell.isEmpty() || !HeatLogic.mayLoadCell(temperature, limit) || used.getCount() >= used.getMaxStackSize() && !used.isEmpty()) {
            return;
        }
        cell.shrink(1);
        items.setStackInSlot(FUEL_SLOT, cell.isEmpty() ? ItemStack.EMPTY : cell);
        burnTicks = HeatLogic.CELL_TICKS;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ReactorMenu(containerId, inventory, this, data);
    }

    @Override
    public void dropContents() {
        if (level != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putDouble("temperature", temperature);
        tag.putInt("burn_ticks", burnTicks);
        tag.putInt("limit", limit);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        temperature = tag.contains("temperature") ? tag.getDouble("temperature") : HeatLogic.AMBIENT;
        burnTicks = tag.getInt("burn_ticks");
        limit = tag.contains("limit") ? tag.getInt("limit") : DEFAULT_LIMIT;
    }

    /** Cells go in, used cells come out. */
    private final class Automation implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == FUEL_SLOT ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == USED_SLOT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    }
}
