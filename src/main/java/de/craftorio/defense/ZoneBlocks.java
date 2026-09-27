package de.craftorio.defense;

import com.mojang.serialization.MapCodec;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Blocks that define a team's defense zone. */
public final class ZoneBlocks {
    private ZoneBlocks() {
    }

    static Optional<Team> teamOf(@Nullable Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return Optional.of(TeamData.registry(serverPlayer.server).ensureTeam(serverPlayer.getUUID(), serverPlayer.getGameProfile().getName()));
        }
        return Optional.empty();
    }

    /** Server-side placement check: inside the placing team's zone. Clients always predict success. */
    static boolean allowedInZone(BlockPlaceContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return true;
        }
        Optional<TowerDefense.Zone> zone = teamOf(context.getPlayer())
                .flatMap(team -> TowerDefense.get(level.getServer()).zone(team.id()));
        boolean ok = level.dimension() == Level.OVERWORLD && zone.isPresent() && TowerDefense.inZone(zone.get().core(), context.getClickedPos());
        if (!ok && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable("craftorio.td.error.outside_zone", TowerDefense.ZONE_RADIUS)
                    .withStyle(ChatFormatting.RED), true);
        }
        return ok;
    }

    /** Centre of the zone and goal of the enemy path. Right-click checks the path. */
    public static final class Core extends Block {
        public static final MapCodec<Core> CODEC = simpleCodec(Core::new);

        public Core(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
            Level level = context.getLevel();
            if (!level.isClientSide) {
                Optional<Team> team = teamOf(context.getPlayer());
                String error = level.dimension() != Level.OVERWORLD ? "craftorio.td.error.overworld_only"
                        : team.isEmpty() ? "craftorio.td.error.no_team"
                        : TowerDefense.get(level.getServer()).zone(team.get().id()).isPresent() ? "craftorio.td.error.zone_exists" : null;
                if (error != null) {
                    if (context.getPlayer() != null) {
                        context.getPlayer().displayClientMessage(Component.translatable(error).withStyle(ChatFormatting.RED), true);
                    }
                    return null;
                }
            }
            return defaultBlockState();
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            if (placer instanceof Player player) {
                teamOf(player).ifPresent(team -> TowerDefense.get(level.getServer()).placeCore(team.id(), pos));
            }
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
                TowerDefense.get(serverLevel.getServer()).removeCore(serverLevel, pos);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel serverLevel) {
                TowerDefense defense = TowerDefense.get(serverLevel.getServer());
                teamOf(player).flatMap(team -> defense.zone(team.id()))
                        .filter(zone -> zone.core().equals(pos))
                        .ifPresentOrElse(zone -> player.displayClientMessage(defense.checkPath(serverLevel, zone).message(), false),
                                () -> player.displayClientMessage(Component.translatable("craftorio.td.error.foreign_zone"), true));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** Where enemies come from; the path starts next to it. One per zone. */
    public static final class Portal extends Block {
        public static final MapCodec<Portal> CODEC = simpleCodec(Portal::new);

        public Portal(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
            return allowedInZone(context) ? defaultBlockState() : null;
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            if (placer instanceof Player player) {
                teamOf(player).ifPresent(team -> TowerDefense.get(level.getServer()).setPortal(team.id(), pos));
            }
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
                TowerDefense.get(serverLevel.getServer()).removePortal(pos);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    /** Flat path block; enemies walk on a single line of these from the portal to the core. */
    public static final class Path extends Block {
        public static final MapCodec<Path> CODEC = simpleCodec(Path::new);
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

        public Path(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean useShapeForLightOcclusion(BlockState state) {
            return true;
        }
    }
}
