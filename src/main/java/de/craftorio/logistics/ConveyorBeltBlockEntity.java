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
    private static final float SIDE_INSERT_POSITION = 0.5F;

    @SuppressWarnings("unchecked")
    private final BeltLane<ItemStack>[] lanes = new BeltLane[]{new BeltLane<ItemStack>(), new BeltLane<ItemStack>()};
    private final IItemHandler[] handlers = new IItemHandler[7];
    private boolean dirty;
    private ItemStack filter = ItemStack.EMPTY;
    private @Nullable SplitterLogic.Output priority;
    private int roundRobin;

    public ConveyorBeltBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVEYOR_BELT.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(ConveyorBeltBlock.FACING);
    }

    public BeltTier tier() {
        return getBlockState().getBlock() instanceof ConveyorBeltBlock belt ? belt.tier() : BeltTier.BASIC;
    }

    public BeltKind kind() {
        return getBlockState().getBlock() instanceof ConveyorBeltBlock belt ? belt.kind() : BeltKind.BELT;
    }

    public BeltSlope slope() {
        return getBlockState().getValue(ConveyorBeltBlock.SLOPE);
    }

    /** Splitter setting: only this item goes to the front output (empty: no filter). */
    public ItemStack filter() {
        return filter;
    }

    public void setFilter(ItemStack stack) {
        filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    /** Splitter setting: the output tried first, or null. */
    public @Nullable SplitterLogic.Output priority() {
        return priority;
    }

    /** Cycles no priority, front, left, right. */
    public @Nullable SplitterLogic.Output cyclePriority() {
        SplitterLogic.Output[] outputs = SplitterLogic.Output.values();
        priority = priority == null ? outputs[0] : priority.ordinal() + 1 < outputs.length ? outputs[priority.ordinal() + 1] : null;
        setChanged();
        return priority;
    }

    public BeltLane<ItemStack> lane(int index) {
        return lanes[index];
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ConveyorBeltBlockEntity belt) {
        float speed = belt.tier().speedPerTick();
        for (int i = 0; i < belt.lanes.length; i++) {
            BeltLane<ItemStack> lane = belt.lanes[i];
            if (lane.isEmpty()) {
                continue;
            }
            int laneIndex = i;
            if (lane.tick(speed, (stack, overshoot) -> !level.isClientSide && belt.handOff(level, stack, laneIndex, overshoot))) {
                belt.dirty = true;
            }
        }
        if (belt.dirty && !level.isClientSide) {
            belt.dirty = false;
            belt.sync();
        }
    }

    private boolean handOff(Level level, ItemStack stack, int lane, float overshoot) {
        BeltKind kind = kind();
        if (kind == BeltKind.UNDERGROUND && !getBlockState().getValue(ConveyorBeltBlock.EXIT)) {
            BlockPos exit = ConveyorBeltBlock.findExit(level, worldPosition, facing(), tier());
            return exit != null && level.getBlockEntity(exit) instanceof ConveyorBeltBlockEntity end && end.acceptFromTunnel(lane, stack);
        }
        if (kind == BeltKind.SPLITTER) {
            return splitOff(level, stack, lane, overshoot);
        }
        return passOn(level, facing(), stack, lane, overshoot);
    }

    /** Hands an item to the belt or container in front of this belt in {@code direction}. */
    private boolean passOn(Level level, Direction direction, ItemStack stack, int lane, float overshoot) {
        BlockPos front = worldPosition.relative(direction);
        boolean sideways = direction != facing();
        boolean beltThere = false;
        // Side outputs of a splitter only feed belts on the same level.
        for (int dy = sideways ? 0 : -1; dy <= (sideways ? 0 : 1); dy++) {
            if (level.getBlockEntity(front.above(dy)) instanceof ConveyorBeltBlockEntity next) {
                beltThere = true;
                if (next.acceptFromBelt(direction, lane, stack, overshoot, worldPosition, slope())) {
                    return true;
                }
            }
        }
        if (beltThere) {
            return false;
        }
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, front, direction.getOpposite());
        return target != null && ItemHandlerHelper.insertItem(target, stack.copy(), false).isEmpty();
    }

    /** Splitter: tries the outputs in the order given by {@link SplitterLogic}. */
    private boolean splitOff(Level level, ItemStack stack, int lane, float overshoot) {
        Direction facing = facing();
        boolean hasFilter = !filter.isEmpty();
        List<SplitterLogic.Output> order = SplitterLogic.order(hasFilter, hasFilter && ItemStack.isSameItemSameComponents(filter, stack),
                priority, roundRobin);
        for (SplitterLogic.Output output : order) {
            Direction direction = switch (output) {
                case FRONT -> facing;
                case LEFT -> facing.getCounterClockWise();
                case RIGHT -> facing.getClockWise();
            };
            if (passOn(level, direction, stack, lane, overshoot)) {
                roundRobin++;
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** Underground exit: items arrive from the entrance at the start of a lane. */
    boolean acceptFromTunnel(int lane, ItemStack stack) {
        boolean accepted = lanes[lane].acceptFromBehind(stack, 0);
        if (accepted) {
            dirty = true;
        }
        return accepted;
    }

    /** An item arriving from a neighbouring belt that moves towards {@code travel}. */
    boolean acceptFromBelt(Direction travel, int lane, ItemStack stack, float overshoot, BlockPos from, BeltSlope senderSlope) {
        Direction facing = facing();
        boolean accepted;
        if (kind() == BeltKind.UNDERGROUND && getBlockState().getValue(ConveyorBeltBlock.EXIT)) {
            return false; // the back of an exit is the tunnel
        }
        if (kind() == BeltKind.SPLITTER && travel != facing) {
            return false; // splitters take items from behind only
        }
        if (!BeltGeometry.connects(from.getY(), senderSlope.outputLevel(), worldPosition.getY(), slope().inputLevel())) {
            return false;
        }
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
        if (level == null) {
            return false;
        }
        for (int dy = -1; dy <= 1; dy++) {
            BlockPos at = worldPosition.relative(facing.getOpposite()).above(dy);
            if (level.getBlockEntity(at) instanceof ConveyorBeltBlockEntity behind && behind.facing() == facing
                    && behind.kind() != BeltKind.UNDERGROUND
                    && BeltGeometry.connects(at.getY(), behind.slope().outputLevel(), worldPosition.getY(), slope().inputLevel())) {
                return true;
            }
        }
        return false;
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
        if (!filter.isEmpty()) {
            tag.put("filter", filter.save(registries));
        }
        if (priority != null) {
            tag.putString("priority", priority.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filter = tag.contains("filter") ? ItemStack.parse(registries, tag.getCompound("filter")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        priority = null;
        if (tag.contains("priority")) {
            try {
                priority = SplitterLogic.Output.valueOf(tag.getString("priority"));
            } catch (IllegalArgumentException unknown) {
                priority = null;
            }
        }
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
