package de.craftorio.machine;

import de.craftorio.energy.EnergyBuffer;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.world.OreFieldBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DrillBlockEntity extends BlockEntity {
    public static final int FUEL_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int RESCAN_INTERVAL = 40;

    private final DrillTier tier;
    private final DrillProduction production;
    private final EnergyBuffer energy;
    private final IItemHandler handler = new Handler();
    private ItemStack fuel = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int burnTicks;
    private List<BlockPos> fieldBlocks = List.of();
    private int nextField;
    private int rescanIn;

    public DrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRILL.get(), pos, state);
        this.tier = state.getBlock() instanceof DrillBlock drill ? drill.tier() : DrillTier.BURNER;
        this.production = new DrillProduction(tier.itemsPerBlockPerSecond());
        this.energy = new EnergyBuffer(Math.max(1, tier.energyPerTick() * 200), tier.energyPerTick() * 4, 0, this::setChanged);
    }

    public DrillTier tier() {
        return tier;
    }

    /** Grid buffer of electric tiers; burner drills have none. */
    public @Nullable EnergyBuffer energy() {
        return tier.usesFuel() ? null : energy;
    }

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    public IItemHandler handler() {
        return handler;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DrillBlockEntity drill) {
        drill.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        if (--rescanIn <= 0) {
            rescanIn = RESCAN_INTERVAL;
            fieldBlocks = scanFieldBlocks(level, pos, tier.radius());
        }
        pushOutput(level, pos, state.getValue(DrillBlock.FACING));

        boolean working = false;
        ItemStack next = nextResource(level);
        if (!next.isEmpty() && hasRoomFor(next) && powered()) {
            working = true;
            int finished = production.tick(fieldBlocks.size());
            for (int i = 0; i < finished && hasRoomFor(nextResource(level)); i++) {
                addOutput(nextResource(level));
                nextField++;
            }
            setChanged();
        }
        if (state.getValue(DrillBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(DrillBlock.LIT, working), 3);
        }
    }

    /** Pays for one tick of work with fuel or grid energy. */
    private boolean powered() {
        if (!tier.usesFuel()) {
            return energy.consume(tier.energyPerTick());
        }
        if (burnTicks > 0 || consumeFuel()) {
            burnTicks--;
            return true;
        }
        return false;
    }

    private static List<BlockPos> scanFieldBlocks(Level level, BlockPos pos, int radius) {
        List<BlockPos> found = new ArrayList<>((2 * radius + 1) * (2 * radius + 1));
        BlockPos center = pos.below();
        for (BlockPos candidate : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
            if (level.getBlockState(candidate).getBlock() instanceof OreFieldBlock) {
                found.add(candidate.immutable());
            }
        }
        return found;
    }

    /** Field blocks are mined round-robin, so mixed areas yield a mix of resources. */
    private ItemStack nextResource(Level level) {
        if (fieldBlocks.isEmpty()) {
            return ItemStack.EMPTY;
        }
        BlockPos fieldPos = fieldBlocks.get(Math.floorMod(nextField, fieldBlocks.size()));
        return level.getBlockState(fieldPos).getBlock() instanceof OreFieldBlock field ? field.resource() : ItemStack.EMPTY;
    }

    private boolean hasRoomFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, stack) && output.getCount() < output.getMaxStackSize());
    }

    private void addOutput(ItemStack stack) {
        if (output.isEmpty()) {
            output = stack.copy();
        } else {
            output.grow(stack.getCount());
        }
    }

    private boolean consumeFuel() {
        if (!isFuel(fuel)) {
            return false;
        }
        burnTicks = fuel.getBurnTime(RecipeType.SMELTING);
        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            fuel = remainder;
        }
        return true;
    }

    private void pushOutput(Level level, BlockPos pos, Direction facing) {
        if (output.isEmpty()) {
            return;
        }
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(facing), facing.getOpposite());
        if (target == null) {
            return;
        }
        while (!output.isEmpty() && ItemHandlerHelper.insertItem(target, output.copyWithCount(1), false).isEmpty()) {
            output.shrink(1);
            setChanged();
        }
    }

    public Component status() {
        int blocks = fieldBlocks.size();
        String rate = String.format(Locale.ROOT, "%.2f", production.itemsPerSecond(blocks));
        if (!tier.usesFuel()) {
            return Component.translatable("craftorio.drill.status_electric", blocks, tier.area(), rate,
                    energy.getEnergyStored(), tier.energyPerTick());
        }
        int fuelSeconds = (burnTicks + (isFuel(fuel) ? fuel.getCount() * fuel.getBurnTime(RecipeType.SMELTING) : 0)) / 20;
        return Component.translatable("craftorio.drill.status", blocks, tier.area(), rate, fuelSeconds);
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), fuel);
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), output);
            fuel = ItemStack.EMPTY;
            output = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fuel", fuel.saveOptional(registries));
        tag.put("output", output.saveOptional(registries));
        tag.putInt("burn_ticks", burnTicks);
        tag.putDouble("progress", production.progress());
        tag.putInt("next_field", nextField);
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = ItemStack.parseOptional(registries, tag.getCompound("fuel"));
        output = ItemStack.parseOptional(registries, tag.getCompound("output"));
        burnTicks = tag.getInt("burn_ticks");
        production.setProgress(tag.getDouble("progress"));
        nextField = tag.getInt("next_field");
        energy.setEnergy(tag.getInt("energy"));
    }

    /** Slot 0 accepts fuel only; slot 1 holds mined output and can only be extracted. */
    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == FUEL_SLOT ? fuel : output;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != FUEL_SLOT || !isFuel(stack) || !tier.usesFuel()) {
                return stack;
            }
            if (!fuel.isEmpty() && !ItemStack.isSameItemSameComponents(fuel, stack)) {
                return stack;
            }
            int room = stack.getMaxStackSize() - fuel.getCount();
            int moved = Math.min(room, stack.getCount());
            if (moved <= 0) {
                return stack;
            }
            if (!simulate) {
                if (fuel.isEmpty()) {
                    fuel = stack.copyWithCount(moved);
                } else {
                    fuel.grow(moved);
                }
                setChanged();
            }
            return stack.copyWithCount(stack.getCount() - moved);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != OUTPUT_SLOT || output.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            int moved = Math.min(amount, output.getCount());
            ItemStack extracted = output.copyWithCount(moved);
            if (!simulate) {
                output.shrink(moved);
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FUEL_SLOT && isFuel(stack) && tier.usesFuel();
        }
    }
}
