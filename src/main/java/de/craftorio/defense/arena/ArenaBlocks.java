package de.craftorio.defense.arena;

import com.mojang.serialization.MapCodec;
import de.craftorio.defense.TowerDefense;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** The blocks that connect a team with its arena. */
public final class ArenaBlocks {
    private ArenaBlocks() {
    }

    /** Built in the overworld; right-click takes the player to the team's arena (created on first use). */
    public static final class Gate extends Block {
        public static final MapCodec<Gate> CODEC = simpleCodec(Gate::new);

        public Gate(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
            if (!context.getLevel().isClientSide && context.getLevel().dimension() != Level.OVERWORLD) {
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(Component.translatable("craftorio.arena.error.overworld_only")
                            .withStyle(ChatFormatting.RED), true);
                }
                return null;
            }
            return defaultBlockState();
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                TowerDefense.get(serverLevel.getServer()).enter(serverPlayer, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** On the stands of an arena; right-click returns to the arena gate the player came from. */
    public static final class Exit extends Block {
        public static final MapCodec<Exit> CODEC = simpleCodec(Exit::new);

        public Exit(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                TowerDefense.get(serverLevel.getServer()).leave(serverPlayer);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** On the stands; hands out the towers packed up after the last map (with their level) and key rewards. */
    public static final class Depot extends Block {
        public static final MapCodec<Depot> CODEC = simpleCodec(Depot::new);

        public Depot(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                TowerDefense.get(serverLevel.getServer()).takeDepot(serverPlayer);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}
