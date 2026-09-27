package de.craftorio.logistics;

import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class InserterBlockEntity extends BlockEntity {
    /** Ticks per transferred item (one item per second). */
    public static final int SWING_TICKS = 20;
    private static final int RETRY_TICKS = 4;

    private int cooldown;

    public InserterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INSERTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InserterBlockEntity inserter) {
        if (inserter.cooldown > 0) {
            inserter.cooldown--;
            return;
        }
        inserter.cooldown = transfer(level, pos, state.getValue(InserterBlock.FACING)) ? SWING_TICKS : RETRY_TICKS;
    }

    /** Moves one item that the target accepts; returns whether anything moved. */
    static boolean transfer(Level level, BlockPos pos, Direction facing) {
        IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(facing.getOpposite()), facing);
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(facing), facing.getOpposite());
        if (source == null || target == null) {
            return false;
        }
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack candidate = source.extractItem(slot, 1, true);
            if (candidate.isEmpty() || !ItemHandlerHelper.insertItem(target, candidate, true).isEmpty()) {
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
}
