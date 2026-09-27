package de.craftorio.compat.jade;

import de.craftorio.Craftorio;
import de.craftorio.economy.Credits;
import de.craftorio.economy.Economy;
import de.craftorio.world.OreFieldBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** What an ore field yields and what that is worth; needs no server data. */
enum OreFieldProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = Craftorio.id("ore_field");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlock() instanceof OreFieldBlock field) {
            ItemStack resource = field.resource();
            tooltip.add(Component.translatable("craftorio.jade.ore_field", resource.getHoverName(),
                    Credits.format(Economy.unitPrice(resource))).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
