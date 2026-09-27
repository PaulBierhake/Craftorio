package de.craftorio.defense.arena;

import de.craftorio.defense.TowerDefense;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Lays the enemy path in the team's arena. Click the floor to set a path block; the next click in the same row or
 * column draws a straight line from the last one. Sneak-click a path block to remove it, sneak-click elsewhere to
 * start a new line.
 */
public final class PathWandItem extends Item {
    public PathWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        TowerDefense defense = TowerDefense.get(level.getServer());
        String error = defense.pathEditError(player, clicked);
        if (error != null) {
            player.displayClientMessage(Component.translatable(error).withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        int[] tile = Arenas.tileAt(clicked);
        int slot = Arenas.slotAt(clicked);
        BlockPos target = Arenas.field(slot, tile[0], tile[1], Arenas.BUILD_Y);
        if (player.isShiftKeyDown()) {
            stack.remove(ModDataComponents.PATH_ANCHOR.get());
            if (level.getBlockState(target).is(ModBlocks.PATH_BLOCK.get())) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            return InteractionResult.CONSUME;
        }
        ArenaLayout layout = defense.layoutAt(slot);
        BlockPos anchor = stack.get(ModDataComponents.PATH_ANCHOR.get());
        List<BlockPos> line = anchor != null && Arenas.slotAt(anchor) == slot && !anchor.equals(target)
                && (anchor.getX() == target.getX() || anchor.getZ() == target.getZ())
                ? line(anchor, target) : List.of(target);
        int placed = 0;
        for (BlockPos pos : line) {
            int[] at = Arenas.tileAt(pos);
            BlockState current = level.getBlockState(pos);
            if (current.is(ModBlocks.PATH_BLOCK.get())) {
                continue;
            }
            if (!layout.tile(at[0], at[1]).allowsPath() || !(current.isAir() || current.canBeReplaced())) {
                player.displayClientMessage(Component.translatable("craftorio.arena.path.blocked").withStyle(ChatFormatting.RED), true);
                break;
            }
            level.setBlock(pos, ModBlocks.PATH_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            placed++;
        }
        if (placed > 0) {
            level.playSound(null, target, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        stack.set(ModDataComponents.PATH_ANCHOR.get(), target);
        return InteractionResult.CONSUME;
    }

    /** Blocks from just after {@code from} up to and including {@code to}, along one axis. */
    private static List<BlockPos> line(BlockPos from, BlockPos to) {
        List<BlockPos> blocks = new java.util.ArrayList<>();
        int dx = Integer.signum(to.getX() - from.getX());
        int dz = Integer.signum(to.getZ() - from.getZ());
        BlockPos pos = from;
        while (!pos.equals(to)) {
            pos = pos.offset(dx, 0, dz);
            blocks.add(pos);
        }
        return blocks;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("craftorio.arena.path_wand.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
