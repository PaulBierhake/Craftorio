package de.craftorio.blueprint;

import com.mojang.serialization.MapCodec;
import de.craftorio.menu.TerminalMenu;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The team's terminal: unlock blueprints, view statistics (tower defense follows in M5). */
public final class TerminalBlock extends Block {
    public static final MapCodec<TerminalBlock> CODEC = simpleCodec(TerminalBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public TerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            TeamRegistry registry = TeamData.registry(serverPlayer.server);
            Team team = registry.ensureTeam(serverPlayer.getUUID(), serverPlayer.getGameProfile().getName());
            TerminalStats stats = TerminalStats.of(team, registry.currentMinute());
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new TerminalMenu(id, inventory, pos, stats), getName()),
                    buf -> {
                        buf.writeBlockPos(pos);
                        TerminalStats.STREAM_CODEC.encode(buf, stats);
                    });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
