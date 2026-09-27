package de.craftorio.logistics;

import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Items travel on two lanes (left and right of the travel direction). Belt-to-belt transfer keeps the lane on
 * straights and curves; a belt feeding into the side of a straight belt loads onto the near lane. Anything else
 * with an item handler in front (chests, trading posts, machines) receives the items directly.
 */
public final class ConveyorBeltBlockEntity extends BlockEntity {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    /** 1.875 blocks per second, Factorio's basic belt. */
    public static final float SPEED = 1.875F / 20F;
    private static final float SIDE_INSERT_POSITION = 0.5F;

    @SuppressWarnings("unchecked")
    private final BeltLane<ItemStack>[] lanes = new BeltLane[]{new BeltLane<ItemStack>(), new BeltLane<ItemStack>()};
    private final IItemHandler[] handlers = new IItemHandler[7];
    private boolean dirty;

    public ConveyorBeltBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVEYOR_BELT.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(ConveyorBeltBlock.FACING);
    }

    public BeltLane<ItemStack> lane(int index) {
        return lanes[index];
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ConveyorBeltBlockEntity belt) {
        for (int i = 0; i < belt.lanes.length; i++) {
            BeltLane<ItemStack> lane = belt.lanes[i];
            if (lane.isEmpty()) {
                continue;
            }
            int laneIndex = i;
            if (lane.tick(SPEED, (stack, overshoot) -> !level.isClientSide && belt.handOff(level, stack, laneIndex, overshoot))) {
                belt.dirty = true;
            }
        }
        if (belt.dirty && !level.isClientSide) {
            belt.dirty = false;
            belt.sync();
        }
    }

    private boolean handOff(Level level, ItemStack stack, int lane, float overshoot) {
        Direction facing = facing();
        BlockPos front = worldPosition.relative(facing);
        if (level.getBlockEntity(front) instanceof ConveyorBeltBlockEntity next) {
            return next.acceptFromBelt(facing, lane, stack, overshoot);
        }
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, front, facing.getOpposite());
        return target != null && ItemHandlerHelper.insertItem(target, stack.copy(), false).isEmpty();
    }

    /** An item arriving from a neighbouring belt that moves towards {@code travel}. */
    boolean acceptFromBelt(Direction travel, int lane, ItemStack stack, float overshoot) {
        Direction facing = facing();
        boolean accepted;
        if (travel == facing) {
            accepted = lanes[lane].acceptFromBehind(stack, overshoot);
        } else if (travel == facing.getOpposite()) {
            accepted = false;
        } else if (isFedFromBehind()) {
            accepted = lanes[laneOnSide(travel.getOpposite())].insertAt(stack, SIDE_INSERT_POSITION);
        } else {
            // Nothing feeds this belt from behind, so it acts as a curve and lanes carry over.
            accepted = lanes[lane].acceptFromBehind(stack, overshoot);
        }
        if (accepted) {
            dirty = true;
        }
        return accepted;
    }

    private boolean isFedFromBehind() {
        Direction facing = facing();
        return level != null && level.getBlockEntity(worldPosition.relative(facing.getOpposite())) instanceof ConveyorBeltBlockEntity behind
                && behind.facing() == facing;
    }

    /** The lane next to the given side of this belt. */
    private int laneOnSide(Direction side) {
        return side == facing().getCounterClockWise() ? LEFT : RIGHT;
    }

    /** Takes items dropped onto the belt, one per tick, onto the lane nearest to them. */
    void pickUp(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        Direction facing = facing();
        Vec3 offset = entity.position().subtract(Vec3.atBottomCenterOf(worldPosition));
        Direction left = facing.getCounterClockWise();
        double sideways = offset.x * left.getStepX() + offset.z * left.getStepZ();
        double along = offset.x * facing.getStepX() + offset.z * facing.getStepZ();
        float position = (float) Math.max(0, Math.min(1, along + 0.5));
        int preferred = sideways >= 0 ? LEFT : RIGHT;
        for (int lane : new int[]{preferred, 1 - preferred}) {
            if (lanes[lane].insertAt(stack.copyWithCount(1), position)) {
                stack.shrink(1);
                dirty = true;
                if (stack.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(stack);
                }
                return;
            }
        }
    }

    public IItemHandler handler(@Nullable Direction side) {
        int index = side == null ? 6 : side.get3DDataValue();
        if (handlers[index] == null) {
            handlers[index] = new Handler(side);
        }
        return handlers[index];
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (BeltLane<ItemStack> lane : lanes) {
            for (BeltLane.Entry<ItemStack> entry : lane.entries()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), entry.item());
            }
            lane.clear();
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag lanesTag = new ListTag();
        for (BeltLane<ItemStack> lane : lanes) {
            ListTag entries = new ListTag();
            for (BeltLane.Entry<ItemStack> entry : lane.entries()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.put("item", entry.item().save(registries));
                entryTag.putFloat("progress", entry.progress());
                entries.add(entryTag);
            }
            lanesTag.add(entries);
        }
        tag.put("lanes", lanesTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag lanesTag = tag.getList("lanes", Tag.TAG_LIST);
        for (int i = 0; i < lanes.length; i++) {
            lanes[i].clear();
            if (i >= lanesTag.size()) {
                continue;
            }
            ListTag entries = lanesTag.getList(i);
            for (int j = 0; j < entries.size(); j++) {
                CompoundTag entryTag = entries.getCompound(j);
                int laneIndex = i;
                ItemStack.parse(registries, entryTag.getCompound("item"))
                        .ifPresent(stack -> lanes[laneIndex].restore(stack, entryTag.getFloat("progress")));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Item handler seen from one side. Inserting from the left or right puts the item on the far lane (like an
     * inserter reaching across), from behind at the start of a lane, from anywhere else in the middle.
     * Slot n exposes the front item of lane n for extraction.
     */
    private final class Handler implements IItemHandler {
        private final @Nullable Direction side;

        Handler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return lanes.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemStack front = lanes[slot].front();
            return front == null ? ItemStack.EMPTY : front;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return stack;
            }
            Direction facing = facing();
            float position = SIDE_INSERT_POSITION;
            List<Integer> order = List.of(RIGHT, LEFT);
            if (side == facing.getOpposite()) {
                position = 0;
            } else if (side == facing.getCounterClockWise()) {
                order = List.of(RIGHT);
            } else if (side == facing.getClockWise()) {
                order = List.of(LEFT);
            }
            for (int lane : order) {
                if (lanes[lane].canInsertAt(position)) {
                    if (!simulate) {
                        lanes[lane].insertAt(stack.copyWithCount(1), position);
                        dirty = true;
                    }
                    return stack.copyWithCount(stack.getCount() - 1);
                }
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack front = lanes[slot].front();
            if (front == null || amount <= 0) {
                return ItemStack.EMPTY;
            }
            if (!simulate) {
                lanes[slot].removeFront();
                dirty = true;
            }
            return front.copy();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }
}
