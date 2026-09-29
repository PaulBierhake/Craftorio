package de.craftorio.logistics;

import com.mojang.serialization.MapCodec;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Two-lane conveyor running towards {@link #FACING}. Also carries dropped items and walking entities. Depending on
 * its {@link BeltKind} it is a plain belt (optionally sloped), one end of an underground belt or a splitter.
 */
public final class ConveyorBeltBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BeltSlope> SLOPE = EnumProperty.create("slope", BeltSlope.class);
    /** Underground belts: false for the entrance, true for the exit. */
    public static final BooleanProperty EXIT = BooleanProperty.create("exit");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 3, 16);
    /** Sloped belts are half a block high at their low end; walking up needs no jump. */
    private static final VoxelShape SLOPE_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    private static final double ENTITY_PUSH = 0.04;

    private final BeltTier tier;
    private final BeltKind kind;
    private final MapCodec<ConveyorBeltBlock> codec;

    public ConveyorBeltBlock(BeltTier tier, Properties properties) {
        this(tier, BeltKind.BELT, properties);
    }

    public ConveyorBeltBlock(BeltTier tier, BeltKind kind, Properties properties) {
        super(properties);
        this.tier = tier;
        this.kind = kind;
        this.codec = simpleCodec(p -> new ConveyorBeltBlock(tier, kind, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SLOPE, BeltSlope.FLAT).setValue(EXIT, false));
    }

    public BeltTier tier() {
        return tier;
    }

    public BeltKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLOPE, EXIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return switch (kind) {
            case BELT -> state.setValue(SLOPE, slopeFor(level, pos, facing));
            case UNDERGROUND -> state.setValue(EXIT, findEntrance(level, pos, facing, tier) != null);
            case SPLITTER -> state;
        };
    }

    /** A belt placed above a flat belt next to it turns that belt into a slope, whichever was placed first. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || kind != BeltKind.BELT || oldState.getBlock() instanceof ConveyorBeltBlock) {
            return;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos lowerPos = pos.relative(side).below();
            BlockState lower = level.getBlockState(lowerPos);
            if (lower.getBlock() instanceof ConveyorBeltBlock belt && belt.kind == BeltKind.BELT && lower.getValue(SLOPE) == BeltSlope.FLAT) {
                BeltSlope slope = slopeFor(level, lowerPos, lower.getValue(FACING));
                if (slope != BeltSlope.FLAT) {
                    level.setBlock(lowerPos, lower.setValue(SLOPE, slope), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** A belt placed with a belt one level higher in front (or behind) turns into a slope. */
    static BeltSlope slopeFor(Level level, BlockPos pos, Direction facing) {
        boolean flatFront = level.getBlockState(pos.relative(facing)).getBlock() instanceof ConveyorBeltBlock;
        boolean flatBack = level.getBlockState(pos.relative(facing.getOpposite())).getBlock() instanceof ConveyorBeltBlock;
        if (!flatFront && level.getBlockState(pos.relative(facing).above()).getBlock() instanceof ConveyorBeltBlock) {
            return BeltSlope.UP;
        }
        if (!flatBack && level.getBlockState(pos.relative(facing.getOpposite()).above()).getBlock() instanceof ConveyorBeltBlock) {
            return BeltSlope.DOWN;
        }
        return BeltSlope.FLAT;
    }

    /** The unpaired underground entrance behind {@code pos} that a new piece there would be the exit of, or null. */
    static @Nullable BlockPos findEntrance(Level level, BlockPos pos, Direction facing, BeltTier tier) {
        for (int distance = 1; distance <= tier.undergroundGap() + 1; distance++) {
            BlockPos candidate = pos.relative(facing.getOpposite(), distance);
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof ConveyorBeltBlock belt && belt.kind == BeltKind.UNDERGROUND && state.getValue(FACING) == facing) {
                if (state.getValue(EXIT)) {
                    return null; // an exit is in between: the entrance further back is already paired
                }
                return distance <= belt.tier.undergroundGap() + 1 && findExit(level, candidate, facing, belt.tier) == null ? candidate : null;
            }
        }
        return null;
    }

    /** The underground exit an entrance at {@code pos} leads to, or null. */
    static @Nullable BlockPos findExit(Level level, BlockPos pos, Direction facing, BeltTier tier) {
        for (int distance = 1; distance <= tier.undergroundGap() + 1; distance++) {
            BlockPos candidate = pos.relative(facing, distance);
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof ConveyorBeltBlock belt && belt.kind == BeltKind.UNDERGROUND && state.getValue(FACING) == facing) {
                return state.getValue(EXIT) ? candidate : null;
            }
        }
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SLOPE) == BeltSlope.FLAT ? SHAPE : SLOPE_SHAPE;
    }

    /** A splitter takes a filter from the item in the player's hand. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (kind == BeltKind.SPLITTER && !stack.isEmpty() && !(stack.getItem() instanceof net.minecraft.world.item.BlockItem block
                && block.getBlock() instanceof ConveyorBeltBlock) && level.getBlockEntity(pos) instanceof ConveyorBeltBlockEntity splitter) {
            if (!level.isClientSide) {
                splitter.setFilter(stack);
                player.displayClientMessage(Component.translatable("craftorio.splitter.filter_set", stack.getHoverName(),
                        Component.translatable("craftorio.splitter.output." + splitter.filterOutput().name().toLowerCase())), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // A plain belt in hand, clicked on the top of a belt, changes the slope instead of stacking a belt on it.
        if (kind == BeltKind.BELT && hit.getDirection() == Direction.UP && !player.isShiftKeyDown()
                && stack.getItem() instanceof net.minecraft.world.item.BlockItem block
                && block.getBlock() instanceof ConveyorBeltBlock other && other.kind == BeltKind.BELT) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(SLOPE, state.getValue(SLOPE).next()), Block.UPDATE_ALL);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Right-click with an empty hand turns a belt into a slope and back. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (kind == BeltKind.SPLITTER && level.getBlockEntity(pos) instanceof ConveyorBeltBlockEntity splitter) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    splitter.setFilter(ItemStack.EMPTY);
                    player.displayClientMessage(Component.translatable("craftorio.splitter.filter_cleared"), true);
                } else if (splitter.filter().isEmpty()) {
                    player.displayClientMessage(Component.translatable("craftorio.splitter.no_filter"), true);
                } else {
                    var output = splitter.cycleFilterOutput();
                    player.displayClientMessage(Component.translatable("craftorio.splitter.filter_set", splitter.filter().getHoverName(),
                            Component.translatable("craftorio.splitter.output." + output.name().toLowerCase())), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind != BeltKind.BELT || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(SLOPE, state.getValue(SLOPE).next()), Block.UPDATE_ALL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorBeltBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Ticks on the client too so items glide smoothly between server updates.
        return createTickerHelper(type, ModBlockEntities.CONVEYOR_BELT.get(), ConveyorBeltBlockEntity::tick);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity item) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ConveyorBeltBlockEntity belt) {
                belt.pickUp(item);
            }
        } else if (entity instanceof LivingEntity && !entity.isShiftKeyDown() && entity.onGround()) {
            Direction facing = state.getValue(FACING);
            entity.setDeltaMovement(entity.getDeltaMovement().add(new Vec3(facing.getStepX(), 0, facing.getStepZ()).scale(ENTITY_PUSH * tier.blocksPerSecond() / BeltTier.BASIC.blocksPerSecond())));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ConveyorBeltBlockEntity belt) {
            belt.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
