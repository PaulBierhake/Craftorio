package de.craftorio.world.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Right-click on a plateau at its cliff: lowers a 5 × 5 area by one step; the debris drops as stone. */
public final class CliffExplosivesItem extends Item {
    public CliffExplosivesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (player != null && !level.mayInteract(player, pos)) {
            return InteractionResult.FAIL;
        }
        if (WorldTools.blastCliff(level, pos) == 0) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("craftorio.tool.cliff.no_cliff"), true);
            }
            return InteractionResult.FAIL;
        }
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
