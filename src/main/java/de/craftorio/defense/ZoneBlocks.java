package de.craftorio.defense;

import com.mojang.serialization.MapCodec;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.defense.arena.Arenas;
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

/** Fixed parts of an arena (core, enemy gate) and the path the player lays. */
public final class ZoneBlocks {
    private ZoneBlocks() {
    }

    static Optional<Team> teamOf(@Nullable Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return Optional.of(TeamData.registry(serverPlayer.server).ensureTeam(serverPlayer.getUUID(), serverPlayer.getGameProfile().getName()));
        }
        return Optional.empty();
    }

    /** Goal of the enemy path in the east wall of an arena. Right-click checks the path. Built by the arena only. */
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
            return null;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel serverLevel && Arenas.isArena(level)) {
                TowerDefense defense = TowerDefense.get(serverLevel.getServer());
                teamOf(player).flatMap(team -> defense.zone(team.id()))
                        .filter(zone -> zone.core().equals(pos))
                        .ifPresentOrElse(zone -> player.displayClientMessage(defense.checkPath(serverLevel, zone).message(), false),
                                () -> player.displayClientMessage(Component.translatable("craftorio.arena.error.foreign"), true));
            } else if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("craftorio.arena.old_core").withStyle(ChatFormatting.GRAY), false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** The open enemy gate in the west wall of an arena. Built by the arena only. */
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
            return null;
        }
    }

    /** Flat path block; enemies walk on a single line of these from the open gate to the core. Laid with the path wand. */
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
