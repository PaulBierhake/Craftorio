package de.craftorio.blueprint;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Rewarded every 10 tower defense levels; needed to unlock the next factory tier. Cannot be crafted or sold. */
public final class KeyMaterialItem extends Item {
    private final int level;

    public KeyMaterialItem(int level, Properties properties) {
        super(properties);
        this.level = level;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("craftorio.key_material.hint", level).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
