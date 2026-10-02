package de.craftorio.world.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Right-click on water: fills a 3 × 3 area up to the water surface with earth (the landfill of Factorio). */
public final class LandfillItem extends Item {
    public LandfillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || !WorldTools.isWater(level, hit.getBlockPos())) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("craftorio.tool.landfill.no_water"), true);
            }
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!level.mayInteract(player, hit.getBlockPos())) {
            return InteractionResultHolder.fail(stack);
        }
        if (WorldTools.landfill(level, hit.getBlockPos()) == 0) {
            return InteractionResultHolder.pass(stack);
        }
        level.playSound(null, hit.getBlockPos(), SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 1.0f, 0.9f);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }
}
