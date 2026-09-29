package de.craftorio.fluid;

import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The fluid container of pipes, underground pipes and storage tanks: one buffer that evens out with the buffers of
 * all connected neighbours every tick (and, for an underground pipe, with the other half of its tunnel).
 */
public final class FluidPipeBlockEntity extends BlockEntity {
    private static final int PARTNER_CHECK_INTERVAL = 20;

    private final FluidBuffer buffer;
    private int partnerCheckIn;
    private @Nullable BlockPos partner;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PIPE.get(), pos, state);
        buffer = new FluidBuffer(state.getBlock() instanceof StorageTankBlock ? StorageTankBlock.CAPACITY : FluidPipeBlock.CAPACITY, this::setChanged);
    }

    public FluidBuffer buffer() {
        return buffer;
    }

    /** The handler other blocks see from {@code side}; null where this block is not connected. */
    public @Nullable IFluidHandler handler(@Nullable Direction side) {
        BlockState state = getBlockState();
        if (side == null || state.getBlock() instanceof StorageTankBlock) {
            return buffer;
        }
        if (state.getBlock() instanceof UndergroundPipeBlock) {
            return side == state.getValue(UndergroundPipeBlock.FACING) ? buffer : null;
        }
        return state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side)) ? buffer : null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity pipe) {
        if (pipe.buffer.isEmpty()) {
            return;
        }
        if (state.getBlock() instanceof UndergroundPipeBlock) {
            if (--pipe.partnerCheckIn <= 0) {
                pipe.partnerCheckIn = PARTNER_CHECK_INTERVAL;
                pipe.partner = UndergroundPipeBlock.partnerOf(level, pos);
            }
            if (pipe.partner != null && level.getBlockEntity(pipe.partner) instanceof FluidPipeBlockEntity other) {
                FluidHelper.balance(pipe.buffer, other.buffer);
            }
            Direction open = state.getValue(UndergroundPipeBlock.FACING);
            IFluidHandler neighbour = FluidHelper.neighbour(level, pos, open);
            if (neighbour != null) {
                FluidHelper.balance(pipe.buffer, neighbour);
            }
            return;
        }
        for (Direction direction : Direction.values()) {
            if (pipe.handler(direction) == null) {
                continue;
            }
            IFluidHandler neighbour = FluidHelper.neighbour(level, pos, direction);
            if (neighbour != null) {
                FluidHelper.balance(pipe.buffer, neighbour);
            }
        }
    }

    /** Right-click: what is in there. */
    static InteractionResult describe(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            serverPlayer.displayClientMessage(pipe.buffer.isEmpty()
                    ? Component.translatable("craftorio.fluid.empty", pipe.buffer.getCapacity())
                    : Component.translatable("craftorio.fluid.contents", pipe.buffer.getFluid().getHoverName(),
                            pipe.buffer.getFluidAmount(), pipe.buffer.getCapacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        buffer.writeToNBT(registries, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.readFromNBT(registries, tag);
    }
}
