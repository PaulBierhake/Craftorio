package de.craftorio.world;

import de.craftorio.quest.Quest;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.function.Supplier;

/**
 * Part of an infinite ore field. Drills on top extract {@link #resource()} forever; mining it by hand yields one
 * item but the block stays, so manual work is possible but slow.
 */
public final class OreFieldBlock extends Block {
    private final Supplier<? extends ItemLike> resource;

    public OreFieldBlock(Supplier<? extends ItemLike> resource, Properties properties) {
        super(properties);
        this.resource = resource;
    }

    public ItemStack resource() {
        return new ItemStack(resource.get());
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (player.isCreative()) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
        if (!level.isClientSide && willHarvest) {
            ItemStack stack = resource();
            popResource(level, pos, stack);
            if (player instanceof ServerPlayer serverPlayer) {
                // Counts for the guide's first step (mining by hand towards the first drill).
                TeamRegistry teams = TeamData.registry(serverPlayer.server);
                Team team = teams.ensureTeam(serverPlayer.getUUID(), serverPlayer.getGameProfile().getName());
                teams.recordBuild(team.id(), Quest.MINED_PREFIX + BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount());
            }
        }
        // Returning false keeps the block; the client's predicted removal is corrected by the server.
        return false;
    }
}
