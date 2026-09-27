package de.craftorio.logistics;

import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

public final class ElevatorBlockEntity extends BlockEntity {
    /** Farthest partner search (enough to reach from the surface to the mine layer). */
    public static final int MAX_DISTANCE = 256;
    private static final int TRANSFER_INTERVAL = 2;
    private static final int ITEMS_PER_TRANSFER = 4;

    /** Items waiting to go up or down (sender) or to be pushed out of the front (receiver). */
    private final ItemStackHandler buffer = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler input = new Input();
    private int cooldown;

    public ElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELEVATOR.get(), pos, state);
    }

    public IItemHandler input() {
        return input;
    }

    public ItemStackHandler buffer() {
        return buffer;
    }

    private ElevatorBlock.Mode mode() {
        return getBlockState().getValue(ElevatorBlock.MODE);
    }

    /** The next elevator above (SEND_UP) or below (SEND_DOWN) in the same column. */
    public static Optional<BlockPos> findPartner(Level level, BlockPos from, ElevatorBlock.Mode mode) {
        if (mode == ElevatorBlock.Mode.RECEIVE) {
            return Optional.empty();
        }
        int step = mode == ElevatorBlock.Mode.SEND_UP ? 1 : -1;
        BlockPos.MutableBlockPos pos = from.mutable();
        for (int i = 1; i <= MAX_DISTANCE; i++) {
            pos.setY(from.getY() + step * i);
            if (level.isOutsideBuildHeight(pos)) {
                break;
            }
            if (level.getBlockEntity(pos) instanceof ElevatorBlockEntity) {
                return Optional.of(pos.immutable());
            }
        }
        return Optional.empty();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElevatorBlockEntity elevator) {
        if (elevator.buffer.getStackInSlot(0).isEmpty() || --elevator.cooldown > 0) {
            return;
        }
        elevator.cooldown = TRANSFER_INTERVAL;
        if (elevator.mode() == ElevatorBlock.Mode.RECEIVE) {
            elevator.pushOut(level, pos, state.getValue(ElevatorBlock.FACING));
        } else {
            findPartner(level, pos, elevator.mode())
                    .map(level::getBlockEntity)
                    .filter(ElevatorBlockEntity.class::isInstance)
                    .map(ElevatorBlockEntity.class::cast)
                    .ifPresent(elevator::sendTo);
        }
    }

    private void sendTo(ElevatorBlockEntity partner) {
        ItemStack moving = buffer.extractItem(0, ITEMS_PER_TRANSFER, true);
        ItemStack rest = partner.buffer.insertItem(0, moving, false);
        buffer.extractItem(0, moving.getCount() - rest.getCount(), false);
    }

    private void pushOut(Level level, BlockPos pos, Direction facing) {
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(facing), facing.getOpposite());
        if (target == null) {
            return;
        }
        for (int i = 0; i < ITEMS_PER_TRANSFER && !buffer.getStackInSlot(0).isEmpty(); i++) {
            ItemStack one = buffer.extractItem(0, 1, true);
            if (!ItemHandlerHelper.insertItem(target, one, false).isEmpty()) {
                return;
            }
            buffer.extractItem(0, 1, false);
        }
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), buffer.getStackInSlot(0));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("buffer", buffer.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.deserializeNBT(registries, tag.getCompound("buffer"));
    }

    /** Senders accept items; receivers only give them out through their front. */
    private final class Input implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return buffer.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return mode() == ElevatorBlock.Mode.RECEIVE ? stack : buffer.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return mode() == ElevatorBlock.Mode.RECEIVE ? buffer.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return mode() != ElevatorBlock.Mode.RECEIVE;
        }
    }
}
