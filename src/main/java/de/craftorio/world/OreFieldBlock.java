package de.craftorio.world;

import net.minecraft.core.BlockPos;
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
            popResource(level, pos, resource());
        }
        // Returning false keeps the block; the client's predicted removal is corrected by the server.
        return false;
    }
}
