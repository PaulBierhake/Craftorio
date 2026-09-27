package de.craftorio.blueprint;

import de.craftorio.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Right-click on a workbench one tier below {@link #targetTier} to upgrade it in place. */
public final class WorkbenchUpgradeItem extends Item {
    private final int targetTier;

    public WorkbenchUpgradeItem(int targetTier, Properties properties) {
        super(properties);
        this.targetTier = targetTier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof WorkbenchBlock workbench) || workbench.tier() != targetTier - 1) {
            if (context.getPlayer() != null && !level.isClientSide) {
                context.getPlayer().displayClientMessage(Component.translatable("craftorio.workbench.upgrade_target", targetTier - 1)
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(context.getClickedPos(), ModBlocks.workbench(targetTier).get().defaultBlockState()
                    .setValue(WorkbenchBlock.FACING, state.getValue(WorkbenchBlock.FACING)));
            level.playSound(null, context.getClickedPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("craftorio.workbench.upgrade_hint", targetTier - 1).withStyle(ChatFormatting.GRAY));
    }
}
