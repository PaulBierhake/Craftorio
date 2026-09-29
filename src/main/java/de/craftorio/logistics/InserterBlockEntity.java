package de.craftorio.logistics;

import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import de.craftorio.protection.BlockOwnership;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class InserterBlockEntity extends BlockEntity {
    private static final int RETRY_TICKS = 4;

    private int cooldown;
    private ItemStack filter = ItemStack.EMPTY;

    public InserterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INSERTER.get(), pos, state);
    }

    public InserterType type() {
        return getBlockState().getBlock() instanceof InserterBlock block ? block.type() : InserterType.BASIC;
    }

    public ItemStack filter() {
        return filter;
    }

    public void setFilter(ItemStack stack) {
        filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InserterBlockEntity inserter) {
        if (inserter.cooldown > 0) {
            inserter.cooldown--;
            return;
        }
        InserterType type = inserter.type();
        boolean moved = transfer(level, pos, state.getValue(InserterBlock.FACING), type.reach(),
                type.hasFilter() ? inserter.filter : ItemStack.EMPTY, type.hasFilter());
        inserter.cooldown = moved ? type.swingTicks() : RETRY_TICKS;
    }

    /** Moves one item that the target accepts; returns whether anything moved. */
    static boolean transfer(Level level, BlockPos pos, Direction facing) {
        return transfer(level, pos, facing, 1, ItemStack.EMPTY, false);
    }

    /**
     * @param reach     blocks to the source and to the target
     * @param filter    with {@code filtered}, only items like this one move (an unset filter moves nothing)
     */
    static boolean transfer(Level level, BlockPos pos, Direction facing, int reach, ItemStack filter, boolean filtered) {
        BlockPos sourcePos = pos.relative(facing.getOpposite(), reach);
        BlockPos targetPos = pos.relative(facing, reach);
        IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, sourcePos, facing);
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, facing.getOpposite());
        if (source == null || target == null || filtered && filter.isEmpty()) {
            return false;
        }
        // An inserter must not take items out of another team's blocks (or push into them).
        if (level instanceof ServerLevel serverLevel && (!BlockOwnership.sameOwner(serverLevel, pos, sourcePos)
                || !BlockOwnership.sameOwner(serverLevel, pos, targetPos))) {
            return false;
        }
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack candidate = source.extractItem(slot, 1, true);
            if (candidate.isEmpty() || filtered && !ItemStack.isSameItemSameComponents(candidate, filter)
                    || !ItemHandlerHelper.insertItem(target, candidate, true).isEmpty()) {
                continue;
            }
            ItemStack taken = source.extractItem(slot, 1, false);
            ItemStack rest = ItemHandlerHelper.insertItem(target, taken, false);
            if (!rest.isEmpty()) {
                // Target changed its mind between simulation and insertion; put the item back.
                source.insertItem(slot, rest, false);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!filter.isEmpty()) {
            tag.put("filter", filter.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filter = tag.contains("filter") ? ItemStack.parse(registries, tag.getCompound("filter")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }
}
